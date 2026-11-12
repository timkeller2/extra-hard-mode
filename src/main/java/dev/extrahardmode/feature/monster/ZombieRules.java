package dev.extrahardmode.feature.monster;

/**
 * Per-zombie speed/damage variance. Minecraft-free so JUnit can cover the mapping.
 * Speed rolls uniformly in {@code [−VARIANCE, +VARIANCE]}; damage % is the negation.
 */
public final class ZombieRules {
    public static final double VARIANCE = 0.20;
    /** Current health when a zombie rises from its head. Max health stays the same. */
    public static final double HEAD_HEALTH_FRACTION = 0.5;
    /** Extra attack damage for that risen zombie, applied after the speed roll. */
    public static final double HEAD_DAMAGE_BONUS = 0.30;
    /** Extra movement speed for that risen zombie, applied after the speed roll. */
    public static final double HEAD_SPEED_BONUS = 0.20;
    /** Hand height, side offset, and forward offset, each as a fraction of body height. */
    public static final double HAND_HEIGHT = 0.42;
    public static final double HAND_SIDE = 0.18;
    public static final double HAND_REACH = 0.06;

    private ZombieRules() {}

    /** Half of max health. Zero when max health is not positive. */
    public static float headHealth(float maxHealth) {
        if (maxHealth <= 0.0F) {
            return 0.0F;
        }
        return maxHealth * (float) HEAD_HEALTH_FRACTION;
    }

    /** Offsets from the feet for the left hand, then the right. Yaw 0 faces south. */
    public record Hand(double x, double y, double z) {}

    public static Hand[] hands(float yawDegrees, double height) {
        double body = Math.max(0.0, height);
        double yaw = Math.toRadians(yawDegrees);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double leftX = Math.cos(yaw);
        double leftZ = Math.sin(yaw);
        double y = body * HAND_HEIGHT;
        double side = body * HAND_SIDE;
        double reach = body * HAND_REACH;
        return new Hand[] {
            new Hand(leftX * side + forwardX * reach, y, leftZ * side + forwardZ * reach),
            new Hand(-leftX * side + forwardX * reach, y, -leftZ * side + forwardZ * reach)
        };
    }

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
