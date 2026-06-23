package com.kltyton.eden_realm.data.lang;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.block.ERWoodSet;
import com.kltyton.eden_realm.registry.ERBlocks;
import com.kltyton.eden_realm.registry.ERItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public final class ERChineseLanguageProvider extends LanguageProvider {
    public ERChineseLanguageProvider(PackOutput output) {
        super(output, ERConstants.MOD_ID, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup.eden_realm.eden_realm", "伊甸之境");

        for (ERWoodSet wood : ERWoodSet.values()) {
            ERBlocks.WoodBlocks blocks = ERBlocks.woodBlocks(wood);
            ERItems.WoodItems items = ERItems.woodItems(wood);
            String name = wood.chineseName();

            addBlock(blocks.log(), name + "原木");
            addBlock(blocks.wood(), name + "木");
            addBlock(blocks.strippedLog(), "去皮" + name + "原木");
            addBlock(blocks.strippedWood(), "去皮" + name + "木");
            addBlock(blocks.planks(), name + "木板");
            addBlock(blocks.stairs(), name + "木楼梯");
            addBlock(blocks.slab(), name + "木台阶");
            addBlock(blocks.fence(), name + "木栅栏");
            addBlock(blocks.fenceGate(), name + "木栅栏门");
            addBlock(blocks.button(), name + "木按钮");
            addBlock(blocks.pressurePlate(), name + "木压力板");
            addBlock(blocks.shelf(), name + "木架");
            addBlock(blocks.leaves(), name + "树叶");
            addBlock(blocks.sapling(), name + "树苗");
            addBlock(blocks.door(), name + "木门");
            addBlock(blocks.trapdoor(), name + "活板门");
            addBlock(blocks.sign(), name + "告示牌");
            addBlock(blocks.wallSign(), "墙上的" + name + "告示牌");
            addBlock(blocks.hangingSign(), name + "悬挂告示牌");
            addBlock(blocks.wallHangingSign(), "墙上的" + name + "悬挂告示牌");
            addItem(items.boat(), name + "船");
            addItem(items.chestBoat(), name + "运输船");
        }
    }
}
