package com.kltyton.eden_realm.client.world.preview.cache;

import com.kltyton.eden_realm.client.world.preview.model.ChunkTiles;

import com.mojang.logging.LogUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.regex.Pattern;

/** Persistent cache for native tile geometry and height data. */
public final class NativeTileCache {
    private static final int MAGIC = 0x4155494E;
    private static final int VERSION = 1;
    private static final int HEIGHT_COUNT = 32 * 32;
    private static final int MAX_MODEL_BYTES = 64 * 1024 * 1024;
    private static final long FIXED_FILE_BYTES = Integer.BYTES * 5L + HEIGHT_COUNT * Integer.BYTES;
    private static final Pattern SHA256_KEY = Pattern.compile("[0-9a-f]{64}");

    private final Path directory;

    public NativeTileCache(Path directory) {
        this.directory = directory.toAbsolutePath().normalize();
    }

    public Optional<ChunkTiles.NativeTile> read(String sha256Key, ChunkTiles.Tile expected) {
        validateKey(sha256Key);
        if (expected == null) throw new NullPointerException("expected");

        Path file = fileFor(sha256Key);
        try {
            if (!Files.isRegularFile(file)) return Optional.empty();
            long fileSize = Files.size(file);
            if (fileSize < FIXED_FILE_BYTES || fileSize > FIXED_FILE_BYTES + MAX_MODEL_BYTES) {
                return miss(sha256Key, "invalid file size");
            }

            try (var input = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
                if (input.readInt() != MAGIC || input.readInt() != VERSION) {
                    return miss(sha256Key, "invalid header");
                }

                int x = input.readInt();
                int z = input.readInt();
                int modelLength = input.readInt();
                if (x != expected.x() || z != expected.z()
                        || modelLength < 0 || modelLength > MAX_MODEL_BYTES
                        || fileSize != FIXED_FILE_BYTES + modelLength) {
                    return miss(sha256Key, "tile or payload mismatch");
                }

                byte[] model = new byte[modelLength];
                input.readFully(model);
                int[] heights = new int[HEIGHT_COUNT];
                for (int i = 0; i < heights.length; i++) heights[i] = input.readInt();
                if (input.read() != -1) return miss(sha256Key, "trailing data");

                return Optional.of(new ChunkTiles.NativeTile(expected, model, heights));
            }
        } catch (IOException | SecurityException failure) {
            LogUtils.getLogger().debug("Native tile cache miss for {}: {}", sha256Key, failure.toString());
            return Optional.empty();
        }
    }

    public void write(String sha256Key, ChunkTiles.NativeTile tile) {
        validateKey(sha256Key);
        if (tile == null) throw new NullPointerException("tile");
        if (tile.key() == null) throw new IllegalArgumentException("tile key is required");
        if (tile.model() == null || tile.model().length > MAX_MODEL_BYTES) {
            throw new IllegalArgumentException("model must be at most 64 MiB");
        }
        if (tile.heights() == null || tile.heights().length != HEIGHT_COUNT) {
            throw new IllegalArgumentException("tile must contain exactly 1024 heights");
        }

        Path temporary = null;
        try {
            Files.createDirectories(directory);
            temporary = Files.createTempFile(directory, ".native-tile-", ".tmp");
            try (var output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temporary)))) {
                output.writeInt(MAGIC);
                output.writeInt(VERSION);
                output.writeInt(tile.key().x());
                output.writeInt(tile.key().z());
                output.writeInt(tile.model().length);
                output.write(tile.model());
                for (int height : tile.heights()) output.writeInt(height);
            }

            Files.move(temporary, fileFor(sha256Key), StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            temporary = null;
        } catch (IOException | SecurityException failure) {
            LogUtils.getLogger().debug("Native tile cache write failed for {}: {}", sha256Key, failure.toString());
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException | SecurityException cleanupFailure) {
                    LogUtils.getLogger().debug("Native tile cache temp cleanup failed: {}", cleanupFailure.toString());
                }
            }
        }
    }

    private Path fileFor(String sha256Key) {
        return directory.resolve(sha256Key + ".tile");
    }

    private static void validateKey(String sha256Key) {
        if (sha256Key == null || !SHA256_KEY.matcher(sha256Key).matches()) {
            throw new IllegalArgumentException("cache key must be 64 lowercase hexadecimal characters");
        }
    }

    private static Optional<ChunkTiles.NativeTile> miss(String key, String reason) {
        LogUtils.getLogger().debug("Native tile cache miss for {}: {}", key, reason);
        return Optional.empty();
    }
}
