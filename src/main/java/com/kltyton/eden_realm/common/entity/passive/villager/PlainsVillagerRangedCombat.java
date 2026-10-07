package com.kltyton.eden_realm.common.entity.passive.villager;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ChargedProjectiles;

/** Server-side ranged behavior after the vanilla villager Brain has ticked. */
final class PlainsVillagerRangedCombat {
    private static final double CLOSE_DISTANCE_SQR = 8 * 8;
    private static final double APPROACH_DISTANCE_SQR = 14 * 14;
    private static final double FIRE_DISTANCE_SQR = 20 * 20;
    private static final int UNSEEN_TIMEOUT = 60;
    // The 1-second authored clips follow a 4-tick GeckoLib controller blend.
    private static final int BOW_DRAW_TICKS = 24;
    private static final int MIN_AIM_TICKS = 8;
    private static final int RECOVER_TICKS = 24;

    private final PlainsVillager villager;
    private int cooldown;
    private int sideTicks;
    private int unseenTicks;
    private int aimTicks;
    private int recoveryTicks;
    private boolean strafeRight;

    PlainsVillagerRangedCombat(PlainsVillager villager) {
        this.villager = villager;
    }

    void tick(ServerLevel level) {
        if (villager.combatRole() == PlainsVillager.UNARMED) {
            stop();
            return;
        }
        if (villager.isBaby() || villager.isSleeping() || villager.isSpeaking()) {
            leaveCombat();
            return;
        }
        LivingEntity target = villager.getTarget();
        if (target == null || !target.isAlive() || !(target instanceof Enemy)
                || target.level() != level || villager.distanceToSqr(target) > FIRE_DISTANCE_SQR) {
            leaveCombat();
            return;
        }

        boolean visible = villager.getSensing().hasLineOfSight(target);
        unseenTicks = visible ? 0 : Math.min(unseenTicks + 1, UNSEEN_TIMEOUT + 1);
        if (villager.combatRole() == PlainsVillager.BOW && unseenTicks > UNSEEN_TIMEOUT) {
            villager.setTarget(null);
            leaveCombat();
            return;
        }
        if (recoveryTicks > 0) {
            if (!visible) {
                advanceRecovery();
                return;
            }
            recoveryTicks = 0;
            cooldown = 0;
        }

        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        double distance = villager.distanceToSqr(target);
        if (!visible || distance > APPROACH_DISTANCE_SQR) {
            villager.getNavigation().moveTo(target, 1.1);
        } else if (distance < CLOSE_DISTANCE_SQR) {
            villager.getNavigation().stop();
            villager.getMoveControl().strafe(-0.8F, strafeRight ? 0.65F : -0.65F);
        } else {
            villager.getNavigation().stop();
            villager.getMoveControl().strafe(0.2F, strafeRight ? 0.55F : -0.55F);
        }
        if (++sideTicks >= 40) {
            sideTicks = 0;
            strafeRight = villager.getRandom().nextBoolean();
        }
        villager.lookAt(target, 30.0F, 30.0F);
        villager.yBodyRot = villager.getYRot();
        villager.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (cooldown > 0) {
            cooldown--;
        }
        if (villager.combatRole() == PlainsVillager.BOW) {
            bowTick(level, target, visible);
        } else {
            crossbowTick(level, target, visible);
        }
    }

    private void bowTick(ServerLevel level, LivingEntity target, boolean visible) {
        if (villager.isUsingItem() && villager.getTicksUsingItem() >= BOW_DRAW_TICKS
                && villager.combatAction() == PlainsVillager.BOW) {
            villager.beginCombatAction(PlainsVillager.BOW_HOLD);
            aimTicks = 0;
        }
        if (!visible) { return; }
        if (cooldown > 0) { return; }
        if (!villager.isUsingItem()) {
            villager.startUsingItem(InteractionHand.MAIN_HAND);
            villager.beginCombatAction(PlainsVillager.BOW);
            aimTicks = 0;
        } else if (villager.combatAction() == PlainsVillager.BOW_HOLD && ++aimTicks >= MIN_AIM_TICKS) {
            villager.stopUsingItem();
            ItemStack bow = villager.getMainHandItem();
            ItemStack projectile = villager.getProjectile(bow);
            AbstractArrow arrow = ProjectileUtil.getMobArrow(villager, projectile, 1.0F, bow);
            double dx = target.getX() - villager.getX();
            double dy = target.getY(0.3333333333333333) - arrow.getY();
            double dz = target.getZ() - villager.getZ();
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            Projectile.spawnProjectileUsingShoot(arrow, level, projectile, dx, dy + horizontal * 0.2,
                    dz, 1.6F, 14 - level.getDifficulty().getId() * 4);
            villager.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F);
            aimTicks = 0;
            cooldown = 8;
        }
    }

    private void crossbowTick(ServerLevel level, LivingEntity target, boolean visible) {
        ItemStack weapon = villager.getMainHandItem();
        if (!(weapon.getItem() instanceof CrossbowItem crossbow)) { return; }
        if (!CrossbowItem.isCharged(weapon)) {
            if (cooldown > 0) { return; }
            if (!villager.isUsingItem()) {
                if (!visible) { return; }
                villager.startUsingItem(InteractionHand.MAIN_HAND);
                villager.beginCombatAction(PlainsVillager.CROSSBOW);
            }
            return;
        }
        if (villager.isUsingItem()) {
            villager.releaseUsingItem();
            cooldown = 10;
            return;
        }
        if (!visible) { return; }
        if (cooldown > 0) { return; }
        crossbow.performShooting(level, villager, InteractionHand.MAIN_HAND, weapon, 1.6F,
                14 - level.getDifficulty().getId() * 4, target);
        villager.endCombatAction();
        cooldown = 20;
    }

    private void leaveCombat() {
        int action = villager.combatAction();
        if (villager.combatRole() == PlainsVillager.BOW
                && (action == PlainsVillager.BOW || action == PlainsVillager.BOW_HOLD || recoveryTicks > 0)) {
            if (recoveryTicks == 0) {
                if (villager.isUsingItem()) { villager.stopUsingItem(); }
                villager.beginCombatAction(PlainsVillager.BOW_RECOVER);
                recoveryTicks = RECOVER_TICKS;
                cooldown = 0;
                aimTicks = 0;
            }
            advanceRecovery();
        } else {
            stop();
        }
    }

    private void advanceRecovery() {
        if (--recoveryTicks == 0) {
            villager.endCombatAction();
        }
    }

    private void stop() {
        boolean wasUsing = villager.isUsingItem();
        if (wasUsing) { villager.stopUsingItem(); }
        villager.endCombatAction();
        if (wasUsing && villager.combatRole() == PlainsVillager.CROSSBOW
                && CrossbowItem.isCharged(villager.getMainHandItem())) {
            villager.getMainHandItem().set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
        }
        cooldown = 0;
        sideTicks = 0;
        unseenTicks = 0;
        aimTicks = 0;
        recoveryTicks = 0;
    }
}
