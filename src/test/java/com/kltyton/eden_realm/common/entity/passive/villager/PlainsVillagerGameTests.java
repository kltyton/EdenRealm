package com.kltyton.eden_realm.common.entity.passive.villager;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.registry.EREntityTypes;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@EventBusSubscriber(modid = ERConstants.MOD_ID)
public final class PlainsVillagerGameTests {
    private PlainsVillagerGameTests() { }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        if (!Boolean.getBoolean("edenrealm.villagerTests")) { return; }
        var environment = event.registerEnvironment(ERConstants.id("villager_dialogue"));
        var data = new TestData<>(environment, ERConstants.id("harvest_empty"), 110, 10, true,
                Rotation.NONE, false, 1, 1, true, 0);
        event.registerTest(ERConstants.id("villager_dialogue"), new GameTestInstance(data) {
            @Override
            public void run(GameTestHelper helper) { dialogue(helper); }
            @Override
            public MapCodec<? extends GameTestInstance> codec() { return MapCodec.unit(this); }
            @Override
            protected MutableComponent typeDescription() { return Component.literal("Villager dialogue lifecycle"); }
        });
        var combatData = new TestData<>(environment, ERConstants.id("harvest_empty"), 130, 10, true,
                Rotation.NONE, false, 1, 1, true, 0);
        event.registerTest(ERConstants.id("villager_bow_cover"), new GameTestInstance(combatData) {
            @Override
            public void run(GameTestHelper helper) { bowCover(helper); }
            @Override
            public MapCodec<? extends GameTestInstance> codec() { return MapCodec.unit(this); }
            @Override
            protected MutableComponent typeDescription() { return Component.literal("Villager bow cover and recovery"); }
        });
        var lossData = new TestData<>(environment, ERConstants.id("harvest_empty"), 60, 10, true,
                Rotation.NONE, false, 1, 1, true, 0);
        event.registerTest(ERConstants.id("villager_bow_draw_loss"), new GameTestInstance(lossData) {
            @Override
            public void run(GameTestHelper helper) { bowDrawLoss(helper); }
            @Override
            public MapCodec<? extends GameTestInstance> codec() { return MapCodec.unit(this); }
            @Override
            protected MutableComponent typeDescription() { return Component.literal("Villager mid-draw target loss"); }
        });
        var crossbowData = new TestData<>(environment, ERConstants.id("harvest_empty"), 85, 10, true,
                Rotation.NONE, false, 1, 1, true, 0);
        event.registerTest(ERConstants.id("villager_crossbow_cover"), new GameTestInstance(crossbowData) {
            @Override
            public void run(GameTestHelper helper) { crossbowCover(helper); }
            @Override
            public MapCodec<? extends GameTestInstance> codec() { return MapCodec.unit(this); }
            @Override
            protected MutableComponent typeDescription() { return Component.literal("Villager crossbow cover and charge"); }
        });
    }

    private static void dialogue(GameTestHelper helper) {
        var level = helper.getLevel();
        var villager = new PlainsVillager(EREntityTypes.PLAINS_VILLAGER.get(), level);
        villager.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(3, 2, 3)));
        villager.setNoAi(true);
        level.addFreshEntity(villager);
        var player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setPos(villager.position().add(0, 0, 2));
        var originalMenu = player.containerMenu;
        villager.getOffers().add(new MerchantOffer(new ItemCost(Items.EMERALD), new ItemStack(Items.BREAD), 10, 1, 0));
        villager.mobInteract(player, InteractionHand.MAIN_HAND);
        require(villager.isSpeaking(), "right click must start speech even with Offers");
        require(villager.getBrain().getMemory(MemoryModuleType.LOOK_TARGET).orElse(null) instanceof EntityTracker tracker
                && tracker.getEntity() == player, "speech must look at the interacting player");
        require(player.containerMenu == originalMenu && !villager.isTrading(), "right click cannot open trading");
        Component selected = villager.dialogueText();
        villager.openTradingScreen(player, Component.literal("Injected trade"), 1);
        require(player.containerMenu == originalMenu, "direct merchant screen entry must remain disabled");
        helper.runAfterDelay(40, () -> {
            villager.mobInteract(player, InteractionHand.OFF_HAND);
            require(villager.dialogueText().equals(selected), "repeated clicks must not replace active speech");
        });
        helper.runAfterDelay(72, () -> {
            require(!villager.isSpeaking(), "speech must expire without being extended by repeat clicks");
            require(villager.getBrain().getMemory(MemoryModuleType.LOOK_TARGET).isEmpty(), "expired dialogue releases its look target");
            villager.mobInteract(player, InteractionHand.MAIN_HAND);
            require(villager.isSpeaking(), "a fresh click after expiry must start another speech");
            require(player.containerMenu == originalMenu && !villager.isTrading(), "dialogue must never become trading");
            villager.discard();
            helper.succeed();
        });
    }

    private static void bowCover(GameTestHelper helper) {
        var level = helper.getLevel();
        PlainsVillager villager = spawnCombatVillager(helper, PlainsVillager.BOW);
        var husk = new Husk(EntityTypes.HUSK, level);
        husk.setPos(helper.absoluteVec(new Vec3(11, 2, 8)));
        husk.setNoAi(true);
        level.addFreshEntity(husk);
        villager.setTarget(husk);
        var combat = new PlainsVillagerRangedCombat(villager);
        PlainsVillager testVillager = villager;
        helper.onEachTick(() -> {
            testVillager.getSensing().tick();
            combat.tick(level);
        });
        helper.runAfterDelay(6, () -> {
            require(Math.abs(Mth.wrapDegrees(testVillager.getYRot() - (-90.0F))) < 10.0F,
                    "bow villager body must turn toward the target");
            for (int y = 1; y <= 6; y++) {
                for (int z = 0; z < 16; z++) { helper.setBlock(new BlockPos(7, y, z), Blocks.STONE); }
            }
        });
        helper.runAfterDelay(38, () -> {
            require(testVillager.isUsingItem(), "covered target must retain the drawn bow");
            require(testVillager.combatAction() == PlainsVillager.BOW_HOLD,
                    "full draw must enter the authored aim hold");
            require(husk.getHealth() == husk.getMaxHealth(), "cover must prevent arrow damage");
        });
        helper.runAfterDelay(70, () -> {
            require(!testVillager.isUsingItem(), "lost target must cancel the draw");
            require(testVillager.combatAction() == PlainsVillager.BOW_RECOVER,
                    "long-hidden target must enter recovery");
            require(testVillager.getTarget() == null, "unseen timeout must release the target");
            require(husk.getHealth() == husk.getMaxHealth(), "canceled draw must not create an arrow");
        });
        helper.runAfterDelay(74, () -> {
            for (int y = 1; y <= 6; y++) {
                for (int z = 0; z < 16; z++) { helper.setBlock(new BlockPos(7, y, z), Blocks.AIR); }
            }
            testVillager.setTarget(husk);
        });
        helper.runAfterDelay(78, () -> require(testVillager.combatAction() == PlainsVillager.BOW,
                "new target must interrupt recovery and restart drawing"));
        helper.runAfterDelay(120, () -> {
            int arrows = level.getEntitiesOfClass(AbstractArrow.class,
                    testVillager.getBoundingBox().inflate(24)).size();
            require(arrows > 0 || husk.getHealth() < husk.getMaxHealth(),
                    "uncovered target must eventually receive an arrow shot");
            testVillager.discard();
            husk.discard();
            helper.succeed();
        });
    }

    private static void bowDrawLoss(GameTestHelper helper) {
        var level = helper.getLevel();
        PlainsVillager villager = spawnCombatVillager(helper, PlainsVillager.BOW);
        var husk = new Husk(EntityTypes.HUSK, level);
        husk.setPos(helper.absoluteVec(new Vec3(11, 2, 8)));
        husk.setNoAi(true);
        level.addFreshEntity(husk);
        villager.setTarget(husk);
        var combat = new PlainsVillagerRangedCombat(villager);
        helper.onEachTick(() -> {
            villager.getSensing().tick();
            combat.tick(level);
        });
        helper.runAfterDelay(8, () -> {
            require(villager.isUsingItem() && villager.combatAction() == PlainsVillager.BOW,
                    "bow must be drawing before target loss");
            villager.setTarget(null);
        });
        helper.runAfterDelay(10, () -> {
            require(!villager.isUsingItem() && villager.combatAction() == PlainsVillager.BOW_RECOVER,
                    "mid-draw target loss must cancel charging and start recovery");
            require(husk.getHealth() == husk.getMaxHealth(), "canceled draw cannot fire");
        });
        helper.runAfterDelay(36, () -> {
            require(villager.combatAction() == PlainsVillager.UNARMED,
                    "recovery must finish once and return to idle");
            villager.discard();
            husk.discard();
            helper.succeed();
        });
    }

    private static void crossbowCover(GameTestHelper helper) {
        var level = helper.getLevel();
        PlainsVillager villager = spawnCombatVillager(helper, PlainsVillager.CROSSBOW);
        var husk = new Husk(EntityTypes.HUSK, level);
        husk.setPos(helper.absoluteVec(new Vec3(11, 2, 8)));
        husk.setNoAi(true);
        level.addFreshEntity(husk);
        villager.setTarget(husk);
        var combat = new PlainsVillagerRangedCombat(villager);
        helper.onEachTick(() -> {
            villager.getSensing().tick();
            combat.tick(level);
        });
        helper.runAfterDelay(6, () -> {
            for (int y = 1; y <= 6; y++) {
                for (int z = 0; z < 16; z++) { helper.setBlock(new BlockPos(7, y, z), Blocks.STONE); }
            }
        });
        helper.runAfterDelay(40, () -> {
            require(CrossbowItem.isCharged(villager.getMainHandItem()),
                    "crossbow must finish loading while the target is covered");
            require(villager.combatAction() == PlainsVillager.CROSSBOW,
                    "loaded crossbow must remain in its aiming pose");
            require(husk.getHealth() == husk.getMaxHealth(), "cover must prevent a crossbow shot");
        });
        helper.runAfterDelay(42, () -> villager.setTarget(null));
        helper.runAfterDelay(44, () -> {
            require(villager.combatAction() == PlainsVillager.UNARMED,
                    "lost target must exit crossbow attack");
            require(CrossbowItem.isCharged(villager.getMainHandItem()),
                    "an already-loaded crossbow keeps its arrow like vanilla");
        });
        helper.runAfterDelay(46, () -> {
            for (int y = 1; y <= 6; y++) {
                for (int z = 0; z < 16; z++) { helper.setBlock(new BlockPos(7, y, z), Blocks.AIR); }
            }
            villager.setTarget(husk);
        });
        helper.runAfterDelay(75, () -> {
            int arrows = level.getEntitiesOfClass(AbstractArrow.class,
                    villager.getBoundingBox().inflate(24)).size();
            require(arrows > 0 || husk.getHealth() < husk.getMaxHealth(),
                    "visible target must receive the retained crossbow shot");
            villager.discard();
            husk.discard();
            helper.succeed();
        });
    }

    private static PlainsVillager spawnCombatVillager(GameTestHelper helper, int role) {
        var level = helper.getLevel();
        Vec3 villagerPos = helper.absoluteVec(new Vec3(3, 2, 8));
        for (int seed = 0; seed < 100; seed++) {
            var candidate = new PlainsVillager(EREntityTypes.PLAINS_VILLAGER.get(), level);
            candidate.setPos(villagerPos);
            candidate.getRandom().setSeed(seed);
            candidate.finalizeSpawn(level, level.getCurrentDifficultyAt(candidate.blockPosition()),
                    EntitySpawnReason.COMMAND, null);
            if (candidate.combatRole() == role) {
                candidate.setNoAi(true);
                level.addFreshEntity(candidate);
                return candidate;
            }
        }
        throw new AssertionError("could not obtain combat villager role " + role);
    }

    private static void require(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }
}
