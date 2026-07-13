package com.kltyton.eden_realm.data.model;

import com.kltyton.eden_realm.client.color.ERGrassColorSource;
import com.kltyton.eden_realm.registry.content.ERTerrainBlocks;
import com.mojang.math.Quadrant;
import java.util.List;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.resources.Identifier;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.Block;

final class ERGrassModelGenerator {
    private static final List<GrassModel> MODELS = List.of(
            new GrassModel("", "eden_grass_block_top_1", null, 120),
            new GrassModel("_top_2", "eden_grass_block_top_2", null, 8),
            new GrassModel("_top_3", "eden_grass_block_top_3", null, 12),
            new GrassModel("_decoration_1", "eden_grass_block_top_1", "eden_grass_block_decoration_1", 1),
            new GrassModel("_decoration_2", "eden_grass_block_top_1", "eden_grass_block_decoration_2", 4),
            new GrassModel("_decoration_3", "eden_grass_block_top_1", "eden_grass_block_decoration_3", 2));

    private ERGrassModelGenerator() {
    }

    static void generate(BlockModelGenerators blockModels) {
        Block block = ERTerrainBlocks.EDEN_GRASS_BLOCK.get();
        Identifier baseModel = ModelLocationUtils.getModelLocation(block);
        WeightedList.Builder<Variant> weightedModels = WeightedList.builder();

        for (GrassModel grassModel : MODELS) {
            Identifier model = grassModel.suffix().isEmpty()
                    ? baseModel
                    : ModelLocationUtils.getModelLocation(block, grassModel.suffix());
            blockModels.modelOutput.accept(
                    model,
                    () -> ERCustomBlockModels.grass(grassModel.topTexture(), grassModel.decorationTexture()));
            addRotations(weightedModels, model, grassModel.weight());
        }

        TextureMapping snowyTextures = new TextureMapping()
                .put(TextureSlot.PARTICLE, ERContentModelGenerators.material("eden_dirt"))
                .put(TextureSlot.BOTTOM, ERContentModelGenerators.material("eden_dirt"))
                .put(TextureSlot.TOP, ERContentModelGenerators.material("eden_grass_block_top_1"))
                .put(TextureSlot.SIDE, ERContentModelGenerators.material("eden_grass_block_snow"));
        Identifier snowyModel = ModelTemplates.CUBE_BOTTOM_TOP.createWithSuffix(
                block, "_snow", snowyTextures, blockModels.modelOutput);

        blockModels.createGrassLikeBlock(
                block,
                new MultiVariant(weightedModels.build()),
                BlockModelGenerators.plainVariant(snowyModel));
        blockModels.registerSimpleTintedItemModel(block, baseModel, new ERGrassColorSource());
    }

    private static void addRotations(
            WeightedList.Builder<Variant> variants,
            Identifier model,
            int weight) {
        variants.add(new Variant(model), weight);
        variants.add(new Variant(model).withYRot(Quadrant.R90), weight);
        variants.add(new Variant(model).withYRot(Quadrant.R180), weight);
        variants.add(new Variant(model).withYRot(Quadrant.R270), weight);
    }

    private record GrassModel(
            String suffix,
            String topTexture,
            String decorationTexture,
            int weight) {
    }
}
