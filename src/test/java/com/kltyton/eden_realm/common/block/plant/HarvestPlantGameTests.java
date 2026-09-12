package com.kltyton.eden_realm.common.block.plant;

import com.kltyton.eden_realm.registry.ERItems;
import com.kltyton.eden_realm.registry.content.ERHarvestBlocks;
import com.kltyton.eden_realm.registry.content.ERPlantBlocks;
import com.kltyton.eden_realm.registry.content.ERTerrainBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

final class HarvestPlantGameTests {
    private HarvestPlantGameTests() {
    }

    static void grainPair(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos lower = helper.absolutePos(new BlockPos(3, 1, 3));
        Player planter = helper.makeMockPlayer(GameType.CREATIVE);
        GameType.CREATIVE.updatePlayerAbilities(planter.getAbilities());
        var grain = ERHarvestBlocks.DEWSPIKE_GRAIN.get();
        level.setBlockAndUpdate(lower.below(), Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
        place(level, planter, ERItems.DEWSPIKE_GRAIN_SEEDS.get(), lower);
        require(level.getBlockState(lower).is(grain) && level.getBlockState(lower.above()).is(grain), "grain seeds must place both halves");
        require(level.getBlockState(lower.above()).getShape(level, lower.above()).isEmpty(), "invisible seedling upper half must have no selection outline");
        require(!level.getBlockState(lower.above()).isRandomlyTicking(), "upper half must not grow independently");
        for (int step = 0; step < 4; step++) {
            grain.performBonemeal(level, RandomSource.create(step), lower.above(), level.getBlockState(lower.above()));
            require(level.getBlockState(lower).getValue(ERDewspikeGrainBlock.AGE)
                    .equals(level.getBlockState(lower.above()).getValue(ERDewspikeGrainBlock.AGE)), "bonemeal on top must synchronize both ages");
        }
        BlockState ripe = level.getBlockState(lower);
        require(ripe.getValue(ERDewspikeGrainBlock.AGE) == 7, "grain must mature at age seven");
        require(!grain.isValidBonemealTarget(level, lower.above(), level.getBlockState(lower.above())), "mature upper half rejects bonemeal");
        for (int x = 0; x < 128; x++) {
            BlockPos sample = lower.offset(x, 0, 0);
            BlockState upper = ripe.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER);
            int variant = ERDewspikeGrainBlock.matureVariant(ripe, sample);
            require(variant == ERDewspikeGrainBlock.matureVariant(upper, sample.above()), "both halves must use the same model seed");
            double upperHeight = upper.getShape(level, sample.above()).bounds().maxY * 16;
            require(Math.abs(upperHeight - (variant == 0 ? 11 : 13)) < 0.001, "mature outline must follow 7 or 7_tall");
        }
        Player harvester = helper.makeMockPlayer(GameType.SURVIVAL);
        breakAsPlayer(level, harvester, lower.above());
        require(level.getBlockState(lower).isAir() && level.getBlockState(lower.above()).isAir(), "harvesting top removes both halves");
        helper.assertItemEntityCountIs(ERItems.DEWSPIKE_GRAIN.get(), new BlockPos(3, 1, 3), 3, 1);
        helper.assertItemEntityCountIs(ERItems.DEWSPIKE_GRAIN_SEEDS.get(), new BlockPos(3, 1, 3), 3, 1);
        helper.despawnItem(new BlockPos(3, 1, 3), 4);
        place(level, planter, ERItems.DEWSPIKE_GRAIN_SEEDS.get(), lower);
        for (int tick = 0; tick < 1000 && level.getBlockState(lower).getValue(ERDewspikeGrainBlock.AGE) < 7; tick++) {
            level.getBlockState(lower).randomTick(level, lower, level.getRandom());
        }
        require(level.getBlockState(lower).getValue(ERDewspikeGrainBlock.AGE) == 7, "grain must also mature by natural random ticks");
        breakAsPlayer(level, planter, lower.above());
        helper.assertItemEntityNotPresent(ERItems.DEWSPIKE_GRAIN.get());
        helper.assertItemEntityNotPresent(ERItems.DEWSPIKE_GRAIN_SEEDS.get());
    }

    static void amphibiousPlants(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos lower = helper.absolutePos(new BlockPos(3, 1, 3));
        Player planter = helper.makeMockPlayer(GameType.CREATIVE);
        for (Block plant : List.of(ERPlantBlocks.WATER_SCALLION.get(), ERPlantBlocks.WATER_FERN.get(),
                ERPlantBlocks.GRAY_SPIKE_REED.get(), ERPlantBlocks.PURPLE_GLOW_CATTAIL.get())) {
            BlockItem item = (BlockItem) plant.asItem();
            for (Block soil : List.of(Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.MUD,
                    ERTerrainBlocks.EDEN_GRASS_BLOCK.get(), ERTerrainBlocks.EDEN_DIRT.get())) {
                clearPlant(level, lower);
                level.setBlockAndUpdate(lower.below(), soil.defaultBlockState());
                place(level, planter, item, lower);
                BlockState state = level.getBlockState(lower);
                require(state.is(plant), "dry placement must accept requested soil: " + plant + " on " + soil);
                require(!state.getValue(BlockStateProperties.WATERLOGGED), "dry plant must not create water");
                require(state.canSurvive(level, lower), "dry planted state must survive");
                require(state.getCollisionShape(level, lower).isEmpty(), "ordinary plants must have no collision");
                if (plant instanceof DoublePlantBlock) {
                    require(level.getBlockState(lower.above()).is(plant), "dry tall plant must have an upper half");
                }
            }
            clearPlant(level, lower);
            level.setBlockAndUpdate(lower.below(), Blocks.STONE.defaultBlockState());
            place(level, planter, item, lower);
            require(level.getBlockState(lower).isAir(), "dry stone must reject these plants");
            level.setBlockAndUpdate(lower, Blocks.WATER.defaultBlockState());
            place(level, planter, item, lower);
            BlockState wet = level.getBlockState(lower);
            require(wet.is(plant) && wet.getValue(BlockStateProperties.WATERLOGGED), "original source-water placement must remain valid");
            ItemStack bucket = ((SimpleWaterloggedBlock) plant).pickupBlock(planter, level, lower, wet);
            require(bucket.is(Items.WATER_BUCKET), "water extraction must return a water bucket");
            require(level.getBlockState(lower).isAir(), "drained plant must not survive on dry stone");
            clearPlant(level, lower);
            level.setBlockAndUpdate(lower.below(), Blocks.DIRT.defaultBlockState());
            level.setBlockAndUpdate(lower, Blocks.WATER.defaultBlockState());
            place(level, planter, item, lower);
            ((SimpleWaterloggedBlock) plant).pickupBlock(planter, level, lower, level.getBlockState(lower));
            require(level.getBlockState(lower).is(plant), "drained plant must remain on supported dry soil");
        }
    }

    private static void clearPlant(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }

    private static void place(ServerLevel level, Player player, BlockItem item, BlockPos pos) {
        ItemStack stack = new ItemStack(item);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0, 0.5, 0), Direction.UP, pos.below(), false);
        item.place(new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, stack, hit));
    }

    private static void breakAsPlayer(ServerLevel level, Player player, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        block.playerWillDestroy(level, pos, state, player);
        level.removeBlock(pos, false);
        if (!player.preventsBlockDrops()) {
            block.playerDestroy(level, player, pos, state, null, ItemStack.EMPTY);
        }
    }

    private static void require(boolean condition, String message) {
        HarvestGameTests.require(condition, message);
    }
}
