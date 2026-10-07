package com.kltyton.eden_realm.world.terrain;

/** A lake or valley connection, sampled in the same warped coordinates as its terrain. */
final class MainlandRiverPath {
    record Hit(double distance, double halfWidth, int biome) { }
    private static final int SEGMENTS = 32;
    private final double[] xs = new double[SEGMENTS + 1], zs = new double[SEGMENTS + 1];
    private final double widthScale, variation, minX, maxX, minZ, maxZ;
    private final int biome;

    MainlandRiverPath(double fromX, double fromZ, double fromCosine, double fromSine,
                      double toX, double toZ, double toCosine, double toSine,
                      int biome, double variation, double widthScale) {
        this.biome = biome;
        this.variation = variation;
        this.widthScale = widthScale;
        double dx = toX - fromX, dz = toZ - fromZ, length = Math.hypot(dx, dz);
        double startSign = dx * fromCosine + dz * fromSine >= 0 ? 1 : -1;
        double endSign = dx * toCosine + dz * toSine >= 0 ? 1 : -1;
        double firstX = fromX + fromCosine * startSign * length * 0.36;
        double firstZ = fromZ + fromSine * startSign * length * 0.36;
        double secondX = toX - toCosine * endSign * length * 0.30;
        double secondZ = toZ - toSine * endSign * length * 0.30;
        double lowX = Double.POSITIVE_INFINITY, highX = Double.NEGATIVE_INFINITY;
        double lowZ = Double.POSITIVE_INFINITY, highZ = Double.NEGATIVE_INFINITY;
        for (int i = 0; i <= SEGMENTS; i++) {
            double t = (double) i / SEGMENTS, s = 1 - t;
            xs[i] = s * s * s * fromX + 3 * s * s * t * firstX + 3 * s * t * t * secondX + t * t * t * toX;
            zs[i] = s * s * s * fromZ + 3 * s * s * t * firstZ + 3 * s * t * t * secondZ + t * t * t * toZ;
            lowX = Math.min(lowX, xs[i]); highX = Math.max(highX, xs[i]);
            lowZ = Math.min(lowZ, zs[i]); highZ = Math.max(highZ, zs[i]);
        }
        double padding = 28 * widthScale + 24;
        minX = lowX - padding; maxX = highX + padding;
        minZ = lowZ - padding; maxZ = highZ + padding;
    }

    Hit at(double x, double z) {
        if (x < minX || x > maxX || z < minZ || z > maxZ) return null;
        double best = Double.POSITIVE_INFINITY, progress = 0;
        for (int i = 0; i < SEGMENTS; i++) {
            double dx = xs[i + 1] - xs[i], dz = zs[i + 1] - zs[i];
            double t = Math.clamp(((x - xs[i]) * dx + (z - zs[i]) * dz) / (dx * dx + dz * dz), 0, 1);
            double px = x - xs[i] - t * dx, pz = z - zs[i] - t * dz;
            double distance = px * px + pz * pz;
            if (distance < best) { best = distance; progress = (i + t) / SEGMENTS; }
        }
        double phase = variation * Math.PI * 2;
        double width = 12 * widthScale * (0.75 + progress * 0.45
                + Math.sin(progress * Math.PI * 2 + phase) * 0.13
                + Math.sin(progress * Math.PI * 6 - phase) * 0.05);
        return new Hit(Math.sqrt(best), Math.max(6, width), biome);
    }
}
