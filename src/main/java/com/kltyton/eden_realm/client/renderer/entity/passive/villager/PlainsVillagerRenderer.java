package com.kltyton.eden_realm.client.renderer.entity.passive.villager;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.client.animation.villager.PlainsVillagerLivelyAnimation;
import com.kltyton.eden_realm.common.entity.passive.villager.PlainsVillager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

public final class PlainsVillagerRenderer<R extends PlainsVillagerRenderState & GeoRenderState>
        extends GeoEntityRenderer<PlainsVillager, R> {
    private final Font font;
    private final ItemModelResolver itemModelResolver;
    private final PlainsVillagerLivelyAnimation livelyAnimation = new PlainsVillagerLivelyAnimation();

    public PlainsVillagerRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(ERConstants.id("passive/plains_villager")));
        font = context.getFont();
        itemModelResolver = context.getItemModelResolver();
        shadowRadius = 0.5F;
    }

    @Override
    @SuppressWarnings("unchecked")
    public R createRenderState(PlainsVillager entity, @Nullable Void relatedObject) {
        // GeckoLib injects GeoRenderState and its data storage into EntityRenderState.
        return (R) new PlainsVillagerRenderState();
    }

    @Override
    public void extractRenderState(PlainsVillager entity, R state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.speaking = entity.isSpeaking();
        state.livelyFrame = livelyAnimation.extract(entity, state, partialTick);
        var heldItem = entity.visibleHeldItem();
        state.heldBow = heldItem.is(Items.BOW);
        // Geo exports -X and GeckoLib bakes it back: the artist's right hand is +X.
        // Vanilla item display transforms call that side left.
        itemModelResolver.updateForLiving(state.heldItem, heldItem,
                ItemDisplayContext.THIRD_PERSON_LEFT_HAND, entity);
        state.dialogueLines = java.util.List.of();
        if (state.speaking && !state.isInvisible) {
            int textWidth = font.width(entity.dialogueText());
            int rows = Math.max(1, (textWidth + 94) / 95);
            state.dialogueLines = font.split(entity.dialogueText(), (textWidth + rows - 1) / rows + 4);
            state.dialogueWidth = state.dialogueLines.stream().mapToInt(font::width).max().orElse(0);
        }
    }

    @Override
    public void preRenderPass(RenderPassInfo<R> pass, SubmitNodeCollector tasks) {
        super.preRenderPass(pass, tasks);
        if (pass.renderState().isInvisible || pass.renderState().heldItem.isEmpty()) { return; }
        var handBone = pass.model().getBone("RightHead")
                .orElseThrow(() -> new IllegalStateException("Missing plains villager RightHead bone"));
        pass.addPerBoneRender(handBone, (info, bone, collector) -> {
            PoseStack pose = info.poseStack();
            pose.pushPose();
            // The artist's former RightHead cube spans x=5.1..6.1, y=13..14.5,
            // z=-9..9. Its center is the grip; the group pivot stays unchanged.
            pose.translate(5.5 / 16.0, -8.25 / 16.0, 0);
            // Align the bow's wooden grip with the authored right-hand pose.
            if (info.renderState().heldBow) {
                pose.translate(-0.5 / 16.0, 1.3 / 16.0, -0.7 / 16.0);
            }
            // Match GeckoLib's right-hand item transform.
            pose.mulPose(Axis.XN.rotationDegrees(90));
            pose.translate(0, 0.125, -0.0625);
            info.renderState().heldItem.submit(pose, collector, info.packedLight(),
                    info.packedOverlay(), info.renderState().outlineColor);
            pose.popPose();
        });
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<R> pass, BoneSnapshots bones) {
        PlainsVillagerRenderState state = pass.renderState();
        if (state.livelyFrame != null) {
            livelyAnimation.apply(state.livelyFrame, bones);
        }
        if (!state.speaking) {
            bones.ifPresent("Mouth", mouth -> mouth.setScale(0.87F, 0.9F, 1));
            bones.ifPresent("Mouth2", mouth -> mouth.setScale(1, 1, 1));
            bones.ifPresent("Mouth3", mouth -> mouth.setScale(1, 1, 1));
        }
    }

    @Override
    public void submit(R state, PoseStack pose, SubmitNodeCollector tasks, CameraRenderState camera) {
        super.submit(state, pose, tasks, camera);
        double dx = state.x - camera.pos.x;
        double dy = state.y - camera.pos.y;
        double dz = state.z - camera.pos.z;
        if (state.dialogueLines.isEmpty() || dx * dx + dy * dy + dz * dz > 256.0) {
            return;
        }
        pose.pushPose();
        pose.translate(0, state.eyeHeight + 0.12F, 0);
        pose.mulPose(camera.orientation);
        pose.scale(0.014F, -0.014F, 0.014F);
        int left = 28;
        int right = left + state.dialogueWidth + 10;
        int top = -state.dialogueLines.size() * font.lineHeight - 10;
        tasks.submitCustomGeometry(pose, RenderTypes.textBackground(), (matrix, buffer) -> {
            quad(matrix, buffer, left - 1, top, right + 1, 0, 0, 0xFF384A39);
            quad(matrix, buffer, left, top - 1, right, 1, 0, 0xFF384A39);
            quad(matrix, buffer, left, top, right, 0, 0.01F, 0xFFF7F4E8);
            tail(matrix, buffer, left - 8, -4, left, -12, -3, 0.02F, 0xFF384A39);
            tail(matrix, buffer, left - 5, -5, left + 1, -10, -5, 0.03F, 0xFFF7F4E8);
        });
        pose.translate(0, 0, 0.03F);
        for (int i = 0; i < state.dialogueLines.size(); i++) {
            tasks.submitText(pose, left + 5, top + 5 + i * font.lineHeight, state.dialogueLines.get(i),
                    false, Font.DisplayMode.NORMAL, LightCoordsUtil.FULL_BRIGHT, 0xFF263528, 0, 0);
        }
        pose.popPose();
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer buffer,
                             float left, float top, float right, float bottom, float depth, int color) {
        buffer.addVertex(pose, left, top, depth).setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
        buffer.addVertex(pose, left, bottom, depth).setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
        buffer.addVertex(pose, right, bottom, depth).setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
        buffer.addVertex(pose, right, top, depth).setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
    }

    private static void tail(PoseStack.Pose pose, VertexConsumer buffer, float tipX, float tipY,
                             float baseX, float top, float bottom, float depth, int color) {
        buffer.addVertex(pose, tipX, tipY, depth).setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
        buffer.addVertex(pose, baseX, bottom, depth).setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
        buffer.addVertex(pose, baseX, top, depth).setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
        buffer.addVertex(pose, baseX, top, depth).setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
    }
}
