package com.kltyton.eden_realm.data.worldgen;

import com.kltyton.eden_realm.world.terrain.IcyLandformDensity;
import com.kltyton.eden_realm.world.terrain.TerrainPack;
import com.kltyton.eden_realm.world.terrain.TerrainProfile;
import java.util.List;
import net.minecraft.core.HolderGetter;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/** Shared seeded region layout for biome selection and reference landforms. */
final class IcyTerrainDensity {
    record Terrain(DensityFunction region, DensityFunction surfaceY, DensityFunction caves) { }
    private IcyTerrainDensity() { }

    static Terrain create(HolderGetter<NormalNoise.NoiseParameters> noises) {
        return create(noises, IcyLandformDensity.BIOMES.stream().map(TerrainProfile::official).toList());
    }

    static Terrain create(HolderGetter<NormalNoise.NoiseParameters> noises, TerrainPack pack) {
        return create(noises, IcyLandformDensity.BIOMES.stream().map(id -> pack.biomes().getOrDefault(id, TerrainProfile.official(id))).toList());
    }

    private static Terrain create(HolderGetter<NormalNoise.NoiseParameters> noises, List<TerrainProfile> profiles) {
        var layout = new DensityFunction.NoiseHolder(noises.getOrThrow(Noises.SHIFT));
        var warp = new DensityFunction.NoiseHolder(noises.getOrThrow(Noises.SHIFT));
        var detail = new DensityFunction.NoiseHolder(noises.getOrThrow(Noises.SURFACE));
        return new Terrain(DensityFunctions.flatCache(new IcyLandformDensity(layout, warp, detail, profiles, false)),
                DensityFunctions.flatCache(new IcyLandformDensity(layout, warp, detail, profiles, true)),
                new IcyLandformDensity(layout, warp, detail, profiles, true, true));
    }
}
