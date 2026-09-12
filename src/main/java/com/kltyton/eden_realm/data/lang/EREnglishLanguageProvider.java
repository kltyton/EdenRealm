package com.kltyton.eden_realm.data.lang;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.block.ERWoodSet;
import com.kltyton.eden_realm.registry.ERBlocks;
import com.kltyton.eden_realm.registry.ERItems;
import com.kltyton.eden_realm.registry.content.ERBlockEntry;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public final class EREnglishLanguageProvider extends LanguageProvider {
    public EREnglishLanguageProvider(PackOutput output) {
        super(output, ERConstants.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup.eden_realm.eden_realm", "Eden Realm");
        add("entity.eden_realm.moss_stone_colossus", "Moss Stone Colossus");
        addItem(ERItems.MOSS_STONE_COLOSSUS_SPAWN_EGG, "Moss Stone Colossus Spawn Egg");
        addItem(ERItems.TIDE_SONG_COCONUT, "Tide Song Coconut");
        addItem(ERItems.SACRED_LIGHT_FRUIT, "Sacred Light Fruit");
        addItem(ERItems.CLOUD_CROWN_FRUIT, "Cloud Crown Fruit");
        addItem(ERItems.TWILIGHT_POMEGRANATE, "Twilight Pomegranate");
        addItem(ERItems.DEWSPIKE_GRAIN, "Dewspike Grain");
        addItem(ERItems.DEWSPIKE_GRAIN_SEEDS, "Dewspike Grain Seeds");
        add("subtitles.eden_realm.entity.moss_stone_colossus.step", "Moss Stone Colossus steps");
        add("subtitles.eden_realm.entity.moss_stone_colossus.ambient", "Moss Stone Colossus rumbles");
        add("subtitles.eden_realm.entity.moss_stone_colossus.hurt", "Moss Stone Colossus hurts");
        add("subtitles.eden_realm.entity.moss_stone_colossus.death", "Moss Stone Colossus dies");

        for (ERWoodSet wood : ERWoodSet.values()) {
            ERBlocks.WoodBlocks blocks = ERBlocks.woodBlocks(wood);
            ERItems.WoodItems items = ERItems.woodItems(wood);
            String name = wood.englishName();

            addBlock(blocks.log(), name + " Log");
            addBlock(blocks.wood(), name + " Wood");
            addBlock(blocks.strippedLog(), "Stripped " + name + " Log");
            addBlock(blocks.strippedWood(), "Stripped " + name + " Wood");
            addBlock(blocks.planks(), name + " Planks");
            addBlock(blocks.stairs(), name + " Stairs");
            addBlock(blocks.slab(), name + " Slab");
            addBlock(blocks.fence(), name + " Fence");
            addBlock(blocks.fenceGate(), name + " Fence Gate");
            addBlock(blocks.button(), name + " Button");
            addBlock(blocks.pressurePlate(), name + " Pressure Plate");
            addBlock(blocks.shelf(), name + " Shelf");
            addItem(items.shelf(), name + " Shelf");
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

        for (ERBlockEntry entry : ERBlocks.contentEntries()) {
            addBlock(entry.block(), entry.englishName());
        }
    }
}
