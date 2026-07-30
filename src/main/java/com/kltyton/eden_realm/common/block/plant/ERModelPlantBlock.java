package com.kltyton.eden_realm.common.block.plant;

import com.kltyton.eden_realm.common.block.shape.ModelShapeProvider;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ERModelPlantBlock extends VegetationBlock {
    public static final MapCodec<ERModelPlantBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Identifier.CODEC.listOf().fieldOf("models").forGetter(block -> block.modelIds),
                    propertiesCodec())
            .apply(instance, ERModelPlantBlock::new));

    private final List<Identifier> modelIds;
    private final List<VoxelShape> variantShapes;

    public ERModelPlantBlock(List<Identifier> modelIds, BlockBehaviour.Properties properties) {
        super(properties);
        if (modelIds.size() != 3) {
            throw new IllegalArgumentException("Variant model plants require exactly three models");
        }
        this.modelIds = List.copyOf(modelIds);
        this.variantShapes = this.modelIds.stream()
                .map(ModelShapeProvider::shape)
                .toList();
    }

    @Override
    public MapCodec<ERModelPlantBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state, pos).move(state.getOffset(pos));
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return shapeFor(state, pos).move(state.getOffset(pos));
    }

    private VoxelShape shapeFor(BlockState state, BlockPos pos) {
        return ModelShapeProvider.selectEqualWeight(variantShapes, state.getSeed(pos));
    }
}
