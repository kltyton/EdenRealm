package com.kltyton.eden_realm.integration.harvest;

import com.kltyton.eden_realm.common.block.fruit.ERFruitBlock;
import com.kltyton.eden_realm.registry.content.block.ERHarvestBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

final class FruitWhitelistGameTests {
    private FruitWhitelistGameTests() { }

    static void placement(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        for (int species = 0; species < 4; species++) {
            BlockPos pos = helper.absolutePos(new BlockPos(2 + species * 3, 5, 3));
            FruitEventGameTests.plant(helper, pos, species, 4);
            Block ground = ERHarvestBlocks.groundFruits().get(species).get();
            place(helper, player, pos.east(), ground);
            if (ground.defaultBlockState().hasProperty(ERFruitBlock.COUNT)) {
                use(player, pos.east(), ground);
                helper.assertTrue(ERFruitBlock.count(level.getBlockState(pos.east())) == 2, "same fruit stacking uses real item placement");
            }
        }
        BlockPos different = helper.absolutePos(new BlockPos(2, 5, 9));
        FruitEventGameTests.plant(helper, different, 0, 4);
        place(helper, player, different.east(), ERHarvestBlocks.CLOUD_CROWN_FRUIT_GROUND.get());
        helper.runAfterDelay(2, () -> {
            for (int species = 0; species < 4; species++) {
                BlockPos pos = helper.absolutePos(new BlockPos(2 + species * 3, 5, 3));
                helper.assertTrue(level.getBlockState(pos).is(ERHarvestBlocks.fruits().get(species).get()),
                        "same-species fruit placement and stacking preserve the ripe hanging fruit");
                FruitEventGameTests.count(helper, pos, 0);
            }
            helper.assertTrue(level.getBlockState(different).isAir(), "different-species fruit remains a valid placement disturbance");
            FruitEventGameTests.count(helper, different, 1);
            helper.succeed();
        });
    }

    static void piston(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos piston = helper.absolutePos(new BlockPos(2, 4, 3));
        BlockPos sourceFruit = piston.east(2).north();
        BlockPos destinationFruit = piston.east(3).south();
        FruitEventGameTests.plant(helper, sourceFruit, 0, 4);
        FruitEventGameTests.plant(helper, destinationFruit, 0, 4);
        level.setBlockAndUpdate(piston.east(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(piston.east(2), ERHarvestBlocks.TIDE_SONG_COCONUT_GROUND.get().defaultBlockState());
        level.setBlockAndUpdate(piston, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST));
        level.setBlockAndUpdate(piston.below(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(level.getBlockState(piston.east(3)).is(ERHarvestBlocks.TIDE_SONG_COCONUT_GROUND.get()),
                    "actual piston moves the same-species ground fruit");
            for (BlockPos pos : java.util.List.of(sourceFruit, destinationFruit)) {
                helper.assertTrue(level.getBlockState(pos).is(ERHarvestBlocks.TIDE_SONG_COCONUT.get()),
                        "same-fruit movement at either endpoint is whitelisted");
                FruitEventGameTests.count(helper, pos, 0);
            }
            helper.succeed();
        });
    }

    private static void place(GameTestHelper helper, Player player, BlockPos pos, Block block) {
        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        use(player, pos.below(), block);
        helper.assertTrue(helper.getLevel().getBlockState(pos).is(block), "fruit item really placed its block");
    }

    private static void use(Player player, BlockPos clicked, Block block) {
        player.setPos(clicked.getX() + 0.5, clicked.getY() + 3, clicked.getZ() + 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(block));
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(clicked), Direction.UP, clicked, false)));
    }
}
