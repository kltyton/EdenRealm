package com.kltyton.eden_realm.data.model;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.registry.content.ERCoralBlocks;
import com.kltyton.eden_realm.registry.content.ERPlantBlocks;
import com.kltyton.eden_realm.registry.content.ERSkyBlocks;
import com.kltyton.eden_realm.registry.content.ERTerrainBlocks;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

final class ERContentModelGenerators {
    private static final ModelTemplate DIRT_PATH_TEMPLATE = new ModelTemplate(
            Optional.of(Identifier.withDefaultNamespace("block/dirt_path")),
            Optional.empty(),
            TextureSlot.PARTICLE,
            TextureSlot.BOTTOM,
            TextureSlot.TOP,
            TextureSlot.SIDE);

    private ERContentModelGenerators() {
    }

    static void generate(BlockModelGenerators blockModels) {
        generateTerrain(blockModels);
        generateSky(blockModels);
        generatePlants(blockModels);
        generateCorals(blockModels);
    }

    private static void generateTerrain(BlockModelGenerators blockModels) {
        List.of(
                        ERTerrainBlocks.WEATHERED_ROCK,
                        ERTerrainBlocks.ROOTED_ROCK,
                        ERTerrainBlocks.BOUNDARY_ROCK,
                        ERTerrainBlocks.RUBBLE,
                        ERTerrainBlocks.TUNDRA_ROCK,
                        ERTerrainBlocks.SHALE,
                        ERTerrainBlocks.RAW_ROCK,
                        ERTerrainBlocks.CHISELED_RAW_ROCK_BRICKS,
                        ERTerrainBlocks.MOSSY_RAW_ROCK_BRICKS,
                        ERTerrainBlocks.RAW_ROCK_BRICKS,
                        ERTerrainBlocks.RAW_ROCK_COAL_ORE,
                        ERTerrainBlocks.RAW_ROCK_IRON_ORE,
                        ERTerrainBlocks.EDEN_DIRT,
                        ERTerrainBlocks.THIN_CLOUD_SOIL,
                        ERTerrainBlocks.FLOATING_ISLAND_ROCK,
                        ERTerrainBlocks.SKY_PLATFORM_STONE,
                        ERTerrainBlocks.ICE_CRYSTAL_ROCK,
                        ERTerrainBlocks.FROST_PATTERN_STONE,
                        ERTerrainBlocks.SEDIMENTARY_SILT,
                        ERTerrainBlocks.PEAT_BLOCK,
                        ERTerrainBlocks.WET_SWAMP_SOIL,
                        ERTerrainBlocks.AMBER_CRYSTAL_BLOCK,
                        ERTerrainBlocks.SUN_ROCK,
                        ERTerrainBlocks.ERODED_SANDSTONE)
                .forEach(holder -> createCubeWithItem(blockModels, holder.get()));

        blockModels.woodProvider(ERTerrainBlocks.MOSSY_RAW_ROCK_PILLAR.get())
                .log(ERTerrainBlocks.MOSSY_RAW_ROCK_PILLAR.get());

        ERGrassModelGenerator.generate(blockModels);
        generateDirtPath(blockModels);
        generateFarmland(blockModels);

        generateSandstoneSet(blockModels, "coast", ERTerrainBlocks.COAST);
        generateSandstoneSet(blockModels, "amber", ERTerrainBlocks.AMBER);
        generateSandstoneSet(blockModels, "oasis", ERTerrainBlocks.OASIS);
        generateTopBottom(
                blockModels,
                ERTerrainBlocks.SPRING_STONE.get(),
                "spring_stone",
                "spring_stone_top",
                "spring_stone_top");
    }

    private static void generateSky(BlockModelGenerators blockModels) {
        List.of(
                        ERSkyBlocks.CLOUD_COURT_STONE,
                        ERSkyBlocks.CLOUD,
                        ERSkyBlocks.SKY_POOL_STONE,
                        ERSkyBlocks.DENSE_CLOUD,
                        ERSkyBlocks.ROSY_CLOUD,
                        ERSkyBlocks.DENSE_ROSY_CLOUD)
                .forEach(holder -> createCubeWithItem(blockModels, holder.get()));

        blockModels.woodProvider(ERSkyBlocks.CLOUD_COURT_STONE_PILLAR.get())
                .log(ERSkyBlocks.CLOUD_COURT_STONE_PILLAR.get());

        List.of(
                        ERSkyBlocks.CLOUD_EDGE_GRASS,
                        ERSkyBlocks.SKY_WIND_GRASS,
                        ERSkyBlocks.CLOUD_CROWN_FLOWER,
                        ERSkyBlocks.CLOUD_FLEECE_VINE)
                .forEach(holder -> blockModels.createCrossBlockWithDefaultItem(
                        holder.get(), BlockModelGenerators.PlantType.NOT_TINTED));
        generateTallSkyWindGrass(blockModels);
    }

    private static void generateTallSkyWindGrass(BlockModelGenerators blockModels) {
        Block tallBlock = ERSkyBlocks.TALL_SKY_WIND_GRASS.get();
        Identifier bottomModel = ModelTemplates.CROSS.create(
                ModelLocationUtils.getModelLocation(tallBlock, "_bottom"),
                TextureMapping.cross(material("tall_sky_wind_grass_bottom")),
                blockModels.modelOutput);
        Identifier topModel = ModelTemplates.CROSS.create(
                ModelLocationUtils.getModelLocation(tallBlock, "_top"),
                TextureMapping.cross(material("tall_sky_wind_grass_top")),
                blockModels.modelOutput);
        blockModels.createDoubleBlock(
                tallBlock,
                BlockModelGenerators.plainVariant(topModel),
                BlockModelGenerators.plainVariant(bottomModel));
    }

    private static void generateDirtPath(BlockModelGenerators blockModels) {
        Block block = ERTerrainBlocks.EDEN_DIRT_PATH.get();
        TextureMapping textures = new TextureMapping()
                .put(TextureSlot.PARTICLE, material("eden_dirt"))
                .put(TextureSlot.BOTTOM, material("eden_dirt"))
                .put(TextureSlot.TOP, material("eden_dirt_path_top"))
                .put(TextureSlot.SIDE, material("eden_dirt_path_side"));
        Identifier model = DIRT_PATH_TEMPLATE.create(block, textures, blockModels.modelOutput);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(
                block, BlockModelGenerators.createRotatedVariants(BlockModelGenerators.plainModel(model))));
        blockModels.registerSimpleItemModel(block, model);
    }

    private static void generateFarmland(BlockModelGenerators blockModels) {
        Block block = ERTerrainBlocks.EDEN_FARMLAND.get();
        TextureMapping dryTextures = new TextureMapping()
                .put(TextureSlot.DIRT, material("eden_dirt"))
                .put(TextureSlot.TOP, material("eden_farmland"));
        TextureMapping moistTextures = new TextureMapping()
                .put(TextureSlot.DIRT, material("eden_dirt"))
                .put(TextureSlot.TOP, material("eden_farmland_moist"));
        MultiVariant dry = BlockModelGenerators.plainVariant(
                ModelTemplates.FARMLAND.create(block, dryTextures, blockModels.modelOutput));
        MultiVariant moist = BlockModelGenerators.plainVariant(ModelTemplates.FARMLAND.create(
                ModelLocationUtils.getModelLocation(block, "_moist"), moistTextures, blockModels.modelOutput));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block)
                .with(BlockModelGenerators.createEmptyOrFullDispatch(
                        BlockStateProperties.MOISTURE, 7, moist, dry)));
        blockModels.registerSimpleItemModel(block, ModelLocationUtils.getModelLocation(block));
    }

    private static void generateSandstoneSet(
            BlockModelGenerators blockModels,
            String prefix,
            ERTerrainBlocks.SandstoneSet set) {
        createCubeWithItem(blockModels, set.sand().get());
        generateTopBottom(
                blockModels,
                set.sandstone().get(),
                prefix + "_sandstone",
                "smooth_" + prefix + "_sandstone",
                prefix + "_sandstone_bottom");
        createCubeWithItem(blockModels, set.smoothSandstone().get());
        String endTexture = "smooth_" + prefix + "_sandstone";
        generateTopBottom(
                blockModels,
                set.cutSandstone().get(),
                "cut_" + prefix + "_sandstone",
                endTexture,
                endTexture);
        generateTopBottom(
                blockModels,
                set.chiseledSandstone().get(),
                "chiseled_" + prefix + "_sandstone",
                endTexture,
                endTexture);
    }

    private static void generateTopBottom(
            BlockModelGenerators blockModels,
            Block block,
            String sideTexture,
            String topTexture,
            String bottomTexture) {
        TextureMapping textures = new TextureMapping()
                .put(TextureSlot.PARTICLE, material(sideTexture))
                .put(TextureSlot.SIDE, material(sideTexture))
                .put(TextureSlot.TOP, material(topTexture))
                .put(TextureSlot.BOTTOM, material(bottomTexture));
        Identifier model = ModelTemplates.CUBE_BOTTOM_TOP.create(block, textures, blockModels.modelOutput);
        blockModels.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(
                block, BlockModelGenerators.plainVariant(model)));
        blockModels.registerSimpleItemModel(block, model);
    }

    private static void generatePlants(BlockModelGenerators blockModels) {
        List.of(
                        ERPlantBlocks.FROST_CRYSTAL_GRASS,
                        ERPlantBlocks.FROST_DOWN_FLOWER,
                        ERPlantBlocks.DUSKY_PURPLE_FOREST_FLOWER,
                        ERPlantBlocks.SPIKE_GRASS_FLOWER,
                        ERPlantBlocks.MOSSBORN_FLOWER,
                        ERPlantBlocks.BLUEBELL,
                        ERPlantBlocks.GOLDEN_STAMEN_FLOWER,
                        ERPlantBlocks.MOONWHITE_ORCHID,
                        ERPlantBlocks.LONGLEAF_SEDGE,
                        ERPlantBlocks.GREEN_SPIKE_GRASS,
                        ERPlantBlocks.DROUGHT_RESISTANT_SHORT_GRASS,
                        ERPlantBlocks.THORN_BRANCH_BUSH,
                        ERPlantBlocks.SANDLAND_SHORT_GRASS)
                .forEach(holder -> blockModels.createCrossBlockWithDefaultItem(
                        holder.get(), BlockModelGenerators.PlantType.NOT_TINTED));

        generateDuckweed(blockModels);
        blockModels.createTrivialBlock(ERPlantBlocks.BUBBLE_GRASS.get(), TexturedModel.SEAGRASS);
        blockModels.registerSimpleFlatItemModel(ERPlantBlocks.BUBBLE_GRASS.get());
        blockModels.createTrivialBlock(ERPlantBlocks.BLUE_COURT_SEAGRASS.get(), TexturedModel.SEAGRASS);
        blockModels.registerSimpleFlatItemModel(ERPlantBlocks.BLUE_COURT_SEAGRASS.get());
        blockModels.createTrivialBlock(ERPlantBlocks.WATER_FERN.get(), TexturedModel.SEAGRASS);
        blockModels.registerSimpleFlatItemModel(ERPlantBlocks.WATER_FERN.get());
        generateBlueCourtSeagrass(blockModels);
        blockModels.createTrivialBlock(ERPlantBlocks.ROTTING_WOOD_FUNGUS_MAT.get(), TexturedModel.CARPET);
        blockModels.registerSimpleItemModel(
                ERPlantBlocks.ROTTING_WOOD_FUNGUS_MAT.get(),
                ModelLocationUtils.getModelLocation(ERPlantBlocks.ROTTING_WOOD_FUNGUS_MAT.get()));
        ERPlantModelGenerator.generate(blockModels);
    }

    private static void generateBlueCourtSeagrass(BlockModelGenerators blockModels) {
        Block tallBlock = ERPlantBlocks.TALL_BLUE_COURT_SEAGRASS.get();
        TextureMapping bottomTextures = new TextureMapping()
                .put(TextureSlot.TEXTURE, material("tall_blue_court_seagrass_bottom"));
        TextureMapping topTextures = new TextureMapping()
                .put(TextureSlot.TEXTURE, material("tall_blue_court_seagrass_top"));
        Identifier bottomModel = ModelTemplates.SEAGRASS.create(
                ModelLocationUtils.getModelLocation(tallBlock, "_bottom"),
                bottomTextures,
                blockModels.modelOutput);
        Identifier topModel = ModelTemplates.SEAGRASS.create(
                ModelLocationUtils.getModelLocation(tallBlock, "_top"),
                topTextures,
                blockModels.modelOutput);
        blockModels.createDoubleBlock(
                tallBlock,
                BlockModelGenerators.plainVariant(topModel),
                BlockModelGenerators.plainVariant(bottomModel));
    }

    private static void generateDuckweed(BlockModelGenerators blockModels) {
        Block duckweed = ERPlantBlocks.DUCKWEED.get();
        Identifier model = ModelLocationUtils.getModelLocation(duckweed);
        blockModels.modelOutput.accept(model, ERCustomBlockModels::duckweed);
        blockModels.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(
                duckweed, BlockModelGenerators.plainVariant(model)));
        blockModels.registerSimpleFlatItemModel(duckweed);
    }

    private static void generateCorals(BlockModelGenerators blockModels) {
        for (ERCoralBlocks.CoralFamily family : ERCoralBlocks.families()) {
            blockModels.createCoral(
                    family.plant().get(),
                    family.deadPlant().get(),
                    family.block().get(),
                    family.deadBlock().get(),
                    family.fan().get(),
                    family.deadFan().get(),
                    family.wallFan().get(),
                    family.deadWallFan().get());
        }
    }

    private static void createCubeWithItem(BlockModelGenerators blockModels, Block block) {
        blockModels.createTrivialBlock(block, TexturedModel.CUBE);
        blockModels.registerSimpleItemModel(block, ModelLocationUtils.getModelLocation(block));
    }

    static Material material(String texture) {
        return new Material(ERConstants.id("block/" + texture));
    }

}
