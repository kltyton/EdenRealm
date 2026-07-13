package com.kltyton.eden_realm.registry.content;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.block.ERDirtPathBlock;
import com.kltyton.eden_realm.common.block.ERFarmlandBlock;
import com.kltyton.eden_realm.common.block.ERGrassBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ERTerrainBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ERConstants.MOD_ID);
    private static final List<ERBlockEntry> ENTRIES = new ArrayList<>();

    public static final DeferredBlock<Block> WEATHERED_ROCK = simple("weathered_rock", "Weathered Rock", "风化岩", Blocks.STONE);
    public static final DeferredBlock<Block> ROOTED_ROCK = simple("rooted_rock", "Rooted Rock", "根岩", Blocks.STONE);
    public static final DeferredBlock<Block> BOUNDARY_ROCK = simple("boundary_rock", "Boundary Rock", "界层岩", Blocks.BEDROCK);
    public static final DeferredBlock<Block> RUBBLE = simple("rubble", "Rubble", "碎岩", Blocks.COBBLESTONE);
    public static final DeferredBlock<Block> TUNDRA_ROCK = simple("tundra_rock", "Tundra Rock", "苔原岩", Blocks.STONE);
    public static final DeferredBlock<Block> SHALE = simple("shale", "Shale", "页岩", Blocks.DEEPSLATE);
    public static final DeferredBlock<Block> RAW_ROCK = simple("raw_rock", "Raw Rock", "原岩", Blocks.STONE);
    public static final DeferredBlock<Block> CHISELED_RAW_ROCK_BRICKS = simple(
            "chiseled_raw_rock_bricks", "Chiseled Raw Rock Bricks", "雕纹原岩砖", Blocks.CHISELED_STONE_BRICKS);
    public static final DeferredBlock<RotatedPillarBlock> MOSSY_RAW_ROCK_PILLAR = register(
            "mossy_raw_rock_pillar",
            "Mossy Raw Rock Pillar",
            "原岩苔石柱",
            RotatedPillarBlock::new,
            copyOf(Blocks.QUARTZ_PILLAR));
    public static final DeferredBlock<Block> MOSSY_RAW_ROCK_BRICKS = simple(
            "mossy_raw_rock_bricks", "Mossy Raw Rock Bricks", "原岩苔砖", Blocks.MOSSY_STONE_BRICKS);
    public static final DeferredBlock<Block> RAW_ROCK_BRICKS = simple(
            "raw_rock_bricks", "Raw Rock Bricks", "原岩砖", Blocks.STONE_BRICKS);

    public static final DeferredBlock<Block> RAW_ROCK_COAL_ORE = simple(
            "raw_rock_coal_ore", "Raw Rock Coal Ore", "原岩煤矿石", Blocks.COAL_ORE);
    public static final DeferredBlock<Block> RAW_ROCK_IRON_ORE = simple(
            "raw_rock_iron_ore", "Raw Rock Iron Ore", "原岩铁矿石", Blocks.IRON_ORE);

    public static final DeferredBlock<Block> EDEN_DIRT = simple("eden_dirt", "Eden Dirt", "伊甸泥土", Blocks.DIRT);
    public static final DeferredBlock<ERGrassBlock> EDEN_GRASS_BLOCK = grass(
            "eden_grass_block", "Eden Grass Block", "伊甸草方块");
    public static final DeferredBlock<ERDirtPathBlock> EDEN_DIRT_PATH = register(
            "eden_dirt_path", "Eden Dirt Path", "伊甸草径", ERDirtPathBlock::new, copyOf(Blocks.DIRT_PATH));
    public static final DeferredBlock<ERFarmlandBlock> EDEN_FARMLAND = register(
            "eden_farmland", "Eden Farmland", "伊甸耕地", ERFarmlandBlock::new, copyOf(Blocks.FARMLAND));

    public static final DeferredBlock<Block> THIN_CLOUD_SOIL = simple(
            "thin_cloud_soil", "Thin Cloud Soil", "薄云土", Blocks.DIRT);
    public static final DeferredBlock<Block> FLOATING_ISLAND_ROCK = simple(
            "floating_island_rock", "Floating Island Rock", "浮岛岩", Blocks.STONE);
    public static final DeferredBlock<Block> SKY_PLATFORM_STONE = simple(
            "sky_platform_stone", "Sky Platform Stone", "天台石板", Blocks.SMOOTH_STONE);

    public static final DeferredBlock<Block> ICE_CRYSTAL_ROCK = simple(
            "ice_crystal_rock", "Ice Crystal Rock", "冰晶岩", Blocks.STONE);
    public static final DeferredBlock<Block> FROST_PATTERN_STONE = simple(
            "frost_pattern_stone", "Frost Pattern Stone", "霜纹石", Blocks.STONE);

    public static final DeferredBlock<Block> SEDIMENTARY_SILT = simple(
            "sedimentary_silt", "Sedimentary Silt", "沉积淤泥", Blocks.MUD);
    public static final DeferredBlock<Block> PEAT_BLOCK = simple("peat_block", "Peat Block", "泥炭块", Blocks.MUD);
    public static final DeferredBlock<Block> WET_SWAMP_SOIL = simple(
            "wet_swamp_soil", "Wet Swamp Soil", "湿沼泥土", Blocks.MUD);

    public static final SandstoneSet COAST = sandstoneSet("coast", "Coast", "海岸", 0xFFE3D19A);
    public static final DeferredBlock<Block> AMBER_CRYSTAL_BLOCK = simple(
            "amber_crystal_block", "Amber Crystal Block", "琥珀结晶", Blocks.AMETHYST_BLOCK);
    public static final SandstoneSet AMBER = sandstoneSet("amber", "Amber", "琥珀", 0xFFE4A64A);
    public static final SandstoneSet OASIS = sandstoneSet("oasis", "Oasis", "绿洲", 0xFFE6CF8D);
    public static final DeferredBlock<Block> SUN_ROCK = simple("sun_rock", "Sun Rock", "烈阳岩", Blocks.STONE);
    public static final DeferredBlock<Block> SPRING_STONE = simple(
            "spring_stone", "Spring Stone", "泉眼石", Blocks.STONE);
    public static final DeferredBlock<Block> ERODED_SANDSTONE = simple(
            "eroded_sandstone", "Eroded Sandstone", "水蚀砂岩", Blocks.SANDSTONE);

    private ERTerrainBlocks() {
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

    private static DeferredBlock<Block> simple(String id, String englishName, String chineseName, Block source) {
        return register(id, englishName, chineseName, Block::new, copyOf(source));
    }

    private static DeferredBlock<ERGrassBlock> grass(String id, String englishName, String chineseName) {
        return register(id, englishName, chineseName, ERGrassBlock::new, copyOf(Blocks.GRASS_BLOCK));
    }

    private static SandstoneSet sandstoneSet(
            String prefix, String englishPrefix, String chinesePrefix, int dustColor) {
        DeferredBlock<ColoredFallingBlock> sand = register(
                prefix + "_sand",
                englishPrefix + " Sand",
                chinesePrefix + "沙",
                properties -> new ColoredFallingBlock(new ColorRGBA(dustColor), properties),
                copyOf(Blocks.SAND));
        DeferredBlock<Block> sandstone = simple(
                prefix + "_sandstone", englishPrefix + " Sandstone", chinesePrefix + "砂岩", Blocks.SANDSTONE);
        DeferredBlock<Block> smooth = simple(
                "smooth_" + prefix + "_sandstone",
                "Smooth " + englishPrefix + " Sandstone",
                "平滑" + chinesePrefix + "砂岩",
                Blocks.SMOOTH_SANDSTONE);
        DeferredBlock<Block> cut = simple(
                "cut_" + prefix + "_sandstone",
                "Cut " + englishPrefix + " Sandstone",
                "切制" + chinesePrefix + "砂岩",
                Blocks.CUT_SANDSTONE);
        DeferredBlock<Block> chiseled = simple(
                "chiseled_" + prefix + "_sandstone",
                "Chiseled " + englishPrefix + " Sandstone",
                "雕纹" + chinesePrefix + "砂岩",
                Blocks.CHISELED_SANDSTONE);
        return new SandstoneSet(sand, sandstone, smooth, cut, chiseled);
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

    public record SandstoneSet(
            DeferredBlock<ColoredFallingBlock> sand,
            DeferredBlock<Block> sandstone,
            DeferredBlock<Block> smoothSandstone,
            DeferredBlock<Block> cutSandstone,
            DeferredBlock<Block> chiseledSandstone) {
    }
}
