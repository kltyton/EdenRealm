package com.kltyton.eden_realm.client.world.preview;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.registry.content.block.ERTerrainBlocks;
import com.kltyton.eden_realm.world.terrain.TerrainRealtimePreview;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.kltyton.eden_realm.client.world.preview.render.BlockSurfaceRenderer;
import com.kltyton.eden_realm.client.world.preview.render.MapCamera;
import io.github.kltyton.kltytonui.chunkmap.ChunkMapSnapshot;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.DynamicUniformStorage;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowyBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

/** Resolves current height and material fields on the GPU, then draws individual textured block surfaces. */
public final class TerrainRealtimeRenderer implements AutoCloseable {
    public record Statistics(long revision, int width, long columns, boolean drawn, String digest) { }
    private static final int FAMILIES = 12;
    private static BindGroupLayout layout(String... samplers) {
        var builder = BindGroupLayout.builder().withUniform("TerrainLiveParams", UniformType.UNIFORM_BUFFER);
        for (String sampler : samplers) builder.withSampler(sampler);
        return builder.build();
    }
    private static final BindGroupLayout GROUND_LAYOUT = layout("Lattice", "Noise");
    private static final BindGroupLayout SHORE_LAYOUT = layout("GroundField");
    private static final BindGroupLayout SURFACE_LAYOUT = layout("GroundField", "Noise", "ShoreRadii", "ShoreDistance");
    private static final BindGroupLayout DRAW_LAYOUT = layout("GroundField", "SurfaceField", "Materials", "MaterialIds", "Sampler0");
    private static final RenderPipeline GROUND = resolve("terrain_live_ground", GROUND_LAYOUT);
    private static final RenderPipeline SHORE = resolve("terrain_live_shore", SHORE_LAYOUT);
    private static final RenderPipeline SURFACE = resolve("terrain_live_surface", SURFACE_LAYOUT);
    private static final RenderPipeline OPAQUE = drawPipeline(false), TRANSPARENT = drawPipeline(true);
    private final ProjectionMatrixBuffer projection = new ProjectionMatrixBuffer("Eden realtime terrain");
    private final DynamicUniformStorage<Params> parameters = new DynamicUniformStorage<>("Eden realtime terrain fields", 64, 256);
    private final ArrayList<Image> images = new ArrayList<>();
    private TerrainRealtimePreview.Frame frame;
    private TerrainRealtimePreview.Base base;
    private BlockStateModelSet models;
    private Image lattice, noise, radii, ground, shore, surface, materials, ids;
    private ByteBuffer transfer;
    private GpuBuffer transferBuffer;
    private Statistics statistics = new Statistics(-1, 0, 0, false, "");

    private static RenderPipeline resolve(String shader, BindGroupLayout layout) {
        return RenderPipeline.builder().withBindGroupLayout(layout)
                .withLocation(ERConstants.id(shader))
                .withVertexShader(ERConstants.id("core/terrain_live_resolve"))
                .withFragmentShader(ERConstants.id("core/" + shader))
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(false)
                .withColorTargetState(new ColorTargetState(Optional.empty(), GpuFormat.RGBA32_FLOAT, 15)).build();
    }
    private static RenderPipeline drawPipeline(boolean transparent) {
        return RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
                .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION).withBindGroupLayout(DRAW_LAYOUT)
                .withLocation(ERConstants.id(transparent ? "terrain_live_transparent" : "terrain_live_opaque"))
                .withVertexShader(ERConstants.id("core/terrain_live_blocks"))
                .withFragmentShader(ERConstants.id("core/terrain_live_blocks"))
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(false)
                .withDepthStencilState(DepthStencilState.DEFAULT)
                .withColorTargetState(new ColorTargetState(transparent ? Optional.of(com.mojang.blaze3d.pipeline.BlendFunction.TRANSLUCENT)
                        : Optional.empty(), GpuFormat.RGBA8_UNORM, 15)).build();
    }
    public Statistics statistics() { return statistics; }
    public static void prewarm() {
        var device = RenderSystem.getDevice();
        for (var pipeline : new RenderPipeline[]{GROUND, SHORE, SURFACE, OPAQUE, TRANSPARENT})
            device.precompilePipeline(pipeline);
    }
    public void update(TerrainRealtimePreview.Frame next) {
        parameters.endFrame();
        if (next == null) { frame = null; return; }
        var currentModels = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        if (frame == next && models == currentModels) return;
        if (base != next.base() || models != currentModels) {
            reset();
            base = next.base(); models = currentModels;
            int side = base.width() + TerrainRealtimePreview.MARGIN * 2;
            int points = side / 4 + 1;
            noise = image("Eden terrain material noise", side, side, false);
            lattice = image("Eden terrain quart lattice", points, points, false);
            radii = image("Eden terrain shore radii", base.biomes().size(), 1, false);
            ground = image("Eden terrain resolved ground", side, side, true);
            shore = image("Eden terrain horizontal shore distance", side, side, true);
            surface = image("Eden terrain resolved surface", side, side, true);
            upload(noise, base.noise());
            captureMaterials();
        }
        frame = next;
        upload(lattice, next.lattice()); upload(radii, next.shoreRadii());
        var params = parameters.writeUniform(new Params(next, 0, 0, 0, 0, 1, 1));
        try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Eden realtime ground resolve", ground.view, Optional.empty())) {
            pass.setPipeline(GROUND); pass.setUniform("TerrainLiveParams", params);
            bind(pass, "Lattice", lattice); bind(pass, "Noise", noise); pass.draw(3, 1, 0, 0);
        }
        try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Eden realtime shore distance", shore.view, Optional.empty())) {
            pass.setPipeline(SHORE); pass.setUniform("TerrainLiveParams", params);
            bind(pass, "GroundField", ground); pass.draw(3, 1, 0, 0);
        }
        try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Eden realtime shoreline resolve", surface.view, Optional.empty())) {
            pass.setPipeline(SURFACE); pass.setUniform("TerrainLiveParams", params);
            bind(pass, "GroundField", ground); bind(pass, "Noise", noise); bind(pass, "ShoreRadii", radii); bind(pass, "ShoreDistance", shore);
            pass.draw(3, 1, 0, 0);
        }
        statistics = new Statistics(next.revision(), base.width(), (long) base.width() * base.width(), false, next.digest());
    }
    public boolean draw(MapCamera camera, RenderTarget target) {
        if (frame == null) return false;
        var transform = RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f(camera.viewMatrix()),
                new Vector4f(1, 1, 1, 1), new Vector3f(), new Matrix4f());
        var previousProjection = RenderSystem.getProjectionMatrixBuffer();
        var previousType = RenderSystem.getProjectionType();
        RenderSystem.setProjectionMatrix(projection.getBuffer(camera.nativeProjectionMatrix(RenderSystem.getDevice().getDeviceInfo().isZZeroToOne())),
                camera.mode().equals("street") ? ProjectionType.PERSPECTIVE : ProjectionType.ORTHOGRAPHIC);
        var atlas = Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
        try {
            for (int layer = 0; layer < 3; layer++) {
                final int passLayer = layer;
                try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Eden realtime textured blocks",
                        target.getColorTextureView(), layer == 0 ? Optional.of(new Vector4f(0.46f, 0.67f, 0.82f, 1)) : Optional.empty(),
                        target.getDepthTextureView(), layer == 0 ? OptionalDouble.of(0) : OptionalDouble.empty())) {
                    pass.setPipeline(layer == 0 ? OPAQUE : TRANSPARENT); RenderSystem.bindDefaultUniforms(pass);
                    pass.setUniform("DynamicTransforms", transform);
                    pass.setUniform("TerrainLiveParams", parameters.writeUniform(new Params(frame, passLayer,
                            base.x() - camera.x(), -camera.y(), base.z() - camera.z(),
                            Math.sin(camera.yaw()) > 0 ? -1 : 1, Math.cos(camera.yaw()) > 0 ? 1 : -1)));
                    bind(pass, "GroundField", ground); bind(pass, "SurfaceField", surface);
                    bind(pass, "Materials", materials); bind(pass, "MaterialIds", ids);
                    pass.bindTexture("Sampler0", atlas.getTextureView(), atlas.getSampler());
                    pass.draw(Math.multiplyExact(base.width() * base.width(), 18), 1, 0, 0);
                }
            }
        } finally { RenderSystem.setProjectionMatrix(previousProjection, previousType); }
        statistics = new Statistics(frame.revision(), base.width(), (long) base.width() * base.width(), true, frame.digest());
        return true;
    }
    private void captureMaterials() {
        BlockState grass = ERTerrainBlocks.EDEN_GRASS_BLOCK.get().defaultBlockState();
        BlockState[] states = {grass.setValue(SnowyBlock.SNOWY, false), grass.setValue(SnowyBlock.SNOWY, true),
                ERTerrainBlocks.EDEN_DIRT.get().defaultBlockState(), Blocks.GRAVEL.defaultBlockState(), Blocks.STONE.defaultBlockState(),
                Blocks.PACKED_ICE.defaultBlockState(), Blocks.BLUE_ICE.defaultBlockState(), Blocks.SNOW_BLOCK.defaultBlockState(),
                ERTerrainBlocks.ICE_CRYSTAL_ROCK.get().defaultBlockState(), Blocks.SNOW.defaultBlockState(),
                Blocks.WATER.defaultBlockState(), Blocks.ICE.defaultBlockState()};
        int count = states.length * base.biomes().size(), halo = BlockSurfaceRenderer.HALO;
        var snapshot = ChunkMapSnapshot.builder(0, 0, count + halo * 2, 1 + halo * 2, 1, 0, 2);
        for (int z = 0; z < 1 + halo * 2; z++) for (int x = 0; x < count + halo * 2; x++) {
            int material = Math.clamp(x - halo, 0, count - 1);
            snapshot.addRun(x, z, 0, 1, states[material % states.length]);
            snapshot.setBiome(x, z, base.biomes().get(material / states.length), base.x() + x, base.z() + z);
        }
        var capture = new BlockSurfaceRenderer.Capture();
        var tile = capture.encode(snapshot.build(), halo, halo, count, 1, () -> false);
        float[] materialIds = new float[count * 4];
        for (int i = 0; i < count; i++) {
            int cell = halo * (count + halo * 2) + i + halo;
            int run = tile.ranges()[cell * 2];
            materialIds[i * 4] = tile.runs()[run * 4 + 2];
        }
        float[] palette = capture.palette();
        materials = image("Eden terrain native model materials", 30, palette.length / 120, false);
        ids = image("Eden terrain material families", FAMILIES, base.biomes().size(), false);
        upload(materials, palette); upload(ids, materialIds);
    }
    private void bind(RenderPass pass, String name, Image image) {
        pass.bindTexture(name, image.view, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
    }
    private Image image(String name, int width, int height, boolean attachment) {
        var texture = RenderSystem.getDevice().createTexture(() -> name,
                GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST | (attachment ? GpuTexture.USAGE_RENDER_ATTACHMENT : 0),
                GpuFormat.RGBA32_FLOAT, width, height, 1, 1);
        var image = new Image(texture, RenderSystem.getDevice().createTextureView(texture));
        images.add(image); return image;
    }
    private void upload(Image image, float[] values) {
        int bytes = values.length * 4;
        if (transfer == null || transfer.capacity() < bytes) {
            if (transfer != null) MemoryUtil.memFree(transfer);
            transfer = MemoryUtil.memAlloc(bytes).order(ByteOrder.nativeOrder());
        }
        if (transferBuffer == null || transferBuffer.size() < bytes) {
            if (transferBuffer != null) transferBuffer.close();
            transferBuffer = RenderSystem.getDevice().createBuffer(() -> "Eden realtime terrain transfer",
                    GpuBuffer.USAGE_COPY_SRC | GpuBuffer.USAGE_COPY_DST, bytes);
        }
        transfer.clear().limit(bytes); for (float value : values) transfer.putFloat(value); transfer.flip();
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        var slice = transferBuffer.slice(0, bytes); encoder.writeToBuffer(slice, transfer);
        int width = image.texture.getWidth(0), height = image.texture.getHeight(0);
        encoder.copyBufferToTexture(slice, 0, 0, width, height, image.texture, 0, 0, width, height, 0, 0);
    }
    private void reset() {
        images.forEach(Image::close); images.clear(); base = null; frame = null;
        statistics = new Statistics(-1, 0, 0, false, "");
    }
    @Override public void close() {
        reset(); projection.close(); parameters.close();
        if (transfer != null) { MemoryUtil.memFree(transfer); transfer = null; }
        if (transferBuffer != null) { transferBuffer.close(); transferBuffer = null; }
    }
    private record Image(GpuTexture texture, GpuTextureView view) {
        void close() { view.close(); texture.close(); }
    }
    private record Params(TerrainRealtimePreview.Frame frame, int layer, double x, double y, double z,
                          double directionX, double directionZ) implements DynamicUniformStorage.DynamicUniform {
        @Override public void write(ByteBuffer buffer) {
            buffer.putFloat(frame.base().width()).putFloat(frame.base().width()).putFloat(layer).putFloat(frame.seaLevel());
            buffer.putFloat((float) x).putFloat((float) y).putFloat((float) z).putFloat(1);
            buffer.putFloat((float) directionX).putFloat((float) directionZ).putFloat(frame.base().x()).putFloat(frame.base().z());
            buffer.putFloat(frame.minY()).putFloat(frame.maxY()).putFloat(TerrainRealtimePreview.MARGIN).putFloat(0);
        }
    }
}
