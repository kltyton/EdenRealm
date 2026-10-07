package com.kltyton.eden_realm.common.block.shape;

import com.kltyton.bonehitboxlib.api.block.shape.ModelShapeProvider;

import com.kltyton.bonehitboxlib.api.block.shape.ModelShapeCache;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ModelShapeCacheCheck {
    private static final double EPSILON = 1.0E-7;

    private ModelShapeCacheCheck() {
    }

    public static void main(String[] args) {
        JsonArray twoCellDoor = JsonParser.parseString("""
                [
                  {"from":[0,0,7],"to":[16,32,9]}
                ]
                """).getAsJsonArray();

        ModelShapeCache.ShapeSet north =
                ModelShapeCache.buildForElements(twoCellDoor, Direction.NORTH);
        require(north.occupiedCells().size() == 2, "north footprint must occupy two cells");
        assertBounds(
                north.localShape(new ModelShapeCache.Cell(0, 0, 0)),
                new AABB(0.0, 0.0, 7.0 / 16.0, 1.0, 1.0, 9.0 / 16.0),
                "north lower part");
        assertBounds(
                north.localShape(new ModelShapeCache.Cell(0, 1, 0)),
                new AABB(0.0, 0.0, 7.0 / 16.0, 1.0, 1.0, 9.0 / 16.0),
                "north upper part");
        assertBounds(
                north.wholeShape(),
                new AABB(0.0, 0.0, 7.0 / 16.0, 1.0, 2.0, 9.0 / 16.0),
                "north whole outline");

        ModelShapeCache.ShapeSet east =
                ModelShapeCache.buildForElements(twoCellDoor, Direction.EAST);
        assertBounds(
                east.localShape(new ModelShapeCache.Cell(0, 0, 0)),
                new AABB(7.0 / 16.0, 0.0, 0.0, 9.0 / 16.0, 1.0, 1.0),
                "east lower part");
        assertBounds(
                east.wholeShape(),
                new AABB(7.0 / 16.0, 0.0, 0.0, 9.0 / 16.0, 2.0, 1.0),
                "east whole outline");

        VoxelShape upperWholeInHitCoordinates =
                north.wholeShape().move(0.0, -1.0, 0.0);
        assertBounds(
                upperWholeInHitCoordinates,
                new AABB(0.0, -1.0, 7.0 / 16.0, 1.0, 1.0, 9.0 / 16.0),
                "whole outline normalized to upper hit cell");

        JsonArray zeroThicknessPlane = JsonParser.parseString("""
                [
                  {"from":[8,0,0],"to":[8,16,16]}
                ]
                """).getAsJsonArray();
        VoxelShape planeShape =
                ModelShapeCache.buildForElements(zeroThicknessPlane, Direction.NORTH).wholeShape();
        require(!planeShape.isEmpty(), "zero-thickness model element must produce a selection shape");
        require(planeShape.bounds().getXsize() <= 0.01, "zero-thickness inflation must remain narrow");

        JsonArray eulerRotatedPlane = JsonParser.parseString("""
                [
                  {
                    "from":[8,0,0],
                    "to":[8,16,16],
                    "rotation":{"origin":[8,8,8],"x":35,"y":45,"z":15}
                  }
                ]
                """).getAsJsonArray();
        VoxelShape eulerShape =
                ModelShapeCache.buildForElements(eulerRotatedPlane, Direction.NORTH).wholeShape();
        require(!eulerShape.isEmpty(), "Euler-rotated model element must produce a selection shape");
        require(eulerShape.bounds().getXsize() > planeShape.bounds().getXsize(),
                "Euler rotation must affect model-derived bounds");

        VoxelShape blueGlowOne = ModelShapeProvider.shape(model("blue_glow_mushroom_1"));
        VoxelShape blueGlowTwo = ModelShapeProvider.shape(model("blue_glow_mushroom_2"));
        VoxelShape blueGlowThree = ModelShapeProvider.shape(model("blue_glow_mushroom_3"));
        require(!sameBounds(blueGlowOne.bounds(), blueGlowTwo.bounds()),
                "blue glow mushroom variants 1 and 2 must retain distinct shapes");
        require(!sameBounds(blueGlowTwo.bounds(), blueGlowThree.bounds()),
                "blue glow mushroom variants 2 and 3 must retain distinct shapes");

        List<VoxelShape> blueGlowShapes = List.of(blueGlowOne, blueGlowTwo, blueGlowThree);
        long modelSeed = seedSelectingNonDefaultShape(blueGlowShapes.size());
        int expectedShape = RandomSource.create(modelSeed).nextInt(blueGlowShapes.size());
        VoxelShape firstSelection = ModelShapeProvider.selectEqualWeight(blueGlowShapes, modelSeed);
        VoxelShape repeatedSelection = ModelShapeProvider.selectEqualWeight(blueGlowShapes, modelSeed);
        require(sameBounds(firstSelection.bounds(), blueGlowShapes.get(expectedShape).bounds()),
                "position-seeded fungus shape must follow weighted renderer selection order");
        require(sameBounds(firstSelection.bounds(), repeatedSelection.bounds()),
                "position-seeded fungus shape must remain stable");

        JsonArray diagonalBar = JsonParser.parseString("""
                [{"from":[0,0,7.5],"to":[16,8,8.5],
                  "rotation":{"origin":[8,0,8],"axis":"y","angle":45}}]
                """).getAsJsonArray();
        VoxelShape diagonal = ModelShapeCache.buildForElements(diagonalBar, Direction.NORTH).wholeShape();
        require(diagonal.clip(new Vec3(0.52, 2, 0.54), new Vec3(0.52, -1, 0.54), BlockPos.ZERO) == null,
                "ray outside the true diagonal bar must miss its old inflated voxel fringe");
        var centerHit = diagonal.clip(new Vec3(0.5, 2, 0.5), new Vec3(0.5, -1, 0.5), BlockPos.ZERO);
        require(centerHit != null && same(centerHit.getLocation().y(), 0.5),
                "center ray must hit the authored top at y=0.5");
        AABB clearCorner = new AABB(0.515, 1, 0.535, 0.525, 1.02, 0.545);
        require(same(diagonal.collide(Direction.Axis.Y, clearCorner, -1), -1),
                "movement through the diagonal bar's empty corner must remain unobstructed");
        int[] edges = {0, 0};
        diagonal.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            edges[0]++;
            if (!same(x1, x2) && !same(z1, z2)) {
                edges[1]++;
            }
        });
        require(edges[0] == 12 && edges[1] == 8,
                "one oriented box must render twelve original edges including eight diagonal edges");
        require(diagonal.move(10, 20, 30).clip(new Vec3(10.52, 22, 30.54),
                new Vec3(10.52, 19, 30.54), BlockPos.ZERO) == null,
                "translating an oriented shape must preserve its exact ray geometry");

        OrientedGeometryCheck.run();
        System.out.println("Automatic block shape checks passed");
    }

    private static long seedSelectingNonDefaultShape(int shapeCount) {
        for (long seed = 0; seed < 64; seed++) {
            if (RandomSource.create(seed).nextInt(shapeCount) != 0) {
                return seed;
            }
        }
        throw new AssertionError("failed to find a non-default shape seed");
    }

    private static void assertBounds(VoxelShape shape, AABB expected, String label) {
        require(!shape.isEmpty(), label + " must not be empty");
        AABB actual = shape.bounds();
        require(same(actual.minX, expected.minX), label + " minX");
        require(same(actual.minY, expected.minY), label + " minY");
        require(same(actual.minZ, expected.minZ), label + " minZ");
        require(same(actual.maxX, expected.maxX), label + " maxX");
        require(same(actual.maxY, expected.maxY), label + " maxY");
        require(same(actual.maxZ, expected.maxZ), label + " maxZ");
    }

    private static boolean same(double first, double second) {
        return Math.abs(first - second) <= EPSILON;
    }

    private static boolean sameBounds(AABB first, AABB second) {
        return same(first.minX, second.minX)
                && same(first.minY, second.minY)
                && same(first.minZ, second.minZ)
                && same(first.maxX, second.maxX)
                && same(first.maxY, second.maxY)
                && same(first.maxZ, second.maxZ);
    }

    private static Identifier model(String name) {
        return Identifier.fromNamespaceAndPath("eden_realm", "block/mushroom/" + name);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
