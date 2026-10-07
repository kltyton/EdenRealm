package com.kltyton.eden_realm.common.block.fruit;

import com.kltyton.eden_realm.common.block.shape.ERHarvestShapes;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.entity.fruit.ERFallingFruitEntity;
import com.kltyton.eden_realm.registry.ERItems;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ERHangingFruitBlock extends Block implements BonemealableBlock {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_4;
    public static final float BUD_CHANCE = 0.005F;
    public static final float BONEMEAL_CHANCE = 0.45F;
    public static final float RIPE_FALL_CHANCE = 0.10F;
    public static final MapCodec<ERHangingFruitBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.fieldOf("support").forGetter(block -> block.support),
            propertiesCodec()).apply(instance, ERHangingFruitBlock::new));
    private final Identifier support;

    public ERHangingFruitBlock(Identifier support, BlockBehaviour.Properties properties) {
        super(properties);
        this.support = support;
        registerDefaultState(defaultBlockState().setValue(AGE, 0));
    }

    @Override
    public MapCodec<ERHangingFruitBlock> codec() {
        return CODEC;
    }

    public static boolean canGrowBelow(LevelReader level, BlockPos pos, BlockState bud) {
        return level.isInsideBuildHeight(pos.below()) && level.getBlockState(pos.below()).isAir()
                && bud.canSurvive(level, pos.below());
    }

    public static void growBelow(ServerLevel level, BlockPos pos, Identifier fruit) {
        BlockState bud = BuiltInRegistries.BLOCK.getValue(fruit).defaultBlockState();
        if (canGrowBelow(level, pos, bud)) {
            level.setBlockAndUpdate(pos.below(), bud);
        }
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return BuiltInRegistries.BLOCK.getKey(above.getBlock()).equals(support);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < 4 || fallsWhenRipe();
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(AGE) == 4) {
            if (fallsWhenRipe() && random.nextFloat() < RIPE_FALL_CHANCE) {
                detachRipe(level, pos);
            }
            return;
        }
        if (state.getValue(AGE) < 4 && state.canSurvive(level, pos)
                && net.neoforged.neoforge.common.CommonHooks.canCropGrow(level, pos, state, random.nextInt(5) == 0)) {
            level.setBlockAndUpdate(pos, state.cycle(AGE));
            net.neoforged.neoforge.common.CommonHooks.fireCropGrowPost(level, pos, state);
        }
    }

    private boolean fallsWhenRipe() {
        return support.equals(ERConstants.id("tide_song_flowering_leaves"))
                || support.equals(ERConstants.id("cloud_crown_flowering_leaves"))
                || support.equals(ERConstants.id("twilight_pomegranate_flowering_leaves"));
    }

    public boolean detachRipe(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(this) || state.getValue(AGE) != 4) {
            return false;
        }
        return !ERFallingFruitEntity.fall(level, pos, groundFruit().defaultBlockState()).isRemoved();
    }

    public boolean isSameFruit(BlockState state) {
        return state.is(this) || state.is(groundFruit());
    }

    private Block groundFruit() {
        String fruit = BuiltInRegistries.BLOCK.getKey(this).getPath().replace("_hanging", "");
        return ((net.minecraft.world.item.BlockItem) ERItems.harvestItem(fruit).get()).getBlock();
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level instanceof ServerLevel server) {
            detachRipe(server, pos);
        }
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, net.minecraft.world.phys.BlockHitResult hit,
            net.minecraft.world.entity.projectile.Projectile projectile) {
        if (level instanceof ServerLevel server) {
            detachRipe(server, hit.getBlockPos());
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return state.getValue(AGE) < 4 && state.canSurvive(level, pos);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (isValidBonemealTarget(level, pos, state)) {
            level.setBlockAndUpdate(pos, state.cycle(AGE));
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ERHarvestShapes.fruit(support, state.getValue(AGE));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction direction, BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        if (direction == Direction.UP && !state.canSurvive(level, pos)) {
            if (state.getValue(AGE) < 4) {
                return Blocks.AIR.defaultBlockState();
            }
            ticks.scheduleTick(pos, this, 1);
        }
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbour, random);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            detachRipe(level, pos);
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData, Player player) {
        String fruit = BuiltInRegistries.BLOCK.getKey(this).getPath().replace("_hanging", "");
        return new ItemStack(ERItems.harvestItem(fruit).get());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }
}
