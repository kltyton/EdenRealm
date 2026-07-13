package com.kltyton.eden_realm.client.color;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ColorMapColorUtil;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.block.state.BlockState;

public final class ERGrassColors {
    private static final int EXPECTED_PIXEL_COUNT = 256 * 256;
    private static volatile int[] pixels = new int[0];

    public static final ColorResolver RESOLVER = ERGrassColors::resolve;
    public static final BlockTintSource BLOCK_TINT = new BlockTintSource() {
        @Override
        public int color(BlockState state) {
            return sample(0.5, 1.0);
        }

        @Override
        public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
            return level.getBlockTint(pos, RESOLVER);
        }

        @Override
        public int colorAsTerrainParticle(BlockState state, BlockAndTintGetter level, BlockPos pos) {
            return -1;
        }
    };

    private ERGrassColors() {
    }

    static void initialize(int[] loadedPixels) {
        if (loadedPixels.length != EXPECTED_PIXEL_COUNT) {
            throw new IllegalArgumentException(
                    "Eden grass colormap must contain exactly " + EXPECTED_PIXEL_COUNT + " pixels");
        }
        pixels = loadedPixels.clone();
    }

    public static int sample(double temperature, double downfall) {
        int[] currentPixels = pixels;
        if (currentPixels.length != EXPECTED_PIXEL_COUNT) {
            return GrassColor.getDefaultColor();
        }
        return ColorMapColorUtil.get(
                Mth.clamp(temperature, 0.0, 1.0),
                Mth.clamp(downfall, 0.0, 1.0),
                currentPixels,
                GrassColor.getDefaultColor());
    }

    private static int resolve(Biome biome, double x, double z) {
        Biome.ClimateSettings climate = biome.getModifiedClimateSettings();
        BiomeSpecialEffects effects = biome.getModifiedSpecialEffects();
        int baseColor = effects.grassColorOverride()
                .orElseGet(() -> sample(climate.temperature(), climate.downfall()));
        return effects.grassColorModifier().modifyColor(x, z, baseColor);
    }
}
