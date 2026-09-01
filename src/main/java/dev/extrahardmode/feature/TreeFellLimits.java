package dev.extrahardmode.feature;

/**
 * BFS bounds for realistic chopping. Minecraft-free so JUnit can cover the pillar false-positive
 * envelope without a running server.
 */
public final class TreeFellLimits {
    public static final int MAX_LOGS = 64;
    public static final int MAX_UP = 30;
    public static final int MAX_DOWN = 2;
    public static final int MAX_CHEBYSHEV_XZ = 2;
    public static final int MIN_LOGS = 2;
    public static final int MIN_ADJACENT_LEAVES = 4;

    private TreeFellLimits() {}

    public static boolean inBounds(int originX, int originY, int originZ, int x, int y, int z) {
        int dy = y - originY;
        if (dy < -MAX_DOWN || dy > MAX_UP) {
            return false;
        }
        int chebyshev = Math.max(Math.abs(x - originX), Math.abs(z - originZ));
        return chebyshev <= MAX_CHEBYSHEV_XZ;
    }
}
