package com.kltyton.eden_realm.integration.harvest;

import com.kltyton.eden_realm.common.block.crop.ERDoubleCropBlock;

import com.kltyton.eden_realm.registry.ERItems;
import com.kltyton.eden_realm.registry.content.block.ERHarvestBlocks;
import com.kltyton.eden_realm.registry.content.block.ERTerrainBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

final class GardenCropGameTests {
    private GardenCropGameTests() {
    }

    static void tallCrops(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos relative = new BlockPos(3, 1, 3);
        BlockPos pos = helper.absolutePos(relative);
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        GameType.CREATIVE.updatePlayerAbilities(player.getAbilities());
        for (var holder : ERHarvestBlocks.gardenCrops()) {
            var crop = holder.get();
            String id = holder.getId().getPath();
            var seed = (BlockItem) ERItems.harvestItem(id + "_seeds").get();
            var product = ERItems.harvestItem(id).get();
            level.removeBlock(pos, false);
            level.setBlockAndUpdate(pos.below(), Blocks.DIRT.defaultBlockState());
            HarvestPlantGameTests.place(level, player, seed, pos);
            helper.assertTrue(level.getBlockState(pos).isAir(), "garden seeds reject unplowed dirt");
            for (Block soil : java.util.List.of(Blocks.FARMLAND, ERTerrainBlocks.EDEN_FARMLAND.get())) {
                level.setBlockAndUpdate(pos.below(), soil.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
                level.setBlockAndUpdate(pos.above(), Blocks.STONE.defaultBlockState());
                HarvestPlantGameTests.place(level, player, seed, pos);
                helper.assertTrue(level.getBlockState(pos).isAir(), "two-block crop rejects obstructed headroom");
                level.removeBlock(pos.above(), false);
                HarvestPlantGameTests.place(level, player, seed, pos);
                helper.assertTrue(level.getBlockState(pos).is(crop) && level.getBlockState(pos.above()).is(crop), "seed places both crop halves");
                helper.assertTrue(level.getBlockState(pos.above()).getShape(level, pos.above()).isEmpty(), "short seedling has no upper selection shape");
                helper.assertTrue(Block.getDrops(level.getBlockState(pos), level, pos, null).stream().allMatch(s -> s.is(seed)), "immature crop returns only seed");
                for (int age = 0; age < 8; age++) {
                    var state = crop.defaultBlockState().setValue(ERDoubleCropBlock.AGE, age);
                    level.setBlockAndUpdate(pos, state);
                    helper.assertTrue(level.getBlockState(pos.above()).getValue(ERDoubleCropBlock.AGE) == age, "all growth ages synchronize upper half");
                    helper.assertTrue(state.getCollisionShape(level, pos).isEmpty(), "crops preserve collision-free plant behavior");
                }
                var survivor = helper.makeMockPlayer(GameType.SURVIVAL);
                HarvestPlantGameTests.breakAsPlayer(level, survivor, pos.above());
                helper.assertTrue(level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir(), "upper harvesting removes whole crop");
                helper.assertItemEntityCountIs(product, relative, 3, 1);
                helper.assertItemEntityCountIs(seed, relative, 3, 1);
                helper.despawnItem(relative, 4);
                HarvestPlantGameTests.place(level, player, seed, pos);
                for (int tick = 0; tick < 1200 && level.getBlockState(pos).getValue(ERDoubleCropBlock.AGE) < 7; tick++) {
                    level.getBlockState(pos).randomTick(level, pos, RandomSource.create(tick + 500));
                }
                helper.assertTrue(level.getBlockState(pos).getValue(ERDoubleCropBlock.AGE) == 7, "replanted crop naturally matures");
                level.setBlockAndUpdate(pos, crop.defaultBlockState());
                for (int step = 0; step < 4; step++) {
                    crop.performBonemeal(level, RandomSource.create(step), pos.above(), level.getBlockState(pos.above()));
                }
                helper.assertTrue(level.getBlockState(pos).getValue(ERDoubleCropBlock.AGE) == 7, "upper-half bonemeal matures crop");
                helper.assertTrue(!crop.isValidBonemealTarget(level, pos, level.getBlockState(pos)), "mature crop rejects more bonemeal");
                HarvestPlantGameTests.breakAsPlayer(level, player, pos);
                helper.assertItemEntityNotPresent(seed);
                helper.assertItemEntityNotPresent(product);
            }
        }
    }

    static void yam(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(3, 1, 3));
        var crop = ERHarvestBlocks.STAR_PATTERN_YAM.get();
        var item = (BlockItem) ERItems.STAR_PATTERN_YAM.get();
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        level.setBlockAndUpdate(pos.below(), Blocks.DIRT.defaultBlockState());
        HarvestPlantGameTests.place(level, player, item, pos);
        helper.assertTrue(level.getBlockState(pos).isAir(), "yam requires tilled soil");
        level.setBlockAndUpdate(pos.below(), ERTerrainBlocks.EDEN_FARMLAND.get().defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
        HarvestPlantGameTests.place(level, player, item, pos);
        helper.assertTrue(level.getBlockState(pos).is(crop), "yam tuber plants its crop");
        for (int tick = 0; tick < 1200 && !crop.isMaxAge(level.getBlockState(pos)); tick++) {
            level.getBlockState(pos).randomTick(level, pos, RandomSource.create(tick + 800));
        }
        helper.assertTrue(crop.isMaxAge(level.getBlockState(pos)), "yam naturally reaches maturity");
        var drops = Block.getDrops(level.getBlockState(pos), level, pos, null);
        helper.assertTrue(drops.stream().allMatch(s -> s.is(item)) && drops.stream().mapToInt(ItemStack::getCount).sum() >= 2,
                "mature yam yields both harvest and replantable tubers");
        level.setBlockAndUpdate(pos, crop.defaultBlockState());
        for (int step = 0; step < 4; step++) {
            crop.performBonemeal(level, RandomSource.create(step), pos, level.getBlockState(pos));
        }
        helper.assertTrue(level.getBlockState(pos).getValue(CropBlock.AGE) == 7, "yam bonemeal reaches maturity");
    }
}
