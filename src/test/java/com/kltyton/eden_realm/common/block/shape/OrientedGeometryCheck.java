package com.kltyton.eden_realm.common.block.shape;

import com.kltyton.bonehitboxlib.api.block.shape.ModelShapeCache;

import com.google.gson.JsonParser;
import com.kltyton.bonehitboxlib.api.block.shape.geometry.BlockShapeCollision;
import com.kltyton.bonehitboxlib.api.block.shape.geometry.CompoundShape;
import com.kltyton.bonehitboxlib.api.block.shape.geometry.ShapeOperations;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

final class OrientedGeometryCheck {
    private static int assertions;
    private OrientedGeometryCheck() { }

    static void run() {
        VoxelShape bar = shape("""
                [{"from":[0,0,7.5],"to":[16,8,8.5],
                  "rotation":{"origin":[8,0,8],"axis":"y","angle":45}}]
                """);
        for (int x = 0; x < 31; x++) {
            for (int z = 0; z < 31; z++) {
                double px = (x + 0.317) / 31, pz = (z + 0.219) / 31;
                boolean expected = Math.abs(px + pz - 1) < Math.sqrt(2) / 32
                        && Math.abs(px - pz) < Math.sqrt(2) / 2;
                boolean actual = bar.clip(new Vec3(px, 2, pz), new Vec3(px, -1, pz), BlockPos.ZERO) != null;
                require(actual == expected, "analytic diagonal footprint at " + px + "," + pz);
            }
        }
        VoxelShape emptyCorner = Shapes.box(0.515, 0.2, 0.535, 0.525, 0.3, 0.545);
        require(!ShapeOperations.joinIsNotEmpty(bar, emptyCorner, BooleanOp.AND), "exact overlap rejects inflated voxel fringe");
        require(!ShapeOperations.joinIsNotEmpty(emptyCorner, bar, BooleanOp.AND), "overlap is symmetric");
        require(ShapeOperations.joinIsNotEmpty(Shapes.block(), bar, BooleanOp.ONLY_FIRST), "enclosing cube has space outside the bar");
        require(!ShapeOperations.joinIsNotEmpty(bar, Shapes.block(), BooleanOp.ONLY_FIRST), "bar is entirely inside unit cube");
        require(!ShapeOperations.joinIsNotEmpty(bar, bar.move(0, 0, 0), BooleanOp.NOT_SAME), "same geometry has no symmetric difference");
        require(ShapeOperations.joinIsNotEmpty(Shapes.INFINITY, bar, BooleanOp.ONLY_FIRST), "unbounded query has points outside finite geometry");
        require(!ShapeOperations.joinIsNotEmpty(bar, Shapes.INFINITY, BooleanOp.ONLY_FIRST), "finite geometry is covered by infinity");
        VoxelShape cut = ShapeOperations.join(bar, Shapes.box(0, 0, 0, 0.5, 1, 1), BooleanOp.ONLY_FIRST);
        require(cut.clip(new Vec3(0.4, 2, 0.6), new Vec3(0.4, -1, 0.6), BlockPos.ZERO) == null, "subtraction removes the left half");
        require(cut.clip(new Vec3(0.6, 2, 0.4), new Vec3(0.6, -1, 0.4), BlockPos.ZERO) != null, "subtraction retains the right half");
        require(ShapeOperations.join(bar, emptyCorner, BooleanOp.AND).isEmpty(), "constructive intersection rejects an empty corner");
        require(bar.getFaceShape(Direction.UP).isEmpty(), "bar does not support the top cell face");
        require(!bar.getFaceShape(Direction.DOWN).isEmpty(), "bar retains its exact bottom cross section");

        VoxelShape wall = shape("""
                [{"from":[0,0,-32],"to":[1.6,32,32],
                  "rotation":{"origin":[0,0,0],"axis":"y","angle":45}}]
                """);
        AABB mover = new AABB(-1, 0.2, 0, -0.9, 1.2, 0.1);
        Vec3 slid = BlockShapeCollision.collide(new Vec3(2, 0, 0), mover, List.of(wall));
        close(slid.x, 1.45, "diagonal wall preserves tangent displacement X");
        close(slid.z, 0.55, "diagonal wall preserves tangent displacement Z");
        require(!((CompoundShape) wall).intersects(mover.move(slid).deflate(1.0E-7)), "slide ends outside the obstacle");
        Vec3 fast = BlockShapeCollision.collide(new Vec3(20, 0, -20), mover, List.of(wall));
        require(fast.length() < 2, "continuous sweep stops a fast crossing instead of tunneling");
        VoxelShape nativeWall = Shapes.box(0.35, 0, -1, 0.45, 2, 2);
        Vec3 constrained = BlockShapeCollision.collide(new Vec3(2, 0, 0), mover, List.of(wall, nativeWall));
        require(mover.move(constrained).maxX <= 0.35 + 1.0E-7, "native neighboring wall still blocks the deflected path");

        VoxelShape ramp = shape("""
                [{"from":[0,-1.6,-16],"to":[48,0,16],
                  "rotation":{"origin":[0,0,0],"axis":"z","angle":30}}]
                """);
        double top = 0.5 / Math.sqrt(3);
        AABB feet = new AABB(0.4, top, -0.05, 0.5, top + 1, 0.05);
        Vec3 roundedContact = BlockShapeCollision.collide(new Vec3(0, -0.08, 0), feet.move(0, -1.0E-8, 0), List.of(ramp));
        close(roundedContact.length(), 0, "float-width rounding at contact must not lose slope support");
        for (int i = 0; i < 12; i++) {
            Vec3 step = BlockShapeCollision.collide(new Vec3(0.1, -0.08, 0), feet, List.of(ramp));
            require(step.x > 0.05 && step.y > 0, "slope ascent makes continuous progress");
            feet = feet.move(step);
            close(feet.minY, feet.maxX / Math.sqrt(3), "feet stay on analytic slanted top");
            require(!((CompoundShape) ramp).intersects(feet.deflate(1.0E-7)), "slope ascent does not penetrate the model");
        }
        Vec3 rest = BlockShapeCollision.collide(new Vec3(0, -0.08, 0), feet, List.of(ramp));
        close(rest.length(), 0, "gravity does not push a resting entity sideways");

        var tall = ModelShapeCache.buildForElements(JsonParser.parseString("""
                [{"from":[0,0,7],"to":[16,32,9],
                  "rotation":{"origin":[8,0,8],"axis":"y","angle":45}}]
                """).getAsJsonArray(), Direction.NORTH);
        VoxelShape upper = tall.localShape(new ModelShapeCache.Cell(0, 1, 0));
        var hit = upper.clip(new Vec3(0.5, 1.5, -1), new Vec3(0.5, 1.5, 2), new BlockPos(0, 1, 0));
        require(hit != null && hit.getBlockPos().equals(new BlockPos(0, 1, 0)), "upper-cell ray preserves hit ownership");
        close(upper.bounds().minY, 0, "upper OBB piece is normalized locally");
        close(upper.bounds().maxY, 1, "upper OBB piece is clipped to one cell");
        VoxelShape far = bar.move(29_000_000, 100, -29_000_000);
        require(far.clip(new Vec3(29_000_000.52, 102, -28_999_999.46),
                new Vec3(29_000_000.52, 99, -28_999_999.46), BlockPos.ZERO) == null, "far-world geometry retains double precision");

        VoxelShape rescaled = shape("""
                [{"from":[4,0,4],"to":[12,8,12],
                  "rotation":{"origin":[8,0,8],"axis":"y","angle":45,"rescale":true}}]
                """);
        close(rescaled.bounds().minX, 0, "26.2 rescale expands rotated local axes");
        close(rescaled.bounds().maxX, 1, "26.2 rescale matches visible model bounds");
        System.out.println("OBB numeric checks passed: " + assertions);
    }

    private static VoxelShape shape(String json) {
        return ModelShapeCache.buildForElements(JsonParser.parseString(json).getAsJsonArray(), Direction.NORTH).wholeShape();
    }

    private static void close(double actual, double expected, String message) {
        require(Math.abs(actual - expected) < 1.0E-6, message + ": expected " + expected + ", got " + actual);
    }

    private static void require(boolean result, String message) {
        assertions++;
        if (!result) { throw new AssertionError(message); }
    }
}

