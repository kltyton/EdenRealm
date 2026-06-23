package com.kltyton.eden_realm.registry;

import com.kltyton.eden_realm.ERConstants;
import java.util.stream.Stream;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ERBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ERConstants.MOD_ID);

    private ERBlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
        modEventBus.addListener(ERBlockEntities::addBlocksToVanillaTypes);
    }

    private static void addBlocksToVanillaTypes(BlockEntityTypeAddBlocksEvent event) {
        event.modify(BlockEntityTypes.SIGN, signBlocks());
        event.modify(BlockEntityTypes.HANGING_SIGN, hangingSignBlocks());
        event.modify(BlockEntityTypes.SHELF, shelfBlocks());
    }

    private static Block[] signBlocks() {
        return ERBlocks.woodEntries().stream()
                .flatMap(blocks -> Stream.of(blocks.sign().get(), blocks.wallSign().get()))
                .toArray(Block[]::new);
    }

    private static Block[] hangingSignBlocks() {
        return ERBlocks.woodEntries().stream()
                .flatMap(blocks -> Stream.of(blocks.hangingSign().get(), blocks.wallHangingSign().get()))
                .toArray(Block[]::new);
    }

    private static Block[] shelfBlocks() {
        return ERBlocks.woodEntries().stream()
                .map(blocks -> (Block) blocks.shelf().get())
                .toArray(Block[]::new);
    }
}
