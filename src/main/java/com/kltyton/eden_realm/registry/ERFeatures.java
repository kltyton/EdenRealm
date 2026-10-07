package com.kltyton.eden_realm.registry;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.world.feature.IcyShoreFreezeFeature;
import com.kltyton.eden_realm.world.feature.FrozenFjordFallsFeature;
import com.kltyton.eden_realm.world.feature.CrystalLakeIslandFeature;
import com.kltyton.eden_realm.world.feature.IcyPineFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ERFeatures {
    private static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, ERConstants.MOD_ID);

    public static final DeferredHolder<Feature<?>, IcyShoreFreezeFeature> ICY_SHORE_FREEZE =
            FEATURES.register("icy_shore_freeze",
                    () -> new IcyShoreFreezeFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, FrozenFjordFallsFeature> FROZEN_FJORD_FALLS =
            FEATURES.register("frozen_fjord_falls",
                    () -> new FrozenFjordFallsFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, CrystalLakeIslandFeature> CRYSTAL_LAKE_ISLAND =
            FEATURES.register("crystal_lake_island",
                    () -> new CrystalLakeIslandFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, IcyPineFeature> ICY_PINE =
            FEATURES.register("icy_pine", () -> new IcyPineFeature(TreeConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, IcyPineFeature> ICE_CRYSTAL_PINE =
            FEATURES.register("ice_crystal_pine", () -> new IcyPineFeature(TreeConfiguration.CODEC, false, false));
    public static final DeferredHolder<Feature<?>, IcyPineFeature> BIG_ICE_CRYSTAL_PINE =
            FEATURES.register("big_ice_crystal_pine", () -> new IcyPineFeature(TreeConfiguration.CODEC, true, false));

    public static final DeferredHolder<Feature<?>, com.kltyton.eden_realm.world.feature.SkySurfaceWaterFeature> SKY_SURFACE_WATER =
            FEATURES.register("sky_surface_water",
                    () -> new com.kltyton.eden_realm.world.feature.SkySurfaceWaterFeature(NoneFeatureConfiguration.CODEC));

    private ERFeatures() {
    }

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
    }
}
