package com.kltyton.eden_realm.common.block.plant;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class ERHangingPlantBlock extends Block implements SimpleWaterloggedBlock {
    public static final MapCodec<ERHangingPlantBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.DOUBLE.fieldOf("shape_width").forGetter(block -> block.shapeWidth),
                    Codec.DOUBLE.fieldOf("shape_min_y").forGetter(block -> block.shapeMinY),
                    Codec.DOUBLE.fieldOf("shape_max_y").forGetter(block -> block.shapeMaxY),
                    Codec.DOUBLE.fieldOf("tip_shape_width").forGetter(block -> block.tipShapeWidth),
                    Codec.DOUBLE.fieldOf("tip_shape_min_y").forGetter(block -> block.tipShapeMinY),
                    Codec.DOUBLE.fieldOf("tip_shape_max_y").forGetter(block -> block.tipShapeMaxY),
                    propertiesCodec())
            .apply(instance, ERHangingPlantBlock::new));
    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty TIP = BooleanProperty.create("tip");

    private final double shapeWidth;
    private final double shapeMinY;
    private final double shapeMaxY;
    private final VoxelShape shape;
    private final double tipShapeWidth;
    private final double tipShapeMinY;
    private final double tipShapeMaxY;
    private final VoxelShape tipShape;

    public ERHangingPlantBlock(
            double shapeWidth,
            double shapeMinY,
            double shapeMaxY,
            BlockBehaviour.Properties properties) {
        this(shapeWidth, shapeMinY, shapeMaxY, shapeWidth, shapeMinY, shapeMaxY, properties);
    }

    public ERHangingPlantBlock(double shapeWidth, double shapeMinY, double shapeMaxY,
                               double tipShapeWidth, double tipShapeMinY, double tipShapeMaxY,
                               BlockBehaviour.Properties properties) {
        super(properties);
        this.shapeWidth = shapeWidth;
        this.shapeMinY = shapeMinY;
        this.shapeMaxY = shapeMaxY;
        this.shape = Block.column(shapeWidth, shapeMinY, shapeMaxY);
        this.tipShapeWidth = tipShapeWidth;
        this.tipShapeMinY = tipShapeMinY;
        this.tipShapeMaxY = tipShapeMaxY;
        this.tipShape = Block.column(tipShapeWidth, tipShapeMinY, tipShapeMaxY);
        registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, false).setValue(TIP, true));
    }

    @Override
    public MapCodec<ERHangingPlantBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED, TIP);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return state.setValue(WATERLOGGED, fluidState.is(Fluids.WATER))
                .setValue(TIP, !context.getLevel().getBlockState(context.getClickedPos().below()).is(this));
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos attachedToPos = pos.above();
        BlockState attached = level.getBlockState(attachedToPos);
        return attached.is(this) || attached.isFaceSturdy(level, attachedToPos, Direction.DOWN);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(TIP) ? tipShape : shape).move(state.getOffset(pos));
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
        if (directionToNeighbour == Direction.UP && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (directionToNeighbour == Direction.DOWN) {
            return state.setValue(TIP, !neighbourState.is(this));
        }
        return super.updateShape(
                state,
                level,
                ticks,
                pos,
                directionToNeighbour,
                neighbourPos,
                neighbourState,
                random);
    }
}
