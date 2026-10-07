package com.kltyton.eden_realm.world.terrain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/** Seeded, irregular biome regions with the landforms from the icy and silver-frost references. */
public final class IcyLandformDensity implements DensityFunction.SimpleFunction {
    public static final List<String> BIOMES = List.of("icy_rolling_hills", "glacier_meander",
            "crystal_lake_shore", "blue_ice_plateau", "ice_ridge_valley", "icefall_fjord",
            "frozen_fissure", "cold_spring_lowland", "crystal_stone_plain", "ice_crystal_basin",
            "silver_frost_hills", "frost_stream_valley", "silver_frost_lakeshore",
            "frost_rock_plateau", "snow_ridge_valley", "silver_frost_basin");
    private static final double CELL_SIZE = 768.0;
    private static final int NEIGHBORS = 3;
    private static final MapCodec<IcyLandformDensity> DATA_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            NoiseHolder.CODEC.fieldOf("layout_noise").forGetter(IcyLandformDensity::layoutNoise),
            NoiseHolder.CODEC.fieldOf("warp_noise").forGetter(IcyLandformDensity::warpNoise),
            NoiseHolder.CODEC.fieldOf("detail_noise").forGetter(IcyLandformDensity::detailNoise),
            TerrainProfile.CODEC.listOf().fieldOf("profiles").forGetter(IcyLandformDensity::profiles),
            Codec.BOOL.fieldOf("surface").forGetter(IcyLandformDensity::surface),
            Codec.BOOL.optionalFieldOf("caves", false).forGetter(value -> value.caves)
    ).apply(instance, IcyLandformDensity::new));
    public static final KeyDispatchDataCodec<IcyLandformDensity> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);

    private final NoiseHolder layoutNoise;
    private final NoiseHolder warpNoise;
    private final NoiseHolder detailNoise;
    private final List<TerrainProfile> profiles;
    private final boolean surface;
    private final boolean caves;
    private final int totalWeight;
    private final ThreadLocal<CavityColumns> cavityColumns = ThreadLocal.withInitial(CavityColumns::new);
    private static final class CavityColumns extends LinkedHashMap<Long, Sample> {
        CavityColumns() { super(128, 0.75f, true); }
        @Override protected boolean removeEldestEntry(Map.Entry<Long, Sample> eldest) { return size() > 4096; }
    }
    private final ThreadLocal<CellWindow> cellWindow = new ThreadLocal<>();
    private final ThreadLocal<CellCache> cellsByGrid = ThreadLocal.withInitial(CellCache::new);
    private final ThreadLocal<PointCache> points = ThreadLocal.withInitial(PointCache::new);
    private final ThreadLocal<ConnectionCache> connections = ThreadLocal.withInitial(ConnectionCache::new);

    private record Cell(double x, double z, double cosine, double sine, double variation, int biome, long key) { }
    private record CellWindow(int x, int z, Cell[] cells) { }
    private record Connection(Cell downstream, List<MainlandRiverPath> paths, double cosine, double sine, double scale) { }
    private record FjordPoint(double influence, double height, int water, boolean reservoir,
                              boolean fall, int dx, int dz, double u, double v) { }
    private static final class PointCache extends LinkedHashMap<Long, Sample> {
        PointCache() { super(128, 0.75f, true); }
        @Override protected boolean removeEldestEntry(Map.Entry<Long, Sample> entry) { return size() > 4096; }
    }
    private static final class ConnectionCache extends LinkedHashMap<Long, Connection> {
        ConnectionCache() { super(32, 0.75f, true); }
        @Override protected boolean removeEldestEntry(Map.Entry<Long, Connection> entry) { return size() > 256; }
    }

    public static final class Sample {
        private double height;
        private int biome;
        private int landform;
        private int waterLevel, fallDx, fallDz;
        private boolean reservoir, frozenFall;
        private double u, v;
        public double height() { return height; }
        public int biome() { return biome; }
        public int waterLevel() { return waterLevel; }
        public boolean reservoir() { return reservoir; }
        public boolean frozenFall() { return frozenFall; }
        public int fallDx() { return fallDx; }
        public int fallDz() { return fallDz; }
    }

    private static final class CellCache extends LinkedHashMap<Long, Cell> {
        private CellCache() { super(128, 0.75f, true); }
        @Override protected boolean removeEldestEntry(Map.Entry<Long, Cell> eldest) { return size() > 4096; }
    }

    public IcyLandformDensity(NoiseHolder layoutNoise, NoiseHolder warpNoise, NoiseHolder detailNoise,
                             List<TerrainProfile> profiles, boolean surface) {
        this(layoutNoise, warpNoise, detailNoise, profiles, surface, false);
    }

    public IcyLandformDensity(NoiseHolder layoutNoise, NoiseHolder warpNoise, NoiseHolder detailNoise,
                             List<TerrainProfile> profiles, boolean surface, boolean caves) {
        this.layoutNoise = layoutNoise;
        this.warpNoise = warpNoise;
        this.detailNoise = detailNoise;
        this.profiles = List.copyOf(profiles);
        if (profiles.size() != 10 && profiles.size() != BIOMES.size())
            throw new IllegalArgumentException("Expected ten legacy or sixteen mainland terrain profiles");
        this.surface = surface;
        this.caves = caves;
        totalWeight = profiles.stream().mapToInt(TerrainProfile::generationChancePercent).sum();
        if (totalWeight == 0) throw new IllegalArgumentException("At least one biome must be enabled");
    }

    private NoiseHolder layoutNoise() { return layoutNoise; }
    private NoiseHolder warpNoise() { return warpNoise; }
    private NoiseHolder detailNoise() { return detailNoise; }
    private List<TerrainProfile> profiles() { return profiles; }
    private boolean surface() { return surface; }

    @Override
    public double compute(FunctionContext context) {
        if (!caves) {
            Sample value = sample(context.blockX(), context.blockZ());
            return surface ? value.height : climate(value.biome, BIOMES.size());
        }
        if (profiles.size() <= 14 || profiles.get(14).generationChancePercent() == 0) return 100.0;
        long key = ((long) context.blockX() << 32) | (context.blockZ() & 0xffffffffL);
        Sample value = cavityColumns.get().computeIfAbsent(key,
                ignored -> sample(context.blockX(), context.blockZ()));
        if (value.landform != 14) return 100.0;
        double bend = Math.sin(value.u / 160.0) * 28.0 + Math.sin(value.u / 61.0) * 9.0;
        double mouth = Math.max((-112.0 - value.u) / 8.0, (value.u + 22.0) / 8.0);
        double section = Math.max(Math.abs(value.v + bend) / 7.0 - 1.0,
                Math.abs(context.blockY() - 64.5 - profiles.get(14).elevationOffset()) / 3.5 - 1.0);
        return Math.clamp(Math.max(mouth, section) * 8.0, -100.0, 100.0);
    }

    public Sample sample(int x, int z) {
        long key = ((long) x << 32) | (z & 0xffffffffL);
        return points.get().computeIfAbsent(key, ignored -> {
            Sample result = new Sample();
            result.height = evaluate(x, z, result);
            return result;
        });
    }

    private double evaluate(double x, double z, Sample result) {
        double wx = x + warpNoise.getValue(x * 0.03, 0, z * 0.03) * 64.0;
        double wz = z + warpNoise.getValue(x * 0.03 + 317.0, 0, z * 0.03 - 193.0) * 64.0;
        Cell first = null;
        Cell second = null;
        double firstDistance = Double.POSITIVE_INFINITY;
        double secondDistance = Double.POSITIVE_INFINITY;
        Cell[] nearby = cells(wx, wz);
        for (Cell cell : nearby) {
            double dx = wx - cell.x();
            double dz = wz - cell.z();
            double distance = (dx * dx + dz * dz) * 100.0 / profiles.get(cell.biome()).biomeSizePercent();
            if (distance < firstDistance) {
                second = first;
                secondDistance = firstDistance;
                first = cell;
                firstDistance = distance;
            } else if (distance < secondDistance) {
                second = cell;
                secondDistance = distance;
            }
        }
        Cell background = background(nearby, wx, wz, first);
        double spacing = 100.0 / profiles.get(first.biome()).spacingPercent();
        double dx = (wx - first.x()) * spacing, dz = (wz - first.z()) * spacing;
        result.u = dx * first.cosine() + dz * first.sine();
        result.v = dz * first.cosine() - dx * first.sine();
        result.landform = first.biome();
        double influence = influence(first, wx, wz);
        int biome = influence >= 0.65 ? first.biome() : background.biome();
        double height = composedHeight(first, background, wx, wz);
        double gap = Math.sqrt(secondDistance) - Math.sqrt(firstDistance);
        if (gap < 56.0) {
            double blend = 0.5 * (1.0 - smooth(gap, 0.0, 56.0));
            height += (composedHeight(second, background, wx, wz) - height) * blend;
        }
        double fjordInfluence = 0, fjordHeight = height;
        boolean elevatedCatchment = false;
        for (Cell cell : nearby) {
            if (cell.biome() == 5) {
                FjordPoint fjord = fjord(cell, wx, wz, x, z);
                if (fjord.influence > fjordInfluence) {
                    fjordInfluence = fjord.influence;
                    fjordHeight = fjord.height;
                    if (fjordInfluence >= 0.65) biome = 5;
                    result.waterLevel = fjord.water;
                    result.reservoir = fjord.reservoir;
                    result.frozenFall = fjord.fall;
                    result.fallDx = fjord.dx;
                    result.fallDz = fjord.dz;
                    elevatedCatchment = fjordInfluence >= 0.65 && fjord.u > 0;
                }
            }
        }
        height += (fjordHeight - height) * fjordInfluence;
        double strongestRiver = 0;
        int riverBiome = -1;
        double riverBed = height;
        for (Cell cell : nearby) {
            if (elevatedCatchment || result.waterLevel > 63) break;
            if (cell.biome() != 1 && cell.biome() != 11 && cell.biome() != 5 && cell.biome() != 14) continue;
            for (MainlandRiverPath path : connection(cell).paths) {
                MainlandRiverPath.Hit hit = path.at(wx, wz);
                if (hit == null) continue;
                double bank = 10 + 3 * Math.sin(cell.variation * 6.28);
                double river = 1 - smooth(hit.distance(), hit.halfWidth(), hit.halfWidth() + bank);
                if (river > strongestRiver) {
                    strongestRiver = river;
                    riverBiome = hit.biome();
                    riverBed = 55 + detailNoise.getValue(wx * 0.45, 0, wz * 0.45) * 1.5;
                }
            }
        }
        height += (Math.min(riverBed, height) - height) * strongestRiver;
        boolean lakeShore = (first.biome == 2 || first.biome == 12) && influence >= 0.65;
        if (!lakeShore && strongestRiver > 0.25 && height <= 67 && riverBiome >= 0 && result.waterLevel <= 63)
            biome = riverBiome;
        if ((first.biome() == 1 || first.biome() == 11) && strongestRiver == 0 && connection(first).paths.isEmpty()) {
            double bend = Math.sin(result.u / 160) * 28 + Math.sin(result.u / 61) * 9;
            if (Math.abs(result.v + bend) > 30 || height > 67) biome = background.biome();
        } else if ((first.biome() == 1 || first.biome() == 11) && strongestRiver == 0) {
            biome = background.biome();
        }
        result.biome = biome;
        return Math.clamp(height, -63.0, 300.0);
    }

    private boolean enabled(int biome) {
        return biome < profiles.size() && profiles.get(biome).generationChancePercent() > 0;
    }

    private static boolean areaBiome(int biome) {
        return biome == 0 || biome == 3 || biome == 7 || biome == 8 || biome == 10 || biome == 13;
    }

    private Cell background(Cell[] nearby, double x, double z, Cell owner) {
        Cell best = owner;
        double closest = Double.POSITIVE_INFINITY;
        for (Cell cell : nearby) {
            if (!areaBiome(cell.biome)) continue;
            double distance = (Math.pow(x - cell.x, 2) + Math.pow(z - cell.z, 2))
                    * 100 / profiles.get(cell.biome).biomeSizePercent();
            if (distance < closest) { closest = distance; best = cell; }
        }
        return best;
    }

    private double composedHeight(Cell cell, Cell background, double x, double z) {
        if (cell.biome == 5 || ((cell.biome == 1 || cell.biome == 11) && !connection(cell).paths.isEmpty())) {
            return areaBiome(background.biome) ? height(background, x, z) : 80 + detailNoise.getValue(x * 0.2, 0, z * 0.2) * 8;
        }
        double height = height(cell, x, z), influence = influence(cell, x, z);
        if (cell != background && influence < 1) height = height(background, x, z)
                + (height - height(background, x, z)) * influence;
        return height;
    }

    private double influence(Cell cell, double x, double z) {
        if (areaBiome(cell.biome)) return 1;
        TerrainProfile profile = profiles.get(cell.biome);
        double spacing = 100.0 / profile.spacingPercent();
        double dx = (x - cell.x) * spacing, dz = (z - cell.z) * spacing;
        double u = dx * cell.cosine + dz * cell.sine, v = dz * cell.cosine - dx * cell.sine;
        double bend = Math.sin(u / 160) * 28 + Math.sin(u / 61) * 9;
        double distance = Math.abs(v + bend);
        double rough = detailNoise.getValue(x * spacing * 0.65, 0, z * spacing * 0.65);
        double broad = detailNoise.getValue(x * spacing * 0.18, 0, z * spacing * 0.18);
        return switch (cell.biome) {
            case 1, 11 -> connection(cell).paths.isEmpty() ? 1 - smooth(distance, 24, 54) : 0;
            case 2 -> 1 - smooth(Math.hypot(u * 0.92, v * 1.08) + rough * 24,
                    155 + cell.variation * 70 + 45, 155 + cell.variation * 70 + 90);
            case 4, 14 -> (1 - smooth(distance, 180, 240)) * (1 - smooth(Math.abs(u), 300, 390));
            case 5 -> 0;
            case 6 -> 1 - smooth(distance, 15, 28);
            case 9, 15 -> 1 - smooth(Math.hypot(u * (0.85 + cell.variation * 0.25), v)
                    + broad * 32 + rough * 10, 225, 280);
            case 12 -> 1 - smooth(Math.hypot(u * 0.88, v * 1.12) + broad * 35 + rough * 14,
                    205 + cell.variation * 55 + 50, 205 + cell.variation * 55 + 110);
            default -> throw new IllegalStateException("Unknown localized mainland biome: " + cell.biome);
        };
    }

    private Connection connection(Cell source) {
        return connections.get().computeIfAbsent(source.key, ignored -> {
            Cell[] nearby = cells(source.x, source.z);
            double spacing = 100.0 / profiles.get(source.biome).spacingPercent();
            double clearance = CELL_SIZE;
            double size = Math.sqrt(profiles.get(source.biome).biomeSizePercent());
            for (Cell cell : nearby) {
                if (cell.key == source.key) continue;
                clearance = Math.min(clearance, Math.hypot(source.x - cell.x, source.z - cell.z)
                        * size / (size + Math.sqrt(profiles.get(cell.biome).biomeSizePercent())) - 20);
            }
            double scale = Math.min(1 / spacing, clearance / 320);
            var paths = new java.util.ArrayList<MainlandRiverPath>();
            Cell target = null;
            double cosine = source.cosine, sine = source.sine;
            if (source.biome == 1 || source.biome == 11) {
                int preferred = source.biome == 11 && enabled(12) ? 12 : 2;
                Cell first = nearest(nearby, source, preferred);
                if (first == null) first = nearest(nearby, source, preferred == 2 ? 12 : 2);
                if (first != null) {
                    paths.add(new MainlandRiverPath(source.x, source.z, cosine, sine, first.x, first.z, 0, 0,
                            source.biome, source.variation, 1 / spacing));
                    Cell opposite = null;
                    double best = Double.POSITIVE_INFINITY;
                    double ax = first.x - source.x, az = first.z - source.z;
                    for (Cell cell : nearby) {
                        if ((cell.biome != 2 && cell.biome != 12) || cell.key == first.key) continue;
                        double bx = cell.x - source.x, bz = cell.z - source.z;
                        double distance = Math.hypot(bx, bz);
                        double score = distance * (ax * bx + az * bz < 0 ? 1 : 3);
                        if (score < best) { best = score; opposite = cell; }
                    }
                    if (opposite != null) paths.add(new MainlandRiverPath(source.x, source.z, cosine, sine,
                            opposite.x, opposite.z, 0, 0, source.biome, 1 - source.variation, 1 / spacing));
                }
            } else if (source.biome == 5 || source.biome == 14) {
                int preferred = source.biome == 5 || !enabled(11) ? 1 : 11;
                target = nearest(nearby, source, preferred);
                if (target == null && source.biome == 14) target = nearest(nearby, source, preferred == 1 ? 11 : 1);
                if (target != null) {
                    double distance = Math.hypot(source.x - target.x, source.z - target.z);
                    if (source.biome == 5) {
                        cosine = (source.x - target.x) / distance;
                        sine = (source.z - target.z) / distance;
                    }
                    double offset = Math.min((source.biome == 5 ? 110 * scale : 180 / spacing), distance * 0.35);
                    double sign = source.biome == 5 ? -1 : 1;
                    double x = source.x + cosine * offset * sign, z = source.z + sine * offset * sign;
                    paths.add(new MainlandRiverPath(x, z, cosine, sine, target.x, target.z,
                            target.cosine, target.sine, target.biome, source.variation,
                            100.0 / profiles.get(target.biome).spacingPercent()));
                }
            }
            return new Connection(target, List.copyOf(paths), cosine, sine, scale);
        });
    }

    private static Cell nearest(Cell[] nearby, Cell source, int biome) {
        Cell result = null;
        double best = Double.POSITIVE_INFINITY;
        for (Cell cell : nearby) {
            if (cell.biome != biome) continue;
            double distance = Math.hypot(source.x - cell.x, source.z - cell.z);
            if (distance < best) { best = distance; result = cell; }
        }
        return result;
    }

    private FjordPoint fjord(Cell cell, double x, double z, double worldX, double worldZ) {
        Connection frame = connection(cell);
        double dx = x - cell.x, dz = z - cell.z;
        double u = (dx * frame.cosine + dz * frame.sine) / frame.scale;
        double v = (dz * frame.cosine - dx * frame.sine) / frame.scale;
        double bend = Math.sin(u / 160) * 28 + Math.sin(u / 61) * 9;
        double stream = Math.abs(v + bend);
        double influence = (1 - smooth(Math.abs(u), 180, 240)) * (1 - smooth(stream, 100, 160));
        TerrainProfile profile = profiles.get(5);
        double shape = profile.shapePercent() / 100.0, relief = profile.reliefPercent() / 100.0;
        int head = (int) Math.clamp(Math.round(63 + Math.max(12, 52 * shape) + profile.elevationOffset()), 63, 289);
        double width = Math.max(12, 6 / frame.scale);
        double side = smooth(stream, width + 5, width + 68);
        double rough = detailNoise.getValue(x * 0.4, 0, z * 0.4) * 2 * relief;
        double lower = 54 + side * 72 * shape;
        double upper = head + 5 + side * 38 * shape;
        double height = lower + (upper - lower) * smooth(u, -8, 8) + rough;
        double channel = smooth(u, 0, 8) * (1 - smooth(u, 155, 205))
                * (1 - smooth(stream, width, width + 12));
        height += (head - 6 - height) * channel;
        double lakeBend = Math.sin(80.0 / 160) * 28 + Math.sin(80.0 / 61) * 9;
        double lake = Math.hypot((u - 80) / 65, (v + lakeBend) / 50);
        double lakeWeight = 1 - smooth(lake, 1.2, 1.55);
        double lakeHeight = head - 7 + smooth(lake, 0.85, 1.25) * 17 + rough * 0.35;
        height += (lakeHeight - height) * lakeWeight;
        boolean fall = influence > 0.95 && Math.abs(u) < 9 && stream < width;
        boolean reservoir = lake < 1.2;
        int directionX = Math.abs(frame.cosine) >= Math.abs(frame.sine) ? (frame.cosine >= 0 ? 1 : -1) : 0;
        int directionZ = directionX == 0 ? (frame.sine >= 0 ? 1 : -1) : 0;
        if (fall) {
            double highestU = u;
            for (int i = 0; i < 4; i++) {
                int px = i == 0 ? -1 : i == 1 ? 1 : 0;
                int pz = i == 2 ? -1 : i == 3 ? 1 : 0;
                double nx = worldX + px, nz = worldZ + pz;
                double warpedX = nx + warpNoise.getValue(nx * 0.03, 0, nz * 0.03) * 64;
                double warpedZ = nz + warpNoise.getValue(nx * 0.03 + 317, 0, nz * 0.03 - 193) * 64;
                double nextU = ((warpedX - cell.x) * frame.cosine + (warpedZ - cell.z) * frame.sine) / frame.scale;
                if (nextU > highestU) { highestU = nextU; directionX = px; directionZ = pz; }
            }
            if (highestU == u) fall = false;
        }
        int water = influence > 0.95 && ((u > 4 && height < head && (reservoir || channel > 0.5)) || fall) ? head : 0;
        return new FjordPoint(influence, height, water, reservoir, fall, directionX, directionZ, u, v);
    }

    private Cell[] cells(double x, double z) {
        int cx = (int) Math.floor(x / CELL_SIZE);
        int cz = (int) Math.floor(z / CELL_SIZE);
        CellWindow cached = cellWindow.get();
        if (cached != null && cached.x() == cx && cached.z() == cz) return cached.cells();
        int side = NEIGHBORS * 2 + 1;
        Cell[] cells = new Cell[side * side];
        int index = 0;
        CellCache grid = cellsByGrid.get();
        for (int dz = -NEIGHBORS; dz <= NEIGHBORS; dz++) {
            for (int dx = -NEIGHBORS; dx <= NEIGHBORS; dx++) {
                int gx = cx + dx;
                int gz = cz + dz;
                long key = ((long) gx << 32) | (gz & 0xffffffffL);
                Cell existing = grid.get(key);
                if (existing != null) {
                    cells[index++] = existing;
                    continue;
                }
                long seed = mix(Double.doubleToLongBits(layoutNoise.getValue(gx * 17.0, 0, gz * 19.0))
                        ^ gx * 0x9E3779B97F4A7C15L ^ gz * 0xC2B2AE3D27D4EB4FL);
                double jitterX = unit(seed) * 0.7 - 0.35;
                double jitterZ = unit(mix(seed)) * 0.7 - 0.35;
                double angle = unit(mix(seed + 1)) * Math.PI * 2.0;
                double choice = unit(mix(seed + 2)) * totalWeight;
                int biome = 0;
                while (choice >= profiles.get(biome).generationChancePercent()) {
                    choice -= profiles.get(biome).generationChancePercent();
                    biome++;
                }
                cells[index++] = new Cell((gx + 0.5 + jitterX) * CELL_SIZE,
                        (gz + 0.5 + jitterZ) * CELL_SIZE, Math.cos(angle), Math.sin(angle),
                        unit(mix(seed + 3)), biome, key);
                grid.put(key, cells[index - 1]);
            }
        }
        cellWindow.set(new CellWindow(cx, cz, cells));
        return cells;
    }

    public BlockPos referenceCenter(String biomeId, BlockPos near) {
        return findReferenceCenter(biomeId, near)
                .orElseThrow(() -> new IllegalArgumentException("Biome cell not found: " + biomeId));
    }

    public java.util.Optional<BlockPos> findReferenceCenter(String biomeId, BlockPos near) {
        int biome = BIOMES.indexOf(biomeId);
        if (biome < 0 || !enabled(biome)) return java.util.Optional.empty();
        var candidates = new java.util.ArrayList<Cell>();
        for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++) {
            for (Cell cell : cells(near.getX() + dx * CELL_SIZE * 7, near.getZ() + dz * CELL_SIZE * 7))
                if (cell.biome == biome) candidates.add(cell);
        }
        candidates.sort(java.util.Comparator.comparingDouble(cell -> Math.hypot(cell.x - near.getX(), cell.z - near.getZ())));
        for (Cell cell : candidates) {
            double cosine = cell.cosine, sine = cell.sine, offset = 0;
            if (biome == 5) {
                Connection frame = connection(cell);
                cosine = frame.cosine; sine = frame.sine; offset = -16 * frame.scale;
            } else if (biome == 14) {
                offset = -94.0 * profiles.get(14).spacingPercent() / 100.0;
            }
            double wx = cell.x + offset * cosine, wz = cell.z + offset * sine;
            double x = wx, z = wz;
            for (int i = 0; i < 8; i++) {
                x = wx - warpNoise.getValue(x * 0.03, 0, z * 0.03) * 64.0;
                z = wz - warpNoise.getValue(x * 0.03 + 317.0, 0, z * 0.03 - 193.0) * 64.0;
            }
            int centerX = (int) Math.round(x), centerZ = (int) Math.round(z);
            for (int radius = 0; radius <= 128; radius += 16) {
                for (int direction = 0; direction < (radius == 0 ? 1 : 8); direction++) {
                    double angle = direction * Math.PI / 4;
                    int px = centerX + (int) Math.round(Math.cos(angle) * radius);
                    int pz = centerZ + (int) Math.round(Math.sin(angle) * radius);
                    if (sample(px, pz).biome == biome) return java.util.Optional.of(new BlockPos(px, 96, pz));
                }
            }
        }
        return java.util.Optional.empty();
    }

    private double height(Cell cell, double x, double z) {
        TerrainProfile p = profiles.get(cell.biome());
        double spacing = 100.0 / p.spacingPercent();
        double dx = (x - cell.x()) * spacing;
        double dz = (z - cell.z()) * spacing;
        double u = dx * cell.cosine() + dz * cell.sine();
        double v = dz * cell.cosine() - dx * cell.sine();
        double rough = detailNoise.getValue(x * spacing * 0.65, 0, z * spacing * 0.65);
        double broad = detailNoise.getValue(x * spacing * 0.18, 0, z * spacing * 0.18);
        double relief = p.reliefPercent() / 100.0;
        double shape = p.shapePercent() / 100.0;
        double bend = Math.sin(u / 160.0) * 28.0 + Math.sin(u / 61.0) * 9.0;
        double distance = Math.abs(v + bend);
        double height = switch (cell.biome()) {
            case 0, 10 -> 82.0 + broad * 18.0 * shape + rough * 4.0 * relief;
            case 1, 11 -> 72.0 + rough * 3.0 * relief
                    - (1.0 - smooth(distance, 12.0, 34.0)) * 24.0 * shape;
            case 2 -> {
                double radius = Math.hypot(u * 0.92, v * 1.08) + rough * 24.0;
                double shore = 155.0 + cell.variation() * 70.0;
                yield 54.0 + smooth(radius, shore, shore + 72.0) * 19.0 * shape + rough * 2.0 * relief;
            }
            case 3 -> {
                double signal = detailNoise.getValue(x * spacing * 0.28, 0, z * spacing * 0.28) * 2.0;
                double level = Math.floor(signal);
                double terrace = level + smooth(signal - level, 0.92, 1.0);
                yield 84.0 + Math.max(-1.0, terrace) * 16.0 * shape + rough * relief;
            }
            case 4 -> {
                double sides = smooth(distance, 28.0, 180.0);
                yield 67.0 + sides * (105.0 + broad * 24.0) * shape + rough * (3.0 + sides * 5.0) * relief;
            }
            case 5 -> {
                double sides = smooth(distance, 28.0, 105.0);
                double upper = smooth(u, -10.0, 4.0);
                yield 52.0 + upper * 62.0 * shape + sides * 82.0 * shape + rough * 3.0 * relief;
            }
            case 6 -> 90.0 + rough * 4.0 * relief
                    - (1.0 - smooth(distance, 4.0, 15.0)) * 34.0 * shape;
            case 7 -> {
                double pools = detailNoise.getValue(x * spacing * 2.1, 0, z * spacing * 2.1);
                yield 66.0 + rough * 2.0 * relief - smooth(pools, 0.08, 0.26) * 10.0 * shape;
            }
            case 8 -> 77.0 + broad * 7.0 * shape + rough * 2.5 * relief;
            case 9, 15 -> {
                double radius = Math.hypot(u * (0.85 + cell.variation() * 0.25), v)
                        + broad * 32.0 + rough * 10.0;
                double lake = cell.variation() < 0.55
                        ? (1.0 - smooth(radius, 43.0, 76.0)) * 15.0 : 0.0;
                yield 68.0 - lake + smooth(radius, 100.0, 220.0) * 86.0 * shape + rough * 3.0 * relief;
            }
            case 12 -> {
                double radius = Math.hypot(u * 0.88, v * 1.12) + broad * 35.0 + rough * 14.0;
                double shore = 205.0 + cell.variation() * 55.0;
                double bank = smooth(radius, shore, shore + 105.0);
                double steps = Math.floor(bank * 8.0) * 3.0;
                double islands = smooth(detailNoise.getValue(x * spacing * 0.85 + 91, 0,
                        z * spacing * 0.85 - 37), 0.20, 0.38) * 16.0 * (1.0 - bank);
                yield 53.0 + steps * shape + islands + rough * 2.0 * relief;
            }
            case 13 -> {
                int gx = (int) Math.floor(u / 142.0), gz = (int) Math.floor(v / 142.0);
                long seed = mix(((long) gx << 32) ^ gz ^ Double.doubleToLongBits(cell.variation()));
                double px = u - (gx + 0.5) * 142.0 + (unit(seed) - 0.5) * 22.0;
                double pz = v - (gz + 0.5) * 142.0 + (unit(mix(seed)) - 0.5) * 22.0;
                double radius = 40.0 + unit(mix(seed + 1)) * 22.0;
                double edge = Math.hypot(px, pz) + rough * 5.0;
                double mesa = 1.0 - smooth(edge, radius - 5.0, radius + 2.0);
                yield 58.0 + mesa * (39.0 + unit(mix(seed + 2)) * 37.0) * shape
                        + rough * 1.5 * relief;
            }
            case 14 -> {
                double side = smooth(distance, 30.0, 200.0);
                double ridge = 118.0 + broad * 38.0 + Math.sin(u / 86.0) * 19.0;
                double channel = (1.0 - smooth(distance, 5.0, 13.0)) * 12.0;
                double caveHill = (1.0 - smooth(Math.abs(u + 94.0), 12.0, 44.0))
                        * (1.0 - smooth(distance, 6.0, 32.0)) * 19.0;
                yield 69.0 + side * ridge * shape - channel + caveHill + rough * (2.0 + side * 5.0) * relief;
            }
            default -> throw new IllegalStateException("Unknown mainland biome index: " + cell.biome());
        };
        if (cell.biome() == 14 && profiles.get(11).generationChancePercent() > 0) {
            TerrainProfile river = profiles.get(11);
            double continuation = 72.0 + rough * 3.0 * river.reliefPercent() / 100.0
                    - (1.0 - smooth(distance, 12.0, 34.0)) * 24.0 * river.shapePercent() / 100.0
                    + river.elevationOffset() - p.elevationOffset();
            height += (continuation - height) * smooth(u, 100.0, 240.0);
        }
        return height + p.elevationOffset();
    }

    public static double climate(int index, int count) { return -0.9 + index * 1.8 / (count - 1); }

    private static double smooth(double value, double low, double high) {
        double t = Math.clamp((value - low) / (high - low), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    private static double unit(long value) { return (value >>> 11) * 0x1.0p-53; }
    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    public static IcyLandformDensity from(net.minecraft.world.level.levelgen.RandomState state) {
        IcyLandformDensity[] result = new IcyLandformDensity[1];
        state.router().preliminarySurfaceLevel().mapAll(value -> {
            if (value instanceof IcyLandformDensity field && field.surface && !field.caves) result[0] = field;
            return value;
        });
        if (result[0] == null) throw new IllegalStateException("Mainland terrain field missing");
        return result[0];
    }

    @Override
    public DensityFunction mapChildren(Visitor visitor) {
        NoiseHolder layout = visitor.visitNoise(layoutNoise), warp = visitor.visitNoise(warpNoise);
        NoiseHolder detail = visitor.visitNoise(detailNoise);
        return layout == layoutNoise && warp == warpNoise && detail == detailNoise ? this
                : new IcyLandformDensity(layout, warp, detail, profiles, surface, caves);
    }

    @Override public double minValue() { return caves ? -100.0 : surface ? -63.0 : -0.9; }
    @Override public double maxValue() { return caves ? 100.0 : surface ? 300.0 : 0.9; }
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() { return CODEC; }
}
