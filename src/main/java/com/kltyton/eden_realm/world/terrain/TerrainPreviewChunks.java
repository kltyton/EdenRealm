package com.kltyton.eden_realm.world.terrain;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.data.worldgen.IcyBiomeWorldgen;
import com.kltyton.eden_realm.data.worldgen.SkyBiomeWorldgen;
import com.kltyton.eden_realm.world.feature.SkySurfaceWaterFeature;
import com.kltyton.eden_realm.world.feature.CrystalLakeIslandFeature;
import com.kltyton.eden_realm.world.feature.FrozenFjordFallsFeature;
import com.kltyton.eden_realm.world.feature.IcyFeatureArea;
import com.kltyton.eden_realm.world.feature.IcyShoreFreezeFeature;
import com.mojang.datafixers.util.Pair;
import io.github.kltyton.kltytonui.chunkmap.ChunkMapSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/** Generates real biome, noise, fluid and surface blocks in memory for the KUI map. */
public final class TerrainPreviewChunks {
    private static final int CENTER_CHUNKS = 6;
    public static final int MIN_CHUNKS = 6;
    public static final int DEFAULT_CHUNKS = 16;
    public static final int MAX_DETAIL_CHUNKS = 32;
    private static final int CHUNK_CONCURRENCY = Math.clamp(Runtime.getRuntime().availableProcessors() / 2, 2, 16);
    private static final ForkJoinPool PREVIEW_POOL = new ForkJoinPool(2);
    private static final Map<CacheKey, ChunkMapSnapshot> SNAPSHOTS = new LinkedHashMap<>(8, 0.75f, true);
    private record CacheKey(RegistryAccess.Frozen registries, String biome, TerrainPack pack,
                            int chunks, BlockPos center) { }
    private static final StructureManager NO_STRUCTURES = new StructureManager(null, null, null) {
        @Override
        public List<StructureStart> startsForStructure(ChunkPos pos, Predicate<Structure> matcher) {
            return List.of();
        }
    };

    private TerrainPreviewChunks() {
    }

    private record Stage(ProtoChunk[][] chunks, int firstChunkX, int firstChunkZ,
                         NoiseBasedChunkGenerator generator, RandomState randomState,
                         TerrainPack pack, LevelHeightAccessor bounds,
                         PalettedContainerFactory factory, WorldGenerationContext generationContext,
                         BiomeManager biomes, IcyPreviewBiomes resolver, ChunkMapSnapshot snapshot, int fullChunks) {
    }

    public record PreviewRequest(CompletableFuture<ChunkMapSnapshot> center,
                                 CompletableFuture<ChunkMapSnapshot> complete,
                                 AtomicBoolean cancelled) {
        public void cancel() {
            cancelled.set(true);
            center.cancel(false);
            complete.cancel(false);
        }
    }

    public static PreviewRequest generate(RegistryAccess.Frozen registries, String biomeId,
                                          TerrainPack pack, int fullChunks) {
        return generate(registries, biomeId, pack, fullChunks, null);
    }

    public static PreviewRequest generate(RegistryAccess.Frozen registries, String biomeId,
                                          TerrainPack pack, int fullChunks, BlockPos knownCenter) {
        return generate(registries, biomeId, pack, fullChunks, knownCenter, new AtomicBoolean());
    }

    public static PreviewRequest generate(RegistryAccess.Frozen registries, String biomeId,
                                          TerrainPack pack, int fullChunks, BlockPos knownCenter,
                                          AtomicBoolean cancelled) {
        if (fullChunks < MIN_CHUNKS || fullChunks > MAX_DETAIL_CHUNKS + 4 || (fullChunks & 1) != 0) {
            throw new IllegalArgumentException("Native region size must be an even chunk count between 6 and 36");
        }
        if (cancelled.get()) return new PreviewRequest(CompletableFuture.failedFuture(new CancellationException()),
                CompletableFuture.failedFuture(new CancellationException()), cancelled);
        CacheKey key = new CacheKey(registries, biomeId, pack, fullChunks,
                knownCenter == null ? null : knownCenter.immutable());
        synchronized (SNAPSHOTS) {
            ChunkMapSnapshot cached = SNAPSHOTS.get(key);
            if (cached != null) {
                return new PreviewRequest(CompletableFuture.completedFuture(cached),
                        CompletableFuture.completedFuture(cached), cancelled);
            }
        }
        CompletableFuture<Stage> stage = CompletableFuture.supplyAsync(() -> {
            if (cancelled.get()) throw new CancellationException();
            TerrainProfile profile = pack.biomes().get(biomeId);
            TerrainPack samplingPack = profile.generationChancePercent() == 0
                    ? pack.withProfile(biomeId, profile.withGenerationChance(100)) : pack;
            NoiseBasedChunkGenerator generator = (NoiseBasedChunkGenerator)
                    dimension(registries, pack, biomeId).generator();
            RandomState randomState = RandomState.create(generator.generatorSettings().value(),
                    registries.lookupOrThrow(Registries.NOISE), pack.seed());
            BlockPos center = knownCenter;
            if (center == null && profile.generationChancePercent() == 0) {
                var locator = (NoiseBasedChunkGenerator) dimension(registries, samplingPack, biomeId).generator();
                var locatorRandom = RandomState.create(locator.generatorSettings().value(),
                        registries.lookupOrThrow(Registries.NOISE), pack.seed());
                var target = ResourceKey.create(Registries.BIOME, ERConstants.id(biomeId));
                var found = locator.getBiomeSource().findBiomeHorizontal(0, 96, 0, 8192, 16,
                        holder -> holder.is(target), RandomSource.create(pack.seed()), true, locatorRandom.sampler());
                if (found == null) throw new IllegalArgumentException("Biome not found: " + biomeId);
                center = found.getFirst();
            }
            return buildCenter(generator, randomState, registries, biomeId, pack, fullChunks, cancelled, center);
        }, PREVIEW_POOL);
        PreviewRequest request = staged(stage, cancelled);
        CompletableFuture<ChunkMapSnapshot> cached = request.complete().thenApply(snapshot -> {
            if (cancelled.get()) throw new CancellationException();
            synchronized (SNAPSHOTS) {
                SNAPSHOTS.put(key, snapshot);
                int columns = SNAPSHOTS.values().stream().mapToInt(value -> value.width() * value.depth()).sum();
                var entries = SNAPSHOTS.entrySet().iterator();
                while (SNAPSHOTS.size() > 8 || columns > 262144) {
                    ChunkMapSnapshot removed = entries.next().getValue();
                    columns -= removed.width() * removed.depth();
                    entries.remove();
                }
            }
            return snapshot;
        });
        return new PreviewRequest(request.center(), cached, cancelled);
    }

    private static net.minecraft.world.level.dimension.LevelStem dimension(RegistryAccess.Frozen registries,
                                                                           TerrainPack pack, String biomeId) {
        return SkyLandformDensity.BIOMES.contains(biomeId) ? SkyBiomeWorldgen.tunedDimension(registries, pack)
                : IcyBiomeWorldgen.tunedDimension(registries, pack);
    }

    private static PreviewRequest staged(CompletableFuture<Stage> stage, AtomicBoolean cancelled) {
        CompletableFuture<ChunkMapSnapshot> center = stage.thenApply(Stage::snapshot);
        CompletableFuture<ChunkMapSnapshot> complete = stage.thenApplyAsync(
                ready -> buildFull(ready, cancelled), PREVIEW_POOL);
        return new PreviewRequest(center, complete, cancelled);
    }

    private static Stage buildCenter(NoiseBasedChunkGenerator generator, RandomState randomState,
                                     RegistryAccess.Frozen registries, String biomeId, TerrainPack pack,
                                     int fullChunks, AtomicBoolean cancelled, BlockPos knownCenter) {
        ResourceKey<Biome> target = ResourceKey.create(Registries.BIOME, ERConstants.id(biomeId));
        Pair<BlockPos, Holder<Biome>> found = knownCenter == null ? generator.getBiomeSource().findBiomeHorizontal(
                0, 96, 0, 8192, 16, holder -> holder.is(target),
                RandomSource.create(pack.seed()), true, randomState.sampler()) : null;
        if (knownCenter == null && found == null) throw new IllegalArgumentException("Biome not found: " + target.identifier());
        var settings = generator.generatorSettings().value().noiseSettings();
        LevelHeightAccessor bounds = LevelHeightAccessor.create(settings.minY(), settings.height());
        BlockPos center = knownCenter != null ? knownCenter
                : representativeCenter(generator, randomState, target, biomeId,
                        found.getFirst());
        int firstChunkX = Math.floorDiv(center.getX(), 16) - CENTER_CHUNKS / 2;
        int firstChunkZ = Math.floorDiv(center.getZ(), 16) - CENTER_CHUNKS / 2;
        PalettedContainerFactory factory = PalettedContainerFactory.create(registries);
        WorldGenerationContext generationContext = new WorldGenerationContext(generator, bounds);
        IcyPreviewBiomes resolver = new IcyPreviewBiomes(registries, randomState, SkyLandformDensity.BIOMES.contains(biomeId));
        BiomeManager biomes = new BiomeManager((x, y, z) -> resolver.getNoiseBiome(x, y, z, randomState.sampler()),
                BiomeManager.obfuscateSeed(pack.seed()));
        ProtoChunk[][] chunks = new ProtoChunk[CENTER_CHUNKS][CENTER_CHUNKS];
        long started = System.nanoTime();
        fillChunks(chunks, firstChunkX, firstChunkZ, generator, randomState,
                bounds, factory, generationContext, biomes, resolver, cancelled);
        long filled = System.nanoTime();
        applyFeatures(chunks, firstChunkX, firstChunkZ, biomes, pack,
                generator, randomState, bounds, false);
        long featured = System.nanoTime();
        ChunkMapSnapshot snapshot = capture(chunks, firstChunkX, firstChunkZ, bounds,
                biomes, cancelled);
        com.kltyton.eden_realm.EdenRealm.LOGGER.debug("Preview center: fill={}ms features={}ms capture={}ms",
                (filled - started) / 1_000_000, (featured - filled) / 1_000_000, (System.nanoTime() - featured) / 1_000_000);
        return new Stage(chunks, firstChunkX, firstChunkZ, generator, randomState, pack,
                bounds, factory, generationContext, biomes, resolver, snapshot, fullChunks);
    }

    static BlockPos representativeCenter(NoiseBasedChunkGenerator generator,
                                          RandomState randomState, ResourceKey<Biome> target,
                                          String biomeId, BlockPos found) {
        if (SkyLandformDensity.BIOMES.contains(biomeId)) return SkyLandformDensity.from(randomState)
                .findReferenceCenter(biomeId, found).orElseThrow(() -> new IllegalArgumentException("Sky biome cell not found: " + biomeId));
        IcyLandformDensity[] landform = new IcyLandformDensity[1];
        randomState.router().preliminarySurfaceLevel().mapAll(field -> {
            if (field instanceof IcyLandformDensity value) landform[0] = value;
            return field;
        });
        if (landform[0] == null) throw new IllegalStateException("Icy landform density missing");
        return landform[0].referenceCenter(biomeId, found);
    }

    private static ChunkMapSnapshot buildFull(Stage stage, AtomicBoolean cancelled) {
        if (cancelled.get()) throw new CancellationException();
        int border = (stage.fullChunks() - CENTER_CHUNKS) / 2;
        int firstChunkX = stage.firstChunkX() - border;
        int firstChunkZ = stage.firstChunkZ() - border;
        ProtoChunk[][] chunks = new ProtoChunk[stage.fullChunks()][stage.fullChunks()];
        for (int z = 0; z < CENTER_CHUNKS; z++) {
            for (int x = 0; x < CENTER_CHUNKS; x++) {
                chunks[z + border][x + border] = stage.chunks()[z][x];
            }
        }
        long started = System.nanoTime();
        fillChunks(chunks, firstChunkX, firstChunkZ, stage.generator(), stage.randomState(),
                stage.bounds(), stage.factory(), stage.generationContext(), stage.biomes(), stage.resolver(), cancelled);
        long filled = System.nanoTime();
        applyFeatures(chunks, firstChunkX, firstChunkZ, stage.biomes(), stage.pack(),
                stage.generator(), stage.randomState(), stage.bounds(), true);
        long featured = System.nanoTime();
        ChunkMapSnapshot snapshot = capture(chunks, firstChunkX, firstChunkZ, stage.bounds(),
                stage.biomes(), cancelled);
        com.kltyton.eden_realm.EdenRealm.LOGGER.debug("Preview {} chunks: fill={}ms features={}ms capture={}ms",
                stage.fullChunks(), (filled - started) / 1_000_000, (featured - filled) / 1_000_000,
                (System.nanoTime() - featured) / 1_000_000);
        return snapshot;
    }

    private static void fillChunks(ProtoChunk[][] chunks, int firstChunkX, int firstChunkZ,
                                   NoiseBasedChunkGenerator generator, RandomState randomState,
                                   LevelHeightAccessor bounds, PalettedContainerFactory factory,
                                   WorldGenerationContext generationContext, BiomeManager biomes,
                                   IcyPreviewBiomes resolver,
                                   AtomicBoolean cancelled) {
        List<CompletableFuture<Void>> active = new ArrayList<>(CHUNK_CONCURRENCY);
        for (int z = 0; z < chunks.length; z++) {
            for (int x = 0; x < chunks.length; x++) {
                if (cancelled.get()) throw new CancellationException();
                if (chunks[z][x] != null) continue;
                ProtoChunk chunk = new ProtoChunk(new ChunkPos(firstChunkX + x, firstChunkZ + z),
                        UpgradeData.EMPTY, generationBounds(randomState, generator, bounds,
                                (firstChunkX + x) * 16, (firstChunkZ + z) * 16), factory, null);
                chunks[z][x] = chunk;
                active.add(CompletableFuture.runAsync(() -> {
                            if (cancelled.get()) throw new CancellationException();
                            chunk.fillBiomesFromNoise(resolver, randomState.sampler());
                        }, Util.backgroundExecutor().forName("icy_preview_biomes"))
                        .thenCompose(ignored -> generator.fillFromNoise(
                                Blender.empty(), randomState, NO_STRUCTURES, chunk))
                        .thenAccept(ignored -> {
                            chunk.setPersistedStatus(ChunkStatus.NOISE);
                            generator.buildSurface(chunk, generationContext,
                                    randomState, NO_STRUCTURES, biomes, Blender.empty(), null);
                            chunk.setPersistedStatus(ChunkStatus.SURFACE);
                        }));
                if (active.size() == CHUNK_CONCURRENCY) {
                    CompletableFuture.anyOf(active.toArray(CompletableFuture[]::new)).join();
                    active.removeIf(task -> {
                        if (!task.isDone()) return false;
                        task.join();
                        return true;
                    });
                }
            }
        }
        CompletableFuture.allOf(active.toArray(CompletableFuture[]::new)).join();
        if (cancelled.get()) throw new CancellationException();
    }

    private static LevelHeightAccessor generationBounds(RandomState randomState, NoiseBasedChunkGenerator generator,
                                                        LevelHeightAccessor bounds, int x, int z) {
        DensityFunction height = randomState.router().preliminarySurfaceLevel();
        double highest = generator.getSeaLevel();
        for (int dz = 0; dz <= 16; dz += 4) {
            for (int dx = 0; dx <= 16; dx += 4) {
                highest = Math.max(highest, height.compute(new DensityFunction.SinglePointContext(x + dx, 0, z + dz)));
            }
        }
        // Bilinear terrain cannot exceed its quart corners; keep space for the preview landmarks above it.
        int top = Math.min(bounds.getMaxY() + 1, Math.ceilDiv((int) Math.ceil(highest) + 32, 16) * 16);
        return LevelHeightAccessor.create(bounds.getMinY(), top - bounds.getMinY());
    }

    private static ChunkMapSnapshot capture(ProtoChunk[][] chunks, int firstChunkX,
                                            int firstChunkZ, LevelHeightAccessor bounds,
                                            BiomeManager biomes, AtomicBoolean cancelled) {
        int startX = firstChunkX * 16;
        int startZ = firstChunkZ * 16;
        int width = chunks.length * 16;
        int highest = Integer.MIN_VALUE;
        for (int z = 0; z < width; z++) {
            for (int x = 0; x < width; x++) {
                ProtoChunk chunk = chunks[z / 16][x / 16];
                int surface = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, startX + x, startZ + z);
                highest = Math.max(highest, surface);
            }
        }
        int minY = bounds.getMinY();
        int maxY = Math.min(bounds.getMaxY() + 1, highest + 4);
        ChunkMapSnapshot.Builder snapshot = ChunkMapSnapshot.builder(startX, startZ,
                width, width, 1, minY, maxY);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int z = 0; z < width; z++) {
            if (cancelled.get()) throw new CancellationException();
            for (int x = 0; x < width; x++) {
                ProtoChunk chunk = chunks[z / 16][x / 16];
                var previous = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
                int start = minY;
                for (int y = minY; y <= maxY; y++) {
                    var state = y == maxY ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                            : chunk.getBlockState(pos.set(startX + x, y, startZ + z));
                    if (state == previous) continue;
                    if (!previous.isAir()) snapshot.addRun(x, z, start, y, previous);
                    previous = state;
                    start = y;
                }
                snapshot.setBiome(x, z, biomes.getBiome(pos.set(startX + x, 64, startZ + z)));
            }
        }
        return snapshot.build();
    }

    private static void applyFeatures(ProtoChunk[][] chunks, int firstChunkX, int firstChunkZ,
                                      BiomeManager biomes, TerrainPack pack,
                                      NoiseBasedChunkGenerator generator, RandomState randomState,
                                      LevelHeightAccessor bounds, boolean landmarks) {
        for (ProtoChunk[] row : chunks) {
            for (ProtoChunk chunk : row) {
                Heightmap.primeHeightmaps(chunk, ChunkStatus.FEATURES.heightmapsAfter());
                chunk.setPersistedStatus(ChunkStatus.FEATURES);
            }
        }
        PreviewArea area = new PreviewArea(chunks, firstChunkX * 16, firstChunkZ * 16,
                biomes, pack, generator, randomState, bounds);
        if (generator.generatorSettings().value().defaultFluid().isAir()) {
            SkyLandformDensity sky = SkyLandformDensity.from(randomState);
            for (int z = 0; z < chunks.length; z++) for (int x = 0; x < chunks.length; x++)
                SkySurfaceWaterFeature.place(area, new BlockPos((firstChunkX + x) * 16, 0,
                        (firstChunkZ + z) * 16), sky);
            return;
        }
        int fallsAttempted = 0;
        int fallsPlaced = 0;
        int islandsPlaced = 0;
        IcyLandformDensity mainland = IcyLandformDensity.from(randomState);
        for (int z = 1; z < chunks.length - 1; z++) for (int x = 1; x < chunks.length - 1; x++) {
            fallsAttempted++;
            if (FrozenFjordFallsFeature.place(area,
                    new BlockPos((firstChunkX + x) * 16, 64, (firstChunkZ + z) * 16), mainland, landmarks)) fallsPlaced++;
        }
        if (landmarks) {
            for (int z = 1; z < chunks.length - 1; z++) {
                for (int x = 1; x < chunks.length - 1; x++) {
                    int chunkX = firstChunkX + x;
                    int chunkZ = firstChunkZ + z;
                    RandomSource random = RandomSource.create(pack.seed()
                            ^ chunkX * 341873128712L ^ chunkZ * 132897987541L);
                    if (random.nextInt(64) == 0) {
                        int islandX = chunkX * 16 + random.nextInt(16);
                        int islandZ = chunkZ * 16 + random.nextInt(16);
                        int islandY = area.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                islandX, islandZ);
                        if (CrystalLakeIslandFeature.place(area,
                                new BlockPos(islandX, islandY, islandZ), random)) islandsPlaced++;
                    }
                }
            }
        }
        if (landmarks) com.kltyton.eden_realm.EdenRealm.LOGGER.debug(
                "Terrain preview landmarks: falls {}/{} islands {}",
                fallsPlaced, fallsAttempted, islandsPlaced);
        for (int z = 0; z < chunks.length; z++) {
            for (int x = 0; x < chunks.length; x++) {
                IcyShoreFreezeFeature.place(area,
                        new BlockPos((firstChunkX + x) * 16, 0, (firstChunkZ + z) * 16));
            }
        }
    }

    private static final class PreviewArea implements IcyFeatureArea {
        private final ProtoChunk[][] chunks;
        private final int startX;
        private final int startZ;
        private final BiomeManager biomes;
        private final TerrainPack pack;
        private final NoiseBasedChunkGenerator generator;
        private final RandomState randomState;
        private final LevelHeightAccessor bounds;

        private PreviewArea(ProtoChunk[][] chunks, int startX, int startZ,
                            BiomeManager biomes, TerrainPack pack,
                            NoiseBasedChunkGenerator generator, RandomState randomState,
                            LevelHeightAccessor bounds) {
            this.chunks = chunks;
            this.startX = startX;
            this.startZ = startZ;
            this.biomes = biomes;
            this.pack = pack;
            this.generator = generator;
            this.randomState = randomState;
            this.bounds = bounds;
        }

        private ProtoChunk chunk(int x, int z) {
            int localX = Math.floorDiv(x - startX, 16);
            int localZ = Math.floorDiv(z - startZ, 16);
            return localX < 0 || localX >= chunks.length || localZ < 0 || localZ >= chunks.length
                    ? null : chunks[localZ][localX];
        }

        @Override
        public int getHeight(Heightmap.Types type, int x, int z) {
            ProtoChunk chunk = chunk(x, z);
            return chunk == null ? generator.getBaseHeight(x, z, type, bounds, randomState)
                    : chunk.getHeight(type, x, z) + 1;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            ProtoChunk chunk = chunk(pos.getX(), pos.getZ());
            return chunk == null ? Blocks.AIR.defaultBlockState() : chunk.getBlockState(pos);
        }

        @Override
        public void setBlock(BlockPos pos, BlockState state, int flags) {
            ProtoChunk chunk = chunk(pos.getX(), pos.getZ());
            if (chunk != null) chunk.setBlockState(pos, state, flags);
        }

        @Override
        public Holder<Biome> getBiome(BlockPos pos) {
            return biomes.getBiome(pos);
        }

        @Override
        public TerrainProfile profile(Holder<Biome> biome) {
            String id = biome.unwrapKey().map(key -> key.identifier().getPath())
                    .orElse("icy_rolling_hills");
            return pack.biomes().getOrDefault(id, TerrainProfile.official(id));
        }

        @Override
        public boolean shouldSnow(Holder<Biome> biome, BlockPos pos) {
            return biome.value().getPrecipitationAt(pos, 63) == Biome.Precipitation.SNOW
                    && (getBlockState(pos).isAir() || getBlockState(pos).is(Blocks.SNOW))
                    && getBlockState(pos.below()).blocksMotion();
        }

        @Override
        public boolean isSolidShore(BlockState state, BlockPos pos) {
            return state.blocksMotion();
        }
    }
}
