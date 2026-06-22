package com.kltyton.eden_realm.data.recipe;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.block.ERWoodSet;
import com.kltyton.eden_realm.registry.ERBlocks;
import com.kltyton.eden_realm.registry.ERItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;

public final class ERRecipeProvider extends RecipeProvider {
    private ERRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        for (ERWoodSet wood : ERWoodSet.values()) {
            ERBlocks.WoodBlocks blocks = ERBlocks.woodBlocks(wood);
            ERItems.WoodItems items = ERItems.woodItems(wood);

            shapeless(RecipeCategory.BUILDING_BLOCKS, blocks.planks().get(), 4)
                    .requires(blocks.log().get())
                    .unlockedBy(getHasName(blocks.log().get()), has(blocks.log().get()))
                    .save(output, ERConstants.recipeKey(wood.planksName() + "_from_log"));

            shapeless(RecipeCategory.BUILDING_BLOCKS, blocks.planks().get(), 4)
                    .requires(blocks.strippedLog().get())
                    .unlockedBy(getHasName(blocks.strippedLog().get()), has(blocks.strippedLog().get()))
                    .save(output, ERConstants.recipeKey(wood.planksName() + "_from_stripped_log"));

            shaped(RecipeCategory.REDSTONE, blocks.door().get(), 3)
                    .pattern("PP")
                    .pattern("PP")
                    .pattern("PP")
                    .define('P', blocks.planks().get())
                    .unlockedBy(getHasName(blocks.planks().get()), has(blocks.planks().get()))
                    .save(output, ERConstants.recipeKey(wood.doorName()));

            shaped(RecipeCategory.REDSTONE, blocks.trapdoor().get(), 2)
                    .pattern("PPP")
                    .pattern("PPP")
                    .define('P', blocks.planks().get())
                    .unlockedBy(getHasName(blocks.planks().get()), has(blocks.planks().get()))
                    .save(output, ERConstants.recipeKey(wood.trapdoorName()));

            shaped(RecipeCategory.TRANSPORTATION, items.boat().get())
                    .pattern("P P")
                    .pattern("PPP")
                    .define('P', blocks.planks().get())
                    .unlockedBy(getHasName(blocks.planks().get()), has(blocks.planks().get()))
                    .save(output, ERConstants.recipeKey(wood.boatName()));

            shapeless(RecipeCategory.TRANSPORTATION, items.chestBoat().get())
                    .requires(items.boat().get())
                    .requires(Items.CHEST)
                    .unlockedBy(getHasName(items.boat().get()), has(items.boat().get()))
                    .save(output, ERConstants.recipeKey(wood.chestBoatName()));
        }
    }

    public static final class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new ERRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "Eden Realm Recipes";
        }
    }
}
