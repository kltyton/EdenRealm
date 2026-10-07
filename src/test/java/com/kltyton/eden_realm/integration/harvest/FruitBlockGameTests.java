package com.kltyton.eden_realm.integration.harvest;

import com.kltyton.eden_realm.common.block.fruit.ERFruitBlock;
import com.kltyton.eden_realm.registry.content.block.ERHarvestBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

final class FruitBlockGameTests {
    private FruitBlockGameTests() {
    }

    static void placedGravityFree(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        var placed = new java.util.LinkedHashMap<BlockPos, net.minecraft.world.level.block.state.BlockState>();
        for (int index = 0; index < ERHarvestBlocks.groundFruits().size(); index++) {
            var fruit = ERHarvestBlocks.groundFruits().get(index).get();
            BlockPos pos = helper.absolutePos(new BlockPos(1 + index * 2, 4, 3));
            level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(fruit));
            var hit = new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false);
            player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
            helper.assertTrue(level.getBlockState(pos).is(fruit), "real player placement creates ordinary fruit");
            placed.put(pos, level.getBlockState(pos));
            level.removeBlock(pos.below(), false);
            level.setBlockAndUpdate(pos.north(), Blocks.STONE.defaultBlockState());
        }
        helper.runAfterDelay(8, () -> {
            placed.forEach((pos, state) -> helper.assertTrue(level.getBlockState(pos).equals(state), "placed fruit survives unsupported and neighbor updates"));
            helper.assertTrue(level.getEntitiesOfClass(com.kltyton.eden_realm.common.entity.fruit.ERFallingFruitEntity.class,
                    helper.getBounds()).isEmpty(), "player-placed fruits never create falling entities");
            helper.succeed();
        });
    }

    static void stacks(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        BlockPos relative = new BlockPos(3, 2, 3);
        BlockPos pos = helper.absolutePos(relative);
        for (var holder : ERHarvestBlocks.groundFruits()) {
            var block = holder.get();
            level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
            level.removeBlock(pos, false);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(block, 8));
            // Given a harvested fruit and empty ground, use its real BlockItem placement path.
            var groundHit = new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false);
            player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, groundHit));
            HarvestGameTests.require(level.getBlockState(pos).is(block), "fruit item must place its matching block");
            HarvestGameTests.require(player.getMainHandItem().getCount() == 7, "survival placement consumes one fruit");
            int maximum = block.defaultBlockState().hasProperty(ERFruitBlock.COUNT) ? 4 : 1;
            for (int count = 2; count <= maximum; count++) {
                // When the same fruit is used on its stack, the native placement path increments it.
                helper.useBlock(relative, player);
                HarvestGameTests.require(ERFruitBlock.count(level.getBlockState(pos)) == count, "stack count must increment by one");
                HarvestGameTests.require(player.getMainHandItem().getCount() == 8 - count, "each stacked fruit is consumed");
            }
            HarvestGameTests.require(maximum == 4 || !block.defaultBlockState().hasProperty(ERFruitBlock.COUNT),
                    "twilight fruit must have no unsupported count states");
            var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            var context = new net.minecraft.world.item.context.BlockPlaceContext(player, InteractionHand.MAIN_HAND,
                    player.getMainHandItem(), hit);
            HarvestGameTests.require(!level.getBlockState(pos).canBeReplaced(context), "full stack cannot accept a fifth fruit");
            HarvestGameTests.require(!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty(), "ground fruit has physical collision");
        }
    }

    static void emptyHand(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        BlockPos relative = new BlockPos(3, 2, 3);
        BlockPos pos = helper.absolutePos(relative);
        for (var holder : ERHarvestBlocks.groundFruits()) {
            var block = holder.get();
            var state = block.defaultBlockState();
            int maximum = state.hasProperty(ERFruitBlock.COUNT) ? 4 : 1;
            if (maximum == 4) {
                state = state.setValue(ERFruitBlock.COUNT, maximum);
            }
            level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(pos, state);
            // Given a nonmatching held item, its fallback must not take a fruit.
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            helper.useBlock(relative, player);
            HarvestGameTests.require(level.getBlockState(pos).equals(state), "nonempty wrong hand must not extract fruit");
            for (int remaining = maximum; remaining > 0; remaining--) {
                player.getInventory().clearContent();
                // When an empty hand right-clicks, take exactly one.
                helper.useBlock(relative, player);
                HarvestGameTests.require(player.getInventory().countItem(block.asItem()) == 1, "one click must return exactly one fruit");
                HarvestGameTests.require(remaining == 1 ? level.getBlockState(pos).isAir()
                        : ERFruitBlock.count(level.getBlockState(pos)) == remaining - 1, "taking last fruit removes the block");
            }
        }
    }

    static void breakCounts(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        for (var holder : ERHarvestBlocks.groundFruits()) {
            var block = holder.get();
            int maximum = block.defaultBlockState().hasProperty(ERFruitBlock.COUNT) ? 4 : 1;
            for (int count = 1; count <= maximum; count++) {
                level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).forEach(ItemEntity::discard);
                var state = maximum == 4 ? block.defaultBlockState().setValue(ERFruitBlock.COUNT, count) : block.defaultBlockState();
                level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(pos, state);
                // When a survival-style destruction executes the generated loot table.
                level.destroyBlock(pos, true);
                int dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream()
                        .map(ItemEntity::getItem).filter(item -> item.is(block.asItem())).mapToInt(ItemStack::getCount).sum();
                HarvestGameTests.require(dropped == count, "breaking ground fruit must drop the exact stack count");
            }
        }
    }
}
