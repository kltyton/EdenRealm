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
            String materialName = woodMaterialName(name);
            String leafName = treePartName(name, "树叶");
            String saplingName = treePartName(name, "树苗");

            addBlock(blocks.log(), name + "原木");
            addBlock(blocks.wood(), materialName);
            addBlock(blocks.strippedLog(), "去皮" + name + "原木");
            addBlock(blocks.strippedWood(), "去皮" + materialName);
            addBlock(blocks.planks(), materialName + "板");
            addBlock(blocks.stairs(), materialName + "楼梯");
            addBlock(blocks.slab(), materialName + "台阶");
            addBlock(blocks.fence(), materialName + "栅栏");
            addBlock(blocks.fenceGate(), materialName + "栅栏门");
            addBlock(blocks.button(), materialName + "按钮");
            addBlock(blocks.pressurePlate(), materialName + "压力板");
            addBlock(blocks.shelf(), materialName + "架");
            addItem(items.shelf(), materialName + "架");
            addBlock(blocks.leaves(), leafName);
            addBlock(blocks.sapling(), saplingName);
            addBlock(blocks.door(), materialName + "门");
            addBlock(blocks.trapdoor(), name + "活板门");
            addBlock(blocks.sign(), name + "告示牌");
            addBlock(blocks.wallSign(), "墙上的" + name + "告示牌");
            addBlock(blocks.hangingSign(), name + "悬挂告示牌");
            addBlock(blocks.wallHangingSign(), "墙上的" + name + "悬挂告示牌");
            addItem(items.boat(), name + "船");
            addItem(items.chestBoat(), name + "运输船");
        }
    }

    private static String woodMaterialName(String name) {
        if (name.endsWith("树")) {
            return name.substring(0, name.length() - 1) + "木";
        }
        return name.endsWith("木") ? name : name + "木";
    }

    private static String treePartName(String name, String part) {
        return name.endsWith("树") ? name.substring(0, name.length() - 1) + part : name + part;
    }
}
