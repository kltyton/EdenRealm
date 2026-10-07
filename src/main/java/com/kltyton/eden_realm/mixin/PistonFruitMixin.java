package com.kltyton.eden_realm.mixin;

import com.kltyton.eden_realm.common.event.fruit.ERFruitDisturbance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Detaches ripe fruit after successful resolution but before native piston destruction can drop it. */
@Mixin(PistonBaseBlock.class)
public abstract class PistonFruitMixin {
    @Redirect(method = "moveBlocks", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/piston/PistonStructureResolver;resolve()Z"))
    private boolean edenRealm$resolvedFruit(PistonStructureResolver resolver, Level level, BlockPos pistonPos,
            Direction direction, boolean extending) {
        boolean resolved = resolver.resolve();
        if (resolved && level instanceof ServerLevel server) {
            ERFruitDisturbance.onResolvedPiston(server, pistonPos, direction, resolver);
        }
        return resolved;
    }
}
