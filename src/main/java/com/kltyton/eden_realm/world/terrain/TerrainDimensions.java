package com.kltyton.eden_realm.world.terrain;

import com.kltyton.eden_realm.ERConstants;
import java.util.Map;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;

/** Keeps the world's inline Eden generator when datapack dimensions are merged. */
public final class TerrainDimensions {
    private static final ResourceKey<LevelStem> DIMENSION = ResourceKey.create(
            Registries.LEVEL_STEM, ERConstants.id("eden_layer"));

    private TerrainDimensions() { }

    public static Registry<LevelStem> datapackDimensions(Map<ResourceKey<LevelStem>, LevelStem> selected,
                                                         Registry<LevelStem> datapack) {
        var dimensions = java.util.Set.of(DIMENSION, com.kltyton.eden_realm.data.worldgen.SkyBiomeWorldgen.DIMENSION);
        var edited = selected.entrySet().stream().filter(entry -> dimensions.contains(entry.getKey())
                && entry.getValue().generator() instanceof NoiseBasedChunkGenerator noise
                && noise.generatorSettings().unwrapKey().isEmpty())
                .map(Map.Entry::getKey).collect(java.util.stream.Collectors.toSet());
        if (edited.isEmpty()) return datapack;
        // Vanilla bake prefers datapack entries; an inline generator carries the
        // edited world settings and must survive both creation and save loading.
        MappedRegistry<LevelStem> remaining = new MappedRegistry<>(
                Registries.LEVEL_STEM, datapack.registryLifecycle());
        datapack.listElements().filter(holder -> !edited.contains(holder.key()))
                .forEach(holder -> remaining.register(holder.key(), holder.value(),
                        datapack.registrationInfo(holder.key()).orElseThrow()));
        return remaining.freeze();
    }
}
