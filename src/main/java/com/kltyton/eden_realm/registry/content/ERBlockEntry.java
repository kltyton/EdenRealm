package com.kltyton.eden_realm.registry.content;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;

public record ERBlockEntry(
        String id,
        String englishName,
        String chineseName,
        DeferredBlock<? extends Block> block,
        boolean hasItem) {
}
