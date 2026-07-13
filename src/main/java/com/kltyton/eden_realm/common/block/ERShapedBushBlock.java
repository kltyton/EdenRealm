package com.kltyton.eden_realm.common.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ERShapedBushBlock extends VegetationBlock implements BonemealableBlock {
    public static final MapCodec<ERShapedBushBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.DOUBLE.fieldOf("shape_width").forGetter(block -> block.shapeWidth),
                    Codec.DOUBLE.fieldOf("shape_height").forGetter(block -> block.shapeHeight),
                    propertiesCodec())
            .apply(instance, ERShapedBushBlock::new));

    private final double shapeWidth;
    private final double shapeHeight;
    private final VoxelShape shape;

    public ERShapedBushBlock(double shapeWidth, double shapeHeight, BlockBehaviour.Properties properties) {
        super(properties);
        this.shapeWidth = shapeWidth;
        this.shapeHeight = shapeHeight;
        this.shape = Block.column(shapeWidth, 0.0, shapeHeight);
    }

    @Override
    public MapCodec<ERShapedBushBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape.move(state.getOffset(pos));
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return BonemealableBlock.hasSpreadableNeighbourPos(level, pos, state);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BonemealableBlock.findSpreadableNeighbourPos(level, pos, state)
                .ifPresent(blockPos -> level.setBlockAndUpdate(blockPos, defaultBlockState()));
    }
}
