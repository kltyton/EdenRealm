package com.kltyton.eden_realm.common.block.shape;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Exposes model-derived shapes for blocks whose collision follows authored model geometry.
 */
public final class ModelShapeProvider {
    private static final Map<List<Identifier>, VoxelShape> CACHE = new ConcurrentHashMap<>();

    private ModelShapeProvider() {
    }

    public static VoxelShape union(List<Identifier> modelIds) {
        List<Identifier> key = List.copyOf(modelIds);
        return CACHE.computeIfAbsent(key, ModelShapeProvider::loadUnion);
    }

    public static VoxelShape shape(Identifier modelId) {
        return union(List.of(modelId));
    }

    public static VoxelShape selectEqualWeight(List<VoxelShape> shapes, long seed) {
        if (shapes.isEmpty()) {
            throw new IllegalArgumentException("Shape list must not be empty");
        }
        return shapes.get(RandomSource.create(seed).nextInt(shapes.size()));
    }

    private static VoxelShape loadUnion(List<Identifier> modelIds) {
        VoxelShape shape = Shapes.empty();
        for (Identifier modelId : modelIds) {
            shape = Shapes.joinUnoptimized(
                    shape,
                    ModelShapeCache.get(modelId, Direction.NORTH).wholeShape(),
                    BooleanOp.OR);
        }
        return shape.isEmpty() ? Shapes.block() : shape.optimize();
    }
}
