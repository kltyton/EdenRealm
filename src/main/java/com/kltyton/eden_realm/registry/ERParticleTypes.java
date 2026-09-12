package com.kltyton.eden_realm.registry;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.block.ERWoodSet;
import java.util.EnumMap;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ERParticleTypes {
    private static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, ERConstants.MOD_ID);
    private static final EnumMap<ERWoodSet, DeferredHolder<ParticleType<?>, SimpleParticleType>> FALLING_LEAVES =
            new EnumMap<>(ERWoodSet.class);

    static {
        for (ERWoodSet wood : ERWoodSet.values()) {
            FALLING_LEAVES.put(wood, PARTICLE_TYPES.register(
                    wood.leavesName(),
                    () -> new SimpleParticleType(false)));
        }
    }

    private ERParticleTypes() {
    }

    public static void register(IEventBus modEventBus) {
        PARTICLE_TYPES.register(modEventBus);
    }

    public static DeferredHolder<ParticleType<?>, SimpleParticleType> fallingLeaves(ERWoodSet wood) {
        return FALLING_LEAVES.get(wood);
    }
}