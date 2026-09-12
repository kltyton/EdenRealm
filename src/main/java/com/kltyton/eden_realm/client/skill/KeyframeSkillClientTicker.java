package com.kltyton.eden_realm.client.skill;

import com.kltyton.eden_realm.EdenRealm;
import com.kltyton.eden_realm.common.skill.keyframe.KeyframeSkillEntity;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public final class KeyframeSkillClientTicker {
    private static final Set<EntityType<?>> WARNED_ENTITY_TYPES = new HashSet<>();

    private static ClientLevel lastLevel;
    private static long lastGameTime = Long.MIN_VALUE;

    private KeyframeSkillClientTicker() {
    }

    public static void tick(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level == null) {
            clear();
            return;
        }
        long gameTime = level.getGameTime();
        if (level == lastLevel && gameTime == lastGameTime) {
            return;
        }
        if (level != lastLevel) {
            WARNED_ENTITY_TYPES.clear();
            lastLevel = level;
        }
        lastGameTime = gameTime;
        if (minecraft.isPaused()) {
            return;
        }

        for (Entity entity : level.entitiesForRendering()) {
            if (entity.isRemoved() || !(entity instanceof KeyframeSkillEntity)) {
                continue;
            }
            try {
                minecraft.getEntityRenderDispatcher().extractEntity(entity, 0.0F);
            } catch (RuntimeException exception) {
                if (WARNED_ENTITY_TYPES.add(entity.getType())) {
                    EdenRealm.LOGGER.warn(
                            "Unable to tick off-screen GeckoLib keyframes for entity type {}",
                            entity.getType(), exception);
                }
            }
        }
    }

    private static void clear() {
        WARNED_ENTITY_TYPES.clear();
        lastLevel = null;
        lastGameTime = Long.MIN_VALUE;
    }
}
