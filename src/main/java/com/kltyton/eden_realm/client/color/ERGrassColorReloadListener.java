package com.kltyton.eden_realm.client.color;

import com.kltyton.eden_realm.ERConstants;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class ERGrassColorReloadListener extends SimplePreparableReloadListener<int[]> {
    private static final Identifier LOCATION = ERConstants.id("textures/colormap/grass.png");

    @Override
    protected int[] prepare(ResourceManager manager, ProfilerFiller profiler) {
        try (InputStream stream = manager.open(LOCATION); NativeImage image = NativeImage.read(stream)) {
            if (image.getWidth() != 256 || image.getHeight() != 256) {
                throw new IllegalStateException(
                        "Eden grass colormap must be 256x256, found " + image.getWidth() + "x" + image.getHeight());
            }
            return image.makePixelArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load Eden grass colormap " + LOCATION, exception);
        }
    }

    @Override
    protected void apply(int[] loadedPixels, ResourceManager manager, ProfilerFiller profiler) {
        ERGrassColors.initialize(loadedPixels);
    }
}
