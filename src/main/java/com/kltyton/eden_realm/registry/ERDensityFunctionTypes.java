package com.kltyton.eden_realm.registry;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.world.terrain.BasinCellDistanceDensity;
import com.kltyton.eden_realm.world.terrain.WeightedBiomeRegion;
import com.kltyton.eden_realm.world.terrain.IcyLandformDensity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ERDensityFunctionTypes {
    private static final DeferredRegister<MapCodec<? extends DensityFunction>> TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, ERConstants.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends DensityFunction>,
            MapCodec<? extends DensityFunction>> ICY_LANDFORM =
            TYPES.register("icy_landform", () -> IcyLandformDensity.CODEC.codec());

    public static final DeferredHolder<MapCodec<? extends DensityFunction>,
            MapCodec<? extends DensityFunction>> BASIN_CELL_DISTANCE =
            TYPES.register("basin_cell_distance", () -> BasinCellDistanceDensity.CODEC.codec());

    public static final DeferredHolder<MapCodec<? extends DensityFunction>,
            MapCodec<? extends DensityFunction>> WEIGHTED_BIOME_REGION =
            TYPES.register("weighted_biome_region", () -> WeightedBiomeRegion.CODEC.codec());

    public static final DeferredHolder<MapCodec<? extends DensityFunction>,
            MapCodec<? extends DensityFunction>> SKY_LANDFORM =
            TYPES.register("sky_landform", () -> com.kltyton.eden_realm.world.terrain.SkyLandformDensity.CODEC.codec());

    private ERDensityFunctionTypes() { }

    public static void register(IEventBus modEventBus) {
        com.kltyton.eden_realm.world.terrain.SkyIslandShape.loadReferences();
        TYPES.register(modEventBus);
    }
}
