package com.kltyton.eden_realm.client.world.preview;

import com.kltyton.eden_realm.world.terrain.TerrainPack;
import com.kltyton.eden_realm.client.world.preview.render.NativeTerrainScene;
import com.kltyton.eden_realm.client.world.preview.procedural.ProceduralPreview;
import io.github.kltyton.kltytonui.chunkmap.ChunkMapSnapshot;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.material.MapColor;

/** Converts a small detached heightfield without waiting for model or disk-cache work. */
public final class TerrainLiveSurface {
    private TerrainLiveSurface() { }

    public static ProceduralPreview.Frame frame(ChunkMapSnapshot snapshot, TerrainPack pack, long generation) {
        int count = snapshot.width() * snapshot.depth();
        int[] heights = new int[count];
        byte[] rgba = new byte[count * 4];
        boolean[] water = new boolean[count];
        for (int z = 0; z < snapshot.depth(); z++) {
            for (int x = 0; x < snapshot.width(); x++) {
                int index = z * snapshot.width() + x;
                var column = snapshot.column(x, z);
                heights[index] = (column.isEmpty() ? snapshot.minY() : column.getLast().toY()) * snapshot.verticalStep() - 1;
                if (column.isEmpty()) continue;
                var state = column.getLast().state();
                var mapColor = state.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
                int color = mapColor.col;
                var tint = snapshot.biome(x, z);
                water[index] = !state.getFluidState().isEmpty();
                if (tint != null && mapColor == MapColor.GRASS) color = tint.grass();
                else if (tint != null && water[index]) color = tint.water();
                rgba[index * 4] = (byte) (color >> 16);
                rgba[index * 4 + 1] = (byte) (color >> 8);
                rgba[index * 4 + 2] = (byte) color;
                rgba[index * 4 + 3] = (byte) 255;
            }
        }
        // A coarse coastal cell represents the average coverage of its narrow ice strip.
        for (int z = 0; z < snapshot.depth(); z++) {
            for (int x = 0; x < snapshot.width(); x++) {
                int index = z * snapshot.width() + x;
                if (!water[index]) continue;
                var tint = snapshot.biome(x, z);
                if (tint == null) continue;
                int radius = pack.biomes().get(tint.path()).shoreIceBlocks();
                if (snapshot.step() <= radius * 2 || radius == 0) continue;
                boolean coast = x > 0 && !water[index - 1]
                        || x + 1 < snapshot.width() && !water[index + 1]
                        || z > 0 && !water[index - snapshot.width()]
                        || z + 1 < snapshot.depth() && !water[index + snapshot.width()];
                if (!coast) continue;
                var column = snapshot.column(x, z);
                var fluid = column.getLast();
                if ((fluid.toY() - fluid.fromY()) * snapshot.verticalStep() > 16) continue;
                float fraction = Math.min(1, radius / (float) snapshot.step());
                int ice = MapColor.ICE.col;
                for (int channel = 0; channel < 3; channel++) {
                    int original = Byte.toUnsignedInt(rgba[index * 4 + channel]);
                    int frozen = ice >> (16 - channel * 8) & 255;
                    rgba[index * 4 + channel] = (byte) Math.round(original + (frozen - original) * fraction);
                }
            }
        }
        long originX = snapshot.originX() * (long) snapshot.step();
        long originZ = snapshot.originZ() * (long) snapshot.step();
        var surface = new NativeTerrainScene.Surface(originX, originZ, snapshot.width(), snapshot.depth(),
                snapshot.step(), rgba, heights, generation);
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var values = ByteBuffer.allocate(32 + count * 4);
            values.putLong(originX).putLong(originZ).putInt(snapshot.width()).putInt(snapshot.depth())
                    .putInt(snapshot.step()).putInt(snapshot.verticalStep());
            for (int height : heights) values.putInt(height);
            digest.update(values.array());
            digest.update(rgba);
            return new ProceduralPreview.Frame(generation, surface, null, Map.of(), null,
                    HexFormat.of().formatHex(digest.digest()));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
