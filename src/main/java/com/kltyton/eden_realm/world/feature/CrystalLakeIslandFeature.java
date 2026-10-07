package com.kltyton.eden_realm.world.feature;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.registry.content.block.ERTerrainBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** A supported snow-covered island inside open crystal-lake water. */
public final class CrystalLakeIslandFeature extends Feature<NoneFeatureConfiguration> {
    private static final ResourceKey<Biome> LAKE = ResourceKey.create(
            Registries.BIOME, ERConstants.id("crystal_lake_shore"));
    private static final ResourceKey<Biome> SILVER_LAKE = ResourceKey.create(
            Registries.BIOME, ERConstants.id("silver_frost_lakeshore"));

    public CrystalLakeIslandFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        return place(IcyFeatureArea.of(context.level()), context.origin(), context.random());
    }

    public static boolean place(IcyFeatureArea level, BlockPos origin, RandomSource random) {
        if ((!level.getBiome(origin).is(LAKE) && !level.getBiome(origin).is(SILVER_LAKE)) || !level.getBlockState(origin.atY(62)).is(Blocks.WATER)) {
            return false;
        }
        int radius = 4 + random.nextInt(4);
        int rise = Math.max(1, Math.round((4 + random.nextInt(4))
                * level.profile(level.getBiome(origin)).shapePercent() / 100.0F));
        for (int dx = -radius - 2; dx <= radius + 2; dx++) {
            for (int dz = -radius - 2; dz <= radius + 2; dz++) {
                if (dx * dx + dz * dz > (radius + 2) * (radius + 2)) {
                    continue;
                }
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                if (!level.getBlockState(new BlockPos(x, 62, z)).is(Blocks.WATER)) {
                    return false;
                }
                if (dx * dx + dz * dz <= radius * radius) {
                    int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
                    if (floor < 47 || floor > 61) {
                        return false;
                    }
                }
            }
        }

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int distanceSquared = dx * dx + dz * dz;
                if (distanceSquared > radius * radius) {
                    continue;
                }
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
                int top = 63 + Math.round((1.0F - (float) distanceSquared / (radius * radius)) * rise);
                for (int y = floor + 1; y <= top; y++) {
                    var state = y == top ? ERTerrainBlocks.EDEN_GRASS_BLOCK.get().defaultBlockState()
                            : y >= top - 2 ? ERTerrainBlocks.EDEN_DIRT.get().defaultBlockState()
                            : Blocks.STONE.defaultBlockState();
                    level.setBlock(new BlockPos(x, y, z), state, 2);
                }
            }
        }
        return true;
    }
}
