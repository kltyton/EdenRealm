package com.kltyton.eden_realm.integration.harvest;

import com.kltyton.eden_realm.common.block.fruit.ERHangingFruitBlock;

import com.kltyton.eden_realm.common.block.tree.ERWoodSet;
import com.kltyton.eden_realm.common.block.fruit.ERFruitBlock;
import com.kltyton.eden_realm.common.entity.fruit.ERFallingFruitEntity;
import com.kltyton.eden_realm.registry.ERBlocks;
import com.kltyton.eden_realm.registry.content.block.ERHarvestBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class FruitPhysicsGameTests {
    private FruitPhysicsGameTests() {
    }

    static void fallingCounts(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(3, 6, 3));
        BlockPos landed = pos.below(5);
        level.setBlockAndUpdate(landed.below(), Blocks.STONE.defaultBlockState());
        for (var holder : ERHarvestBlocks.groundFruits()) {
            var block = holder.get();
            int maximum = block.defaultBlockState().hasProperty(ERFruitBlock.COUNT) ? 4 : 1;
            for (int count = 1; count <= maximum; count++) {
                level.removeBlock(landed, false);
                var state = maximum == 4 ? block.defaultBlockState().setValue(ERFruitBlock.COUNT, count) : block.defaultBlockState();
                level.setBlockAndUpdate(pos, state);
                // Only the explicit natural-detachment transition creates a falling entity.
                ERFallingFruitEntity.fall(level, pos, state);
                var entities = level.getEntitiesOfClass(ERFallingFruitEntity.class, new AABB(pos).inflate(1));
                HarvestGameTests.require(entities.size() == 1 && level.getBlockState(pos).isAir(), "unsupported fruit must become one falling entity");
                var falling = entities.getFirst();
                for (int tick = 0; tick < 100 && falling.isAlive(); tick++) {
                    falling.tick();
                }
                HarvestGameTests.require(!falling.isAlive() && level.getBlockState(landed).equals(state), "landing preserves fruit type and whole stack count");
                level.removeBlock(landed.below(), false);
                state.tick(level, landed, RandomSource.create(1));
                HarvestGameTests.require(level.getBlockState(landed).equals(state), "landed fruit remains ordinary when its floor disappears");
                level.setBlockAndUpdate(landed.below(), Blocks.STONE.defaultBlockState());
                // Native timeout must also return the entire stack, not a single item.
                var timeout = ERFallingFruitEntity.fall(level, pos, state);
                timeout.time = 601;
                timeout.tick();
                var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
                HarvestGameTests.require(drops.stream().mapToInt(item -> item.getItem().getCount()).sum() == count,
                        "failed falling placement must preserve the complete stack");
                drops.forEach(ItemEntity::discard);
            }
        }
    }

    static void fallProbability(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(3, 5, 3));
        for (int species = 0; species < 4; species++) {
            var fruit = ERHarvestBlocks.fruits().get(species).get();
            boolean shouldFall = species != 1;
            level.setBlockAndUpdate(pos.above(), support(species));
            RandomSource actual = RandomSource.create(207);
            RandomSource expected = RandomSource.create(207);
            for (int tick = 0; tick < 200; tick++) {
                var ripe = fruit.defaultBlockState().setValue(ERHangingFruitBlock.AGE, 4);
                level.setBlockAndUpdate(pos, ripe);
                boolean detached = shouldFall && expected.nextFloat() < 0.10F;
                // When a mature fruit receives one random tick.
                ripe.randomTick(level, pos, actual);
                HarvestGameTests.require(level.getBlockState(pos).isAir() == detached, "each mature random tick must use exactly 10 percent for the three falling fruits");
                level.getEntitiesOfClass(ERFallingFruitEntity.class, new AABB(pos).inflate(1)).forEach(ERFallingFruitEntity::discard);
            }
        }
    }

    static void anvilDamage(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(3, 5, 3));
        for (double distance : new double[] {0.5, 1, 2, 5.5, 30}) {
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            level.addFreshEntity(player);
            var fruit = ERFallingFruitEntity.fall(level, pos, ERHarvestBlocks.TIDE_SONG_COCONUT_GROUND.get().defaultBlockState());
            // When the native fall-damage callback is evaluated at a known distance.
            fruit.causeFallDamage(distance, 1, level.damageSources().fall());
            float fruitDamage = 20 - player.getHealth();
            HarvestGameTests.require(fruitDamage == Math.min(20, Math.max(0, Math.min(40, Math.ceil(distance - 1) * 2))),
                    "native anvil formula must inflict the expected numerical damage");
            player.discard();
            fruit.discard();
            var anvilTarget = helper.makeMockPlayer(GameType.SURVIVAL);
            anvilTarget.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            level.addFreshEntity(anvilTarget);
            var anvil = FallingBlockEntity.fall(level, pos, Blocks.ANVIL.defaultBlockState());
            anvil.setHurtsEntities(2, 40);
            anvil.causeFallDamage(distance, 1, level.damageSources().fall());
            HarvestGameTests.require(fruitDamage == 20 - anvilTarget.getHealth(), "fruit damage must match native anvil damage at the same distance");
            anvilTarget.discard();
            anvil.discard();
        }
    }

    static void adjacentFruit(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(3, 4, 3));
        for (int species = 0; species < 4; species++) {
            var fruit = ERHarvestBlocks.fruits().get(species).get();
            var ripe = fruit.defaultBlockState().setValue(ERHangingFruitBlock.AGE, 4);
            level.setBlockAndUpdate(pos.above(), support(species));
            level.setBlockAndUpdate(pos.east().above(), support(species));
            level.setBlockAndUpdate(pos, ripe);
            level.setBlockAndUpdate(pos.east(), ripe.setValue(ERHangingFruitBlock.AGE, 3));
            fruit.performBonemeal(level, RandomSource.create(1), pos.east(), level.getBlockState(pos.east()));
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos adjacent = pos.relative(direction);
                if (!adjacent.equals(pos.east())) {
                    level.setBlockAndUpdate(adjacent, Blocks.STONE.defaultBlockState());
                    level.removeBlock(adjacent, false);
                }
            }
            ripe.tick(level, pos, RandomSource.create(1));
            HarvestGameTests.require(level.getBlockState(pos).equals(ripe)
                    && level.getBlockState(pos.east()).equals(ripe), "adjacent ripe fruits coexist through maturity and block updates");
            HarvestGameTests.require(level.getEntitiesOfClass(ERFallingFruitEntity.class, new AABB(pos).inflate(2)).isEmpty(),
                    "neighbor maturity and source-free updates never create falling fruit");
        }
    }

    private static BlockState support(int species) {
        return ERHarvestBlocks.floweringLeaves().get(species).get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    static void twilightBody(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        var fruit = ERHarvestBlocks.TWILIGHT_POMEGRANATE_GROUND.get().defaultBlockState();
        var shape = fruit.getShape(level, pos);
        var collision = fruit.getCollisionShape(level, pos);
        var leafOnly = new Vec3(pos.getX() + 0.02, pos.getY() + 0.5, pos.getZ() - 1);
        var throughLeaves = leafOnly.add(0, 0, 3);
        HarvestGameTests.require(shape.clip(leafOnly, throughLeaves, pos) == null
                && collision.clip(leafOnly, throughLeaves, pos) == null, "decorative leaf-only area has no outline or collision");
        var throughBody = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() - 1);
        HarvestGameTests.require(shape.clip(throughBody, throughBody.add(0, 0, 3), pos) != null
                && collision.clip(throughBody, throughBody.add(0, 0, 3), pos) != null, "fruit body retains outline and collision");
        HarvestGameTests.require(shape.bounds().equals(new AABB(1.5 / 16, 0, 1.5 / 16, 14.5 / 16, 1, 14.5 / 16)),
                "body and stem bounds follow original authored solids");
        for (int age : new int[]{3, 4}) {
            var hanging = ERHarvestBlocks.TWILIGHT_POMEGRANATE.get().defaultBlockState().setValue(ERHangingFruitBlock.AGE, age);
            HarvestGameTests.require(hanging.getShape(level, pos).clip(leafOnly, throughLeaves, pos) == null,
                    "twilight hanging leaf planes do not affect outline");
            HarvestGameTests.require(hanging.getCollisionShape(level, pos).isEmpty(), "hanging fruit stays noncolliding");
        }
    }

    static void twilightContact(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        var fruit = ERHarvestBlocks.TWILIGHT_POMEGRANATE_GROUND.get().defaultBlockState();
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos, fruit);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            level.setBlockAndUpdate(pos.relative(direction), Blocks.STONE.defaultBlockState());
        }
        HarvestGameTests.require(fruit.canSurvive(level, pos), "twilight fruit permits solid blocks on every horizontal side");
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        fruit.entityInside(level, pos, player, net.minecraft.world.entity.InsideBlockEffectApplier.NOOP, true);
        HarvestGameTests.require(player.getHealth() == 19, "twilight contact deals cactus-equivalent one-point damage");
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            level.removeBlock(pos.relative(direction), false);
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            var target = helper.makeMockPlayer(GameType.SURVIVAL);
            var start = new net.minecraft.world.phys.Vec3(pos.getX() + 0.5 + direction.getStepX() * 0.95,
                    pos.getY(), pos.getZ() + 0.5 + direction.getStepZ() * 0.95);
            target.setPos(start);
            target.move(net.minecraft.world.entity.MoverType.SELF,
                    new net.minecraft.world.phys.Vec3(-direction.getStepX() * 0.5, 0, -direction.getStepZ() * 0.5));
            target.applyEffectsFromBlocks(start, target.position());
            HarvestGameTests.require(target.getHealth() == 19, "walking into each fruit side must trigger contact damage: " + direction);
        }
        var standing = helper.makeMockPlayer(GameType.SURVIVAL);
        var above = new net.minecraft.world.phys.Vec3(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5);
        standing.setPos(above);
        standing.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(0, -1, 0));
        standing.applyEffectsFromBlocks(above, standing.position());
        HarvestGameTests.require(standing.getHealth() == 19, "standing on twilight fruit must trigger contact damage");
    }
}
