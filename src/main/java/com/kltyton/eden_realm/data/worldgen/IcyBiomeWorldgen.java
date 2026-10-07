package com.kltyton.eden_realm.data.worldgen;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.block.tree.ERWoodSet;
import com.kltyton.eden_realm.registry.ERBlocks;
import com.kltyton.eden_realm.registry.ERFeatures;
import com.kltyton.eden_realm.registry.content.block.ERPlantBlocks;
import com.kltyton.eden_realm.registry.content.block.ERTerrainBlocks;
import com.kltyton.eden_realm.world.terrain.TerrainPack;
import com.kltyton.eden_realm.world.terrain.TerrainProfile;
import com.kltyton.eden_realm.world.tree.IceCrystalPineTrees;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowyBlock;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.BlockColumnConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.BlockPileConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.PineFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.NoiseThresholdCountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/** Connected icy and silver-frost landscapes in the development dimension. */
public final class IcyBiomeWorldgen {
    private static final List<BiomeSpec> BIOMES = List.of(
            new BiomeSpec("icy_rolling_hills", 1, 1, 0),
            new BiomeSpec("glacier_meander", 2, 1, 0),
            new BiomeSpec("crystal_lake_shore", 2, 1, 1),
            new BiomeSpec("blue_ice_plateau", 1, 0, 1),
            new BiomeSpec("ice_ridge_valley", 2, 1, 1),
            new BiomeSpec("icefall_fjord", 1, 0, 1),
            new BiomeSpec("frozen_fissure", 1, 0, 1),
            new BiomeSpec("cold_spring_lowland", 2, 2, 0),
            new BiomeSpec("crystal_stone_plain", 1, 1, 2),
            new BiomeSpec("ice_crystal_basin", 2, 1, 1),
            new BiomeSpec("silver_frost_hills", 0, 0, 0),
            new BiomeSpec("frost_stream_valley", 0, 0, 0),
            new BiomeSpec("silver_frost_lakeshore", 0, 0, 0),
            new BiomeSpec("frost_rock_plateau", 0, 0, 0),
            new BiomeSpec("snow_ridge_valley", 0, 0, 0),
            new BiomeSpec("silver_frost_basin", 0, 0, 0));
    public static final ResourceKey<NoiseGeneratorSettings> TERRAIN = ResourceKey.create(
            Registries.NOISE_SETTINGS, ERConstants.id("icy_rolling_hills"));
    public static final ResourceKey<LevelStem> DIMENSION = ResourceKey.create(
            Registries.LEVEL_STEM, ERConstants.id("eden_layer"));
    private static final ResourceKey<ConfiguredFeature<?, ?>> ICE_PINE = configured("icy_rolling_hills_ice_pine");
    private static final ResourceKey<ConfiguredFeature<?, ?>> FROST_GRASS = configured("icy_rolling_hills_frost_grass");
    private static final ResourceKey<ConfiguredFeature<?, ?>> CRYSTAL_SPIRE = configured("icy_crystal_spire");
    private static final ResourceKey<ConfiguredFeature<?, ?>> CRYSTAL_PILE = configured("icy_crystal_pile");
    private static final ResourceKey<ConfiguredFeature<?, ?>> SHORE_FREEZE = configured("icy_shore_freeze");
    private static final ResourceKey<ConfiguredFeature<?, ?>> FJORD_FALLS = configured("frozen_fjord_falls");
    private static final ResourceKey<ConfiguredFeature<?, ?>> LAKE_ISLAND = configured("crystal_lake_island");
    private static final ResourceKey<PlacedFeature> ICE_PINE_PLACED = placed("icy_rolling_hills_ice_pine");
    private static final ResourceKey<PlacedFeature> FROST_GRASS_PLACED = placed("icy_rolling_hills_frost_grass");
    private static final ResourceKey<PlacedFeature> ICE_PINE_DENSE = placed("icy_ice_pine_dense");
    private static final ResourceKey<PlacedFeature> FROST_GRASS_DENSE = placed("icy_frost_grass_dense");
    private static final ResourceKey<PlacedFeature> CRYSTAL_SPIRE_PLACED = placed("icy_crystal_spire");
    private static final ResourceKey<PlacedFeature> CRYSTAL_PILE_PLACED = placed("icy_crystal_pile");
    private static final ResourceKey<PlacedFeature> SHORE_FREEZE_PLACED = placed("icy_shore_freeze");
    private static final ResourceKey<PlacedFeature> FJORD_FALLS_PLACED = placed("frozen_fjord_falls");
    private static final ResourceKey<PlacedFeature> LAKE_ISLAND_PLACED = placed("crystal_lake_island");

    private IcyBiomeWorldgen() {
    }

    private record BiomeSpec(String id, int pine, int grass, int crystal) {
        ResourceKey<Biome> key() {
            return biomeKey(id);
        }
    }

    private static ResourceKey<Biome> biomeKey(String id) {
        return ResourceKey.create(Registries.BIOME, ERConstants.id(id));
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configured(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ERConstants.id(name));
    }

    private static ResourceKey<PlacedFeature> placed(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, ERConstants.id(name));
    }

    static void configuredFeatures(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        FeatureUtils.register(context, ICE_PINE, ERFeatures.ICY_PINE.get(), pineConfiguration(false));
        FeatureUtils.register(context, IceCrystalPineTrees.SINGLE, ERFeatures.ICE_CRYSTAL_PINE.get(), pineConfiguration(false));
        FeatureUtils.register(context, IceCrystalPineTrees.PAIRED, ERFeatures.BIG_ICE_CRYSTAL_PINE.get(), pineConfiguration(true));
        FeatureUtils.register(context, FROST_GRASS, Feature.SIMPLE_BLOCK,
                new SimpleBlockConfiguration(BlockStateProvider.simple(ERPlantBlocks.FROST_CRYSTAL_GRASS.get())));
        FeatureUtils.register(context, CRYSTAL_SPIRE, Feature.BLOCK_COLUMN,
                BlockColumnConfiguration.simple(UniformInt.of(2, 5),
                        BlockStateProvider.simple(ERTerrainBlocks.ICE_CRYSTAL_ROCK.get())));
        FeatureUtils.register(context, CRYSTAL_PILE, Feature.BLOCK_PILE,
                new BlockPileConfiguration(BlockStateProvider.simple(ERTerrainBlocks.ICE_CRYSTAL_ROCK.get())));
        FeatureUtils.register(context, SHORE_FREEZE, ERFeatures.ICY_SHORE_FREEZE.get(),
                NoneFeatureConfiguration.INSTANCE);
        FeatureUtils.register(context, FJORD_FALLS, ERFeatures.FROZEN_FJORD_FALLS.get(),
                NoneFeatureConfiguration.INSTANCE);
        FeatureUtils.register(context, LAKE_ISLAND, ERFeatures.CRYSTAL_LAKE_ISLAND.get(),
                NoneFeatureConfiguration.INSTANCE);
    }

    private static TreeConfiguration pineConfiguration(boolean large) {
        var pine = ERBlocks.woodBlocks(ERWoodSet.ICE_CRYSTAL_PINE);
        return new TreeConfiguration.TreeConfigurationBuilder(
                        BlockStateProvider.simple(pine.log().get()),
                        new StraightTrunkPlacer(large ? 22 : 9, large ? 2 : 5, 0),
                        BlockStateProvider.simple(pine.leaves().get()),
                        new PineFoliagePlacer(ConstantInt.of(1), ConstantInt.of(1), UniformInt.of(3, 4)),
                        new TwoLayersFeatureSize(2, 0, 2),
                        BlockStateProvider.simple(ERTerrainBlocks.EDEN_DIRT.get()))
                        .ignoreVines().build();
    }

    static void placedFeatures(BootstrapContext<PlacedFeature> context) {
        var features = context.lookup(Registries.CONFIGURED_FEATURE);
        PlacementUtils.register(context, ICE_PINE_PLACED, features.getOrThrow(ICE_PINE),
                NoiseThresholdCountPlacement.of(0.05, 0, 1),
                InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_NO_LEAVES,
                PlacementUtils.filteredByBlockSurvival(ERBlocks.woodBlocks(ERWoodSet.ICE_CRYSTAL_PINE).sapling().get()),
                BiomeFilter.biome());
        PlacementUtils.register(context, ICE_PINE_DENSE, features.getOrThrow(ICE_PINE),
                NoiseThresholdCountPlacement.of(0.05, 0, 3),
                InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_NO_LEAVES,
                PlacementUtils.filteredByBlockSurvival(ERBlocks.woodBlocks(ERWoodSet.ICE_CRYSTAL_PINE).sapling().get()),
                BiomeFilter.biome());

        var onEdenGrass = BlockPredicateFilter.forPredicate(BlockPredicate.allOf(
                BlockPredicate.ONLY_IN_AIR_PREDICATE,
                BlockPredicate.matchesBlocks(Direction.DOWN.getUnitVec3i(), ERTerrainBlocks.EDEN_GRASS_BLOCK.get())));
        PlacementUtils.register(context, FROST_GRASS_PLACED, features.getOrThrow(FROST_GRASS),
                CountPlacement.of(8), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP,
                onEdenGrass, BiomeFilter.biome());
        PlacementUtils.register(context, FROST_GRASS_DENSE, features.getOrThrow(FROST_GRASS),
                CountPlacement.of(20), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP,
                onEdenGrass, BiomeFilter.biome());
        var onFrozenGround = BlockPredicateFilter.forPredicate(BlockPredicate.allOf(
                BlockPredicate.ONLY_IN_AIR_PREDICATE,
                BlockPredicate.matchesBlocks(Direction.DOWN.getUnitVec3i(),
                        ERTerrainBlocks.EDEN_GRASS_BLOCK.get(), ERTerrainBlocks.ICE_CRYSTAL_ROCK.get(),
                        Blocks.STONE, Blocks.SNOW_BLOCK, Blocks.PACKED_ICE)));
        PlacementUtils.register(context, CRYSTAL_SPIRE_PLACED, features.getOrThrow(CRYSTAL_SPIRE),
                RarityFilter.onAverageOnceEvery(6), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP,
                onFrozenGround, BiomeFilter.biome());
        PlacementUtils.register(context, CRYSTAL_PILE_PLACED, features.getOrThrow(CRYSTAL_PILE),
                RarityFilter.onAverageOnceEvery(3), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP,
                onFrozenGround, BiomeFilter.biome());
        PlacementUtils.register(context, SHORE_FREEZE_PLACED, features.getOrThrow(SHORE_FREEZE), BiomeFilter.biome());
        PlacementUtils.register(context, FJORD_FALLS_PLACED, features.getOrThrow(FJORD_FALLS), BiomeFilter.biome());
        PlacementUtils.register(context, LAKE_ISLAND_PLACED, features.getOrThrow(LAKE_ISLAND),
                RarityFilter.onAverageOnceEvery(64), InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP, BiomeFilter.biome());
    }

    static void biomes(BootstrapContext<Biome> context) {
        for (BiomeSpec spec : BIOMES) {
            context.register(spec.key(), biome(context, spec));
        }
    }

    private static Biome biome(BootstrapContext<Biome> context, BiomeSpec spec) {
        var generation = new BiomeGenerationSettings.Builder(
                context.lookup(Registries.PLACED_FEATURE), context.lookup(Registries.CONFIGURED_CARVER));
        generation.addCarver(Carvers.CAVE);
        generation.addCarver(Carvers.CAVE_EXTRA_UNDERGROUND);
        generation.addCarver(Carvers.CANYON);
        if (BIOMES.indexOf(spec) < 10) {
            generation.addFeature(GenerationStep.Decoration.LAKES, MiscOverworldPlacements.LAKE_LAVA_UNDERGROUND);
            BiomeDefaultFeatures.addDefaultCrystalFormations(generation);
            BiomeDefaultFeatures.addDefaultMonsterRoom(generation);
            BiomeDefaultFeatures.addDefaultUndergroundVariety(generation);
            BiomeDefaultFeatures.addDefaultSprings(generation);
            BiomeDefaultFeatures.addDefaultOres(generation);
            BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        }
        generation.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, FJORD_FALLS_PLACED);
        if (spec.id().equals("crystal_lake_shore") || spec.id().equals("silver_frost_lakeshore")) {
            generation.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, LAKE_ISLAND_PLACED);
        }
        generation.addFeature(GenerationStep.Decoration.TOP_LAYER_MODIFICATION, SHORE_FREEZE_PLACED);
        if (spec.pine() > 0) {
            generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION,
                    spec.pine() == 2 ? ICE_PINE_DENSE : ICE_PINE_PLACED);
        }
        if (spec.grass() > 0) {
            generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION,
                    spec.grass() == 2 ? FROST_GRASS_DENSE : FROST_GRASS_PLACED);
        }
        if (spec.crystal() > 0) {
            generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, CRYSTAL_SPIRE_PLACED);
            if (spec.crystal() == 2) {
                generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, CRYSTAL_PILE_PLACED);
            }
        }

        var mobs = new MobSpawnSettings.Builder();
        if (BIOMES.indexOf(spec) < 10) BiomeDefaultFeatures.snowySpawns(mobs, true);
        return new Biome.BiomeBuilder()
                .hasPrecipitation(true).temperature(0.0F).downfall(0.5F)
                .setAttribute(EnvironmentAttributes.SKY_COLOR, 0x77ADFF)
                .specialEffects(new BiomeSpecialEffects.Builder().waterColor(4159204).build())
                .mobSpawnSettings(mobs.build()).generationSettings(generation.build()).build();
    }

    static void noiseSettings(BootstrapContext<NoiseGeneratorSettings> context) {
        NoiseRouter vanilla = OverworldRouter.create(context);
        IcyTerrainDensity.Terrain terrain = IcyTerrainDensity.create(context.lookup(Registries.NOISE));
        NoiseRouter router = shapedRouter(vanilla, terrain);

        var grass = SurfaceRules.state(ERTerrainBlocks.EDEN_GRASS_BLOCK.get().defaultBlockState()
                .setValue(SnowyBlock.SNOWY, false));
        var dirt = SurfaceRules.state(ERTerrainBlocks.EDEN_DIRT.get().defaultBlockState());
        var stone = SurfaceRules.state(Blocks.STONE.defaultBlockState());
        var andesite = SurfaceRules.state(Blocks.ANDESITE.defaultBlockState());
        var packedIce = SurfaceRules.state(Blocks.PACKED_ICE.defaultBlockState());
        var blueIce = SurfaceRules.state(Blocks.BLUE_ICE.defaultBlockState());
        var snow = SurfaceRules.state(Blocks.SNOW_BLOCK.defaultBlockState());
        var gravel = SurfaceRules.state(Blocks.GRAVEL.defaultBlockState());
        var crystalRock = SurfaceRules.state(ERTerrainBlocks.ICE_CRYSTAL_ROCK.get().defaultBlockState());
        var biomes = context.lookup(Registries.BIOME);
        var top = SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.not(SurfaceRules.waterBlockCheck(0, 0)), gravel),
                SurfaceRules.ifTrue(SurfaceRules.isBiome(biomes, biomeKey("crystal_stone_plain")),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(SurfaceRules.noiseCondition2d(Noises.SURFACE, 0.15), crystalRock),
                                SurfaceRules.ifTrue(SurfaceRules.steep(), stone))),
                SurfaceRules.ifTrue(SurfaceRules.isBiome(biomes, biomeKey("blue_ice_plateau"), biomeKey("frost_rock_plateau")),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(SurfaceRules.steep(), SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(SurfaceRules.noiseCondition2d(Noises.SURFACE_SECONDARY, 0.35), packedIce), stone)),
                                SurfaceRules.ifTrue(SurfaceRules.noiseCondition2d(Noises.SURFACE_SECONDARY, 0.45), blueIce))),
                SurfaceRules.ifTrue(SurfaceRules.isBiome(biomes, biomeKey("ice_ridge_valley"), biomeKey("snow_ridge_valley")),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(SurfaceRules.steep(), stone),
                                SurfaceRules.ifTrue(SurfaceRules.isBiome(biomes, biomeKey("snow_ridge_valley")),
                                        SurfaceRules.ifTrue(SurfaceRules.yBlockCheck(
                                                net.minecraft.world.level.levelgen.VerticalAnchor.absolute(142), 0), snow)),
                                SurfaceRules.ifTrue(SurfaceRules.noiseCondition2d(Noises.SURFACE_SECONDARY, 0.48), snow))),
                SurfaceRules.ifTrue(SurfaceRules.isBiome(biomes, biomeKey("icefall_fjord")),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(SurfaceRules.steep(), SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(SurfaceRules.noiseCondition2d(Noises.SURFACE_SECONDARY, 0.25), andesite),
                                        stone)))),
                SurfaceRules.ifTrue(SurfaceRules.isBiome(biomes, biomeKey("frozen_fissure")),
                        SurfaceRules.ifTrue(SurfaceRules.steep(), packedIce)),
                SurfaceRules.ifTrue(SurfaceRules.isBiome(biomes, biomeKey("ice_crystal_basin"), biomeKey("silver_frost_basin")),
                        SurfaceRules.ifTrue(SurfaceRules.steep(), stone)),
                SurfaceRules.ifTrue(SurfaceRules.steep(), stone),
                SurfaceRules.ifTrue(SurfaceRules.noiseCondition2d(Noises.SURFACE, -0.54, -0.48), dirt),
                grass);
        var surface = SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(), SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, top),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SurfaceRules.sequence(
                                SurfaceRules.ifTrue(SurfaceRules.isBiome(biomes,
                                        biomeKey("blue_ice_plateau")), SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(SurfaceRules.noiseCondition2d(Noises.SURFACE_SECONDARY, 0.35), packedIce), stone)),
                                dirt)))),
                SurfaceRuleData.overworld(context.lookup(Registries.BIOME)));
        context.register(TERRAIN, new NoiseGeneratorSettings(
                NoiseSettings.create(-64, 384, 1, 2), Blocks.STONE.defaultBlockState(),
                Blocks.WATER.defaultBlockState(), router, surface,
                new OverworldBiomeBuilder().spawnTarget(), 63, false, false, true, false));
    }

    public static NoiseGeneratorSettings tunedNoiseSettings(HolderGetter.Provider registries, TerrainPack pack) {
        NoiseGeneratorSettings base = registries.lookupOrThrow(Registries.NOISE_SETTINGS)
                .getOrThrow(TERRAIN).value();
        HolderGetter<NormalNoise.NoiseParameters> noises = registries.lookupOrThrow(Registries.NOISE);
        NoiseRouter vanilla = OverworldRouter.create(registries.lookupOrThrow(Registries.DENSITY_FUNCTION), noises);
        NoiseRouter router = shapedRouter(vanilla, IcyTerrainDensity.create(noises, pack));
        return new NoiseGeneratorSettings(base.noiseSettings(), base.defaultBlock(), base.defaultFluid(),
                router, base.surfaceRule(), base.spawnTarget(), base.seaLevel(), base.disableMobGeneration(),
                base.aquifersEnabled(), base.oreVeinsEnabled(), base.useLegacyRandomSource());
    }

    private static NoiseRouter shapedRouter(NoiseRouter vanilla, IcyTerrainDensity.Terrain terrain) {
        DensityFunction ground = DensityFunctions.interpolated(DensityFunctions.add(
                DensityFunctions.yClampedGradient(-64, 320, 150.0, -234.0),
                DensityFunctions.add(terrain.surfaceY(), DensityFunctions.constant(-86.0))));
        return new NoiseRouter(
                vanilla.barrierNoise(), vanilla.fluidLevelFloodednessNoise(), vanilla.fluidLevelSpreadNoise(),
                vanilla.lavaNoise(), terrain.region(), vanilla.vegetation(), vanilla.continents(),
                vanilla.erosion(), vanilla.depth(), vanilla.ridges(), terrain.surfaceY(), DensityFunctions.min(ground, DensityFunctions.interpolated(terrain.caves())),
                vanilla.veinToggle(), vanilla.veinRidged(), vanilla.veinGap());
    }

    private static final class OverworldRouter extends NoiseRouterData {
        private static NoiseRouter create(BootstrapContext<?> context) {
            return create(context.lookup(Registries.DENSITY_FUNCTION), context.lookup(Registries.NOISE));
        }

        private static NoiseRouter create(HolderGetter<DensityFunction> density,
                                          HolderGetter<NormalNoise.NoiseParameters> noises) {
            return overworld(density, noises, false, false);
        }
    }

    static void dimensions(BootstrapContext<LevelStem> context) {
        context.register(DIMENSION, new LevelStem(
                context.lookup(Registries.DIMENSION_TYPE).getOrThrow(BuiltinDimensionTypes.OVERWORLD),
                new NoiseBasedChunkGenerator(biomeSource(context.lookup(Registries.BIOME), null),
                        context.lookup(Registries.NOISE_SETTINGS).getOrThrow(TERRAIN))));
    }

    public static LevelStem tunedDimension(HolderGetter.Provider registries, TerrainPack pack) {
        return new LevelStem(
                registries.lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(BuiltinDimensionTypes.OVERWORLD),
                new NoiseBasedChunkGenerator(biomeSource(registries.lookupOrThrow(Registries.BIOME), pack),
                        Holder.direct(tunedNoiseSettings(registries, pack))));
    }

    private static MultiNoiseBiomeSource biomeSource(HolderGetter<Biome> biomeLookup, TerrainPack pack) {
        Climate.Parameter any = Climate.Parameter.span(-2.0F, 2.0F);
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> entries = new ArrayList<>();
        for (BiomeSpec spec : BIOMES) {
            if (pack != null && pack.biomes().getOrDefault(spec.id(), TerrainProfile.official(spec.id()))
                    .generationChancePercent() == 0) continue;
            entries.add(Pair.of(Climate.parameters(Climate.Parameter.point((float) com.kltyton.eden_realm.world.terrain.IcyLandformDensity.climate(
                            BIOMES.indexOf(spec), BIOMES.size())),
                    any, any, any, any, any, 0.0F), biomeLookup.getOrThrow(spec.key())));
        }
        if (entries.isEmpty()) throw new IllegalArgumentException("At least one biome must be enabled");
        return MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<>(entries));
    }
}
