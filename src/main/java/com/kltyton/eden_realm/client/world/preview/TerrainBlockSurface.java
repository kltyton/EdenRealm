package com.kltyton.eden_realm.client.world.preview;

import com.kltyton.eden_realm.world.terrain.TerrainPack;
import com.kltyton.eden_realm.world.terrain.TerrainPreviewChunks;
import com.kltyton.eden_realm.client.world.preview.render.BlockSurfaceRenderer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.Util;

/** Streams native generation regions into a complete block-resolved surface without retaining every ProtoChunk. */
public final class TerrainBlockSurface {
    private static final int REGION_BLOCKS = 256;
    private TerrainBlockSurface() { }

    public static final class Job {
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicInteger next = new AtomicInteger(), completed = new AtomicInteger();
        private final RegistryAccess.Frozen registries;
        private final String biome;
        private final TerrainPack pack;
        private final int x, z, width;
        private final long revision;
        private final List<BlockPos> regions;
        private final BlockSurfaceRenderer.Capture capture = new BlockSurfaceRenderer.Capture();
        private final List<BlockSurfaceRenderer.Tile> tiles = java.util.Collections.synchronizedList(new ArrayList<>());
        private final CompletableFuture<BlockSurfaceRenderer.Surface> result;

        public Job(RegistryAccess.Frozen registries, String biome, TerrainPack pack, int chunks, BlockPos center, long revision) {
            this.registries = registries; this.biome = biome; this.pack = pack; this.revision = revision;
            width = Math.multiplyExact(chunks, 16);
            x = Math.floorDiv(center.getX(), 16) * 16 - width / 2;
            z = Math.floorDiv(center.getZ(), 16) * 16 - width / 2;
            var coordinates = new ArrayList<BlockPos>();
            for (int dz = 0; dz < width; dz += REGION_BLOCKS) for (int dx = 0; dx < width; dx += REGION_BLOCKS)
                coordinates.add(new BlockPos(x + dx, 0, z + dz));
            coordinates.sort(Comparator.comparingDouble(p -> Math.hypot(p.getX() + REGION_BLOCKS / 2.0 - center.getX(),
                    p.getZ() + REGION_BLOCKS / 2.0 - center.getZ())));
            regions = List.copyOf(coordinates);
            CompletableFuture<Void> generated;
            if (chunks <= TerrainPreviewChunks.MAX_DETAIL_CHUNKS) {
                var contiguous = TerrainPreviewChunks.generate(registries, biome, pack, chunks + 4, center, cancelled);
                generated = contiguous.complete().thenCompose(snapshot -> {
                    java.util.function.Supplier<CompletableFuture<Void>> encode = () -> CompletableFuture.runAsync(() -> {
                        int region;
                        while ((region = next.getAndIncrement()) < regions.size()) {
                            if (cancelled.get()) throw new CancellationException();
                            var origin = regions.get(region);
                            tiles.add(capture.encode(snapshot, origin.getX(), origin.getZ(), Math.min(REGION_BLOCKS, x + width - origin.getX()),
                                    Math.min(REGION_BLOCKS, z + width - origin.getZ()), cancelled::get));
                            completed.incrementAndGet();
                        }
                    }, Util.backgroundExecutor().forName("eden_block_surface"));
                    return CompletableFuture.allOf(encode.get(), encode.get());
                });
            } else generated = CompletableFuture.allOf(chain(), chain());
            result = generated.thenApply(ignored -> {
                if (cancelled.get()) throw new CancellationException();
                var complete = tiles.stream().sorted(Comparator.comparingInt(BlockSurfaceRenderer.Tile::z)
                        .thenComparingInt(BlockSurfaceRenderer.Tile::x)).toList();
                long columns = complete.stream().mapToLong(tile -> (long) tile.width() * tile.depth()).sum();
                if (columns != (long) width * width) throw new IllegalStateException("Incomplete block surface coverage: " + columns);
                return new BlockSurfaceRenderer.Surface(revision, x, z, width, width, complete, capture.palette());
            });
        }
        private CompletableFuture<Void> chain() {
            if (cancelled.get()) return CompletableFuture.failedFuture(new CancellationException());
            int index = next.getAndIncrement();
            if (index >= regions.size()) return CompletableFuture.completedFuture(null);
            BlockPos origin = regions.get(index);
            int tileWidth = Math.min(REGION_BLOCKS, x + width - origin.getX());
            int tileDepth = Math.min(REGION_BLOCKS, z + width - origin.getZ());
            var request = TerrainPreviewChunks.generate(registries, biome, pack, 20,
                    new BlockPos(origin.getX() + 128, 96, origin.getZ() + 128), cancelled);
            return request.complete().thenAcceptAsync(snapshot -> {
                if (cancelled.get()) throw new CancellationException();
                tiles.add(capture.encode(snapshot, origin.getX(), origin.getZ(), tileWidth, tileDepth, cancelled::get));
                completed.incrementAndGet();
            }, Util.backgroundExecutor().forName("eden_block_surface")).thenCompose(ignored -> chain());
        }
        public CompletableFuture<BlockSurfaceRenderer.Surface> result() { return result; }
        public int completedRegions() { return completed.get(); }
        public int totalRegions() { return regions.size(); }
        public void cancel() { cancelled.set(true); }
    }
}
