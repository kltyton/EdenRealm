package com.kltyton.eden_realm.common.event;

import com.kltyton.eden_realm.registry.content.ERTerrainBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class ERBlockToolEvents {
    private ERBlockToolEvents() {
    }

    public static void onBlockToolModification(BlockEvent.BlockToolModificationEvent event) {
        BlockState state = event.getState();
        boolean dirt = state.is(ERTerrainBlocks.EDEN_DIRT.get());
        boolean grass = state.is(ERTerrainBlocks.EDEN_GRASS_BLOCK.get());
        boolean path = state.is(ERTerrainBlocks.EDEN_DIRT_PATH.get());

        if (event.getItemAbility() == ItemAbilities.HOE_TILL
                && (dirt || grass || path)
                && event.getContext().getClickedFace() != Direction.DOWN
                && event.getLevel().getBlockState(event.getPos().above()).isAir()) {
            event.setFinalState(ERTerrainBlocks.EDEN_FARMLAND.get().defaultBlockState());
        } else if (event.getItemAbility() == ItemAbilities.SHOVEL_FLATTEN && (dirt || grass)) {
            event.setFinalState(ERTerrainBlocks.EDEN_DIRT_PATH.get().defaultBlockState());
        }
    }
}
