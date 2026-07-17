package com.kltyton.eden_realm.data.loot;

import com.kltyton.eden_realm.common.block.ERWoodSet;
import com.kltyton.eden_realm.registry.ERBlocks;
import com.kltyton.eden_realm.registry.content.ERBlockEntry;
import com.kltyton.eden_realm.registry.content.ERCoralBlocks;
import com.kltyton.eden_realm.registry.content.ERPlantBlocks;
import com.kltyton.eden_realm.registry.content.ERTerrainBlocks;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.NonNull;

public final class ERBlockLootSubProvider extends BlockLootSubProvider {
    public ERBlockLootSubProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
    }

    @Override
    protected void generate() {
        for (ERWoodSet wood : ERWoodSet.values()) {
            ERBlocks.WoodBlocks blocks = ERBlocks.woodBlocks(wood);

            dropSelf(blocks.log().get());
            dropSelf(blocks.wood().get());
            dropSelf(blocks.strippedLog().get());
            dropSelf(blocks.strippedWood().get());
            dropSelf(blocks.planks().get());
            dropSelf(blocks.stairs().get());
            dropSelf(blocks.slab().get());
            dropSelf(blocks.fence().get());
            dropSelf(blocks.fenceGate().get());
            dropSelf(blocks.button().get());
            dropSelf(blocks.pressurePlate().get());
            dropSelf(blocks.shelf().get());
            add(blocks.leaves().get(), createLeavesDrops(blocks.leaves().get(), blocks.sapling().get(), NORMAL_LEAVES_SAPLING_CHANCES));
            dropSelf(blocks.sapling().get());
            add(blocks.door().get(), createDoorTable(blocks.door().get()));
            dropSelf(blocks.trapdoor().get());
            dropSelf(blocks.sign().get());
            dropOther(blocks.wallSign().get(), blocks.sign().get());
            dropSelf(blocks.hangingSign().get());
            dropOther(blocks.wallHangingSign().get(), blocks.hangingSign().get());
        }

        Set<Block> specialized = new HashSet<>();
        specialized.add(ERTerrainBlocks.BOUNDARY_ROCK.get());
        specialized.add(ERTerrainBlocks.RAW_ROCK.get());
        specialized.add(ERTerrainBlocks.RAW_ROCK_COAL_ORE.get());
        specialized.add(ERTerrainBlocks.RAW_ROCK_IRON_ORE.get());
        specialized.add(ERTerrainBlocks.EDEN_DIRT_PATH.get());
        specialized.add(ERTerrainBlocks.EDEN_FARMLAND.get());
        specialized.add(ERTerrainBlocks.EDEN_GRASS_BLOCK.get());
        specialized.add(ERPlantBlocks.BUBBLE_GRASS.get());
        specialized.add(ERPlantBlocks.BLUE_COURT_SEAGRASS.get());
        specialized.add(ERPlantBlocks.TALL_BLUE_COURT_SEAGRASS.get());
        ERCoralBlocks.families().forEach(family -> {
            specialized.add(family.block().get());
            specialized.add(family.deadBlock().get());
            specialized.add(family.plant().get());
            specialized.add(family.deadPlant().get());
            specialized.add(family.fan().get());
            specialized.add(family.deadFan().get());
        });

        for (ERBlockEntry entry : ERBlocks.contentEntries()) {
            Block block = entry.block().get();
            if (entry.hasItem() && !specialized.contains(block)) {
                dropSelf(block);
            }
        }

        add(
                ERTerrainBlocks.RAW_ROCK.get(),
                createSingleItemTableWithSilkTouch(ERTerrainBlocks.RAW_ROCK.get(), ERTerrainBlocks.RUBBLE.get()));
        add(
                ERTerrainBlocks.RAW_ROCK_COAL_ORE.get(),
                createOreDrop(ERTerrainBlocks.RAW_ROCK_COAL_ORE.get(), Items.COAL));
        add(
                ERTerrainBlocks.RAW_ROCK_IRON_ORE.get(),
                createOreDrop(ERTerrainBlocks.RAW_ROCK_IRON_ORE.get(), Items.RAW_IRON));
        add(
                ERTerrainBlocks.EDEN_GRASS_BLOCK.get(),
                createSingleItemTableWithSilkTouch(
                        ERTerrainBlocks.EDEN_GRASS_BLOCK.get(), ERTerrainBlocks.EDEN_DIRT.get()));
        dropOther(ERTerrainBlocks.EDEN_DIRT_PATH.get(), ERTerrainBlocks.EDEN_DIRT.get());
        dropOther(ERTerrainBlocks.EDEN_FARMLAND.get(), ERTerrainBlocks.EDEN_DIRT.get());
        add(ERPlantBlocks.BUBBLE_GRASS.get(), createShearsOnlyDrop(ERPlantBlocks.BUBBLE_GRASS.get()));
        add(
                ERPlantBlocks.BLUE_COURT_SEAGRASS.get(),
                createShearsOnlyDrop(ERPlantBlocks.BLUE_COURT_SEAGRASS.get()));
        add(
                ERPlantBlocks.TALL_BLUE_COURT_SEAGRASS.get(),
                createDoublePlantShearsDrop(ERPlantBlocks.BLUE_COURT_SEAGRASS.get()));

        for (ERCoralBlocks.CoralFamily family : ERCoralBlocks.families()) {
            add(family.block().get(), createSingleItemTableWithSilkTouch(family.block().get(), family.deadBlock().get()));
            dropSelf(family.deadBlock().get());
            add(family.plant().get(), createSilkTouchOnlyTable(family.plant().get()));
            add(family.deadPlant().get(), createSilkTouchOnlyTable(family.deadPlant().get()));
            add(family.fan().get(), createSilkTouchOnlyTable(family.fan().get()));
            add(family.deadFan().get(), createSilkTouchOnlyTable(family.deadFan().get()));
        }
    }

    @Override
    protected @NonNull Iterable<Block> getKnownBlocks() {
        return ERBlocks.entries();
    }
}
