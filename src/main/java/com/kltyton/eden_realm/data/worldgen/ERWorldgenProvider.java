package com.kltyton.eden_realm.data.worldgen;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;

public final class ERWorldgenProvider {
    public static final RegistrySetBuilder REGISTRIES = new RegistrySetBuilder()
            .add(Registries.DIMENSION_TYPE, SkyBiomeWorldgen::dimensionTypes)
            .add(Registries.CONFIGURED_FEATURE, context -> {
                IcyBiomeWorldgen.configuredFeatures(context);
                SkyBiomeWorldgen.configuredFeatures(context);
            })
            .add(Registries.PLACED_FEATURE, context -> {
                IcyBiomeWorldgen.placedFeatures(context);
                SkyBiomeWorldgen.placedFeatures(context);
            })
            .add(Registries.BIOME, context -> {
                IcyBiomeWorldgen.biomes(context);
                SkyBiomeWorldgen.biomes(context);
            })
            .add(Registries.NOISE_SETTINGS, context -> {
                IcyBiomeWorldgen.noiseSettings(context);
                SkyBiomeWorldgen.noiseSettings(context);
            })
            .add(Registries.LEVEL_STEM, context -> {
                IcyBiomeWorldgen.dimensions(context);
                SkyBiomeWorldgen.dimensions(context);
            });

    private ERWorldgenProvider() {
    }
}
