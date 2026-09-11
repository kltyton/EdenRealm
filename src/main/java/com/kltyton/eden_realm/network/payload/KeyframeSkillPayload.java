package com.kltyton.eden_realm.network.payload;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.skill.keyframe.KeyframeSkillRegistrar;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record KeyframeSkillPayload(
        int entityId,
        long clientSequence,
        long observedGameTime,
        String marker,
        String controllerName,
        String animationName,
        double markerTimeSeconds) implements CustomPacketPayload {

    public static final Type<KeyframeSkillPayload> TYPE =
            new Type<>(ERConstants.id("keyframe_skill"));
    public static final StreamCodec<RegistryFriendlyByteBuf, KeyframeSkillPayload> CODEC =
            StreamCodec.ofMember(KeyframeSkillPayload::write, KeyframeSkillPayload::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeLong(clientSequence);
        buffer.writeLong(observedGameTime);
        buffer.writeUtf(marker, KeyframeSkillRegistrar.MAX_FIELD_LENGTH);
        buffer.writeUtf(controllerName, KeyframeSkillRegistrar.MAX_FIELD_LENGTH);
        buffer.writeUtf(animationName, KeyframeSkillRegistrar.MAX_FIELD_LENGTH);
        buffer.writeDouble(markerTimeSeconds);
    }

    private static KeyframeSkillPayload read(RegistryFriendlyByteBuf buffer) {
        return new KeyframeSkillPayload(
                buffer.readVarInt(),
                buffer.readLong(),
                buffer.readLong(),
                buffer.readUtf(KeyframeSkillRegistrar.MAX_FIELD_LENGTH),
                buffer.readUtf(KeyframeSkillRegistrar.MAX_FIELD_LENGTH),
                buffer.readUtf(KeyframeSkillRegistrar.MAX_FIELD_LENGTH),
                buffer.readDouble());
    }
}
