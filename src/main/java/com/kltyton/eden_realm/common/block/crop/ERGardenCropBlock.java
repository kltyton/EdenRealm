package com.kltyton.eden_realm.common.block.crop;

import com.kltyton.eden_realm.registry.ERItems;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ERGardenCropBlock extends ERDoubleCropBlock {
    public static final MapCodec<ERGardenCropBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Kind::values).fieldOf("crop").forGetter(block -> block.kind),
            propertiesCodec()).apply(instance, ERGardenCropBlock::new));
    private final Kind kind;

    public ERGardenCropBlock(Kind kind, BlockBehaviour.Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public MapCodec<ERGardenCropBlock> codec() {
        return CODEC;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return kind.shape(state.getValue(AGE), state.getValue(HALF));
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData, Player player) {
        return new ItemStack(ERItems.harvestItem(kind.getSerializedName() + "_seeds").get());
    }

    /** Per-half visible columns measured with tools/measurement/measure_crop_models.py from authored UVs. */
    public enum Kind implements StringRepresentable {
        MOON_CLOVER("moon_clover", new double[][]{
                {16.04, 13, 0, 0}, {16.04, 16, 3.13, 1}, {17.46, 16, 14.63, 7}, {17.46, 16, 16.04, 8}}),
        CRYSTAL_DEW_FRUIT("crystal_dew_fruit", new double[][]{
                {4.95, 9, 0, 0}, {8.57, 13, 0, 0}, {10.95, 16, 5.41, 2.07},
                {10.73, 16, 5.76, 3}, {13.45, 16, 7.53, 5}}),
        VINE_BEAN("vine_bean", new double[][]{
                {9.9, 11, 0, 0}, {11.4, 16, 7.78, 3}, {19.76, 16, 13.3, 7},
                {18.01, 16, 17.46, 13}, {18.86, 16, 21.18, 15}});

        private final String id;
        private final VoxelShape[] lower;
        private final VoxelShape[] upper;

        Kind(String id, double[][] bounds) {
            this.id = id;
            lower = new VoxelShape[bounds.length];
            upper = new VoxelShape[bounds.length];
            for (int stage = 0; stage < bounds.length; stage++) {
                double[] size = bounds[stage];
                lower[stage] = Block.column(size[0], 0, size[1]);
                upper[stage] = size[3] == 0 ? Shapes.empty() : Block.column(size[2], 0, size[3]);
            }
        }

        public int stage(int age) {
            return age * (lower.length - 1) / 7;
        }

        VoxelShape shape(int age, DoubleBlockHalf half) {
            return (half == DoubleBlockHalf.LOWER ? lower : upper)[stage(age)];
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }
}
