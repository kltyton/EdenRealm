package com.kltyton.eden_realm.common.block.plant;

import com.kltyton.eden_realm.registry.ERItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.CommonHooks;
import org.jspecify.annotations.Nullable;

public final class ERDewspikeGrainBlock extends DoublePlantBlock implements BonemealableBlock {
    public static final MapCodec<ERDewspikeGrainBlock> CODEC = simpleCodec(ERDewspikeGrainBlock::new);
    public static final IntegerProperty AGE = BlockStateProperties.AGE_7;

    public ERDewspikeGrainBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(AGE, 0));
    }

    @Override
    public MapCodec<ERDewspikeGrainBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState soil, BlockGetter level, BlockPos pos) {
        return soil.is(BlockTags.SUPPORTS_CROPS);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        return context.getLevel().getFluidState(pos).isEmpty()
                && context.getLevel().getFluidState(pos.above()).isEmpty() ? super.getStateForPlacement(context) : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? super.canSurvive(state, level, pos)
                : CropBlock.hasSufficientLight(level, pos) && super.canSurvive(state, level, pos);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER && state.getValue(AGE) < 7;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!isRandomlyTicking(state) || !level.isAreaLoaded(pos, 1) || level.getRawBrightness(pos, 0) < 9
                || !paired(level, pos)) {
            return;
        }
        float speed = CropBlock.getGrowthSpeed(state, level, pos);
        if (CommonHooks.canCropGrow(level, pos, state, random.nextInt((int) (25.0F / speed) + 1) == 0)) {
            level.setBlockAndUpdate(pos, state.setValue(AGE, state.getValue(AGE) + 1));
            CommonHooks.fireCropGrowPost(level, pos, state);
        }
    }

    private boolean paired(LevelReader level, BlockPos lower) {
        BlockState bottom = level.getBlockState(lower);
        BlockState top = level.getBlockState(lower.above());
        return bottom.is(this) && bottom.getValue(HALF) == DoubleBlockHalf.LOWER
                && top.is(this) && top.getValue(HALF) == DoubleBlockHalf.UPPER;
    }

    private static BlockPos lowerPos(BlockState state, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction direction, BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        BlockState result = super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbour, random);
        return result.is(this) && state.getValue(HALF) == DoubleBlockHalf.UPPER
                && direction == Direction.DOWN && neighbour.is(this)
                ? result.setValue(AGE, neighbour.getValue(AGE)) : result;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        BlockPos lower = lowerPos(state, pos);
        return paired(level, lower) && level.getBlockState(lower).getValue(AGE) < 7;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (isValidBonemealTarget(level, pos, state)) {
            BlockPos lower = lowerPos(state, pos);
            BlockState bottom = level.getBlockState(lower);
            level.setBlockAndUpdate(lower, bottom.setValue(AGE, Math.min(7, bottom.getValue(AGE) + Mth.nextInt(random, 2, 5))));
        }
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

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AGE);
    }
}
