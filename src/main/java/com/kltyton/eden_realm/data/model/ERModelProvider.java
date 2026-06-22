package com.kltyton.eden_realm.data.model;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.block.ERWoodSet;
import com.kltyton.eden_realm.registry.ERBlocks;
import com.kltyton.eden_realm.registry.ERItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.data.BlockFamily;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;

public final class ERModelProvider extends ModelProvider {
    public ERModelProvider(PackOutput output) {
        super(output, ERConstants.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        for (ERWoodSet wood : ERWoodSet.values()) {
            ERBlocks.WoodBlocks blocks = ERBlocks.woodBlocks(wood);
            ERItems.WoodItems items = ERItems.woodItems(wood);

            blockModels.woodProvider(blocks.log().get()).log(blocks.log().get());
            blockModels.woodProvider(blocks.strippedLog().get()).log(blocks.strippedLog().get());
            createCubeWithItem(blockModels, blocks.leaves().get(), TexturedModel.LEAVES);
            blockModels.createDoor(blocks.door().get());
            blockModels.createTrapdoor(blocks.trapdoor().get());
            blockModels.createCrossBlockWithDefaultItem(blocks.sapling().get(), BlockModelGenerators.PlantType.NOT_TINTED);

            BlockFamily signFamily = new BlockFamily.Builder(blocks.planks().get())
                    .strippedLog(blocks.strippedLog().get())
                    .sign(blocks.sign().get(), blocks.wallSign().get())
                    .hangingSign(blocks.hangingSign().get(), blocks.wallHangingSign().get())
                    .getFamily();
            blockModels.family(blocks.planks().get()).generateFor(signFamily);
            blockModels.registerSimpleItemModel(blocks.planks().get(), ModelLocationUtils.getModelLocation(blocks.planks().get()));

            itemModels.generateFlatItem(items.boat().get(), ModelTemplates.FLAT_ITEM);
            itemModels.generateFlatItem(items.chestBoat().get(), ModelTemplates.FLAT_ITEM);
        }
    }

    private static void createCubeWithItem(BlockModelGenerators blockModels, Block block, TexturedModel.Provider modelProvider) {
        blockModels.createTrivialBlock(block, modelProvider);
        blockModels.registerSimpleItemModel(block, ModelLocationUtils.getModelLocation(block));
    }
}
