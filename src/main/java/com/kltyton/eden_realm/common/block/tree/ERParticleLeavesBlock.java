package com.kltyton.eden_realm.common.block.tree;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ERParticleLeavesBlock extends LeavesBlock {
    public static final MapCodec<ERParticleLeavesBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    ExtraCodecs.floatRange(0.0F, 1.0F)
                            .fieldOf("leaf_particle_chance")
                            .forGetter(block -> block.leafParticleChance),
                    Identifier.CODEC.fieldOf("leaf_particle").forGetter(block -> block.leafParticle),
                    propertiesCodec())
            .apply(instance, ERParticleLeavesBlock::new));

    private final Identifier leafParticle;

    public ERParticleLeavesBlock(float leafParticleChance, Identifier leafParticle, BlockBehaviour.Properties properties) {
        super(leafParticleChance, properties);
        this.leafParticle = leafParticle;
    }

    @Override
    protected void spawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random) {
        ParticleType<?> particleType = BuiltInRegistries.PARTICLE_TYPE.getValue(leafParticle);
        if (!(particleType instanceof SimpleParticleType particle)) {
            throw new IllegalStateException("Missing simple leaf particle type: " + leafParticle);
        }
        ParticleUtils.spawnParticleBelow(level, pos, random, particle);
    }

    @Override
    public MapCodec<? extends ERParticleLeavesBlock> codec() {
        return CODEC;
    }
}
