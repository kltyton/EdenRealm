package com.kltyton.eden_realm.client.command;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;

/** Localizes command suggestions without changing the identifier inserted into the command. */
public final class IdentifierTranslation {
    private IdentifierTranslation() {
    }

    public static Suggestions suggestions(Suggestions original, String input) {
        String kind = suggestionKind(input);
        if (kind == null) {
            return original;
        }
        List<Suggestion> translated = new ArrayList<>(original.getList().size());
        boolean changed = false;
        for (Suggestion suggestion : original.getList()) {
            String name = suggestion.getTooltip() == null ? localizedName(kind, suggestion.getText()) : null;
            if (name == null) {
                translated.add(suggestion);
            } else {
                translated.add(new Suggestion(suggestion.getRange(), suggestion.getText(), Component.literal(name)));
                changed = true;
            }
        }
        return changed ? new Suggestions(original.getRange(), translated) : original;
    }

    public static Component locateFeedback(Component message) {
        if (!(message.getContents() instanceof TranslatableContents contents)) {
            return message;
        }
        String kind = switch (contents.getKey()) {
            case "commands.locate.biome.success" -> "biome";
            case "commands.locate.structure.success" -> "structure";
            case "commands.locate.poi.success" -> "poi";
            default -> null;
        };
        Object[] originalArgs = contents.getArgs();
        if (kind == null || originalArgs.length == 0 || !(originalArgs[0] instanceof String id)) {
            return message;
        }
        String name = localizedName(kind, id);
        if (name == null) {
            return message;
        }
        Object[] args = originalArgs.clone();
        args[0] = Component.literal(name).append(Component.literal(" (" + id + ")").withStyle(ChatFormatting.GRAY));
        MutableComponent translated = Component.translatable(contents.getKey(), args).setStyle(message.getStyle());
        message.getSiblings().forEach(translated::append);
        return translated;
    }

    private static String suggestionKind(String input) {
        String command = input.startsWith("/") ? input.substring(1) : input;
        String[] words = command.trim().split("\\s+");
        if (words.length < 2) {
            return null;
        }
        return switch (words[0]) {
            case "locate" -> switch (words[1]) {
                case "biome" -> "biome";
                case "structure" -> "structure";
                case "poi" -> "poi";
                default -> null;
            };
            case "fillbiome" -> "biome";
            case "place" -> switch (words[1]) {
                case "feature" -> "feature";
                case "structure" -> "structure";
                default -> null;
            };
            default -> null;
        };
    }

    private static String localizedName(String kind, String rawId) {
        if (rawId.startsWith("#")) {
            return null;
        }
        Identifier id = Identifier.tryParse(rawId);
        if (id == null) {
            return null;
        }
        String key = kind + "." + id.getNamespace() + "." + id.getPath().replace('/', '.');
        Language language = Language.getInstance();
        return language.has(key) ? language.getOrDefault(key) : null;
    }
}
