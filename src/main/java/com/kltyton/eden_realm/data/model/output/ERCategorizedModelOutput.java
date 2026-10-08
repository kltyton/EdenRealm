package com.kltyton.eden_realm.data.model.output;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.block.tree.ERWoodSet;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;

/** Routes native model output and its references into content directories before caching. */
public final class ERCategorizedModelOutput implements CachedOutput {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final List<String> FRUITS = List.of(
            "tide_song_coconut",
            "sacred_light_fruit",
            "cloud_crown_fruit",
            "twilight_pomegranate");
    private static final List<String> CROPS = List.of(
            "dewspike_grain",
            "star_pattern_yam",
            "moon_clover",
            "vine_bean",
            "crystal_dew_fruit");
    private static final List<String> AQUATIC = List.of(
            "blue_court_seagrass",
            "tall_blue_court_seagrass",
            "purple_glow_cattail",
            "gray_spike_reed",
            "water_scallion",
            "umbrella_hygrophila",
            "duckweed",
            "water_fern",
            "bubble_grass");
    private static final List<String> WOOD_PARTS = List.of(
            "log",
            "wood",
            "stripped_log",
            "stripped_wood",
            "planks",
            "stairs",
            "slab",
            "fence",
            "fence_gate",
            "button",
            "pressure_plate",
            "shelf",
            "leaves",
            "flowering_leaves",
            "sapling",
            "door",
            "trapdoor",
            "sign",
            "wall_sign",
            "hanging_sign",
            "wall_hanging_sign",
            "boat",
            "chest_boat");
    private static final List<String> TERRAIN = List.of(
            "weathered_rock",
            "rooted_rock",
            "boundary_rock",
            "rubble",
            "tundra_rock",
            "shale",
            "raw_rock",
            "chiseled_raw_rock_bricks",
            "mossy_raw_rock",
            "eden_dirt",
            "eden_grass_block",
            "grass_covered_raw_rock",
            "grass_covered_floating_island_rock",
            "eden_farmland",
            "thin_cloud_soil",
            "floating_island_rock",
            "sky_platform_stone",
            "ice_crystal_rock",
            "frost_pattern_stone",
            "sedimentary_silt",
            "peat_block",
            "wet_swamp_soil",
            "amber_crystal_block",
            "sun_rock",
            "spring_stone",
            "eroded_sandstone",
            "coast_sand",
            "amber_sand",
            "oasis_sand",
            "smooth_coast_sand",
            "smooth_amber_sand",
            "smooth_oasis_sand",
            "cut_coast_sand",
            "cut_amber_sand",
            "cut_oasis_sand",
            "chiseled_coast_sand",
            "chiseled_amber_sand",
            "chiseled_oasis_sand");
    private static final List<String> SKY = List.of(
            "cloud_court_stone",
            "cloud",
            "sky_pool_stone",
            "dense_cloud",
            "rosy_cloud",
            "dense_rosy_cloud");
    private final Path modelRoot;
    private final CachedOutput output;

    public ERCategorizedModelOutput(PackOutput packOutput, CachedOutput output) {
        this.modelRoot = packOutput.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models")
                .json(ERConstants.id("anchor")).getParent();
        this.output = output;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void writeIfNeeded(Path path, byte[] input, HashCode hash) throws IOException {
        Path target = path.startsWith(modelRoot)
                ? modelRoot.resolve(assetPath(modelRoot.relativize(path).toString().replace('\\', '/')))
                : path;
        JsonElement json = rewrite(JsonParser.parseString(new String(input, StandardCharsets.UTF_8)));
        byte[] bytes = JSON.toJson(json).getBytes(StandardCharsets.UTF_8);
        output.writeIfNeeded(target, bytes, Hashing.sha1().hashBytes(bytes));
    }

    private static JsonElement rewrite(JsonElement value) {
        if (value.isJsonObject()) {
            for (var entry : value.getAsJsonObject().entrySet()) {
                entry.setValue(rewrite(entry.getValue()));
            }
        } else if (value.isJsonArray()) {
            var array = value.getAsJsonArray();
            for (int index = 0; index < array.size(); index++) {
                array.set(index, rewrite(array.get(index)));
            }
        } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            String text = value.getAsString();
            String prefix = ERConstants.MOD_ID + ":";
            if (text.startsWith(prefix)) {
                return new JsonPrimitive(prefix + assetPath(text.substring(prefix.length())));
            }
        }
        return value;
    }

    private static String assetPath(String path) {
        int separator = path.indexOf('/');
        if (separator < 0) {
            return path;
        }
        String kind = path.substring(0, separator);
        if (!kind.equals("block") && !kind.equals("item")) {
            return path;
        }
        String name = path.substring(separator + 1);
        if (name.startsWith("crop/")) {
            name = name.substring(5);
        } else if (name.contains("/")) {
            return path;
        }
        String stem = name.endsWith(".json") ? name.substring(0, name.length() - 5) : name;
        return kind + "/" + category(stem) + "/" + name;
    }

    private static boolean begins(String name, List<String> roots) {
        return roots.stream().anyMatch(root -> name.equals(root) || name.startsWith(root + "_"));
    }

    private static String category(String name) {
        for (ERWoodSet wood : ERWoodSet.values()) {
            String prefix = wood.id() + "_";
            if (name.startsWith(prefix) && begins(name.substring(prefix.length()), WOOD_PARTS)) {
                return "wood/" + wood.id();
            }
        }
        if (begins(name, FRUITS)) return "fruit";
        if (begins(name, CROPS)) return "crop";
        if (name.startsWith("wild_")) return "plant/wild_crop";
        if (name.contains("mushroom") || name.contains("fungus")) return "mushroom";
        if (name.contains("coral")) return "coral";
        if (name.contains("_ore")) return "ore";
        if (begins(name, List.of("sea_valley_stone_pillar", "temple_stone_pillar", "mossy_temple_stone_pillar")))
            return "building/pillar";
        if (begins(name, TERRAIN) || name.contains("sandstone")) return "terrain";
        if (SKY.contains(name) || begins(name, List.of("cloud_court_stone", "sky_pool_stone"))) return "sky";
        if (name.endsWith("_spawn_egg")) return "spawn_egg";
        if (List.of("_sword", "_axe", "_pickaxe", "_shovel", "_hoe").stream().anyMatch(name::endsWith)) return "tool";
        if (begins(name, AQUATIC)) return "plant/aquatic";
        return "plant";
    }
}
