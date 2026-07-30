package com.kltyton.eden_realm.registry.content;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.block.ERGrowableGrassBlock;
import com.kltyton.eden_realm.common.block.ERHangingPlantBlock;
import com.kltyton.eden_realm.common.block.ERShapedBushBlock;
import com.kltyton.eden_realm.common.block.ERTallGrassBlock;
import com.kltyton.eden_realm.common.block.cloud.ERSinkingCloudBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ERSkyBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ERConstants.MOD_ID);
    private static final List<ERBlockEntry> ENTRIES = new ArrayList<>();

    public static final DeferredBlock<Block> CLOUD_COURT_STONE = simple(
            "cloud_court_stone", "Cloud Court Stone", "云庭石板", Blocks.SMOOTH_STONE);
    public static final DeferredBlock<RotatedPillarBlock> CLOUD_COURT_STONE_PILLAR = register(
            "cloud_court_stone_pillar",
            "Cloud Court Stone Pillar",
            "云庭石柱",
            RotatedPillarBlock::new,
            copyOf(Blocks.QUARTZ_PILLAR));
    public static final DeferredBlock<ERSinkingCloudBlock> CLOUD = register(
            "cloud",
            "Cloud",
            "云朵",
            ERSinkingCloudBlock::new,
            transparentCopyOf(Blocks.SNOW_BLOCK));
    public static final DeferredBlock<Block> SKY_POOL_STONE = simple(
            "sky_pool_stone", "Sky Pool Stone", "天池石", Blocks.STONE);
    public static final DeferredBlock<Block> DENSE_CLOUD = simple(
            "dense_cloud", "Dense Cloud", "致密云朵", Blocks.SNOW_BLOCK);
    public static final DeferredBlock<ERSinkingCloudBlock> ROSY_CLOUD = register(
            "rosy_cloud",
            "Rosy Cloud",
            "霞云朵",
            ERSinkingCloudBlock::new,
            transparentCopyOf(Blocks.SNOW_BLOCK));
    public static final DeferredBlock<Block> DENSE_ROSY_CLOUD = simple(
            "dense_rosy_cloud", "Dense Rosy Cloud", "致密霞云朵", Blocks.SNOW_BLOCK);

    public static final DeferredBlock<ERShapedBushBlock> CLOUD_EDGE_GRASS = register(
            "cloud_edge_grass",
            "Cloud Edge Grass",
            "云边草",
            properties -> new ERShapedBushBlock(14.0, 5.0, properties),
            copyOf(Blocks.SHORT_GRASS));
    public static final DeferredBlock<ERGrowableGrassBlock> SKY_WIND_GRASS = register(
            "sky_wind_grass",
            "Sky Wind Grass",
            "天风草",
            properties -> new ERGrowableGrassBlock(
                    14.0, 13.0, ERSkyBlocks::tallSkyWindGrassState, properties),
            copyOf(Blocks.SHORT_GRASS));
    public static final DeferredBlock<ERTallGrassBlock> TALL_SKY_WIND_GRASS = registerWithoutItem(
            "tall_sky_wind_grass",
            "Tall Sky Wind Grass",
            "高天风草",
            properties -> new ERTallGrassBlock(
                    14.0, 16.0, 12.0, 8.0, ERSkyBlocks::skyWindGrassState, properties),
            copyOf(Blocks.TALL_GRASS));
    public static final DeferredBlock<ERShapedBushBlock> CLOUD_CROWN_FLOWER = register(
            "cloud_crown_flower",
            "Cloud Crown Flower",
            "云冠花",
            properties -> new ERShapedBushBlock(11.0, 14.0, properties),
            copyOf(Blocks.DANDELION));
    public static final DeferredBlock<ERHangingPlantBlock> CLOUD_FLEECE_VINE = register(
            "cloud_fleece_vine",
            "Cloud Fleece Vine",
            "云绒藤",
            properties -> new ERHangingPlantBlock(15.0, 1.0, 16.0, properties),
            copyOf(Blocks.HANGING_ROOTS));

    private ERSkyBlocks() {
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

    private static Supplier<BlockBehaviour.Properties> copyOf(Block source) {
        return () -> BlockBehaviour.Properties.ofFullCopy(source);
    }

    private static Supplier<BlockBehaviour.Properties> transparentCopyOf(Block source) {
        return () -> BlockBehaviour.Properties.ofFullCopy(source)
                .noOcclusion()
                .dynamicShape()
                .forceSolidOn();
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

    private static BlockState skyWindGrassState() {
        return SKY_WIND_GRASS.get().defaultBlockState();
    }

    private static BlockState tallSkyWindGrassState() {
        return TALL_SKY_WIND_GRASS.get().defaultBlockState();
    }
}
