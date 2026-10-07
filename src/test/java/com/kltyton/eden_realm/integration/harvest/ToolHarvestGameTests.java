package com.kltyton.eden_realm.integration.harvest;

import com.kltyton.eden_realm.registry.content.block.ERTerrainBlocks;
import com.kltyton.eden_realm.registry.content.item.ERToolItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

final class ToolHarvestGameTests {
    private ToolHarvestGameTests() {
    }

    static void tools(GameTestHelper helper) {
        double[][] expected = {
                {6.8, 1.6}, {4.5, 1.2}, {9.5, 0.9}, {5, 1}, {1, 3},
                {6.5, 1.7}, {4, 1.2}, {9, 1}, {4.5, 1}, {1, 3.2}};
        var miningTargets = java.util.List.of(Blocks.COBWEB, Blocks.STONE, Blocks.OAK_LOG, Blocks.DIRT, Blocks.HAY_BLOCK);
        for (int index = 0; index < 10; index++) {
            var entry = ERToolItems.entries().get(index);
            var stack = new ItemStack(entry.item().get());
            var attributes = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
            helper.assertTrue(attributes != null, "tool has attack attributes");
            helper.assertTrue(Math.abs(attributes.compute(Attributes.ATTACK_DAMAGE, 1, EquipmentSlot.MAINHAND) - expected[index][0]) < 0.00001,
                    "total attack damage matches customer image: " + entry.item().getId());
            helper.assertTrue(Math.abs(attributes.compute(Attributes.ATTACK_SPEED, 4, EquipmentSlot.MAINHAND) - expected[index][1]) < 0.00001,
                    "attack speed matches customer image: " + entry.item().getId());
            helper.assertTrue(stack.getMaxDamage() == (index < 5 ? 850 : 650), "durability matches customer image");
            helper.assertTrue(stack.is(entry.tag()), "tool has native category tag");
            if (index % 5 != 0) {
                float speed = stack.getDestroySpeed(miningTargets.get(index % 5).defaultBlockState());
                helper.assertTrue(speed == (index < 5 ? 7.5F : 7F), "effective tool mining speed matches customer image");
            }
        }
        helper.assertTrue(new ItemStack(ERToolItems.ROCK_STEEL_PICKAXE.get()).isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "rock steel mines diamond-tier blocks");
        helper.assertTrue(!new ItemStack(ERToolItems.SINKING_STAR_PICKAXE.get()).isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "sinking star remains iron tier");
        helper.assertTrue(new ItemStack(ERToolItems.SINKING_STAR_PICKAXE.get()).isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()), "sinking star mines iron-tier ores");
    }

    static void ores(GameTestHelper helper) {
        var stone = new ItemStack(Items.STONE_PICKAXE);
        var iron = new ItemStack(Items.IRON_PICKAXE);
        var wood = new ItemStack(Items.WOODEN_PICKAXE);
        var radiance = ERTerrainBlocks.PRIMAL_RADIANCE_ORE.get().defaultBlockState();
        helper.assertTrue(stone.isCorrectToolForDrops(radiance) && !wood.isCorrectToolForDrops(radiance), "primal radiance requires at least stone pickaxe");
        for (var ore : java.util.List.of(ERTerrainBlocks.RAW_ROCK_COAL_ORE, ERTerrainBlocks.RAW_ROCK_IRON_ORE,
                ERTerrainBlocks.ROCK_STEEL_ORE, ERTerrainBlocks.SINKING_STAR_ORE)) {
            var state = ore.get().defaultBlockState();
            helper.assertTrue(state.requiresCorrectToolForDrops() && iron.isCorrectToolForDrops(state) && !stone.isCorrectToolForDrops(state),
                    "other ores require at least iron pickaxe: " + ore.getId());
            helper.assertTrue(!new ItemStack(Items.IRON_SHOVEL).isCorrectToolForDrops(state), "iron material alone cannot replace pickaxe category");
        }
    }
}
