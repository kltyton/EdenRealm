package com.kltyton.eden_realm.common.skill.keyframe;

public interface KeyframeSkillEntity {
    void registerKeyframeSkills(KeyframeSkillRegistrar registrar);

    String activeKeyframeSkillId();

    long keyframeSkillStartGameTime();

    default boolean canAcceptKeyframeSkill(
            KeyframeSkillRegistration registration,
            KeyframeSkillContext context) {
        long elapsedTicks = context.serverGameTime() - keyframeSkillStartGameTime();
        return registration.skillId().equals(activeKeyframeSkillId())
                && elapsedTicks >= registration.minimumDelayTicks();
    }
}
