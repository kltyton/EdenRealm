package com.kltyton.eden_realm.common.skill.keyframe;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public record KeyframeSkillContext(
        ServerPlayer reporter,
        Entity entity,
        KeyframeSkillRegistration registration,
        long serverGameTime,
        long observedGameTime,
        long clientSequence) {
}
