package com.kltyton.eden_realm.data.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.block.plant.ERDewspikeGrainBlock;
import com.kltyton.eden_realm.common.block.plant.ERHangingFruitBlock;
import com.kltyton.eden_realm.registry.ERItems;
import com.kltyton.eden_realm.registry.content.ERHarvestBlocks;
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

final class ERHarvestModelGenerator {
    private ERHarvestModelGenerator() {
    }

    static void generate(BlockModelGenerators blocks, ItemModelGenerators items) {
        for (var holder : ERHarvestBlocks.fruits()) {
            Block block = holder.get();
            String fruit = holder.getId().getPath().replace("_hanging", "");
            blocks.blockStateOutput.accept(MultiVariantGenerator.dispatch(block)
                    .with(PropertyDispatch.initial(ERHangingFruitBlock.AGE)
                            .generate(age -> BlockModelGenerators.plainVariant(model(fruit + "_stage_" + age)))));
            blocks.registerSimpleItemModel(block, model(fruit + "_stage_0"));
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
            if (item != ERItems.SACRED_LIGHT_FRUIT) {
                items.generateFlatItem(item.get(), ModelTemplates.FLAT_ITEM);
            }
        }
        blocks.registerSimpleItemModel(ERItems.SACRED_LIGHT_FRUIT.get(), model("sacred_light_fruit_stage_4"));
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
