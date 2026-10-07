package com.kltyton.eden_realm.client;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.client.color.ERGrassColorReloadListener;
import com.kltyton.eden_realm.client.color.ERGrassColorSource;
import com.kltyton.eden_realm.client.color.ERGrassColors;
import com.kltyton.eden_realm.client.particle.ERFallingLeavesParticle;
import com.kltyton.eden_realm.client.renderer.entity.boss.MossStoneColossusRenderer;
import com.kltyton.eden_realm.client.renderer.entity.passive.villager.PlainsVillagerRenderer;
import com.kltyton.eden_realm.client.sound.villager.PlainsVillagerSpeechAudio;
import com.kltyton.eden_realm.common.entity.passive.villager.VillagerSpeechPlayback;
import com.kltyton.bonehitboxlib.client.compat.geckolib.skill.GeoKeyframeSkillClientBridge;
import com.kltyton.eden_realm.common.block.tree.ERWoodSet;
import com.kltyton.eden_realm.common.skill.keyframe.KeyframeSkillHooks;
import com.kltyton.eden_realm.registry.EREntityTypes;
import com.kltyton.eden_realm.registry.ERParticleTypes;
import com.kltyton.eden_realm.registry.content.block.ERTerrainBlocks;
import java.util.List;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = ERConstants.MOD_ID, value = Dist.CLIENT)
public final class ERClientEvents {
    static {
        KeyframeSkillHooks.installClientForwarder(GeoKeyframeSkillClientBridge::forward);
        VillagerSpeechPlayback.ClientFactory.install(PlainsVillagerSpeechAudio::play);
    }

    private ERClientEvents() {
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        for (ERWoodSet wood : ERWoodSet.values()) {
            event.registerSpriteSet(ERParticleTypes.fallingLeaves(wood).get(), ERFallingLeavesParticle.Provider::new);
        }
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
        event.registerEntityRenderer(EREntityTypes.PLAINS_VILLAGER.get(), PlainsVillagerRenderer::new);
        event.registerEntityRenderer(EREntityTypes.FALLING_FRUIT.get(), FallingBlockRenderer::new);
        event.registerEntityRenderer(
                EREntityTypes.MOSS_STONE_COLOSSUS.get(),
                MossStoneColossusRenderer::new);
        for (ERWoodSet wood : ERWoodSet.values()) {
            event.registerEntityRenderer(EREntityTypes.boat(wood).get(), context -> new BoatRenderer(context, boatLayer(wood)));
            event.registerEntityRenderer(EREntityTypes.chestBoat(wood).get(), context -> new BoatRenderer(context, chestBoatLayer(wood)));
        }
    }

    @SubscribeEvent
    public static void registerBlockTintSources(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(List.of(ERGrassColors.BLOCK_TINT), ERTerrainBlocks.EDEN_GRASS_BLOCK.get(),
                ERTerrainBlocks.GRASS_COVERED_RAW_ROCK.get(), ERTerrainBlocks.GRASS_COVERED_FLOATING_ISLAND_ROCK.get());
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
