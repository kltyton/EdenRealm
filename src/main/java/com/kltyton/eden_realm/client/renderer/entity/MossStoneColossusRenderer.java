package com.kltyton.eden_realm.client.renderer.entity;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.entity.boss.MossStoneColossus;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public final class MossStoneColossusRenderer
        extends GeoEntityRenderer<MossStoneColossus, LivingEntityRenderState> {

    public MossStoneColossusRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(ERConstants.id("moss_stone_colossus")));
        shadowRadius = 2.0F;
    }

    @Override
    protected float getDeathMaxRotation(GeoRenderState renderState) {
        return 0.0F;
    }
}
