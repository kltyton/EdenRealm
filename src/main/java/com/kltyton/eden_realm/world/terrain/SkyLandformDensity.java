package com.kltyton.eden_realm.world.terrain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/** The same seeded island columns drive solid rock, biome regions and surface water. */
public final class SkyLandformDensity implements DensityFunction.SimpleFunction {
    public static final List<String> BIOMES = List.of("cloud_sea_flatlands", "star_stream_plateau",
            "sky_mirror_lake", "cloud_island_chain", "rosy_cloud_terraces", "flower_mirror_lake");
    public static final String AIRSPACE = "sky_airspace";
    public static final List<String> ALL_BIOMES = java.util.stream.Stream.concat(
            BIOMES.stream(), java.util.stream.Stream.of(AIRSPACE)).toList();
    public static final int REGION = 0, SURFACE = 1, SOLID = 2;
    public static final int MIN_Y = -64, HEIGHT = 576;
    private static final double MAX_ISLAND_SCALE = 1.5;
    private static final double CELL_SIZE = 768.0 * MAX_ISLAND_SCALE;
    // The highest cloud foundation ends at -61; keep twelve blocks of air below the islands.
    private static final int MIN_ISLAND_BOTTOM = -49;
    private static final double SMALL_ISLAND_MAX_DIAMETER = 48.0;
    private static final double SMALL_ISLAND_RIM_WIDTH = 2.0;
    private static final MapCodec<SkyLandformDensity> DATA_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            NoiseHolder.CODEC.fieldOf("layout_noise").forGetter(value -> value.layout),
            NoiseHolder.CODEC.fieldOf("detail_noise").forGetter(value -> value.detail),
            TerrainProfile.CODEC.listOf().fieldOf("profiles").forGetter(value -> value.profiles),
            Codec.intRange(REGION, SOLID).fieldOf("mode").forGetter(value -> value.mode),
            Codec.doubleRange(1.0, MAX_ISLAND_SCALE).optionalFieldOf("vertical_scale", 1.0).forGetter(value -> value.maxIslandScale)
    ).apply(instance, SkyLandformDensity::new));
    public static final KeyDispatchDataCodec<SkyLandformDensity> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);
    private final NoiseHolder layout, detail;
    private final List<TerrainProfile> profiles;
    private final int mode, totalWeight;
    private final double maxIslandScale;
    private final ThreadLocal<ColumnCache> columns = ThreadLocal.withInitial(ColumnCache::new);
    private final ThreadLocal<CellCache> cells = ThreadLocal.withInitial(CellCache::new);
    private final ThreadLocal<ShapeCache> shapes = ThreadLocal.withInitial(ShapeCache::new);

    private record Cell(double x, double z, double angle,
                        double variation, double scale, int biome, double clearance) { }
    public record Column(int biome, int top, int bottom, int water, boolean land,
                         int rockTop, int rockBottom, boolean waterfall,
                         int dirtBottom, int cloudBottom, boolean smallIslandRim, boolean rockRim,
                         List<SolidSpan> solidSpans, double verticalScale) {
        public int biomeIndex() {
            return land || rockTop > rockBottom || waterfall ? biome : BIOMES.size();
        }
        public int scaledDepth(int depth) { return SkyLandformDensity.scaledDepth(depth, verticalScale); }
        private Column withoutWater() {
            return new Column(biome, top, bottom, 0, land, rockTop, rockBottom, false,
                    dirtBottom, cloudBottom, smallIslandRim, rockRim, solidSpans, verticalScale);
        }
    }
    public record SolidSpan(int fromY, int toY) { }
    private static final class ColumnCache extends LinkedHashMap<Long, Column> {
        ColumnCache() { super(256, 0.75f, true); }
        @Override protected boolean removeEldestEntry(Map.Entry<Long, Column> entry) { return size() > 4096; }
    }
    private static final class CellCache extends LinkedHashMap<Long, Cell> {
        CellCache() { super(64, 0.75f, true); }
        @Override protected boolean removeEldestEntry(Map.Entry<Long, Cell> entry) { return size() > 1024; }
    }
    private record ShapeKey(long seed, double rotation) { }
    private static final class ShapeCache extends LinkedHashMap<ShapeKey, SkyIslandShape> {
        ShapeCache() { super(32, 0.75f, true); }
        @Override protected boolean removeEldestEntry(Map.Entry<ShapeKey, SkyIslandShape> entry) { return size() > 256; }
    }
    private SkyIslandShape shape(long seed, double rotation) {
        return shapes.get().computeIfAbsent(new ShapeKey(seed, rotation),
                key -> new SkyIslandShape(key.seed(), key.rotation()));
    }

    public SkyLandformDensity(NoiseHolder layout, NoiseHolder detail, List<TerrainProfile> profiles, int mode) {
        this(layout, detail, profiles, mode, MAX_ISLAND_SCALE);
    }

    public SkyLandformDensity(NoiseHolder layout, NoiseHolder detail, List<TerrainProfile> profiles, int mode, double maxIslandScale) {
        if (profiles.size() != BIOMES.size()) throw new IllegalArgumentException("Six sky terrain profiles required");
        this.layout = layout;
        this.detail = detail;
        this.profiles = List.copyOf(profiles);
        this.mode = mode;
        this.maxIslandScale = maxIslandScale;
        totalWeight = profiles.stream().mapToInt(TerrainProfile::generationChancePercent).sum();
        if (totalWeight == 0) throw new IllegalArgumentException("At least one sky biome must be enabled");
    }

    @Override public double compute(FunctionContext context) {
        Column column = sample(context.blockX(), context.blockZ());
        if (mode == REGION) return IcyLandformDensity.climate(column.biomeIndex(), ALL_BIOMES.size());
        if (mode == SURFACE) return Math.max(column.land() ? column.top() : cloudTop(context.blockX(), context.blockZ()),
                column.rockTop() > 0 ? column.rockTop() : -64);
        double foundation = cloudTop(context.blockX(), context.blockZ()) - 0.5 - context.blockY();
        double mass = -32.0;
        for (SolidSpan span : column.solidSpans()) mass = Math.max(mass,
                Math.min(span.toY() - 0.5 - context.blockY(), context.blockY() - span.fromY() + 0.5));
        if (column.rockTop() > 0) mass = Math.max(mass, Math.min(column.rockTop() - 0.5 - context.blockY(),
                context.blockY() - column.rockBottom() + 0.5));
        return Math.clamp(Math.max(foundation, mass) / 4.0, -100.0, 100.0);
    }

    public Column sample(int x, int z) {
        long key = ((long) x << 32) | (z & 0xffffffffL);
        return columns.get().computeIfAbsent(key, ignored -> {
            Column value = column(x, z);
            return value.waterfall() && !hasStreamSource(x, z, value.water()) ? value.withoutWater() : value;
        });
    }

    private boolean hasStreamSource(int x, int z, int water) {
        for (int i = 0; i < 4; i++) {
            int dx = i == 0 ? -1 : i == 1 ? 1 : 0;
            int dz = i == 2 ? -1 : i == 3 ? 1 : 0;
            Column source = column(x + dx, z + dz);
            if (source.biome() == 1 && source.land() && source.water() == water && source.top() < water)
                return true;
        }
        return false;
    }

    public int cloudTop(int x, int z) {
        return detail.getValue(x * 0.12, 0, z * 0.12) < -0.12 ? -62 : -61;
    }

    private static double scaledHeight(double y, double scale) {
        return MIN_ISLAND_BOTTOM + (y - MIN_ISLAND_BOTTOM) * scale;
    }

    private static int scaledDepth(int depth, double scale) { return (int) Math.ceil(depth * scale); }

    public int maxY() { return MIN_Y + HEIGHT; }

    private double islandScale(long seed) {
        return 1.0 + (maxIslandScale - 1.0) * unit(mix(seed + 4));
    }

    private Cell cell(int gx, int gz) {
        long key = ((long) gx << 32) | (gz & 0xffffffffL);
        return cells.get().computeIfAbsent(key, ignored -> {
            Cell center = rawCell(gx, gz);
            double size = Math.sqrt(profiles.get(center.biome()).biomeSizePercent());
            double clearance = CELL_SIZE;
            for (int dz = -2; dz <= 2; dz++) for (int dx = -2; dx <= 2; dx++) {
                if (dx == 0 && dz == 0) continue;
                Cell neighbor = rawCell(gx + dx, gz + dz);
                double neighborSize = Math.sqrt(profiles.get(neighbor.biome()).biomeSizePercent());
                // Keep the entire outline inside its weighted region, with an air gap.
                clearance = Math.min(clearance, Math.hypot(center.x() - neighbor.x(), center.z() - neighbor.z())
                        * size / (size + neighborSize) - 16);
            }
            return new Cell(center.x(), center.z(), center.angle(), center.variation(), center.scale(),
                    center.biome(), clearance);
        });
    }

    private Cell rawCell(int gx, int gz) {
        long seed = mix(Double.doubleToLongBits(layout.getValue(gx * 17.0, 0, gz * 19.0))
                ^ gx * 0x9E3779B97F4A7C15L ^ gz * 0xC2B2AE3D27D4EB4FL);
        double choice = unit(mix(seed + 2)) * totalWeight;
        int biome = 0;
        while (choice >= profiles.get(biome).generationChancePercent()) {
            choice -= profiles.get(biome).generationChancePercent();
            biome++;
        }
        return new Cell((gx + 0.5 + unit(seed) * 0.5 - 0.25) * CELL_SIZE,
                (gz + 0.5 + unit(mix(seed)) * 0.5 - 0.25) * CELL_SIZE,
                unit(mix(seed + 1)) * Math.PI * 2, unit(mix(seed + 3)), islandScale(seed), biome, 0);
    }

    private Cell nearest(double x, double z) {
        int gx = (int) Math.floor(x / CELL_SIZE), gz = (int) Math.floor(z / CELL_SIZE);
        Cell nearest = null;
        double distance = Double.POSITIVE_INFINITY;
        for (int dz = -2; dz <= 2; dz++) for (int dx = -2; dx <= 2; dx++) {
            Cell cell = cell(gx + dx, gz + dz);
            double candidate = Math.pow(x - cell.x(), 2) + Math.pow(z - cell.z(), 2);
            candidate *= 100.0 / profiles.get(cell.biome()).biomeSizePercent();
            if (candidate < distance) { nearest = cell; distance = candidate; }
        }
        return nearest;
    }

    private Column column(int x, int z) {
        double wx = x, wz = z;
        Cell cell = nearest(wx, wz);
        double islandScale = cell.scale();
        TerrainProfile profile = profiles.get(cell.biome());
        double spacing = 100.0 / profile.spacingPercent();
        double dx = (wx - cell.x()) * spacing, dz = (wz - cell.z()) * spacing;
        double u = dx * Math.cos(cell.angle()) + dz * Math.sin(cell.angle());
        double v = dz * Math.cos(cell.angle()) - dx * Math.sin(cell.angle());
        double radius = radius(cell, profile, spacing);
        double elevation = 151 + Math.floor(cell.variation() * 5) * 13 + profile.elevationOffset();
        double relief = profile.reliefPercent() / 100.0, shape = profile.shapePercent() / 100.0;
        double variation = cell.variation();
        if (cell.biome() == 3) {
            double pitch = Math.min(220 * MAX_ISLAND_SCALE, cell.clearance() * spacing / 2.4);
            int ix = Math.clamp((int) Math.floor(u / pitch + 0.5), -1, 1);
            int iz = Math.clamp((int) Math.floor(v / pitch + 0.5), -1, 1);
            long seed = mix(((long) ix << 32) ^ iz ^ Double.doubleToLongBits(cell.variation()));
            u -= ix * pitch + (unit(seed) - 0.5) * pitch * 0.08;
            v -= iz * pitch + (unit(mix(seed)) - 0.5) * pitch * 0.08;
            islandScale = islandScale(seed);
            radius = pitch * (0.23 + unit(mix(seed + 1)) * 0.07) * islandScale / MAX_ISLAND_SCALE;
            elevation = 126 + Math.floor(unit(mix(seed + 2)) * 8) * 15 + profile.elevationOffset();
            variation = unit(mix(seed + 3));
        }
        double noiseX = x * spacing / islandScale, noiseZ = z * spacing / islandScale;
        double rough = Math.clamp(detail.getValue(noiseX * 0.75, 0, noiseZ * 0.75), -1, 1);
        double broad = Math.clamp(detail.getValue(noiseX * 0.15, 0, noiseZ * 0.15), -1, 1);
        double worldU = u * Math.cos(cell.angle()) - v * Math.sin(cell.angle());
        double worldV = u * Math.sin(cell.angle()) + v * Math.cos(cell.angle());
        SkyIslandShape island = shape(Double.doubleToLongBits(variation), cell.angle());
        var local = island.localCoordinates(worldU, worldV);
        u = local.x();
        v = local.z();
        double radial = Math.hypot(u * 0.93, v * 1.07);
        double inset = island.inset(worldU / radius, worldV / radius) * radius;
        boolean land = inset > 0;
        double rimWidth = Math.min(radius * 0.3, Math.max(18 * spacing * islandScale, radius * 0.18));
        double hillA = 1 - smooth(Math.hypot(u / radius + 0.34, v / radius - 0.22), 0, 0.66);
        double hillB = 1 - smooth(Math.hypot(u / radius - 0.36, v / radius + 0.24), 0, 0.58);
        double hills = hillA * 7 + hillB * 5;
        double heightScale = Math.min(1, radius / spacing / islandScale / 80);
        double footprintRadius = radius;
        // Evaluate the surface in model space; round only after continuous vertical scaling.
        u /= islandScale;
        v /= islandScale;
        radius /= islandScale;
        radial /= islandScale;
        rimWidth /= islandScale;
        double top = elevation + (broad * 2.2 + rough * 0.45 + hills * heightScale) * relief;
        double rockOffset = 0;
        double lowestTop = elevation - 12 - 9 * relief - 3 * shape;
        int water = 0;
        boolean waterfall = false;
        double riverBlend = 0;
        double bend = riverBend(u, radius);
        double stream = Math.abs(v + bend);
        switch (cell.biome()) {
            case 0 -> top = elevation + (broad * 1.75 + rough * 0.20) * relief;
            case 1 -> {
                int head = (int) elevation;
                double channelWidth = 15 + broad * 3;
                double wetWidth = 6 + broad * 0.8;
                double entrance = smooth(u, -radius * 0.6, -radius * 0.35);
                if (entrance > 0 && stream < channelWidth + 6) {
                    riverBlend = entrance * (1 - smooth(stream, channelWidth, channelWidth + 6));
                    double bed = head - 5 * (1 - smooth(stream, 2.5, wetWidth))
                            + 2 * smooth(stream, wetWidth, channelWidth);
                    top += riverBlend * (bed - top);
                    if (stream < wetWidth) {
                        water = head;
                        waterfall = !land && inset / spacing > -1.25 && u > 0;
                    }
                }
            }
            case 2, 5 -> {
                double lakeRadius = radius * (cell.biome() == 2 ? 0.52 : 0.61);
                double lakeEdge = radial + broad * 16 + rough * 4;
                double bankWidth = Math.min(90 * spacing, (radius - lakeRadius) * 0.75);
                double bank = Math.max(smooth(lakeEdge, lakeRadius, lakeRadius + bankWidth),
                        1 - smooth(inset / islandScale, rimWidth, rimWidth * 2));
                double firstIsland = 1 - smooth(Math.hypot((u / radius + 0.18) / 0.11,
                        (v / radius - 0.10) / 0.13), 0.15, 1.2);
                double secondIsland = 1 - smooth(Math.hypot((u / radius - 0.23) / 0.09,
                        (v / radius + 0.18) / 0.11), 0.15, 1.2);
                double lakeIsland = Math.max(smooth(detail.getValue(noiseX * 0.24 + 91, 0,
                        noiseZ * 0.24 - 37), 0.05, 0.65) * 14,
                        Math.max(firstIsland, secondIsland) * 13.5) * (1 - bank);
                top = elevation - 10 + bank * (14 + 5 * shape + hills * heightScale * relief)
                        + lakeIsland + rough * 0.35;
                if (lakeEdge < lakeRadius + bankWidth) water = (int) elevation;
            }
            case 3 -> {
                if (radius > 32 && variation < 0.65) {
                    double pond = Math.hypot(u, v) + rough * 2;
                    double basin = 1 - smooth(pond, radius * 0.18, radius * 0.6);
                    top += basin * (elevation - 4 - top);
                    if (pond < radius * 0.6) water = (int) elevation;
                }
            }
            case 4 -> {
                double direction = variation * Math.PI * 2;
                double along = (u * Math.cos(direction) + v * Math.sin(direction)) / radius;
                double across = (v * Math.cos(direction) - u * Math.sin(direction)) / radius;
                double amplitude = (12 + 4 * shape) * heightScale;
                double grade = (along + Math.sin(across * 3.4 + direction) * 0.16 + broad * 0.08) * amplitude;
                double terrace = grade / 3.0;
                double level = Math.floor(terrace);
                top = elevation + ((level + smooth(terrace - level, 0.68, 1)) * 3 + rough * 0.25) * relief;
                // The rock follows the continuous slope, without carrying the terrace steps into the underside.
                rockOffset = grade * relief;
                lowestTop = elevation - (amplitude * 1.35 + 3) * relief;
            }
            default -> { }
        }
        int surface = (int) Math.ceil(scaledHeight(Math.clamp(top, 70, 295), islandScale));
        water = water == 0 ? 0 : (int) scaledHeight(Math.clamp(water, 0, 295), islandScale);
        if (!waterfall && surface >= water) water = 0;
        double soil = Math.clamp(detail.getValue(noiseX * 3.6 + 37, 0,
                noiseZ * 3.6 + 127), -1, 1);
        int dirtDepth = Math.clamp(4 + (int) Math.floor(broad * 2 + rough + soil * 4), 2, 10);
        double strata = Math.clamp(detail.getValue(noiseX * 4.2 + 149, 0,
                noiseZ * 4.2 - 73), -1, 1);
        int cloudDepth = Math.clamp(5 + (int) Math.floor(broad * 1.5 + strata * 4), 2, 11);
        double physicalRadius = footprintRadius / spacing;
        boolean smallIsland = island.diameter() * physicalRadius + 2 <= SMALL_ISLAND_MAX_DIAMETER;
        boolean smallIslandRim = smallIsland && inset / spacing < SMALL_ISLAND_RIM_WIDTH;
        // At the rim the four strata compress to one block each; their inland thickness cannot extrude a lid.
        int minimumSupport = smallIslandRim ? 2 : 4;
        double depthBudget = Math.floor(Math.clamp(lowestTop, 70, 295)) - 23 - MIN_ISLAND_BOTTOM;
        double verticalRadius = physicalRadius / islandScale;
        double depthScale = Math.min(shape, depthBudget / (verticalRadius * island.maxDepth()));
        double rockElevation = Math.clamp(elevation + rockOffset, 70, 295);
        double rockScale = verticalRadius * depthScale;
        double rockDepth = rockScale * island.rockDepth(worldU / footprintRadius, worldV / footprintRadius);
        int bottom = Math.min(surface - scaledDepth(minimumSupport, islandScale), (int) Math.floor(scaledHeight(rockElevation - rockDepth, islandScale)));
        int dirtBottom = Math.max(bottom + scaledDepth(2, islandScale), surface - 1 - scaledDepth(dirtDepth, islandScale));
        int cloudBottom = Math.max(bottom + scaledDepth(1, islandScale), dirtBottom - scaledDepth(cloudDepth, islandScale));
        int rockTop = 0, rockBottom = 0;
        double rockScaleFactor = 1.0;
        boolean rockRim = false;
        int rockPitch = scaledDepth(144, MAX_ISLAND_SCALE);
        int rx = Math.floorDiv(x, rockPitch), rz = Math.floorDiv(z, rockPitch);
        long rockSeed = mix(Double.doubleToLongBits(layout.getValue(rx * 31, 0, rz * 29))
                ^ rx * 0x9E3779B97F4A7C15L ^ rz * 0xC2B2AE3D27D4EB4FL);
        if (unit(rockSeed) < 0.13) {
            rockScaleFactor = islandScale(rockSeed);
            double rockRadius = (8 + unit(mix(rockSeed)) * 10) * rockScaleFactor;
            SkyIslandShape floatingRock = shape(rockSeed, 0);
            int rockCenterX = rx * rockPitch + rockPitch / 2, rockCenterZ = rz * rockPitch + rockPitch / 2;
            double rockX = (x - rockCenterX) / rockRadius;
            double rockZ = (z - rockCenterZ) / rockRadius;
            double rockInset = floatingRock.inset(rockX, rockZ) * rockRadius;
            if (rockInset > 0 && clearRockSite(rockCenterX, rockCenterZ, rockRadius)) {
                Cell rockCell = nearest(rockCenterX, rockCenterZ);
                double satelliteElevation = 151 + Math.floor(rockCell.variation() * 5) * 13
                        + profiles.get(rockCell.biome()).elevationOffset();
                double rockElevationBase = Math.min(306, satelliteElevation + 38 + unit(mix(rockSeed + 1)) * 38);
                rockTop = (int) Math.ceil(scaledHeight(rockElevationBase, rockScaleFactor));
                rockRim = floatingRock.diameter() * rockRadius + 2 <= SMALL_ISLAND_MAX_DIAMETER
                        && rockInset < SMALL_ISLAND_RIM_WIDTH;
                rockBottom = Math.min(rockTop - scaledDepth(rockRim ? 2 : 4, rockScaleFactor),
                        (int) Math.floor(scaledHeight(rockElevationBase, rockScaleFactor)
                                - rockRadius * 1.8 * floatingRock.rockDepth(rockX, rockZ)));
            }
        }
        List<SolidSpan> spans = land ? List.of(new SolidSpan(bottom, surface)) : List.of();
        return new Column(cell.biome(), surface, bottom, water, land,
                rockTop, rockBottom, waterfall, dirtBottom, cloudBottom, smallIslandRim, rockRim, spans,
                rockTop > 0 ? rockScaleFactor : islandScale);
    }

    private boolean clearRockSite(int x, int z, double radius) {
        int gx = (int) Math.floor(x / CELL_SIZE), gz = (int) Math.floor(z / CELL_SIZE);
        for (int dz = -2; dz <= 2; dz++) for (int dx = -2; dx <= 2; dx++) {
            Cell cell = cell(gx + dx, gz + dz);
            if (Math.hypot(x - cell.x(), z - cell.z()) < cell.clearance() + radius + 2) return false;
        }
        return true;
    }

    private static double radius(Cell cell, TerrainProfile profile, double spacing) {
        double limit = cell.clearance() * spacing * 0.95 * cell.scale() / MAX_ISLAND_SCALE;
        double elevation = Math.clamp(151 + Math.floor(cell.variation() * 5) * 13
                + profile.elevationOffset(), 70, 295);
        double heightLimit = (elevation - MIN_ISLAND_BOTTOM) * spacing / 1.4 * cell.scale();
        return Math.min((292 + cell.variation() * 80) * cell.scale() * Math.sqrt(profile.biomeSizePercent() / 100.0),
                Math.min(limit, heightLimit));
    }

    public Optional<BlockPos> findReferenceCenter(String id, BlockPos near) {
        int target = BIOMES.indexOf(id);
        int gx = (int) Math.floor(near.getX() / CELL_SIZE), gz = (int) Math.floor(near.getZ() / CELL_SIZE);
        Cell nearest = null;
        double distance = Double.POSITIVE_INFINITY;
        for (int dz = -8; dz <= 8; dz++) for (int dx = -8; dx <= 8; dx++) {
            Cell cell = cell(gx + dx, gz + dz);
            double d = Math.hypot(cell.x() - near.getX(), cell.z() - near.getZ());
            if (cell.biome() == target && d < distance) { nearest = cell; distance = d; }
        }
        if (nearest == null) return Optional.empty();
        double wx = nearest.x(), wz = nearest.z();
        if (target == 1) {
            TerrainProfile profile = profiles.get(target);
            double spacing = 100.0 / profile.spacingPercent();
            double radius = radius(nearest, profile, spacing);
            double u = radius * 0.7;
            double v = -riverBend(u / nearest.scale(), radius / nearest.scale()) * nearest.scale();
            var offset = shape(Double.doubleToLongBits(nearest.variation()), nearest.angle()).worldCoordinates(u, v);
            double scale = profile.spacingPercent() / 100.0;
            wx += offset.x() * scale;
            wz += offset.z() * scale;
        }
        var preferred = landingPosition(wx, wz, target);
        if (preferred.isPresent()) return preferred;
        double step = radius(nearest, profiles.get(target), 100.0 / profiles.get(target).spacingPercent())
                * profiles.get(target).spacingPercent() / 400.0;
        for (int dz = -2; dz <= 2; dz++) for (int dx = -2; dx <= 2; dx++) {
            var candidate = landingPosition(nearest.x() + dx * step, nearest.z() + dz * step, target);
            if (candidate.isPresent()) return candidate;
        }
        return Optional.empty();
    }

    private Optional<BlockPos> landingPosition(double x, double z, int target) {
        int bx = (int) Math.round(x), bz = (int) Math.round(z);
        Column column = sample(bx, bz);
        return column.land() && column.biome() == target
                ? Optional.of(new BlockPos(bx, Math.max(column.top(), column.water()) + 8, bz)) : Optional.empty();
    }

    public static SkyLandformDensity from(net.minecraft.world.level.levelgen.RandomState state) {
        SkyLandformDensity[] result = new SkyLandformDensity[1];
        state.router().preliminarySurfaceLevel().mapAll(value -> {
            if (value instanceof SkyLandformDensity sky) result[0] = sky;
            return value;
        });
        if (result[0] == null) throw new IllegalStateException("Sky landform density missing");
        return result[0];
    }

    private static double smooth(double value, double low, double high) {
        double t = Math.clamp((value - low) / (high - low), 0.0, 1.0);
        return t * t * (3 - 2 * t);
    }
    private static double riverBend(double u, double radius) {
        return (Math.sin(u / 91) * 14 + Math.sin(u / 37) * 5) * Math.min(1, radius / 150);
    }
    private static double unit(long value) { return (value >>> 11) * 0x1.0p-53; }
    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
    @Override public DensityFunction mapChildren(Visitor visitor) {
        NoiseHolder mappedLayout = visitor.visitNoise(layout), mappedDetail = visitor.visitNoise(detail);
        return mappedLayout == layout && mappedDetail == detail ? this
                : new SkyLandformDensity(mappedLayout, mappedDetail, profiles, mode, maxIslandScale);
    }
    @Override public double minValue() { return mode == REGION ? -0.9 : mode == SURFACE ? -62 : -100; }
    @Override public double maxValue() { return mode == REGION ? 0.9 : mode == SURFACE ? scaledHeight(306, maxIslandScale) + 1 : 100; }
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() { return CODEC; }
}
