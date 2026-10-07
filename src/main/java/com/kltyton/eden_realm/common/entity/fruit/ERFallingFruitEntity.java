package com.kltyton.eden_realm.common.entity.fruit;

import com.kltyton.eden_realm.common.block.fruit.ERFruitBlock;
import com.kltyton.eden_realm.registry.EREntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Keeps vanilla falling physics, persistence and spawn packets, including the fruit count. */
public final class ERFallingFruitEntity extends FallingBlockEntity {
    public ERFallingFruitEntity(EntityType<? extends ERFallingFruitEntity> type, Level level) {
        super(type, level);
    }

    public static ERFallingFruitEntity fall(ServerLevel level, BlockPos pos, BlockState fruit) {
        BlockState source = level.getBlockState(pos);
        ERFallingFruitEntity entity = new ERFallingFruitEntity(EREntityTypes.FALLING_FRUIT.get(), level);
        entity.blockState = fruit;
        entity.blocksBuilding = true;
        entity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.xo = entity.getX();
        entity.yo = entity.getY();
        entity.zo = entity.getZ();
        entity.setStartPos(pos);
        entity.setHurtsEntities(2.0F, 40);
        if (!level.addFreshEntity(entity)) {
            entity.discard();
            return entity;
        }
        if (!level.getBlockState(pos).equals(source)
                || (!source.isAir() && !level.setBlock(pos, level.getFluidState(pos).createLegacyBlock(), 3))) {
            entity.discard();
        }
        return entity;
    }

    @Override
    public @Nullable ItemEntity spawnAtLocation(ServerLevel level, ItemLike item) {
        return super.spawnAtLocation(level, new ItemStack(item, ERFruitBlock.count(getBlockState())));
    }
}
