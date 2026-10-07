package com.kltyton.eden_realm.mixin;

import com.kltyton.eden_realm.common.block.cloud.ERSinkingCloudBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.WaterFluid;
import net.minecraft.world.phys.shapes.Shapes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Falling water ends at sinking clouds instead of spreading across their walkable surface. */
@Mixin(FlowingFluid.class)
public abstract class CloudWaterFlowMixin {
    @Inject(method = "spreadToSides", at = @At("HEAD"), cancellable = true)
    private void edenRealm$stopCloudSurfaceSpread(ServerLevel level, BlockPos pos, FluidState fluidState,
                                                BlockState state, CallbackInfo callback) {
        if ((Object) this instanceof WaterFluid
                && level.getBlockState(pos.below()).getBlock() instanceof ERSinkingCloudBlock) {
            callback.cancel();
        }
    }

    @Inject(method = "getNewLiquid", at = @At("HEAD"), cancellable = true)
    private void edenRealm$keepOnlyIncomingCloudWater(ServerLevel level, BlockPos pos, BlockState state,
                                                    CallbackInfoReturnable<FluidState> callback) {
        if (!((Object) this instanceof WaterFluid)
                || !(level.getBlockState(pos.below()).getBlock() instanceof ERSinkingCloudBlock)) return;
        FlowingFluid water = (FlowingFluid) (Object) this;
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        boolean incoming = aboveState.getFluidState().getType().isSame(water)
                && !Shapes.mergedFaceOccludes(state.getCollisionShape(level, pos),
                        aboveState.getCollisionShape(level, above), Direction.UP);
        callback.setReturnValue(incoming ? water.getFlowing(8, true) : Fluids.EMPTY.defaultFluidState());
    }
}
