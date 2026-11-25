package dev.extrahardmode.feature;

/**
 * Points of a straight gold beam from the caster to one target. Minecraft-free so
 * the path can be checked without a world. Both ends stay on the two positions.
 */
public final class GoldBeamRules {
    /** Blocks between samples along the beam. */
    public static final double STEP = 0.5;
    /** Longest beam, in samples. Farther strikes stretch this spacing. */
    public static final int MAX_SEGMENTS = 48;
    /** Dust color, RGB. Same gold as a learned ability level. */
    public static final int COLOR = 0xFFAA00;
    /** Dust scale. */
    public static final float SCALE = 1.25F;

    private GoldBeamRules() {}

    /** Samples between the ends. Zero when the two positions are the same point. */
    public static int segments(double distance) {
        if (!(distance > 1.0e-6)) {
            return 0;
        }
        int steps = (int) Math.ceil(distance / STEP);
        if (steps < 1) {
            steps = 1;
        }
        return Math.min(steps, MAX_SEGMENTS);
    }

    /**
     * Inclusive points from the start to the end. The first point is the start and the
     * last point is the end. Every point lies on the segment.
     */
    public static double[][] points(double x0, double y0, double z0, double x1, double y1, double z1) {
        double dx = x1 - x0;
        double dy = y1 - y0;
        double dz = z1 - z0;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int steps = segments(distance);
        double[][] out = new double[steps + 1][3];
        if (steps == 0) {
            out[0][0] = x0;
            out[0][1] = y0;
            out[0][2] = z0;
            return out;
        }
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / (double) steps;
            out[i][0] = x0 + dx * t;
            out[i][1] = y0 + dy * t;
            out[i][2] = z0 + dz * t;
        }
        return out;
    }
}
