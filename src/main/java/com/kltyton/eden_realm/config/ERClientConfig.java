package com.kltyton.eden_realm.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client preferences that do not change a world's generation seed or saved terrain profiles. */
public final class ERClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.LongValue TERRAIN_PREVIEW_SEED;
    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("terrainPreview");
        TERRAIN_PREVIEW_SEED = builder.comment("Seed used only by the terrain preview. World creation and exported data packs use the world's seed.")
                .defineInRange("seed", 4305210398655669057L, Long.MIN_VALUE, Long.MAX_VALUE);
        builder.pop();
        SPEC = builder.build();
    }
    private ERClientConfig() { }
}
