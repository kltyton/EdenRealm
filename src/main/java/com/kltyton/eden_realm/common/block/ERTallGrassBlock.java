package com.kltyton.eden_realm.common.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ERTallGrassBlock extends DoublePlantBlock {
    public static final MapCodec<ERTallGrassBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.DOUBLE.fieldOf("lower_shape_width").forGetter(block -> block.lowerShapeWidth),
                    Codec.DOUBLE.fieldOf("lower_shape_height").forGetter(block -> block.lowerShapeHeight),
                    Codec.DOUBLE.fieldOf("upper_shape_width").forGetter(block -> block.upperShapeWidth),
                    Codec.DOUBLE.fieldOf("upper_shape_height").forGetter(block -> block.upperShapeHeight),
                    BlockState.CODEC.fieldOf("short_plant").forGetter(block -> block.shortPlant.get()),
                    propertiesCodec())
            .apply(instance, ERTallGrassBlock::decode));

    private final double lowerShapeWidth;
    private final double lowerShapeHeight;
    private final double upperShapeWidth;
    private final double upperShapeHeight;
    private final Supplier<BlockState> shortPlant;
    private final VoxelShape lowerShape;
    private final VoxelShape upperShape;

    public ERTallGrassBlock(
            double lowerShapeWidth,
            double lowerShapeHeight,
            double upperShapeWidth,
            double upperShapeHeight,
            Supplier<BlockState> shortPlant,
            BlockBehaviour.Properties properties) {
        super(properties);
        this.lowerShapeWidth = lowerShapeWidth;
        this.lowerShapeHeight = lowerShapeHeight;
        this.upperShapeWidth = upperShapeWidth;
        this.upperShapeHeight = upperShapeHeight;
        this.shortPlant = shortPlant;
        this.lowerShape = Block.column(lowerShapeWidth, 0.0, lowerShapeHeight);
        this.upperShape = Block.column(upperShapeWidth, 0.0, upperShapeHeight);
    }

    private static ERTallGrassBlock decode(
            double lowerShapeWidth,
            double lowerShapeHeight,
            double upperShapeWidth,
            double upperShapeHeight,
            BlockState shortPlant,
            BlockBehaviour.Properties properties) {
        return new ERTallGrassBlock(
                lowerShapeWidth,
                lowerShapeHeight,
                upperShapeWidth,
                upperShapeHeight,
                () -> shortPlant,
                properties);
    }

    @Override
    public MapCodec<ERTallGrassBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = state.getValue(HALF) == DoubleBlockHalf.LOWER ? lowerShape : upperShape;
        return shape.move(state.getOffset(pos));
    }

    @Override
    public ItemStack getCloneItemStack(
            LevelReader level,
            BlockPos pos,
            BlockState state,
            boolean includeData,
            Player player) {
        return new ItemStack(shortPlant.get().getBlock());
    }
}
