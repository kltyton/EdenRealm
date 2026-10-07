package com.kltyton.eden_realm.common.block.shape;

import com.kltyton.bonehitboxlib.api.block.shape.ModelShapeCache;

import com.kltyton.bonehitboxlib.api.block.shape.AutoWholeShapeBlock;

import com.kltyton.bonehitboxlib.api.block.shape.AutoPartShapeBlock;

import com.google.gson.JsonParser;
import com.kltyton.eden_realm.ERConstants;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Server-only synthetic geometry fixtures; excluded from the distributable jar. */
@EventBusSubscriber(modid = ERConstants.MOD_ID)
public final class ObbGameTests {
    private static Block bar;
    private static Block wall;
    private static Block ramp;
    private static Column column;

    private ObbGameTests() { }

    @SubscribeEvent
    public static void registerBlocks(RegisterEvent event) {
        if (!Boolean.getBoolean("edenrealm.obbTests")) { return; }
        event.register(Registries.BLOCK, registry -> {
            bar = fixture("bar", "[{\"from\":[0,0,7.5],\"to\":[16,8,8.5],\"rotation\":{\"origin\":[8,0,8],\"axis\":\"y\",\"angle\":45}}]");
            wall = fixture("wall", "[{\"from\":[7,0,0],\"to\":[9,16,16],\"rotation\":{\"origin\":[8,0,8],\"axis\":\"y\",\"angle\":45}}]");
            ramp = fixture("ramp", "[{\"from\":[0,-1.6,0],\"to\":[16,0,16],\"rotation\":{\"origin\":[0,0,0],\"axis\":\"z\",\"angle\":30}}]");
            column = new Column(properties("column"));
            registry.register(ERConstants.id("obb_fixture_bar"), bar);
            registry.register(ERConstants.id("obb_fixture_wall"), wall);
            registry.register(ERConstants.id("obb_fixture_ramp"), ramp);
            registry.register(ERConstants.id("obb_fixture_column"), column);
        });
    }

    private static Block.Properties properties(String name) {
        return Block.Properties.of().setId(ResourceKey.create(Registries.BLOCK, ERConstants.id("obb_fixture_" + name)))
                .noOcclusion().dynamicShape();
    }

    private static Block fixture(String name, String elements) {
        VoxelShape shape = ModelShapeCache.buildForElements(JsonParser.parseString(elements).getAsJsonArray(), Direction.NORTH).wholeShape();
        return new Block(properties(name)) {
            @Override
            protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return shape; }
            @Override
            protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return shape; }
            @Override
            protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
        };
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        if (!Boolean.getBoolean("edenrealm.obbTests")) { return; }
        register(event, "ray_and_overlap", ObbGameTests::rayAndOverlap);
        register(event, "wall_movement", ObbGameTests::wallMovement);
        register(event, "slope_step", ObbGameTests::slopeStep);
        register(event, "linked_cleanup", ObbGameTests::linkedCleanup);
    }

    private static void register(RegisterGameTestsEvent event, String name, Consumer<GameTestHelper> test) {
        var environment = event.registerEnvironment(ERConstants.id("obb_" + name));
        var data = new TestData<>(environment, ERConstants.id("harvest_empty"), 100, 10, true,
                Rotation.NONE, false, 1, 1, true, 0);
        event.registerTest(ERConstants.id("obb_" + name), new GameTestInstance(data) {
            @Override
            public void run(GameTestHelper helper) { test.accept(helper); helper.succeed(); }
            @Override
            public MapCodec<? extends GameTestInstance> codec() { return MapCodec.unit(this); }
            @Override
            protected MutableComponent typeDescription() { return Component.literal("Eden Realm OBB integration"); }
        });
    }

    private static void rayAndOverlap(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(4, 3, 4));
        level.setBlockAndUpdate(pos, bar.defaultBlockState());
        Vec3 base = Vec3.atLowerCornerOf(pos);
        var miss = level.clip(new ClipContext(base.add(.52, 2, .54), base.add(.52, -.5, .54),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, CollisionContext.empty()));
        require(miss.getType() == HitResult.Type.MISS, "ray in the old voxel fringe must miss the real diagonal bar");
        var hit = level.clip(new ClipContext(base.add(.5, 2, .5), base.add(.5, -.5, .5),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, CollisionContext.empty()));
        require(hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos), "center ray must hit its owning cell");
        near(hit.getLocation().y, base.y + .5, "ray must hit the authored top");
        AABB corner = new AABB(.515, .2, .535, .525, .3, .545).move(base);
        require(level.noCollision(corner), "placement collision must reject the broad-box false positive");
        require(!level.noCollision(new AABB(.495, .2, .495, .505, .3, .505).move(base)), "placement must detect actual OBB penetration");
        Vec3 fall = Entity.collideBoundingBox(CollisionContext.empty(), new Vec3(0, -.7, 0), corner.move(0, .6, 0), level, List.of());
        near(fall.y, -.7, "context-only falling query must pass through empty fringe");
    }

    private static void wallMovement(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(5, 3, 5));
        level.setBlockAndUpdate(pos, wall.defaultBlockState());
        Vec3 base = Vec3.atLowerCornerOf(pos);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(base.add(-.8, .1, .5));
        player.setOnGround(false);
        player.move(MoverType.SELF, new Vec3(2, 0, 0));
        require(player.getZ() > base.z + .7, "real Entity.move must slide along the diagonal wall");
        require(level.noCollision(player.getBoundingBox().deflate(1.0E-6)), "sliding must not leave a player inside the OBB");
        player.setPos(base.add(-.8, .1, 1.8));
        player.setOnGround(false);
        player.move(MoverType.SELF, new Vec3(4, 0, -4));
        require(player.getX() < base.x + .5 && player.getZ() > base.z + .5, "fast diagonal movement must not tunnel through the wall");
        require(level.noCollision(player.getBoundingBox().deflate(1.0E-6)), "high-speed endpoint must remain outside the wall");
        // This neighbor is outside the initial Z sweep but inside the deflected movement.
        level.setBlockAndUpdate(pos.south(), Blocks.STONE.defaultBlockState());
        player.setPos(base.add(-.8, .1, .5));
        player.setOnGround(false);
        player.move(MoverType.SELF, new Vec3(2, 0, 0));
        require(player.getBoundingBox().maxZ <= base.z + 1.000001, "sliding must also stop at the neighboring native block");
        require(level.noCollision(player.getBoundingBox().deflate(1.0E-6)), "mixed native/OBB collision must remain nonpenetrating");
    }

    private static void slopeStep(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(5, 3, 5));
        level.setBlockAndUpdate(pos, ramp.defaultBlockState());
        Vec3 base = Vec3.atLowerCornerOf(pos);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(base.add(.2, .5 / Math.sqrt(3), .5));
        player.setOnGround(true);
        for (int step = 1; step <= 3; step++) {
            player.move(MoverType.SELF, new Vec3(.1, -.08, 0));
            near(player.getX(), base.x + .2 + step * .1, "slope step must retain requested horizontal travel");
            near(player.getY(), base.y + (player.getBoundingBox().maxX - base.x) / Math.sqrt(3),
                    "native step selection must follow the nearby slope, without jumping to its distant maximum");
            require(player.onGround(), "uphill movement must retain native ground contact");
            require(level.noCollision(player.getBoundingBox().deflate(1.0E-6)), "slope step must not penetrate the surface");
        }
        Vec3 resting = player.position();
        for (int tick = 0; tick < 10; tick++) { player.move(MoverType.SELF, new Vec3(0, -.08, 0)); }
        near(player.getX(), resting.x, "gravity must not create sideways drift");
        near(player.getY(), resting.y, "resting feet must remain on the slope");
    }

    private static void linkedCleanup(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(4, 3, 4));
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST}) {
            for (int removal = 0; removal < 3; removal++) {
                BlockState state = column.defaultBlockState().setValue(AutoPartShapeBlock.FACING, facing);
                level.setBlockAndUpdate(origin, state);
                column.setPlacedBy(level, origin, state, null, ItemStack.EMPTY);
                BlockPos upper = origin.above();
                BlockState part = level.getBlockState(upper);
                require(part.is(column) && column.getOriginPosition(part, upper).equals(origin), "upper geometry must retain origin ownership");
                var hit = level.clip(new ClipContext(Vec3.atLowerCornerOf(upper).add(.5, .25, 2),
                        Vec3.atLowerCornerOf(upper).add(.5, .25, .5), ClipContext.Block.OUTLINE,
                        ClipContext.Fluid.NONE, CollisionContext.empty()));
                require(hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(upper), "upper-part ray must select its occupied cell");
                int[] edges = {0};
                column.getWholeOutlineShape(level, part, upper).forAllEdges((x1, y1, z1, x2, y2, z2) -> edges[0]++);
                require(edges[0] == 12, "whole outline must retain twelve rotated box edges without cell seams");
                if (removal == 0) { level.setBlockAndUpdate(origin, Blocks.STONE.defaultBlockState()); }
                else { level.destroyBlock(removal == 1 ? origin : upper, true); }
                level.updateNeighborsAt(origin, column);
                require(level.getBlockState(upper).isAir(), "origin replacement/destruction must not leave an orphan part");
                require(!level.getBlockState(origin).is(column), "destruction from either cell must remove the logical block");
                var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(origin).inflate(2));
                int count = drops.stream().filter(item -> item.getItem().is(Items.DIAMOND)).mapToInt(item -> item.getItem().getCount()).sum();
                require(count == (removal == 0 ? 0 : 1), "linked cleanup must produce exactly one origin drop for destruction");
                drops.forEach(ItemEntity::discard);
                level.removeBlock(origin, false);
            }
        }
    }

    private static void near(double actual, double expected, String message) {
        require(Math.abs(actual - expected) < 1.0E-5, message + ": expected " + expected + ", got " + actual);
    }

    private static void require(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }

    private static final class Column extends AutoWholeShapeBlock {
        private static final MapCodec<Column> CODEC = simpleCodec(Column::new);
        private Column(Properties properties) { super(ERConstants.id("obb_fixture_column"), properties); }
        @Override
        protected MapCodec<? extends Column> codec() { return CODEC; }
        @Override
        protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(FACING);
        }
        @Override
        protected RenderShape getOriginRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
        @Override
        protected List<ItemStack> getOriginDrops(BlockState state, LootParams.Builder params) { return List.of(new ItemStack(Items.DIAMOND)); }
    }
}
