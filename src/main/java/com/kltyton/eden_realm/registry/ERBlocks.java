package com.kltyton.eden_realm.registry;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.block.ERButtonBlock;
import com.kltyton.eden_realm.common.block.ERPressurePlateBlock;
import com.kltyton.eden_realm.common.block.ERStairBlock;
import com.kltyton.eden_realm.common.block.ERWoodSet;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TintedParticleLeavesBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ERBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ERConstants.MOD_ID);
    private static final EnumMap<ERWoodSet, WoodBlocks> WOOD_BLOCKS = new EnumMap<>(ERWoodSet.class);

    static {
        for (ERWoodSet wood : ERWoodSet.values()) {
            WOOD_BLOCKS.put(wood, registerWoodBlocks(wood));
        }
    }

    private ERBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }

    public static List<Block> entries() {
        return BLOCKS.getEntries().stream()
                .map(holder -> (Block) holder.get())
                .toList();
    }

    public static WoodBlocks woodBlocks(ERWoodSet wood) {
        return WOOD_BLOCKS.get(wood);
    }

    public static List<WoodBlocks> woodEntries() {
        return List.copyOf(WOOD_BLOCKS.values());
    }

    private static WoodBlocks registerWoodBlocks(ERWoodSet wood) {
        DeferredBlock<RotatedPillarBlock> log = BLOCKS.registerBlock(
                wood.logName(),
                RotatedPillarBlock::new,
                properties -> properties
                        .mapColor(state -> state.getValue(RotatedPillarBlock.AXIS) == net.minecraft.core.Direction.Axis.Y
                                ? MapColor.WOOD
                                : MapColor.PODZOL)
                        .sound(SoundType.WOOD)
                        .strength(2.0F)
                        .ignitedByLava());
        DeferredBlock<RotatedPillarBlock> strippedLog = BLOCKS.registerBlock(
                wood.strippedLogName(),
                RotatedPillarBlock::new,
                properties -> properties
                        .mapColor(MapColor.WOOD)
                        .sound(SoundType.WOOD)
                        .strength(2.0F)
                        .ignitedByLava());
        DeferredBlock<Block> planks = BLOCKS.registerSimpleBlock(
                wood.planksName(),
                properties -> properties
                        .mapColor(MapColor.WOOD)
                        .sound(SoundType.WOOD)
                        .strength(2.0F, 3.0F)
                        .ignitedByLava());
        DeferredBlock<StairBlock> stairs = BLOCKS.registerBlock(
                wood.stairsName(),
                properties -> new ERStairBlock(planks.get().defaultBlockState(), properties),
                ERBlocks::woodBlockProperties);
        DeferredBlock<SlabBlock> slab = BLOCKS.registerBlock(
                wood.slabName(),
                SlabBlock::new,
                ERBlocks::woodBlockProperties);
        DeferredBlock<FenceBlock> fence = BLOCKS.registerBlock(
                wood.fenceName(),
                FenceBlock::new,
                properties -> woodBlockProperties(properties)
                        .forceSolidOn());
        DeferredBlock<FenceGateBlock> fenceGate = BLOCKS.registerBlock(
                wood.fenceGateName(),
                properties -> new FenceGateBlock(wood.woodType(), properties),
                properties -> woodBlockProperties(properties)
                        .forceSolidOn());
        DeferredBlock<ButtonBlock> button = BLOCKS.registerBlock(
                wood.buttonName(),
                properties -> new ERButtonBlock(wood.blockSetType(), 30, properties),
                properties -> properties
                        .noCollision()
                        .strength(0.5F)
                        .pushReaction(PushReaction.DESTROY));
        DeferredBlock<PressurePlateBlock> pressurePlate = BLOCKS.registerBlock(
                wood.pressurePlateName(),
                properties -> new ERPressurePlateBlock(wood.blockSetType(), properties),
                properties -> properties
                        .mapColor(MapColor.WOOD)
                        .forceSolidOn()
                        .instrument(NoteBlockInstrument.BASS)
                        .noCollision()
                        .strength(0.5F)
                        .ignitedByLava()
                        .pushReaction(PushReaction.DESTROY));
        DeferredBlock<TintedParticleLeavesBlock> leaves = BLOCKS.registerBlock(
                wood.leavesName(),
                properties -> new TintedParticleLeavesBlock(0.01F, properties),
                properties -> properties
                        .mapColor(MapColor.PLANT)
                        .strength(0.2F)
                        .randomTicks()
                        .sound(SoundType.GRASS)
                        .noOcclusion()
                        .ignitedByLava());
        DeferredBlock<SaplingBlock> sapling = BLOCKS.registerBlock(
                wood.saplingName(),
                properties -> new SaplingBlock(new TreeGrower(wood.registryName(), Optional.empty(), Optional.empty(), Optional.empty()), properties),
                properties -> properties
                        .mapColor(MapColor.PLANT)
                        .noCollision()
                        .randomTicks()
                        .instabreak()
                        .sound(SoundType.GRASS));
        DeferredBlock<DoorBlock> door = BLOCKS.registerBlock(
                wood.doorName(),
                properties -> new DoorBlock(wood.blockSetType(), properties),
                properties -> properties
                        .mapColor(MapColor.WOOD)
                        .strength(3.0F)
                        .sound(SoundType.WOOD)
                        .noOcclusion()
                        .ignitedByLava());
        DeferredBlock<TrapDoorBlock> trapdoor = BLOCKS.registerBlock(
                wood.trapdoorName(),
                properties -> new TrapDoorBlock(wood.blockSetType(), properties),
                properties -> properties
                        .mapColor(MapColor.WOOD)
                        .strength(3.0F)
                        .sound(SoundType.WOOD)
                        .noOcclusion()
                        .isValidSpawn((state, level, pos, entityType) -> false)
                        .ignitedByLava());
        DeferredBlock<StandingSignBlock> sign = BLOCKS.registerBlock(
                wood.signName(),
                properties -> new StandingSignBlock(wood.woodType(), properties),
                properties -> properties
                        .mapColor(MapColor.WOOD)
                        .forceSolidOn()
                        .noCollision()
                        .strength(1.0F)
                        .ignitedByLava());
        DeferredBlock<WallSignBlock> wallSign = BLOCKS.registerBlock(
                wood.wallSignName(),
                properties -> new WallSignBlock(wood.woodType(), properties),
                properties -> properties
                        .mapColor(MapColor.WOOD)
                        .forceSolidOn()
                        .noCollision()
                        .strength(1.0F)
                        .ignitedByLava());
        DeferredBlock<CeilingHangingSignBlock> hangingSign = BLOCKS.registerBlock(
                wood.hangingSignName(),
                properties -> new CeilingHangingSignBlock(wood.woodType(), properties),
                properties -> properties
                        .mapColor(MapColor.WOOD)
                        .forceSolidOn()
                        .noCollision()
                        .strength(1.0F)
                        .ignitedByLava());
        DeferredBlock<WallHangingSignBlock> wallHangingSign = BLOCKS.registerBlock(
                wood.wallHangingSignName(),
                properties -> new WallHangingSignBlock(wood.woodType(), properties),
                properties -> properties
                        .mapColor(MapColor.WOOD)
                        .forceSolidOn()
                        .noCollision()
                        .strength(1.0F)
                        .ignitedByLava());

        return new WoodBlocks(log, strippedLog, planks, stairs, slab, fence, fenceGate, button, pressurePlate, leaves, sapling, door, trapdoor, sign, wallSign, hangingSign, wallHangingSign);
    }

    private static net.minecraft.world.level.block.state.BlockBehaviour.Properties woodBlockProperties(
            net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        return properties
                .mapColor(MapColor.WOOD)
                .instrument(NoteBlockInstrument.BASS)
                .sound(SoundType.WOOD)
                .strength(2.0F, 3.0F)
                .ignitedByLava();
    }

    public record WoodBlocks(
            DeferredBlock<RotatedPillarBlock> log,
            DeferredBlock<RotatedPillarBlock> strippedLog,
            DeferredBlock<Block> planks,
            DeferredBlock<StairBlock> stairs,
            DeferredBlock<SlabBlock> slab,
            DeferredBlock<FenceBlock> fence,
            DeferredBlock<FenceGateBlock> fenceGate,
            DeferredBlock<ButtonBlock> button,
            DeferredBlock<PressurePlateBlock> pressurePlate,
            DeferredBlock<TintedParticleLeavesBlock> leaves,
            DeferredBlock<SaplingBlock> sapling,
            DeferredBlock<DoorBlock> door,
            DeferredBlock<TrapDoorBlock> trapdoor,
            DeferredBlock<StandingSignBlock> sign,
            DeferredBlock<WallSignBlock> wallSign,
            DeferredBlock<CeilingHangingSignBlock> hangingSign,
            DeferredBlock<WallHangingSignBlock> wallHangingSign) {
    }
}
