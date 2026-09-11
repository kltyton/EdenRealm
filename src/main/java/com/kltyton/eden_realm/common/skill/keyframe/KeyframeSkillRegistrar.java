package com.kltyton.eden_realm.common.skill.keyframe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

public final class KeyframeSkillRegistrar {
    public static final int MAX_FIELD_LENGTH = 128;

    private final List<KeyframeSkillRegistration> registrations = new ArrayList<>();

    public KeyframeSkillRegistration point(
            String skillId,
            String controllerName,
            String animationName,
            String marker,
            double markerTimeSeconds,
            Consumer<KeyframeSkillContext> handler) {
        KeyframeSkillRegistration registration = new KeyframeSkillRegistration(
                normalizeToken(skillId, "skill id"),
                requireBounded(controllerName, "controller name"),
                requireBounded(animationName, "animation name"),
                normalizeToken(marker, "marker"),
                requireFiniteNonNegative(markerTimeSeconds),
                Objects.requireNonNull(handler, "handler"));
        boolean duplicate = registrations.stream().anyMatch(existing ->
                existing.controllerName().equals(registration.controllerName())
                        && existing.animationName().equals(registration.animationName())
                        && existing.marker().equals(registration.marker()));
        if (duplicate) {
            throw new IllegalArgumentException("Duplicate keyframe skill marker: " + registration.marker());
        }
        registrations.add(registration);
        return registration;
    }

    public List<KeyframeSkillRegistration> registrations() {
        return List.copyOf(registrations);
    }

    public static String normalizeMarker(String marker) {
        return normalizeToken(marker, "marker");
    }

    private static String normalizeToken(String value, String label) {
        String normalized = Objects.requireNonNullElse(value, "").trim().toLowerCase(Locale.ROOT);
        while (normalized.endsWith(";")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        if (normalized.isBlank() || normalized.length() > MAX_FIELD_LENGTH
                || !normalized.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("Invalid keyframe skill " + label + ": " + value);
        }
        return normalized;
    }

    private static String requireBounded(String value, String label) {
        String normalized = Objects.requireNonNullElse(value, "").trim();
        if (normalized.isBlank() || normalized.length() > MAX_FIELD_LENGTH) {
            throw new IllegalArgumentException("Invalid keyframe skill " + label + ": " + value);
        }
        return normalized;
    }

    private static double requireFiniteNonNegative(double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException("Invalid keyframe marker time: " + value);
        }
        return value;
    }
}
