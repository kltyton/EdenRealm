package com.kltyton.eden_realm.world.terrain;

import com.kltyton.eden_realm.ERConstants;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.RandomState;

/** The icy biome source has five identical climate intervals and one discrete, Y-independent region axis. */
final class IcyPreviewBiomes implements BiomeResolver {
    private final DensityFunction regions;
    private final List<? extends Holder<Biome>> biomes;
    private final Map<Long, Holder<Biome>> columns = new ConcurrentHashMap<>();

    IcyPreviewBiomes(RegistryAccess.Frozen registries, RandomState randomState, boolean sky) {
        regions = randomState.router().temperature();
        var lookup = registries.lookupOrThrow(Registries.BIOME);
        biomes = (sky ? SkyLandformDensity.ALL_BIOMES : IcyLandformDensity.BIOMES).stream().map(id ->
                lookup.getOrThrow(ResourceKey.create(Registries.BIOME, ERConstants.id(id)))).toList();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        long key = ((long) x << 32) | (z & 0xffffffffL);
        return columns.computeIfAbsent(key, ignored -> biomes.get((int) Math.round(
                Math.clamp((regions.compute(new DensityFunction.SinglePointContext(
                        QuartPos.toBlock(x), 64, QuartPos.toBlock(z))) + 0.9) * (biomes.size() - 1) / 1.8, 0, biomes.size() - 1))));
    }
}
