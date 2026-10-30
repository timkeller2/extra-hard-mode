package dev.extrahardmode.feature;

import dev.extrahardmode.module.EntityHelper;

/**
 * How often grass spreads onto dirt. Minecraft-free so JUnit can cover the roll.
 *
 * <p>100 is the usual rate. 0 never spreads. The default is one quarter.
 */
public final class GrassSpreadRules {
    public static final int DEFAULT_PERCENT = 25;

    public static final String CONFIG_COMMENT =
            "Grass Growth Rate: 25% of the usual spread of grass onto a dirt block. 100 is the usual rate. 0 never spreads.";

    private GrassSpreadRules() {}

    /** {@code roll} is 0–99 from {@code nextInt(100)}. */
    public static boolean allowSpread(int percent, int roll) {
        return EntityHelper.percent(roll, percent);
    }

    /**
     * A fence, gate, door, wall, or similar block leaves the grass open.
     * Air stays on the usual light check. A block that covers the whole face
     * against the grass, or a full fluid, still blocks it.
     */
    public static boolean openCover(boolean air, boolean sealsGrass, boolean fullFluid) {
        return !air && !sealsGrass && !fullFluid;
    }

    /** Brightest of the cover and the air around it. The usual check uses only {@code above}. */
    public static int spreadLight(int above, int higher, int north, int east, int south, int west) {
        return Math.max(above, Math.max(higher, Math.max(Math.max(north, east), Math.max(south, west))));
    }
}
