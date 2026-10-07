package com.kltyton.eden_realm.world.terrain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Portable profile format shared by the terrain editor and later world import. */
public record TerrainPack(String format, int version, Target target, long seed,
                          Map<String, TerrainProfile> biomes) {
    public static final String FORMAT = "eden_realm:terrain_profile";
    public static final int VERSION = 1;
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Codec<TerrainPack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("format").forGetter(TerrainPack::format),
            Codec.INT.fieldOf("version").forGetter(TerrainPack::version),
            Target.CODEC.fieldOf("target").forGetter(TerrainPack::target),
            Codec.LONG.fieldOf("seed").forGetter(TerrainPack::seed),
            Codec.unboundedMap(Codec.STRING, TerrainProfile.CODEC).fieldOf("biomes")
                    .forGetter(TerrainPack::biomes)
    ).apply(instance, TerrainPack::new));

    public record Target(String minecraft, String loader, String dimension) {
        private static final Codec<Target> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("minecraft").forGetter(Target::minecraft),
                Codec.STRING.fieldOf("loader").forGetter(Target::loader),
                Codec.STRING.fieldOf("dimension").forGetter(Target::dimension)
        ).apply(instance, Target::new));
    }

    public static TerrainPack official(long seed, Collection<String> biomeIds) {
        Map<String, TerrainProfile> profiles = new LinkedHashMap<>();
        for (String biomeId : biomeIds) {
            profiles.put(biomeId, TerrainProfile.official(biomeId));
        }
        return new TerrainPack(FORMAT, VERSION,
                new Target("26.2", "neoforge", "eden_realm:eden_layer"), seed, Map.copyOf(profiles));
    }

    public TerrainPack withProfile(String biomeId, TerrainProfile profile) {
        Map<String, TerrainProfile> updated = new LinkedHashMap<>(biomes);
        updated.put(biomeId, profile);
        return new TerrainPack(format, version, target, seed, Map.copyOf(updated));
    }

    public TerrainPack reset(String biomeId) {
        return withProfile(biomeId, TerrainProfile.official(biomeId));
    }

    public TerrainPack withSeed(long nextSeed) {
        return new TerrainPack(format, version, target, nextSeed, biomes);
    }

    public String toJson() {
        return JSON.toJson(CODEC.encodeStart(JsonOps.INSTANCE, this).getOrThrow());
    }

    public static TerrainPack fromJson(String json) {
        TerrainPack pack = CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
        if (!FORMAT.equals(pack.format) || pack.version != VERSION) {
            throw new IllegalArgumentException("Unsupported terrain pack format or version");
        }
        if (pack.biomes.values().stream().noneMatch(profile -> profile.generationChancePercent() > 0)) {
            throw new IllegalArgumentException("At least one biome must have a positive generation chance");
        }
        return pack;
    }
}
