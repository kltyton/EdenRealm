package com.kltyton.eden_realm.common.skill.keyframe;

import com.geckolib.animation.state.KeyFrameEvent;
import com.geckolib.cache.animation.keyframeevent.CustomInstructionKeyframeData;
import java.util.Objects;

public final class KeyframeSkillHooks {
    private static Forwarder forwarder = event -> {
    };

    private KeyframeSkillHooks() {
    }

    public static void installClientForwarder(Forwarder clientForwarder) {
        forwarder = Objects.requireNonNull(clientForwarder, "clientForwarder");
    }

    public static void forward(KeyFrameEvent<?, CustomInstructionKeyframeData> event) {
        forwarder.forward(event);
    }

    @FunctionalInterface
    public interface Forwarder {
        void forward(KeyFrameEvent<?, CustomInstructionKeyframeData> event);
    }
}
