package com.kltyton.eden_realm.common.block.building;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Selects pillar end and middle textures from same-material, same-axis neighbors. */
public final class ERConnectedPillarBlock extends RotatedPillarBlock {
    public static final MapCodec<ERConnectedPillarBlock> CODEC = simpleCodec(ERConnectedPillarBlock::new);
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    public enum Part implements StringRepresentable {
        SINGLE("single"), TOP("top"), MIDDLE("middle"), BOTTOM("bottom");
        private final String name;
        Part(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
        Part reversed() {
            return switch (this) { case TOP -> BOTTOM; case BOTTOM -> TOP; default -> this; };
        }
    }

    public ERConnectedPillarBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(PART, Part.SINGLE));
    }

    @Override public MapCodec<? extends ERConnectedPillarBlock> codec() { return CODEC; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connectedState(super.getStateForPlacement(context), context.getLevel(), context.getClickedPos());
    }

    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                              BlockPos pos, Direction direction, BlockPos neighborPos,
                                              BlockState neighborState, RandomSource random) {
        return direction.getAxis() == state.getValue(AXIS) ? connectedState(state, level, pos) : state;
    }

    private BlockState connectedState(BlockState state, BlockGetter level, BlockPos pos) {
        Direction top = modelTop(state.getValue(AXIS));
        boolean above = connects(state, level.getBlockState(pos.relative(top)));
        boolean below = connects(state, level.getBlockState(pos.relative(top.getOpposite())));
        Part part = above ? (below ? Part.MIDDLE : Part.BOTTOM) : (below ? Part.TOP : Part.SINGLE);
        return state.setValue(PART, part);
    }

    private boolean connects(BlockState state, BlockState neighbor) {
        return neighbor.is(this) && neighbor.getValue(AXIS) == state.getValue(AXIS);
    }

    // Matches vanilla column model rotations: Y=identity, Z=X90, X=X90/Y90.
    private static Direction modelTop(Direction.Axis axis) {
        return switch (axis) { case Y -> Direction.UP; case Z -> Direction.NORTH; case X -> Direction.EAST; };
    }

    @Override protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState rotated = super.rotate(state, rotation);
        return rotation.rotate(modelTop(state.getValue(AXIS))) == modelTop(rotated.getValue(AXIS))
                ? rotated : rotated.setValue(PART, state.getValue(PART).reversed());
    }

    @Override protected BlockState mirror(BlockState state, Mirror mirror) {
        Direction top = modelTop(state.getValue(AXIS));
        return mirror.mirror(top) == top ? state : state.setValue(PART, state.getValue(PART).reversed());
    }
}
