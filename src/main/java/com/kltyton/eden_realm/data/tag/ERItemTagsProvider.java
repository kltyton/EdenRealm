package com.kltyton.eden_realm.data.tag;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.block.ERWoodSet;
import com.kltyton.eden_realm.registry.ERItems;
import com.kltyton.eden_realm.util.ERTags;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ItemTagsProvider;
import org.jspecify.annotations.NonNull;

public final class ERItemTagsProvider extends ItemTagsProvider {
    public ERItemTagsProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            CompletableFuture<TagsProvider.TagLookup<Block>> blockTags) {
        super(output, lookupProvider, ERConstants.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.@NonNull Provider registries) {
        for (ERWoodSet wood : ERWoodSet.values()) {
            ERItems.WoodItems items = ERItems.woodItems(wood);

            tag(ERTags.Items.EDEN_REALM_LOGS)
                    .add(items.log().getKey())
                    .add(items.strippedLog().getKey());
            tag(ERTags.Items.EDEN_REALM_PLANKS)
                    .add(items.planks().getKey());
            tag(ERTags.Items.EDEN_REALM_LEAVES)
                    .add(items.leaves().getKey());
            tag(ERTags.Items.EDEN_REALM_SAPLINGS)
                    .add(items.sapling().getKey());
            tag(ERTags.Items.EDEN_REALM_BOATS)
                    .add(items.boat().getKey());
            tag(ERTags.Items.EDEN_REALM_CHEST_BOATS)
                    .add(items.chestBoat().getKey());

            tag(ItemTags.LOGS)
                    .add(items.log().getKey())
                    .add(items.strippedLog().getKey());
            tag(ItemTags.LOGS_THAT_BURN)
                    .add(items.log().getKey())
                    .add(items.strippedLog().getKey());
            tag(ItemTags.PLANKS)
                    .add(items.planks().getKey());
            tag(ItemTags.LEAVES)
                    .add(items.leaves().getKey());
            tag(ItemTags.SAPLINGS)
                    .add(items.sapling().getKey());
            tag(ItemTags.WOODEN_DOORS)
                    .add(items.door().getKey());
            tag(ItemTags.WOODEN_TRAPDOORS)
                    .add(items.trapdoor().getKey());
            tag(ItemTags.SIGNS)
                    .add(items.sign().getKey());
            tag(ItemTags.HANGING_SIGNS)
                    .add(items.hangingSign().getKey());
            tag(ItemTags.BOATS)
                    .add(items.boat().getKey());
            tag(ItemTags.CHEST_BOATS)
                    .add(items.chestBoat().getKey());
        }
    }
}
