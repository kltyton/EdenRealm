package com.kltyton.eden_realm.client.world;

import com.kltyton.eden_realm.ERConstants;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;

@EventBusSubscriber(modid = ERConstants.MOD_ID, value = Dist.CLIENT)
public final class SkyEnvironmentEvents {
    private static final Identifier SKY_LAYER = ERConstants.id("sky_layer");

    private SkyEnvironmentEvents() { }

    public static boolean isSkyLayer(ClientLevel level) {
        return level.dimension().identifier().equals(SKY_LAYER);
    }

    @SubscribeEvent
    public static void extractSky(ExtractLevelRenderStateEvent event) {
        if (isSkyLayer(event.getLevel()))
            event.getRenderState().skyRenderState.shouldRenderDarkDisc = false;
    }
}
