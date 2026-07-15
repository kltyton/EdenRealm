package com.kltyton.eden_realm.common.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class ERSeagrassBlock extends VegetationBlock implements LiquidBlockContainer {
    public static final MapCodec<ERSeagrassBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.DOUBLE.fieldOf("shape_width").forGetter(block -> block.shapeWidth),
                    Codec.DOUBLE.fieldOf("shape_height").forGetter(block -> block.shapeHeight),
                    propertiesCodec())
            .apply(instance, ERSeagrassBlock::new));

    private final double shapeWidth;
    private final double shapeHeight;
    private final VoxelShape shape;

    public ERSeagrassBlock(double shapeWidth, double shapeHeight, BlockBehaviour.Properties properties) {
        super(properties);
        this.shapeWidth = shapeWidth;
        this.shapeHeight = shapeHeight;
        this.shape = Block.column(shapeWidth, 0.0, shapeHeight);
    }

    @Override
    public MapCodec<ERSeagrassBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.isFaceSturdy(level, pos, Direction.UP) && !state.is(BlockTags.CANNOT_SUPPORT_SEAGRASS);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return fluidState.is(FluidTags.WATER) && fluidState.isFull()
                ? super.getStateForPlacement(context)
                : null;
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction directionToNeighbour,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random) {
        BlockState result = super.updateShape(
                state,
                level,
                ticks,
                pos,
                directionToNeighbour,
                neighbourPos,
                neighbourState,
                random);
        if (!result.isAir()) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return result;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return Fluids.WATER.getSource(false);
    }

    @Override
    public boolean canPlaceLiquid(
            @Nullable LivingEntity user,
            BlockGetter level,
            BlockPos pos,
            BlockState state,
            Fluid type) {
        return false;
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        return false;
    }
}
