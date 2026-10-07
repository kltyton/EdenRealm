package com.kltyton.eden_realm.common.block.crop;

import com.kltyton.eden_realm.common.block.shape.ERHarvestShapes;

import com.kltyton.eden_realm.registry.ERItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ERDewspikeGrainBlock extends ERDoubleCropBlock {
    public static final MapCodec<ERDewspikeGrainBlock> CODEC = simpleCodec(ERDewspikeGrainBlock::new);

    public ERDewspikeGrainBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<ERDewspikeGrainBlock> codec() {
        return CODEC;
    }

    public static int matureVariant(BlockState state, BlockPos pos) {
        return RandomSource.create(state.getSeed(pos)).nextInt(2);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int age = state.getValue(AGE);
        return ERHarvestShapes.grain(age == 7 ? 7 + matureVariant(state, pos) : age, state.getValue(HALF));
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData, Player player) {
        return new ItemStack(ERItems.DEWSPIKE_GRAIN_SEEDS.get());
    }

}
