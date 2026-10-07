package com.kltyton.eden_realm.mixin.client;

import com.kltyton.eden_realm.client.world.TerrainWorldCreation;
import com.kltyton.eden_realm.client.world.TerrainEditorScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Saves the selected Eden terrain profiles into the new world's dimension generator. */
@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {
    @Shadow private MenuTabBar tabNavigationBar;

    @Inject(method = "init", at = @At("TAIL"))
    private void edenRealm$addTerrainWorkbench(CallbackInfo callback) {
        CreateWorldScreen screen = (CreateWorldScreen) (Object) this;
        GridLayout worldOptions = (GridLayout) tabNavigationBar.getTabs().get(1).getLayout();
        worldOptions.addChild(Button.builder(
                Component.translatable("screen.eden_realm.terrain.title"),
                button -> TerrainEditorScreen.open(screen)).width(310).build(), 3, 0, 1, 2);
        screen.repositionElements();
        TerrainWorldCreation.prewarm(screen);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void edenRealm$prewarmTerrainFrame(CallbackInfo callback) {
        TerrainWorldCreation.tickPreview((CreateWorldScreen) (Object) this);
        TerrainWorldCreation.tickCreation((CreateWorldScreen) (Object) this);
    }

    @Inject(method = "popScreen", at = @At("HEAD"))
    private void edenRealm$releaseTerrainPreview(CallbackInfo callback) {
        TerrainWorldCreation.releasePreview((CreateWorldScreen) (Object) this);
    }

    @Inject(method = "onCreate", at = @At("HEAD"), cancellable = true)
    private void edenRealm$applyTerrainProfiles(CallbackInfo callback) {
        TerrainWorldCreation.releasePreview((CreateWorldScreen) (Object) this);
        if (!TerrainWorldCreation.prepareCreation((CreateWorldScreen) (Object) this)) {
            callback.cancel();
            return;
        }
        TerrainWorldCreation.apply((CreateWorldScreen) (Object) this);
    }

    @ModifyArg(method = "createNewWorld", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/screens/worldselection/WorldOpenFlows;createLevelFromExistingSettings(" +
                    "Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;" +
                    "Lnet/minecraft/server/ReloadableServerResources;" +
                    "Lnet/minecraft/core/LayeredRegistryAccess;" +
                    "Lnet/minecraft/world/level/storage/LevelDataAndDimensions$WorldDataAndGenSettings;" +
                    "Ljava/util/Optional;)V"), index = 0)
    private LevelStorageSource.LevelStorageAccess edenRealm$saveTerrainPack(
            LevelStorageSource.LevelStorageAccess access) {
        TerrainWorldCreation.writeWorldProfile((CreateWorldScreen) (Object) this, access);
        return access;
    }
}
