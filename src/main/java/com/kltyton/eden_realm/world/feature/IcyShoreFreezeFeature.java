package com.kltyton.eden_realm.world.feature;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Puts snow on land and freezes supported source water only near a real shore. */
public final class IcyShoreFreezeFeature extends Feature<NoneFeatureConfiguration> {
    public IcyShoreFreezeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        return place(IcyFeatureArea.of(context.level()), context.origin());
    }

    public static boolean place(IcyFeatureArea level, BlockPos origin) {
        BlockPos.MutableBlockPos top = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();
        Map<Integer, ShoreMask> shores = new HashMap<>();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                top.set(x, y, z);
                below.set(x, y - 1, z);
                Holder<Biome> biome = level.getBiome(top);
                BlockState surface = level.getBlockState(below);

                if (!surface.is(Blocks.ICE) && !surface.is(Blocks.PACKED_ICE)
                        && !surface.is(Blocks.BLUE_ICE)
                        && level.shouldSnow(biome, top)) {
                    level.setBlock(top, Blocks.SNOW.defaultBlockState(), 2);
                    if (surface.hasProperty(SnowyBlock.SNOWY)) {
                        level.setBlock(below, surface.setValue(SnowyBlock.SNOWY, true), 2);
                    }
                }

                if (surface.is(Blocks.WATER) && level.getFluidState(below).isSource()
                        && nearSolidShore(level, below, origin,
                                level.profile(biome).shoreIceBlocks(), shores)
                        && supportedWater(level, below)) {
                    level.setBlock(below, Blocks.ICE.defaultBlockState(), 2);
                }
            }
        }
        return true;
    }

    private static boolean nearSolidShore(IcyFeatureArea level, BlockPos water, BlockPos origin,
                                          int radius, Map<Integer, ShoreMask> shores) {
        if (radius == 0) return false;
        ShoreMask mask = shores.get(water.getY());
        if (mask == null || mask.radius < radius) {
            mask = ShoreMask.capture(level, origin, water.getY(), radius);
            shores.put(water.getY(), mask);
        }
        int localX = water.getX() - mask.minX;
        int localZ = water.getZ() - mask.minZ;
        int width = mask.width;
        boolean[] solid = mask.solid;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= radius * radius
                        && solid[(localZ + dz) * width + localX + dx]) return true;
            }
        }
        return false;
    }

    private static boolean supportedWater(IcyFeatureArea level, BlockPos water) {
        BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();
        for (int depth = 1; depth <= 16; depth++) {
            neighbor.set(water.getX(), water.getY() - depth, water.getZ());
            BlockState state = level.getBlockState(neighbor);
            if (state.isAir()) return false;
            if (state.getFluidState().isEmpty()) return level.isSolidShore(state, neighbor);
        }
        return true;
    }

    private record ShoreMask(int minX, int minZ, int radius, int width, boolean[] solid) {
        static ShoreMask capture(IcyFeatureArea level, BlockPos origin, int y, int radius) {
            int width = 16 + radius * 2;
            int minX = origin.getX() - radius;
            int minZ = origin.getZ() - radius;
            boolean[] solid = new boolean[width * width];
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int z = 0; z < width; z++) {
                for (int x = 0; x < width; x++) {
                    pos.set(minX + x, y, minZ + z);
                    BlockState state = level.getBlockState(pos);
                    solid[z * width + x] = !state.is(Blocks.ICE)
                            && !state.is(Blocks.PACKED_ICE) && !state.is(Blocks.BLUE_ICE)
                            && level.isSolidShore(state, pos);
                }
            }
            return new ShoreMask(minX, minZ, radius, width, solid);
        }
    }
}
