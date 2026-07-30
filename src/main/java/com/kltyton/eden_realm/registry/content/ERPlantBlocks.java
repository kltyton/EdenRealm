package com.kltyton.eden_realm.registry.content;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.block.ERSeagrassBlock;
import com.kltyton.eden_realm.common.block.ERShapedBushBlock;
import com.kltyton.eden_realm.common.block.ERShapedDryVegetationBlock;
import com.kltyton.eden_realm.common.block.ERTallSeagrassBlock;
import com.kltyton.eden_realm.common.block.plant.ERModelPlantBlock;
import com.kltyton.eden_realm.common.block.plant.ERPlantShapes;
import com.kltyton.eden_realm.common.block.plant.ERShapedDoublePlantBlock;
import com.kltyton.eden_realm.common.block.plant.ERTallWaterPlantBlock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.DryVegetationBlock;
import net.minecraft.world.level.block.LilyPadBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
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
    public static final DeferredBlock<ERSeagrassBlock> BUBBLE_GRASS = register(
            "bubble_grass",
            "Bubble Grass",
            "泡泡草",
            properties -> new ERSeagrassBlock(13.0, 5.0, properties),
            copyOf(Blocks.SEAGRASS));
    public static final DeferredBlock<ERSeagrassBlock> BLUE_COURT_SEAGRASS = register(
            "blue_court_seagrass",
            "Blue Court Seagrass",
            "蓝庭海草",
            properties -> new ERSeagrassBlock(
                    16.0, 16.0, ERPlantBlocks::tallBlueCourtSeagrassState, properties),
            copyOf(Blocks.SEAGRASS));
    public static final DeferredBlock<ERTallSeagrassBlock> TALL_BLUE_COURT_SEAGRASS = registerWithoutItem(
            "tall_blue_court_seagrass",
            "Tall Blue Court Seagrass",
            "高蓝庭海草",
            properties -> new ERTallSeagrassBlock(
                    16.0, 16.0, ERPlantBlocks::blueCourtSeagrassState, properties),
            copyOf(Blocks.TALL_SEAGRASS));
    public static final DeferredBlock<CarpetBlock> ROTTING_WOOD_FUNGUS_MAT = register(
            "rotting_wood_fungus_mat",
            "Rotting Wood Fungus Mat",
            "腐木菌毯",
            CarpetBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.MOSS_CARPET).noCollision().noOcclusion());
    public static final DeferredBlock<ERShapedBushBlock> DUSKY_PURPLE_FOREST_FLOWER = register(
            "dusky_purple_forest_flower",
            "Dusky Purple Forest Flower",
            "幽紫林花",
            properties -> new ERShapedBushBlock(14.0, 10.0, properties),
            copyOf(Blocks.DANDELION));
    public static final DeferredBlock<ERShapedBushBlock> SPIKE_GRASS_FLOWER = register(
            "spike_grass_flower",
            "Spike Grass Flower",
            "穗草花",
            properties -> new ERShapedBushBlock(9.0, 14.0, properties),
            copyOf(Blocks.DANDELION));
    public static final DeferredBlock<ERShapedBushBlock> MOSSBORN_FLOWER = register(
            "mossborn_flower",
            "Mossborn Flower",
            "苔生花",
            properties -> new ERShapedBushBlock(14.0, 5.0, properties),
            copyOf(Blocks.DANDELION));
    public static final DeferredBlock<ERShapedBushBlock> BLUEBELL = register(
            "bluebell",
            "Bluebell",
            "蓝铃花",
            properties -> new ERShapedBushBlock(9.0, 14.0, properties),
            copyOf(Blocks.DANDELION));
    public static final DeferredBlock<ERShapedBushBlock> GOLDEN_STAMEN_FLOWER = register(
            "golden_stamen_flower",
            "Golden Stamen Flower",
            "金蕊花",
            properties -> new ERShapedBushBlock(10.0, 13.0, properties),
            copyOf(Blocks.DANDELION));
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
    public static final DeferredBlock<ERShapedBushBlock> MOONWHITE_ORCHID = register(
            "moonwhite_orchid",
            "Moonwhite Orchid",
            "月白兰",
            properties -> new ERShapedBushBlock(10.0, 12.0, properties),
            copyOf(Blocks.DANDELION));
    public static final DeferredBlock<ERSeagrassBlock> WATER_FERN = register(
            "water_fern",
            "Water Fern",
            "水蕨",
            properties -> new ERSeagrassBlock(14.0, 10.0, properties),
            copyOf(Blocks.SEAGRASS));
    public static final DeferredBlock<ERShapedBushBlock> LONGLEAF_SEDGE = register(
            "longleaf_sedge",
            "Longleaf Sedge",
            "长叶莎草",
            properties -> new ERShapedBushBlock(12.0, 14.0, properties),
            copyOf(Blocks.SHORT_GRASS));
    public static final DeferredBlock<ERShapedBushBlock> GREEN_SPIKE_GRASS = register(
            "green_spike_grass",
            "Green Spike Grass",
            "青穗草",
            properties -> new ERShapedBushBlock(12.0, 13.0, properties),
            copyOf(Blocks.SHORT_GRASS));

    public static final DeferredBlock<ERShapedDoublePlantBlock> GOLDEN_SPIKE_GRASS = register(
            "golden_spike_grass",
            "Golden Spike Grass",
            "金穗草",
            properties -> new ERShapedDoublePlantBlock(ERPlantShapes.GOLDEN_SPIKE_GRASS, properties),
            copyOf(Blocks.TALL_GRASS));
    public static final DeferredBlock<ERTallWaterPlantBlock> PURPLE_GLOW_CATTAIL = register(
            "purple_glow_cattail",
            "Purple Glow Cattail",
            "紫光香蒲",
            properties -> new ERTallWaterPlantBlock(false, ERPlantShapes.PURPLE_GLOW_CATTAIL, properties),
            copyOf(Blocks.TALL_GRASS));
    public static final DeferredBlock<ERTallWaterPlantBlock> GRAY_SPIKE_REED = register(
            "gray_spike_reed",
            "Gray Spike Reed",
            "灰穗芦苇",
            properties -> new ERTallWaterPlantBlock(false, ERPlantShapes.GRAY_SPIKE_REED, properties),
            copyOf(Blocks.TALL_GRASS));
    public static final DeferredBlock<ERTallWaterPlantBlock> WATER_SCALLION = register(
            "water_scallion",
            "Water Scallion",
            "水葱",
            properties -> new ERTallWaterPlantBlock(false, ERPlantShapes.WATER_SCALLION, properties),
            copyOf(Blocks.TALL_GRASS));
    public static final DeferredBlock<ERTallWaterPlantBlock> UMBRELLA_HYGROPHILA = register(
            "umbrella_hygrophila",
            "Umbrella Hygrophila",
            "伞花水蓑衣",
            properties -> new ERTallWaterPlantBlock(true, ERPlantShapes.UMBRELLA_HYGROPHILA, properties),
            copyOf(Blocks.TALL_SEAGRASS));

    public static final DeferredBlock<ERModelPlantBlock> SMALL_PARASOL_MUSHROOM = register(
            "small_parasol_mushroom",
            "Small Parasol Mushroom",
            "小伞菇",
            properties -> new ERModelPlantBlock(
                    models(
                            "small_parasol_mushroom_1",
                            "small_parasol_mushroom_2",
                            "small_parasol_mushroom_3"),
                    properties),
            collidableFungusProperties(MapColor.COLOR_BROWN));
    public static final DeferredBlock<ERModelPlantBlock> CRUMBLY_MUSHROOM = register(
            "crumbly_mushroom",
            "Crumbly Mushroom",
            "掉渣菇",
            properties -> new ERModelPlantBlock(
                    models(
                            "crumbly_mushroom_1",
                            "crumbly_mushroom_2",
                            "crumbly_mushroom_3"),
                    properties),
            collidableFungusProperties(MapColor.COLOR_BROWN));
    public static final DeferredBlock<ERModelPlantBlock> BLUE_GLOW_MUSHROOM = register(
            "blue_glow_mushroom",
            "Blue Glow Mushroom",
            "蓝荧菇",
            properties -> new ERModelPlantBlock(
                    models(
                            "blue_glow_mushroom_1",
                            "blue_glow_mushroom_2",
                            "blue_glow_mushroom_3"),
                    properties),
            collidableFungusProperties(MapColor.COLOR_BLUE));

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

    private static Supplier<BlockBehaviour.Properties> collidableFungusProperties(MapColor mapColor) {
        return () -> BlockBehaviour.Properties.of()
                .mapColor(mapColor)
                .noOcclusion()
                .instabreak()
                .sound(SoundType.GRASS)
                .pushReaction(PushReaction.DESTROY);
    }

    private static List<Identifier> models(String... names) {
        return Arrays.stream(names)
                .map(name -> ERConstants.id("block/" + name))
                .toList();
    }

    private static <T extends Block> DeferredBlock<T> register(
            String id,
            String englishName,
            String chineseName,
            Function<BlockBehaviour.Properties, ? extends T> factory,
            Supplier<BlockBehaviour.Properties> properties) {
        return register(id, englishName, chineseName, factory, properties, true);
    }

    private static <T extends Block> DeferredBlock<T> registerWithoutItem(
            String id,
            String englishName,
            String chineseName,
            Function<BlockBehaviour.Properties, ? extends T> factory,
            Supplier<BlockBehaviour.Properties> properties) {
        return register(id, englishName, chineseName, factory, properties, false);
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

    private static BlockState blueCourtSeagrassState() {
        return BLUE_COURT_SEAGRASS.get().defaultBlockState();
    }

    private static BlockState tallBlueCourtSeagrassState() {
        return TALL_BLUE_COURT_SEAGRASS.get().defaultBlockState();
    }
}
