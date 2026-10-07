package com.kltyton.eden_realm.world.terrain;

import com.kltyton.eden_realm.ERConstants;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.event.level.LevelEvent;

/** Immutable per-world profile snapshots published before chunk decoration begins. */
public final class TerrainWorldProfiles {
    private static final Map<ServerLevel, TerrainPack> ACTIVE = new ConcurrentHashMap<>();

    private TerrainWorldProfiles() {
    }

    public static void onLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || (!level.dimension().identifier().equals(ERConstants.id("eden_layer"))
                && !level.dimension().identifier().equals(ERConstants.id("sky_layer")))) return;
        var resource = level.getServer().getResourceManager().getResource(ERConstants.id("terrain_profiles/terrain-pack.json"));
        if (resource.isPresent()) {
            try (var reader = resource.get().openAsReader()) {
                ACTIVE.put(level, TerrainPack.fromJson(reader.lines().collect(java.util.stream.Collectors.joining("\n"))));
            } catch (IOException exception) {
                throw new UncheckedIOException("Cannot read terrain profiles from the world's data pack", exception);
            }
            return;
        }
        Path file = level.getServer().getWorldPath(LevelResource.ROOT)
                .resolve("eden_realm/terrain-pack.json");
        if (Files.exists(file)) {
            try {
                ACTIVE.put(level, TerrainPackFiles.read(file));
            } catch (IOException exception) {
                throw new UncheckedIOException("Cannot read world terrain profiles: " + file, exception);
            }
        }
    }

    public static void onUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) ACTIVE.remove(level);
    }

    public static TerrainProfile profile(WorldGenLevel worldgen, Holder<Biome> biome) {
        ServerLevel level = worldgen instanceof WorldGenRegion region ? region.getLevel()
                : worldgen instanceof ServerLevel server ? server : null;
        TerrainPack pack = level == null ? null : ACTIVE.get(level);
        String id = biome.unwrapKey().map(key -> key.identifier().getPath()).orElse("");
        return pack == null ? TerrainProfile.official(id)
                : pack.biomes().getOrDefault(id, TerrainProfile.official(id));
    }
}
