package com.kltyton.eden_realm.registry;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.block.ERWoodSet;
import com.kltyton.eden_realm.common.item.ERBoatItem;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ERItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ERConstants.MOD_ID);
    private static final EnumMap<ERWoodSet, WoodItems> WOOD_ITEMS = new EnumMap<>(ERWoodSet.class);

    static {
        for (ERWoodSet wood : ERWoodSet.values()) {
            WOOD_ITEMS.put(wood, registerWoodItems(wood));
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

    private static WoodItems registerWoodItems(ERWoodSet wood) {
        ERBlocks.WoodBlocks blocks = ERBlocks.woodBlocks(wood);
        DeferredItem<BlockItem> log = registerBlockItem(wood.logName(), blocks.log());
        DeferredItem<BlockItem> strippedLog = registerBlockItem(wood.strippedLogName(), blocks.strippedLog());
        DeferredItem<BlockItem> planks = registerBlockItem(wood.planksName(), blocks.planks());
        DeferredItem<BlockItem> stairs = registerBlockItem(wood.stairsName(), blocks.stairs());
        DeferredItem<BlockItem> slab = registerBlockItem(wood.slabName(), blocks.slab());
        DeferredItem<BlockItem> fence = registerBlockItem(wood.fenceName(), blocks.fence());
        DeferredItem<BlockItem> fenceGate = registerBlockItem(wood.fenceGateName(), blocks.fenceGate());
        DeferredItem<BlockItem> button = registerBlockItem(wood.buttonName(), blocks.button());
        DeferredItem<BlockItem> pressurePlate = registerBlockItem(wood.pressurePlateName(), blocks.pressurePlate());
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

        return new WoodItems(log, strippedLog, planks, stairs, slab, fence, fenceGate, button, pressurePlate, leaves, sapling, door, trapdoor, sign, hangingSign, boat, chestBoat);
    }

    private static DeferredItem<BlockItem> registerBlockItem(String name, Supplier<? extends Block> block) {
        return ITEMS.registerSimpleBlockItem(name, block);
    }

    public record WoodItems(
            DeferredItem<BlockItem> log,
            DeferredItem<BlockItem> strippedLog,
            DeferredItem<BlockItem> planks,
            DeferredItem<BlockItem> stairs,
            DeferredItem<BlockItem> slab,
            DeferredItem<BlockItem> fence,
            DeferredItem<BlockItem> fenceGate,
            DeferredItem<BlockItem> button,
            DeferredItem<BlockItem> pressurePlate,
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
