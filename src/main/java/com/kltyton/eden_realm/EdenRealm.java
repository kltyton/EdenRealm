package com.kltyton.eden_realm;

import com.kltyton.eden_realm.data.ERDataGenerators;
import com.kltyton.eden_realm.common.event.ERBlockToolEvents;
import com.kltyton.eden_realm.registry.ERBlockEntities;
import com.kltyton.eden_realm.registry.ERBlocks;
import com.kltyton.eden_realm.registry.ERCreativeTabs;
import com.kltyton.eden_realm.registry.ERDataComponents;
import com.kltyton.eden_realm.registry.EREntityTypes;
import com.kltyton.eden_realm.registry.ERItems;
import com.kltyton.eden_realm.registry.ERMenuTypes;
import com.kltyton.eden_realm.registry.ERMobEffects;
import com.kltyton.eden_realm.registry.ERParticleTypes;
import com.kltyton.eden_realm.registry.ERSoundEvents;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(ERConstants.MOD_ID)
public final class EdenRealm {
    public static final Logger LOGGER = LogUtils.getLogger();

    public EdenRealm(IEventBus modEventBus, ModContainer modContainer) {
        ERBlocks.register(modEventBus);
        ERItems.register(modEventBus);
        ERCreativeTabs.register(modEventBus);
        EREntityTypes.register(modEventBus);
        ERBlockEntities.register(modEventBus);
        ERMobEffects.register(modEventBus);
        ERSoundEvents.register(modEventBus);
        ERParticleTypes.register(modEventBus);
        ERMenuTypes.register(modEventBus);
        ERDataComponents.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(ERBlockToolEvents::onBlockToolModification);

        modEventBus.addListener(ERDataGenerators::gatherClientData);
        modEventBus.addListener(ERDataGenerators::gatherServerData);
    }
}
