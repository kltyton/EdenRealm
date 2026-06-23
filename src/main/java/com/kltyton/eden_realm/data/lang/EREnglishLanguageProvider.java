package com.kltyton.eden_realm.data.lang;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.block.ERWoodSet;
import com.kltyton.eden_realm.registry.ERBlocks;
import com.kltyton.eden_realm.registry.ERItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public final class EREnglishLanguageProvider extends LanguageProvider {
    public EREnglishLanguageProvider(PackOutput output) {
        super(output, ERConstants.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup.eden_realm.eden_realm", "Eden Realm");

        for (ERWoodSet wood : ERWoodSet.values()) {
            ERBlocks.WoodBlocks blocks = ERBlocks.woodBlocks(wood);
            ERItems.WoodItems items = ERItems.woodItems(wood);
            String name = wood.englishName();

            addBlock(blocks.log(), name + " Log");
            addBlock(blocks.strippedLog(), "Stripped " + name + " Log");
            addBlock(blocks.planks(), name + " Planks");
            addBlock(blocks.stairs(), name + " Stairs");
            addBlock(blocks.slab(), name + " Slab");
            addBlock(blocks.fence(), name + " Fence");
            addBlock(blocks.fenceGate(), name + " Fence Gate");
            addBlock(blocks.button(), name + " Button");
            addBlock(blocks.pressurePlate(), name + " Pressure Plate");
            addBlock(blocks.leaves(), name + " Leaves");
            addBlock(blocks.sapling(), name + " Sapling");
            addBlock(blocks.door(), name + " Door");
            addBlock(blocks.trapdoor(), name + " Trapdoor");
            addBlock(blocks.sign(), name + " Sign");
            addBlock(blocks.wallSign(), name + " Wall Sign");
            addBlock(blocks.hangingSign(), name + " Hanging Sign");
            addBlock(blocks.wallHangingSign(), name + " Wall Hanging Sign");
            addItem(items.boat(), name + " Boat");
            addItem(items.chestBoat(), name + " Chest Boat");
        }
    }
}
