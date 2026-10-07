package com.kltyton.eden_realm.integration.harvest;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.PistonEvent;

final class FruitPistonGameTests {
    private FruitPistonGameTests() { }

    static void movement(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos chain = helper.absolutePos(new BlockPos(2, 4, 2));
        BlockPos direct = helper.absolutePos(new BlockPos(2, 4, 8));
        BlockPos canceled = helper.absolutePos(new BlockPos(10, 4, 2));
        BlockPos blocked = helper.absolutePos(new BlockPos(10, 4, 8));
        BlockPos ripe = chain.east(3).south();
        BlockPos unripe = chain.east(2).north();
        FruitEventGameTests.plant(helper, ripe, 2, 4);
        var immature = FruitEventGameTests.plant(helper, unripe, 0, 3);
        FruitEventGameTests.plant(helper, direct.east(), 0, 4);
        var denied = FruitEventGameTests.plant(helper, canceled.east().south(), 0, 4);
        var stuck = FruitEventGameTests.plant(helper, blocked.east().south(), 0, 4);
        level.setBlockAndUpdate(chain.east(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(chain.east(2), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(blocked.east(), Blocks.OBSIDIAN.defaultBlockState());
        Consumer<PistonEvent.Pre> cancel = event -> {
            if (event.getPos().equals(canceled)) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(cancel);
        for (BlockPos pos : java.util.List.of(chain, direct, canceled, blocked)) {
            level.setBlockAndUpdate(pos, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST));
            level.setBlockAndUpdate(pos.below(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        }
        helper.runAfterDelay(4, () -> {
            NeoForge.EVENT_BUS.unregister(cancel);
            helper.assertTrue(level.getBlockState(chain).getValue(PistonBaseBlock.EXTENDED)
                    && level.getBlockState(chain.east(3)).is(Blocks.STONE), "actual piston pushes the whole chain");
            helper.assertTrue(level.getBlockState(ripe).isAir(), "chain destination disturbs adjacent mature cloud fruit");
            FruitEventGameTests.count(helper, ripe, 1);
            helper.assertTrue(level.getBlockState(unripe).equals(immature), "piston movement preserves adjacent immature fruit");
            helper.assertTrue(level.getBlockState(direct).getValue(PistonBaseBlock.EXTENDED), "direct piston extends into ripe fruit");
            FruitEventGameTests.count(helper, direct.east(), 1);
            helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(direct.east()).inflate(2)).isEmpty(), "direct contact must not also drop an item");
            helper.assertTrue(!level.getBlockState(canceled).getValue(PistonBaseBlock.EXTENDED)
                    && level.getBlockState(canceled.east().south()).equals(denied), "canceled movement preserves fruit");
            helper.assertTrue(!level.getBlockState(blocked).getValue(PistonBaseBlock.EXTENDED)
                    && level.getBlockState(blocked.east().south()).equals(stuck), "blocked movement preserves fruit");
            helper.succeed();
        });
    }

    static void retraction(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos sticky = helper.absolutePos(new BlockPos(2, 4, 2));
        BlockPos empty = helper.absolutePos(new BlockPos(2, 4, 8));
        level.setBlockAndUpdate(sticky.east(), Blocks.STONE.defaultBlockState());
        for (BlockPos pos : java.util.List.of(sticky, empty)) {
            level.setBlockAndUpdate(pos, (pos.equals(sticky) ? Blocks.STICKY_PISTON : Blocks.PISTON)
                    .defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST));
            level.setBlockAndUpdate(pos.below(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        }
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(level.getBlockState(sticky.east(2)).is(Blocks.STONE), "sticky piston fully extends before pull");
            FruitEventGameTests.plant(helper, sticky.east(2).south(), 3, 4);
            FruitEventGameTests.plant(helper, empty.east().south(), 1, 4);
            level.removeBlock(sticky.below(), false);
            level.removeBlock(empty.below(), false);
            helper.runAfterDelay(4, () -> {
                helper.assertTrue(!level.getBlockState(sticky).getValue(PistonBaseBlock.EXTENDED)
                        && level.getBlockState(sticky.east()).is(Blocks.STONE), "sticky piston actually retracts and pulls");
                helper.assertTrue(level.getBlockState(sticky.east(2).south()).isAir(), "pulled source disturbs mature fruit");
                FruitEventGameTests.count(helper, sticky.east(2).south(), 1);
                helper.assertTrue(!level.getBlockState(empty).getValue(PistonBaseBlock.EXTENDED)
                        && level.getBlockState(empty.east().south()).isAir(), "empty head retraction disturbs mature fruit");
                FruitEventGameTests.count(helper, empty.east().south(), 1);
                helper.succeed();
            });
        });
    }
}
