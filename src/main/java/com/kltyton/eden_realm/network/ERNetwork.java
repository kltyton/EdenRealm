package com.kltyton.eden_realm.network;

import com.kltyton.eden_realm.common.skill.keyframe.KeyframeSkillDispatcher;
import com.kltyton.eden_realm.network.payload.KeyframeSkillPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ERNetwork {
    private static final String NETWORK_VERSION = "1";

    private ERNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION).playToServer(
                KeyframeSkillPayload.TYPE,
                KeyframeSkillPayload.CODEC,
                (payload, context) -> KeyframeSkillDispatcher.handle(
                        payload, (ServerPlayer) context.player()));
    }
}
