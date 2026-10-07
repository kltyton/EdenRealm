package com.kltyton.eden_realm.world.terrain;

import com.kltyton.eden_realm.registry.content.block.ERSkyBlocks;
import com.kltyton.eden_realm.world.feature.SkySurfaceWaterFeature;
import io.github.kltyton.kltytonui.chunkmap.ChunkMapSnapshot;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

/** Coarse sky overview using the production island columns; native chunks supply block detail. */
final class SkyTerrainPreview {
    private SkyTerrainPreview() { }
    static TerrainQuickPreview.Sample create(TerrainPreviewField session, SkyLandformDensity field, String biomeId,
                                               TerrainPack pack, int chunks, BlockPos knownCenter, int resolution,
                                               BooleanSupplier cancelled) {
        BlockPos center = knownCenter;
        if (center == null) {
            var profile = pack.biomes().get(biomeId);
            var locator = profile.generationChancePercent() == 0
                    ? session.prepareSky(pack.withProfile(biomeId, profile.withGenerationChance(100))).join() : field;
            center = locator.findReferenceCenter(biomeId, BlockPos.ZERO)
                    .orElseThrow(() -> new IllegalArgumentException("Sky biome not found: " + biomeId));
        }
        long extent = chunks * 16L;
        int scale = (int) Math.max(1, Math.ceilDiv(extent, resolution));
        int side = (int) Math.ceilDiv(extent, scale);
        int ox = (int) Math.floorDiv(Math.floorDiv(center.getX(), 16) * 16L - extent / 2, scale);
        int oz = (int) Math.floorDiv(Math.floorDiv(center.getZ(), 16) * 16L - extent / 2, scale);
        var snapshot = ChunkMapSnapshot.builder(ox, oz, side, side, scale, SkyLandformDensity.MIN_Y, field.maxY());
        for (int z = 0; z < side; z++) {
            if (cancelled.getAsBoolean()) throw new CancellationException();
            for (int x = 0; x < side; x++) {
                int wx = (ox + x) * scale, wz = (oz + z) * scale;
                var value = field.sample(wx, wz);
                snapshot.addRun(x, z, -64, field.cloudTop(wx, wz), ERSkyBlocks.CLOUD.get().defaultBlockState());
                if (value.land()) {
                    for (var span : value.solidSpans()) {
                        int start = span.fromY();
                        var material = SkySurfaceWaterFeature.terrainBlock(value, start);
                        int[] boundaries = {value.cloudBottom(), value.dirtBottom(), value.top() - value.scaledDepth(2),
                                value.top() - 1, span.toY()};
                        Arrays.sort(boundaries);
                        for (int y : boundaries) {
                            if (y <= start || y > span.toY()) continue;
                            var next = y == span.toY() ? Blocks.AIR.defaultBlockState()
                                    : SkySurfaceWaterFeature.terrainBlock(value, y);
                            if (next != material) {
                                snapshot.addRun(x, z, start, y, material);
                                start = y;
                                material = next;
                            }
                        }
                    }
                    if (value.water() > value.top()) snapshot.addRun(x, z, value.top(), value.water(), Blocks.WATER.defaultBlockState());
                } else if (value.waterfall()) snapshot.addRun(x, z, field.cloudTop(wx, wz),
                        value.water(), SkySurfaceWaterFeature.waterfallBlock());
                if (value.rockTop() > value.rockBottom()) {
                    int start = value.rockBottom();
                    var material = SkySurfaceWaterFeature.rockTerrainBlock(value, start);
                    int[] boundaries = {value.rockBottom() + value.scaledDepth(1), value.rockBottom() + value.scaledDepth(2),
                            value.rockTop() - value.scaledDepth(5), value.rockTop() - value.scaledDepth(3),
                            value.rockTop() - 1, value.rockTop()};
                    Arrays.sort(boundaries);
                    for (int y : boundaries) {
                        if (y <= start) continue;
                        var next = y == value.rockTop() ? Blocks.AIR.defaultBlockState()
                                : SkySurfaceWaterFeature.rockTerrainBlock(value, y);
                        if (next != material) {
                            snapshot.addRun(x, z, start, y, material);
                            start = y;
                            material = next;
                        }
                    }
                }
                snapshot.setBiome(x, z, session.skyBiomes.get(value.biomeIndex()), wx, wz);
            }
        }
        return new TerrainQuickPreview.Sample(snapshot.build(), center);
    }
}
