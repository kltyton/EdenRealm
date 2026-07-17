package com.kltyton.eden_realm.common.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class ERTallSeagrassBlock extends DoublePlantBlock implements LiquidBlockContainer {
    public static final MapCodec<ERTallSeagrassBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.DOUBLE.fieldOf("shape_width").forGetter(block -> block.shapeWidth),
                    Codec.DOUBLE.fieldOf("shape_height").forGetter(block -> block.shapeHeight),
                    BlockState.CODEC.fieldOf("short_seagrass").forGetter(block -> block.shortSeagrass.get()),
                    propertiesCodec())
            .apply(instance, ERTallSeagrassBlock::decode));
    public static final EnumProperty<DoubleBlockHalf> HALF = DoublePlantBlock.HALF;

    private final double shapeWidth;
    private final double shapeHeight;
    private final Supplier<BlockState> shortSeagrass;
    private final VoxelShape shape;

    public ERTallSeagrassBlock(
            double shapeWidth,
            double shapeHeight,
            Supplier<BlockState> shortSeagrass,
            BlockBehaviour.Properties properties) {
        super(properties);
        this.shapeWidth = shapeWidth;
        this.shapeHeight = shapeHeight;
        this.shortSeagrass = shortSeagrass;
        this.shape = Block.column(shapeWidth, 0.0, shapeHeight);
    }

    private static ERTallSeagrassBlock decode(
            double shapeWidth,
            double shapeHeight,
            BlockState shortSeagrass,
            BlockBehaviour.Properties properties) {
        return new ERTallSeagrassBlock(shapeWidth, shapeHeight, () -> shortSeagrass, properties);
    }

    @Override
    public MapCodec<ERTallSeagrassBlock> codec() {
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
    public ItemStack getCloneItemStack(
            LevelReader level,
            BlockPos pos,
            BlockState state,
            boolean includeData,
            Player player) {
        return new ItemStack(shortSeagrass.get().getBlock());
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state != null) {
            FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos().above());
            if (fluidState.is(FluidTags.WATER) && fluidState.isFull()) {
                return state;
            }
        }
        return null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockState belowState = level.getBlockState(pos.below());
            return belowState.is(this) && belowState.getValue(HALF) == DoubleBlockHalf.LOWER;
        }

        FluidState fluidState = level.getFluidState(pos);
        return super.canSurvive(state, level, pos) && fluidState.is(FluidTags.WATER) && fluidState.isFull();
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
