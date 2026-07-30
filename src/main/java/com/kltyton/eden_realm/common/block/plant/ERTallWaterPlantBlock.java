package com.kltyton.eden_realm.common.block.plant;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public final class ERTallWaterPlantBlock extends ERShapedDoublePlantBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<ERTallWaterPlantBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.BOOL.fieldOf("requires_full_water").forGetter(block -> block.requiresFullWater),
                    Codec.DOUBLE.fieldOf("lower_shape_width").forGetter(ERTallWaterPlantBlock::lowerShapeWidth),
                    Codec.DOUBLE.fieldOf("lower_shape_height").forGetter(ERTallWaterPlantBlock::lowerShapeHeight),
                    Codec.DOUBLE.fieldOf("upper_shape_width").forGetter(ERTallWaterPlantBlock::upperShapeWidth),
                    Codec.DOUBLE.fieldOf("upper_shape_height").forGetter(ERTallWaterPlantBlock::upperShapeHeight),
                    propertiesCodec())
            .apply(instance, ERTallWaterPlantBlock::new));
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private final boolean requiresFullWater;

    public ERTallWaterPlantBlock(
            boolean requiresFullWater,
            ERPlantShapes.DoublePlantShape shape,
            BlockBehaviour.Properties properties) {
        this(
                requiresFullWater,
                shape.lowerWidth(),
                shape.lowerHeight(),
                shape.upperWidth(),
                shape.upperHeight(),
                properties);
    }

    public ERTallWaterPlantBlock(
            boolean requiresFullWater,
            double lowerShapeWidth,
            double lowerShapeHeight,
            double upperShapeWidth,
            double upperShapeHeight,
            BlockBehaviour.Properties properties) {
        super(lowerShapeWidth, lowerShapeHeight, upperShapeWidth, upperShapeHeight, properties);
        this.requiresFullWater = requiresFullWater;
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    public MapCodec<ERTallWaterPlantBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.isFaceSturdy(level, pos, Direction.UP) && !state.is(BlockTags.CANNOT_SUPPORT_SEAGRASS);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }

        BlockPos lowerPos = context.getClickedPos();
        FluidState lowerFluid = context.getLevel().getFluidState(lowerPos);
        FluidState upperFluid = context.getLevel().getFluidState(lowerPos.above());
        boolean lowerWater = lowerFluid.is(FluidTags.WATER) && lowerFluid.isFull();
        boolean upperWater = upperFluid.is(FluidTags.WATER) && upperFluid.isFull();
        boolean upperEmpty = upperFluid.isEmpty();
        if (requiresFullWater ? !(lowerWater && upperWater) : !(lowerWater && upperEmpty)) {
            return null;
        }
        return state.setValue(WATERLOGGED, true);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockState below = level.getBlockState(pos.below());
            boolean paired = below.is(this) && below.getValue(HALF) == DoubleBlockHalf.LOWER;
            return paired && (!requiresFullWater || state.getValue(WATERLOGGED));
        }
        return super.canSurvive(state, level, pos) && state.getValue(WATERLOGGED);
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
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        BlockState result = super.updateShape(
                state,
                level,
                ticks,
                pos,
                directionToNeighbour,
                neighbourPos,
                neighbourState,
                random);
        if (!result.isAir() && requiresFullWater && !result.getValue(WATERLOGGED)) {
            return Blocks.AIR.defaultBlockState();
        }
        return result;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED);
    }
}
