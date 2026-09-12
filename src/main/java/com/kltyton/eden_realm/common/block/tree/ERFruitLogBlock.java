package com.kltyton.eden_realm.common.block.tree;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.block.plant.ERHangingFruitBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class ERFruitLogBlock extends RotatedPillarBlock {
    public static final MapCodec<ERFruitLogBlock> CODEC = simpleCodec(ERFruitLogBlock::new);

    public ERFruitLogBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<ERFruitLogBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AXIS).isHorizontal();
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(AXIS).isHorizontal() && random.nextFloat() < ERHangingFruitBlock.BUD_CHANCE) {
            ERHangingFruitBlock.growBelow(level, pos, ERConstants.id("twilight_pomegranate_hanging"));
        }
    }
}
