package com.kltyton.eden_realm.common.entity.boss;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import com.kltyton.eden_realm.common.skill.keyframe.KeyframeSkillContext;
import com.kltyton.eden_realm.common.skill.keyframe.KeyframeSkillEntity;
import com.kltyton.eden_realm.common.skill.keyframe.KeyframeSkillHooks;
import com.kltyton.eden_realm.common.skill.keyframe.KeyframeSkillRegistrar;
import com.kltyton.eden_realm.registry.ERSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class MossStoneColossus extends Monster implements GeoEntity, KeyframeSkillEntity {
    public static final String CONTROLLER = "main";
    public static final String SPAWN_ANIMATION = "animation.moss_stone_colossus.spawn";
    public static final String DEATH_ANIMATION = "animation.moss_stone_colossus.death";
    public static final float HITBOX_WIDTH = 3.4F;
    public static final float HITBOX_HEIGHT = 6.0F;

    private static final byte PHASE_NORMAL = 0;
    private static final byte PHASE_SPAWNING = 1;
    private static final byte PHASE_DYING = 2;
    private static final long SPAWN_FALLBACK_TICKS = 110L;
    private static final long DEATH_FALLBACK_TICKS = 140L;

    private static final EntityDataAccessor<Byte> PHASE =
            SynchedEntityData.defineId(MossStoneColossus.class, EntityDataSerializers.BYTE);

    private static final RawAnimation SPAWN = once(SPAWN_ANIMATION);
    private static final RawAnimation IDLE = loop("animation.moss_stone_colossus.idle");
    private static final RawAnimation WEAK_POINT = once("animation.moss_stone_colossus.weak_point");
    private static final RawAnimation DEATH = once(DEATH_ANIMATION);
    private static final RawAnimation WALK_1 = loop("animation.moss_stone_colossus.walk_1");
    private static final RawAnimation WALK_2 = loop("animation.moss_stone_colossus.walk_2");
    private static final RawAnimation[] ATTACKS = {
            once("animation.moss_stone_colossus.attack_1_right_slam"),
            once("animation.moss_stone_colossus.attack_2_left_slam"),
            once("animation.moss_stone_colossus.attack_3_double_ground_slam"),
            once("animation.moss_stone_colossus.attack_4_double_charge"),
            once("animation.moss_stone_colossus.attack_5_summon"),
            once("animation.moss_stone_colossus.attack_6_grab"),
            once("animation.moss_stone_colossus.attack_7_grab_failed"),
            once("animation.moss_stone_colossus.attack_8_throw_success"),
            once("animation.moss_stone_colossus.attack_9_charge_prepare"),
            once("animation.moss_stone_colossus.attack_10_charge"),
            once("animation.moss_stone_colossus.attack_11_charge_end"),
            once("animation.moss_stone_colossus.attack_12_charge_end_early"),
            once("animation.moss_stone_colossus.attack_13_charge_end_middle"),
            once("animation.moss_stone_colossus.attack_14_charge_end_late"),
            once("animation.moss_stone_colossus.attack_15_boulder_slam")
    };

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private long skillStartGameTime;
    private boolean wasMoving;
    private boolean useSecondWalk;

    public MossStoneColossus(EntityType<? extends MossStoneColossus> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.FOLLOW_RANGE, 35.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ARMOR, 2.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(PHASE, PHASE_NORMAL);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            EntitySpawnReason spawnReason,
            @Nullable SpawnGroupData groupData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
        beginSkill(PHASE_SPAWNING);
        return result;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()
                && phase() == PHASE_SPAWNING
                && level().getGameTime() - skillStartGameTime >= SPAWN_FALLBACK_TICKS) {
            completeSpawnAnimation(null);
        }
    }

    @Override
    protected void tickDeath() {
        if (!level().isClientSide() && phase() != PHASE_DYING) {
            beginSkill(PHASE_DYING);
        }
        deathTime++;
        if (!level().isClientSide()
                && level().getGameTime() - skillStartGameTime >= DEATH_FALLBACK_TICKS) {
            completeDeathAnimation(null);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putByte("AnimationPhase", phase());
        output.putLong("SkillStartGameTime", skillStartGameTime);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(PHASE, input.getByteOr("AnimationPhase", PHASE_NORMAL));
        skillStartGameTime = input.getLongOr("SkillStartGameTime", 0L);
        setNoAi(phase() != PHASE_NORMAL);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<MossStoneColossus> controller = new AnimationController<>(
                CONTROLLER, 2, this::selectAnimation)
                .receiveTriggeredAnimations()
                .setParticleKeyframeHandler(event -> {
                    // ParticleStorm's timeline mixin owns dispatch; GeckoLib still requires a non-null handler.
                })
                .setCustomInstructionKeyframeHandler(KeyframeSkillHooks::forward)
                .triggerableAnim("spawn", SPAWN)
                .triggerableAnim("idle", IDLE)
                .triggerableAnim("weak_point", WEAK_POINT)
                .triggerableAnim("death", DEATH)
                .triggerableAnim("walk_1", WALK_1)
                .triggerableAnim("walk_2", WALK_2);
        for (int index = 0; index < ATTACKS.length; index++) {
            controller.triggerableAnim("attack_" + (index + 1), ATTACKS[index]);
        }
        controllers.add(controller);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ERSoundEvents.MOSS_STONE_COLOSSUS_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ERSoundEvents.MOSS_STONE_COLOSSUS_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ERSoundEvents.MOSS_STONE_COLOSSUS_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        playSound(ERSoundEvents.MOSS_STONE_COLOSSUS_STEP.get(), 0.15F, 1.0F);
    }

    @Override
    public void registerKeyframeSkills(KeyframeSkillRegistrar registrar) {
        registrar.point("spawn", CONTROLLER, SPAWN_ANIMATION, "spawn_complete", 4.45,
                this::completeSpawnAnimation);
        registrar.point("death", CONTROLLER, DEATH_ANIMATION, "death_complete", 6.04,
                this::completeDeathAnimation);
    }

    @Override
    public String activeKeyframeSkillId() {
        return switch (phase()) {
            case PHASE_SPAWNING -> "spawn";
            case PHASE_DYING -> "death";
            default -> "";
        };
    }

    @Override
    public long keyframeSkillStartGameTime() {
        return skillStartGameTime;
    }

    private PlayState selectAnimation(AnimationTest<MossStoneColossus> test) {
        if (phase() == PHASE_SPAWNING) {
            wasMoving = false;
            return test.setAndContinue(SPAWN);
        }
        if (phase() == PHASE_DYING || isDeadOrDying()) {
            wasMoving = false;
            return test.setAndContinue(DEATH);
        }
        boolean moving = test.isMoving();
        if (moving && !wasMoving) {
            useSecondWalk = !useSecondWalk;
        }
        wasMoving = moving;
        return test.setAndContinue(moving ? (useSecondWalk ? WALK_2 : WALK_1) : IDLE);
    }

    private void beginSkill(byte newPhase) {
        entityData.set(PHASE, newPhase);
        skillStartGameTime = level().getGameTime();
        setNoAi(true);
    }

    private void completeSpawnAnimation(@Nullable KeyframeSkillContext context) {
        if (phase() != PHASE_SPAWNING) {
            return;
        }
        entityData.set(PHASE, PHASE_NORMAL);
        skillStartGameTime = 0L;
        setNoAi(false);
    }

    private void completeDeathAnimation(@Nullable KeyframeSkillContext context) {
        if (phase() != PHASE_DYING || level().isClientSide() || isRemoved()) {
            return;
        }
        level().broadcastEntityEvent(this, (byte) 60);
        remove(Entity.RemovalReason.KILLED);
    }

    private byte phase() {
        return entityData.get(PHASE);
    }

    private static RawAnimation once(String name) {
        return RawAnimation.begin().thenPlay(name);
    }

    private static RawAnimation loop(String name) {
        return RawAnimation.begin().thenLoop(name);
    }
}
