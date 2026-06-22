package com.kltyton.eden_realm.data;

import com.kltyton.eden_realm.data.lang.EREnglishLanguageProvider;
import com.kltyton.eden_realm.data.lang.ERChineseLanguageProvider;
import com.kltyton.eden_realm.data.loot.ERLootTableProvider;
import com.kltyton.eden_realm.data.model.ERModelProvider;
import com.kltyton.eden_realm.data.recipe.ERRecipeProvider;
import com.kltyton.eden_realm.data.tag.ERBlockTagsProvider;
import com.kltyton.eden_realm.data.tag.EREntityTypeTagsProvider;
import com.kltyton.eden_realm.data.tag.ERItemTagsProvider;
import com.kltyton.eden_realm.data.worldgen.ERWorldgenProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public final class ERDataGenerators {
    private ERDataGenerators() {
    }

    public static void gatherClientData(GatherDataEvent.Client event) {
        addClientProviders(event);
        addServerProviders(event);
    }

    public static void gatherServerData(GatherDataEvent.Server event) {
        addServerProviders(event);
    }

    private static void addClientProviders(GatherDataEvent event) {
        event.createProvider(EREnglishLanguageProvider::new);
        event.createProvider(ERChineseLanguageProvider::new);
        event.createProvider(ERModelProvider::new);
    }

    private static void addServerProviders(GatherDataEvent event) {
        event.createProvider(ERRecipeProvider.Runner::new);
        event.createProvider(ERLootTableProvider::new);
        event.createBlockAndItemTags(ERBlockTagsProvider::new, ERItemTagsProvider::new);
        event.createProvider(EREntityTypeTagsProvider::new);
        event.createProvider(ERWorldgenProvider::new);
    }
}
