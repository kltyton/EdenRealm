package com.kltyton.eden_realm.registry.content;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.block.ERShapedBushBlock;
import com.kltyton.eden_realm.common.block.ERWoodSet;
import com.kltyton.eden_realm.common.block.plant.ERDewspikeGrainBlock;
import com.kltyton.eden_realm.common.block.plant.ERHangingFruitBlock;
import com.kltyton.eden_realm.common.block.plant.ERShapedDoublePlantBlock;
import com.kltyton.eden_realm.common.block.tree.ERFloweringLeavesBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ERHarvestBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ERConstants.MOD_ID);
    private static final List<ERBlockEntry> ENTRIES = new ArrayList<>();

    public static final DeferredBlock<ERHangingFruitBlock> TIDE_SONG_COCONUT = fruit(
            "tide_song_coconut", "Tide Song Coconut", "潮歌椰", "tide_song_flowering_leaves");
    public static final DeferredBlock<ERHangingFruitBlock> SACRED_LIGHT_FRUIT = fruit(
            "sacred_light_fruit", "Sacred Light Fruit", "圣辉果", "sacred_light_flowering_leaves");
    public static final DeferredBlock<ERHangingFruitBlock> CLOUD_CROWN_FRUIT = fruit(
            "cloud_crown_fruit", "Cloud Crown Fruit", "云冠果", "cloud_crown_flowering_leaves");
    public static final DeferredBlock<ERHangingFruitBlock> TWILIGHT_POMEGRANATE = fruit(
            "twilight_pomegranate", "Twilight Pomegranate", "暮光榴果", "twilight_pomegranate_log");
    public static final DeferredBlock<ERFloweringLeavesBlock> TIDE_SONG_FLOWERING_LEAVES = leaves(
            ERWoodSet.TIDE_SONG, "tide_song_coconut", "潮歌树开花树叶");
    public static final DeferredBlock<ERFloweringLeavesBlock> SACRED_LIGHT_FLOWERING_LEAVES = leaves(
            ERWoodSet.SACRED_LIGHT, "sacred_light_fruit", "圣辉树开花树叶");
    public static final DeferredBlock<ERFloweringLeavesBlock> CLOUD_CROWN_FLOWERING_LEAVES = leaves(
            ERWoodSet.CLOUD_CROWN, "cloud_crown_fruit", "云冠树开花树叶");
    public static final DeferredBlock<ERDewspikeGrainBlock> DEWSPIKE_GRAIN = register(
            "dewspike_grain", "Dewspike Grain", "露穗谷", ERDewspikeGrainBlock::new,
            () -> plantProperties().randomTicks(), false);
    public static final DeferredBlock<ERShapedBushBlock> WILD_STAR_PATTERN_YAM = register(
            "wild_star_pattern_yam", "Wild Star Pattern Yam", "野生星纹薯",
            properties -> new ERShapedBushBlock(15, 13, properties), ERHarvestBlocks::plantProperties, true);
    public static final DeferredBlock<ERShapedDoublePlantBlock> WILD_CRYSTAL_DEW_FRUIT = register(
            "wild_crystal_dew_fruit", "Wild Crystal Dew Fruit", "野生晶露果",
            properties -> new ERShapedDoublePlantBlock(14.2, 16, 6.11, 6.54, properties), ERHarvestBlocks::plantProperties, true);
    public static final DeferredBlock<ERShapedDoublePlantBlock> WILD_VINE_BEAN = register(
            "wild_vine_bean", "Wild Vine Bean", "野生藤豆",
            properties -> new ERShapedDoublePlantBlock(15.22, 16, 14.7, 14, properties), ERHarvestBlocks::plantProperties, true);
    public static final DeferredBlock<ERShapedDoublePlantBlock> WILD_MOON_CLOVER = register(
            "wild_moon_clover", "Wild Moon Clover", "野生月苜草",
            properties -> new ERShapedDoublePlantBlock(17.45, 16, 14.63, 6, properties), ERHarvestBlocks::plantProperties, true);

    private ERHarvestBlocks() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }

    public static List<ERBlockEntry> entries() {
        return List.copyOf(ENTRIES);
    }

    public static List<Block> blocks() {
        return ENTRIES.stream().map(entry -> (Block) entry.block().get()).toList();
    }

    public static List<DeferredBlock<ERHangingFruitBlock>> fruits() {
        return List.of(TIDE_SONG_COCONUT, SACRED_LIGHT_FRUIT, CLOUD_CROWN_FRUIT, TWILIGHT_POMEGRANATE);
    }

    public static List<DeferredBlock<ERFloweringLeavesBlock>> floweringLeaves() {
        return List.of(TIDE_SONG_FLOWERING_LEAVES, SACRED_LIGHT_FLOWERING_LEAVES, CLOUD_CROWN_FLOWERING_LEAVES);
    }

    public static List<DeferredBlock<ERShapedDoublePlantBlock>> tallWildPlants() {
        return List.of(WILD_CRYSTAL_DEW_FRUIT, WILD_VINE_BEAN, WILD_MOON_CLOVER);
    }

    private static BlockBehaviour.Properties plantProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision().noOcclusion()
                .instabreak().sound(SoundType.CROP).pushReaction(PushReaction.DESTROY);
    }

    private static DeferredBlock<ERHangingFruitBlock> fruit(String id, String english, String chinese, String support) {
        return register(id + "_hanging", english + " Growth", chinese + "生长植株",
                properties -> new ERHangingFruitBlock(ERConstants.id(support), properties),
                () -> plantProperties().randomTicks(), true);
    }

    private static DeferredBlock<ERFloweringLeavesBlock> leaves(ERWoodSet wood, String fruit, String chinese) {
        return register(wood.id() + "_flowering_leaves", wood.englishName() + " Flowering Leaves", chinese,
                properties -> new ERFloweringLeavesBlock(ERConstants.id(fruit + "_hanging"), ERConstants.id(wood.leavesName()), properties),
                () -> BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.2F).randomTicks()
                        .sound(SoundType.GRASS).noOcclusion().ignitedByLava(), true);
    }

    private static <T extends Block> DeferredBlock<T> register(String id, String english, String chinese,
            Function<BlockBehaviour.Properties, ? extends T> factory, Supplier<BlockBehaviour.Properties> properties, boolean item) {
        DeferredBlock<T> block = BLOCKS.registerBlock(id, factory, properties);
        ENTRIES.add(new ERBlockEntry(id, english, chinese, block, item));
        return block;
    }
}
