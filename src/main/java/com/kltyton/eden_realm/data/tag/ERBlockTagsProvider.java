package com.kltyton.eden_realm.data.tag;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.block.ERWoodSet;
import com.kltyton.eden_realm.registry.ERBlocks;
import com.kltyton.eden_realm.util.ERTags;
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
                    .add(blocks.strippedLog().getKey());
            tag(ERTags.Blocks.EDEN_REALM_PLANKS)
                    .add(blocks.planks().getKey());
            tag(ERTags.Blocks.EDEN_REALM_LEAVES)
                    .add(blocks.leaves().getKey());
            tag(ERTags.Blocks.EDEN_REALM_SAPLINGS)
                    .add(blocks.sapling().getKey());

            tag(BlockTags.LOGS)
                    .add(blocks.log().getKey())
                    .add(blocks.strippedLog().getKey());
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
                    .add(blocks.strippedLog().getKey())
                    .add(blocks.planks().getKey())
                    .add(blocks.stairs().getKey())
                    .add(blocks.slab().getKey())
                    .add(blocks.fence().getKey())
                    .add(blocks.fenceGate().getKey())
                    .add(blocks.button().getKey())
                    .add(blocks.pressurePlate().getKey())
                    .add(blocks.door().getKey())
                    .add(blocks.trapdoor().getKey())
                    .add(blocks.sign().getKey())
                    .add(blocks.wallSign().getKey())
                    .add(blocks.hangingSign().getKey())
                    .add(blocks.wallHangingSign().getKey());
            tag(BlockTags.MINEABLE_WITH_HOE)
                    .add(blocks.leaves().getKey());
        }
    }
}
