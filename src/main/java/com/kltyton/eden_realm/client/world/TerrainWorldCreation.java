package com.kltyton.eden_realm.client.world;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.data.worldgen.IcyBiomeWorldgen;
import com.kltyton.eden_realm.data.worldgen.SkyBiomeWorldgen;
import com.kltyton.eden_realm.world.terrain.TerrainPack;
import com.kltyton.eden_realm.world.terrain.TerrainPackFiles;
import com.kltyton.eden_realm.world.terrain.TerrainPreviewChunks;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.WorldDataConfiguration;
import java.io.IOException;
import java.io.UncheckedIOException;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.levelgen.WorldDimensions;

/** Owns draft terrain profiles while a create-world screen is open. */
public final class TerrainWorldCreation {
    private static final ResourceKey<LevelStem> DIMENSION = ResourceKey.create(
            Registries.LEVEL_STEM, ERConstants.id("eden_layer"));
    private static final Map<CreateWorldScreen, TerrainPack> DRAFTS = new WeakHashMap<>();
    private static final String WORLD_PACK = "eden-realm-terrain.zip";
    private record Installation(TerrainPack pack, WorldCreationContext context) { }
    private static final Map<CreateWorldScreen, Installation> RELOADING = new WeakHashMap<>();
    private static final Map<CreateWorldScreen, Installation> INSTALLED = new WeakHashMap<>();
    private static CreateWorldScreen previewOwner;
    private static TerrainEditorController warmedPreview;

    private TerrainWorldCreation() {
    }

    public static void prewarm(CreateWorldScreen screen) {
        if (previewOwner != screen) {
            com.kltyton.eden_realm.client.world.preview.TerrainRealtimeRenderer.prewarm();
            if (warmedPreview != null) warmedPreview.close();
            previewOwner = screen;
            warmedPreview = new TerrainEditorController(screen);
        }
        warmedPreview.tick();
    }

    public static void tickPreview(CreateWorldScreen screen) {
        if (previewOwner == screen && warmedPreview != null) warmedPreview.tick();
    }

    public static TerrainEditorController takePreview(CreateWorldScreen screen) {
        DRAFTS.remove(screen);
        TerrainPack saved = draft(screen);
        if (previewOwner == screen && warmedPreview != null
                && !warmedPreview.pack().withSeed(0).equals(saved.withSeed(0))) releasePreview(screen);
        prewarm(screen);
        TerrainEditorController result = warmedPreview;
        warmedPreview = null;
        previewOwner = null;
        return result;
    }

    public static void releasePreview(CreateWorldScreen screen) {
        if (previewOwner != screen) return;
        warmedPreview.close();
        warmedPreview = null;
        previewOwner = null;
    }

    public static TerrainPack draft(CreateWorldScreen screen) {
        return DRAFTS.computeIfAbsent(screen, owner -> {
            TerrainPack defaults = official(owner);
            if (!Files.exists(savedFile())) return defaults;
            try {
                TerrainPack saved = TerrainPackFiles.readDataPack(savedFile());
                if (!saved.target().equals(defaults.target()) || !defaults.biomes().keySet().containsAll(saved.biomes().keySet()))
                    throw new IOException("Saved terrain pack does not match this dimension's biomes");
                var profiles = new LinkedHashMap<>(defaults.biomes());
                profiles.putAll(saved.biomes());
                return new TerrainPack(saved.format(), saved.version(), saved.target(), defaults.seed(), Map.copyOf(profiles));
            } catch (IOException | IllegalArgumentException exception) {
                throw new UncheckedIOException("Cannot read saved terrain draft: " + savedFile(),
                        exception instanceof IOException io ? io : new IOException(exception));
            }
        });
    }

    private static Path savedFile() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("cache/eden_realm/terrain-packs/draft.zip");
    }

    public static CompletableFuture<Path> save(CreateWorldScreen screen, TerrainPack pack) {
        return export(screen, pack, savedFile()).thenApplyAsync(file -> {
            DRAFTS.put(screen, pack);
            INSTALLED.remove(screen);
            return file;
        }, Minecraft.getInstance());
    }

    public static CompletableFuture<Path> export(CreateWorldScreen screen, TerrainPack snapshot, Path file) {
        TerrainPack pack = snapshot.withSeed(screen.getUiState().getSettings().options().seed());
        var registries = screen.getUiState().getSettings().worldgenLoadContext();
        return CompletableFuture.supplyAsync(() -> {
            try { TerrainPackFiles.writeDataPack(file, pack, registries); }
            catch (IOException exception) { throw new UncheckedIOException("Cannot export terrain data pack: " + file, exception); }
            return file;
        }, Util.ioPool());
    }

    /** Installs the saved draft through vanilla's pack validation before creating any chunks. */
    public static boolean prepareCreation(CreateWorldScreen screen) {
        if (!Files.exists(savedFile())) return true;
        var context = screen.getUiState().getSettings();
        var current = draft(screen).withSeed(context.options().seed());
        Installation installed = INSTALLED.get(screen);
        if (installed != null && installed.pack().equals(current) && installed.context() == context) return true;
        if (RELOADING.containsKey(screen)) return false;
        var game = Minecraft.getInstance();
        var settings = screen.getDataPackSelectionSettings(context.dataConfiguration());
        if (settings == null) return false;
        Installation pending = new Installation(current, context);
        RELOADING.put(screen, pending);
        var waiting = new GenericMessageScreen(Component.translatable("screen.eden_realm.terrain.installing"));
        game.setScreenAndShow(waiting);
        export(screen, current, settings.getFirst().resolve(WORLD_PACK)).whenComplete((file, failure) -> game.execute(() -> {
            if (RELOADING.get(screen) != pending || game.gui.screen() != waiting) return;
            if (failure != null) {
                RELOADING.remove(screen);
                com.kltyton.eden_realm.EdenRealm.LOGGER.error("Cannot install saved terrain data pack", failure);
                game.gui.setScreen(screen);
                SystemToast.onPackCopyFailure(game, screen.getUiState().getTargetFolder());
                return;
            }
            var repository = settings.getSecond();
            repository.reload();
            String id = "file/" + WORLD_PACK;
            var enabled = new java.util.ArrayList<>(context.dataConfiguration().dataPacks().getEnabled());
            enabled.remove(id);
            enabled.add(id);
            repository.setSelected(enabled);
            var disabled = repository.getAvailableIds().stream().filter(pack -> !enabled.contains(pack)).toList();
            var config = new WorldDataConfiguration(new DataPackConfig(java.util.List.copyOf(enabled), disabled),
                    context.dataConfiguration().enabledFeatures());
            screen.applyNewPackConfig(repository, config, ignored -> {
                RELOADING.remove(screen);
                game.gui.setScreen(screen);
                SystemToast.onPackCopyFailure(game, screen.getUiState().getTargetFolder());
            });
        }));
        return false;
    }

    public static void tickCreation(CreateWorldScreen screen) {
        Installation pending = RELOADING.get(screen);
        var context = screen.getUiState().getSettings();
        if (pending == null || context == pending.context() || Minecraft.getInstance().gui.screen() != screen) return;
        RELOADING.remove(screen);
        INSTALLED.put(screen, new Installation(pending.pack(), context));
        screen.onCreate();
    }

    public static void apply(CreateWorldScreen screen) {
        if (!DRAFTS.containsKey(screen)
                && screen.getUiState().getSettings().selectedDimensions().get(DIMENSION).isPresent()
                && screen.getUiState().getSettings().selectedDimensions().get(SkyBiomeWorldgen.DIMENSION).isPresent()) return;
        TerrainPack pack = draft(screen).withSeed(screen.getUiState().getSettings().options().seed());
        screen.getUiState().updateDimensions((registries, dimensions) -> {
            Map<ResourceKey<LevelStem>, LevelStem> stems = new LinkedHashMap<>(dimensions.dimensions());
            stems.put(DIMENSION, IcyBiomeWorldgen.tunedDimension(registries, pack));
            stems.put(SkyBiomeWorldgen.DIMENSION, SkyBiomeWorldgen.tunedDimension(registries, pack));
            return new WorldDimensions(Map.copyOf(stems));
        });
    }

    public static TerrainPreviewChunks.PreviewRequest preview(CreateWorldScreen screen, String biomeId,
                                                               int previewChunks) {
        TerrainPack pack = draft(screen).withSeed(com.kltyton.eden_realm.config.ERClientConfig.TERRAIN_PREVIEW_SEED.get());
        var registries = screen.getUiState().getSettings().worldgenLoadContext();
        return TerrainPreviewChunks.generate(registries, biomeId, pack, previewChunks);
    }

    public static void writeWorldProfile(CreateWorldScreen screen,
                                         LevelStorageSource.LevelStorageAccess access) {
        TerrainPack pack = DRAFTS.get(screen);
        if (pack == null) return;
        var file = access.getLevelPath(LevelResource.ROOT).resolve("eden_realm/terrain-pack.json");
        try {
            TerrainPackFiles.write(file, pack.withSeed(screen.getUiState().getSettings().options().seed()));
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot save world terrain profiles: " + file, exception);
        }
    }

    private static TerrainPack official(CreateWorldScreen screen) {
        var context = screen.getUiState().getSettings();
        var ids = context.worldgenLoadContext().lookupOrThrow(Registries.BIOME).listElements()
                .map(holder -> holder.key().identifier())
                .filter(id -> id.getNamespace().equals(ERConstants.MOD_ID))
                .map(id -> id.getPath()).toList();
        return TerrainPack.official(context.options().seed(), ids);
    }
}
