package com.kltyton.eden_realm.mixin.client;

import com.kltyton.eden_realm.client.command.IdentifierTranslation;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Localizes the displayed locate result while preserving coordinates and the original ID. */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @ModifyVariable(method = "addServerSystemMessage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Component edenRealm$translateLocateFeedback(Component message) {
        return IdentifierTranslation.locateFeedback(message);
    }
}
