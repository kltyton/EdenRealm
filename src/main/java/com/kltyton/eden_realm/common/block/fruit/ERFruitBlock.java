package com.kltyton.eden_realm.common.block.fruit;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.bonehitboxlib.api.block.shape.ModelShapeProvider;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ERFruitBlock extends Block {
    public static final IntegerProperty COUNT = IntegerProperty.create("count", 1, 4);
    public static final MapCodec<ERFruitBlock> CODEC = simpleCodec(ERFruitBlock::new);

    public ERFruitBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends ERFruitBlock> codec() {
        return CODEC;
    }

    public static int count(BlockState state) {
        return state.hasProperty(COUNT) ? state.getValue(COUNT) : 1;
    }

    public static String modelName(BlockState state) {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return id + (state.hasProperty(COUNT) ? "_ground_" + count(state) : "_stage_4");
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return state.hasProperty(COUNT) && count(state) < 4 && !context.isSecondaryUseActive()
                && context.getItemInHand().is(asItem());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        return state.is(this) && state.hasProperty(COUNT)
                ? state.setValue(COUNT, Math.min(4, count(state) + 1)) : defaultBlockState();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty() || !player.mayBuild()) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel) {
            if (count(state) == 1) {
                level.removeBlock(pos, false);
            } else {
                level.setBlockAndUpdate(pos, state.setValue(COUNT, count(state) - 1));
            }
            ItemStack fruit = new ItemStack(asItem());
            if (!player.addItem(fruit)) {
                player.drop(fruit, false);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ModelShapeProvider.shape(ERConstants.id("block/fruit/" + modelName(state)));
    }

    /** The authored single-block twilight fruit intentionally has no count state. */
    public static final class Stacked extends ERFruitBlock {
        public static final MapCodec<Stacked> CODEC = simpleCodec(Stacked::new);

        public Stacked(BlockBehaviour.Properties properties) {
            super(properties);
            registerDefaultState(defaultBlockState().setValue(COUNT, 1));
        }

        @Override
        public MapCodec<Stacked> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(COUNT);
        }
    }
}
