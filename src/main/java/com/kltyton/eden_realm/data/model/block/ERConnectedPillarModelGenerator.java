package com.kltyton.eden_realm.data.model.block;

import com.kltyton.eden_realm.common.block.building.ERConnectedPillarBlock;
import com.mojang.math.Quadrant;
import java.util.EnumMap;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

final class ERConnectedPillarModelGenerator {
    private ERConnectedPillarModelGenerator() { }

    static void generate(BlockModelGenerators models, ERConnectedPillarBlock block) {
        var variants = new EnumMap<ERConnectedPillarBlock.Part, Identifier>(ERConnectedPillarBlock.Part.class);
        var horizontalVariants = new EnumMap<ERConnectedPillarBlock.Part, Identifier>(ERConnectedPillarBlock.Part.class);
        for (var part : ERConnectedPillarBlock.Part.values()) {
            String suffix = part == ERConnectedPillarBlock.Part.SINGLE ? "" : "_" + part.getSerializedName();
            var textures = TextureMapping.column(TextureMapping.getBlockTexture(block, suffix),
                    TextureMapping.getBlockTexture(block, "_end"));
            variants.put(part, ModelTemplates.CUBE_COLUMN.create(ModelLocationUtils.getModelLocation(block, suffix),
                    textures, models.modelOutput));
            horizontalVariants.put(part, ModelTemplates.CUBE_COLUMN_HORIZONTAL.create(
                    ModelLocationUtils.getModelLocation(block, suffix + "_horizontal"), textures, models.modelOutput));
        }
        var dispatch = PropertyDispatch.initial(ERConnectedPillarBlock.AXIS, ERConnectedPillarBlock.PART);
        for (Direction.Axis axis : Direction.Axis.values()) {
            for (var part : ERConnectedPillarBlock.Part.values()) {
                MultiVariant variant = BlockModelGenerators.plainVariant(
                        (axis == Direction.Axis.Y ? variants : horizontalVariants).get(part));
                if (axis != Direction.Axis.Y) variant = variant.with(VariantMutator.X_ROT.withValue(Quadrant.R90));
                if (axis == Direction.Axis.X) variant = variant.with(VariantMutator.Y_ROT.withValue(Quadrant.R90));
                dispatch.select(axis, part, variant);
            }
        }
        models.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(dispatch));
        models.registerSimpleItemModel(block, variants.get(ERConnectedPillarBlock.Part.SINGLE));
    }
}
