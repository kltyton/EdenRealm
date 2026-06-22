package com.kltyton.eden_realm.data.loot;

import com.kltyton.eden_realm.common.block.ERWoodSet;
import com.kltyton.eden_realm.registry.ERBlocks;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.NonNull;

public final class ERBlockLootSubProvider extends BlockLootSubProvider {
    public ERBlockLootSubProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
    }

    @Override
    protected void generate() {
        for (ERWoodSet wood : ERWoodSet.values()) {
            ERBlocks.WoodBlocks blocks = ERBlocks.woodBlocks(wood);

            dropSelf(blocks.log().get());
            dropSelf(blocks.strippedLog().get());
            dropSelf(blocks.planks().get());
            add(blocks.leaves().get(), createLeavesDrops(blocks.leaves().get(), blocks.sapling().get(), NORMAL_LEAVES_SAPLING_CHANCES));
            dropSelf(blocks.sapling().get());
            add(blocks.door().get(), createDoorTable(blocks.door().get()));
            dropSelf(blocks.trapdoor().get());
            dropSelf(blocks.sign().get());
            dropOther(blocks.wallSign().get(), blocks.sign().get());
            dropSelf(blocks.hangingSign().get());
            dropOther(blocks.wallHangingSign().get(), blocks.hangingSign().get());
        }
    }

    @Override
    protected @NonNull Iterable<Block> getKnownBlocks() {
        return ERBlocks.entries();
    }
}
