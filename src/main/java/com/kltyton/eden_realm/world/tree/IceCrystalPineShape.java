package com.kltyton.eden_realm.world.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

/** Builds the reference pine crowns with fixed leaf patterns and height-dependent tiers. */
public final class IceCrystalPineShape {
    public record Position(int x, int y, int z) {
        Position offset(int dx, int dy, int dz) { return new Position(x + dx, y + dy, z + dz); }
    }

    public enum Part {
        LEAF(false), LOG_X(false), LOG_Y(false), LOG_Z(false), ROOT_X(true), ROOT_Y(true), ROOT_Z(true), ROOT_BARK(true);
        public final boolean root;
        Part(boolean root) { this.root = root; }
    }

    private record Column(int x, int z) { }
    private static final int[][] CARDINAL = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};

    private IceCrystalPineShape() { }

    public static Map<Position, Part> create(int height, boolean large, int pairX, int pairZ) {
        var shape = new Builder();
        int trunkHeight = height - (large && height == 22 ? 2 : 3);
        for (int y = 0; y < trunkHeight; y++) shape.log(new Position(0, y, 0), Part.LOG_Y);
        if (large) shape.large(trunkHeight, height, pairX, pairZ);
        else shape.small(trunkHeight);
        shape.closeLongGaps();
        shape.symmetrizeProjection();
        return Collections.unmodifiableMap(shape.blocks);
    }

    private static final class Builder {
        private final Map<Position, Part> blocks = new LinkedHashMap<>();

        void log(Position pos, Part part) { blocks.put(pos, part); }
        void leaf(Position pos) { blocks.putIfAbsent(pos, Part.LEAF); }
        void root(Position pos, Part part) { blocks.putIfAbsent(pos, part); }

        void small(int height) {
            if (height <= 7) {
                branches(2, 1);
                layer(new Position(0, 2, 0), 2, true, 0);
                layer(new Position(0, height - 2, 0), 2, false, 0);
                layer(new Position(0, height - 1, 0), 1, false, 0);
            } else {
                boolean dense = height >= 11;
                int bottom = dense ? 3 : 2;
                int upper = height - 2;
                branches(bottom, 1);
                layer(new Position(0, bottom, 0), 3, true, dense ? 12 : 15);
                if (!dense) branches(bottom + 1, 2);
                layer(new Position(0, bottom + 1, 0), 3, false, 0);
                if (dense) layer(new Position(0, bottom + 2, 0), 1, false, 0);
                int middle = upper - 3;
                if (middle > bottom + 2) {
                    branches(middle, 1);
                    layer(new Position(0, middle, 0), 3, false, 0);
                    layer(new Position(0, middle + 1, 0), 2, dense, 0);
                    if (dense) layer(new Position(0, middle + 2, 0), 1, false, 0);
                }
                branches(upper, 1);
                layer(new Position(0, upper, 0), 2, false, 0);
                layer(new Position(0, upper + 1, 0), 1, false, 0);
            }
            tip(height, true);
        }

        void branches(int y, int length) {
            for (int[] d : CARDINAL) {
                for (int step = 1; step <= length; step++)
                    log(new Position(d[0] * step, y, d[1] * step), axis(d[0], d[1]));
            }
        }

        void large(int height, int totalHeight, int pairX, int pairZ) {
            boolean shortCrown = totalHeight == 22;
            for (int y = 0; y < 3; y++) log(new Position(pairX, y, pairZ), Part.LOG_Y);
            // Every upper root has a complete lower tier beneath it.
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (Math.abs(x) + Math.abs(z) <= 3) root(new Position(x, 0, z), Part.ROOT_BARK);
                }
            }
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) root(new Position(x, 1, z), Part.ROOT_Y);
            }
            for (int[] d : CARDINAL) root(new Position(d[0] * 3, 0, d[1] * 3), Part.ROOT_BARK);
            for (int[] d : CARDINAL) {
                Part along = d[0] == 0 ? Part.ROOT_Z : Part.ROOT_X;
                for (int step = 1; step <= 5; step++)
                    root(new Position(d[0] * step, -1, d[1] * step), step == 5 ? Part.ROOT_Y : along);
                Part across = d[0] == 0 ? Part.ROOT_X : Part.ROOT_Z;
                for (int side : new int[]{-1, 1}) {
                    root(new Position(d[0] * 3 - d[1] * side, -1, d[1] * 3 + d[0] * side), across);
                    root(new Position(d[0] * 3 - d[1] * side * 2, -1, d[1] * 3 + d[0] * side * 2), Part.ROOT_Y);
                }
            }

            for (int i = 0; i < 4; i++) {
                int[] d = CARDINAL[i];
                int y = i == 0 ? 3 : 4;
                Position end = risingBranch(new Position(0, y, 0), d[0], d[1], shortCrown ? 3 : 4);
                tuft(end, 2);
            }
            for (int i = 0; i < 4; i++) {
                int[] d = CARDINAL[i];
                int y = i == 0 ? 6 : i == 2 ? 8 : 7;
                Position end = risingBranch(new Position(0, y, 0), d[0], d[1], i == 2 && !shortCrown ? 4 : 3);
                tuft(end, 2);
            }
            Position split = new Position(-1, 6, 0);
            for (int side : new int[]{-1, 1}) {
                Position end = new Position(-3, 7, side * 2);
                limb(split, end);
                tuft(end, 1);
            }
            int[][] upper = {{1, 1, 11}, {-1, 1, 12}, {1, -1, 12}, {-1, -1, 13}};
            for (int[] d : upper) {
                Position end = new Position(d[0] * 2, d[2], d[1] * 2);
                limb(new Position(0, d[2] - 2, 0), end);
                taperedTuft(end);
            }
            Position[] tips = {new Position(-2, 15, 1), new Position(2, 14, 2), new Position(0, 15, -3)};
            for (Position end : tips) {
                limb(new Position(0, 13, 0), end);
                tuft(end, 1);
            }

            int start = height - (shortCrown ? 3 : 4);
            layer(new Position(0, start, 0), 2, false, 0);
            layer(new Position(0, start + 1, 0), 2, !shortCrown, shortCrown ? 0 : 15);
            if (!shortCrown) layer(new Position(0, height - 2, 0), 1, true, 0);
            layer(new Position(0, height - 1, 0), 1, false, 0);
            tip(height, !shortCrown);
        }

        Position risingBranch(Position start, int dx, int dz, int length) {
            Position end = start;
            for (int step = 1; step <= length; step++) {
                end = start.offset(dx * step, step == length && length >= 4 ? 1 : 0, dz * step);
                log(end, step == length && length >= 4 ? Part.LOG_Y : axis(dx, dz));
            }
            if (length < 4) {
                end = end.offset(0, 1, 0);
                log(end, Part.LOG_Y);
            }
            return end;
        }

        void limb(Position start, Position end) {
            int dx = end.x() - start.x(), dy = end.y() - start.y(), dz = end.z() - start.z();
            int steps = Math.max(Math.abs(dy), Math.max(Math.abs(dx), Math.abs(dz)));
            Position previous = start;
            for (int i = 1; i <= steps; i++) {
                Position current = start.offset((int) Math.round(dx * i / (double) steps),
                        (int) Math.round(dy * i / (double) steps), (int) Math.round(dz * i / (double) steps));
                log(current, current.x() == previous.x() && current.z() == previous.z()
                        ? Part.LOG_Y : axis(current.x() - previous.x(), current.z() - previous.z()));
                previous = current;
            }
        }

        static Part axis(int dx, int dz) { return Math.abs(dx) > Math.abs(dz) ? Part.LOG_X : Part.LOG_Z; }

        void tuft(Position end, int radius) {
            layer(end.offset(0, -1, 0), radius, true, 0);
            layer(end, radius, false, 0);
            layer(end.offset(0, 1, 0), Math.max(0, radius - 1), false, 0);
        }

        void taperedTuft(Position end) {
            layer(end.offset(0, -1, 0), 2, false, 0);
            layer(end, 1, false, 0);
            leaf(end.offset(0, 1, 0));
        }

        void tip(int height, boolean longTip) {
            leaf(new Position(0, height, 0));
            if (longTip) {
                layer(new Position(0, height + 1, 0), 1, false, 0);
                leaf(new Position(0, height + 2, 0));
            } else {
                leaf(new Position(0, height, -1));
                leaf(new Position(0, height + 1, 0));
            }
        }

        void layer(Position center, int radius, boolean lobed, int notchMask) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) + Math.abs(z) > radius + (lobed ? 1 : 0)) continue;
                    int side = x == radius && z == 0 ? 1 : z == radius && x == 0 ? 2
                            : x == -radius && z == 0 ? 4 : z == -radius && x == 0 ? 8 : 0;
                    if ((notchMask & side) != 0) continue;
                    leaf(center.offset(x, 0, z));
                }
            }
        }

        void closeLongGaps() {
            var rows = new TreeSet<Integer>();
            blocks.forEach((p, part) -> { if (part == Part.LEAF) rows.add(p.y()); });
            int previous = rows.first();
            for (int y = previous + 1; y < rows.last(); y++) {
                if (rows.contains(y)) previous = y;
                else if (y - previous == 2) {
                    int source = rows.higher(y);
                    var row = blocks.entrySet().stream().filter(e -> e.getKey().y() == source && !e.getValue().root).toList();
                    for (var entry : row) {
                        Position p = entry.getKey();
                        blocks.putIfAbsent(new Position(p.x(), y, p.z()), entry.getValue());
                    }
                    rows.add(y);
                    previous = y;
                }
            }
        }

        void symmetrizeProjection() {
            var projection = new HashSet<Column>();
            var leaves = new ArrayList<Map.Entry<Position, Part>>();
            blocks.forEach((p, part) -> {
                if (part == Part.LEAF) {
                    projection.add(new Column(p.x(), p.z()));
                    leaves.add(Map.entry(p, part));
                }
            });
            for (var entry : leaves) {
                Position p = entry.getKey();
                for (Column c : new Column[]{new Column(-p.z(), p.x()), new Column(-p.x(), -p.z()), new Column(p.z(), -p.x())}) {
                    if (projection.add(c)) {
                        Position candidate = new Position(c.x(), p.y(), c.z());
                        while (blocks.containsKey(candidate)) candidate = candidate.offset(0, 1, 0);
                        leaf(candidate);
                    }
                }
            }
        }
    }
}
