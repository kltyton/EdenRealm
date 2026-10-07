package com.kltyton.eden_realm.world.terrain;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.data.worldgen.IcyBiomeWorldgen;
import com.kltyton.eden_realm.registry.content.block.ERTerrainBlocks;
import io.github.kltyton.kltytonui.chunkmap.ChunkMapSnapshot;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

/** Current density sampled for a responsive overview; native chunks provide local detail. */
public final class TerrainQuickPreview {
    public record Sample(ChunkMapSnapshot snapshot, BlockPos center) { }

    private TerrainQuickPreview() { }

    public static CompletableFuture<Sample> sample(TerrainPreviewField session, String biomeId,
                                                  TerrainPack pack, int chunks, BlockPos knownCenter, int resolution,
                                                  BooleanSupplier cancelled) {
        if (SkyLandformDensity.BIOMES.contains(biomeId)) return session.prepareSky(pack)
                .thenApplyAsync(field -> SkyTerrainPreview.create(session, field, biomeId, pack, chunks,
                        knownCenter, resolution, cancelled));
        return session.prepare(pack).thenApplyAsync(field -> create(session, field, biomeId, pack,
                chunks, knownCenter, resolution, cancelled));
    }

    public static Sample sampleReady(TerrainPreviewField session, String biomeId, TerrainPack pack,
                                     int chunks, BlockPos center, int resolution) {
        if (SkyLandformDensity.BIOMES.contains(biomeId)) {
            SkyLandformDensity field = session.readySky(pack);
            return field == null ? null : SkyTerrainPreview.create(session, field, biomeId, pack, chunks,
                    center, resolution, () -> false);
        }
        IcyLandformDensity field = session.ready(pack);
        if (field == null) return null;
        if (center == null) {
            TerrainProfile profile = pack.biomes().get(biomeId);
            IcyLandformDensity locator = profile.generationChancePercent() == 0
                    ? session.ready(pack.withProfile(biomeId, profile.withGenerationChance(100))) : field;
            center = locator.findReferenceCenter(biomeId, BlockPos.ZERO).orElse(null);
            if (center == null) return null;
        }
        return create(session, field, biomeId, pack, chunks, center, resolution, () -> false);
    }

    private static Sample create(TerrainPreviewField session, IcyLandformDensity field, String biomeId, TerrainPack pack,
                                  int chunks, BlockPos knownCenter, int resolution, BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean()) throw new CancellationException();
        TerrainProfile profile = pack.biomes().get(biomeId);
        BlockPos center = knownCenter;
        if (center == null) {
            TerrainPack sampling = profile.generationChancePercent() == 0
                    ? pack.withProfile(biomeId, profile.withGenerationChance(100)) : pack;
            IcyLandformDensity locatorField = sampling == pack ? field : session.prepare(sampling).join();
            center = locatorField.findReferenceCenter(biomeId, BlockPos.ZERO).orElse(null);
            if (center == null) {
                NoiseBasedChunkGenerator locator = (NoiseBasedChunkGenerator) IcyBiomeWorldgen.tunedDimension(session.registries, sampling).generator();
                RandomState locatorRandom = RandomState.create(locator.generatorSettings().value(),
                        session.registries.lookupOrThrow(Registries.NOISE), pack.seed());
                var target = ResourceKey.create(Registries.BIOME, ERConstants.id(biomeId));
                var found = locator.getBiomeSource().findBiomeHorizontal(0, 96, 0, 8192, 16,
                        holder -> holder.is(target), RandomSource.create(pack.seed()), true, locatorRandom.sampler());
                if (found == null) throw new IllegalArgumentException("Biome not found: " + biomeId);
                center = TerrainPreviewChunks.representativeCenter(locator, locatorRandom, target, biomeId, found.getFirst());
            }
        }
        long extent = chunks * 16L;
        int scale = (int) Math.max(1, Math.ceilDiv(extent, resolution));
        int heightStep = resolution <= 64 ? 1 : Math.min(scale, 4);
        int side = (int) Math.ceilDiv(extent, scale);
        int originX = (int) Math.floorDiv(Math.floorDiv(center.getX(), 16) * 16L - extent / 2, scale);
        int originZ = (int) Math.floorDiv(Math.floorDiv(center.getZ(), 16) * 16L - extent / 2, scale);
        int sea = Math.ceilDiv(session.settings.seaLevel(), heightStep);
        int[] heights = new int[side * side];
        int[] waterLevels = new int[heights.length];
        @SuppressWarnings("unchecked") Holder<Biome>[] biomes = (Holder<Biome>[]) new Holder<?>[heights.length];
        var holders = session.biomes;
        var bounds = session.settings.noiseSettings();
        int workers = side >= 128 ? Math.clamp(Runtime.getRuntime().availableProcessors() / 2, 1, 8) : 1;
        int rows = Math.ceilDiv(side, workers);
        List<Map<Long, IcyLandformDensity.Sample>> lattices = new ArrayList<>(workers);
        List<CompletableFuture<Void>> tasks = new ArrayList<>(workers);
        for (int worker = 0; worker < workers; worker++) {
            int first = worker * rows;
            int last = Math.min(side, first + rows);
            Map<Long, IcyLandformDensity.Sample> lattice = new HashMap<>();
            lattices.add(lattice);
            Runnable sampleRows = () -> {
                for (int z = first; z < last; z++) {
                    if (cancelled.getAsBoolean()) throw new CancellationException();
                    for (int x = 0; x < side; x++) {
                        int worldX = coordinate(originX + x, scale);
                        int worldZ = coordinate(originZ + z, scale);
                        int height = (int)Math.ceil(surface(field, lattice, worldX, worldZ));
                        height = Math.clamp(height, bounds.minY() + 1, bounds.minY() + bounds.height());
                        heights[z * side + x] = Math.floorDiv(height - 1, heightStep) + 1;
                        var column = value(field, lattice, worldX, worldZ);
                        waterLevels[z * side + x] = column.frozenFall() ? sea : Math.max(sea,
                                Math.ceilDiv(column.waterLevel(), heightStep));
                        int qx = QuartPos.toBlock(QuartPos.fromBlock(worldX));
                        int qz = QuartPos.toBlock(QuartPos.fromBlock(worldZ));
                        biomes[z * side + x] = holders.get(value(field, lattice, qx, qz).biome());
                    }
                }
            };
            if (workers == 1) sampleRows.run();
            else tasks.add(CompletableFuture.runAsync(sampleRows, Util.backgroundExecutor().forName("icy_preview_height")));
        }
        CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new)).join();
        int lowest = java.util.Arrays.stream(heights).min().orElseThrow();
        int highest = Math.max(java.util.Arrays.stream(waterLevels).max().orElseThrow(),
                java.util.Arrays.stream(heights).max().orElseThrow() + 1);
        int bottom = lowest - 3;
        ChunkMapSnapshot.Builder snapshot = ChunkMapSnapshot.builder(originX, originZ, side, side,
                scale, bottom, highest + 2).verticalStep(heightStep);
        BlockState grass = ERTerrainBlocks.EDEN_GRASS_BLOCK.get().defaultBlockState().setValue(SnowyBlock.SNOWY, false);
        BlockPos.MutableBlockPos snowPos = new BlockPos.MutableBlockPos();
        for (int z = 0; z < side; z++) {
            if (cancelled.getAsBoolean()) throw new CancellationException();
            Map<Long, IcyLandformDensity.Sample> lattice = lattices.get(z / rows);
            for (int x = 0; x < side; x++) {
                int top = heights[z * side + x];
                int water = waterLevels[z * side + x];
                snapshot.addRun(x, z, bottom, top - 1, Blocks.STONE.defaultBlockState());
                snapshot.addRun(x, z, top - 1, top, top >= water ? grass : Blocks.GRAVEL.defaultBlockState());
                if (top < water) {
                    int worldX = coordinate(originX + x, scale);
                    int worldZ = coordinate(originZ + z, scale);
                    String id = biomes[z * side + x].unwrapKey().orElseThrow().identifier().getPath();
                    int radius = pack.biomes().get(id).shoreIceBlocks();
                    boolean frozen = water * heightStep - top * heightStep <= 16
                            && nearShore(field, lattice, worldX, worldZ, water * heightStep, radius, scale);
                    int waterTop = frozen ? water - 1 : water;
                    if (top < waterTop) snapshot.addRun(x, z, top, waterTop, Blocks.WATER.defaultBlockState());
                    if (frozen) snapshot.addRun(x, z, water - 1, water, Blocks.ICE.defaultBlockState());
                }
                else if (biomes[z * side + x].value().getPrecipitationAt(snowPos.set(
                        coordinate(originX + x, scale), top * heightStep, coordinate(originZ + z, scale)),
                        session.seaLevel()) == Biome.Precipitation.SNOW) {
                    snapshot.addRun(x, z, top, top + 1, Blocks.SNOW.defaultBlockState());
                }
                int worldX = coordinate(originX + x, scale), worldZ = coordinate(originZ + z, scale);
                var column = value(field, lattice, worldX, worldZ);
                if (column.frozenFall()) {
                    int cliff = (int) Math.ceil(surface(field, lattice,
                            worldX + column.fallDx(), worldZ + column.fallDz()));
                    int iceTop = Math.floorDiv(Math.min(cliff, column.waterLevel()) - 1, heightStep) + 1;
                    if (iceTop > top) snapshot.addRun(x, z, top, iceTop,
                            com.kltyton.eden_realm.world.feature.FrozenFjordFallsFeature.fallBlock(worldX, top * heightStep, worldZ));
                }
                snapshot.setBiome(x, z, biomes[z * side + x], coordinate(originX + x, scale), coordinate(originZ + z, scale));
            }
        }
        return new Sample(snapshot.build(), center);
    }

    private static int coordinate(int cell, int scale) {
        return (int) Math.clamp((long) cell * scale, Integer.MIN_VALUE + 4L, Integer.MAX_VALUE - 4L);
    }

    private static boolean nearShore(IcyLandformDensity density, Map<Long, IcyLandformDensity.Sample> lattice,
                                      int x, int z, int sea, int radius, int scale) {
        if (radius == 0 || scale > radius * 2) return false;
        int stride = Math.min(4, radius);
        for (int dz = -radius; dz <= radius; dz += stride) {
            for (int dx = -radius; dx <= radius; dx += stride) {
                if (dx * dx + dz * dz <= radius * radius
                        && surface(density, lattice, x + dx, z + dz) >= sea) return true;
            }
        }
        return false;
    }

    private static double surface(IcyLandformDensity field, Map<Long, IcyLandformDensity.Sample> lattice, int x, int z) {
        int x0 = Math.floorDiv(x, 4) * 4;
        int z0 = Math.floorDiv(z, 4) * 4;
        double fx = (x - x0) * 0.25;
        double fz = (z - z0) * 0.25;
        double a = value(field, lattice, x0, z0).height();
        if (fx == 0 && fz == 0) return a;
        double b = fx == 0 ? a : value(field, lattice, x0 + 4, z0).height();
        if (fz == 0) return a + fx * (b - a);
        double c = value(field, lattice, x0, z0 + 4).height();
        double d = fx == 0 ? c : value(field, lattice, x0 + 4, z0 + 4).height();
        return (a + fx * (b - a)) + fz * ((c + fx * (d - c)) - (a + fx * (b - a)));
    }

    private static IcyLandformDensity.Sample value(IcyLandformDensity field, Map<Long, IcyLandformDensity.Sample> lattice, int x, int z) {
        long key = ((long)x << 32) | (z & 0xffffffffL);
        return lattice.computeIfAbsent(key, ignored -> field.sample(x, z));
    }
}
