package com.kltyton.eden_realm.world.terrain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Per-biome settings used by the world-creation terrain editor. */
public record TerrainProfile(
        int elevationOffset,
        int reliefPercent,
        int spacingPercent,
        int shapePercent,
        int shoreIceBlocks,
        int generationChancePercent,
        int biomeSizePercent) {
    public static final Codec<TerrainProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(-64, 96).fieldOf("elevationOffset").forGetter(TerrainProfile::elevationOffset),
            Codec.intRange(25, 250).fieldOf("reliefPercent").forGetter(TerrainProfile::reliefPercent),
            Codec.intRange(40, 240).fieldOf("spacingPercent").forGetter(TerrainProfile::spacingPercent),
            Codec.intRange(25, 250).fieldOf("shapePercent").forGetter(TerrainProfile::shapePercent),
            Codec.intRange(0, 24).fieldOf("shoreIceBlocks").forGetter(TerrainProfile::shoreIceBlocks),
            Codec.intRange(0, 200).fieldOf("generationChancePercent")
                    .forGetter(TerrainProfile::generationChancePercent),
            Codec.intRange(40, 240).fieldOf("biomeSizePercent").forGetter(TerrainProfile::biomeSizePercent)
    ).apply(instance, TerrainProfile::new));

    public static TerrainProfile official(String biomeId) {
        int shore = switch (biomeId) {
            case "crystal_lake_shore" -> 10;
            case "silver_frost_lakeshore" -> 5;
            case "cloud_sea_flatlands", "star_stream_plateau", "sky_mirror_lake", "cloud_island_chain",
                    "rosy_cloud_terraces", "flower_mirror_lake" -> 0;
            case "glacier_meander", "icefall_fjord", "frost_stream_valley", "snow_ridge_valley" -> 3;
            default -> 2;
        };
        return new TerrainProfile(0, 100, 100, 100, shore, 100, 100);
    }

    public TerrainProfile withGenerationChance(int chancePercent) {
        return new TerrainProfile(elevationOffset, reliefPercent, spacingPercent, shapePercent,
                shoreIceBlocks, chancePercent, biomeSizePercent);
    }
}
