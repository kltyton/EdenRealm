package com.kltyton.eden_realm.data.particle;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.block.tree.ERWoodSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

public final class ERParticleDescriptionProvider implements DataProvider {
    private static final int VARIANT_COUNT = 8;
    private final PackOutput.PathProvider paths;

    public ERParticleDescriptionProvider(PackOutput output) {
        paths = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "particles");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        for (ERWoodSet wood : ERWoodSet.values()) {
            writes.add(writeDescription(output, wood.leavesName(), wood.id()));
        }
        writes.add(writeDescription(output, "honey_maple_red_leaves", "honey_maple_red"));
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    private CompletableFuture<?> writeDescription(CachedOutput output, String particleId, String textureSet) {
        JsonArray textures = new JsonArray();
        for (int index = 0; index < VARIANT_COUNT; index++) {
            textures.add(ERConstants.id("leaves/" + textureSet + "/" + index).toString());
        }
        JsonObject definition = new JsonObject();
        definition.add("textures", textures);
        return DataProvider.saveStable(output, definition, paths.json(ERConstants.id(particleId)));
    }

    @Override
    public String getName() {
        return "Eden Realm Falling Leaf Particle Descriptions";
    }
}
