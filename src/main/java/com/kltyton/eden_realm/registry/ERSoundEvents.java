package com.kltyton.eden_realm.registry;

import com.kltyton.eden_realm.ERConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ERSoundEvents {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, ERConstants.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> MOSS_STONE_COLOSSUS_STEP =
            register("entity.moss_stone_colossus.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOSS_STONE_COLOSSUS_AMBIENT =
            register("entity.moss_stone_colossus.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOSS_STONE_COLOSSUS_HURT =
            register("entity.moss_stone_colossus.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOSS_STONE_COLOSSUS_DEATH =
            register("entity.moss_stone_colossus.death");

    private ERSoundEvents() {
    }

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(ERConstants.id(name)));
    }
}
