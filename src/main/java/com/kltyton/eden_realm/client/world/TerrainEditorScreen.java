package com.kltyton.eden_realm.client.world;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kltyton.eden_realm.EdenRealm;
import com.kltyton.eden_realm.world.terrain.TerrainPack;
import com.kltyton.eden_realm.world.terrain.TerrainProfile;
import com.kltyton.eden_realm.client.world.preview.TerrainRealtimeRenderer;
import com.kltyton.eden_realm.client.world.preview.render.MapCamera;
import com.kltyton.eden_realm.client.world.preview.render.NativeTerrainScene;
import com.kltyton.eden_realm.client.world.preview.render.BlockSurfaceRenderer;
import com.kltyton.eden_realm.client.world.preview.procedural.ProceduralPreview;
import io.github.kltyton.kltytonui.client.gui.KuiNativeViewport;
import io.github.kltyton.kltytonui.client.gui.pip.KltytonUiPipRenderState;
import io.github.kltyton.kltytonui.init.Document;
import io.github.kltyton.kltytonui.init.Element;
import io.github.kltyton.kltytonui.screen.KltytonScreen;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.util.Objects;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.minecraft.util.Util;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;

/** The KUI workbench keeps the create-world draft alive while editing one biome. */
public final class TerrainEditorScreen extends KltytonScreen {
    private static final String PAGE = "eden_realm/terrain-shell.html";
    private static final Gson JSON = new Gson();
    private final CreateWorldScreen parent;
    private final TerrainEditorController controller;
    private final TerrainWorkbenchServer workbench;
    private NativeTerrainScene nativeRenderer;
    private BlockSurfaceRenderer blockRenderer;
    private TerrainRealtimeRenderer realtimeRenderer;
    private boolean displayedRealtime;
    private boolean displayedWhole;
    private ProceduralPreview.Frame displayedFrame;
    private MapCamera displayedCamera;
    private String publishedState;
    private String feedback = "";
    private boolean fileOperation;
    private final JsonObject acknowledged = new JsonObject();
    private long publishedVersion = -1;
    private long publishedSeed;
    private String publishedFeedback;
    private double viewportX, viewportY, viewportWidth, viewportHeight;
    private record TooltipRegion(double x, double y, double width, double height) { }
    private List<TooltipRegion> tooltipRegions = List.of();
    private double cameraX, cameraY, cameraZ, pixelsPerBlock = 1, yaw = 0.75, angle = 0.7;
    private NativeTerrainScene.Surface cameraSurface;
    private int cameraSurfaceMinY, cameraSurfaceMaxY;
    private boolean resetView = true, closed;
    private long nativeFrames, publishedNativeFrames = -1, displayedSceneKey = -1;
    private long pendingInputAt;
    private long inputResponseAt;
    private double inputToRenderMillis;
    private long lastNativeFrameAt;
    private double nativeFrameIntervalMillis;

    public static void open(CreateWorldScreen parent) {
        Minecraft.getInstance().gui.setScreen(new TerrainEditorScreen(parent));
    }

    private TerrainEditorScreen(CreateWorldScreen parent) {
        super(PAGE);
        this.parent = parent;
        this.controller = TerrainWorldCreation.takePreview(parent);
        try {
            this.workbench = new TerrainWorkbenchServer(Minecraft.getInstance());
        } catch (IOException exception) {
            controller.close();
            throw new java.io.UncheckedIOException(exception);
        }
        setPauseGame(true);
    }

    @Override
    protected void init() {
        super.init();
        resetView = true;
        publishState();
        installWorkbench();
    }

    @Override
    public void tick() {
        super.tick();
        installWorkbench();
        drainCommands();
        if (minecraft.gui.screen() != this) return;
        controller.tick();
        if (controller.previewSeed() != publishedSeed) resetView = true;
        publishState();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        drainCommands();
        if (minecraft.gui.screen() != this) return;
        controller.frame();
        publishState();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        extractPreview(graphics);
        extractTooltips(graphics);
    }

    private void drainCommands() {
        String raw;
        while ((raw = workbench.pollCommands()) != null) {
            try {
                for (JsonElement entry : JsonParser.parseString(raw).getAsJsonArray()) {
                    handleCommand(entry.getAsJsonObject());
                    if (minecraft.gui.screen() != this) return;
                }
            } catch (RuntimeException exception) {
                feedback = Component.translatable("screen.eden_realm.terrain.error",
                        exception.getMessage()).getString();
                EdenRealm.LOGGER.warn("Terrain workbench command failed", exception);
            }
        }
    }

    @Override
    public void onClose() {
        if (fileOperation) return;
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void removed() {
        closed = true;
        if (nativeRenderer != null) nativeRenderer.close();
        if (blockRenderer != null) blockRenderer.close();
        if (realtimeRenderer != null) realtimeRenderer.close();
        controller.close();
        workbench.close();
        super.removed();
    }

    private void installWorkbench() {
        Document document = getLinkedDocument();
        Element frame = document == null ? null : document.getElementById("terrain-browser");
        if (frame != null && !frame.hasAttribute("src")) frame.setAttribute("src", workbench.url());
    }

    private void handleCommand(JsonObject command) {
        String action = command.get("action").getAsString();
        if (fileOperation && !action.equals("viewport") && !action.equals("camera") && !action.equals("tooltips")) return;
        long previousGeneration = controller.generation();
        if (command.has("sequence")) {
            acknowledged.addProperty(action.equals("set") ? command.get("field").getAsString() : action,
                    command.get("sequence").getAsLong());
        }
        switch (action) {
            case "viewport" -> updateViewport(command);
            case "tooltips" -> updateTooltips(command);
            case "camera" -> updateCamera(command);
            case "select" -> {
                controller.select(command.get("biome").getAsString());
                resetView = true;
            }
            case "set" -> updateField(command.get("field").getAsString(), command.get("value").getAsInt());
            case "reset" -> controller.reset();
            case "export" -> exportPack();
            case "save" -> savePack();
            case "chunks" -> {
                controller.setPreviewChunks(command.get("value").getAsInt());
                resetView = true;
            }
            case "view" -> controller.view(command.get("sceneKey").getAsLong(), command.get("x").getAsDouble(),
                    command.get("z").getAsDouble(), command.get("span").getAsDouble());
            case "close" -> onClose();
            default -> throw new IllegalArgumentException("Unknown terrain action: " + action);
        }
        if (command.has("inputAt") && (controller.generation() != previousGeneration || action.equals("camera"))) {
            pendingInputAt = command.get("inputAt").getAsLong();
        }
    }

    private void updateViewport(JsonObject command) {
        viewportX = finite(command, "x", 0);
        viewportY = finite(command, "y", 0);
        viewportWidth = Math.max(0, finite(command, "width", 0));
        viewportHeight = Math.max(0, finite(command, "height", 0));
        publishedState = null;
    }

    private void updateTooltips(JsonObject command) {
        List<TooltipRegion> regions = new ArrayList<>();
        for (JsonElement entry : command.getAsJsonArray("regions")) {
            JsonObject region = entry.getAsJsonObject();
            regions.add(new TooltipRegion(finite(region, "x", 0), finite(region, "y", 0),
                    Math.max(0, finite(region, "width", 0)), Math.max(0, finite(region, "height", 0))));
        }
        tooltipRegions = List.copyOf(regions);
    }

    private void extractTooltips(GuiGraphicsExtractor graphics) {
        if (tooltipRegions.isEmpty()) return;
        graphics.nextStratum();
        var submitted = new HashSet<ScreenRectangle>();
        for (TooltipRegion region : tooltipRegions) {
            int left = Math.max(0, (int) Math.floor(region.x() * width));
            int top = Math.max(0, (int) Math.floor(region.y() * height));
            int right = Math.min(width, (int) Math.ceil((region.x() + region.width()) * width));
            int bottom = Math.min(height, (int) Math.ceil((region.y() + region.height()) * height));
            if (left >= right || top >= bottom) continue;
            graphics.enableScissor(left, top, right, bottom);
            ScreenRectangle clip = graphics.peekScissorStack();
            if (submitted.add(clip)) graphics.submitPictureInPictureRenderState(KltytonUiPipRenderState.ui(
                    0, 0, width, height, clip));
            graphics.disableScissor();
        }
    }

    private void updateCamera(JsonObject command) {
        if (command.has("reset") && command.get("reset").getAsBoolean()) {
            yaw = finite(command, "yaw", 0.75);
            angle = Math.clamp(finite(command, "angle", 0.7), 0.05, 1.5);
            resetView = true;
        }
        yaw += finite(command, "rotationDelta", 0);
        angle = Math.clamp(angle + finite(command, "angleDelta", 0), 0.05, 1.5);
        pixelsPerBlock = Math.clamp(pixelsPerBlock / Math.clamp(finite(command, "zoomFactor", 1), 0.05, 20), 0.0001, 32);
        double dx = finite(command, "panX", 0) * viewportWidth * width / pixelsPerBlock;
        double dz = finite(command, "panZ", 0) * viewportHeight * height / (pixelsPerBlock * Math.cos(angle));
        cameraX -= dx * Math.cos(yaw) + dz * Math.sin(yaw);
        cameraZ -= dx * Math.sin(yaw) - dz * Math.cos(yaw);
        publishedState = null;
    }

    private static double finite(JsonObject object, String name, double fallback) {
        double value = object.has(name) ? object.get(name).getAsDouble() : fallback;
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite terrain " + name);
        return value;
    }
    private void updateField(String field, int value) {
        TerrainProfile old = controller.pack().biomes().get(controller.selectedBiome());
        int[] values = {old.elevationOffset(), old.reliefPercent(), old.spacingPercent(),
                old.shapePercent(), old.shoreIceBlocks(), old.generationChancePercent(),
                old.biomeSizePercent()};
        int index = switch (field) {
            case "elevationOffset" -> 0;
            case "reliefPercent" -> 1;
            case "spacingPercent" -> 2;
            case "shapePercent" -> 3;
            case "shoreIceBlocks" -> 4;
            case "generationChancePercent" -> 5;
            case "biomeSizePercent" -> 6;
            default -> throw new IllegalArgumentException("Unknown terrain field: " + field);
        };
        values[index] = value;
        TerrainProfile updated = new TerrainProfile(values[0], values[1], values[2], values[3],
                values[4], values[5], values[6]);
        TerrainProfile.CODEC.encodeStart(JsonOps.INSTANCE, updated).getOrThrow();
        controller.update(updated);
        feedback = "";
    }

    private void exportPack() {
        TerrainPack snapshot = controller.pack();
        String title = Component.translatable("screen.eden_realm.terrain.export").getString();
        String replace = Component.translatable("screen.eden_realm.terrain.replace").getString();
        Path defaultFile = minecraft.gameDirectory.toPath().toAbsolutePath().resolve("eden-realm-terrain.zip");
        beginFileOperation("exporting");
        CompletableFuture.supplyAsync(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                String selected = TinyFileDialogs.tinyfd_saveFileDialog(title, defaultFile.toString(),
                        stack.pointers(stack.UTF8("*.zip")), "ZIP data pack");
                if (selected == null) return null;
                Path file = Path.of(selected);
                if (!selected.toLowerCase(Locale.ROOT).endsWith(".zip")) {
                    file = Path.of(selected + ".zip");
                    if (Files.exists(file) && TinyFileDialogs.tinyfd_messageBox(title,
                            replace + "\n" + file, "yesno", "question", 0) == 0) return null;
                }
                return file;
            }
        }, Util.ioPool()).whenComplete((destination, dialogFailure) -> minecraft.execute(() -> {
            if (dialogFailure != null || destination == null) {
                finishFileOperation(null, dialogFailure);
            } else {
                TerrainWorldCreation.export(parent, snapshot, destination).whenComplete((file, failure) ->
                        minecraft.execute(() -> finishFileOperation(file, failure)));
            }
        }));
    }

    private void savePack() {
        beginFileOperation("saving");
        TerrainWorldCreation.save(parent, controller.pack()).whenComplete((file, failure) -> minecraft.execute(() -> {
            fileOperation = false;
            if (failure == null) {
                if (minecraft.gui.screen() == this) onClose();
            } else finishFileOperation(null, failure);
        }));
    }

    private void beginFileOperation(String label) {
        fileOperation = true;
        feedback = Component.translatable("screen.eden_realm.terrain." + label).getString();
        publishedState = null;
        publishState();
    }

    private void finishFileOperation(Path file, Throwable failure) {
        fileOperation = false;
        if (failure != null) EdenRealm.LOGGER.error("Cannot write terrain data pack", failure);
        if (minecraft.gui.screen() != this) return;
        feedback = failure == null
                ? file == null ? Component.translatable("screen.eden_realm.terrain.exportCancelled").getString()
                : Component.translatable("screen.eden_realm.terrain.exported", file.toString()).getString()
                : Component.translatable("screen.eden_realm.terrain.error", failure.getMessage()).getString();
        publishedState = null;
        publishState();
    }

    private void publishState() {
        long seed = controller.previewSeed();
        if (publishedState != null && publishedVersion == controller.version()
                && publishedSeed == seed && publishedNativeFrames == nativeFrames
                && Objects.equals(publishedFeedback, feedback)) return;
        TerrainPack pack = controller.pack();
        JsonObject state = new JsonObject();
        state.add("acknowledged", acknowledged.deepCopy());
        JsonArray biomes = new JsonArray();
        pack.biomes().keySet().stream().sorted().forEach(id -> {
            JsonObject biome = new JsonObject();
            biome.addProperty("value", id);
            biome.addProperty("title", Component.translatable("biome.eden_realm." + id).getString());
            biomes.add(biome);
        });
        state.add("biomes", biomes);
        state.addProperty("selected", controller.selectedBiome());
        state.addProperty("seed", Long.toString(seed));
        state.add("preview", previewState());
        state.addProperty("loading", controller.loading());
        state.addProperty("previewAvailable", controller.nativeFrame() != null);
        state.addProperty("detailed", controller.detailed());
        state.addProperty("macro", controller.macro());
        state.addProperty("wholePrecisionSupported", controller.wholePrecisionSupported());
        state.addProperty("wholeProgress", controller.wholeProgress());
        state.addProperty("realtimeReady", displayedRealtime);
        state.addProperty("generation", controller.generation());
        state.addProperty("detailLoading", controller.detailLoading());
        state.addProperty("detailPrepared", controller.detailPrepared());
        state.addProperty("previewChunks", controller.previewChunks());
        state.addProperty("minPreviewChunks", com.kltyton.eden_realm.world.terrain.TerrainPreviewChunks.MIN_CHUNKS);
        state.addProperty("error", controller.error() == null ? "" : controller.error().getMessage());
        state.addProperty("feedback", feedback);
        state.addProperty("busy", fileOperation);
        TerrainProfile profile = pack.biomes().get(controller.selectedBiome());
        state.add("profile", profileFields(profile));
        state.add("defaults", profileFields(TerrainProfile.official(controller.selectedBiome())));
        JsonObject labels = new JsonObject();
        for (String key : new String[]{"title", "biome", "preview", "loading", "refining", "macro", "detailLoading", "detailReady", "wholeLoading", "wholeReady", "realtime", "reset", "export",
                "back", "done", "saveTip", "cancelTip", "exportTip", "resetTip", "left", "right", "help", "chunks", "parameters", "seed", "default", "elevationOffset", "reliefPercent",
                "spacingPercent", "shapePercent", "shoreIceBlocks", "generationChancePercent",
                "biomeSizePercent"}) {
            labels.addProperty(key, Component.translatable("screen.eden_realm.terrain." + key).getString());
        }
        state.add("labels", labels);
        String serialized = JSON.toJson(state);
        if (!serialized.equals(publishedState)) {
            workbench.publish(serialized);
            publishedState = serialized;
        }
        publishedVersion = controller.version();
        publishedSeed = seed;
        publishedFeedback = feedback;
        publishedNativeFrames = nativeFrames;
    }

    private static JsonObject profileFields(TerrainProfile profile) {
        JsonObject fields = new JsonObject();
        fields.addProperty("elevationOffset", profile.elevationOffset());
        fields.addProperty("reliefPercent", profile.reliefPercent());
        fields.addProperty("spacingPercent", profile.spacingPercent());
        fields.addProperty("shapePercent", profile.shapePercent());
        fields.addProperty("shoreIceBlocks", profile.shoreIceBlocks());
        fields.addProperty("generationChancePercent", profile.generationChancePercent());
        fields.addProperty("biomeSizePercent", profile.biomeSizePercent());
        return fields;
    }

    private void extractPreview(GuiGraphicsExtractor graphics) {
        var frame = controller.nativeFrame();
        int x = (int) Math.ceil(viewportX * width), y = (int) Math.ceil(viewportY * height);
        int viewWidth = (int) Math.floor((viewportX + viewportWidth) * width) - x;
        int viewHeight = (int) Math.floor((viewportY + viewportHeight) * height) - y;
        if (closed || frame == null || viewWidth <= 0 || viewHeight <= 0
                || x >= width || y >= height || x + viewWidth <= 0 || y + viewHeight <= 0) return;
        if (nativeRenderer == null) nativeRenderer = new NativeTerrainScene();
        if (blockRenderer == null) blockRenderer = new BlockSurfaceRenderer();
        if (realtimeRenderer == null) realtimeRenderer = new TerrainRealtimeRenderer();
        var surface = frame.surface();
        if (surface != cameraSurface) {
            cameraSurfaceMinY = cameraSurfaceMaxY = controller.previewSeaLevel();
            for (int value : surface.heights()) {
                cameraSurfaceMinY = Math.min(cameraSurfaceMinY, value);
                cameraSurfaceMaxY = Math.max(cameraSurfaceMaxY, value);
            }
            cameraSurface = surface;
        }
        if (resetView && frame.sceneKey() == controller.generation()) {
            double spanX = surface.width() * (double) surface.step();
            double spanZ = surface.depth() * (double) surface.step();
            var center = controller.previewCenter();
            cameraX = center == null ? surface.x() + spanX / 2 : center.getX();
            cameraZ = center == null ? surface.z() + spanZ / 2 : center.getZ();
            int low = java.util.Arrays.stream(surface.heights()).min().orElse(0);
            int high = java.util.Arrays.stream(surface.heights()).max().orElse(low);
            double projectedWidth = Math.abs(Math.cos(yaw)) * spanX + Math.abs(Math.sin(yaw)) * spanZ;
            double projectedHeight = (Math.abs(Math.sin(yaw)) * spanX + Math.abs(Math.cos(yaw)) * spanZ)
                    * Math.cos(angle) + (high - low) * Math.sin(angle);
            pixelsPerBlock = Math.min(viewWidth / (projectedWidth * 1.12), viewHeight / (projectedHeight * 1.12));
            resetView = false;
        }
        cameraY = controller.previewSeaLevel();
        var whole = controller.wholeSurface();
        double visibleSpan = Math.max(viewWidth / pixelsPerBlock,
                viewHeight / (pixelsPerBlock * Math.cos(angle)));
        double unit = Math.max(1, Math.min(surface.width() * (double) surface.step(), visibleSpan) / 256.0);
        double minY = cameraSurfaceMinY, maxY = cameraSurfaceMaxY;
        if (frame.tiles() != null) {
            minY = Math.min(minY, frame.tiles().minY());
            maxY = Math.max(maxY, frame.tiles().maxY());
        }
        if (whole != null) for (var tile : whole.tiles()) {
            if (Float.isFinite(tile.minY())) minY = Math.min(minY, tile.minY());
            if (Float.isFinite(tile.maxY())) maxY = Math.max(maxY, tile.maxY());
        }
        double minX = whole == null ? surface.x() : Math.min(surface.x(), whole.x());
        double minZ = whole == null ? surface.z() : Math.min(surface.z(), whole.z());
        double maxX = surface.x() + surface.width() * (double) surface.step();
        double maxZ = surface.z() + surface.depth() * (double) surface.step();
        if (whole != null) {
            maxX = Math.max(maxX, whole.x() + (double) whole.width());
            maxZ = Math.max(maxZ, whole.z() + (double) whole.depth());
        }
        var camera = new MapCamera(cameraX, cameraY, cameraZ, pixelsPerBlock, yaw, angle,
                "isometric", viewWidth, viewHeight, unit, 75)
                .withSceneBounds(minX, minY, minZ, maxX, maxY, maxZ);
        controller.view(frame.sceneKey(), cameraX, cameraZ, visibleSpan);
        var renderer = nativeRenderer;
        var blocks = blockRenderer;
        var realtime = realtimeRenderer;
        var live = controller.realtimeFrame();
        graphics.enableScissor(Math.max(0, x), Math.max(0, y), Math.min(width, x + viewWidth), Math.min(height, y + viewHeight));
        KuiNativeViewport.submit(graphics, x, y, viewWidth, viewHeight, (target, ignoredWidth, ignoredHeight) -> {
            if (closed) return false;
            blocks.update(whole);
            boolean wholeDrawn = whole != null && blocks.draw(camera, target, 1);
            boolean liveDrawn = false;
            if (!wholeDrawn) { realtime.update(live); liveDrawn = realtime.draw(camera, target); }
            if (wholeDrawn || liveDrawn || renderer.drawOpaque(camera, frame.surface(), frame.tiles(), frame.masks(), target, 1, false)) {
                if (!wholeDrawn && !liveDrawn) renderer.drawTransparent();
                displayedWhole = wholeDrawn;
                displayedRealtime = liveDrawn;
                nativeFrames++;
                long drawnAt = System.nanoTime();
                nativeFrameIntervalMillis = lastNativeFrameAt == 0 ? 0 : (drawnAt - lastNativeFrameAt) / 1_000_000.0;
                lastNativeFrameAt = drawnAt;
                long renderedGeneration = liveDrawn ? live.revision() : frame.sceneKey();
                if (pendingInputAt != 0 && renderedGeneration == controller.generation()
                        && (wholeDrawn || liveDrawn || controller.previewChunks() > 32)) {
                    inputToRenderMillis = Math.max(0, System.currentTimeMillis() - pendingInputAt);
                    inputResponseAt = pendingInputAt;
                    pendingInputAt = 0;
                }
                displayedSceneKey = renderedGeneration;
                displayedFrame = frame;
                displayedCamera = camera;
                return true;
            }
            return false;
        });
        graphics.disableScissor();
    }


    private JsonObject previewState() {
        JsonObject preview = new JsonObject();
        preview.addProperty("sceneKey", displayedSceneKey);
        preview.addProperty("displayedSceneKey", displayedSceneKey);
        preview.addProperty("nativeFrames", nativeFrames);
        preview.addProperty("preciseCacheHits", controller.nativeCacheHits());
        preview.addProperty("preciseCacheMisses", controller.nativeCacheMisses());
        preview.addProperty("preciseTileRenders", controller.nativeBakedTiles());
        preview.addProperty("liveCacheHits", controller.liveCacheHits());
        preview.addProperty("liveComputeMs", controller.liveComputeMillis());
        var live = realtimeRenderer == null ? null : realtimeRenderer.statistics();
        preview.addProperty("realtimeReady", displayedRealtime);
        preview.addProperty("realtimeRevision", live == null ? -1 : live.revision());
        preview.addProperty("realtimeColumns", live == null ? 0 : live.columns());
        preview.addProperty("realtimeWidth", live == null ? 0 : live.width());
        preview.addProperty("realtimeStep", displayedRealtime ? 1 : 0);
        preview.addProperty("realtimeDigest", live == null ? "" : live.digest());
        preview.addProperty("realtimeComputeMs", controller.realtimeFrame() == null ? 0 : controller.realtimeFrame().computeMillis());
        preview.addProperty("inputToRenderMs", inputToRenderMillis);
        preview.addProperty("inputResponseAt", inputResponseAt);
        preview.addProperty("rotation", displayedCamera == null ? yaw : displayedCamera.yaw());
        preview.addProperty("cameraDistance", displayedCamera == null ? 0 : displayedCamera.height()
                / (2 * displayedCamera.pixelsPerBlock() * Math.tan(Math.toRadians(37.5))));
        preview.addProperty("cameraX", displayedCamera == null ? cameraX : displayedCamera.x());
        preview.addProperty("cameraZ", displayedCamera == null ? cameraZ : displayedCamera.z());
        preview.addProperty("cameraY", displayedCamera == null ? cameraY : displayedCamera.y());
        preview.addProperty("pixelsPerBlock", pixelsPerBlock);
        if (displayedFrame == null || nativeRenderer == null) return preview;
        var stats = nativeRenderer.statistics();
        preview.addProperty("preciseTileCount", stats.visible());
        preview.addProperty("preciseAvailableTiles", displayedFrame.masks().size());
        var wholeStats = blockRenderer == null ? null : blockRenderer.statistics();
        boolean wholeReady = displayedWhole && wholeStats != null && wholeStats.revision() == controller.generation()
                && wholeStats.uploaded() == wholeStats.total() && wholeStats.columns() == (long) controller.previewChunks() * controller.previewChunks() * 256;
        preview.addProperty("wholePrecisionReady", wholeReady);
        preview.addProperty("wholePrecisionWidth", controller.wholeSurface() == null ? 0 : controller.wholeSurface().width());
        preview.addProperty("wholePrecisionStep", controller.wholeSurface() == null ? 0 : 1);
        preview.addProperty("wholeUploadedRegions", wholeStats == null ? 0 : wholeStats.uploaded());
        preview.addProperty("wholeTotalRegions", wholeStats == null ? 0 : wholeStats.total());
        preview.addProperty("wholeColumns", wholeStats == null ? 0 : wholeStats.columns());
        preview.addProperty("wholeDrawnRegions", wholeStats == null ? 0 : wholeStats.drawn());
        preview.addProperty("wholeGeometryStep", wholeStats == null ? 0 : wholeStats.geometryStep());
        preview.addProperty("detailHires", controller.wholePrecisionSupported() ? wholeReady : stats.visible() > 0);
        preview.addProperty("detailScale", stats.visible() > 0 ? 1 : 0);
        preview.addProperty("residentTiles", stats.resident());
        preview.addProperty("loadingTiles", stats.loading());
        preview.addProperty("residentBytes", stats.residentBytes());
        preview.addProperty("drawCalls", stats.drawCalls());
        preview.addProperty("frameIntervalMs", nativeFrameIntervalMillis);
        var surface = displayedFrame.surface();
        var whole = displayedWhole ? controller.wholeSurface() : null;
        preview.addProperty("baseWidth", displayedRealtime ? live.width() : whole == null ? surface.width() : whole.width());
        preview.addProperty("baseDepth", displayedRealtime ? live.width() : whole == null ? surface.depth() : whole.depth());
        preview.addProperty("baseStep", displayedRealtime ? 1 : whole == null ? surface.step() : 1);
        preview.addProperty("span", whole == null ? Math.max(surface.width(), surface.depth()) * (long) surface.step() : Math.max(whole.width(), whole.depth()));
        preview.addProperty("digest", displayedFrame.digest());
        var precise = displayedFrame.precise();
        if (precise != null) {
            JsonObject bounds = new JsonObject();
            bounds.addProperty("minX", precise.originX() * (long) precise.step());
            bounds.addProperty("minZ", precise.originZ() * (long) precise.step());
            bounds.addProperty("maxX", (precise.originX() + precise.width()) * (long) precise.step());
            bounds.addProperty("maxZ", (precise.originZ() + precise.depth()) * (long) precise.step());
            preview.add("preciseBounds", bounds);
        }
        return preview;
    }
}
