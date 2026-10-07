package com.kltyton.eden_realm.mixin.client;

import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import org.mesdag.particlestorm.PSDebugEntries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Starts ParticleStorm's counter disabled in the default profile; users can still enable it. */
@Mixin(value = PSDebugEntries.class, remap = false)
public abstract class ParticleStormDebugDefaultsMixin {
    @ModifyArg(method = "register", at = @At(value = "INVOKE", target =
            "Lnet/neoforged/neoforge/client/event/RegisterDebugEntriesEvent;includeInProfile(" +
                    "Lnet/minecraft/resources/Identifier;Lnet/minecraft/client/gui/components/debug/DebugScreenProfile;" +
                    "Lnet/minecraft/client/gui/components/debug/DebugScreenEntryStatus;)V"), index = 2, remap = false)
    private static DebugScreenEntryStatus edenRealm$disableParticleCounter(DebugScreenEntryStatus status) {
        return DebugScreenEntryStatus.NEVER;
    }
}
