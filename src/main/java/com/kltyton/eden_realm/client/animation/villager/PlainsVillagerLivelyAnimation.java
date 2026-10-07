package com.kltyton.eden_realm.client.animation.villager;

import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.renderer.base.BoneSnapshots;
import com.kltyton.eden_realm.client.renderer.entity.passive.villager.PlainsVillagerRenderState;
import com.kltyton.eden_realm.common.entity.passive.villager.PlainsVillager;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Client visual attention and secondary motion, applied after authored keyframes.
 */
public final class PlainsVillagerLivelyAnimation {
    private static final double ACQUIRE_RADIUS = 6;
    private static final double KEEP_RADIUS = 7;
    private static final String[] AUTHORED_HEAD_CHAIN = {"Root", "body", "UpperBody", "head"};
    private final Map<PlainsVillager, Entry> states = new WeakHashMap<>();

    private static final class Entry {
        final PlainsVillagerMotion motion;
        @Nullable UUID target;
        double nextScan = Double.NEGATIVE_INFINITY;
        double lastTime = Double.NaN;

        Entry(PlainsVillager entity) {
            UUID id = entity.getUUID();
            motion = new PlainsVillagerMotion(id.getMostSignificantBits() ^ id.getLeastSignificantBits());
        }
    }

    /**
     * Contains no entity or Level reference; bone application only consumes this frame's data.
     */
    public record Frame(PlainsVillagerMotion motion, double time, double yaw, double pitch,
                        boolean hasTarget, double speed, double bodyYaw, boolean disabled) {
    }

    public Frame extract(PlainsVillager entity, PlainsVillagerRenderState state, float partialTick) {
        Entry entry = states.computeIfAbsent(entity, Entry::new);
        boolean paused = Minecraft.getInstance().isPaused();
        double time = (entity.tickCount + partialTick) / 20.0;
        if (paused && Double.isFinite(entry.lastTime)) {
            time = entry.lastTime;
        }
        if (time < entry.lastTime) {
            entry.nextScan = Double.NEGATIVE_INFINITY;
        }
        entry.lastTime = time;
        boolean disabled = !entity.isAlive() || entity.isSleeping() || state.bedOrientation != null;
        if (disabled) {
            entry.target = null;
        } else if (!state.speaking && !paused && time >= entry.nextScan) {
            Player previous = player(entity, entry.target);
            if (previous == null || !valid(entity, previous, KEEP_RADIUS, state.bodyRot)) {
                Player nearest = null;
                double distance = ACQUIRE_RADIUS * ACQUIRE_RADIUS;
                for (Player candidate : entity.level().players()) {
                    double candidateDistance = entity.distanceToSqr(candidate);
                    if (candidateDistance < distance && valid(entity, candidate, ACQUIRE_RADIUS, state.bodyRot)) {
                        nearest = candidate;
                        distance = candidateDistance;
                    }
                }
                entry.target = nearest == null ? null : nearest.getUUID();
            }
            entry.nextScan = time + 0.2;
        }
        Player target = player(entity, entry.target);
        double yaw = 0;
        double pitch = 0;
        if (state.speaking) {
            // The server's LookControl already tracks the conversation partner, including in multiplayer.
            yaw = state.yRot;
            pitch = state.xRot;
        } else if (target != null) {
            Vec3 direction = target.getEyePosition(partialTick).subtract(entity.getEyePosition(partialTick));
            yaw = relativeYaw(direction, state.bodyRot);
            pitch = -Math.toDegrees(Math.atan2(direction.y, Math.max(1e-5, Math.hypot(direction.x, direction.z))));
        }
        double speed = PlainsVillagerMotion.clamp(entity.getDeltaMovement().horizontalDistance() / 0.18, 0, 1);
        return new Frame(entry.motion, time, yaw, pitch, state.speaking || target != null, speed, state.bodyRot, disabled);
    }

    public void apply(Frame frame, BoneSnapshots bones) {
        BoneSnapshot flags = required(bones, "alive_flags");
        double authoredYaw = 0;
        double authoredPitch = 0;
        for (String name : AUTHORED_HEAD_CHAIN) {
            BoneSnapshot bone = required(bones, name);
            authoredYaw -= Math.toDegrees(bone.getRotY());
            authoredPitch -= Math.toDegrees(bone.getRotX());
        }
        PlainsVillagerMotion.Pose pose = frame.motion.update(new PlainsVillagerMotion.Input(
                frame.time, frame.yaw, frame.pitch, frame.hasTarget, frame.speed,
                flags.getScaleX(), flags.getScaleY(), flags.getScaleZ(),
                authoredYaw, authoredPitch, frame.bodyYaw, frame.disabled));
        for (int i = 0; i < PlainsVillagerMotion.BONES.length; i++) {
            BoneSnapshot bone = required(bones, PlainsVillagerMotion.BONES[i]);
            double y = pose.get(i, 4);
            if (i == 4 || i == 5) {
                double open = PlainsVillagerMotion.clamp(required(bones, i == 4 ? "Eyelid" : "Eyelid2").getTranslateY(), 0, 2);
                y = Math.max(-open, Math.min(0, y));
            }
            // GeckoLib's X/Y rotation signs oppose Minecraft's facing angles.
            bone.setRotation((float) -Math.toRadians(pose.get(i, 0)),
                            (float) -Math.toRadians(pose.get(i, 1)), (float) Math.toRadians(pose.get(i, 2)))
                    .setTranslation((float) pose.get(i, 3), (float) y, (float) pose.get(i, 5))
                    .setScale(1, 1, 1);
        }
    }

    private static BoneSnapshot required(BoneSnapshots bones, String name) {
        return bones.get(name).orElseThrow(() -> new IllegalStateException("Missing plains villager bone: " + name));
    }

    private static @Nullable Player player(PlainsVillager entity, @Nullable UUID id) {
        return id == null ? null : entity.level().getPlayerByUUID(id);
    }

    private static boolean valid(PlainsVillager entity, Player player, double radius, float bodyYaw) {
        return player.isAlive() && !player.isSpectator() && !player.isInvisible()
                && entity.distanceToSqr(player) <= radius * radius
                && Math.abs(relativeYaw(player.position().subtract(entity.position()), bodyYaw)) < 100
                && entity.hasLineOfSight(player);
    }

    private static double relativeYaw(Vec3 direction, float bodyYaw) {
        return PlainsVillagerMotion.wrap(Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90 - bodyYaw);
    }
}
