package com.kltyton.eden_realm.common.block.tree;

import com.kltyton.eden_realm.common.block.plant.ERHangingFruitBlock;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class ERFloweringLeavesBlock extends ERParticleLeavesBlock implements BonemealableBlock {
    public static final MapCodec<ERFloweringLeavesBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.fieldOf("fruit").forGetter(block -> block.fruit),
            Identifier.CODEC.fieldOf("leaf_particle").forGetter(block -> block.particle),
            propertiesCodec()).apply(instance, ERFloweringLeavesBlock::new));
    private final Identifier fruit;
    private final Identifier particle;

    public ERFloweringLeavesBlock(Identifier fruit, Identifier particle, BlockBehaviour.Properties properties) {
        super(0.01F, particle, properties);
        this.fruit = fruit;
        this.particle = particle;
    }

    @Override
    public MapCodec<ERFloweringLeavesBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        // TODO: Require a living tree using vanilla leaf distance once these trees have world generation.
        if (level.getBlockState(pos).is(this) && random.nextFloat() < ERHangingFruitBlock.BUD_CHANCE) {
            ERHangingFruitBlock.growBelow(level, pos, fruit);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return ERHangingFruitBlock.canGrowBelow(level, pos, BuiltInRegistries.BLOCK.getValue(fruit).defaultBlockState());
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return random.nextFloat() < ERHangingFruitBlock.BONEMEAL_CHANCE;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        ERHangingFruitBlock.growBelow(level, pos, fruit);
    }
}
