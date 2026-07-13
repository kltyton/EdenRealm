package com.kltyton.eden_realm.registry.content;

import com.kltyton.eden_realm.ERConstants;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.world.level.block.BaseCoralFanBlock;
import net.minecraft.world.level.block.BaseCoralPlantBlock;
import net.minecraft.world.level.block.BaseCoralWallFanBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CoralBlock;
import net.minecraft.world.level.block.CoralFanBlock;
import net.minecraft.world.level.block.CoralPlantBlock;
import net.minecraft.world.level.block.CoralWallFanBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ERCoralBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ERConstants.MOD_ID);
    private static final List<ERBlockEntry> ENTRIES = new ArrayList<>();
    private static final EnumMap<CoralSpecies, CoralFamily> FAMILIES = new EnumMap<>(CoralSpecies.class);

    static {
        for (CoralSpecies species : CoralSpecies.values()) {
            FAMILIES.put(species, registerFamily(species));
        }
    }

    private ERCoralBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }

    public static List<ERBlockEntry> entries() {
        return List.copyOf(ENTRIES);
    }

    public static List<Block> blocks() {
        return BLOCKS.getEntries().stream().map(holder -> (Block) holder.get()).toList();
    }

    public static CoralFamily family(CoralSpecies species) {
        return FAMILIES.get(species);
    }

    public static List<CoralFamily> families() {
        return List.copyOf(FAMILIES.values());
    }

    private static CoralFamily registerFamily(CoralSpecies species) {
        String id = species.id();
        String english = species.englishName();
        String chinese = species.chineseName();

        DeferredBlock<Block> deadBlock = register(
                "dead_" + id + "_coral_block",
                "Dead " + english + " Coral Block",
                "失活" + chinese + "珊瑚块",
                Block::new,
                copyOf(Blocks.DEAD_TUBE_CORAL_BLOCK),
                true);
        DeferredBlock<CoralBlock> block = register(
                id + "_coral_block",
                english + " Coral Block",
                chinese + "珊瑚块",
                properties -> new CoralBlock(deadBlock.get(), properties),
                copyOf(Blocks.TUBE_CORAL_BLOCK),
                true);
        DeferredBlock<BaseCoralPlantBlock> deadPlant = register(
                "dead_" + id + "_coral",
                "Dead " + english + " Coral",
                "失活" + chinese + "珊瑚",
                BaseCoralPlantBlock::new,
                copyOf(Blocks.DEAD_TUBE_CORAL),
                true);
        DeferredBlock<CoralPlantBlock> plant = register(
                id + "_coral",
                english + " Coral",
                chinese + "珊瑚",
                properties -> new CoralPlantBlock(deadPlant.get(), properties),
                copyOf(Blocks.TUBE_CORAL),
                true);
        DeferredBlock<BaseCoralFanBlock> deadFan = register(
                "dead_" + id + "_coral_fan",
                "Dead " + english + " Coral Fan",
                "失活" + chinese + "珊瑚扇",
                BaseCoralFanBlock::new,
                copyOf(Blocks.DEAD_TUBE_CORAL_FAN),
                true);
        DeferredBlock<CoralFanBlock> fan = register(
                id + "_coral_fan",
                english + " Coral Fan",
                chinese + "珊瑚扇",
                properties -> new CoralFanBlock(deadFan.get(), properties),
                copyOf(Blocks.TUBE_CORAL_FAN),
                true);
        DeferredBlock<BaseCoralWallFanBlock> deadWallFan = register(
                "dead_" + id + "_coral_wall_fan",
                "Dead " + english + " Coral Wall Fan",
                "墙上的失活" + chinese + "珊瑚扇",
                BaseCoralWallFanBlock::new,
                copyWithLoot(Blocks.DEAD_TUBE_CORAL_WALL_FAN, deadFan),
                false);
        DeferredBlock<CoralWallFanBlock> wallFan = register(
                id + "_coral_wall_fan",
                english + " Coral Wall Fan",
                "墙上的" + chinese + "珊瑚扇",
                properties -> new CoralWallFanBlock(deadWallFan.get(), properties),
                copyWithLoot(Blocks.TUBE_CORAL_WALL_FAN, fan),
                false);

        return new CoralFamily(deadBlock, block, deadPlant, plant, deadFan, fan, deadWallFan, wallFan);
    }

    private static Supplier<BlockBehaviour.Properties> copyOf(Block source) {
        return () -> BlockBehaviour.Properties.ofFullCopy(source);
    }

    private static Supplier<BlockBehaviour.Properties> copyWithLoot(
            Block source, Supplier<? extends Block> lootTableOwner) {
        return () -> BlockBehaviour.Properties.ofFullCopy(source)
                .overrideLootTable(lootTableOwner.get().getLootTable());
    }

    private static <T extends Block> DeferredBlock<T> register(
            String id,
            String englishName,
            String chineseName,
            Function<BlockBehaviour.Properties, ? extends T> factory,
            Supplier<BlockBehaviour.Properties> properties,
            boolean hasItem) {
        DeferredBlock<T> block = BLOCKS.registerBlock(id, factory, properties);
        ENTRIES.add(new ERBlockEntry(id, englishName, chineseName, block, hasItem));
        return block;
    }

    public enum CoralSpecies {
        HONEYCOMB("honeycomb", "Honeycomb", "蜂孔"),
        TUBE("tube", "Tube", "管状"),
        SPHERICAL("spherical", "Spherical", "球状"),
        SEA_FLOWER("sea_flower", "Sea Flower", "海花"),
        BRANCHING("branching", "Branching", "枝状"),
        FAN_SHAPED("fan_shaped", "Fan-Shaped", "扇形");

        private final String id;
        private final String englishName;
        private final String chineseName;

        CoralSpecies(String id, String englishName, String chineseName) {
            this.id = id;
            this.englishName = englishName;
            this.chineseName = chineseName;
        }

        public String id() {
            return id;
        }

        public String englishName() {
            return englishName;
        }

        public String chineseName() {
            return chineseName;
        }
    }

    public record CoralFamily(
            DeferredBlock<Block> deadBlock,
            DeferredBlock<CoralBlock> block,
            DeferredBlock<BaseCoralPlantBlock> deadPlant,
            DeferredBlock<CoralPlantBlock> plant,
            DeferredBlock<BaseCoralFanBlock> deadFan,
            DeferredBlock<CoralFanBlock> fan,
            DeferredBlock<BaseCoralWallFanBlock> deadWallFan,
            DeferredBlock<CoralWallFanBlock> wallFan) {
    }
}
