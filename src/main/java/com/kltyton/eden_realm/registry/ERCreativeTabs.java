package com.kltyton.eden_realm.registry;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.block.ERWoodSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ERCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ERConstants.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EDEN_REALM = TABS.register(
            "eden_realm",
            id -> new CreativeModeTab.Builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemGroup.eden_realm.eden_realm"))
                    .icon(() -> new ItemStack(ERItems.woodItems(ERWoodSet.ICE_CRYSTAL_PINE).sapling().get()))
                    .displayItems((parameters, output) -> {
                        for (ERWoodSet wood : ERWoodSet.values()) {
                            ERItems.WoodItems items = ERItems.woodItems(wood);
                            output.accept(items.log().get());
                            output.accept(items.strippedLog().get());
                            output.accept(items.planks().get());
                            output.accept(items.stairs().get());
                            output.accept(items.slab().get());
                            output.accept(items.fence().get());
                            output.accept(items.fenceGate().get());
                            output.accept(items.button().get());
                            output.accept(items.pressurePlate().get());
                            output.accept(items.leaves().get());
                            output.accept(items.sapling().get());
                            output.accept(items.door().get());
                            output.accept(items.trapdoor().get());
                            output.accept(items.sign().get());
                            output.accept(items.hangingSign().get());
                            output.accept(items.boat().get());
                            output.accept(items.chestBoat().get());
                        }
                    })
                    .build());

    private ERCreativeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        TABS.register(modEventBus);
    }
}
