package com.kltyton.eden_realm.world.feature;

import com.kltyton.eden_realm.world.terrain.TerrainProfile;
import com.kltyton.eden_realm.world.terrain.TerrainWorldProfiles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;

/** The block operations shared by normal icy features and their in-memory preview. */
public interface IcyFeatureArea {
    int getHeight(Heightmap.Types type, int x, int z);

    BlockState getBlockState(BlockPos pos);

    default FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    void setBlock(BlockPos pos, BlockState state, int flags);

    Holder<Biome> getBiome(BlockPos pos);

    TerrainProfile profile(Holder<Biome> biome);

    boolean shouldSnow(Holder<Biome> biome, BlockPos pos);

    boolean isSolidShore(BlockState state, BlockPos pos);

    static IcyFeatureArea of(WorldGenLevel level) {
        return new IcyFeatureArea() {
            @Override
            public int getHeight(Heightmap.Types type, int x, int z) {
                return level.getHeight(type, x, z);
            }

            @Override
            public BlockState getBlockState(BlockPos pos) {
                return level.getBlockState(pos);
            }

            @Override
            public void setBlock(BlockPos pos, BlockState state, int flags) {
                level.setBlock(pos, state, flags);
            }

            @Override
            public Holder<Biome> getBiome(BlockPos pos) {
                return level.getBiome(pos);
            }

            @Override
            public TerrainProfile profile(Holder<Biome> biome) {
                return TerrainWorldProfiles.profile(level, biome);
            }

            @Override
            public boolean shouldSnow(Holder<Biome> biome, BlockPos pos) {
                return biome.value().shouldSnow(level, pos);
            }

            @Override
            public boolean isSolidShore(BlockState state, BlockPos pos) {
                return state.isFaceSturdy(level, pos, Direction.UP);
            }
        };
    }
}
