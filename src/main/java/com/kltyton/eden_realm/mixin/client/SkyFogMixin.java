package com.kltyton.eden_realm.mixin.client;

import com.kltyton.eden_realm.client.world.SkyEnvironmentEvents;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Keeps the sky layer's cloud foundation out of vanilla's void fade, before status-effect fog is applied. */
@Mixin(FogRenderer.class)
public abstract class SkyFogMixin {
    @ModifyExpressionValue(method = "computeFogColor", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/Mth;clamp(FFF)F", ordinal = 0))
    private float edenRealm$skyVoidDarkness(float darkness, Camera camera, float partialTicks, ClientLevel level) {
        return SkyEnvironmentEvents.isSkyLayer(level) ? 0.0F : darkness;
    }
}
