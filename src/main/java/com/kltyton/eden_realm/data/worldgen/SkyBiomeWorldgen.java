package com.kltyton.eden_realm.data.worldgen;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.registry.ERFeatures;
import com.kltyton.eden_realm.registry.content.block.ERSkyBlocks;
import com.kltyton.eden_realm.registry.content.block.ERTerrainBlocks;
import com.kltyton.eden_realm.world.terrain.IcyLandformDensity;
import com.kltyton.eden_realm.world.terrain.SkyLandformDensity;
import com.kltyton.eden_realm.world.terrain.TerrainPack;
import com.kltyton.eden_realm.world.terrain.TerrainProfile;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.biome.OverworldBiomes;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TimelineTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.attribute.AmbientSounds;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;

/** Terrain and water for the sky layer; vegetation and access mechanics are separate content. */
public final class SkyBiomeWorldgen {
    public static final ResourceKey<LevelStem> DIMENSION = ResourceKey.create(Registries.LEVEL_STEM, ERConstants.id("sky_layer"));
    public static final ResourceKey<DimensionType> DIMENSION_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, ERConstants.id("sky_layer"));
    public static final ResourceKey<NoiseGeneratorSettings> TERRAIN = ResourceKey.create(Registries.NOISE_SETTINGS, ERConstants.id("sky_layer"));
    private static final ResourceKey<ConfiguredFeature<?, ?>> WATER = ResourceKey.create(
            Registries.CONFIGURED_FEATURE, ERConstants.id("sky_surface_water"));
    private static final ResourceKey<PlacedFeature> WATER_PLACED = ResourceKey.create(
            Registries.PLACED_FEATURE, ERConstants.id("sky_surface_water"));
    private SkyBiomeWorldgen() { }

    static void dimensionTypes(BootstrapContext<DimensionType> context) {
        var attributes = EnvironmentAttributeMap.builder()
                .set(EnvironmentAttributes.FOG_COLOR, -4138753)
                .set(EnvironmentAttributes.SKY_COLOR, OverworldBiomes.calculateSkyColor(0.8F))
                .set(EnvironmentAttributes.AMBIENT_LIGHT_COLOR, -16119286)
                .set(EnvironmentAttributes.CLOUD_COLOR, ARGB.white(0.8F))
                .set(EnvironmentAttributes.CLOUD_HEIGHT, 192.33F)
                .set(EnvironmentAttributes.BACKGROUND_MUSIC, BackgroundMusic.OVERWORLD)
                .set(EnvironmentAttributes.BED_RULE, BedRule.CAN_SLEEP_WHEN_DARK)
                .set(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, false)
                .set(EnvironmentAttributes.NETHER_PORTAL_SPAWNS_PIGLINS, true)
                .set(EnvironmentAttributes.AMBIENT_SOUNDS, AmbientSounds.LEGACY_CAVE_SETTINGS).build();
        context.register(DIMENSION_TYPE, new DimensionType(false, true, false, false, 1.0,
                SkyLandformDensity.MIN_Y, SkyLandformDensity.HEIGHT, SkyLandformDensity.HEIGHT,
                context.lookup(Registries.BLOCK).getOrThrow(BlockTags.INFINIBURN_OVERWORLD), 0.0F,
                new DimensionType.MonsterSettings(UniformInt.of(0, 7), 0), DimensionType.Skybox.OVERWORLD,
                CardinalLighting.Type.DEFAULT, attributes,
                context.lookup(Registries.TIMELINE).getOrThrow(TimelineTags.IN_OVERWORLD),
                Optional.of(context.lookup(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD))));
    }

    static void configuredFeatures(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        FeatureUtils.register(context, WATER, ERFeatures.SKY_SURFACE_WATER.get(), NoneFeatureConfiguration.INSTANCE);
    }
    static void placedFeatures(BootstrapContext<PlacedFeature> context) {
        // Run for every chunk, including a biome boundary crossing a stream.
        PlacementUtils.register(context, WATER_PLACED, context.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(WATER), BiomeFilter.biome());
    }
    static void biomes(BootstrapContext<Biome> context) {
        for (String id : SkyLandformDensity.ALL_BIOMES) {
            var generation = new BiomeGenerationSettings.Builder(context.lookup(Registries.PLACED_FEATURE),
                    context.lookup(Registries.CONFIGURED_CARVER));
            generation.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, WATER_PLACED);
            context.register(biome(id), new Biome.BiomeBuilder().hasPrecipitation(true)
                    .temperature(0.6F).downfall(0.5F).setAttribute(EnvironmentAttributes.SKY_COLOR, 0x93C8F3)
                    .setAttribute(EnvironmentAttributes.CLOUD_HEIGHT, 112.33F)
                    .specialEffects(new BiomeSpecialEffects.Builder().waterColor(0x68A9DF).build())
                    .mobSpawnSettings(new MobSpawnSettings.Builder().build()).generationSettings(generation.build()).build());
        }
    }
    private static ResourceKey<Biome> biome(String id) { return ResourceKey.create(Registries.BIOME, ERConstants.id(id)); }

    private static NoiseRouter router(HolderGetter<net.minecraft.world.level.levelgen.synth.NormalNoise.NoiseParameters> noises, TerrainPack pack) {
        var layout = new DensityFunction.NoiseHolder(noises.getOrThrow(Noises.SHIFT));
        var detail = new DensityFunction.NoiseHolder(noises.getOrThrow(Noises.SURFACE));
        var profiles = SkyLandformDensity.BIOMES.stream().map(id -> pack == null ? TerrainProfile.official(id)
                : pack.biomes().getOrDefault(id, TerrainProfile.official(id))).toList();
        var region = DensityFunctions.flatCache(new SkyLandformDensity(layout, detail, profiles, SkyLandformDensity.REGION));
        var surface = DensityFunctions.flatCache(new SkyLandformDensity(layout, detail, profiles, SkyLandformDensity.SURFACE));
        var solid = new SkyLandformDensity(layout, detail, profiles, SkyLandformDensity.SOLID);
        var zero = DensityFunctions.constant(0);
        return new NoiseRouter(zero, zero, zero, zero, region, zero, zero, zero, zero, zero, surface, solid, zero, zero, zero);
    }

    static void noiseSettings(BootstrapContext<NoiseGeneratorSettings> context) {
        var rock = SurfaceRules.state(ERTerrainBlocks.FLOATING_ISLAND_ROCK.get().defaultBlockState());
        var surface = SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(-58), 0)),
                        SurfaceRules.state(ERSkyBlocks.CLOUD.get().defaultBlockState())),
                rock);
        context.register(TERRAIN, new NoiseGeneratorSettings(NoiseSettings.create(SkyLandformDensity.MIN_Y, SkyLandformDensity.HEIGHT, 1, 2),
                ERTerrainBlocks.FLOATING_ISLAND_ROCK.get().defaultBlockState(), Blocks.AIR.defaultBlockState(),
                router(context.lookup(Registries.NOISE), null), surface, java.util.List.of(), -64, true, false, false, false));
    }
    public static NoiseGeneratorSettings tunedNoiseSettings(HolderGetter.Provider registries, TerrainPack pack) {
        var base = registries.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(TERRAIN).value();
        return new NoiseGeneratorSettings(base.noiseSettings(), base.defaultBlock(), base.defaultFluid(),
                router(registries.lookupOrThrow(Registries.NOISE), pack), base.surfaceRule(), base.spawnTarget(), base.seaLevel(),
                base.disableMobGeneration(), false, false, base.useLegacyRandomSource());
    }
    private static MultiNoiseBiomeSource biomeSource(HolderGetter<Biome> lookup, TerrainPack pack) {
        var any = Climate.Parameter.span(-2, 2);
        var entries = new ArrayList<Pair<Climate.ParameterPoint, Holder<Biome>>>();
        for (int i = 0; i < SkyLandformDensity.BIOMES.size(); i++) {
            String id = SkyLandformDensity.BIOMES.get(i);
            if (pack != null && pack.biomes().getOrDefault(id, TerrainProfile.official(id)).generationChancePercent() == 0) continue;
            entries.add(Pair.of(Climate.parameters(Climate.Parameter.point((float) IcyLandformDensity.climate(i,
                    SkyLandformDensity.ALL_BIOMES.size())), any, any, any, any, any, 0), lookup.getOrThrow(biome(id))));
        }
        if (entries.isEmpty()) throw new IllegalArgumentException("At least one sky biome must be enabled");
        entries.add(Pair.of(Climate.parameters(Climate.Parameter.point((float) IcyLandformDensity.climate(
                SkyLandformDensity.BIOMES.size(), SkyLandformDensity.ALL_BIOMES.size())), any, any, any, any, any, 0),
                lookup.getOrThrow(biome(SkyLandformDensity.AIRSPACE))));
        return MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<>(entries));
    }
    static void dimensions(BootstrapContext<LevelStem> context) {
        context.register(DIMENSION, new LevelStem(context.lookup(Registries.DIMENSION_TYPE)
                .getOrThrow(DIMENSION_TYPE), new NoiseBasedChunkGenerator(
                biomeSource(context.lookup(Registries.BIOME), null), context.lookup(Registries.NOISE_SETTINGS).getOrThrow(TERRAIN))));
    }
    public static LevelStem tunedDimension(HolderGetter.Provider registries, TerrainPack pack) {
        return new LevelStem(registries.lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(DIMENSION_TYPE),
                new NoiseBasedChunkGenerator(biomeSource(registries.lookupOrThrow(Registries.BIOME), pack),
                        Holder.direct(tunedNoiseSettings(registries, pack))));
    }
}
