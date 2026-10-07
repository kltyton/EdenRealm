package com.kltyton.eden_realm.mixin.client;

import com.kltyton.eden_realm.client.command.IdentifierTranslation;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds localized hover names to biome, structure and feature identifier suggestions. */
@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {
    @Shadow @Final private EditBox input;
    @Shadow private @Nullable CompletableFuture<Suggestions> pendingSuggestions;

    @Inject(method = "showSuggestions", at = @At("HEAD"))
    private void edenRealm$translateSuggestions(boolean immediateNarration, CallbackInfo callback) {
        if (pendingSuggestions != null && pendingSuggestions.isDone()) {
            Suggestions original = pendingSuggestions.join();
            Suggestions translated = IdentifierTranslation.suggestions(original, input.getValue());
            if (translated != original) {
                pendingSuggestions = CompletableFuture.completedFuture(translated);
            }
        }
    }
}
