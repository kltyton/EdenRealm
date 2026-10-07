package com.kltyton.eden_realm.world.terrain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/** Evaluates each horizontal biome score once without duplicating its density tree. */
public record WeightedBiomeRegion(DensityFunction input, List<Band> bands)
        implements DensityFunction.SimpleFunction {
    public record Band(double center, double distanceScale, double weightBias,
                       double variation, DensityFunction detail) {
        private static final Codec<Band> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.doubleRange(-1.0, 1.0).fieldOf("center").forGetter(Band::center),
                Codec.DOUBLE.fieldOf("distance_scale").forGetter(Band::distanceScale),
                Codec.DOUBLE.fieldOf("weight_bias").forGetter(Band::weightBias),
                Codec.DOUBLE.fieldOf("variation").forGetter(Band::variation),
                DensityFunction.CODEC.fieldOf("detail").forGetter(Band::detail)
        ).apply(instance, Band::new));

        private double score(double original, FunctionContext context) {
            double distance = Math.abs(original - center) * distanceScale;
            double adjustment = variation == 0.0 ? 0.0 : detail.compute(context) * variation;
            return (distance + weightBias) + adjustment;
        }
    }

    private static final MapCodec<WeightedBiomeRegion> DATA_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    DensityFunction.CODEC.fieldOf("input").forGetter(WeightedBiomeRegion::input),
                    Band.CODEC.listOf().fieldOf("bands").forGetter(WeightedBiomeRegion::bands)
            ).apply(instance, WeightedBiomeRegion::new));
    public static final KeyDispatchDataCodec<WeightedBiomeRegion> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);

    public WeightedBiomeRegion {
        bands = List.copyOf(bands);
        if (bands.isEmpty()) throw new IllegalArgumentException("At least one biome must be enabled");
    }

    @Override
    public double compute(FunctionContext context) {
        double original = input.compute(context);
        double selected = bands.getFirst().center();
        double leading = bands.getFirst().score(original, context);
        for (int i = 1; i < bands.size(); i++) {
            Band band = bands.get(i);
            double candidate = band.score(original, context);
            double blend = Math.max(0.0, Math.min(1.0, (candidate - leading) * 6.25 + 0.5));
            selected = i == 1 ? blend * (band.center() - selected) + selected
                    : blend * band.center() + (1.0 - blend) * selected;
            leading = Math.max(leading, candidate);
        }
        return selected;
    }

    @Override
    public DensityFunction mapChildren(Visitor visitor) {
        return new WeightedBiomeRegion(visitor.apply(input), bands.stream()
                .map(band -> new Band(band.center(), band.distanceScale(), band.weightBias(),
                        band.variation(), visitor.apply(band.detail()))).toList());
    }

    @Override public double minValue() { return bands.stream().mapToDouble(Band::center).min().orElseThrow(); }
    @Override public double maxValue() { return bands.stream().mapToDouble(Band::center).max().orElseThrow(); }
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() { return CODEC; }
}
