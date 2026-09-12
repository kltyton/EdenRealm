package com.kltyton.eden_realm.common.block.plant;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.block.ERWoodSet;
import com.kltyton.eden_realm.registry.ERBlocks;
import com.kltyton.eden_realm.registry.ERItems;
import com.kltyton.eden_realm.registry.content.ERHarvestBlocks;
import com.mojang.serialization.MapCodec;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@EventBusSubscriber(modid = ERConstants.MOD_ID)
public final class HarvestGameTests {
    private HarvestGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        StructureUtils.testStructuresSourceDir = Path.of(System.getProperty("edenrealm.testStructures"));
        register(event, "flowering_leaves", HarvestGameTests::floweringLeaves);
        register(event, "horizontal_log", HarvestGameTests::horizontalLog);
        register(event, "fruit_growth", HarvestGameTests::fruitGrowth);
        register(event, "grain_pair", HarvestPlantGameTests::grainPair);
        register(event, "amphibious_plants", HarvestPlantGameTests::amphibiousPlants);
    }

    private static void register(RegisterGameTestsEvent event, String name, Consumer<GameTestHelper> test) {
        var environment = event.registerEnvironment(ERConstants.id("harvest_" + name));
        var data = new TestData<>(environment, ERConstants.id("harvest_empty"), 200, 10, true,
                Rotation.NONE, false, 1, 1, true, 0);
        event.registerTest(ERConstants.id("harvest_" + name), new GameTestInstance(data) {
            @Override
            public void run(GameTestHelper helper) {
                test.accept(helper);
                helper.succeed();
            }

            @Override
            public MapCodec<? extends GameTestInstance> codec() {
                return MapCodec.unit(this);
            }

            @Override
            protected MutableComponent typeDescription() {
                return Component.literal("Eden Realm harvest regression");
            }
        });
    }

    private static void floweringLeaves(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos support = helper.absolutePos(new BlockPos(3, 5, 3));
        for (int species = 0; species < 3; species++) {
            var leaves = ERHarvestBlocks.floweringLeaves().get(species).get();
            var fruit = ERHarvestBlocks.fruits().get(species).get();
            BlockState leaf = leaves.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
            level.setBlockAndUpdate(support, leaf);
            level.setBlockAndUpdate(support.below(), Blocks.AIR.defaultBlockState());
            require(leaves.isValidBonemealTarget(level, support, leaf), "air below leaves must accept bonemeal");
            leaves.performBonemeal(level, RandomSource.create(7), support, leaf);
            require(level.getBlockState(support.below()).is(fruit), "bonemeal must create the matching independent bud");
            require(!leaves.isValidBonemealTarget(level, support, leaf), "occupied space must reject bonemeal");
            level.setBlockAndUpdate(support.below(), Blocks.WATER.defaultBlockState());
            require(!leaves.isValidBonemealTarget(level, support, leaf), "water is not air");
            RandomSource actual = RandomSource.create(109);
            RandomSource expected = RandomSource.create(109);
            int grown = 0;
            for (int tick = 0; tick < 2000; tick++) {
                level.setBlockAndUpdate(support.below(), Blocks.AIR.defaultBlockState());
                boolean shouldGrow = expected.nextFloat() < 0.005F;
                leaf.randomTick(level, support, actual);
                boolean didGrow = level.getBlockState(support.below()).is(fruit);
                require(didGrow == shouldGrow, "natural bud chance must be exactly 0.005 per random tick");
                if (didGrow) {
                    grown++;
                }
            }
            require(grown > 0, "natural growth test must exercise success");
            actual = RandomSource.create(401);
            expected = RandomSource.create(401);
            for (int attempt = 0; attempt < 1000; attempt++) {
                require(leaves.isBonemealSuccess(level, actual, support, leaf) == (expected.nextFloat() < 0.45F),
                        "flowering leaf bonemeal chance must be 45 percent");
            }
            level.removeBlock(support, false);
            require(level.getBlockState(support.below()).isAir(), "removing support must remove the bud");
        }
    }

    private static void horizontalLog(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos support = helper.absolutePos(new BlockPos(3, 5, 3));
        var wood = ERBlocks.woodBlocks(ERWoodSet.TWILIGHT_POMEGRANATE);
        var fruit = ERHarvestBlocks.TWILIGHT_POMEGRANATE.get();
        for (Direction.Axis axis : Direction.Axis.values()) {
            level.setBlockAndUpdate(support.below(), Blocks.AIR.defaultBlockState());
            BlockState log = wood.log().get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
            level.setBlockAndUpdate(support, log);
            require(ERHangingFruitBlock.canGrowBelow(level, support, fruit.defaultBlockState()) == axis.isHorizontal(),
                    "only a horizontal original log supports twilight fruit");
            require(log.isRandomlyTicking() == axis.isHorizontal(), "vertical logs must not random tick");
            if (axis.isHorizontal()) {
                RandomSource random = RandomSource.create(77);
                for (int tick = 0; tick < 2000 && level.getBlockState(support.below()).isAir(); tick++) {
                    log.randomTick(level, support, random);
                }
                require(level.getBlockState(support.below()).is(fruit), "horizontal log must naturally grow its bud");
            }
        }
        level.setBlockAndUpdate(support.below(), Blocks.AIR.defaultBlockState());
        for (var log : List.of(wood.strippedLog(), wood.wood(), wood.strippedWood())) {
            level.setBlockAndUpdate(support, log.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
            require(!fruit.defaultBlockState().canSurvive(level, support.below()), "bark wood and stripped logs must not qualify");
        }
    }

    private static void fruitGrowth(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(3, 4, 3));
        for (int species = 0; species < ERHarvestBlocks.fruits().size(); species++) {
            var holder = ERHarvestBlocks.fruits().get(species);
            var fruit = holder.get();
            BlockState support = species < 3 ? ERHarvestBlocks.floweringLeaves().get(species).get().defaultBlockState()
                    .setValue(LeavesBlock.PERSISTENT, true)
                    : ERBlocks.woodBlocks(ERWoodSet.TWILIGHT_POMEGRANATE).log().get().defaultBlockState()
                            .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
            level.setBlockAndUpdate(pos.above(), support);
            level.setBlockAndUpdate(pos, fruit.defaultBlockState());
            for (int age = 0; age < 4; age++) {
                BlockState state = level.getBlockState(pos);
                require(state.getValue(ERHangingFruitBlock.AGE) == age, "bonemeal must advance one fruit stage");
                require(Block.getDrops(state, level, pos, null).isEmpty(), "immature fruit must not drop ripe produce");
                require(state.getCollisionShape(level, pos).isEmpty(), "hanging plants have no physical collision");
                fruit.performBonemeal(level, RandomSource.create(1), pos, state);
            }
            BlockState ripe = level.getBlockState(pos);
            require(!fruit.isValidBonemealTarget(level, pos, ripe), "mature fruit cannot consume further bonemeal");
            var drops = Block.getDrops(ripe, level, pos, null);
            var product = ERItems.harvestItem(holder.getId().getPath().replace("_hanging", "")).get();
            require(drops.size() == 1 && drops.getFirst().is(product) && drops.getFirst().getCount() == 1,
                    "ripe fruit drops exactly its matching produce");
            level.setBlockAndUpdate(pos, fruit.defaultBlockState());
            RandomSource random = RandomSource.create(22);
            for (int tick = 0; tick < 100; tick++) {
                BlockState state = level.getBlockState(pos);
                state.randomTick(level, pos, random);
            }
            require(level.getBlockState(pos).getValue(ERHangingFruitBlock.AGE) == 4, "all four fruits must mature naturally");
        }
    }

    static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
