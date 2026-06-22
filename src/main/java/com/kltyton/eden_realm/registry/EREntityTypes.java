package com.kltyton.eden_realm.registry;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.block.ERWoodSet;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EREntityTypes {
    private static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities(ERConstants.MOD_ID);
    private static final EnumMap<ERWoodSet, WoodEntities> WOOD_ENTITIES = new EnumMap<>(ERWoodSet.class);

    static {
        for (ERWoodSet wood : ERWoodSet.values()) {
            WOOD_ENTITIES.put(wood, registerWoodEntities(wood));
        }
    }

    private EREntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }

    public static DeferredHolder<EntityType<?>, EntityType<Boat>> boat(ERWoodSet wood) {
        return WOOD_ENTITIES.get(wood).boat();
    }

    public static DeferredHolder<EntityType<?>, EntityType<ChestBoat>> chestBoat(ERWoodSet wood) {
        return WOOD_ENTITIES.get(wood).chestBoat();
    }

    public static List<WoodEntities> woodEntries() {
        return List.copyOf(WOOD_ENTITIES.values());
    }

    private static WoodEntities registerWoodEntities(ERWoodSet wood) {
        DeferredHolder<EntityType<?>, EntityType<Boat>> boat = ENTITY_TYPES.registerEntityType(
                wood.boatName(),
                (type, level) -> new Boat(type, level, () -> ERItems.boatItem(wood).get()),
                MobCategory.MISC,
                builder -> builder.sized(1.375F, 0.5625F).clientTrackingRange(10));
        DeferredHolder<EntityType<?>, EntityType<ChestBoat>> chestBoat = ENTITY_TYPES.registerEntityType(
                wood.chestBoatName(),
                (type, level) -> new ChestBoat(type, level, () -> ERItems.chestBoatItem(wood).get()),
                MobCategory.MISC,
                builder -> builder.sized(1.375F, 0.5625F).clientTrackingRange(10));

        return new WoodEntities(boat, chestBoat);
    }

    public record WoodEntities(
            DeferredHolder<EntityType<?>, EntityType<Boat>> boat,
            DeferredHolder<EntityType<?>, EntityType<ChestBoat>> chestBoat) {
    }
}
