package com.kltyton.eden_realm.integration.harvest;

import com.kltyton.eden_realm.common.block.fruit.ERHangingFruitBlock;

import com.kltyton.eden_realm.common.entity.fruit.ERFallingFruitEntity;
import com.kltyton.eden_realm.registry.content.block.ERHarvestBlocks;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

final class FruitEventGameTests {
    private FruitEventGameTests() { }

    static void strikes(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = player(helper);
        BlockPos pos = helper.absolutePos(new BlockPos(3, 4, 3));
        for (int species = 0; species < 4; species++) {
            for (int age = 0; age <= 4; age++) {
                BlockState state = plant(helper, pos, species, age);
                strike(player, pos.above());
                helper.assertTrue(age == 4 ? level.getBlockState(pos).isAir() : level.getBlockState(pos).equals(state),
                        "validated support strike only detaches mature fruit");
                count(helper, pos, age == 4 ? 1 : 0);
                clearFalls(helper, pos);
                plant(helper, pos, species, age);
                var projectile = new Snowball(level, pos.getX() - 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                        new ItemStack(Items.SNOWBALL));
                projectile.setDeltaMovement(1, 0, 0);
                projectile.tick();
                helper.assertTrue(age == 4 ? level.getBlockState(pos).isAir() : level.getBlockState(pos).equals(state),
                        "real projectile flight into support only detaches mature fruit");
                count(helper, pos, age == 4 ? 1 : 0);
                clearFalls(helper, pos);
                projectile.discard();
            }
        }
        BlockState ripe = plant(helper, pos, 0, 4);
        Consumer<PlayerInteractEvent.LeftClickBlock> cancel = event -> {
            if (event.getEntity() == player) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(cancel);
        try { strike(player, pos.above()); } finally { NeoForge.EVENT_BUS.unregister(cancel); }
        helper.assertTrue(level.getBlockState(pos).equals(ripe), "canceled real strike preserves fruit");
        player.setPos(pos.getX() + 40, pos.getY(), pos.getZ());
        player.gameMode.handleBlockBreakAction(pos.above(), ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                Direction.UP, level.getMaxY(), 0);
        helper.assertTrue(level.getBlockState(pos).equals(ripe), "out of range strike preserves fruit");
        Consumer<EntityJoinLevelEvent> rejectFall = event -> {
            if (event.getEntity() instanceof ERFallingFruitEntity) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(rejectFall);
        try { strike(player, pos.above()); } finally { NeoForge.EVENT_BUS.unregister(rejectFall); }
        helper.assertTrue(level.getBlockState(pos).equals(ripe), "rejected falling entity preserves its source");
        count(helper, pos, 0);
        strike(player, pos);
        count(helper, pos, 1);
        helper.assertTrue(level.getBlockState(pos).isAir(), "direct ripe strike creates one falling fruit");
    }

    static void events(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = player(helper);
        BlockPos allowed = helper.absolutePos(new BlockPos(2, 5, 2));
        BlockPos canceled = helper.absolutePos(new BlockPos(6, 5, 2));
        BlockPos lateRipe = helper.absolutePos(new BlockPos(2, 5, 6));
        BlockPos nonplayer = helper.absolutePos(new BlockPos(6, 5, 6));
        plant(helper, allowed, 0, 4);
        BlockState canceledRipe = plant(helper, canceled, 2, 4);
        BlockState late = plant(helper, lateRipe, 0, 3);
        plant(helper, nonplayer, 1, 4);
        place(helper, player, allowed.east());
        Consumer<BlockEvent.EntityPlaceEvent> cancel = event -> {
            if (event.getPos().equals(canceled.east())) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(cancel);
        try { place(helper, player, canceled.east()); } finally { NeoForge.EVENT_BUS.unregister(cancel); }
        helper.assertTrue(level.getBlockState(canceled.east()).isAir(), "real canceled placement rolls back");
        place(helper, player, lateRipe.east());
        ((ERHangingFruitBlock) late.getBlock()).performBonemeal(level, net.minecraft.util.RandomSource.create(1), lateRipe, late);
        var mob = new Snowball(EntityTypes.SNOWBALL, level);
        level.setBlockAndUpdate(nonplayer.east(), Blocks.STONE.defaultBlockState());
        level.gameEvent(net.minecraft.world.level.gameevent.GameEvent.BLOCK_PLACE, nonplayer.east(),
                net.minecraft.world.level.gameevent.GameEvent.Context.of(mob, Blocks.STONE.defaultBlockState()));
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(level.getBlockState(allowed).isAir(), "committed adjacent player placement detaches ripe fruit");
            count(helper, allowed, 1);
            helper.assertTrue(level.getBlockState(canceled).equals(canceledRipe), "canceled placement preserves ripe fruit");
            helper.assertTrue(level.getBlockState(lateRipe).equals(late.setValue(ERHangingFruitBlock.AGE, 4)),
                    "fruit immature at placement cannot detach after ripening before callback");
            helper.assertTrue(level.getBlockState(nonplayer).isAir(), "sourced nonplayer placement detaches fruit");
            level.removeBlock(canceled.above(), false);
            level.removeBlock(lateRipe.above(), false);
            level.setBlockAndUpdate(lateRipe.above(), ERHarvestBlocks.TIDE_SONG_FLOWERING_LEAVES.get()
                    .defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(level.getBlockState(canceled).isAir(), "actual support loss detaches mature fruit");
                count(helper, canceled, 1);
                helper.assertTrue(level.getBlockState(lateRipe).getBlock() instanceof ERHangingFruitBlock,
                        "restored support cancels pending support-loss detachment");
                helper.succeed();
            });
        });
    }

    static BlockState plant(GameTestHelper helper, BlockPos pos, int species, int age) {
        var level = helper.getLevel();
        level.setBlockAndUpdate(pos.above(), ERHarvestBlocks.floweringLeaves().get(species).get()
                .defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
        BlockState state = ERHarvestBlocks.fruits().get(species).get().defaultBlockState().setValue(ERHangingFruitBlock.AGE, age);
        level.setBlockAndUpdate(pos, state);
        return state;
    }

    static void count(GameTestHelper helper, BlockPos pos, int expected) {
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ERFallingFruitEntity.class, new AABB(pos).inflate(1)).size() == expected,
                "falling entity count must be " + expected + " at " + pos);
    }

    private static void clearFalls(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().getEntitiesOfClass(ERFallingFruitEntity.class, new AABB(pos).inflate(2)).forEach(ERFallingFruitEntity::discard);
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        new net.minecraft.server.network.ServerGamePacketListenerImpl(helper.getLevel().getServer(),
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), player,
                net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(), false));
        return player;
    }

    private static void strike(ServerPlayer player, BlockPos pos) {
        player.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 2);
        player.gameMode.handleBlockBreakAction(pos, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                Direction.UP, player.level().getMaxY(), 0);
    }

    private static void place(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        player.setPos(pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
        var hit = new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false);
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }
}
