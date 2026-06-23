package com.kltyton.eden_realm.common.block;

import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class ERButtonBlock extends ButtonBlock {
    public ERButtonBlock(BlockSetType type, int ticksToStayPressed, BlockBehaviour.Properties properties) {
        super(type, ticksToStayPressed, properties);
    }
}
