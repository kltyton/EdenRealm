package com.kltyton.eden_realm.common.block.fruit;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ERTwilightFruitBlock extends ERFruitBlock {
    public static final MapCodec<ERTwilightFruitBlock> CODEC = simpleCodec(ERTwilightFruitBlock::new);
    private static final VoxelShape CONTACT_BOUNDS = Block.column(14, 0, 15);

    public ERTwilightFruitBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<ERTwilightFruitBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return com.kltyton.bonehitboxlib.api.block.shape.ModelShapeProvider.shape(
                com.kltyton.eden_realm.ERConstants.id("block/fruit/twilight_pomegranate_shape_4"));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Cactus-like contact margin lets a touching entity enter the damage cell without changing the model or outline.
        return Shapes.join(getShape(state, level, pos, context), CONTACT_BOUNDS, BooleanOp.AND);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
            InsideBlockEffectApplier effects, boolean isPrecise) {
        entity.hurt(level.damageSources().cactus(), 1.0F);
    }
}
