package com.kltyton.eden_realm.data.model.block;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;

public final class OreModelCheck {
    private OreModelCheck() { }

    public static void main(String[] args) throws java.io.IOException {
        for (String name : List.of("raw_rock_coal_ore", "raw_rock_iron_ore", "primal_radiance_ore", "rock_steel_ore", "sinking_star_ore")) {
            Path path = Path.of("src/generated/resources/assets/eden_realm/models/block/ore/" + name + ".json");
            try (var reader = Files.newBufferedReader(path)) {
                CuboidModel model = CuboidModel.GSON.fromJson(reader, CuboidModel.class);
                require(model.geometry() instanceof UnbakedCuboidGeometry, "native parser accepts ore geometry");
                var elements = ((UnbakedCuboidGeometry) model.geometry()).elements();
                require(elements.size() == 2, "ore has base and supplied glow layer");
                var base = elements.getFirst();
                var glow = elements.getLast();
                require(base.lightEmission() == 0 && base.shade() && base.faceData().ambientOcclusion(),
                        "rock base retains normal light and shading");
                require(glow.lightEmission() == 15 && !glow.shade() && !glow.faceData().ambientOcclusion(),
                        "native parser retains fullbright unshaded glow with no AO");
                require(base.from().equals(glow.from()) && base.to().equals(glow.to()) && glow.faces().size() == 6,
                        "glow aligns with the original cube on all six faces");
                for (var face : glow.faces().values()) {
                    require(face.texture().equals("#glow") && !face.faceData().ambientOcclusion(),
                            "every overlay face uses its mask and inherits the no-AO material");
                }
            }
        }
        System.out.println("Ore models passed native parsing: 5 base/glow pairs");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
