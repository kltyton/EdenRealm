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
import org.jspecify.annotations.NonNull;

public final class ERRecipeProvider extends RecipeProvider {
    private ERRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        for (ERWoodSet wood : ERWoodSet.values()) {
            ERBlocks.WoodBlocks blocks = ERBlocks.woodBlocks(wood);
            ERItems.WoodItems items = ERItems.woodItems(wood);

            shaped(RecipeCategory.BUILDING_BLOCKS, blocks.wood().get(), 3)
                    .pattern("LL")
                    .pattern("LL")
                    .define('L', blocks.log().get())
                    .unlockedBy(getHasName(blocks.log().get()), has(blocks.log().get()))
                    .save(output, ERConstants.recipeKey(wood.woodName()));

            shaped(RecipeCategory.BUILDING_BLOCKS, blocks.strippedWood().get(), 3)
                    .pattern("LL")
                    .pattern("LL")
                    .define('L', blocks.strippedLog().get())
                    .unlockedBy(getHasName(blocks.strippedLog().get()), has(blocks.strippedLog().get()))
                    .save(output, ERConstants.recipeKey(wood.strippedWoodName()));

            shapeless(RecipeCategory.BUILDING_BLOCKS, blocks.planks().get(), 4)
                    .requires(blocks.log().get())
                    .unlockedBy(getHasName(blocks.log().get()), has(blocks.log().get()))
                    .save(output, ERConstants.recipeKey(wood.planksName() + "_from_log"));

            shapeless(RecipeCategory.BUILDING_BLOCKS, blocks.planks().get(), 4)
                    .requires(blocks.wood().get())
                    .unlockedBy(getHasName(blocks.wood().get()), has(blocks.wood().get()))
                    .save(output, ERConstants.recipeKey(wood.planksName() + "_from_wood"));

            shapeless(RecipeCategory.BUILDING_BLOCKS, blocks.planks().get(), 4)
                    .requires(blocks.strippedLog().get())
                    .unlockedBy(getHasName(blocks.strippedLog().get()), has(blocks.strippedLog().get()))
                    .save(output, ERConstants.recipeKey(wood.planksName() + "_from_stripped_log"));

            shapeless(RecipeCategory.BUILDING_BLOCKS, blocks.planks().get(), 4)
                    .requires(blocks.strippedWood().get())
                    .unlockedBy(getHasName(blocks.strippedWood().get()), has(blocks.strippedWood().get()))
                    .save(output, ERConstants.recipeKey(wood.planksName() + "_from_stripped_wood"));

            shaped(RecipeCategory.BUILDING_BLOCKS, blocks.stairs().get(), 4)
                    .pattern("P  ")
                    .pattern("PP ")
                    .pattern("PPP")
                    .define('P', blocks.planks().get())
                    .unlockedBy(getHasName(blocks.planks().get()), has(blocks.planks().get()))
                    .save(output, ERConstants.recipeKey(wood.stairsName()));

            shaped(RecipeCategory.BUILDING_BLOCKS, blocks.slab().get(), 6)
                    .pattern("PPP")
                    .define('P', blocks.planks().get())
                    .unlockedBy(getHasName(blocks.planks().get()), has(blocks.planks().get()))
                    .save(output, ERConstants.recipeKey(wood.slabName()));

            shaped(RecipeCategory.DECORATIONS, blocks.fence().get(), 3)
                    .pattern("PSP")
                    .pattern("PSP")
                    .define('P', blocks.planks().get())
                    .define('S', Items.STICK)
                    .unlockedBy(getHasName(blocks.planks().get()), has(blocks.planks().get()))
                    .save(output, ERConstants.recipeKey(wood.fenceName()));

            shaped(RecipeCategory.REDSTONE, blocks.fenceGate().get())
                    .pattern("SPS")
                    .pattern("SPS")
                    .define('P', blocks.planks().get())
                    .define('S', Items.STICK)
                    .unlockedBy(getHasName(blocks.planks().get()), has(blocks.planks().get()))
                    .save(output, ERConstants.recipeKey(wood.fenceGateName()));

            shapeless(RecipeCategory.REDSTONE, blocks.button().get())
                    .requires(blocks.planks().get())
                    .unlockedBy(getHasName(blocks.planks().get()), has(blocks.planks().get()))
                    .save(output, ERConstants.recipeKey(wood.buttonName()));

            shaped(RecipeCategory.REDSTONE, blocks.pressurePlate().get())
                    .pattern("PP")
                    .define('P', blocks.planks().get())
                    .unlockedBy(getHasName(blocks.planks().get()), has(blocks.planks().get()))
                    .save(output, ERConstants.recipeKey(wood.pressurePlateName()));

            shaped(RecipeCategory.DECORATIONS, blocks.shelf().get(), 6)
                    .pattern("###")
                    .pattern("   ")
                    .pattern("###")
                    .define('#', blocks.strippedLog().get())
                    .group("shelf")
                    .unlockedBy(getHasName(blocks.strippedLog().get()), has(blocks.strippedLog().get()))
                    .save(output, ERConstants.recipeKey(wood.shelfName()));

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
        protected @NonNull RecipeProvider createRecipeProvider(HolderLookup.@NonNull Provider registries, @NonNull RecipeOutput output) {
            return new ERRecipeProvider(registries, output);
        }

        @Override
        public @NonNull String getName() {
            return "Eden Realm Recipes";
        }
    }
}
