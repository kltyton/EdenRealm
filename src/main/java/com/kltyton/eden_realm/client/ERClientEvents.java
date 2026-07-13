package com.kltyton.eden_realm.client;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.client.color.ERGrassColorReloadListener;
import com.kltyton.eden_realm.client.color.ERGrassColorSource;
import com.kltyton.eden_realm.client.color.ERGrassColors;
import com.kltyton.eden_realm.common.block.ERWoodSet;
import com.kltyton.eden_realm.registry.EREntityTypes;
import com.kltyton.eden_realm.registry.content.ERTerrainBlocks;
import java.util.List;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = ERConstants.MOD_ID, value = Dist.CLIENT)
public final class ERClientEvents {
    private ERClientEvents() {
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (ERWoodSet wood : ERWoodSet.values()) {
            event.registerLayerDefinition(boatLayer(wood), BoatModel::createBoatModel);
            event.registerLayerDefinition(chestBoatLayer(wood), BoatModel::createChestBoatModel);
        }
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (ERWoodSet wood : ERWoodSet.values()) {
            event.registerEntityRenderer(EREntityTypes.boat(wood).get(), context -> new BoatRenderer(context, boatLayer(wood)));
            event.registerEntityRenderer(EREntityTypes.chestBoat(wood).get(), context -> new BoatRenderer(context, chestBoatLayer(wood)));
        }
    }

    @SubscribeEvent
    public static void registerBlockTintSources(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(List.of(ERGrassColors.BLOCK_TINT), ERTerrainBlocks.EDEN_GRASS_BLOCK.get());
    }

    @SubscribeEvent
    public static void registerColorResolvers(RegisterColorHandlersEvent.ColorResolvers event) {
        event.register(ERGrassColors.RESOLVER);
    }

    @SubscribeEvent
    public static void registerItemTintSources(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(ERConstants.id("grass"), ERGrassColorSource.MAP_CODEC);
    }

    @SubscribeEvent
    public static void addReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(ERConstants.id("grass_colormap"), new ERGrassColorReloadListener());
    }

    private static ModelLayerLocation boatLayer(ERWoodSet wood) {
        return new ModelLayerLocation(ERConstants.id("boat/" + wood.id()), "main");
    }

    private static ModelLayerLocation chestBoatLayer(ERWoodSet wood) {
        return new ModelLayerLocation(ERConstants.id("chest_boat/" + wood.id()), "main");
    }
}
