package com.kltyton.eden_realm.data.model;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.registry.content.ERPlantBlocks;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.resources.Identifier;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.Block;

final class ERPlantModelGenerator {
    private ERPlantModelGenerator() {
    }

    static void generate(BlockModelGenerators blockModels) {
        createTall(blockModels, ERPlantBlocks.GOLDEN_SPIKE_GRASS.get(), "golden_spike_grass");
        createTall(blockModels, ERPlantBlocks.PURPLE_GLOW_CATTAIL.get(), "purple_glow_cattail");
        createTallVariants(blockModels, ERPlantBlocks.GRAY_SPIKE_REED.get(), "gray_spike_reed", 3);
        createTall(blockModels, ERPlantBlocks.WATER_SCALLION.get(), "water_scallion");
        createTall(blockModels, ERPlantBlocks.UMBRELLA_HYGROPHILA.get(), "umbrella_hygrophila");

        createRandomFungus(blockModels, ERPlantBlocks.SMALL_PARASOL_MUSHROOM.get(), "small_parasol_mushroom", 3);
        createRandomFungus(blockModels, ERPlantBlocks.CRUMBLY_MUSHROOM.get(), "crumbly_mushroom", 3);
        createRandomFungus(blockModels, ERPlantBlocks.BLUE_GLOW_MUSHROOM.get(), "blue_glow_mushroom", 3);
    }

    private static void createTall(BlockModelGenerators blockModels, Block block, String modelName) {
        Identifier bottom = model(modelName + "_bottom");
        Identifier top = model(modelName + "_top");
        blockModels.createDoubleBlock(
                block,
                BlockModelGenerators.plainVariant(top),
                BlockModelGenerators.plainVariant(bottom));
        blockModels.registerSimpleFlatItemModel(block, "_item");
    }

    private static void createTallVariants(
            BlockModelGenerators blockModels,
            Block block,
            String modelName,
            int variantCount) {
        Identifier[] bottomModels = new Identifier[variantCount];
        Identifier[] topModels = new Identifier[variantCount];
        for (int index = 0; index < variantCount; index++) {
            String suffix = "_" + (index + 1);
            bottomModels[index] = model(modelName + suffix + "_bottom");
            topModels[index] = model(modelName + suffix + "_top");
        }
        blockModels.createDoubleBlock(block, weighted(topModels), weighted(bottomModels));
        blockModels.registerSimpleFlatItemModel(block, "_item");
    }

    private static void createRandomFungus(
            BlockModelGenerators blockModels,
            Block block,
            String modelName,
            int variantCount) {
        Identifier[] models = new Identifier[variantCount];
        for (int index = 0; index < variantCount; index++) {
            String suffix = "_" + (index + 1);
            models[index] = model(modelName + suffix);
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, weighted(models)));
        blockModels.registerSimpleFlatItemModel(block, "_item");
    }

    private static MultiVariant weighted(Identifier... models) {
        List<Weighted<Variant>> variants = Arrays.stream(models)
                .map(model -> new Weighted<>(new Variant(model), 1))
                .toList();
        return new MultiVariant(WeightedList.of(variants));
    }

    private static Identifier model(String name) {
        return ERConstants.id("block/" + name);
    }
}
