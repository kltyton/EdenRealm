package com.kltyton.eden_realm.world.terrain;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Util;
import net.minecraft.world.level.biome.Biome;

/** Samples the native quart lattice; the GPU resolves individual blocks without constructing underground chunks. */
public final class TerrainRealtimePreview {
    public static final int MARGIN = 32;
    public record Base(int x, int z, int width, float[] noise, List<? extends Holder<Biome>> biomes) { }
    public record Frame(long revision, Base base, float[] lattice, float[] shoreRadii, int seaLevel,
                        int minY, int maxY, String digest, double computeMillis) { }
    private final TerrainPreviewField field;
    private BaseKey baseKey;
    private CompletableFuture<Base> base;
    private record BaseKey(int x, int z, int width) { }

    public TerrainRealtimePreview(TerrainPreviewField field) { this.field = field; }

    public CompletableFuture<Frame> sample(TerrainPack pack, int chunks, BlockPos center, long revision,
                                            BooleanSupplier cancelled) {
        int width = Math.multiplyExact(chunks, 16);
        int x = Math.floorDiv(center.getX(), 16) * 16 - width / 2;
        int z = Math.floorDiv(center.getZ(), 16) * 16 - width / 2;
        BaseKey key = new BaseKey(x, z, width);
        if (!key.equals(baseKey)) {
            baseKey = key;
            base = field.noises().thenApplyAsync(noises -> {
                int side = width + MARGIN * 2;
                float[] values = new float[side * side * 4];
                parallelRows(side, cancelled, (first, last) -> {
                    for (int dz = first; dz < last; dz++) {
                        if (cancelled.getAsBoolean()) throw new CancellationException();
                        for (int dx = 0; dx < side; dx++) {
                            int wx = x + dx - MARGIN, wz = z + dz - MARGIN;
                            int index = (dz * side + dx) * 4;
                            values[index] = (float) noises[1].getValue(wx, 0, wz);
                            values[index + 1] = (float) noises[2].getValue(wx, 0, wz);
                            values[index + 2] = 1.0F;
                        }
                    }
                });
                return new Base(x, z, width, values, field.biomes);
            }, Util.backgroundExecutor().forName("eden_preview_material_noise"));
        }
        long started = System.nanoTime();
        return base.thenCompose(prepared -> field.prepare(pack).thenApplyAsync(density -> {
                    if (cancelled.getAsBoolean()) throw new CancellationException();
                    int side = (width + MARGIN * 2) / 4 + 1;
                    float[] lattice = new float[side * side * 4];
                    parallelRows(side, cancelled, (first, last) -> {
                        for (int dz = first; dz < last; dz++) {
                            if (cancelled.getAsBoolean()) throw new CancellationException();
                            for (int dx = 0; dx < side; dx++) {
                                var value = density.sample(x - MARGIN + dx * 4, z - MARGIN + dz * 4);
                                int index = (dz * side + dx) * 4;
                                lattice[index] = (float) value.height();
                                lattice[index + 1] = value.biome();
                            }
                        }
                    });
                    float[] radii = new float[IcyLandformDensity.BIOMES.size() * 4];
                    for (int i = 0; i < IcyLandformDensity.BIOMES.size(); i++)
                        radii[i * 4] = pack.biomes().get(IcyLandformDensity.BIOMES.get(i)).shoreIceBlocks();
                    var bounds = field.settings.noiseSettings();
                    return new Frame(revision, prepared, lattice, radii, field.settings.seaLevel(), bounds.minY(),
                            bounds.minY() + bounds.height(), digest(lattice, radii), (System.nanoTime() - started) / 1_000_000.0);
                }, Util.backgroundExecutor().forName("eden_preview_lattice")));
    }

    private interface Rows { void sample(int first, int last); }
    private static void parallelRows(int side, BooleanSupplier cancelled, Rows rows) {
        int workers = Math.clamp(Runtime.getRuntime().availableProcessors() / 2, 1, 8);
        int count = Math.ceilDiv(side, workers);
        var tasks = new java.util.ArrayList<CompletableFuture<Void>>(workers);
        for (int i = 0; i < workers; i++) {
            int first = i * count, last = Math.min(side, first + count);
            if (first >= last) break;
            tasks.add(CompletableFuture.runAsync(() -> {
                if (cancelled.getAsBoolean()) throw new CancellationException();
                rows.sample(first, last);
            }, Util.backgroundExecutor().forName("eden_preview_rows")));
        }
        CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new)).join();
    }

    private static String digest(float[] lattice, float[] radii) {
        try {
            var hash = MessageDigest.getInstance("SHA-256");
            ByteBuffer bytes = ByteBuffer.allocate((lattice.length + radii.length) * 4);
            for (float value : lattice) bytes.putFloat(value);
            for (float value : radii) bytes.putFloat(value);
            return HexFormat.of().formatHex(hash.digest(bytes.array()));
        } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
}
