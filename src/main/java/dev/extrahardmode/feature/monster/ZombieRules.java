package dev.extrahardmode.feature.monster;

/**
 * Per-zombie speed/damage variance. Minecraft-free so JUnit can cover the mapping.
 * Speed rolls uniformly in {@code [−VARIANCE, +VARIANCE]}; damage % is the negation.
 */
public final class ZombieRules {
    public static final double VARIANCE = 0.20;

    private ZombieRules() {}

    /**
     * Maps a unit roll in {@code [0, 1]} onto {@code [−VARIANCE, +VARIANCE]}.
     * Values outside that range are clamped.
     */
    public static double speedDelta(double unit) {
        double u = unit;
        if (u < 0.0) {
            u = 0.0;
        } else if (u > 1.0) {
            u = 1.0;
        }
        return (u * 2.0 - 1.0) * VARIANCE;
    }

    /** Faster zombies hit softer; slower zombies hit harder, by the same percent. */
    public static double damageDelta(double speedDelta) {
        return -speedDelta;
    }

    /** Faster zombies are smaller; slower zombies are larger, by the same percent. */
    public static double scaleDelta(double speedDelta) {
        return -speedDelta;
    }
}
