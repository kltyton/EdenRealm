package com.kltyton.eden_realm.common.block.plant;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.kltyton.eden_realm.registry.content.ERTerrainBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
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
                    Codec.BOOL.optionalFieldOf("allows_dry_land", false).forGetter(block -> block.allowsDryLand),
                    Codec.DOUBLE.fieldOf("lower_shape_width").forGetter(ERTallWaterPlantBlock::lowerShapeWidth),
                    Codec.DOUBLE.fieldOf("lower_shape_height").forGetter(ERTallWaterPlantBlock::lowerShapeHeight),
                    Codec.DOUBLE.fieldOf("upper_shape_width").forGetter(ERTallWaterPlantBlock::upperShapeWidth),
                    Codec.DOUBLE.fieldOf("upper_shape_height").forGetter(ERTallWaterPlantBlock::upperShapeHeight),
                    propertiesCodec())
            .apply(instance, ERTallWaterPlantBlock::new));
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private final boolean requiresFullWater;
    private final boolean allowsDryLand;

    public ERTallWaterPlantBlock(
            boolean requiresFullWater,
            ERPlantShapes.DoublePlantShape shape,
            BlockBehaviour.Properties properties) {
        this(
                requiresFullWater,
                false,
                shape.lowerWidth(),
                shape.lowerHeight(),
                shape.upperWidth(),
                shape.upperHeight(),
                properties);
    }

    public ERTallWaterPlantBlock(
            boolean requiresFullWater,
            boolean allowsDryLand,
            ERPlantShapes.DoublePlantShape shape,
            BlockBehaviour.Properties properties) {
        this(requiresFullWater, allowsDryLand, shape.lowerWidth(), shape.lowerHeight(),
                shape.upperWidth(), shape.upperHeight(), properties);
    }

    public ERTallWaterPlantBlock(
            boolean requiresFullWater,
            double lowerShapeWidth,
            double lowerShapeHeight,
            double upperShapeWidth,
            double upperShapeHeight,
            BlockBehaviour.Properties properties) {
        this(requiresFullWater, false, lowerShapeWidth, lowerShapeHeight,
                upperShapeWidth, upperShapeHeight, properties);
    }

    public ERTallWaterPlantBlock(
            boolean requiresFullWater,
            boolean allowsDryLand,
            double lowerShapeWidth,
            double lowerShapeHeight,
            double upperShapeWidth,
            double upperShapeHeight,
            BlockBehaviour.Properties properties) {
        super(lowerShapeWidth, lowerShapeHeight, upperShapeWidth, upperShapeHeight, properties);
        this.requiresFullWater = requiresFullWater;
        this.allowsDryLand = allowsDryLand;
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
        if (allowsDryLand && !requiresFullWater && lowerFluid.isEmpty() && upperEmpty
                && supportsDryPlant(context.getLevel().getBlockState(lowerPos.below()))) {
            return state.setValue(WATERLOGGED, false);
        }
        if (requiresFullWater ? !(lowerWater && upperWater) : !(lowerWater && upperEmpty)) {
            return null;
        }
        return state.setValue(WATERLOGGED, true);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getBlock() != this) {
            return super.canSurvive(state, level, pos);
        }
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockState below = level.getBlockState(pos.below());
            boolean paired = below.is(this) && below.getValue(HALF) == DoubleBlockHalf.LOWER;
            return paired && (!requiresFullWater || state.getValue(WATERLOGGED));
        }
        return super.canSurvive(state, level, pos) && (state.getValue(WATERLOGGED)
                || allowsDryLand && !requiresFullWater && supportsDryPlant(level.getBlockState(pos.below())));
    }

    static boolean supportsDryPlant(BlockState soil) {
        return soil.is(Blocks.GRASS_BLOCK) || soil.is(Blocks.DIRT) || soil.is(Blocks.MUD)
                || soil.is(ERTerrainBlocks.EDEN_GRASS_BLOCK.get()) || soil.is(ERTerrainBlocks.EDEN_DIRT.get());
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
    public ItemStack pickupBlock(@Nullable LivingEntity user, LevelAccessor level, BlockPos pos, BlockState state) {
        ItemStack bucket = SimpleWaterloggedBlock.super.pickupBlock(user, level, pos, state);
        BlockState dry = level.getBlockState(pos);
        if (dry.is(this) && !dry.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
        return bucket;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED);
    }
}
