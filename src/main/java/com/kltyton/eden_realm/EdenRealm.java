package com.kltyton.eden_realm;

import com.kltyton.eden_realm.data.ERDataGenerators;
import com.kltyton.eden_realm.world.dimension.SkyLayerTravel;
import com.kltyton.eden_realm.common.event.block.ERBlockToolEvents;
import com.kltyton.eden_realm.registry.ERBlockEntities;
import com.kltyton.eden_realm.registry.ERBlocks;
import com.kltyton.eden_realm.registry.ERCreativeTabs;
import com.kltyton.eden_realm.registry.ERDataComponents;
import com.kltyton.eden_realm.registry.ERDensityFunctionTypes;
import com.kltyton.eden_realm.registry.EREntityTypes;
import com.kltyton.eden_realm.registry.ERFeatures;
import com.kltyton.eden_realm.registry.ERItems;
import com.kltyton.eden_realm.registry.ERMenuTypes;
import com.kltyton.eden_realm.registry.ERMobEffects;
import com.kltyton.eden_realm.registry.ERParticleTypes;
import com.kltyton.eden_realm.registry.ERSoundEvents;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import com.kltyton.eden_realm.config.ERClientConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(ERConstants.MOD_ID)
public final class EdenRealm {
    public static final Logger LOGGER = LogUtils.getLogger();

    public EdenRealm(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, ERClientConfig.SPEC, "eden_realm-client.toml");
        ERBlocks.register(modEventBus);
        ERItems.register(modEventBus);
        ERCreativeTabs.register(modEventBus);
        EREntityTypes.register(modEventBus);
        ERBlockEntities.register(modEventBus);
        ERMobEffects.register(modEventBus);
        ERSoundEvents.register(modEventBus);
        ERParticleTypes.register(modEventBus);
        ERFeatures.register(modEventBus);
        ERDensityFunctionTypes.register(modEventBus);
        ERMenuTypes.register(modEventBus);
        ERDataComponents.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(ERBlockToolEvents::onBlockToolModification);
        NeoForge.EVENT_BUS.addListener(SkyLayerTravel::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(com.kltyton.eden_realm.world.terrain.TerrainWorldProfiles::onLoad);
        NeoForge.EVENT_BUS.addListener(com.kltyton.eden_realm.world.terrain.TerrainWorldProfiles::onUnload);
        NeoForge.EVENT_BUS.addListener(com.kltyton.eden_realm.common.event.fruit.ERFruitDisturbance::onEntityPlace);
        NeoForge.EVENT_BUS.addListener(com.kltyton.eden_realm.common.event.fruit.ERFruitDisturbance::onGameEvent);
        NeoForge.EVENT_BUS.addListener(com.kltyton.eden_realm.common.event.fruit.ERFruitDisturbance::onPistonPost);

        modEventBus.addListener(EREntityTypes::registerAttributes);
        modEventBus.addListener(ERDataGenerators::gatherClientData);
        modEventBus.addListener(ERDataGenerators::gatherServerData);
    }
}
