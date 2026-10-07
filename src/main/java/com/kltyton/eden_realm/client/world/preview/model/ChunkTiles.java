package com.kltyton.eden_realm.client.world.preview.model;

/** Immutable encoded tiles shared by procedural capture, caching and drawing. */
public final class ChunkTiles {
    public record Tile(int x, int z) { }
    public record NativeTile(Tile key, byte[] model, int[] heights) { }
    private ChunkTiles() { }
}
