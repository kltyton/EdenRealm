package com.kltyton.eden_realm.data.recipe;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.block.ERWoodSet;
import com.kltyton.eden_realm.registry.ERBlocks;
import com.kltyton.eden_realm.registry.ERItems;
import com.kltyton.eden_realm.registry.content.ERTerrainBlocks;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CookingBookCategory;
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

        shaped(RecipeCategory.BUILDING_BLOCKS, ERTerrainBlocks.RAW_ROCK_BRICKS.get(), 4)
                .pattern("RR")
                .pattern("RR")
                .define('R', ERTerrainBlocks.RAW_ROCK.get())
                .unlockedBy(getHasName(ERTerrainBlocks.RAW_ROCK.get()), has(ERTerrainBlocks.RAW_ROCK.get()))
                .save(output, ERConstants.recipeKey("raw_rock_bricks"));
        shapeless(RecipeCategory.BUILDING_BLOCKS, ERTerrainBlocks.MOSSY_RAW_ROCK_BRICKS.get())
                .requires(ERTerrainBlocks.RAW_ROCK_BRICKS.get())
                .requires(Items.VINE)
                .unlockedBy(getHasName(ERTerrainBlocks.RAW_ROCK_BRICKS.get()), has(ERTerrainBlocks.RAW_ROCK_BRICKS.get()))
                .save(output, ERConstants.recipeKey("mossy_raw_rock_bricks"));
        shaped(RecipeCategory.BUILDING_BLOCKS, ERTerrainBlocks.CHISELED_RAW_ROCK_BRICKS.get())
                .pattern("R")
                .pattern("R")
                .define('R', ERTerrainBlocks.RAW_ROCK_BRICKS.get())
                .unlockedBy(getHasName(ERTerrainBlocks.RAW_ROCK_BRICKS.get()), has(ERTerrainBlocks.RAW_ROCK_BRICKS.get()))
                .save(output, ERConstants.recipeKey("chiseled_raw_rock_bricks"));
        shaped(RecipeCategory.BUILDING_BLOCKS, ERTerrainBlocks.MOSSY_RAW_ROCK_PILLAR.get(), 2)
                .pattern("R")
                .pattern("R")
                .define('R', ERTerrainBlocks.RAW_ROCK_BRICKS.get())
                .unlockedBy(getHasName(ERTerrainBlocks.RAW_ROCK_BRICKS.get()), has(ERTerrainBlocks.RAW_ROCK_BRICKS.get()))
                .save(output, ERConstants.recipeKey("mossy_raw_rock_pillar"));
        stonecutterResultFromBase(
                RecipeCategory.BUILDING_BLOCKS,
                ERTerrainBlocks.CHISELED_RAW_ROCK_BRICKS.get(),
                ERTerrainBlocks.RAW_ROCK.get());
        stonecutterResultFromBase(
                RecipeCategory.BUILDING_BLOCKS,
                ERTerrainBlocks.MOSSY_RAW_ROCK_PILLAR.get(),
                ERTerrainBlocks.RAW_ROCK.get());
        stonecutterResultFromBase(
                RecipeCategory.BUILDING_BLOCKS,
                ERTerrainBlocks.RAW_ROCK_BRICKS.get(),
                ERTerrainBlocks.RAW_ROCK.get());

        buildSandstoneRecipes(ERTerrainBlocks.COAST);
        buildSandstoneRecipes(ERTerrainBlocks.AMBER);
        buildSandstoneRecipes(ERTerrainBlocks.OASIS);

        oreSmelting(
                List.of(ERTerrainBlocks.RAW_ROCK_COAL_ORE.get()),
                RecipeCategory.MISC,
                CookingBookCategory.MISC,
                Items.COAL,
                0.1F,
                200,
                "eden_realm_coal");
        oreBlasting(
                List.of(ERTerrainBlocks.RAW_ROCK_COAL_ORE.get()),
                RecipeCategory.MISC,
                CookingBookCategory.MISC,
                Items.COAL,
                0.1F,
                100,
                "eden_realm_coal");
        oreSmelting(
                List.of(ERTerrainBlocks.RAW_ROCK_IRON_ORE.get()),
                RecipeCategory.MISC,
                CookingBookCategory.MISC,
                Items.IRON_INGOT,
                0.7F,
                200,
                "eden_realm_iron");
        oreBlasting(
                List.of(ERTerrainBlocks.RAW_ROCK_IRON_ORE.get()),
                RecipeCategory.MISC,
                CookingBookCategory.MISC,
                Items.IRON_INGOT,
                0.7F,
                100,
                "eden_realm_iron");
    }

    private void buildSandstoneRecipes(ERTerrainBlocks.SandstoneSet set) {
        String sandstoneId = set.sandstone().getKey().identifier().getPath();
        shaped(RecipeCategory.BUILDING_BLOCKS, set.sandstone().get())
                .pattern("SS")
                .pattern("SS")
                .define('S', set.sand().get())
                .unlockedBy(getHasName(set.sand().get()), has(set.sand().get()))
                .save(output, ERConstants.recipeKey(sandstoneId));
        shaped(RecipeCategory.BUILDING_BLOCKS, set.cutSandstone().get(), 4)
                .pattern("SS")
                .pattern("SS")
                .define('S', set.sandstone().get())
                .unlockedBy(getHasName(set.sandstone().get()), has(set.sandstone().get()))
                .save(output, ERConstants.recipeKey(set.cutSandstone().getKey().identifier().getPath()));
        shaped(RecipeCategory.BUILDING_BLOCKS, set.chiseledSandstone().get())
                .pattern("S")
                .pattern("S")
                .define('S', set.cutSandstone().get())
                .unlockedBy(getHasName(set.cutSandstone().get()), has(set.cutSandstone().get()))
                .save(output, ERConstants.recipeKey(set.chiseledSandstone().getKey().identifier().getPath()));
        smeltingResultFromBase(set.smoothSandstone().get(), set.sandstone().get());
        stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, set.cutSandstone().get(), set.sandstone().get());
        stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, set.chiseledSandstone().get(), set.sandstone().get());
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
