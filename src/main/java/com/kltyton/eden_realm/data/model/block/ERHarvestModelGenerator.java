package com.kltyton.eden_realm.data.model.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.block.crop.ERDewspikeGrainBlock;
import com.kltyton.eden_realm.common.block.crop.ERDoubleCropBlock;
import net.minecraft.world.level.block.CropBlock;
import com.kltyton.eden_realm.common.block.fruit.ERFruitBlock;
import com.kltyton.eden_realm.common.block.fruit.ERHangingFruitBlock;
import com.kltyton.eden_realm.registry.ERItems;
import com.kltyton.eden_realm.registry.content.block.ERHarvestBlocks;
import java.util.List;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.resources.Identifier;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public final class ERHarvestModelGenerator {
    private ERHarvestModelGenerator() {
    }

    private static JsonObject twilightBody(int stage) {
        String path = "/assets/eden_realm/models/block/fruit/twilight_pomegranate_stage_" + stage + ".json";
        try (var stream = ERHarvestModelGenerator.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing authored fruit model: " + path);
            }
            JsonObject model = com.google.gson.JsonParser.parseReader(
                    new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray body = new JsonArray();
            // Only these two twilight stages use zero-thickness planes for decorative leaves.
            for (var value : model.getAsJsonArray("elements")) {
                JsonObject element = value.getAsJsonObject();
                JsonArray from = element.getAsJsonArray("from");
                JsonArray to = element.getAsJsonArray("to");
                if (from.get(0).getAsDouble() != to.get(0).getAsDouble()
                        && from.get(1).getAsDouble() != to.get(1).getAsDouble()
                        && from.get(2).getAsDouble() != to.get(2).getAsDouble()) {
                    body.add(element);
                }
            }
            if (body.isEmpty()) {
                throw new IllegalStateException("Fruit body has no solid model elements: " + path);
            }
            model.add("elements", body);
            return model;
        } catch (java.io.IOException exception) {
            throw new java.io.UncheckedIOException("Cannot read authored fruit model: " + path, exception);
        }
    }

    public static void generate(BlockModelGenerators blocks, ItemModelGenerators items) {
        for (int stage : new int[]{3, 4}) {
            blocks.modelOutput.accept(model("twilight_pomegranate_shape_" + stage), () -> twilightBody(stage));
        }
        for (var holder : ERHarvestBlocks.groundFruits()) {
            Block block = holder.get();
            MultiVariantGenerator definition;
            if (block.defaultBlockState().hasProperty(ERFruitBlock.COUNT)) {
                definition = MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(ERFruitBlock.COUNT)
                        .generate(count -> BlockModelGenerators.plainVariant(model(
                                ERFruitBlock.modelName(block.defaultBlockState().setValue(ERFruitBlock.COUNT, count))))));
            } else {
                definition = MultiVariantGenerator.dispatch(block,
                        BlockModelGenerators.plainVariant(model(ERFruitBlock.modelName(block.defaultBlockState()))));
            }
            blocks.blockStateOutput.accept(definition);
        }
        for (var holder : ERHarvestBlocks.fruits()) {
            Block block = holder.get();
            String fruit = holder.getId().getPath().replace("_hanging", "");
            blocks.blockStateOutput.accept(MultiVariantGenerator.dispatch(block)
                    .with(PropertyDispatch.initial(ERHangingFruitBlock.AGE)
                            .generate(age -> BlockModelGenerators.plainVariant(model(fruit + "_stage_" + age)))));
        }
        for (var holder : ERHarvestBlocks.floweringLeaves()) {
            blocks.createTrivialBlock(holder.get(), TexturedModel.LEAVES);
            blocks.registerSimpleItemModel(holder.get(), ModelLocationUtils.getModelLocation(holder.get()));
        }
        Block grain = ERHarvestBlocks.DEWSPIKE_GRAIN.get();
        MultiVariant mature = new MultiVariant(WeightedList.of(List.of(
                new Weighted<>(new Variant(model("dewspike_grain_stage_7")), 1),
                new Weighted<>(new Variant(model("dewspike_grain_stage_7_tall")), 1))));
        Identifier[] upperStages = new Identifier[8];
        for (int age = 0; age < upperStages.length; age++) {
            upperStages[age] = invisibleUpper(blocks, "dewspike_grain_stage_" + age);
        }
        MultiVariant matureUpper = new MultiVariant(WeightedList.of(List.of(
                new Weighted<>(new Variant(upperStages[7]), 1),
                new Weighted<>(new Variant(invisibleUpper(blocks, "dewspike_grain_stage_7_tall")), 1))));
        blocks.blockStateOutput.accept(MultiVariantGenerator.dispatch(grain)
                .with(PropertyDispatch.initial(ERDewspikeGrainBlock.AGE, DoublePlantBlock.HALF)
                        .generate((age, half) -> half == DoubleBlockHalf.UPPER
                                ? age == 7 ? matureUpper : BlockModelGenerators.plainVariant(upperStages[age])
                                : age == 7 ? mature : BlockModelGenerators.plainVariant(model("dewspike_grain_stage_" + age)))));
        for (var holder : ERHarvestBlocks.gardenCrops()) {
            var crop = holder.get();
            String name = holder.getId().getPath();
            Identifier[] tops = new Identifier[5];
            for (int age = 0; age < 8; age++) {
                int stage = crop.kind().stage(age);
                if (tops[stage] == null) {
                    tops[stage] = invisibleUpper(blocks, name + "_stage_" + stage);
                }
            }
            blocks.blockStateOutput.accept(MultiVariantGenerator.dispatch(crop)
                    .with(PropertyDispatch.initial(ERDoubleCropBlock.AGE, DoublePlantBlock.HALF)
                            .generate((age, half) -> BlockModelGenerators.plainVariant(half == DoubleBlockHalf.UPPER
                                    ? tops[crop.kind().stage(age)] : model(name + "_stage_" + crop.kind().stage(age))))));
        }
        blocks.blockStateOutput.accept(MultiVariantGenerator.dispatch(ERHarvestBlocks.STAR_PATTERN_YAM.get())
                .with(PropertyDispatch.initial(CropBlock.AGE)
                        .generate(age -> BlockModelGenerators.plainVariant(model("star_pattern_yam_stage_" + age * 3 / 7)))));
        for (var holder : ERHarvestBlocks.tallWildPlants()) {
            blocks.createDoubleBlock(holder.get(), BlockModelGenerators.plainVariant(invisibleUpper(blocks, holder.getId().getPath())),
                    BlockModelGenerators.plainVariant(model(holder.getId().getPath())));
            blocks.registerSimpleFlatItemModel(holder.get().asItem());
        }
        Block yam = ERHarvestBlocks.WILD_STAR_PATTERN_YAM.get();
        blocks.blockStateOutput.accept(MultiVariantGenerator.dispatch(yam,
                BlockModelGenerators.plainVariant(model("wild_star_pattern_yam"))));
        blocks.registerSimpleFlatItemModel(yam.asItem());
        for (var item : ERItems.harvestEntries()) {
            items.generateFlatItem(item.get(), ModelTemplates.FLAT_ITEM);
        }
        items.generateFlatItem(ERItems.DEWSPIKE_GRAIN_SEEDS.get(), ModelTemplates.FLAT_ITEM);
    }

    private static Identifier invisibleUpper(BlockModelGenerators blocks, String name) {
        Identifier upper = model(name + "_upper");
        blocks.modelOutput.accept(upper, () -> {
            JsonObject json = new JsonObject();
            json.addProperty("parent", model(name).toString());
            json.add("elements", new JsonArray());
            return json;
        });
        return upper;
    }

    private static Identifier model(String name) {
        return ERConstants.id("block/" + name);
    }
}
