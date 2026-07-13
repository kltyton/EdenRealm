package com.kltyton.eden_realm.client.color;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record ERGrassColorSource(float temperature, float downfall) implements ItemTintSource {
    public static final MapCodec<ERGrassColorSource> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    ExtraCodecs.floatRange(0.0F, 1.0F).fieldOf("temperature").forGetter(ERGrassColorSource::temperature),
                    ExtraCodecs.floatRange(0.0F, 1.0F).fieldOf("downfall").forGetter(ERGrassColorSource::downfall))
            .apply(instance, ERGrassColorSource::new));

    public ERGrassColorSource() {
        this(0.5F, 1.0F);
    }

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        return ERGrassColors.sample(temperature, downfall);
    }

    @Override
    public MapCodec<ERGrassColorSource> type() {
        return MAP_CODEC;
    }
}
