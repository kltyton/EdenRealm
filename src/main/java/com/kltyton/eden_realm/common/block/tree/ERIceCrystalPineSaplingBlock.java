package com.kltyton.eden_realm.common.block.tree;

import com.kltyton.eden_realm.world.tree.IceCrystalPineTrees;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;

/** Grows a large pine from two horizontal neighbors, preserving both saplings on failure. */
public final class ERIceCrystalPineSaplingBlock extends SaplingBlock {
    public static final MapCodec<ERIceCrystalPineSaplingBlock> CODEC = simpleCodec(ERIceCrystalPineSaplingBlock::new);
    private static final TreeGrower GROWER = new TreeGrower("eden_realm:ice_crystal_pine",
            Optional.empty(), Optional.of(IceCrystalPineTrees.SINGLE), Optional.empty());

    public ERIceCrystalPineSaplingBlock(Properties properties) {
        super(GROWER, properties);
    }

    @Override
    public MapCodec<ERIceCrystalPineSaplingBlock> codec() {
        return CODEC;
    }

    @Override
    public void advanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if (state.getValue(STAGE) == 1) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                if (level.getBlockState(pos.relative(direction)).is(this)) {
                    var feature = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE)
                            .getOrThrow(IceCrystalPineTrees.PAIRED).value();
                    feature.place(level, level.getChunkSource().getGenerator(), random, pos);
                    return;
                }
            }
        }
        super.advanceTree(level, pos, state, random);
    }
}
