package com.kltyton.eden_realm.data.tag;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.block.ERWoodSet;
import com.kltyton.eden_realm.registry.ERBlocks;
import com.kltyton.eden_realm.registry.content.ERCoralBlocks;
import com.kltyton.eden_realm.registry.content.ERPlantBlocks;
import com.kltyton.eden_realm.registry.content.ERTerrainBlocks;
import com.kltyton.eden_realm.util.ERTags;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import org.jspecify.annotations.NonNull;

public final class ERBlockTagsProvider extends BlockTagsProvider {
    public ERBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, ERConstants.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.@NonNull Provider registries) {
        for (ERWoodSet wood : ERWoodSet.values()) {
            ERBlocks.WoodBlocks blocks = ERBlocks.woodBlocks(wood);

            tag(ERTags.Blocks.EDEN_REALM_LOGS)
                    .add(blocks.log().getKey())
                    .add(blocks.wood().getKey())
                    .add(blocks.strippedLog().getKey())
                    .add(blocks.strippedWood().getKey());
            tag(ERTags.Blocks.EDEN_REALM_PLANKS)
                    .add(blocks.planks().getKey());
            tag(ERTags.Blocks.EDEN_REALM_SHELVES)
                    .add(blocks.shelf().getKey());
            tag(ERTags.Blocks.EDEN_REALM_LEAVES)
                    .add(blocks.leaves().getKey());
            tag(ERTags.Blocks.EDEN_REALM_SAPLINGS)
                    .add(blocks.sapling().getKey());

            tag(BlockTags.LOGS)
                    .add(blocks.log().getKey())
                    .add(blocks.wood().getKey())
                    .add(blocks.strippedLog().getKey())
                    .add(blocks.strippedWood().getKey());
            tag(BlockTags.OVERWORLD_NATURAL_LOGS)
                    .add(blocks.log().getKey());
            tag(BlockTags.PLANKS)
                    .add(blocks.planks().getKey());
            tag(BlockTags.WOODEN_STAIRS)
                    .add(blocks.stairs().getKey());
            tag(BlockTags.STAIRS)
                    .add(blocks.stairs().getKey());
            tag(BlockTags.WOODEN_SLABS)
                    .add(blocks.slab().getKey());
            tag(BlockTags.SLABS)
                    .add(blocks.slab().getKey());
            tag(BlockTags.WOODEN_FENCES)
                    .add(blocks.fence().getKey());
            tag(BlockTags.FENCES)
                    .add(blocks.fence().getKey());
            tag(BlockTags.FENCE_GATES)
                    .add(blocks.fenceGate().getKey());
            tag(BlockTags.WOODEN_BUTTONS)
                    .add(blocks.button().getKey());
            tag(BlockTags.BUTTONS)
                    .add(blocks.button().getKey());
            tag(BlockTags.WOODEN_PRESSURE_PLATES)
                    .add(blocks.pressurePlate().getKey());
            tag(BlockTags.PRESSURE_PLATES)
                    .add(blocks.pressurePlate().getKey());
            tag(BlockTags.WOODEN_SHELVES)
                    .add(blocks.shelf().getKey());
            tag(BlockTags.LEAVES)
                    .add(blocks.leaves().getKey());
            tag(BlockTags.WOODEN_DOORS)
                    .add(blocks.door().getKey());
            tag(BlockTags.DOORS)
                    .add(blocks.door().getKey());
            tag(BlockTags.WOODEN_TRAPDOORS)
                    .add(blocks.trapdoor().getKey());
            tag(BlockTags.TRAPDOORS)
                    .add(blocks.trapdoor().getKey());
            tag(BlockTags.STANDING_SIGNS)
                    .add(blocks.sign().getKey());
            tag(BlockTags.WALL_SIGNS)
                    .add(blocks.wallSign().getKey());
            tag(BlockTags.SIGNS)
                    .add(blocks.sign().getKey())
                    .add(blocks.wallSign().getKey());
            tag(BlockTags.CEILING_HANGING_SIGNS)
                    .add(blocks.hangingSign().getKey());
            tag(BlockTags.WALL_HANGING_SIGNS)
                    .add(blocks.wallHangingSign().getKey());
            tag(BlockTags.ALL_HANGING_SIGNS)
                    .add(blocks.hangingSign().getKey())
                    .add(blocks.wallHangingSign().getKey());
            tag(BlockTags.ALL_SIGNS)
                    .add(blocks.sign().getKey())
                    .add(blocks.wallSign().getKey())
                    .add(blocks.hangingSign().getKey())
                    .add(blocks.wallHangingSign().getKey());

            tag(BlockTags.MINEABLE_WITH_AXE)
                    .add(blocks.log().getKey())
                    .add(blocks.wood().getKey())
                    .add(blocks.strippedLog().getKey())
                    .add(blocks.strippedWood().getKey())
                    .add(blocks.planks().getKey())
                    .add(blocks.stairs().getKey())
                    .add(blocks.slab().getKey())
                    .add(blocks.fence().getKey())
                    .add(blocks.fenceGate().getKey())
                    .add(blocks.button().getKey())
                    .add(blocks.pressurePlate().getKey())
                    .add(blocks.shelf().getKey())
                    .add(blocks.door().getKey())
                    .add(blocks.trapdoor().getKey())
                    .add(blocks.sign().getKey())
                    .add(blocks.wallSign().getKey())
                    .add(blocks.hangingSign().getKey())
                    .add(blocks.wallHangingSign().getKey());
            tag(BlockTags.MINEABLE_WITH_HOE)
                    .add(blocks.leaves().getKey());
        }

        var pickaxe = tag(BlockTags.MINEABLE_WITH_PICKAXE);
        List.of(
                        ERTerrainBlocks.WEATHERED_ROCK,
                        ERTerrainBlocks.ROOTED_ROCK,
                        ERTerrainBlocks.BOUNDARY_ROCK,
                        ERTerrainBlocks.RUBBLE,
                        ERTerrainBlocks.TUNDRA_ROCK,
                        ERTerrainBlocks.SHALE,
                        ERTerrainBlocks.RAW_ROCK,
                        ERTerrainBlocks.CHISELED_RAW_ROCK_BRICKS,
                        ERTerrainBlocks.MOSSY_RAW_ROCK_PILLAR,
                        ERTerrainBlocks.MOSSY_RAW_ROCK_BRICKS,
                        ERTerrainBlocks.RAW_ROCK_BRICKS,
                        ERTerrainBlocks.RAW_ROCK_COAL_ORE,
                        ERTerrainBlocks.RAW_ROCK_IRON_ORE,
                        ERTerrainBlocks.FLOATING_ISLAND_ROCK,
                        ERTerrainBlocks.SKY_PLATFORM_STONE,
                        ERTerrainBlocks.ICE_CRYSTAL_ROCK,
                        ERTerrainBlocks.FROST_PATTERN_STONE,
                        ERTerrainBlocks.AMBER_CRYSTAL_BLOCK,
                        ERTerrainBlocks.SUN_ROCK,
                        ERTerrainBlocks.SPRING_STONE,
                        ERTerrainBlocks.ERODED_SANDSTONE)
                .forEach(holder -> pickaxe.add(holder.getKey()));
        for (ERTerrainBlocks.SandstoneSet set : List.of(
                ERTerrainBlocks.COAST, ERTerrainBlocks.AMBER, ERTerrainBlocks.OASIS)) {
            pickaxe.add(set.sandstone().getKey())
                    .add(set.smoothSandstone().getKey())
                    .add(set.cutSandstone().getKey())
                    .add(set.chiseledSandstone().getKey());
        }

        var shovel = tag(BlockTags.MINEABLE_WITH_SHOVEL)
                .add(ERTerrainBlocks.EDEN_DIRT.getKey())
                .add(ERTerrainBlocks.EDEN_DIRT_PATH.getKey())
                .add(ERTerrainBlocks.EDEN_FARMLAND.getKey())
                .add(ERTerrainBlocks.THIN_CLOUD_SOIL.getKey())
                .add(ERTerrainBlocks.SEDIMENTARY_SILT.getKey())
                .add(ERTerrainBlocks.PEAT_BLOCK.getKey())
                .add(ERTerrainBlocks.WET_SWAMP_SOIL.getKey())
                .add(ERTerrainBlocks.COAST.sand().getKey())
                .add(ERTerrainBlocks.AMBER.sand().getKey())
                .add(ERTerrainBlocks.OASIS.sand().getKey());
        shovel.add(ERTerrainBlocks.EDEN_GRASS_BLOCK.getKey());

        tag(BlockTags.MINEABLE_WITH_HOE)
                .add(ERPlantBlocks.ROTTING_WOOD_FUNGUS_MAT.getKey());
        tag(BlockTags.NEEDS_STONE_TOOL)
                .add(ERTerrainBlocks.RAW_ROCK_IRON_ORE.getKey());
        tag(BlockTags.IRON_ORES)
                .add(ERTerrainBlocks.RAW_ROCK_IRON_ORE.getKey());

        var dirt = tag(BlockTags.DIRT)
                .add(ERTerrainBlocks.EDEN_DIRT.getKey())
                .add(ERTerrainBlocks.THIN_CLOUD_SOIL.getKey())
                .add(ERTerrainBlocks.WET_SWAMP_SOIL.getKey());
        var vegetationSupport = tag(BlockTags.SUPPORTS_VEGETATION)
                .add(ERTerrainBlocks.EDEN_DIRT.getKey())
                .add(ERTerrainBlocks.THIN_CLOUD_SOIL.getKey())
                .add(ERTerrainBlocks.WET_SWAMP_SOIL.getKey())
                .add(ERTerrainBlocks.EDEN_FARMLAND.getKey());
        dirt.add(ERTerrainBlocks.EDEN_GRASS_BLOCK.getKey());
        vegetationSupport.add(ERTerrainBlocks.EDEN_GRASS_BLOCK.getKey());
        tag(BlockTags.SUPPORTS_CROPS)
                .add(ERTerrainBlocks.EDEN_FARMLAND.getKey());
        tag(BlockTags.SAND)
                .add(ERTerrainBlocks.COAST.sand().getKey())
                .add(ERTerrainBlocks.AMBER.sand().getKey())
                .add(ERTerrainBlocks.OASIS.sand().getKey());
        tag(BlockTags.SUPPORTS_DRY_VEGETATION)
                .add(ERTerrainBlocks.COAST.sand().getKey())
                .add(ERTerrainBlocks.AMBER.sand().getKey())
                .add(ERTerrainBlocks.OASIS.sand().getKey())
                .add(ERTerrainBlocks.EDEN_DIRT.getKey());
        tag(BlockTags.UNDERWATER_BONEMEALS)
                .add(ERPlantBlocks.BUBBLE_GRASS.getKey())
                .add(ERPlantBlocks.BLUE_COURT_SEAGRASS.getKey());

        var coralBlocks = tag(BlockTags.CORAL_BLOCKS);
        var coralPlants = tag(BlockTags.CORAL_PLANTS);
        var corals = tag(BlockTags.CORALS);
        var wallCorals = tag(BlockTags.WALL_CORALS);
        for (ERCoralBlocks.CoralFamily family : ERCoralBlocks.families()) {
            coralBlocks.add(family.block().getKey());
            coralPlants.add(family.plant().getKey());
            corals.add(family.plant().getKey()).add(family.fan().getKey());
            wallCorals.add(family.wallFan().getKey());
            pickaxe.add(family.block().getKey()).add(family.deadBlock().getKey());
        }
    }
}
