package com.kltyton.eden_realm.common.item;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ERBoatItem extends Item {
    private final Supplier<? extends EntityType<? extends AbstractBoat>> entityType;

    public ERBoatItem(Supplier<? extends EntityType<? extends AbstractBoat>> entityType, Properties properties) {
        super(properties);
        this.entityType = entityType;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        HitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hitResult.getType() == HitResult.Type.MISS) {
            return InteractionResult.PASS;
        }

        Vec3 viewVector = player.getViewVector(1.0F);
        List<Entity> entities = level.getEntities(
                player,
                player.getBoundingBox().expandTowards(viewVector.scale(5.0D)).inflate(1.0D),
                EntitySelector.CAN_BE_PICKED);
        if (!entities.isEmpty()) {
            Vec3 from = player.getEyePosition();

            for (Entity entity : entities) {
                AABB box = entity.getBoundingBox().inflate(entity.getPickRadius());
                if (box.contains(from)) {
                    return InteractionResult.PASS;
                }
            }
        }

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }

        AbstractBoat boat = getBoat(level, hitResult, itemStack, player);
        if (boat == null) {
            return InteractionResult.FAIL;
        }

        boat.setYRot(player.getYRot());
        if (!level.noCollision(boat, boat.getBoundingBox())) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            level.addFreshEntity(boat);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, hitResult.getLocation());
            itemStack.consume(1, player);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResult.SUCCESS;
    }

    private @Nullable AbstractBoat getBoat(Level level, HitResult hitResult, ItemStack itemStack, Player player) {
        AbstractBoat boat = entityType.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (boat != null) {
            Vec3 location = hitResult.getLocation();
            boat.setInitialPos(location.x, location.y, location.z);
            if (level instanceof ServerLevel serverLevel) {
                EntityType.<AbstractBoat>createDefaultStackConfig(serverLevel, itemStack, player).apply(boat);
            }
        }

        return boat;
    }
}
