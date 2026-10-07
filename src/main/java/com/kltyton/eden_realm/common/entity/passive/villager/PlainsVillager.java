package com.kltyton.eden_realm.common.entity.passive.villager;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class PlainsVillager extends Villager implements GeoEntity {
    static final int UNARMED = 0;
    static final int BOW = 1;
    static final int CROSSBOW = 2;
    static final int BOW_HOLD = 3;
    static final int BOW_RECOVER = 4;
    private static final EntityDataAccessor<Integer> COMBAT_ROLE =
            SynchedEntityData.defineId(PlainsVillager.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COMBAT_ACTION =
            SynchedEntityData.defineId(PlainsVillager.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COMBAT_SEQUENCE =
            SynchedEntityData.defineId(PlainsVillager.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DIALOGUE =
            SynchedEntityData.defineId(PlainsVillager.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> SPEECH_END =
            SynchedEntityData.defineId(PlainsVillager.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> SPEECH_SEQUENCE =
            SynchedEntityData.defineId(PlainsVillager.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DIALOGUE_GESTURE =
            SynchedEntityData.defineId(PlainsVillager.class, EntityDataSerializers.INT);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.plains_villager.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.plains_villager.walk");
    private static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("animation.plains_villager.sleep");
    private static final RawAnimation TALK = RawAnimation.begin().thenLoop("animation.plains_villager.talk");
    private static final RawAnimation BOW_AIM = RawAnimation.begin().thenPlayAndHold("animation.plains_villager.bow_aim");
    private static final RawAnimation BOW_HOLD_ANIMATION = RawAnimation.begin().thenLoop("animation.plains_villager.bow_hold");
    private static final RawAnimation BOW_RECOVER_ANIMATION = RawAnimation.begin().thenPlay("animation.plains_villager.bow_release_recover");
    private static final RawAnimation CROSSBOW_LOAD = RawAnimation.begin().thenPlayAndHold("animation.plains_villager.crossbow_load");
    // TODO: 仅用于对话动作测试；正式交互确定后替换随机播放开心、摇摆互动、摇头的逻辑。
    private static final List<RawAnimation> DIALOGUE_GESTURES = List.of(
            RawAnimation.begin().thenPlay("animation.plains_villager.happy").thenLoop("animation.plains_villager.idle"),
            RawAnimation.begin().thenPlay("animation.plains_villager.sway").thenLoop("animation.plains_villager.idle"),
            RawAnimation.begin().thenPlay("animation.plains_villager.shake_head").thenLoop("animation.plains_villager.idle"));
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final PlainsVillagerRangedCombat rangedCombat = new PlainsVillagerRangedCombat(this);
    private ItemStack combatWeapon = ItemStack.EMPTY;
    private VillagerSpeechPlayback clientPlayback = VillagerSpeechPlayback.NONE;
    private int lastSpeechSequence;
    private int clientSpeechLine;
    private int clientDialogueGesture;
    private int lastBodySpeechSequence;
    private int lastCombatSequence;
    private int lastCombatAction;
    private @Nullable Player conversationPartner;

    public PlainsVillager(EntityType<? extends PlainsVillager> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder data) {
        super.defineSynchedData(data);
        data.define(COMBAT_ROLE, UNARMED);
        data.define(COMBAT_ACTION, UNARMED);
        data.define(COMBAT_SEQUENCE, 0);
        data.define(DIALOGUE, 0);
        data.define(SPEECH_END, 0L);
        data.define(SPEECH_SEQUENCE, 0);
        data.define(DIALOGUE_GESTURE, 0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                (target, serverLevel) -> target instanceof Enemy) {
            @Override
            public boolean canUse() {
                return combatRole() != UNARMED && !isBaby() && !isSpeaking() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return combatRole() != UNARMED && !isBaby() && !isSpeaking() && super.canContinueToUse();
            }
        });
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                   EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, groupData);
        int roll = random.nextInt(10);
        int role = roll < 3 ? BOW : roll < 6 ? CROSSBOW : UNARMED;
        entityData.set(COMBAT_ROLE, role);
        if (role != UNARMED) {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(role == BOW ? Items.BOW : Items.CROSSBOW));
        }
        return result;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("EdenCombatRole", combatRole());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        int role = input.getIntOr("EdenCombatRole", UNARMED);
        entityData.set(COMBAT_ROLE, role == BOW || role == CROSSBOW ? role : UNARMED);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isAlive() || isSleeping()) {
            return InteractionResult.PASS;
        }
        ItemStack offered = player.getItemInHand(hand);
        if (!level().isClientSide() && !offered.isEmpty()) {
            ItemStack previous = getOffhandItem().copy();
            setItemSlot(EquipmentSlot.OFFHAND, offered.copyWithCount(1));
            setGuaranteedDrop(EquipmentSlot.OFFHAND);
            setPersistenceRequired();
            offered.shrink(1);
            if (!previous.isEmpty()) {
                player.getInventory().add(previous);
                if (!previous.isEmpty()) { player.drop(previous, false); }
            }
            return InteractionResult.SUCCESS;
        }
        if (!level().isClientSide() && offered.isEmpty() && player.isSecondaryUseActive()
                && !getOffhandItem().isEmpty()) {
            ItemStack previous = getOffhandItem().copy();
            setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            player.getInventory().add(previous);
            if (!previous.isEmpty()) { player.drop(previous, false); }
            return InteractionResult.SUCCESS;
        }
        if (!level().isClientSide() && !isSpeaking()) {
            int line = random.nextInt(2) + 1;
            entityData.set(DIALOGUE, line);
            entityData.set(DIALOGUE_GESTURE, random.nextInt(DIALOGUE_GESTURES.size()));
            // Measured Vorbis durations: reunion 2.837313 s; relief 3.477292 s.
            entityData.set(SPEECH_END, level().getGameTime() + (line == 1 ? 57L : 70L));
            entityData.set(SPEECH_SEQUENCE, entityData.get(SPEECH_SEQUENCE) + 1);
            conversationPartner = player;
            lookAtConversationPartner();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void openTradingScreen(Player player, Component title, int level) {
        // This villager has dialogue only, including when external data supplies Offers.
        setTradingPlayer(null);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        restoreCombatWeapon();
        rangedCombat.tick(level);
        lookAtConversationPartner();
    }

    private void restoreCombatWeapon() {
        int role = combatRole();
        if (role == UNARMED) { return; }
        var expected = role == BOW ? Items.BOW : Items.CROSSBOW;
        ItemStack held = getMainHandItem();
        if (!combatWeapon.is(expected) || (held.is(expected) && held != combatWeapon)) {
            combatWeapon = held.is(expected) ? held : new ItemStack(expected);
        }
        // Vanilla ShowTradesToPlayer clears a villager's main hand near players.
        // Restore the same stack so a charging crossbow keeps its loaded components.
        if (held != combatWeapon) {
            setItemSlot(EquipmentSlot.MAINHAND, combatWeapon);
        }
    }

    private void lookAtConversationPartner() {
        if (conversationPartner == null) { return; }
        if (!isSpeaking() || !conversationPartner.isAlive() || conversationPartner.isRemoved()
                || conversationPartner.level() != level() || distanceToSqr(conversationPartner) > 64) {
            if (getBrain().getMemory(MemoryModuleType.LOOK_TARGET).orElse(null) instanceof EntityTracker tracker
                    && tracker.getEntity() == conversationPartner) {
                getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
            }
            conversationPartner = null;
            return;
        }
        getNavigation().stop();
        getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(conversationPartner, true));
        getLookControl().setLookAt(conversationPartner, 30.0F, 30.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            int sequence = entityData.get(SPEECH_SEQUENCE);
            if (sequence != lastSpeechSequence) {
                lastSpeechSequence = sequence;
                clientPlayback.stop();
                clientSpeechLine = entityData.get(DIALOGUE);
                clientDialogueGesture = entityData.get(DIALOGUE_GESTURE);
                long remaining = entityData.get(SPEECH_END) - level().getGameTime();
                long duration = clientSpeechLine == 1 ? 57L : 70L;
                // Do not replay a whole line when a player starts tracking mid-speech.
                clientPlayback = clientSpeechLine > 0 && remaining >= duration - 5
                        ? VillagerSpeechPlayback.ClientFactory.play(this, clientSpeechLine) : VillagerSpeechPlayback.NONE;
            }
            if (!isAlive()) { clientPlayback.stop(); }
            return;
        }
        if (!level().isClientSide() && entityData.get(DIALOGUE) != 0 && !isSpeaking()) {
            entityData.set(DIALOGUE, 0);
            entityData.set(SPEECH_END, 0L);
            lookAtConversationPartner();
        }
    }

    public boolean isSpeaking() {
        if (level().isClientSide()) { return clientPlayback.isPlaying(); }
        return isAlive() && entityData.get(DIALOGUE) != 0 && level().getGameTime() < entityData.get(SPEECH_END);
    }

    public Component dialogueText() {
        return Component.translatable("dialogue.eden_realm.plains_villager."
                + ((level().isClientSide() ? clientSpeechLine : entityData.get(DIALOGUE)) == 1 ? "reunion" : "relief"));
    }

    int combatRole() {
        return entityData.get(COMBAT_ROLE);
    }

    int combatAction() {
        return entityData.get(COMBAT_ACTION);
    }

    void beginCombatAction(int action) {
        entityData.set(COMBAT_ACTION, action);
        entityData.set(COMBAT_SEQUENCE, entityData.get(COMBAT_SEQUENCE) + 1);
    }

    void endCombatAction() {
        entityData.set(COMBAT_ACTION, UNARMED);
    }

    public ItemStack visibleHeldItem() {
        if (entityData.get(COMBAT_ACTION) != UNARMED || getOffhandItem().isEmpty()) {
            return getMainHandItem();
        }
        return getOffhandItem();
    }

    @Override
    public ItemStack getProjectile(ItemStack heldWeapon) {
        if (combatRole() != UNARMED && (heldWeapon.is(Items.BOW) || heldWeapon.is(Items.CROSSBOW))) {
            return new ItemStack(Items.ARROW);
        }
        return super.getProjectile(heldWeapon);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("body", 4, test -> {
            if (lastBodySpeechSequence != lastSpeechSequence) {
                lastBodySpeechSequence = lastSpeechSequence;
                test.controller().reset();
            }
            return test.setAndContinue(isSleeping() ? SLEEP : isSpeaking()
                    ? DIALOGUE_GESTURES.get(clientDialogueGesture) : test.isMoving() ? WALK : IDLE);
        }));
        controllers.add(new AnimationController<>("mouth", 0, test -> {
            if (isSpeaking()) { return test.setAndContinue(TALK); }
            test.controller().reset();
            return PlayState.STOP;
        }));
        controllers.add(new AnimationController<>("combat", 4, test -> {
            int action = entityData.get(COMBAT_ACTION);
            if (action == UNARMED) {
                lastCombatAction = UNARMED;
                test.controller().reset();
                return PlayState.STOP;
            }
            int sequence = entityData.get(COMBAT_SEQUENCE);
            if (sequence != lastCombatSequence) {
                lastCombatSequence = sequence;
                if (action == lastCombatAction) { test.controller().reset(); }
            }
            lastCombatAction = action;
            return test.setAndContinue(switch (action) {
                case BOW -> BOW_AIM;
                case BOW_HOLD -> BOW_HOLD_ANIMATION;
                case BOW_RECOVER -> BOW_RECOVER_ANIMATION;
                case CROSSBOW -> CROSSBOW_LOAD;
                default -> throw new IllegalStateException("Unknown plains villager combat action: " + action);
            });
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
