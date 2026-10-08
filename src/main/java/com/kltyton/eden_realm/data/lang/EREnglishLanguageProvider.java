package com.kltyton.eden_realm.data.lang;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.block.tree.ERWoodSet;
import com.kltyton.eden_realm.registry.ERBlocks;
import com.kltyton.eden_realm.registry.ERItems;
import com.kltyton.eden_realm.registry.content.block.ERBlockEntry;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public final class EREnglishLanguageProvider extends LanguageProvider {
    public EREnglishLanguageProvider(PackOutput output) {
        super(output, ERConstants.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup.eden_realm.eden_realm", "Eden Realm");
        add("particle.eden_realm.honey_maple_leaves", "Orange Honey Maple Falling Leaves");
        add("particle.eden_realm.honey_maple_red_leaves", "Red Honey Maple Falling Leaves");
        add("screen.eden_realm.terrain.title", "Eden Terrain Workbench");
        add("screen.eden_realm.terrain.biome", "Select biome");
        add("screen.eden_realm.terrain.preview", "Terrain preview");
        add("screen.eden_realm.terrain.chunks", "Chunks");
        add("screen.eden_realm.terrain.loading", "Generating preview…");
        add("screen.eden_realm.terrain.refining", "Live preview · Refining full chunks…");
        add("screen.eden_realm.terrain.macro", "Overview · Zoom in to load terrain detail around the camera.");
        add("screen.eden_realm.terrain.detailLoading", "Loading terrain detail around the camera…");
        add("screen.eden_realm.terrain.detailReady", "Nearby block detail loaded");
        add("screen.eden_realm.terrain.wholeLoading", "Loading block-resolved terrain across the entire area…");
        add("screen.eden_realm.terrain.wholeReady", "Block-resolved terrain loaded across the entire area");
        add("screen.eden_realm.terrain.realtime", "Terrain updates live; landform features are refining");
        add("screen.eden_realm.terrain.parameters", "Biome parameters");
        add("screen.eden_realm.terrain.seed", "Seed");
        add("screen.eden_realm.terrain.default", "Default");
        add("screen.eden_realm.terrain.reset", "Restore defaults");
        add("screen.eden_realm.terrain.export", "Export ZIP data pack");
        add("screen.eden_realm.terrain.back", "Cancel and exit");
        add("screen.eden_realm.terrain.done", "Save and exit");
        add("screen.eden_realm.terrain.saveTip", "Save the terrain to a temporary data pack and exit. It is installed into this world's datapacks folder and loaded when creating the world. Reopen the workbench to edit the saved draft.");
        add("screen.eden_realm.terrain.cancelTip", "Discard unsaved changes and exit, keeping the last saved data pack. Esc does the same.");
        add("screen.eden_realm.terrain.exportTip", "Choose a destination for a ZIP copy without exiting or saving the workbench draft. Server owners must put the ZIP in the server world's datapacks folder and enable it before world creation. Existing chunks are not regenerated.");
        add("screen.eden_realm.terrain.resetTip", "Restore this biome's default parameters. The change is kept only after Save and exit.");
        add("screen.eden_realm.terrain.left", "Rotate left");
        add("screen.eden_realm.terrain.right", "Rotate right");
        add("screen.eden_realm.terrain.help", "Left drag: pan · Right drag: orbit · Wheel: zoom. Vegetation and structures appear in-world.");
        add("screen.eden_realm.terrain.elevationOffset", "Terrain height");
        add("screen.eden_realm.terrain.reliefPercent", "Relief");
        add("screen.eden_realm.terrain.spacingPercent", "Terrain spacing");
        add("screen.eden_realm.terrain.shapePercent", "Landform strength");
        add("screen.eden_realm.terrain.shoreIceBlocks", "Shore ice width");
        add("screen.eden_realm.terrain.generationChancePercent", "Generation weight");
        add("screen.eden_realm.terrain.biomeSizePercent", "Biome size");
        add("screen.eden_realm.terrain.error", "Action failed: %s");
        add("screen.eden_realm.terrain.exported", "Exported: %s; place it in the target world's datapacks folder and enable it");
        add("screen.eden_realm.terrain.exportCancelled", "Export cancelled; the workbench draft was not saved");
        add("screen.eden_realm.terrain.saving", "Saving terrain draft…");
        add("screen.eden_realm.terrain.exporting", "Choose a ZIP export destination…");
        add("screen.eden_realm.terrain.replace", "The destination ZIP already exists. Replace it?");
        add("screen.eden_realm.terrain.installing", "Preparing the terrain data pack for the new world…");
        add("pack.eden_realm.terrain", "Eden Realm terrain parameters");
        add("biome.eden_realm.icy_rolling_hills", "Icy Blue Rolling Hills");
        add("biome.eden_realm.glacier_meander", "Glacier Meander");
        add("biome.eden_realm.crystal_lake_shore", "Crystal Lake Shore");
        add("biome.eden_realm.blue_ice_plateau", "Blue Ice Plateau");
        add("biome.eden_realm.ice_ridge_valley", "Ice Ridge Valley");
        add("biome.eden_realm.icefall_fjord", "Icefall Fjord");
        add("biome.eden_realm.frozen_fissure", "Frozen Fissure");
        add("biome.eden_realm.cold_spring_lowland", "Cold Spring Lowland");
        add("biome.eden_realm.crystal_stone_plain", "Crystal Stone Plain");
        add("biome.eden_realm.ice_crystal_basin", "Ice Crystal Basin");
        add("biome.eden_realm.silver_frost_hills", "Silver Frost Hills");
        add("biome.eden_realm.frost_stream_valley", "Frost Stream Valley");
        add("biome.eden_realm.silver_frost_lakeshore", "Silver Frost Lakeshore");
        add("biome.eden_realm.frost_rock_plateau", "Frost Rock Plateau");
        add("biome.eden_realm.snow_ridge_valley", "Snow Ridge Valley");
        add("biome.eden_realm.silver_frost_basin", "Silver Frost Basin");
        add("biome.eden_realm.cloud_sea_flatlands", "Cloud Sea Flatlands");
        add("biome.eden_realm.sky_airspace", "Sky Airspace");
        add("biome.eden_realm.star_stream_plateau", "Star Stream Plateau");
        add("biome.eden_realm.sky_mirror_lake", "Sky Mirror Lake Plateau");
        add("biome.eden_realm.cloud_island_chain", "Cloud Island Chain");
        add("biome.eden_realm.rosy_cloud_terraces", "Rosy Cloud Terraces");
        add("biome.eden_realm.flower_mirror_lake", "Flower Mirror Lake");
        add("dimension.eden_realm.sky_layer", "Eden Realm: Sky Layer");
        add("dimension.eden_realm.eden_layer", "Eden Realm: Main Layer");
        add("feature.eden_realm.icy_rolling_hills_ice_pine", "Ice Crystal Pine");
        add("feature.eden_realm.icy_rolling_hills_frost_grass", "Frost Crystal Grass");
        add("feature.eden_realm.icy_crystal_spire", "Ice Crystal Spire");
        add("feature.eden_realm.icy_crystal_pile", "Ice Crystal Cluster");
        add("feature.eden_realm.icy_shore_freeze", "Shore Ice");
        add("feature.eden_realm.frozen_fjord_falls", "Frozen Fjord Falls");
        add("feature.eden_realm.crystal_lake_island", "Crystal Lake Island");
        add("entity.eden_realm.moss_stone_colossus", "Moss Stone Colossus");
        add("entity.eden_realm.plains_villager", "Plains Villager");
        addItem(ERItems.PLAINS_VILLAGER_SPAWN_EGG, "Plains Villager Spawn Egg");
        add("dialogue.eden_realm.plains_villager.reunion", "We meet again. How has your journey been?");
        add("dialogue.eden_realm.plains_villager.relief", "Oh, it's you. I thought there was more work to do.");
        add("entity.eden_realm.falling_fruit", "Falling Fruit");
        addItem(ERItems.MOSS_STONE_COLOSSUS_SPAWN_EGG, "Moss Stone Colossus Spawn Egg");
        addItem(ERItems.TIDE_SONG_COCONUT, "Tide Song Coconut");
        addItem(ERItems.SACRED_LIGHT_FRUIT, "Sacred Light Fruit");
        addItem(ERItems.CLOUD_CROWN_FRUIT, "Cloud Crown Fruit");
        addItem(ERItems.TWILIGHT_POMEGRANATE, "Twilight Pomegranate");
        addItem(ERItems.DEWSPIKE_GRAIN, "Dewspike Grain");
        addItem(ERItems.DEWSPIKE_GRAIN_SEEDS, "Dewspike Grain Seeds");
        addItem(ERItems.STAR_PATTERN_YAM, "Star Pattern Yam");
        addItem(ERItems.MOON_CLOVER, "Moon Clover");
        addItem(ERItems.CRYSTAL_DEW_FRUIT, "Crystal Dew Fruit");
        addItem(ERItems.VINE_BEAN, "Vine Bean");
        addItem(ERItems.MOON_CLOVER_SEEDS, "Moon Clover Seeds");
        addItem(ERItems.CRYSTAL_DEW_FRUIT_SEEDS, "Crystal Dew Fruit Seeds");
        addItem(ERItems.VINE_BEAN_SEEDS, "Vine Bean Seeds");
        for (var tool : com.kltyton.eden_realm.registry.content.item.ERToolItems.entries()) {
            addItem(tool.item(), tool.english());
        }
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
            addBlock(blocks.leaves(), (wood == ERWoodSet.HONEY_MAPLE ? "Orange " : "") + name + " Leaves");
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

        addBlock(ERBlocks.HONEY_MAPLE_RED_LEAVES, "Red Honey Maple Leaves");
        for (ERBlockEntry entry : ERBlocks.contentEntries()) {
            addBlock(entry.block(), entry.englishName());
        }
    }
}
