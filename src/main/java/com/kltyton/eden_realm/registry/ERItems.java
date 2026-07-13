package com.kltyton.eden_realm.registry;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.block.ERWoodSet;
import com.kltyton.eden_realm.common.item.ERBoatItem;
import com.kltyton.eden_realm.registry.content.ERBlockEntry;
import com.kltyton.eden_realm.registry.content.ERCoralBlocks;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ERItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ERConstants.MOD_ID);
    private static final EnumMap<ERWoodSet, WoodItems> WOOD_ITEMS = new EnumMap<>(ERWoodSet.class);
    private static final Map<String, DeferredItem<BlockItem>> CONTENT_ITEMS = new LinkedHashMap<>();

    static {
        for (ERWoodSet wood : ERWoodSet.values()) {
            WOOD_ITEMS.put(wood, registerWoodItems(wood));
        }
        for (ERBlockEntry entry : ERBlocks.contentEntries()) {
            if (entry.hasItem()) {
                CONTENT_ITEMS.put(entry.id(), registerContentBlockItem(entry));
            }
        }
    }

    private ERItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    public static WoodItems woodItems(ERWoodSet wood) {
        return WOOD_ITEMS.get(wood);
    }

    public static DeferredItem<ERBoatItem> boatItem(ERWoodSet wood) {
        return WOOD_ITEMS.get(wood).boat();
    }

    public static DeferredItem<ERBoatItem> chestBoatItem(ERWoodSet wood) {
        return WOOD_ITEMS.get(wood).chestBoat();
    }

    public static List<WoodItems> woodEntries() {
        return List.copyOf(WOOD_ITEMS.values());
    }

    public static DeferredItem<BlockItem> contentItem(String id) {
        DeferredItem<BlockItem> item = CONTENT_ITEMS.get(id);
        if (item == null) {
            throw new IllegalArgumentException("Unknown Eden Realm content item: " + id);
        }
        return item;
    }

    public static List<DeferredItem<BlockItem>> contentEntries() {
        return List.copyOf(CONTENT_ITEMS.values());
    }

    private static WoodItems registerWoodItems(ERWoodSet wood) {
        ERBlocks.WoodBlocks blocks = ERBlocks.woodBlocks(wood);
        DeferredItem<BlockItem> log = registerBlockItem(wood.logName(), blocks.log());
        DeferredItem<BlockItem> woodItem = registerBlockItem(wood.woodName(), blocks.wood());
        DeferredItem<BlockItem> strippedLog = registerBlockItem(wood.strippedLogName(), blocks.strippedLog());
        DeferredItem<BlockItem> strippedWood = registerBlockItem(wood.strippedWoodName(), blocks.strippedWood());
        DeferredItem<BlockItem> planks = registerBlockItem(wood.planksName(), blocks.planks());
        DeferredItem<BlockItem> stairs = registerBlockItem(wood.stairsName(), blocks.stairs());
        DeferredItem<BlockItem> slab = registerBlockItem(wood.slabName(), blocks.slab());
        DeferredItem<BlockItem> fence = registerBlockItem(wood.fenceName(), blocks.fence());
        DeferredItem<BlockItem> fenceGate = registerBlockItem(wood.fenceGateName(), blocks.fenceGate());
        DeferredItem<BlockItem> button = registerBlockItem(wood.buttonName(), blocks.button());
        DeferredItem<BlockItem> pressurePlate = registerBlockItem(wood.pressurePlateName(), blocks.pressurePlate());
        DeferredItem<BlockItem> shelf = ITEMS.registerItem(
                wood.shelfName(),
                properties -> new BlockItem(blocks.shelf().get(), properties.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)));
        DeferredItem<BlockItem> leaves = registerBlockItem(wood.leavesName(), blocks.leaves());
        DeferredItem<BlockItem> sapling = registerBlockItem(wood.saplingName(), blocks.sapling());
        DeferredItem<BlockItem> door = registerBlockItem(wood.doorName(), blocks.door());
        DeferredItem<BlockItem> trapdoor = registerBlockItem(wood.trapdoorName(), blocks.trapdoor());
        DeferredItem<SignItem> sign = ITEMS.registerItem(
                wood.signName(),
                properties -> new SignItem(blocks.sign().get(), blocks.wallSign().get(), properties.stacksTo(16).useBlockDescriptionPrefix()));
        DeferredItem<HangingSignItem> hangingSign = ITEMS.registerItem(
                wood.hangingSignName(),
                properties -> new HangingSignItem(blocks.hangingSign().get(), blocks.wallHangingSign().get(), properties.stacksTo(16).useBlockDescriptionPrefix()));
        DeferredItem<ERBoatItem> boat = ITEMS.registerItem(
                wood.boatName(),
                properties -> new ERBoatItem(() -> EREntityTypes.boat(wood).get(), properties.stacksTo(1)));
        DeferredItem<ERBoatItem> chestBoat = ITEMS.registerItem(
                wood.chestBoatName(),
                properties -> new ERBoatItem(() -> EREntityTypes.chestBoat(wood).get(), properties.stacksTo(1)));

        return new WoodItems(log, woodItem, strippedLog, strippedWood, planks, stairs, slab, fence, fenceGate, button, pressurePlate, shelf, leaves, sapling, door, trapdoor, sign, hangingSign, boat, chestBoat);
    }

    private static DeferredItem<BlockItem> registerBlockItem(String name, Supplier<? extends Block> block) {
        return ITEMS.registerSimpleBlockItem(name, block);
    }

    private static DeferredItem<BlockItem> registerContentBlockItem(ERBlockEntry entry) {
        if (entry.id().equals("duckweed")) {
            return registerCustomBlockItem(
                    entry.id(), properties -> new PlaceOnWaterBlockItem(entry.block().get(), properties));
        }

        for (ERCoralBlocks.CoralSpecies species : ERCoralBlocks.CoralSpecies.values()) {
            ERCoralBlocks.CoralFamily family = ERCoralBlocks.family(species);
            if (entry.id().equals(species.id() + "_coral_fan")) {
                return registerStandingAndWallBlockItem(entry.id(), family.fan(), family.wallFan());
            }
            if (entry.id().equals("dead_" + species.id() + "_coral_fan")) {
                return registerStandingAndWallBlockItem(entry.id(), family.deadFan(), family.deadWallFan());
            }
        }

        return registerBlockItem(entry.id(), entry.block());
    }

    private static DeferredItem<BlockItem> registerCustomBlockItem(
            String name, Function<Item.Properties, BlockItem> factory) {
        return ITEMS.registerItem(name, factory, properties -> properties.useBlockDescriptionPrefix());
    }

    private static DeferredItem<BlockItem> registerStandingAndWallBlockItem(
            String name, Supplier<? extends Block> floorBlock, Supplier<? extends Block> wallBlock) {
        return registerCustomBlockItem(
                name,
                properties -> new StandingAndWallBlockItem(
                        floorBlock.get(), wallBlock.get(), Direction.DOWN, properties));
    }

    public record WoodItems(
            DeferredItem<BlockItem> log,
            DeferredItem<BlockItem> wood,
            DeferredItem<BlockItem> strippedLog,
            DeferredItem<BlockItem> strippedWood,
            DeferredItem<BlockItem> planks,
            DeferredItem<BlockItem> stairs,
            DeferredItem<BlockItem> slab,
            DeferredItem<BlockItem> fence,
            DeferredItem<BlockItem> fenceGate,
            DeferredItem<BlockItem> button,
            DeferredItem<BlockItem> pressurePlate,
            DeferredItem<BlockItem> shelf,
            DeferredItem<BlockItem> leaves,
            DeferredItem<BlockItem> sapling,
            DeferredItem<BlockItem> door,
            DeferredItem<BlockItem> trapdoor,
            DeferredItem<SignItem> sign,
            DeferredItem<HangingSignItem> hangingSign,
            DeferredItem<ERBoatItem> boat,
            DeferredItem<ERBoatItem> chestBoat) {
    }
}
