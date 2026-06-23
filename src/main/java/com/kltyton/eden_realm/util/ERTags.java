package com.kltyton.eden_realm.util;

import com.kltyton.eden_realm.ERConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ERTags {
    private ERTags() {
    }

    public static final class Blocks {
        public static final TagKey<Block> EDEN_REALM_LOGS = create("eden_realm_logs");
        public static final TagKey<Block> EDEN_REALM_PLANKS = create("eden_realm_planks");
        public static final TagKey<Block> EDEN_REALM_SHELVES = create("eden_realm_shelves");
        public static final TagKey<Block> EDEN_REALM_LEAVES = create("eden_realm_leaves");
        public static final TagKey<Block> EDEN_REALM_SAPLINGS = create("eden_realm_saplings");

        private Blocks() {
        }

        private static TagKey<Block> create(String path) {
            return TagKey.create(Registries.BLOCK, ERConstants.id(path));
        }
    }

    public static final class Items {
        public static final TagKey<Item> EDEN_REALM_LOGS = create("eden_realm_logs");
        public static final TagKey<Item> EDEN_REALM_PLANKS = create("eden_realm_planks");
        public static final TagKey<Item> EDEN_REALM_SHELVES = create("eden_realm_shelves");
        public static final TagKey<Item> EDEN_REALM_LEAVES = create("eden_realm_leaves");
        public static final TagKey<Item> EDEN_REALM_SAPLINGS = create("eden_realm_saplings");
        public static final TagKey<Item> EDEN_REALM_BOATS = create("eden_realm_boats");
        public static final TagKey<Item> EDEN_REALM_CHEST_BOATS = create("eden_realm_chest_boats");

        private Items() {
        }

        private static TagKey<Item> create(String path) {
            return TagKey.create(Registries.ITEM, ERConstants.id(path));
        }
    }

    public static final class EntityTypes {
        public static final TagKey<EntityType<?>> EDEN_REALM_BOATS = create("eden_realm_boats");
        public static final TagKey<EntityType<?>> EDEN_REALM_CHEST_BOATS = create("eden_realm_chest_boats");

        private EntityTypes() {
        }

        private static TagKey<EntityType<?>> create(String path) {
            return TagKey.create(Registries.ENTITY_TYPE, ERConstants.id(path));
        }
    }
}
