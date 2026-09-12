package com.kltyton.eden_realm.data.loot;

import com.kltyton.eden_realm.registry.EREntityTypes;
import java.util.function.BiConsumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

public final class EREntityLootSubProvider implements LootTableSubProvider {
    public EREntityLootSubProvider(HolderLookup.Provider registries) {
    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        output.accept(
                EREntityTypes.MOSS_STONE_COLOSSUS.get().getDefaultLootTable().orElseThrow(),
                LootTable.lootTable());
    }
}
