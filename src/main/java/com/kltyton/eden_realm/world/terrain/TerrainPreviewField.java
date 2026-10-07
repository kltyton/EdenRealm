package com.kltyton.eden_realm.world.terrain;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.data.worldgen.IcyBiomeWorldgen;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.DensityFunction.NoiseHolder;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.Noises;

/** Seeded two-dimensional inputs for an editor session, independent of the vanilla ore and climate graphs. */
public final class TerrainPreviewField {
    final RegistryAccess.Frozen registries;
    final NoiseGeneratorSettings settings;
    final List<? extends Holder<Biome>> biomes;
    final List<? extends Holder<Biome>> skyBiomes;
    private final CompletableFuture<NoiseHolder[]> noises;

    public TerrainPreviewField(RegistryAccess.Frozen registries, long seed) {
        this.registries = registries;
        settings = registries.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(IcyBiomeWorldgen.TERRAIN).value();
        var lookup = registries.lookupOrThrow(Registries.BIOME);
        biomes = IcyLandformDensity.BIOMES.stream().map(id ->
                lookup.getOrThrow(ResourceKey.create(Registries.BIOME, ERConstants.id(id)))).toList();
        skyBiomes = SkyLandformDensity.ALL_BIOMES.stream().map(id ->
                lookup.getOrThrow(ResourceKey.create(Registries.BIOME, ERConstants.id(id)))).toList();
        noises = CompletableFuture.supplyAsync(() -> {
            var parameters = registries.lookupOrThrow(Registries.NOISE);
            var random = settings.getRandomSource().newInstance(seed).forkPositional();
            return new NoiseHolder[] {
                    new NoiseHolder(parameters.getOrThrow(Noises.SHIFT), Noises.instantiate(parameters, random, Noises.SHIFT)),
                    new NoiseHolder(parameters.getOrThrow(Noises.SURFACE), Noises.instantiate(parameters, random, Noises.SURFACE)),
                    new NoiseHolder(parameters.getOrThrow(Noises.SURFACE_SECONDARY), Noises.instantiate(parameters, random, Noises.SURFACE_SECONDARY))
            };
        });
    }

    CompletableFuture<IcyLandformDensity> prepare(TerrainPack pack) {
        return noises.thenApply(ready -> field(ready, pack));
    }

    CompletableFuture<SkyLandformDensity> prepareSky(TerrainPack pack) {
        return noises.thenApply(ready -> skyField(ready, pack));
    }
    SkyLandformDensity readySky(TerrainPack pack) {
        NoiseHolder[] ready = noises.getNow(null);
        return ready == null ? null : skyField(ready, pack);
    }
    private static SkyLandformDensity skyField(NoiseHolder[] ready, TerrainPack pack) {
        return new SkyLandformDensity(ready[0], ready[1], SkyLandformDensity.BIOMES.stream()
                .map(id -> pack.biomes().getOrDefault(id, TerrainProfile.official(id))).toList(), SkyLandformDensity.SURFACE);
    }

    CompletableFuture<NoiseHolder[]> noises() { return noises; }

    public int seaLevel() { return settings.seaLevel(); }

    IcyLandformDensity ready(TerrainPack pack) {
        NoiseHolder[] ready = noises.getNow(null);
        return ready == null ? null : field(ready, pack);
    }

    private static IcyLandformDensity field(NoiseHolder[] ready, TerrainPack pack) {
        return new IcyLandformDensity(ready[0], ready[0], ready[1],
                IcyLandformDensity.BIOMES.stream().map(id -> pack.biomes().getOrDefault(id, TerrainProfile.official(id))).toList(), true);
    }
}
