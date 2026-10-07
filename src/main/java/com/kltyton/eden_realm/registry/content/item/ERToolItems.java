package com.kltyton.eden_realm.registry.content.item;

import com.kltyton.eden_realm.ERConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ToolMaterial;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ERToolItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ERConstants.MOD_ID);
    private static final List<Entry> ENTRIES = new ArrayList<>();
    public static final TagKey<Item> ROCK_STEEL_REPAIR = TagKey.create(Registries.ITEM, ERConstants.id("rock_steel_tool_materials"));
    public static final TagKey<Item> SINKING_STAR_REPAIR = TagKey.create(Registries.ITEM, ERConstants.id("sinking_star_tool_materials"));
    public static final ToolMaterial ROCK_STEEL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 850, 7.5F, 0, ToolMaterial.DIAMOND.enchantmentValue(), ROCK_STEEL_REPAIR);
    public static final ToolMaterial SINKING_STAR = new ToolMaterial(
            BlockTags.INCORRECT_FOR_IRON_TOOL, 650, 7, 0, ToolMaterial.IRON.enchantmentValue(), SINKING_STAR_REPAIR);

    // Baselines exclude the player's 1 attack damage and 4 attack speed.
    public static final DeferredItem<Item> ROCK_STEEL_SWORD = tool("rock_steel_sword", "Rock Steel Sword", "岩钢剑", ItemTags.SWORDS,
            p -> new Item(p.sword(ROCK_STEEL, 5.8F, -2.4F)));
    public static final DeferredItem<Item> ROCK_STEEL_PICKAXE = tool("rock_steel_pickaxe", "Rock Steel Pickaxe", "岩钢镐", ItemTags.PICKAXES,
            p -> new Item(p.pickaxe(ROCK_STEEL, 3.5F, -2.8F)));
    public static final DeferredItem<Item> ROCK_STEEL_AXE = tool("rock_steel_axe", "Rock Steel Axe", "岩钢斧", ItemTags.AXES,
            p -> new AxeItem(ROCK_STEEL, 8.5F, -3.1F, p));
    public static final DeferredItem<Item> ROCK_STEEL_SHOVEL = tool("rock_steel_shovel", "Rock Steel Shovel", "岩钢锹", ItemTags.SHOVELS,
            p -> new ShovelItem(ROCK_STEEL, 4, -3, p));
    public static final DeferredItem<Item> ROCK_STEEL_HOE = tool("rock_steel_hoe", "Rock Steel Hoe", "岩钢锄", ItemTags.HOES,
            p -> new HoeItem(ROCK_STEEL, 0, -1, p));
    public static final DeferredItem<Item> SINKING_STAR_SWORD = tool("sinking_star_sword", "Sinking Star Sword", "沉星剑", ItemTags.SWORDS,
            p -> new Item(p.sword(SINKING_STAR, 5.5F, -2.3F)));
    public static final DeferredItem<Item> SINKING_STAR_PICKAXE = tool("sinking_star_pickaxe", "Sinking Star Pickaxe", "沉星镐", ItemTags.PICKAXES,
            p -> new Item(p.pickaxe(SINKING_STAR, 3, -2.8F)));
    public static final DeferredItem<Item> SINKING_STAR_AXE = tool("sinking_star_axe", "Sinking Star Axe", "沉星斧", ItemTags.AXES,
            p -> new AxeItem(SINKING_STAR, 8, -3, p));
    public static final DeferredItem<Item> SINKING_STAR_SHOVEL = tool("sinking_star_shovel", "Sinking Star Shovel", "沉星锹", ItemTags.SHOVELS,
            p -> new ShovelItem(SINKING_STAR, 3.5F, -3, p));
    public static final DeferredItem<Item> SINKING_STAR_HOE = tool("sinking_star_hoe", "Sinking Star Hoe", "沉星锄", ItemTags.HOES,
            p -> new HoeItem(SINKING_STAR, 0, -0.8F, p));

    private ERToolItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    public static List<Entry> entries() {
        return List.copyOf(ENTRIES);
    }

    private static DeferredItem<Item> tool(String id, String english, String chinese, TagKey<Item> tag,
            Function<Item.Properties, Item> factory) {
        DeferredItem<Item> item = ITEMS.registerItem(id, factory);
        ENTRIES.add(new Entry(item, english, chinese, tag));
        return item;
    }

    public record Entry(DeferredItem<Item> item, String english, String chinese, TagKey<Item> tag) {
    }
}
