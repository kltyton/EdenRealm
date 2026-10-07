package com.kltyton.eden_realm.world.terrain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.kltyton.eden_realm.data.worldgen.IcyBiomeWorldgen;
import com.kltyton.eden_realm.data.worldgen.SkyBiomeWorldgen;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.util.InclusiveRange;
import net.minecraft.world.level.dimension.LevelStem;

/** Reads and exports editable terrain profiles without owning the destination directory. */
public final class TerrainPackFiles {
    private TerrainPackFiles() {
    }

    public static TerrainPack read(Path file) throws IOException {
        return TerrainPack.fromJson(Files.readString(file, StandardCharsets.UTF_8));
    }

    public static void write(Path file, TerrainPack pack) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, pack.toJson(), StandardCharsets.UTF_8);
    }

    public static TerrainPack readDataPack(Path file) throws IOException {
        try (var zip = new ZipFile(file.toFile(), StandardCharsets.UTF_8)) {
            var profile = zip.getEntry("data/eden_realm/terrain_profiles/terrain-pack.json");
            if (profile == null) throw new IOException("Missing terrain profiles in " + file);
            try (var input = zip.getInputStream(profile)) {
                return TerrainPack.fromJson(new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }

    public static void writeDataPack(Path file, TerrainPack pack, RegistryAccess.Frozen registries) throws IOException {
        var version = SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA);
        var metadata = new PackMetadataSection(Component.translatable("pack.eden_realm.terrain"), new InclusiveRange<>(version));
        var root = new JsonObject();
        root.add("pack", PackMetadataSection.SERVER_TYPE.codec().encodeStart(JsonOps.INSTANCE, metadata).getOrThrow());
        var dimension = LevelStem.CODEC.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE),
                IcyBiomeWorldgen.tunedDimension(registries, pack)).getOrThrow();
        var skyDimension = LevelStem.CODEC.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE),
                SkyBiomeWorldgen.tunedDimension(registries, pack)).getOrThrow();
        file = file.toAbsolutePath();
        Files.createDirectories(file.getParent());
        Path pending = file.resolveSibling(file.getFileName() + "." + UUID.randomUUID() + ".tmp");
        try {
            writeZip(pending, root.toString(), dimension.toString(), skyDimension.toString(), pack);
            try {
                Files.move(pending, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(pending, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            try { Files.deleteIfExists(pending); }
            catch (IOException cleanup) { exception.addSuppressed(cleanup); }
            throw exception;
        }
    }

    private static void writeZip(Path file, String metadata, String dimension, String skyDimension, TerrainPack pack) throws IOException {
        var output = Files.newOutputStream(file, StandardOpenOption.CREATE_NEW);
        try (var zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            entry(zip, "pack.mcmeta", metadata);
            entry(zip, "data/eden_realm/dimension/eden_layer.json", dimension);
            entry(zip, "data/eden_realm/dimension/sky_layer.json", skyDimension);
            entry(zip, "data/eden_realm/terrain_profiles/terrain-pack.json", pack.toJson());
        } catch (IOException exception) {
            try { Files.deleteIfExists(file); }
            catch (IOException cleanup) { exception.addSuppressed(cleanup); }
            throw exception;
        }
    }

    private static void entry(ZipOutputStream zip, String name, String json) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(json.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
