package com.kltyton.eden_realm.common.block.plant;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class PlantBehaviorCheck {
    private PlantBehaviorCheck() {
    }

    public static void main(String[] args) {
        List<String> failures = new ArrayList<>();
        verifyCalibratedTallPlantShapes(failures);
        verifyAquaticPlantItemModels(failures);
        verifyFungusUsesPositionWeightedModels(failures);
        verifyVanillaStylePlantLoot(failures);

        if (!failures.isEmpty()) {
            throw new AssertionError(String.join(System.lineSeparator(), failures));
        }
        System.out.println("Plant behavior checks passed");
    }

    private static void verifyAquaticPlantItemModels(List<String> failures) {
        for (String id : List.of("water_scallion", "umbrella_hygrophila")) {
            String model = resource("assets/eden_realm/models/item/" + id + ".json");
            if (!model.contains("eden_realm:item/" + id)) {
                failures.add(id + " must use its dedicated item texture instead of a block texture");
            }
            resource("assets/eden_realm/textures/item/" + id + ".png");
        }
    }

    private static void verifyCalibratedTallPlantShapes(List<String> failures) {
        ERPlantShapes.DoublePlantShape waterScallion = ERPlantShapes.WATER_SCALLION;
        if (waterScallion.lowerWidth() != 12.0
                || waterScallion.lowerHeight() != 16.0
                || waterScallion.upperWidth() != 9.0
                || waterScallion.upperHeight() != 7.0
                || waterScallion.totalHeight() != 23.0) {
            failures.add("water scallion must keep its measured 12x16 lower and 9x7 upper shapes");
        }
    }

    private static void verifyFungusUsesPositionWeightedModels(List<String> failures) {
        for (String id : List.of(
                "small_parasol_mushroom",
                "crumbly_mushroom",
                "blue_glow_mushroom")) {
            String blockState = resource("assets/eden_realm/blockstates/" + id + ".json");
            if (blockState.contains("\"variant=")) {
                failures.add(id + " must use position-seeded weighted models without a persisted variant property");
            }
            for (int variant = 1; variant <= 3; variant++) {
                if (!blockState.contains("eden_realm:block/" + id + "_" + variant)) {
                    failures.add(id + " blockstate is missing weighted model " + variant);
                }
            }
        }
    }

    private static void verifyVanillaStylePlantLoot(List<String> failures) {
        for (String id : List.of(
                "longleaf_sedge",
                "green_spike_grass",
                "golden_spike_grass",
                "purple_glow_cattail",
                "gray_spike_reed",
                "water_scallion",
                "umbrella_hygrophila")) {
            String loot = resource("data/eden_realm/loot_table/blocks/" + id + ".json");
            if (!loot.contains("minecraft:shears")) {
                failures.add(id + " loot must be conditional like vanilla grass instead of unconditional self-drop");
            }
        }
    }

    private static String resource(String path) {
        try (InputStream input = PlantBehaviorCheck.class.getClassLoader().getResourceAsStream(path)) {
            if (input == null) {
                throw new AssertionError("missing generated resource: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new AssertionError("failed to read generated resource: " + path, exception);
        }
    }
}
