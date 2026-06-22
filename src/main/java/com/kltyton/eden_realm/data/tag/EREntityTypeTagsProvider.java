package com.kltyton.eden_realm.data.tag;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.block.ERWoodSet;
import com.kltyton.eden_realm.registry.EREntityTypes;
import com.kltyton.eden_realm.util.ERTags;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.tags.EntityTypeTags;

public final class EREntityTypeTagsProvider extends EntityTypeTagsProvider {
    public EREntityTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, ERConstants.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        for (ERWoodSet wood : ERWoodSet.values()) {
            tag(ERTags.EntityTypes.EDEN_REALM_BOATS)
                    .add(EREntityTypes.boat(wood).getKey());
            tag(ERTags.EntityTypes.EDEN_REALM_CHEST_BOATS)
                    .add(EREntityTypes.chestBoat(wood).getKey());
            tag(EntityTypeTags.BOAT)
                    .add(EREntityTypes.boat(wood).getKey());
        }
    }
}
