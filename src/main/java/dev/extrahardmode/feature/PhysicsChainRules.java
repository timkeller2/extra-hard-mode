package dev.extrahardmode.feature;

/**
 * How far a cave-in or loose-block collapse may travel from the block that started it.
 * Minecraft-free so the range can be unit-tested.
 */
public final class PhysicsChainRules {
    /** Straight-line blocks. The block at this distance still falls; the next one does not. */
    public static final int MAX_RANGE = 50;
    /** Ticks a block waits after the one that exposed it before it starts falling. */
    public static final int HOP_DELAY_TICKS = 4;

    private PhysicsChainRules() {}

    public static boolean withinRange(int originX, int originY, int originZ, int x, int y, int z) {
        long dx = (long) x - originX;
        long dy = (long) y - originY;
        long dz = (long) z - originZ;
        long limit = (long) MAX_RANGE * MAX_RANGE;
        return dx * dx + dy * dy + dz * dz <= limit;
    }
}
