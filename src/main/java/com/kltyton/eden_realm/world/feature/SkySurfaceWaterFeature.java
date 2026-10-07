package com.kltyton.eden_realm.world.feature;

import com.kltyton.eden_realm.registry.content.block.ERTerrainBlocks;
import com.kltyton.eden_realm.world.terrain.SkyLandformDensity;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;

/** Applies island strata and fills elevated water from the same complete island columns. */
public final class SkySurfaceWaterFeature extends Feature<NoneFeatureConfiguration> {
    public SkySurfaceWaterFeature(Codec<NoneFeatureConfiguration> codec) { super(codec); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        SkyLandformDensity field = SkyLandformDensity.from(context.level().getLevel().getChunkSource().randomState());
        boolean placed = place(IcyFeatureArea.of(context.level()), context.origin(), field);
        if (placed) scheduleLakeEdges(context, field);
        return placed;
    }

    private static void scheduleLakeEdges(FeaturePlaceContext<NoneFeatureConfiguration> context, SkyLandformDensity field) {
        var level = context.level();
        BlockPos.MutableBlockPos source = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();
        for (int dz = 0; dz < 16; dz++) for (int dx = 0; dx < 16; dx++) {
            int x = context.origin().getX() + dx, z = context.origin().getZ() + dz;
            var column = field.sample(x, z);
            if ((column.biome() != 2 && column.biome() != 3 && column.biome() != 5)
                    || !column.land() || column.water() <= column.top()) continue;
            source.set(x, column.water() - 1, z);
            if (!level.getBlockState(source).is(Blocks.WATER)) continue;
            for (int side = 0; side < 4; side++) {
                int offsetX = side == 0 ? -1 : side == 1 ? 1 : 0;
                int offsetZ = side == 2 ? -1 : side == 3 ? 1 : 0;
                if (level.getBlockState(neighbor.set(x + offsetX, source.getY(), z + offsetZ)).isAir()) {
                    // Worldgen writes do not start fluid simulation; exposed lake sources use vanilla ticks.
                    level.scheduleTick(source.immutable(), Fluids.WATER, 0);
                    break;
                }
            }
        }
    }
    public static boolean place(IcyFeatureArea area, BlockPos origin, SkyLandformDensity field) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dz = 0; dz < 16; dz++) for (int dx = 0; dx < 16; dx++) {
            int x = origin.getX() + dx, z = origin.getZ() + dz;
            var column = field.sample(x, z);
            for (var span : column.solidSpans()) {
                for (int y = span.fromY(); y < span.toY(); y++) {
                    pos.set(x, y, z);
                    BlockState old = area.getBlockState(pos);
                    if (isTerrain(old)) {
                        BlockState material = terrainBlock(column, y);
                        if (old != material) area.setBlock(pos, material, 2);
                    }
                }
            }
            for (int y = column.rockBottom(); y < column.rockTop(); y++) {
                pos.set(x, y, z);
                BlockState old = area.getBlockState(pos);
                if (isTerrain(old)) {
                    BlockState material = rockTerrainBlock(column, y);
                    if (old != material) area.setBlock(pos, material, 2);
                }
            }
            if (column.water() == 0) continue;
            if (column.waterfall()) {
                BlockState falling = waterfallBlock();
                for (int y = column.water() - 1; y >= field.cloudTop(x, z); y--) {
                    pos.set(x, y, z);
                    BlockState old = area.getBlockState(pos);
                    if (old.blocksMotion()) break;
                    if ((old.isAir() || old.is(Blocks.WATER)) && old != falling) area.setBlock(pos, falling, 2);
                }
                continue;
            }
            if (!column.land()) continue;
            int floor = column.top() - 1;
            if (floor >= column.water() - 1 || !area.getBlockState(pos.set(x, floor, z)).blocksMotion()) continue;
            for (int y = floor + 1; y < column.water(); y++) {
                pos.set(x, y, z);
                if (area.getBlockState(pos).isAir()) area.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
            }
        }
        return true;
    }

    public static BlockState terrainBlock(SkyLandformDensity.Column column, int y) {
        var rock = ERTerrainBlocks.FLOATING_ISLAND_ROCK.get().defaultBlockState();
        if (column.water() > column.top()) {
            boolean lake = column.biome() == 2 || column.biome() == 3 || column.biome() == 5;
            return lake && column.water() - column.top() <= 4 * column.verticalScale()
                    && y >= column.top() - 2 * column.verticalScale() && y > column.bottom()
                    ? ERTerrainBlocks.COAST.sand().get().defaultBlockState() : rock;
        }
        return dryTerrainBlock(column.top(), column.dirtBottom(), column.cloudBottom(), column.smallIslandRim(), y);
    }

    public static BlockState waterfallBlock() {
        return Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 8);
    }

    public static BlockState rockTerrainBlock(SkyLandformDensity.Column column, int y) {
        int dirtBottom = Math.max(column.rockBottom() + column.scaledDepth(2), column.rockTop() - column.scaledDepth(3));
        int cloudBottom = Math.max(column.rockBottom() + column.scaledDepth(1), dirtBottom - column.scaledDepth(2));
        return dryTerrainBlock(column.rockTop(), dirtBottom, cloudBottom, column.rockRim(), y);
    }

    private static BlockState dryTerrainBlock(int top, int dirtBottom, int cloudBottom, boolean smallRim, int y) {
        if (y == top - 1) return smallRim
                ? ERTerrainBlocks.GRASS_COVERED_FLOATING_ISLAND_ROCK.get().defaultBlockState()
                : ERTerrainBlocks.EDEN_GRASS_BLOCK.get().defaultBlockState();
        if (smallRim) return ERTerrainBlocks.FLOATING_ISLAND_ROCK.get().defaultBlockState();
        if (y >= dirtBottom) return ERTerrainBlocks.EDEN_DIRT.get().defaultBlockState();
        if (y >= cloudBottom) return ERTerrainBlocks.THIN_CLOUD_SOIL.get().defaultBlockState();
        return ERTerrainBlocks.FLOATING_ISLAND_ROCK.get().defaultBlockState();
    }

    private static boolean isTerrain(BlockState state) {
        return state.is(ERTerrainBlocks.FLOATING_ISLAND_ROCK.get())
                || state.is(ERTerrainBlocks.EDEN_GRASS_BLOCK.get())
                || state.is(ERTerrainBlocks.EDEN_DIRT.get())
                || state.is(ERTerrainBlocks.THIN_CLOUD_SOIL.get())
                || state.is(ERTerrainBlocks.GRASS_COVERED_FLOATING_ISLAND_ROCK.get());
    }
}
