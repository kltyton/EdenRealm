package com.kltyton.eden_realm.client.world;

import com.kltyton.eden_realm.EdenRealm;
import com.kltyton.eden_realm.config.ERClientConfig;
import com.kltyton.eden_realm.client.world.preview.TerrainLiveSurface;
import com.kltyton.eden_realm.client.world.preview.TerrainBlockSurface;
import com.kltyton.eden_realm.client.world.preview.render.BlockSurfaceRenderer;
import com.kltyton.eden_realm.client.world.preview.procedural.ProceduralPreview;
import com.kltyton.eden_realm.client.world.preview.render.NativeTerrainScene;
import com.kltyton.eden_realm.world.terrain.TerrainPack;
import com.kltyton.eden_realm.world.terrain.TerrainProfile;
import com.kltyton.eden_realm.world.terrain.TerrainPreviewChunks;
import com.kltyton.eden_realm.world.terrain.TerrainQuickPreview;
import com.kltyton.eden_realm.world.terrain.TerrainPreviewField;
import com.kltyton.eden_realm.world.terrain.TerrainRealtimePreview;
import net.minecraft.core.BlockPos;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;

/** Client-thread editor state; noise sampling and mesh building run on shared workers. */
public final class TerrainEditorController {
    private record LiveKey(TerrainPack pack, String biome, int chunks, BlockPos center) { }
    private record CachedLive(BlockPos center, NativeTerrainScene.Surface surface, String digest) { }
    private record SurfaceCache(net.minecraft.client.renderer.block.BlockStateModelSet models,
                                java.util.LinkedHashMap<LiveKey, CachedLive> entries) { }
    private static final Map<net.minecraft.core.RegistryAccess.Frozen, SurfaceCache> LIVE_CACHE = new java.util.WeakHashMap<>();
    private final CreateWorldScreen createWorld;
    private final Minecraft minecraft;
    private TerrainPack pack;
    private ProceduralPreview previewRenderer;
    private String selectedBiome;
    private TerrainPreviewChunks.PreviewRequest request;
    private ProceduralPreview.Frame nativeFrame;
    private ProceduralPreview.Frame overviewFrame;
    private Throwable error;
    private int idleTicks;
    private boolean quickDirty;
    private CompletableFuture<?> quick;
    private AtomicBoolean quickCancellation;
    private final Map<String, BlockPos> centers = new HashMap<>();
    private long centerEpoch;
    private long previewEpoch;
    private BlockPos liveCenter;
    private long fullGeneration = -1;
    private long shownGeneration = -1;
    private boolean detailed;
    private CompletableFuture<?> refinement;
    private AtomicBoolean refinementCancellation;
    private long generation;
    private long version;
    private boolean closed;
    private long previewSeed;
    private TerrainPreviewField previewField;
    private TerrainRealtimePreview realtimePreview;
    private TerrainRealtimePreview.Frame realtimeFrame;
    private CompletableFuture<?> realtimeWork;
    private AtomicBoolean realtimeCancellation;
    private long realtimeRequestedGeneration = -1;
    private net.minecraft.core.RegistryAccess.Frozen previewRegistries;
    private int preciseChunks;
    private record ViewRegion(int chunkX, int chunkZ, int chunks, boolean blocks) { }
    private ViewRegion viewRegion;
    private CompletableFuture<?> viewWork;
    private TerrainPreviewChunks.PreviewRequest viewChunks;
    private AtomicBoolean viewCancellation;
    private int viewIdleTicks;
    private boolean viewPrepared;
    private boolean viewDetailed;
    private int previewChunks = TerrainPreviewChunks.DEFAULT_CHUNKS;
    private long liveCacheHits;
    private double liveComputeMillis;
    private TerrainBlockSurface.Job wholeWork;
    private BlockSurfaceRenderer.Surface wholeSurface;
    private long wholeGeneration = -1;
    private record BlockKey(TerrainPack pack, String biome, int chunks, BlockPos center) { }
    private record BlockCache(net.minecraft.client.renderer.block.BlockStateModelSet models,
                              java.util.LinkedHashMap<BlockKey, BlockSurfaceRenderer.Surface> entries) { }
    private static final Map<net.minecraft.core.RegistryAccess.Frozen, BlockCache> BLOCK_CACHE = new java.util.WeakHashMap<>();

    public TerrainEditorController(CreateWorldScreen createWorld) {
        this.createWorld = createWorld;
        this.minecraft = Minecraft.getInstance();
        this.pack = TerrainWorldCreation.draft(createWorld);
        this.previewRenderer = new ProceduralPreview(
                minecraft.gameDirectory.toPath().resolve("cache/eden_realm/chunk-preview"));
        this.selectedBiome = TerrainWorldCreation.draft(createWorld).biomes().keySet().stream()
                .sorted().findFirst().orElseThrow();
        previewSeed = ERClientConfig.TERRAIN_PREVIEW_SEED.get();
        previewRegistries = createWorld.getUiState().getSettings().worldgenLoadContext();
        previewField = new TerrainPreviewField(previewRegistries, previewSeed);
        realtimePreview = new TerrainRealtimePreview(previewField);
        schedulePreview();
    }

    public TerrainPack pack() {
        return pack;
    }

    public String selectedBiome() {
        return selectedBiome;
    }

    public ProceduralPreview.Frame nativeFrame() { return nativeFrame; }
    public BlockPos previewCenter() { return liveCenter; }
    public long nativeCacheHits() { return previewRenderer.cacheHits(); }
    public long nativeCacheMisses() { return previewRenderer.cacheMisses(); }
    public long nativeBakedTiles() { return previewRenderer.bakedTiles(); }
    public long previewSeed() { return previewSeed; }
    public long liveCacheHits() { return liveCacheHits; }
    public double liveComputeMillis() { return liveComputeMillis; }
    public TerrainRealtimePreview.Frame realtimeFrame() {
        return com.kltyton.eden_realm.world.terrain.SkyLandformDensity.BIOMES.contains(selectedBiome) ? null : realtimeFrame;
    }
    public int previewSeaLevel() {
        return com.kltyton.eden_realm.world.terrain.SkyLandformDensity.BIOMES.contains(selectedBiome) ? -64 : previewField.seaLevel();
    }
    public boolean wholePrecisionSupported() { return previewChunks <= 256; }
    public BlockSurfaceRenderer.Surface wholeSurface() { return wholeSurface; }
    public double wholeProgress() { return wholeSurface != null ? 1 : wholeWork == null ? 0 : wholeWork.completedRegions() / (double) wholeWork.totalRegions(); }

    public int previewChunks() {
        return previewChunks;
    }

    public void setPreviewChunks(int chunks) {
        if (chunks < TerrainPreviewChunks.MIN_CHUNKS || (chunks & 1) != 0)
            throw new IllegalArgumentException("Invalid preview chunk count: " + chunks);
        if (previewChunks == chunks) return;
        if (quickCancellation != null) quickCancellation.set(true);
        previewChunks = chunks;
        schedulePreview();
    }

    public Throwable error() {
        return error;
    }

    public long version() {
        return version;
    }

    public boolean loading() {
        return quickDirty || quick != null || refinement != null || request != null || wholeWork != null
                || error == null && fullGeneration != generation;
    }

    public boolean detailed() { return detailed; }
    public boolean preciseReady() { return preciseChunks > 0; }
    public int preciseChunks() { return preciseChunks; }
    public boolean macro() { return previewChunks > TerrainPreviewChunks.MAX_DETAIL_CHUNKS; }
    public long generation() { return generation; }
    public boolean detailLoading() { return viewRegion != null && (!viewPrepared || viewRegion.blocks() && !viewDetailed); }
    public boolean detailPrepared() { return viewRegion != null && viewRegion.blocks() && viewPrepared; }

    public void view(long sceneKey, double x, double z, double span) {
        if (closed || sceneKey != generation || !Double.isFinite(x) || !Double.isFinite(z)
                || !Double.isFinite(span) || span <= 0) return;
        if (wholePrecisionSupported() && (wholeWork != null || wholeSurface != null
                && x >= wholeSurface.x() && z >= wholeSurface.z() && x < wholeSurface.x() + wholeSurface.width()
                && z < wholeSurface.z() + wholeSurface.depth())) {
            if (viewRegion != null) { cancelView(); viewRegion = null; version++; }
            return;
        }
        if (span >= previewChunks * 16.0 * 0.8) {
            if (viewRegion != null) {
                cancelView(); viewRegion = null;
                restoreOverview();
                version++;
            }
            return;
        }
        int chunks = (int) Math.max(TerrainPreviewChunks.MIN_CHUNKS, Math.ceil(span * 1.5 / 32.0) * 2.0);
        ViewRegion region = new ViewRegion((int) Math.floor(Math.clamp(x, -30_000_000.0, 30_000_000.0) / 16.0),
                (int) Math.floor(Math.clamp(z, -30_000_000.0, 30_000_000.0) / 16.0), chunks,
                 chunks <= TerrainPreviewChunks.MAX_DETAIL_CHUNKS);
        if (viewRegion != null && region.blocks() == viewRegion.blocks()
                && region.chunks() <= viewRegion.chunks()
                && region.chunks() * 2L >= viewRegion.chunks()
                && Math.abs(region.chunkX() - viewRegion.chunkX()) <= Math.max(1, viewRegion.chunks() / 4)
                && Math.abs(region.chunkZ() - viewRegion.chunkZ()) <= Math.max(1, viewRegion.chunks() / 4)) return;
        int coveredChunks = (int) Math.min(region.blocks() ? TerrainPreviewChunks.MAX_DETAIL_CHUNKS : Integer.MAX_VALUE - 1L,
                region.blocks() ? chunks * 2L : chunks);
        region = new ViewRegion(region.chunkX(), region.chunkZ(), coveredChunks,
                coveredChunks <= TerrainPreviewChunks.MAX_DETAIL_CHUNKS);
        cancelView();
        viewRegion = region;
        viewIdleTicks = 0;
        quickDirty = true;
        version++;
    }

    private void cancelView() {
        if (viewCancellation != null) viewCancellation.set(true);
        if (viewChunks != null) viewChunks.cancel();
        viewChunks = null;
        viewPrepared = false;
        viewDetailed = false;
    }

    private void restoreOverview() {
        if (overviewFrame == null || overviewFrame.sceneKey() != generation) {
            fullGeneration = -1;
            detailed = false;
            quickDirty = true;
            return;
        }
        nativeFrame = overviewFrame;
        detailed = overviewFrame.surface().width() >= 512 && overviewFrame.precise() != null;
        fullGeneration = detailed ? generation : -1;
    }

    private void startViewSurface() {
        long current = generation;
        ViewRegion region = viewRegion;
        AtomicBoolean cancelled = new AtomicBoolean();
        viewCancellation = cancelled;
        BlockPos center = new BlockPos(region.chunkX() * 16, 96, region.chunkZ() * 16);
        viewWork = TerrainQuickPreview.sample(previewField, selectedBiome,
                pack().withSeed(previewSeed), region.chunks(), center, 512, cancelled::get)
                .thenCompose(sample -> previewRenderer.renderSurface(sample.snapshot(), current, cancelled::get))
                .whenComplete((ready, failure) -> minecraft.execute(() -> {
                    viewWork = null;
                    if (closed || cancelled.get() || current != generation || !region.equals(viewRegion)) return;
                    if (failure == null) {
                        nativeFrame = ready;
                        viewPrepared = true;
                        fullGeneration = current;
                        detailed = true;
                    } else {
                        error = failure;
                        EdenRealm.LOGGER.error("Camera terrain surface failed", failure);
                    }
                    version++;
                }));
    }

    private void startViewBlocks() {
        long current = generation;
        ViewRegion region = viewRegion;
        var context = createWorld.getUiState().getSettings();
        AtomicBoolean cancelled = new AtomicBoolean();
        viewCancellation = cancelled;
        viewChunks = TerrainPreviewChunks.generate(context.worldgenLoadContext(), selectedBiome,
                pack().withSeed(previewSeed), region.chunks(),
                new BlockPos(region.chunkX() * 16, 96, region.chunkZ() * 16), cancelled);
        viewWork = viewChunks.complete().thenCompose(area -> previewRenderer.render(area, current, cancelled::get))
                .whenComplete((complete, failure) -> minecraft.execute(() -> {
                    viewWork = null;
                    if (closed || cancelled.get() || current != generation || !region.equals(viewRegion)) return;
                    viewChunks = null;
                    if (failure != null) {
                        error = failure;
                        EdenRealm.LOGGER.error("Block terrain detail failed", failure);
                    } else {
                        nativeFrame = complete;
                        viewPrepared = true;
                        viewDetailed = true;
                        preciseChunks = region.chunks();
                        fullGeneration = current;
                        detailed = true;
                    }
                    version++;
                }));
    }

    public void select(String biomeId) {
        if (!pack().biomes().containsKey(biomeId)) throw new IllegalArgumentException("Unknown Eden biome: " + biomeId);
        if (quickCancellation != null) quickCancellation.set(true);
        selectedBiome = biomeId;
        schedulePreview();
    }

    public void update(TerrainProfile profile) {
        TerrainProfile previous = pack().biomes().get(selectedBiome);
        if (previous.equals(profile)) return;
        TerrainPack updated = pack().withProfile(selectedBiome, profile);
        var domain = com.kltyton.eden_realm.world.terrain.SkyLandformDensity.BIOMES.contains(selectedBiome)
                ? com.kltyton.eden_realm.world.terrain.SkyLandformDensity.BIOMES
                : com.kltyton.eden_realm.world.terrain.IcyLandformDensity.BIOMES;
        if (domain.stream().noneMatch(id -> updated.biomes().get(id).generationChancePercent() > 0)) {
            throw new IllegalArgumentException("At least one biome must remain enabled");
        }
        pack = updated;
        boolean distributionChanged = previous.generationChancePercent() != profile.generationChancePercent()
                || previous.biomeSizePercent() != profile.biomeSizePercent();
        if (distributionChanged) invalidateCenters();
        schedulePreview(!distributionChanged);
    }

    public void reset() {
        TerrainProfile previous = pack().biomes().get(selectedBiome);
        pack = pack.reset(selectedBiome);
        TerrainProfile reset = pack().biomes().get(selectedBiome);
        boolean distributionChanged = previous.generationChancePercent() != reset.generationChancePercent()
                || previous.biomeSizePercent() != reset.biomeSizePercent();
        if (distributionChanged) invalidateCenters();
        schedulePreview(!distributionChanged);
    }

    public void tick() {
        if (closed) return;
        long configuredSeed = ERClientConfig.TERRAIN_PREVIEW_SEED.get();
        if (configuredSeed != previewSeed) {
            previewSeed = configuredSeed;
            previewField = new TerrainPreviewField(previewRegistries, previewSeed);
            resetRealtime();
            invalidateCenters();
            nativeFrame = null;
            schedulePreview();
        }
        if (!previewRenderer.resourcesCurrent()) {
            schedulePreview();
            previewRenderer.close();
            previewRenderer = new ProceduralPreview(
                    minecraft.gameDirectory.toPath().resolve("cache/eden_realm/chunk-preview"));
            nativeFrame = null;
        }
        var registries = createWorld.getUiState().getSettings().worldgenLoadContext();
        if (registries != previewRegistries) {
            nativeFrame = null;
            previewRegistries = registries;
            previewField = new TerrainPreviewField(registries, previewSeed);
            resetRealtime();
            invalidateCenters();
            schedulePreview();
        }
        idleTicks++;
        viewIdleTicks++;
        frame();
        if (idleTicks >= 6 && !quickDirty && quick == null && wholePrecisionSupported() && wholeGeneration != generation) startWhole();
        if (idleTicks >= 6 && viewIdleTicks >= 3 && viewRegion != null && viewWork == null
                && !quickDirty && quick == null && refinement == null && !viewPrepared) {
            if (viewRegion.blocks()) startViewBlocks();
            else startViewSurface();
        }
        if (idleTicks >= 6 && viewWork == null && viewRegion == null && !quickDirty && quick == null
                && refinement == null && !wholePrecisionSupported() && fullGeneration != generation) startFull();
    }

    private record QuickRendered(BlockPos center, ProceduralPreview.Frame frame) { }

    public void frame() {
        if (closed) return;
        if (quickDirty && quick == null) startQuick();
        if (!com.kltyton.eden_realm.world.terrain.SkyLandformDensity.BIOMES.contains(selectedBiome)
                && previewChunks <= TerrainPreviewChunks.MAX_DETAIL_CHUNKS && liveCenter != null && realtimeWork == null
                && realtimeRequestedGeneration != generation) startRealtime();
    }

    private void startRealtime() {
        long current = generation;
        String biome = selectedBiome;
        int chunks = previewChunks;
        var field = previewField;
        BlockPos center = liveCenter.immutable();
        AtomicBoolean cancelled = new AtomicBoolean();
        realtimeCancellation = cancelled;
        realtimeRequestedGeneration = current;
        var work = realtimePreview.sample(pack().withSeed(previewSeed), chunks, center, current, cancelled::get);
        realtimeWork = work;
        work.whenComplete((ready, failure) -> minecraft.execute(() -> {
            if (realtimeWork != work) return;
            realtimeWork = null;
            if (closed || cancelled.get() || previewField != field) return;
            if (failure != null) {
                error = failure;
                EdenRealm.LOGGER.error("Realtime block terrain failed for {}", biome, failure);
            } else if (biome.equals(selectedBiome) && chunks == previewChunks && center.equals(liveCenter)
                    && (realtimeFrame == null || ready.revision() > realtimeFrame.revision())) realtimeFrame = ready;
            version++;
        }));
    }

    private void resetRealtime() {
        if (realtimeCancellation != null) realtimeCancellation.set(true);
        realtimeWork = null; realtimeFrame = null; realtimeRequestedGeneration = -1;
        realtimePreview = new TerrainRealtimePreview(previewField);
    }

    private void startQuick() {
        long started = System.nanoTime();
        long current = generation;
        String biome = selectedBiome;
        long sampledCenterEpoch = centerEpoch;
        long epoch = previewEpoch;
        AtomicBoolean cancelled = new AtomicBoolean();
        quickCancellation = cancelled;
        quickDirty = false;
        TerrainPack snapshot = pack().withSeed(previewSeed);
        ViewRegion region = viewRegion;
        BlockPos center = region == null ? liveCenter : new BlockPos(region.chunkX() * 16, 96, region.chunkZ() * 16);
        int chunks = region == null ? previewChunks : region.chunks();
        LiveKey key = new LiveKey(snapshot, biome, chunks, center);
        var cached = liveCache().entries().get(key);
        if (cached != null) {
            liveCacheHits++;
            var previous = cached.surface();
            var surface = new NativeTerrainScene.Surface(previous.x(), previous.z(), previous.width(), previous.depth(),
                    previous.step(), previous.rgba(), previous.heights(), current);
            installQuick(new QuickRendered(cached.center(), new ProceduralPreview.Frame(current, surface, null,
                    Map.of(), null, cached.digest())), biome, region, sampledCenterEpoch);
            liveComputeMillis = (System.nanoTime() - started) / 1_000_000.0;
            return;
        }
        var immediate = TerrainQuickPreview.sampleReady(previewField, biome, snapshot, chunks, center, 32);
        if (immediate != null) {
            var ready = new QuickRendered(immediate.center(), TerrainLiveSurface.frame(immediate.snapshot(), snapshot, current));
            cacheQuick(key, ready);
            installQuick(ready, biome, region, sampledCenterEpoch);
            liveComputeMillis = (System.nanoTime() - started) / 1_000_000.0;
            return;
        }
        quick = TerrainQuickPreview.sample(previewField, biome, snapshot,
                chunks, center, 32, cancelled::get)
                .thenApply(sample -> new QuickRendered(sample.center(), TerrainLiveSurface.frame(sample.snapshot(), snapshot, current)))
                .whenComplete((ready, failure) -> minecraft.execute(() -> {
                    quick = null;
                    if (closed || cancelled.get() || epoch != previewEpoch) return;
                    if ((region == null) != (viewRegion == null)) return;
                    if (failure == null) {
                        cacheQuick(key, ready);
                        installQuick(ready, biome, region, sampledCenterEpoch);
                    } else {
                        error = failure;
                        EdenRealm.LOGGER.error("Realtime terrain preview failed for {}", biome, failure);
                    }
                    version++;
                }));
    }

    private SurfaceCache liveCache() {
        var models = minecraft.getModelManager().getBlockStateModelSet();
        SurfaceCache cache = LIVE_CACHE.get(previewRegistries);
        if (cache == null || cache.models() != models) {
            cache = new SurfaceCache(models, new java.util.LinkedHashMap<>(128, 0.75f, true));
            LIVE_CACHE.put(previewRegistries, cache);
        }
        return cache;
    }

    private void cacheQuick(LiveKey key, QuickRendered ready) {
        var cache = liveCache().entries();
        var entry = new CachedLive(ready.center(), ready.frame().surface(), ready.frame().digest());
        cache.put(key, entry);
        if (key.center() == null) cache.put(new LiveKey(key.pack(), key.biome(), key.chunks(), ready.center()), entry);
        while (cache.size() > 128) cache.remove(cache.keySet().iterator().next());
    }

    private void installQuick(QuickRendered ready, String biome, ViewRegion region, long sampledCenterEpoch) {
        if (ready.frame().sceneKey() < shownGeneration) return;
        if (region == null) {
            liveCenter = ready.center();
            if (sampledCenterEpoch == centerEpoch) centers.put(biome, liveCenter);
            overviewFrame = ready.frame();
        }
        shownGeneration = ready.frame().sceneKey();
        nativeFrame = ready.frame();
        detailed = false;
        preciseChunks = 0;
        version++;
    }

    private CompletableFuture<io.github.kltyton.kltytonui.chunkmap.ChunkMapSnapshot> preciseSnapshot(
            net.minecraft.core.RegistryAccess.Frozen registries, String biome, TerrainPack pack,
            BlockPos center, AtomicBoolean cancelled) {
        if (cancelled.get()) return CompletableFuture.failedFuture(new java.util.concurrent.CancellationException());
        var nativeRequest = TerrainPreviewChunks.generate(registries, biome, pack, TerrainPreviewChunks.MIN_CHUNKS, center, cancelled);
        return nativeRequest.complete();
    }

    private void startFull() {
        long current = generation;
        fullGeneration = current;
        var context = createWorld.getUiState().getSettings();
        if (macro()) {
            AtomicBoolean cancelled = new AtomicBoolean();
            refinementCancellation = cancelled;
            String biome = selectedBiome;
            TerrainPack snapshot = pack().withSeed(previewSeed);
            refinement = TerrainQuickPreview.sample(previewField, biome,
                    snapshot, previewChunks, centers.get(biome), 512, cancelled::get)
                    .thenCompose(sample -> preciseSnapshot(context.worldgenLoadContext(), biome, snapshot, sample.center(), cancelled)
                            .thenCompose(precise -> previewRenderer.renderWithDetail(sample.snapshot(), precise, current, cancelled::get)))
                    .whenComplete((ready, failure) -> minecraft.execute(() -> {
                        refinement = null;
                        if (closed || cancelled.get() || current != generation) return;
                        error = failure;
                        if (failure == null) { nativeFrame = ready; overviewFrame = ready; shownGeneration = current; detailed = true;
                            preciseChunks = TerrainPreviewChunks.MIN_CHUNKS; }
                        else EdenRealm.LOGGER.error("Macro terrain preview failed", failure);
                        version++;
                    }));
            return;
        }
        request = TerrainPreviewChunks.generate(context.worldgenLoadContext(), selectedBiome,
                pack().withSeed(previewSeed), previewChunks, centers.get(selectedBiome));
        var active = request;
        active.complete().thenCompose(snapshot -> previewRenderer.render(snapshot, current, active.cancelled()::get))
                .whenComplete((ready, failure) -> minecraft.execute(() -> {
                    if (closed || current != generation) return;
                    request = null;
                    error = failure;
                    if (failure == null) {
                        nativeFrame = ready;
                        overviewFrame = ready;
                        shownGeneration = current;
                        detailed = true;
                        preciseChunks = previewChunks;
                    } else EdenRealm.LOGGER.error("Detailed terrain preview failed for {}", selectedBiome, failure);
                    version++;
                }));
    }

    private void startWhole() {
        long current = generation;
        wholeGeneration = current;
        TerrainPack snapshot = pack().withSeed(previewSeed);
        BlockKey key = new BlockKey(snapshot, selectedBiome, previewChunks, liveCenter);
        var models = minecraft.getModelManager().getBlockStateModelSet();
        BlockCache cache = BLOCK_CACHE.get(previewRegistries);
        if (cache == null || cache.models() != models) {
            cache = new BlockCache(models, new java.util.LinkedHashMap<>(2, 0.75f, true));
            BLOCK_CACHE.put(previewRegistries, cache);
        }
        var saved = cache.entries().get(key);
        if (saved != null) {
            wholeSurface = new BlockSurfaceRenderer.Surface(current, saved.x(), saved.z(), saved.width(), saved.depth(), saved.tiles(), saved.palette());
            fullGeneration = current; detailed = true;
            version++;
            return;
        }
        BlockCache activeCache = cache;
        var context = createWorld.getUiState().getSettings();
        var active = new TerrainBlockSurface.Job(context.worldgenLoadContext(), selectedBiome, snapshot, previewChunks, liveCenter, current);
        wholeWork = active;
        active.result().whenComplete((ready, failure) -> minecraft.execute(() -> {
            if (closed || wholeWork != active || current != generation) return;
            wholeWork = null;
            if (failure == null) {
                wholeSurface = ready;
                fullGeneration = current; detailed = true;
                activeCache.entries().put(key, ready);
                while (activeCache.entries().size() > 2 || activeCache.entries().values().stream().mapToLong(TerrainEditorController::surfaceBytes).sum() > (1L << 30))
                    activeCache.entries().remove(activeCache.entries().keySet().iterator().next());
            } else {
                error = failure;
                EdenRealm.LOGGER.error("Whole block surface failed for {}", selectedBiome, failure);
            }
            version++;
        }));
    }

    private static long surfaceBytes(BlockSurfaceRenderer.Surface surface) {
        return surface.palette().length * 4L + surface.tiles().stream().mapToLong(tile -> 4L * (tile.heights().length + tile.ranges().length + tile.runs().length)).sum();
    }

    public void close() {
        closed = true;
        if (realtimeCancellation != null) realtimeCancellation.set(true);
        realtimeWork = null; realtimeFrame = null;
        if (wholeWork != null) wholeWork.cancel();
        wholeWork = null; wholeSurface = null;
        cancelView();
        if (quickCancellation != null) quickCancellation.set(true);
        if (refinementCancellation != null) refinementCancellation.set(true);
        if (request != null) request.cancel();
        request = null;
        nativeFrame = null;
        overviewFrame = null;
        previewRenderer.close();
    }

    private void invalidateCenters() {
        centers.clear();
        centerEpoch++;
    }

    private void schedulePreview() {
        previewEpoch++;
        liveCenter = centers.get(selectedBiome);
        if (quickCancellation != null) quickCancellation.set(true);
        schedulePreview(false);
    }

    private void schedulePreview(boolean keepView) {
        generation++;
        if (wholeWork != null) wholeWork.cancel();
        wholeWork = null; wholeSurface = null; wholeGeneration = -1;
        overviewFrame = null;
        preciseChunks = 0;
        cancelView();
        if (!keepView) viewRegion = null;
        viewIdleTicks = 0;
        if (refinementCancellation != null) refinementCancellation.set(true);
        if (request != null) request.cancel();
        request = null;
        error = null;
        idleTicks = 0;
        quickDirty = true;
        detailed = false;
        version++;
    }

}
