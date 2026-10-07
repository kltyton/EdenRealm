package com.kltyton.eden_realm.client.renderer.entity.passive.villager;

import com.kltyton.eden_realm.client.animation.villager.PlainsVillagerLivelyAnimation;
import java.util.List;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

public class PlainsVillagerRenderState extends LivingEntityRenderState {
    public List<FormattedCharSequence> dialogueLines = List.of();
    public int dialogueWidth;
    public boolean speaking;
    public PlainsVillagerLivelyAnimation.@Nullable Frame livelyFrame;
    public final ItemStackRenderState heldItem = new ItemStackRenderState();
    public boolean heldBow;

}
