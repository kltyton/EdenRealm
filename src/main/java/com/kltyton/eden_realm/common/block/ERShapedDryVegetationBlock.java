package com.kltyton.eden_realm.common.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DryVegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ERShapedDryVegetationBlock extends DryVegetationBlock {
    public static final MapCodec<ERShapedDryVegetationBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.DOUBLE.fieldOf("shape_width").forGetter(block -> block.shapeWidth),
                    Codec.DOUBLE.fieldOf("shape_height").forGetter(block -> block.shapeHeight),
                    propertiesCodec())
            .apply(instance, ERShapedDryVegetationBlock::new));

    private final double shapeWidth;
    private final double shapeHeight;
    private final VoxelShape shape;

    public ERShapedDryVegetationBlock(double shapeWidth, double shapeHeight, BlockBehaviour.Properties properties) {
        super(properties);
        this.shapeWidth = shapeWidth;
        this.shapeHeight = shapeHeight;
        this.shape = Block.column(shapeWidth, 0.0, shapeHeight);
    }

    @Override
    public MapCodec<ERShapedDryVegetationBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape.move(state.getOffset(pos));
    }
}
