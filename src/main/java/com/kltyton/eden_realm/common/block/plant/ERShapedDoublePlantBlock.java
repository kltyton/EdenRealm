package com.kltyton.eden_realm.common.block.plant;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ERShapedDoublePlantBlock extends DoublePlantBlock {
    public static final MapCodec<ERShapedDoublePlantBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.DOUBLE.fieldOf("lower_shape_width").forGetter(block -> block.lowerShapeWidth),
                    Codec.DOUBLE.fieldOf("lower_shape_height").forGetter(block -> block.lowerShapeHeight),
                    Codec.DOUBLE.fieldOf("upper_shape_width").forGetter(block -> block.upperShapeWidth),
                    Codec.DOUBLE.fieldOf("upper_shape_height").forGetter(block -> block.upperShapeHeight),
                    propertiesCodec())
            .apply(instance, ERShapedDoublePlantBlock::new));

    private final double lowerShapeWidth;
    private final double lowerShapeHeight;
    private final double upperShapeWidth;
    private final double upperShapeHeight;
    private final VoxelShape lowerShape;
    private final VoxelShape upperShape;

    public ERShapedDoublePlantBlock(
            ERPlantShapes.DoublePlantShape shape,
            BlockBehaviour.Properties properties) {
        this(
                shape.lowerWidth(),
                shape.lowerHeight(),
                shape.upperWidth(),
                shape.upperHeight(),
                properties);
    }

    public ERShapedDoublePlantBlock(
            double lowerShapeWidth,
            double lowerShapeHeight,
            double upperShapeWidth,
            double upperShapeHeight,
            BlockBehaviour.Properties properties) {
        super(properties);
        this.lowerShapeWidth = lowerShapeWidth;
        this.lowerShapeHeight = lowerShapeHeight;
        this.upperShapeWidth = upperShapeWidth;
        this.upperShapeHeight = upperShapeHeight;
        this.lowerShape = Block.column(lowerShapeWidth, 0.0, lowerShapeHeight);
        this.upperShape = Block.column(upperShapeWidth, 0.0, upperShapeHeight);
    }

    @Override
    public MapCodec<? extends ERShapedDoublePlantBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        VoxelShape shape = state.getValue(HALF) == DoubleBlockHalf.LOWER ? lowerShape : upperShape;
        return shape.move(state.getOffset(pos));
    }

    protected final double lowerShapeWidth() {
        return lowerShapeWidth;
    }

    protected final double lowerShapeHeight() {
        return lowerShapeHeight;
    }

    protected final double upperShapeWidth() {
        return upperShapeWidth;
    }

    protected final double upperShapeHeight() {
        return upperShapeHeight;
    }
}
