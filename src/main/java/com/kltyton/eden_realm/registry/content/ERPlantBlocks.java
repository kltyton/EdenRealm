package com.kltyton.eden_realm.registry.content;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.block.ERShapedBushBlock;
import com.kltyton.eden_realm.common.block.ERShapedDryVegetationBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.DryVegetationBlock;
import net.minecraft.world.level.block.LilyPadBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ERPlantBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ERConstants.MOD_ID);
    private static final List<ERBlockEntry> ENTRIES = new ArrayList<>();

    public static final DeferredBlock<ERShapedBushBlock> FROST_CRYSTAL_GRASS = register(
            "frost_crystal_grass",
            "Frost Crystal Grass",
            "寒晶草",
            properties -> new ERShapedBushBlock(12.0, 13.0, properties),
            copyOf(Blocks.SHORT_GRASS));
    public static final DeferredBlock<ERShapedBushBlock> FROST_DOWN_FLOWER = register(
            "frost_down_flower",
            "Frost Down Flower",
            "霜绒花",
            properties -> new ERShapedBushBlock(9.0, 12.0, properties),
            copyOf(Blocks.DANDELION));
    public static final DeferredBlock<LilyPadBlock> DUCKWEED = register(
            "duckweed", "Duckweed", "浮萍", LilyPadBlock::new, copyOf(Blocks.LILY_PAD));
    public static final DeferredBlock<CarpetBlock> ROTTING_WOOD_FUNGUS_MAT = register(
            "rotting_wood_fungus_mat",
            "Rotting Wood Fungus Mat",
            "腐木菌毯",
            CarpetBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.MOSS_CARPET).noOcclusion());
    public static final DeferredBlock<DryVegetationBlock> DROUGHT_RESISTANT_SHORT_GRASS = register(
            "drought_resistant_short_grass",
            "Drought-Resistant Short Grass",
            "耐旱短草",
            properties -> new ERShapedDryVegetationBlock(14.0, 6.0, properties),
            copyOf(Blocks.SHORT_DRY_GRASS));
    public static final DeferredBlock<DryVegetationBlock> THORN_BRANCH_BUSH = register(
            "thorn_branch_bush",
            "Thorn Branch Bush",
            "刺枝灌木",
            properties -> new ERShapedDryVegetationBlock(14.0, 10.0, properties),
            copyOf(Blocks.DEAD_BUSH));
    public static final DeferredBlock<DryVegetationBlock> SANDLAND_SHORT_GRASS = register(
            "sandland_short_grass",
            "Sandland Short Grass",
            "沙原短草",
            properties -> new ERShapedDryVegetationBlock(14.0, 5.0, properties),
            copyOf(Blocks.SHORT_DRY_GRASS));

    private ERPlantBlocks() {
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

    private static Supplier<BlockBehaviour.Properties> copyOf(Block source) {
        return () -> BlockBehaviour.Properties.ofFullCopy(source);
    }

    private static <T extends Block> DeferredBlock<T> register(
            String id,
            String englishName,
            String chineseName,
            Function<BlockBehaviour.Properties, ? extends T> factory,
            Supplier<BlockBehaviour.Properties> properties) {
        DeferredBlock<T> block = BLOCKS.registerBlock(id, factory, properties);
        ENTRIES.add(new ERBlockEntry(id, englishName, chineseName, block, true));
        return block;
    }
}
