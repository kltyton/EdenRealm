package com.kltyton.eden_realm.client.particle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LeafParticleAssetsCheck {
    private static final Path SOURCE_ROOT = Path.of("参考资料/树木的基础方块,物品和实体/树叶粒子");
    private static final Path TEXTURE_ROOT = Path.of("src/main/resources/assets/eden_realm/textures/particle/leaves");
    private static final Path DESCRIPTION_ROOT = Path.of("src/generated/resources/assets/eden_realm/particles");

    private LeafParticleAssetsCheck() {
    }

    public static void main(String[] args) throws IOException {
        Map<String, String> woods = new LinkedHashMap<>();
        woods.put("云冠树", "cloud_crown");
        woods.put("冰晶松", "ice_crystal_pine");
        woods.put("古灵树", "ancient_spirit");
        woods.put("圣辉树", "sacred_light");
        woods.put("天穹柏树", "sky_cypress");
        woods.put("岩脊松树", "ridge_pine");
        woods.put("幽灯树", "gloomlight");
        woods.put("星辉树", "starshine");
        woods.put("晨露树", "morning_dew");
        woods.put("暮光榴树", "twilight_pomegranate");
        woods.put("月潮树", "moon_tide");
        woods.put("海冠树", "sea_crown");
        woods.put("潮歌树", "tide_song");
        woods.put("炽羽木", "blazing_feather");
        woods.put("琥珀树", "amber");
        woods.put("苍穹树", "firmament");
        woods.put("蜜枫树", "honey_maple");
        woods.put("金叶榉树", "golden_beech");
        woods.put("银霜松", "silver_frost_fir");
        woods.put("雾藤木", "mist_vine");
        woods.put("风铃松", "wind_chime_pine");
        woods.put("龙鳞树", "dragon_scale");
        woods.put("王树", "king_tree");

        if (woods.size() != 23) {
            throw new IllegalStateException("Expected 23 wood particle mappings");
        }
        for (Map.Entry<String, String> entry : woods.entrySet()) {
            verifyWood(entry.getKey(), entry.getValue());
        }
        verifyBindings();
        System.out.println("Leaf particle asset checks passed: 23 species, 184 textures");
    }

    private static void verifyWood(String sourceName, String woodId) throws IOException {
        Path sourceDirectory = SOURCE_ROOT.resolve(sourceName);
        Path targetDirectory = TEXTURE_ROOT.resolve(woodId);
        long sourceCount;
        try (var files = Files.list(sourceDirectory)) {
            sourceCount = files.filter(path -> path.getFileName().toString().endsWith(".png")).count();
        }
        if (sourceCount != 8) {
            throw new IllegalStateException(sourceName + " expected 8 source textures, found " + sourceCount);
        }

        boolean kingTree = woodId.equals("king_tree");
        for (int index = 0; index < 8; index++) {
            Path source = sourceDirectory.resolve((kingTree ? index : index + 1) + ".png");
            Path target = targetDirectory.resolve(index + ".png");
            if (!Files.isRegularFile(target) || Files.mismatch(source, target) != -1L) {
                throw new IllegalStateException("Leaf texture mismatch: " + source + " -> " + target);
            }
        }

        Path description = DESCRIPTION_ROOT.resolve(woodId + "_leaves.json");
        String json = Files.readString(description);
        for (int index = 0; index < 8; index++) {
            String texture = "eden_realm:particle/leaves/" + woodId + "/" + index;
            if (!json.contains("\"" + texture + "\"")) {
                throw new IllegalStateException("Missing particle texture reference: " + texture);
            }
        }
    }

    private static void verifyBindings() throws IOException {
        String particleTypes = Files.readString(Path.of(
                "src/main/java/com/kltyton/eden_realm/registry/ERParticleTypes.java"));
        String blocks = Files.readString(Path.of(
                "src/main/java/com/kltyton/eden_realm/registry/ERBlocks.java"));
        String clientEvents = Files.readString(Path.of(
                "src/main/java/com/kltyton/eden_realm/client/ERClientEvents.java"));
        if (!particleTypes.contains("new SimpleParticleType(false)")
                || !blocks.contains("new ERParticleLeavesBlock(0.01F")
                || !clientEvents.contains("registerParticleProviders(RegisterParticleProvidersEvent")) {
            throw new IllegalStateException("Leaf particle registration chain is incomplete");
        }
    }
}