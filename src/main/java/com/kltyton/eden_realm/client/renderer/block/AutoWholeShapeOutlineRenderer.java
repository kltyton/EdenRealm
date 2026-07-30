package com.kltyton.eden_realm.client.renderer.block;

import com.kltyton.eden_realm.common.block.shape.AutoWholeShapeBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

/**
 * Replaces the vanilla local-cell outline for {@link AutoWholeShapeBlock}
 * without changing the shape used by the player's block ray cast.
 */
public final class AutoWholeShapeOutlineRenderer {
    private AutoWholeShapeOutlineRenderer() {
    }

    public static void extract(ExtractBlockOutlineRenderStateEvent event) {
        BlockState state = event.getBlockState();
        if (!(state.getBlock() instanceof AutoWholeShapeBlock block)) {
            return;
        }

        BlockPos partPos = event.getBlockPos();
        if (!block.isValidPart(event.getLevel(), state, partPos)) {
            return;
        }
        BlockPos originPos = block.getOriginPosition(state, partPos).immutable();
        VoxelShape wholeShape =
                block.getWholeOutlineShape(event.getLevel(), state, partPos);
        if (wholeShape.isEmpty()) {
            return;
        }

        event.addCustomRenderer((renderState, output, poseStack, levelRenderState) ->
                render(
                        originPos,
                        wholeShape,
                        renderState,
                        output,
                        poseStack,
                        levelRenderState));
    }

    private static boolean render(
            BlockPos originPos,
            VoxelShape shape,
            BlockOutlineRenderState renderState,
            SubmitNodeCollector output,
            PoseStack poseStack,
            LevelRenderState levelRenderState) {
        Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
        poseStack.pushPose();
        try {
            poseStack.translate(
                    originPos.getX() - cameraPos.x,
                    originPos.getY() - cameraPos.y,
                    originPos.getZ() - cameraPos.z);
            if (renderState.highContrast()) {
                output.submitShapeOutline(
                        poseStack,
                        shape,
                        RenderTypes.secondaryBlockOutline(),
                        -16777216,
                        7.0F,
                        renderState.isTranslucent());
            }

            int color = renderState.highContrast() ? -11010079 : ARGB.black(102);
            float lineWidth = Minecraft.getInstance()
                    .gameRenderer
                    .gameRenderState()
                    .windowRenderState
                    .appropriateLineWidth;
            output.submitShapeOutline(
                    poseStack,
                    shape,
                    RenderTypes.lines(),
                    color,
                    lineWidth,
                    renderState.isTranslucent());
        } finally {
            poseStack.popPose();
        }
        return true;
    }
}
