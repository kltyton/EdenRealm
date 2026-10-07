package com.kltyton.eden_realm.world.terrain;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/** Island outlines and rock depths from the supplied structural voxel references. */
public final class SkyIslandShape {
    private static volatile List<Reference> references;
    private static final double ROCK_DEPTH = 0.50;
    private final Reference reference;
    private final int quarter;
    private final boolean mirrored;

    public static void loadReferences() {
        try {
            references = List.of(read("broad_meadow.bin.gz"), read("linked_rock.bin.gz"));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load Eden island structure references", exception);
        }
    }

    public SkyIslandShape(long seed, double rotation) {
        long variant = mix(seed);
        reference = references.get((int) (variant & 1));
        mirrored = (variant & 2) != 0;
        quarter = (int) Math.floorMod(Math.round(rotation / (Math.PI * 0.5)), 4L);
    }

    public double maxDepth() { return ROCK_DEPTH; }
    public double diameter() { return reference.diameter / reference.radius; }

    public double inset(double x, double z) {
        Coordinates point = localCoordinates(x, z);
        double px = point.x * reference.radius + reference.width * 0.5;
        double pz = point.z * reference.radius + reference.depth * 0.5;
        return reference.inset(px, pz) / reference.radius;
    }

    public double rockDepth(double x, double z) {
        Coordinates point = localCoordinates(x, z);
        return ROCK_DEPTH * reference.rockDepth(point.x * reference.radius + reference.width * 0.5,
                point.z * reference.radius + reference.depth * 0.5);
    }

    public record Coordinates(double x, double z) { }

    public Coordinates localCoordinates(double x, double z) {
        Coordinates point = switch (quarter) {
            case 1 -> new Coordinates(z, -x);
            case 2 -> new Coordinates(-x, -z);
            case 3 -> new Coordinates(-z, x);
            default -> new Coordinates(x, z);
        };
        return mirrored ? new Coordinates(-point.x, point.z) : point;
    }

    public Coordinates worldCoordinates(double x, double z) {
        double px = mirrored ? -x : x;
        return switch (quarter) {
            case 1 -> new Coordinates(-z, px);
            case 2 -> new Coordinates(-px, -z);
            case 3 -> new Coordinates(z, -px);
            default -> new Coordinates(px, z);
        };
    }

    private record Point(double x, double z) { }
    private record Segment(double x0, double z0, double x1, double z1) {
        double distanceSquared(double x, double z) {
            double dx = x1 - x0, dz = z1 - z0;
            double fraction = Math.clamp(((x - x0) * dx + (z - z0) * dz) / (dx * dx + dz * dz), 0, 1);
            double rx = x - x0 - dx * fraction, rz = z - z0 - dz * fraction;
            return rx * rx + rz * rz;
        }
    }

    private static final class Reference {
        private final int width, height, depth;
        private final int[] bottoms;
        private final double[] distances;
        private final List<Segment> boundary;
        private final double radius, diameter;
        private final double upperWallDepth;

        private Reference(int width, int height, int depth, int[] bottoms) {
            this.width = width;
            this.height = height;
            this.depth = depth;
            this.bottoms = bottoms;
            var edges = new ArrayList<Segment>();
            for (int x = 0; x <= width; x++) {
                int start = -1;
                for (int z = 0; z <= depth; z++) {
                    boolean exposed = z < depth && contains(x - 1, z) != contains(x, z);
                    if (exposed && start < 0) start = z;
                    else if (!exposed && start >= 0) {
                        edges.add(new Segment(x, start, x, z));
                        start = -1;
                    }
                }
            }
            for (int z = 0; z <= depth; z++) {
                int start = -1;
                for (int x = 0; x <= width; x++) {
                    boolean exposed = x < width && contains(x, z - 1) != contains(x, z);
                    if (exposed && start < 0) start = x;
                    else if (!exposed && start >= 0) {
                        edges.add(new Segment(start, z, x, z));
                        start = -1;
                    }
                }
            }
            boundary = List.copyOf(edges);
            var corners = new ArrayList<Point>();
            double bound = 0;
            for (Segment edge : boundary) {
                corners.add(new Point(edge.x0, edge.z0));
                corners.add(new Point(edge.x1, edge.z1));
            }
            double span = 0;
            for (Point first : corners) {
                bound = Math.max(bound, Math.hypot(first.x - width * 0.5, first.z - depth * 0.5));
                for (Point second : corners) span = Math.max(span,
                        Math.hypot(first.x - second.x, first.z - second.z));
            }
            radius = bound;
            diameter = span;
            int commonWallDepth = height;
            for (int z = 0; z < depth; z++) for (int x = 0; x < width; x++) {
                if (contains(x, z) && (!contains(x - 1, z) || !contains(x + 1, z)
                        || !contains(x, z - 1) || !contains(x, z + 1)))
                    commonWallDepth = Math.min(commonWallDepth, height - bottoms[z * width + x]);
            }
            // The structural diagram's common upper wall is replaced by the actual surface strata.
            upperWallDepth = commonWallDepth * 0.80;
            distances = new double[(width + 2) * (depth + 2)];
            for (int z = -1; z <= depth; z++) for (int x = -1; x <= width; x++)
                distances[(z + 1) * (width + 2) + x + 1] = signedDistance(x + 0.5, z + 0.5);
        }

        private boolean contains(int x, int z) {
            return x >= 0 && z >= 0 && x < width && z < depth && bottoms[z * width + x] >= 0;
        }

        private double signedDistance(double x, double z) {
            double distance = Double.POSITIVE_INFINITY;
            for (Segment edge : boundary) distance = Math.min(distance, edge.distanceSquared(x, z));
            return (contains((int) Math.floor(x), (int) Math.floor(z)) ? 1 : -1) * Math.sqrt(distance);
        }

        private double inset(double x, double z) {
            int ix = (int) Math.floor(x - 0.5), iz = (int) Math.floor(z - 0.5);
            if (ix < -1 || iz < -1 || ix >= width || iz >= depth) return signedDistance(x, z);
            double tx = x - 0.5 - ix, tz = z - 0.5 - iz;
            int index = (iz + 1) * (width + 2) + ix + 1;
            double near = distances[index] * (1 - tx) + distances[index + 1] * tx;
            double far = distances[index + width + 2] * (1 - tx) + distances[index + width + 3] * tx;
            return near * (1 - tz) + far * tz;
        }

        private double rockDepth(double x, double z) {
            int ix = (int) Math.floor(x - 0.5), iz = (int) Math.floor(z - 0.5);
            double tx = x - 0.5 - ix, tz = z - 0.5 - iz;
            double total = 0, weight = 0;
            for (int dz = 0; dz <= 1; dz++) for (int dx = 0; dx <= 1; dx++) {
                if (!contains(ix + dx, iz + dz)) continue;
                double part = (dx == 0 ? 1 - tx : tx) * (dz == 0 ? 1 - tz : tz);
                total += (height - bottoms[(iz + dz) * width + ix + dx]) * part;
                weight += part;
            }
            return weight == 0 ? 0 : Math.max(0, (total / weight - upperWallDepth) / (height - upperWallDepth));
        }
    }

    private static Reference read(String name) throws IOException {
        String path = "/data/eden_realm/terrain/shape/" + name;
        InputStream resource = SkyIslandShape.class.getResourceAsStream(path);
        if (resource == null) throw new IOException("Missing island structure resource " + path);
        try (resource; var input = new DataInputStream(new GZIPInputStream(resource))) {
            if (input.readInt() != 0x45525348) throw new IOException("Invalid island structure header " + path);
            int width = input.readUnsignedShort(), height = input.readUnsignedShort(), depth = input.readUnsignedShort();
            if (width == 0 || depth == 0 || height == 0 || height > 255)
                throw new IOException("Invalid island structure dimensions " + path);
            int[] bottoms = new int[Math.multiplyExact(width, depth)];
            int occupied = 0;
            for (int index = 0; index < bottoms.length; index++) {
                int count = input.readUnsignedByte();
                if (count > 1) throw new IOException("Island structure contains a split solid column " + path);
                bottoms[index] = -1;
                if (count == 1) {
                    int from = input.readUnsignedByte(), to = input.readUnsignedByte();
                    if (from >= to || to > height) throw new IOException("Invalid island structure column " + path);
                    bottoms[index] = from;
                    occupied++;
                }
            }
            if (occupied == 0 || input.read() != -1) throw new IOException("Invalid island structure payload " + path);
            return new Reference(width, height, depth, bottoms);
        }
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
