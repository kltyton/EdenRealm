package com.kltyton.eden_realm.common.block.shape;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;

/**
 * Builds server-safe collision data directly from packaged block model JSON.
 *
 * <p>The result is cached per model and horizontal facing. Each shape is local
 * to one occupied block cell; the complete shape is local to the origin cell.</p>
 */
final class ModelShapeCache {
    static final int MAX_PART_OFFSET = 4;

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_PARENT_DEPTH = 16;
    private static final int MAX_EXACT_ELEMENTS = 4096;
    private static final int MAX_ROTATED_VOXELS = 262_144;
    private static final double ROTATED_VOXEL_STEP = 1.0;
    private static final double MIN_ROTATED_THICKNESS = 0.1;
    private static final double MIN_ELEMENT_THICKNESS = 0.1;
    private static final double MIN_MODEL_COORDINATE = -64.0;
    private static final double MAX_MODEL_COORDINATE = 80.0;
    private static final double EPSILON = 1.0E-7;

    private static final Map<ShapeKey, ShapeSet> CACHE = new ConcurrentHashMap<>();

    private ModelShapeCache() {
    }

    static ShapeSet get(Identifier modelId, Direction facing) {
        Direction horizontalFacing = facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
        return CACHE.computeIfAbsent(new ShapeKey(modelId, horizontalFacing), ModelShapeCache::load);
    }

    static ShapeSet buildForElements(JsonArray elements, Direction facing) {
        return build(parseGeometry(elements), facing);
    }

    private static ShapeSet load(ShapeKey key) {
        JsonArray elements = resolveElements(key.modelId());
        if (elements == null || elements.isEmpty()) {
            LOGGER.warn(
                    "Automatic block shape could not read elements for model {}; using one full origin cell",
                    key.modelId());
            return fallback();
        }

        try {
            return build(parseGeometry(elements), key.facing());
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Automatic block shape failed for model {} facing {}; using one full origin cell",
                    key.modelId(),
                    key.facing(),
                    exception);
            return fallback();
        }
    }

    private static ShapeSet build(ModelGeometry geometry, Direction facing) {
        int turns = horizontalTurns(facing);
        Map<Cell, List<AABB>> boxesByCell = new HashMap<>();

        if (geometry.elements().size() > MAX_EXACT_ELEMENTS) {
            addSplitBox(boxesByCell, rotate90Y(geometry.bounds(), turns));
        } else {
            for (ModelElement element : geometry.elements()) {
                if (element.voxelBoxes().isEmpty()) {
                    addSplitBox(boxesByCell, rotate90Y(element.bounds(), turns));
                } else {
                    for (AABB box : element.voxelBoxes()) {
                        addSplitBox(boxesByCell, rotate90Y(box, turns));
                    }
                }
            }
        }

        Map<Cell, VoxelShape> localShapes = new HashMap<>();
        VoxelShape wholeShape = Shapes.empty();
        for (Map.Entry<Cell, List<AABB>> entry : boxesByCell.entrySet()) {
            VoxelShape localShape = shapeFromBoxes(entry.getValue());
            if (localShape.isEmpty()) {
                continue;
            }
            Cell cell = entry.getKey();
            localShapes.put(cell, localShape);
            wholeShape = Shapes.joinUnoptimized(
                    wholeShape,
                    localShape.move(cell.x(), cell.y(), cell.z()),
                    BooleanOp.OR);
        }

        if (localShapes.isEmpty()) {
            return fallback();
        }

        List<Cell> occupiedCells = new ArrayList<>(localShapes.keySet());
        if (!localShapes.containsKey(Cell.ORIGIN)) {
            occupiedCells.add(Cell.ORIGIN);
        }
        occupiedCells.sort(Comparator
                .comparingInt(Cell::y)
                .thenComparingInt(Cell::x)
                .thenComparingInt(Cell::z));
        return new ShapeSet(Map.copyOf(localShapes), List.copyOf(occupiedCells), wholeShape.optimize());
    }

    private static ModelGeometry parseGeometry(JsonArray elements) {
        List<ModelElement> parsed = new ArrayList<>(elements.size());
        AABB bounds = null;
        for (JsonElement rawElement : elements) {
            if (!rawElement.isJsonObject()) {
                continue;
            }
            ModelElement element = parseElement(rawElement.getAsJsonObject());
            if (element == null) {
                continue;
            }
            parsed.add(element);
            bounds = bounds == null ? element.bounds() : bounds.minmax(element.bounds());
        }
        if (parsed.isEmpty() || bounds == null) {
            throw new IllegalArgumentException("model contains no valid elements");
        }
        return new ModelGeometry(List.copyOf(parsed), clampModelBox(bounds));
    }

    private static ModelElement parseElement(JsonObject json) {
        JsonArray from = json.getAsJsonArray("from");
        JsonArray to = json.getAsJsonArray("to");
        if (from == null || to == null || from.size() < 3 || to.size() < 3) {
            return null;
        }

        AABB box = normalizedBox(
                from.get(0).getAsDouble(),
                from.get(1).getAsDouble(),
                from.get(2).getAsDouble(),
                to.get(0).getAsDouble(),
                to.get(1).getAsDouble(),
                to.get(2).getAsDouble());
        ElementRotation rotation = parseRotation(json.getAsJsonObject("rotation"));
        if (rotation == null || rotation.isZero()) {
            return new ModelElement(box, box, List.of());
        }

        AABB rotatedBounds = clampModelBox(rotatedBounds(box, rotation));
        return new ModelElement(
                box,
                rotatedBounds,
                List.copyOf(voxelizeRotatedElement(box, rotation, rotatedBounds)));
    }

    private static ElementRotation parseRotation(JsonObject json) {
        if (json == null) {
            return null;
        }
        JsonArray origin = json.getAsJsonArray("origin");
        if (origin == null || origin.size() < 3) {
            return null;
        }

        double x = radians(json, "x");
        double y = radians(json, "y");
        double z = radians(json, "z");
        JsonElement axis = json.get("axis");
        JsonElement angle = json.get("angle");
        if (axis != null && angle != null) {
            double axisAngle = Math.toRadians(angle.getAsDouble());
            switch (axis.getAsString()) {
                case "x" -> x = axisAngle;
                case "y" -> y = axisAngle;
                case "z" -> z = axisAngle;
                default -> {
                    return null;
                }
            }
        }

        return new ElementRotation(
                x,
                y,
                z,
                origin.get(0).getAsDouble(),
                origin.get(1).getAsDouble(),
                origin.get(2).getAsDouble());
    }

    private static double radians(JsonObject json, String key) {
        JsonElement value = json.get(key);
        return value == null ? 0.0 : Math.toRadians(value.getAsDouble());
    }

    private static List<AABB> voxelizeRotatedElement(
            AABB box,
            ElementRotation rotation,
            AABB bounds) {
        int countX = positiveCellCount(bounds.minX, bounds.maxX, ROTATED_VOXEL_STEP);
        int countY = positiveCellCount(bounds.minY, bounds.maxY, ROTATED_VOXEL_STEP);
        int countZ = positiveCellCount(bounds.minZ, bounds.maxZ, ROTATED_VOXEL_STEP);
        long estimatedCells = (long) countX * countY * countZ;
        if (estimatedCells > MAX_ROTATED_VOXELS) {
            LOGGER.warn(
                    "Rotated block model element needs {} voxels; using its bounds instead",
                    estimatedCells);
            return List.of(bounds);
        }

        List<AABB> boxes = new ArrayList<>((int) estimatedCells);
        double startX = Math.floor(bounds.minX / ROTATED_VOXEL_STEP) * ROTATED_VOXEL_STEP;
        double startY = Math.floor(bounds.minY / ROTATED_VOXEL_STEP) * ROTATED_VOXEL_STEP;
        double startZ = Math.floor(bounds.minZ / ROTATED_VOXEL_STEP) * ROTATED_VOXEL_STEP;
        double expansion = Math.max(ROTATED_VOXEL_STEP * 0.5, MIN_ROTATED_THICKNESS);

        for (double x = startX; x < bounds.maxX - EPSILON; x += ROTATED_VOXEL_STEP) {
            for (double y = startY; y < bounds.maxY - EPSILON; y += ROTATED_VOXEL_STEP) {
                for (double z = startZ; z < bounds.maxZ - EPSILON; z += ROTATED_VOXEL_STEP) {
                    double x1 = Math.min(x + ROTATED_VOXEL_STEP, bounds.maxX);
                    double y1 = Math.min(y + ROTATED_VOXEL_STEP, bounds.maxY);
                    double z1 = Math.min(z + ROTATED_VOXEL_STEP, bounds.maxZ);
                    double centerX = (Math.max(x, bounds.minX) + x1) * 0.5;
                    double centerY = (Math.max(y, bounds.minY) + y1) * 0.5;
                    double centerZ = (Math.max(z, bounds.minZ) + z1) * 0.5;
                    double[] local = inverseRotatePoint(centerX, centerY, centerZ, rotation);
                    if (insideExpanded(box, local, expansion)) {
                        boxes.add(new AABB(
                                Math.max(x, bounds.minX),
                                Math.max(y, bounds.minY),
                                Math.max(z, bounds.minZ),
                                x1,
                                y1,
                                z1));
                    }
                }
            }
        }
        return boxes.isEmpty() ? List.of(bounds) : boxes;
    }

    private static boolean insideExpanded(AABB box, double[] point, double expansion) {
        return point[0] >= box.minX - expansion
                && point[0] <= box.maxX + expansion
                && point[1] >= box.minY - expansion
                && point[1] <= box.maxY + expansion
                && point[2] >= box.minZ - expansion
                && point[2] <= box.maxZ + expansion;
    }

    private static AABB rotatedBounds(AABB box, ElementRotation rotation) {
        AABB result = null;
        for (double x : new double[]{box.minX, box.maxX}) {
            for (double y : new double[]{box.minY, box.maxY}) {
                for (double z : new double[]{box.minZ, box.maxZ}) {
                    double[] point = rotatePoint(x, y, z, rotation);
                    AABB pointBox = new AABB(
                            point[0], point[1], point[2],
                            point[0], point[1], point[2]);
                    result = result == null ? pointBox : result.minmax(pointBox);
                }
            }
        }
        return result == null ? box : result;
    }

    private static double[] rotatePoint(double x, double y, double z, ElementRotation rotation) {
        double localX = x - rotation.originX();
        double localY = y - rotation.originY();
        double localZ = z - rotation.originZ();
        double[] rotated = rotateX(localX, localY, localZ, rotation.xRadians());
        rotated = rotateY(rotated[0], rotated[1], rotated[2], rotation.yRadians());
        rotated = rotateZ(rotated[0], rotated[1], rotated[2], rotation.zRadians());
        return new double[]{
                rotation.originX() + rotated[0],
                rotation.originY() + rotated[1],
                rotation.originZ() + rotated[2]};
    }

    private static double[] inverseRotatePoint(double x, double y, double z, ElementRotation rotation) {
        double localX = x - rotation.originX();
        double localY = y - rotation.originY();
        double localZ = z - rotation.originZ();
        double[] rotated = rotateZ(localX, localY, localZ, -rotation.zRadians());
        rotated = rotateY(rotated[0], rotated[1], rotated[2], -rotation.yRadians());
        rotated = rotateX(rotated[0], rotated[1], rotated[2], -rotation.xRadians());
        return new double[]{
                rotation.originX() + rotated[0],
                rotation.originY() + rotated[1],
                rotation.originZ() + rotated[2]};
    }

    private static double[] rotateX(double x, double y, double z, double angle) {
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        return new double[]{x, y * cos - z * sin, y * sin + z * cos};
    }

    private static double[] rotateY(double x, double y, double z, double angle) {
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        return new double[]{x * cos + z * sin, y, -x * sin + z * cos};
    }

    private static double[] rotateZ(double x, double y, double z, double angle) {
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        return new double[]{x * cos - y * sin, x * sin + y * cos, z};
    }

    private static void addSplitBox(Map<Cell, List<AABB>> boxesByCell, AABB rawBox) {
        AABB box = clampModelBox(rawBox);
        int minX = Math.max(-MAX_PART_OFFSET, cellIndex(box.minX, false));
        int minY = Math.max(-MAX_PART_OFFSET, cellIndex(box.minY, false));
        int minZ = Math.max(-MAX_PART_OFFSET, cellIndex(box.minZ, false));
        int maxX = Math.min(MAX_PART_OFFSET, cellIndex(box.maxX, true));
        int maxY = Math.min(MAX_PART_OFFSET, cellIndex(box.maxY, true));
        int maxZ = Math.min(MAX_PART_OFFSET, cellIndex(box.maxZ, true));

        for (int cellX = minX; cellX <= maxX; cellX++) {
            for (int cellY = minY; cellY <= maxY; cellY++) {
                for (int cellZ = minZ; cellZ <= maxZ; cellZ++) {
                    double cellMinX = cellX * 16.0;
                    double cellMinY = cellY * 16.0;
                    double cellMinZ = cellZ * 16.0;
                    double minLocalX = Math.max(box.minX, cellMinX);
                    double minLocalY = Math.max(box.minY, cellMinY);
                    double minLocalZ = Math.max(box.minZ, cellMinZ);
                    double maxLocalX = Math.min(box.maxX, cellMinX + 16.0);
                    double maxLocalY = Math.min(box.maxY, cellMinY + 16.0);
                    double maxLocalZ = Math.min(box.maxZ, cellMinZ + 16.0);
                    if (maxLocalX <= minLocalX
                            || maxLocalY <= minLocalY
                            || maxLocalZ <= minLocalZ) {
                        continue;
                    }
                    boxesByCell.computeIfAbsent(
                                    new Cell(cellX, cellY, cellZ),
                                    ignored -> new ArrayList<>())
                            .add(new AABB(
                                    (minLocalX - cellMinX) / 16.0,
                                    (minLocalY - cellMinY) / 16.0,
                                    (minLocalZ - cellMinZ) / 16.0,
                                    (maxLocalX - cellMinX) / 16.0,
                                    (maxLocalY - cellMinY) / 16.0,
                                    (maxLocalZ - cellMinZ) / 16.0));
                }
            }
        }
    }

    private static VoxelShape shapeFromBoxes(List<AABB> rawBoxes) {
        List<AABB> boxes = optimizeBoxes(rawBoxes);
        if (boxes.isEmpty()) {
            return Shapes.empty();
        }
        VoxelShape result = Shapes.empty();
        for (AABB box : boxes) {
            result = Shapes.joinUnoptimized(result, Shapes.create(box), BooleanOp.OR);
        }
        return result.optimize();
    }

    private static List<AABB> optimizeBoxes(List<AABB> rawBoxes) {
        List<AABB> current = new ArrayList<>();
        for (AABB box : rawBoxes) {
            if (box.maxX > box.minX && box.maxY > box.minY && box.maxZ > box.minZ) {
                current.add(box);
            }
        }
        int previousSize;
        do {
            previousSize = current.size();
            current = mergeOnAxis(current, Direction.Axis.X);
            current = mergeOnAxis(current, Direction.Axis.Y);
            current = mergeOnAxis(current, Direction.Axis.Z);
        } while (current.size() < previousSize);
        return current;
    }

    private static List<AABB> mergeOnAxis(List<AABB> boxes, Direction.Axis axis) {
        if (boxes.size() < 2) {
            return boxes;
        }
        List<AABB> remaining = new ArrayList<>(boxes);
        List<AABB> merged = new ArrayList<>(boxes.size());
        while (!remaining.isEmpty()) {
            AABB current = remaining.removeLast();
            boolean changed;
            do {
                changed = false;
                for (int index = remaining.size() - 1; index >= 0; index--) {
                    AABB candidate = remaining.get(index);
                    if (canMerge(current, candidate, axis)) {
                        current = merge(current, candidate);
                        remaining.remove(index);
                        changed = true;
                    }
                }
            } while (changed);
            merged.add(current);
        }
        return merged;
    }

    private static boolean canMerge(AABB first, AABB second, Direction.Axis axis) {
        return switch (axis) {
            case X -> same(first.minY, second.minY)
                    && same(first.maxY, second.maxY)
                    && same(first.minZ, second.minZ)
                    && same(first.maxZ, second.maxZ)
                    && (same(first.maxX, second.minX) || same(second.maxX, first.minX));
            case Y -> same(first.minX, second.minX)
                    && same(first.maxX, second.maxX)
                    && same(first.minZ, second.minZ)
                    && same(first.maxZ, second.maxZ)
                    && (same(first.maxY, second.minY) || same(second.maxY, first.minY));
            case Z -> same(first.minX, second.minX)
                    && same(first.maxX, second.maxX)
                    && same(first.minY, second.minY)
                    && same(first.maxY, second.maxY)
                    && (same(first.maxZ, second.minZ) || same(second.maxZ, first.minZ));
        };
    }

    private static AABB merge(AABB first, AABB second) {
        return new AABB(
                Math.min(first.minX, second.minX),
                Math.min(first.minY, second.minY),
                Math.min(first.minZ, second.minZ),
                Math.max(first.maxX, second.maxX),
                Math.max(first.maxY, second.maxY),
                Math.max(first.maxZ, second.maxZ));
    }

    private static JsonArray resolveElements(Identifier modelId) {
        Identifier current = modelId;
        for (int depth = 0; depth < MAX_PARENT_DEPTH; depth++) {
            JsonObject model = readJson(current);
            if (model == null) {
                return null;
            }
            JsonArray elements = model.getAsJsonArray("elements");
            if (elements != null && !elements.isEmpty()) {
                return elements;
            }
            JsonElement parent = model.get("parent");
            if (parent == null || !parent.isJsonPrimitive()) {
                return null;
            }
            String parentId = parent.getAsString();
            current = parentId.indexOf(':') >= 0
                    ? Identifier.parse(parentId)
                    : Identifier.fromNamespaceAndPath(current.getNamespace(), parentId);
        }
        return null;
    }

    private static JsonObject readJson(Identifier modelId) {
        String path = "assets/%s/models/%s.json".formatted(
                modelId.getNamespace(),
                modelId.getPath());
        try (InputStream stream = ModelShapeCache.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception exception) {
            LOGGER.warn("Failed to read automatic block shape model {}", modelId, exception);
            return null;
        }
    }

    private static ShapeSet fallback() {
        Map<Cell, VoxelShape> shapes = Map.of(Cell.ORIGIN, Shapes.block());
        return new ShapeSet(shapes, List.of(Cell.ORIGIN), Shapes.block());
    }

    private static AABB rotate90Y(AABB source, int turns) {
        AABB result = source;
        for (int turn = 0; turn < Math.floorMod(turns, 4); turn++) {
            result = new AABB(
                    16.0 - result.maxZ,
                    result.minY,
                    result.minX,
                    16.0 - result.minZ,
                    result.maxY,
                    result.maxX);
        }
        return result;
    }

    private static int horizontalTurns(Direction facing) {
        return switch (facing) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
    }

    private static AABB normalizedBox(
            double x1,
            double y1,
            double z1,
            double x2,
            double y2,
            double z2) {
        double minX = Math.min(x1, x2);
        double minY = Math.min(y1, y2);
        double minZ = Math.min(z1, z2);
        double maxX = Math.max(x1, x2);
        double maxY = Math.max(y1, y2);
        double maxZ = Math.max(z1, z2);
        double[] x = ensureThickness(minX, maxX);
        double[] y = ensureThickness(minY, maxY);
        double[] z = ensureThickness(minZ, maxZ);
        return clampModelBox(new AABB(x[0], y[0], z[0], x[1], y[1], z[1]));
    }

    private static double[] ensureThickness(double minimum, double maximum) {
        if (maximum - minimum > EPSILON) {
            return new double[]{minimum, maximum};
        }
        double halfThickness = MIN_ELEMENT_THICKNESS * 0.5;
        return new double[]{minimum - halfThickness, maximum + halfThickness};
    }

    private static AABB clampModelBox(AABB box) {
        return new AABB(
                clamp(box.minX),
                clamp(box.minY),
                clamp(box.minZ),
                clamp(box.maxX),
                clamp(box.maxY),
                clamp(box.maxZ));
    }

    private static double clamp(double coordinate) {
        return Math.max(MIN_MODEL_COORDINATE, Math.min(MAX_MODEL_COORDINATE, coordinate));
    }

    private static int cellIndex(double coordinate, boolean maximum) {
        return (int) Math.floor(coordinate / 16.0 + (maximum ? -EPSILON : EPSILON));
    }

    private static int positiveCellCount(double minimum, double maximum, double step) {
        return Math.max(1, (int) Math.ceil((maximum - minimum) / step));
    }

    private static boolean same(double first, double second) {
        return Math.abs(first - second) <= EPSILON;
    }

    record Cell(int x, int y, int z) {
        static final Cell ORIGIN = new Cell(0, 0, 0);
    }

    record ShapeSet(
            Map<Cell, VoxelShape> localShapes,
            List<Cell> occupiedCells,
            VoxelShape wholeShape) {
        VoxelShape localShape(Cell cell) {
            return localShapes.getOrDefault(cell, Shapes.empty());
        }
    }

    private record ShapeKey(Identifier modelId, Direction facing) {
    }

    private record ModelGeometry(List<ModelElement> elements, AABB bounds) {
    }

    private record ModelElement(AABB source, AABB bounds, List<AABB> voxelBoxes) {
    }

    private record ElementRotation(
            double xRadians,
            double yRadians,
            double zRadians,
            double originX,
            double originY,
            double originZ) {
        boolean isZero() {
            return Math.abs(xRadians) <= EPSILON
                    && Math.abs(yRadians) <= EPSILON
                    && Math.abs(zRadians) <= EPSILON;
        }
    }
}
