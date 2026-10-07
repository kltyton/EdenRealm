package com.kltyton.eden_realm.world.dimension;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.EdenRealm;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Moves players between Eden's main layer and the sky layer at the vertical boundaries. */
public final class SkyLayerTravel {
    private static final ResourceKey<Level> EDEN = ResourceKey.create(
            Registries.DIMENSION, ERConstants.id("eden_layer"));
    private static final ResourceKey<Level> SKY = ResourceKey.create(
            Registries.DIMENSION, ERConstants.id("sky_layer"));
    private static final double ENTRY_HEIGHT = 512.0;
    private static final double CLOUD_CLEARANCE = 5.0;
    private static final double RETURN_HEIGHT = -64.0;
    // 500 sits clear of the ENTRY_HEIGHT trigger so a return does not immediately bounce back upward.
    private static final double RETURN_ARRIVAL_HEIGHT = 500.0;

    private SkyLayerTravel() { }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) return;
        ResourceKey<Level> dimension = player.level().dimension();
        if (dimension.equals(EDEN) && player.getY() >= ENTRY_HEIGHT) {
            ServerLevel sky = resolve(player, SKY);
            if (sky != null) transfer(player, sky, sky.getMinY() + CLOUD_CLEARANCE);
        } else if (dimension.equals(SKY) && player.getY() <= RETURN_HEIGHT) {
            ServerLevel eden = resolve(player, EDEN);
            if (eden != null) transfer(player, eden, RETURN_ARRIVAL_HEIGHT);
        }
    }

    private static ServerLevel resolve(ServerPlayer player, ResourceKey<Level> key) {
        ServerLevel level = player.level().getServer().getLevel(key);
        if (level == null && player.tickCount % 100 == 0)
            EdenRealm.LOGGER.error("Cannot travel to {}: dimension is not loaded", key.identifier());
        return level;
    }

    // Native transition keeps X/Z, rotation and current momentum; only the fall distance accrued so far is cleared.
    private static void transfer(ServerPlayer player, ServerLevel destination, double y) {
        Vec3 arrival = new Vec3(player.getX(), y, player.getZ());
        player.teleport(new TeleportTransition(destination, arrival, player.getKnownMovement(),
                player.getYRot(), player.getXRot(),
                TeleportTransition.PLACE_PORTAL_TICKET.then(entity -> entity.resetFallDistance())));
    }
}
