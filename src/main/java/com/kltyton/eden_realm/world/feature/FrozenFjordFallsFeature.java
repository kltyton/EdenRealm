package com.kltyton.eden_realm.world.feature;

import com.kltyton.eden_realm.world.terrain.IcyLandformDensity;
import com.kltyton.eden_realm.registry.content.block.ERTerrainBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** A real upstream reservoir feeding frozen falls that cling to the fjord cliff face. */
public final class FrozenFjordFallsFeature extends Feature<NoneFeatureConfiguration> {
    private static final int FJORD = IcyLandformDensity.BIOMES.indexOf("icefall_fjord");

    public FrozenFjordFallsFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        return place(IcyFeatureArea.of(context.level()), context.origin(),
                IcyLandformDensity.from(context.level().getLevel().getChunkSource().randomState()));
    }

    public static boolean place(IcyFeatureArea area, BlockPos origin, IcyLandformDensity field) {
        return place(area, origin, field, true);
    }

    public static boolean place(IcyFeatureArea area, BlockPos origin, IcyLandformDensity field, boolean frozenFalls) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int baseX = origin.getX() & ~15;
        int baseZ = origin.getZ() & ~15;
        boolean changed = false;
        for (int dz = 0; dz < 16; dz++) {
            for (int dx = 0; dx < 16; dx++) {
                int x = baseX + dx;
                int z = baseZ + dz;
                IcyLandformDensity.Sample column = field.sample(x, z);
                if (column.biome() != FJORD) continue;
                int water = column.waterLevel();
                if (water <= 63) continue;
                if (column.frozenFall()) {
                    if (frozenFalls) changed |= buildFall(area, pos, x, z, water, column.fallDx(), column.fallDz());
                } else {
                    changed |= buildReservoir(area, field, pos, x, z, water, column.reservoir());
                }
            }
        }
        return changed;
    }

    private static boolean buildReservoir(IcyFeatureArea area, IcyLandformDensity field,
                                          BlockPos.MutableBlockPos pos, int x, int z, int water, boolean reservoir) {
        int floor = area.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (floor >= water || !area.getBlockState(pos.set(x, floor - 1, z)).blocksMotion()) return false;
        boolean changed = false;
        if (reservoir) {
            for (int y = floor - 1; ; y--) {
                pos.set(x, y, z);
                BlockState bed = area.getBlockState(pos);
                if (!bed.is(Blocks.GRASS_BLOCK) && !bed.is(Blocks.DIRT)
                        && !bed.is(ERTerrainBlocks.EDEN_GRASS_BLOCK.get()) && !bed.is(ERTerrainBlocks.EDEN_DIRT.get())) break;
                area.setBlock(pos, Blocks.GRAVEL.defaultBlockState(), 2);
                changed = true;
            }
        }
        for (int y = floor; y < water; y++) {
            pos.set(x, y, z);
            if (area.getBlockState(pos).isAir()) {
                area.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
                changed = true;
            }
        }
        if (reservoir && isShore(field, x, z, water)) {
            pos.set(x, water - 1, z);
            area.setBlock(pos, Blocks.ICE.defaultBlockState(), 2);
            changed = true;
        }
        return changed;
    }

    private static boolean isShore(IcyLandformDensity field, int x, int z, int water) {
        return field.sample(x + 1, z).waterLevel() != water || field.sample(x - 1, z).waterLevel() != water
                || field.sample(x, z + 1).waterLevel() != water || field.sample(x, z - 1).waterLevel() != water;
    }

    private static boolean buildFall(IcyFeatureArea area, BlockPos.MutableBlockPos pos,
                                     int x, int z, int water, int dx, int dz) {
        int ground = area.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        boolean changed = false;
        for (int y = ground; y < water; y++) {
            pos.set(x, y, z);
            BlockState old = area.getBlockState(pos);
            if (!old.isAir() && !old.is(Blocks.WATER)) continue;
            pos.set(x + dx, y, z + dz);
            BlockState cliff = area.getBlockState(pos);
            if (!cliff.blocksMotion() || cliff.is(Blocks.ICE) || cliff.is(Blocks.BLUE_ICE)
                    || cliff.is(Blocks.PACKED_ICE) || cliff.is(Blocks.WATER)) continue;
            pos.set(x, y, z);
            area.setBlock(pos, fallBlock(x, y, z), 2);
            changed = true;
        }
        return changed;
    }

    public static BlockState fallBlock(int x, int y, int z) {
        return Math.floorMod(x * 31 + z * 17 + y * 7, 3) == 0
                ? Blocks.PACKED_ICE.defaultBlockState() : Blocks.BLUE_ICE.defaultBlockState();
    }
}
