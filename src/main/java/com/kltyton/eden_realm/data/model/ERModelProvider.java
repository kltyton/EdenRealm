package com.kltyton.eden_realm.data.model;

import com.kltyton.eden_realm.data.model.block.ERContentModelGenerators;
import com.kltyton.eden_realm.data.model.block.ERHarvestModelGenerator;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.data.model.output.ERCategorizedModelOutput;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import com.kltyton.eden_realm.common.block.tree.ERWoodSet;
import com.kltyton.eden_realm.registry.ERBlocks;
import com.kltyton.eden_realm.registry.ERItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.resources.Identifier;
import net.minecraft.util.random.WeightedList;
import net.minecraft.data.BlockFamily;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.NonNull;

public final class ERModelProvider extends ModelProvider {
    private final PackOutput output;

    public ERModelProvider(PackOutput output) {
        super(output, ERConstants.MOD_ID);
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(@NonNull CachedOutput cache) {
        return super.run(new ERCategorizedModelOutput(output, cache));
    }

    @Override
    protected void registerModels(@NonNull BlockModelGenerators blockModels, @NonNull ItemModelGenerators itemModels) {
        for (ERWoodSet wood : ERWoodSet.values()) {
            ERBlocks.WoodBlocks blocks = ERBlocks.woodBlocks(wood);
            ERItems.WoodItems items = ERItems.woodItems(wood);

            blockModels.woodProvider(blocks.log().get()).log(blocks.log().get()).wood(blocks.wood().get());
            blockModels.woodProvider(blocks.strippedLog().get()).log(blocks.strippedLog().get()).wood(blocks.strippedWood().get());
            if (wood == ERWoodSet.HONEY_MAPLE) createHoneyMapleLeaves(blockModels, blocks.leaves().get());
            else createCubeWithItem(blockModels, blocks.leaves().get(), TexturedModel.LEAVES);
            blockModels.createDoor(blocks.door().get());
            blockModels.createShelf(blocks.shelf().get(), blocks.strippedLog().get());
            blockModels.createCrossBlockWithDefaultItem(blocks.sapling().get(), BlockModelGenerators.PlantType.NOT_TINTED);

            BlockFamily woodFamily = new BlockFamily.Builder(blocks.planks().get())
                    .strippedLog(blocks.strippedLog().get())
                    .stairs(blocks.stairs().get())
                    .slab(blocks.slab().get())
                    .fence(blocks.fence().get())
                    .fenceGate(blocks.fenceGate().get())
                    .button(blocks.button().get())
                    .pressurePlate(blocks.pressurePlate().get())
                    .trapdoor(blocks.trapdoor().get())
                    .sign(blocks.sign().get(), blocks.wallSign().get())
                    .hangingSign(blocks.hangingSign().get(), blocks.wallHangingSign().get())
                    .getFamily();
            blockModels.family(blocks.planks().get()).generateFor(woodFamily);
            blockModels.registerSimpleItemModel(blocks.planks().get(), ModelLocationUtils.getModelLocation(blocks.planks().get()));

            itemModels.generateFlatItem(items.boat().get(), ModelTemplates.FLAT_ITEM);
            itemModels.generateFlatItem(items.chestBoat().get(), ModelTemplates.FLAT_ITEM);
        }

        itemModels.generateFlatItem(ERItems.MOSS_STONE_COLOSSUS_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ERItems.PLAINS_VILLAGER_SPAWN_EGG.get(), net.minecraft.world.item.Items.VILLAGER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);

        ERContentModelGenerators.generate(blockModels);
        ERHarvestModelGenerator.generate(blockModels, itemModels);
        for (var entry : com.kltyton.eden_realm.registry.content.item.ERToolItems.entries()) {
            itemModels.generateFlatItem(entry.item().get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        }
    }

    private static void createHoneyMapleLeaves(BlockModelGenerators blockModels, Block block) {
        Identifier orange = TexturedModel.LEAVES.create(block, blockModels.modelOutput);
        Identifier red = ModelTemplates.LEAVES.createWithSuffix(block, "_red",
                TextureMapping.cube(TextureMapping.getBlockTexture(block, "_red")), blockModels.modelOutput);
        var variants = WeightedList.<Variant>builder().add(new Variant(orange), 1).add(new Variant(red), 1).build();
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, new MultiVariant(variants)));
        blockModels.registerSimpleItemModel(block, orange);
    }

    private static void createCubeWithItem(BlockModelGenerators blockModels, Block block, TexturedModel.Provider modelProvider) {
        blockModels.createTrivialBlock(block, modelProvider);
        blockModels.registerSimpleItemModel(block, ModelLocationUtils.getModelLocation(block));
    }
}
