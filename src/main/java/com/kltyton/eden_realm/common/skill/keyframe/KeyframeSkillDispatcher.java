package com.kltyton.eden_realm.common.skill.keyframe;

import com.kltyton.eden_realm.EdenRealm;
import com.kltyton.eden_realm.network.payload.KeyframeSkillPayload;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class KeyframeSkillDispatcher {
    private static final long CROSS_CLIENT_TIME_TOLERANCE = 4L;
    private static final long SEEN_MARKER_RETENTION = 80L;

    private static final Map<UUID, Long> LAST_SEQUENCE_BY_REPORTER = new HashMap<>();
    private static final Map<MarkerKey, SeenMarker> SEEN_MARKERS = new LinkedHashMap<>();

    private KeyframeSkillDispatcher() {
    }

    public static void handle(KeyframeSkillPayload payload, ServerPlayer reporter) {
        if (!(reporter.level() instanceof ServerLevel level)) {
            return;
        }
        Entity rawEntity = level.getEntity(payload.entityId());
        if (!(rawEntity instanceof KeyframeSkillEntity skillEntity)
                || !isTrackedBy(level, reporter, rawEntity)) {
            return;
        }

        long previousSequence = LAST_SEQUENCE_BY_REPORTER.getOrDefault(reporter.getUUID(), Long.MIN_VALUE);
        if (payload.clientSequence() <= previousSequence) {
            return;
        }

        String marker;
        try {
            marker = KeyframeSkillRegistrar.normalizeMarker(payload.marker());
        } catch (IllegalArgumentException ignored) {
            return;
        }
        KeyframeSkillRegistration registration = findRegistration(
                skillEntity, payload.controllerName(), payload.animationName(), marker);
        if (registration == null || !Double.isFinite(payload.markerTimeSeconds())
                || Math.abs(payload.markerTimeSeconds() - registration.markerTimeSeconds()) > 0.001) {
            return;
        }

        long serverTick = level.getServer().getTickCount();
        KeyframeSkillContext context = new KeyframeSkillContext(
                reporter,
                rawEntity,
                registration,
                level.getGameTime(),
                payload.observedGameTime(),
                payload.clientSequence());
        if (!skillEntity.canAcceptKeyframeSkill(registration, context)) {
            return;
        }

        LAST_SEQUENCE_BY_REPORTER.put(reporter.getUUID(), payload.clientSequence());
        prune(level, serverTick);
        MarkerKey key = new MarkerKey(
                level.dimension().identifier().toString(),
                rawEntity.getUUID(),
                registration.controllerName(),
                registration.animationName(),
                registration.marker());
        SeenMarker previous = SEEN_MARKERS.get(key);
        if (previous != null
                && Math.abs(payload.observedGameTime() - previous.observedGameTime()) <= CROSS_CLIENT_TIME_TOLERANCE
                && serverTick - previous.serverTick() <= SEEN_MARKER_RETENTION) {
            return;
        }
        SEEN_MARKERS.put(key, new SeenMarker(payload.observedGameTime(), serverTick));

        try {
            registration.handler().accept(context);
        } catch (RuntimeException exception) {
            EdenRealm.LOGGER.error(
                    "Keyframe skill callback failed: entity={}, skill={}, marker={}",
                    rawEntity.getScoreboardName(), registration.skillId(), registration.marker(), exception);
        }
    }

    private static KeyframeSkillRegistration findRegistration(
            KeyframeSkillEntity entity,
            String controllerName,
            String animationName,
            String marker) {
        KeyframeSkillRegistrar registrar = new KeyframeSkillRegistrar();
        entity.registerKeyframeSkills(registrar);
        return registrar.registrations().stream()
                .filter(registration -> registration.controllerName().equals(controllerName)
                        && registration.animationName().equals(animationName)
                        && registration.marker().equals(marker))
                .findFirst()
                .orElse(null);
    }

    private static boolean isTrackedBy(ServerLevel level, ServerPlayer reporter, Entity target) {
        boolean[] tracked = {false};
        level.getChunkSource().chunkMap.forEachEntityTrackedBy(reporter, entity -> {
            if (entity == target) {
                tracked[0] = true;
            }
        });
        return tracked[0];
    }

    private static void prune(ServerLevel level, long serverTick) {
        SEEN_MARKERS.entrySet().removeIf(entry ->
                serverTick - entry.getValue().serverTick() > SEEN_MARKER_RETENTION);
        Set<UUID> onlinePlayers = level.getServer().getPlayerList().getPlayers().stream()
                .map(ServerPlayer::getUUID)
                .collect(Collectors.toSet());
        LAST_SEQUENCE_BY_REPORTER.keySet().retainAll(onlinePlayers);
    }

    private record MarkerKey(
            String levelId,
            UUID entityId,
            String controller,
            String animation,
            String marker) {
    }

    private record SeenMarker(long observedGameTime, long serverTick) {
    }
}
