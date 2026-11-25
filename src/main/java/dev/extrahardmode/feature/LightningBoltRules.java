package dev.extrahardmode.feature;

/**
 * Points of a lightning bolt from the caster to one target. Minecraft-free so the
 * path can be checked without a world. Both ends stay on the two positions.
 */
public final class LightningBoltRules {
    /** Blocks between samples along the bolt. */
    public static final double STEP = 0.75;
    /** Longest bolt, in samples. Farther strikes stretch this spacing. */
    public static final int MAX_SEGMENTS = 40;
    /** How far a midpoint may leave the straight line, in blocks. Ends stay put. */
    public static final double JAG = 0.45;

    private LightningBoltRules() {}

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
     * last point is the end. {@code salt} picks the jag so two bolts to one target differ.
     */
    public static double[][] points(
            double x0, double y0, double z0, double x1, double y1, double z1, int salt) {
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
        double inv = 1.0 / distance;
        double ux = dx * inv;
        double uy = dy * inv;
        double uz = dz * inv;
        double ax = Math.abs(uy) < 0.9 ? 0.0 : 1.0;
        double ay = Math.abs(uy) < 0.9 ? 1.0 : 0.0;
        double az = 0.0;
        double px = uy * az - uz * ay;
        double py = uz * ax - ux * az;
        double pz = ux * ay - uy * ax;
        double pLen = Math.sqrt(px * px + py * py + pz * pz);
        if (pLen < 1.0e-6) {
            px = 1.0;
            py = 0.0;
            pz = 0.0;
            pLen = 1.0;
        }
        px /= pLen;
        py /= pLen;
        pz /= pLen;
        double qx = uy * pz - uz * py;
        double qy = uz * px - ux * pz;
        double qz = ux * py - uy * px;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / (double) steps;
            if (i == 0 || i == steps) {
                out[i][0] = x0 + dx * t;
                out[i][1] = y0 + dy * t;
                out[i][2] = z0 + dz * t;
                continue;
            }
            double envelope = Math.sin(Math.PI * t);
            double jag = envelope * JAG * unit(i, salt);
            double jag2 = envelope * JAG * unit(i, salt + 17);
            out[i][0] = x0 + dx * t + px * jag + qx * jag2;
            out[i][1] = y0 + dy * t + py * jag + qy * jag2;
            out[i][2] = z0 + dz * t + pz * jag + qz * jag2;
        }
        return out;
    }

    /** Distance from point {@code p} to the segment from {@code a} to {@code b}. */
    public static double distanceToSegment(
            double px, double py, double pz, double ax, double ay, double az, double bx, double by, double bz) {
        double dx = bx - ax;
        double dy = by - ay;
        double dz = bz - az;
        double len2 = dx * dx + dy * dy + dz * dz;
        if (len2 < 1.0e-8) {
            double ox = px - ax;
            double oy = py - ay;
            double oz = pz - az;
            return Math.sqrt(ox * ox + oy * oy + oz * oz);
        }
        double t = ((px - ax) * dx + (py - ay) * dy + (pz - az) * dz) / len2;
        if (t < 0.0) {
            t = 0.0;
        } else if (t > 1.0) {
            t = 1.0;
        }
        double ox = px - (ax + dx * t);
        double oy = py - (ay + dy * t);
        double oz = pz - (az + dz * t);
        return Math.sqrt(ox * ox + oy * oy + oz * oz);
    }

    private static double unit(int index, int salt) {
        int n = index * 374761393 + salt * 668265263;
        n = (n ^ (n >>> 13)) * 1274126177;
        n ^= n >>> 16;
        return ((n & 0xFFFF) / 32768.0) - 1.0;
    }
}
