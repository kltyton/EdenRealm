package com.kltyton.eden_realm.common.event.fruit;

import com.kltyton.eden_realm.common.block.fruit.ERHangingFruitBlock;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.PistonEvent;

public final class ERFruitDisturbance {
    private ERFruitDisturbance() {
    }

    public static void onEntityPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled() || event.getEntity() == null || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        var snapshots = event instanceof BlockEvent.EntityMultiPlaceEvent multi
                ? multi.getReplacedBlockSnapshots() : java.util.List.of(event.getBlockSnapshot());
        for (var snapshot : snapshots) {
            BlockPos pos = snapshot.getPos().immutable();
            BlockState before = snapshot.getState();
            BlockState placed = snapshot.getCurrentState();
            if (placed.equals(before) || placed.isAir()) {
                continue;
            }
            Map<BlockPos, BlockState> ripe = captureAround(level, pos, before, placed);
            if (!ripe.isEmpty()) {
                // BlockItem placement can still be canceled and rolled back after this listener returns.
                level.getServer().schedule(new TickTask(level.getServer().getTickCount(), () -> {
                    if (!event.isCanceled() && level.hasChunkAt(pos) && level.getBlockState(pos).equals(placed)) {
                        detach(level, ripe);
                    }
                }));
            }
        }
    }

    public static void onGameEvent(VanillaGameEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getVanillaEvent().equals(GameEvent.BLOCK_PLACE)
                && event.getCause() != null && !(event.getCause() instanceof Player)) {
            // Nonplayer vanilla placement reports its final position here, after the placement was accepted.
            BlockPos pos = BlockPos.containing(event.getEventPosition());
            BlockState placed = event.getContext().affectedState();
            if (placed != null && !placed.isAir() && level.getBlockState(pos).equals(placed)) {
                detach(level, captureAround(level, pos, placed));
            }
        }
    }

    public static void onPistonPost(PistonEvent.Post event) {
        if (event.getPistonMoveType() == PistonEvent.PistonMoveType.RETRACT
                && event.getLevel() instanceof ServerLevel level) {
            detach(level, captureAround(level, event.getFaceOffsetPos(), event.getState()));
        }
    }

    public static void onResolvedPiston(ServerLevel level, BlockPos pistonPos, Direction facing,
            PistonStructureResolver resolver) {
        BlockPos head = pistonPos.relative(facing);
        Map<BlockPos, BlockState> ripe = captureAround(level, head, level.getBlockState(head));
        for (BlockPos source : resolver.getToPush()) {
            BlockState moved = level.getBlockState(source);
            BlockPos destination = source.relative(resolver.getPushDirection());
            ripe.putAll(captureAround(level, source, moved));
            ripe.putAll(captureAround(level, destination, moved, level.getBlockState(destination)));
        }
        for (BlockPos destroyed : resolver.getToDestroy()) {
            capture(level, destroyed, ripe);
            ripe.putAll(captureAround(level, destroyed, level.getBlockState(destroyed)));
        }
        detach(level, ripe);
    }

    private static Map<BlockPos, BlockState> captureAround(ServerLevel level, BlockPos pos, BlockState... sources) {
        Map<BlockPos, BlockState> ripe = new LinkedHashMap<>();
        for (Direction direction : Direction.values()) {
            capture(level, pos.relative(direction), ripe);
        }
        ripe.entrySet().removeIf(entry -> {
            ERHangingFruitBlock fruit = (ERHangingFruitBlock) entry.getValue().getBlock();
            for (BlockState source : sources) {
                if (fruit.isSameFruit(source)) {
                    return true;
                }
            }
            return false;
        });
        return ripe;
    }

    private static void capture(ServerLevel level, BlockPos pos, Map<BlockPos, BlockState> ripe) {
        if (level.hasChunkAt(pos)) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof ERHangingFruitBlock && state.getValue(ERHangingFruitBlock.AGE) == 4) {
                ripe.put(pos.immutable(), state);
            }
        }
    }

    private static void detach(ServerLevel level, Map<BlockPos, BlockState> ripe) {
        ripe.forEach((pos, state) -> {
            if (level.hasChunkAt(pos) && level.getBlockState(pos).equals(state)) {
                ((ERHangingFruitBlock) state.getBlock()).detachRipe(level, pos);
            }
        });
    }
}
