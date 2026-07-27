package com.kltyton.eden_realm.common.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ERGrowableGrassBlock extends VegetationBlock implements BonemealableBlock {
    public static final MapCodec<ERGrowableGrassBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.DOUBLE.fieldOf("shape_width").forGetter(block -> block.shapeWidth),
                    Codec.DOUBLE.fieldOf("shape_height").forGetter(block -> block.shapeHeight),
                    BlockState.CODEC.fieldOf("tall_plant").forGetter(block -> block.tallPlant.get()),
                    propertiesCodec())
            .apply(instance, ERGrowableGrassBlock::decode));

    private final double shapeWidth;
    private final double shapeHeight;
    private final Supplier<BlockState> tallPlant;
    private final VoxelShape shape;

    public ERGrowableGrassBlock(
            double shapeWidth,
            double shapeHeight,
            Supplier<BlockState> tallPlant,
            BlockBehaviour.Properties properties) {
        super(properties);
        this.shapeWidth = shapeWidth;
        this.shapeHeight = shapeHeight;
        this.tallPlant = tallPlant;
        this.shape = Block.column(shapeWidth, 0.0, shapeHeight);
    }

    private static ERGrowableGrassBlock decode(
            double shapeWidth,
            double shapeHeight,
            BlockState tallPlant,
            BlockBehaviour.Properties properties) {
        return new ERGrowableGrassBlock(shapeWidth, shapeHeight, () -> tallPlant, properties);
    }

    @Override
    public MapCodec<ERGrowableGrassBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape.move(state.getOffset(pos));
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        BlockState tallState = tallPlant.get();
        return tallState.canSurvive(level, pos)
                && level.isEmptyBlock(pos.above())
                && level.isInsideBuildHeight(pos.above());
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        DoublePlantBlock.placeAt(level, tallPlant.get(), pos, 2);
    }
}
