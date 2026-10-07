package com.kltyton.eden_realm.common.block.crop;

import com.kltyton.eden_realm.registry.ERItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ERYamCropBlock extends CropBlock {
    public static final MapCodec<ERYamCropBlock> CODEC = simpleCodec(ERYamCropBlock::new);
    private static final VoxelShape[] SHAPES = {
            Block.column(9.54, 0, 4), Block.column(9.54, 0, 7),
            Block.column(10.25, 0, 11), Block.column(12, 0, 14)};

    public ERYamCropBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<ERYamCropBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ERItems.STAR_PATTERN_YAM.get();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[getAge(state) * 3 / 7];
    }
}
