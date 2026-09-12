package com.kltyton.eden_realm.common.block.plant;

import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Visible bounds measured offline by tools/measure_crop_models.py, with authored rotations and UVs. */
final class ERHarvestShapes {
    private static final Map<String, VoxelShape[]> FRUITS = Map.of(
            "tide_song_flowering_leaves", new VoxelShape[]{column(4, 8, 16), column(12.94, 6, 16),
                    column(5, 10, 16), column(6, 9, 16), column(7, 8, 16)},
            "sacred_light_flowering_leaves", new VoxelShape[]{column(2.4, 14, 16), column(7.4, 14, 16),
                    column(3, 11.5, 16), column(4, 12, 17), column(6, 8, 16)},
            "cloud_crown_flowering_leaves", new VoxelShape[]{column(2.2, 13, 16), column(7.4, 14, 16),
                    column(3, 8, 16), column(4, 7, 17), column(4, 0, 16)},
            "twilight_pomegranate_log", new VoxelShape[]{column(6, 9.25, 16), column(11.09, 10, 16),
                    column(8, 6, 16), column(12, 4, 17), column(16, -1.5, 16)});
    private static final int[] GRAIN_HEIGHTS = {2, 5, 8, 13, 19, 22, 25, 27, 29};
    private static final VoxelShape[] GRAIN_LOWER = new VoxelShape[GRAIN_HEIGHTS.length];
    private static final VoxelShape[] GRAIN_UPPER = new VoxelShape[GRAIN_HEIGHTS.length];

    static {
        for (int stage = 0; stage < GRAIN_HEIGHTS.length; stage++) {
            GRAIN_LOWER[stage] = column(15.33, 0, Math.min(16, GRAIN_HEIGHTS[stage]));
            GRAIN_UPPER[stage] = GRAIN_HEIGHTS[stage] <= 16 ? Shapes.empty()
                    : column(15.33, 0, GRAIN_HEIGHTS[stage] - 16);
        }
    }

    private ERHarvestShapes() {
    }

    static VoxelShape fruit(Identifier support, int age) {
        return FRUITS.get(support.getPath())[age];
    }

    static VoxelShape grain(int stage, DoubleBlockHalf half) {
        return (half == DoubleBlockHalf.LOWER ? GRAIN_LOWER : GRAIN_UPPER)[stage];
    }

    private static VoxelShape column(double width, double bottom, double top) {
        return Block.column(width, bottom, top);
    }
}
