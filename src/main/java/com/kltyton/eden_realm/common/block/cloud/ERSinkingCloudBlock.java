package com.kltyton.eden_realm.common.block.cloud;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ERSinkingCloudBlock extends HalfTransparentBlock {
    public static final MapCodec<ERSinkingCloudBlock> CODEC = simpleCodec(ERSinkingCloudBlock::new);
    private static final VoxelShape BOTTOM_SUPPORT = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.000625, 1.0);
    private static final VoxelShape FALLING_SUPPORT = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.9, 1.0);
    private static final VoxelShape ITEM_SUPPORT = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.65, 1.0);
    private static final double TERMINAL_SINK_SPEED = -0.08;
    private static final double TERMINAL_SINK_THRESHOLD = -0.0784000015258789;

    public ERSinkingCloudBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends ERSinkingCloudBlock> codec() {
        return CODEC;
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier effectApplier,
            boolean isPrecise) {
        entity.resetFallDistance();
        if (entity instanceof ItemEntity itemEntity) {
            itemEntity.setDeltaMovement(itemEntity.getDeltaMovement().scale(0.99F));
            return;
        }

        if (entity.getDeltaMovement().y < TERMINAL_SINK_THRESHOLD) {
            if (!(entity instanceof Projectile)) {
                entity.setDeltaMovement(
                        entity.getDeltaMovement().x(),
                        TERMINAL_SINK_SPEED,
                        entity.getDeltaMovement().z());
            }
            entity.setOnGround(entity instanceof LivingEntity livingEntity
                    && (!(livingEntity instanceof Player player) || !player.getAbilities().flying));
        }
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        if (level.getBlockState(pos.above()).getBlock() instanceof ERSinkingCloudBlock) {
            return Shapes.block();
        }
        if (context instanceof EntityCollisionContext entityContext) {
            Entity entity = entityContext.getEntity();
            if (entity instanceof ItemEntity) {
                return ITEM_SUPPORT;
            }
            if (entity != null) {
                if (entity.fallDistance > 2.5
                        && (!(entity instanceof LivingEntity living) || !living.isFallFlying())) {
                    return FALLING_SUPPORT;
                }
            }
        }
        return BOTTOM_SUPPORT;
    }

    @Override
    protected VoxelShape getVisualShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 0.25F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return false;
    }
}
