package com.kltyton.eden_realm.world.terrain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/** Horizontal distance to a basin center; chance changes center frequency. */
public record BasinCellDistanceDensity(int chancePercent) implements DensityFunction.SimpleFunction {
    private static final MapCodec<BasinCellDistanceDensity> DATA_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(Codec.intRange(0, 200).fieldOf("chance_percent")
                    .forGetter(BasinCellDistanceDensity::chancePercent))
                    .apply(instance, BasinCellDistanceDensity::new));
    public static final KeyDispatchDataCodec<BasinCellDistanceDensity> CODEC =
            KeyDispatchDataCodec.of(DATA_CODEC);

    @Override
    public double compute(DensityFunction.FunctionContext context) {
        if (chancePercent == 0) return 1024.0;
        int cellSize = chancePercent > 100 ? 51200 / chancePercent : 512;
        int cellX = Math.floorDiv(context.blockX(), cellSize);
        int cellZ = Math.floorDiv(context.blockZ(), cellSize);
        int jitter = cellSize / 12;
        double nearestSquared = 1024.0 * 1024.0;
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                int currentX = cellX + dx;
                int currentZ = cellZ + dz;
                long mixed = mix((long) currentX * 0x9E3779B97F4A7C15L
                        ^ (long) currentZ * 0xC2B2AE3D27D4EB4FL);
                if (chancePercent < 100 && Math.floorMod(mixed >>> 8, 100) >= chancePercent) {
                    continue;
                }
                long centerX = (long) currentX * cellSize + cellSize / 2
                        + Math.floorMod(mixed, jitter * 2 + 1) - jitter;
                long centerZ = (long) currentZ * cellSize + cellSize / 2
                        + Math.floorMod(mixed >>> 32, jitter * 2 + 1) - jitter;
                double distanceX = context.blockX() - centerX;
                double distanceZ = context.blockZ() - centerZ;
                nearestSquared = Math.min(nearestSquared,
                        distanceX * distanceX + distanceZ * distanceZ);
            }
        }
        return Math.sqrt(nearestSquared);
    }

    public BlockPos centerNear(int blockX, int blockZ) {
        int cellSize = chancePercent > 100 ? 51200 / chancePercent : 512;
        int cellX = Math.floorDiv(blockX, cellSize);
        int cellZ = Math.floorDiv(blockZ, cellSize);
        int jitter = cellSize / 12;
        long nearestSquared = Long.MAX_VALUE;
        long bestX = blockX;
        long bestZ = blockZ;
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                int currentX = cellX + dx;
                int currentZ = cellZ + dz;
                long mixed = mix((long) currentX * 0x9E3779B97F4A7C15L
                        ^ (long) currentZ * 0xC2B2AE3D27D4EB4FL);
                if (chancePercent < 100 && Math.floorMod(mixed >>> 8, 100) >= chancePercent) {
                    continue;
                }
                long centerX = (long) currentX * cellSize + cellSize / 2
                        + Math.floorMod(mixed, jitter * 2 + 1) - jitter;
                long centerZ = (long) currentZ * cellSize + cellSize / 2
                        + Math.floorMod(mixed >>> 32, jitter * 2 + 1) - jitter;
                long distanceX = blockX - centerX;
                long distanceZ = blockZ - centerZ;
                long distanceSquared = distanceX * distanceX + distanceZ * distanceZ;
                if (distanceSquared < nearestSquared) {
                    nearestSquared = distanceSquared;
                    bestX = centerX;
                    bestZ = centerZ;
                }
            }
        }
        return new BlockPos((int) bestX, 96, (int) bestZ);
    }

    private static long mix(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        return value ^ value >>> 33;
    }

    @Override public double minValue() { return 0.0; }
    @Override public double maxValue() { return 1024.0; }
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() { return CODEC; }
}
