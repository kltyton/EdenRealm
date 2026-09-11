package com.kltyton.eden_realm.common.entity.boss;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public final class MossStoneColossusCheck {
    private static final Path SOUND_DIRECTORY = Path.of(
            "src/main/resources/assets/eden_realm/sounds/entity/moss_stone_colossus");

    private MossStoneColossusCheck() {
    }

    public static void main(String[] args) throws Exception {
        List<String> failures = new ArrayList<>();
        verifyEntityContract(failures);
        verifyAnimationContract(failures);
        verifySoundResources(failures);
        if (!failures.isEmpty()) {
            throw new IllegalStateException(String.join(System.lineSeparator(), failures));
        }
        System.out.println("Moss Stone Colossus checks passed");
    }

    private static void verifyEntityContract(List<String> failures) throws IOException {
        String entity = Files.readString(Path.of(
                "src/main/java/com/kltyton/eden_realm/common/entity/boss/MossStoneColossus.java"));
        String registry = Files.readString(Path.of(
                "src/main/java/com/kltyton/eden_realm/registry/EREntityTypes.java"));

        expectContains(failures, entity, "extends Monster", "dedicated Monster superclass");
        expectContains(failures, entity, "HITBOX_WIDTH = 3.4F", "model-scale hitbox width");
        expectContains(failures, entity, "HITBOX_HEIGHT = 6.0F", "model-scale hitbox height");
        expectContains(failures, entity, ".add(Attributes.MAX_HEALTH, 20.0)", "max health");
        expectContains(failures, entity, ".add(Attributes.FOLLOW_RANGE, 35.0)", "follow range");
        expectContains(failures, entity, ".add(Attributes.MOVEMENT_SPEED, 0.23)", "movement speed");
        expectContains(failures, entity, ".add(Attributes.ATTACK_DAMAGE, 3.0)", "attack damage");
        expectContains(failures, entity, ".add(Attributes.ARMOR, 2.0)", "armor");
        expectContains(failures, entity, "new MeleeAttackGoal", "basic melee AI");
        expectContains(failures, entity, "protected void playStepSound", "vanilla step-sound hook");
        expectContains(failures, registry,
                ".sized(MossStoneColossus.HITBOX_WIDTH, MossStoneColossus.HITBOX_HEIGHT)",
                "entity type model-scale dimensions");
        expectContains(failures, registry, "MossStoneColossus.createAttributes()", "dedicated attributes");

        expectAbsent(failures, entity, "extends Zombie", "zombie superclass");
        expectAbsent(failures, entity, "setBaby(", "zombie baby initialization");
        expectAbsent(failures, entity, "SPAWN_REINFORCEMENTS_CHANCE", "zombie reinforcement attribute");
        expectAbsent(failures, entity, "setSoundKeyframeHandler", "walking sound keyframe handler");
        expectAbsent(failures, entity, "handleSoundKeyframe", "walking sound keyframe callback");
        expectAbsent(failures, registry, "Zombie.createAttributes()", "zombie attribute factory");
    }

    private static void verifyAnimationContract(List<String> failures) throws IOException {
        String animation = Files.readString(Path.of(
                "src/main/resources/assets/eden_realm/geckolib/animations/entity/moss_stone_colossus.animation.json"));
        if (animation.contains("\"sound_effects\"")) {
            failures.add("Walking sounds must not be driven by GeckoLib sound keyframes");
        }
        if (!animation.contains("\"6.04\": \"death_complete\"")
                || !animation.contains("\"4.45\": \"spawn_complete\"")) {
            failures.add("Spawn and death skill markers must remain registered");
        }
        if (!animation.contains("\"particle_effects\"")) {
            failures.add("Particle keyframe tracks must remain present");
        }
    }

    private static void verifySoundResources(List<String> failures)
            throws IOException, NoSuchAlgorithmException {
        String sounds = Files.readString(Path.of("src/generated/resources/assets/eden_realm/sounds.json"));
        for (String event : List.of("step", "ambient", "hurt", "death")) {
            if (!sounds.contains("\"entity.moss_stone_colossus." + event + "\"")) {
                failures.add("Missing generated sound event: " + event);
            }
        }

        for (int index = 1; index <= 6; index++) {
            Path step = SOUND_DIRECTORY.resolve("step_" + index + ".ogg");
            byte[] header = Files.readAllBytes(step);
            String marker = new String(header, 0, Math.min(header.length, 256), StandardCharsets.US_ASCII);
            if (!marker.contains("vorbis") || marker.contains("fLaC")) {
                failures.add(step + " must be Ogg-Vorbis for Minecraft's JOrbis decoder");
            }
        }

        String placeholderHash = sha256(Path.of("参考资料/通用占位符音效.ogg"));
        for (String name : List.of("ambient.ogg", "hurt.ogg", "death.ogg")) {
            Path sound = SOUND_DIRECTORY.resolve(name);
            if (!Files.isRegularFile(sound) || !placeholderHash.equals(sha256(sound))) {
                failures.add(sound + " must be an exact copy of the generic placeholder sound");
            }
        }
    }

    private static String sha256(Path path) throws IOException, NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

    private static void expectContains(List<String> failures, String text, String expected, String name) {
        if (!text.contains(expected)) {
            failures.add("Missing " + name + ": " + expected);
        }
    }

    private static void expectAbsent(List<String> failures, String text, String forbidden, String name) {
        if (text.contains(forbidden)) {
            failures.add("Unexpected " + name + ": " + forbidden);
        }
    }
}
