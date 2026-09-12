package com.kltyton.eden_realm.common.skill.keyframe;

import java.util.function.Consumer;

public record KeyframeSkillRegistration(
        String skillId,
        String controllerName,
        String animationName,
        String marker,
        double markerTimeSeconds,
        Consumer<KeyframeSkillContext> handler) {

    public int minimumDelayTicks() {
        return Math.max(0, (int) Math.floor(markerTimeSeconds * 20.0) - 4);
    }
}
