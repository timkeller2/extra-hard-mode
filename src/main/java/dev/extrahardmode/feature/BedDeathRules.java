package dev.extrahardmode.feature;

/**
 * Clear hostiles around a bed when the player dies nearby. Minecraft-free for JUnit.
 */
public final class BedDeathRules {
    public static final int RANGE = 6;
    /** Horizontal offsets for the other half of a two-block bed. */
    private static final int[][] BED_HALVES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private BedDeathRules() {}

    public static boolean withinRange(int x1, int y1, int z1, int x2, int y2, int z2) {
        return withinRange(x1, y1, z1, x2, y2, z2, RANGE);
    }

    public static boolean withinRange(int x1, int y1, int z1, int x2, int y2, int z2, int range) {
        int reach = Math.max(0, range);
        return Math.max(Math.abs(x1 - x2), Math.max(Math.abs(y1 - y2), Math.abs(z1 - z2))) <= reach;
    }

    /** True if {@code (x,y,z)} is within range of either half of a bed at {@code bed*}. */
    public static boolean withinRangeOfBed(int x, int y, int z, int bedX, int bedY, int bedZ) {
        if (withinRange(x, y, z, bedX, bedY, bedZ)) {
            return true;
        }
        for (int[] half : BED_HALVES) {
            if (withinRange(x, y, z, bedX + half[0], bedY, bedZ + half[1])) {
                return true;
            }
        }
        return false;
    }
}
