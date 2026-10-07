package com.kltyton.eden_realm.world.tree;

import com.kltyton.eden_realm.ERConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public final class IceCrystalPineTrees {
    public static final ResourceKey<ConfiguredFeature<?, ?>> SINGLE = key("ice_crystal_pine");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PAIRED = key("big_ice_crystal_pine");

    private IceCrystalPineTrees() { }

    private static ResourceKey<ConfiguredFeature<?, ?>> key(String id) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ERConstants.id(id));
    }
}
