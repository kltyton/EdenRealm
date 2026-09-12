package com.kltyton.eden_realm.client.skill;

import com.geckolib.animation.state.KeyFrameEvent;
import com.geckolib.cache.animation.keyframeevent.CustomInstructionKeyframeData;
import com.kltyton.eden_realm.common.skill.keyframe.KeyframeSkillEntity;
import com.kltyton.eden_realm.network.payload.KeyframeSkillPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class KeyframeSkillClientBridge {
    private static long sequence;

    private KeyframeSkillClientBridge() {
    }

    public static void forward(KeyFrameEvent<?, CustomInstructionKeyframeData> event) {
        if (!(event.animatable() instanceof Entity entity)
                || !(entity instanceof KeyframeSkillEntity)
                || !entity.level().isClientSide()
                || Minecraft.getInstance().getConnection() == null) {
            return;
        }
        var animationPoint = event.controller().getCurrentAnimationPoint();
        if (animationPoint == null) {
            return;
        }
        ClientPacketDistributor.sendToServer(new KeyframeSkillPayload(
                entity.getId(),
                ++sequence,
                entity.level().getGameTime(),
                event.keyframeData().getInstructions(),
                event.controller().getName(),
                animationPoint.animation().name(),
                event.keyframeData().getTime()));
    }
}
