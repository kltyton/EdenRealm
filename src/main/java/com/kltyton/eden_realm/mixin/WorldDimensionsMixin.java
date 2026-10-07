package com.kltyton.eden_realm.mixin;

import com.kltyton.eden_realm.world.terrain.TerrainDimensions;
import net.minecraft.core.Registry;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Preserves inline Eden settings during new-world and saved-world dimension baking. */
@Mixin(WorldDimensions.class)
public abstract class WorldDimensionsMixin {
    @ModifyVariable(method = "bake", at = @At("HEAD"), argsOnly = true)
    private Registry<LevelStem> edenRealm$preserveWorldTerrain(Registry<LevelStem> datapack) {
        return TerrainDimensions.datapackDimensions(((WorldDimensions) (Object) this).dimensions(), datapack);
    }
}
