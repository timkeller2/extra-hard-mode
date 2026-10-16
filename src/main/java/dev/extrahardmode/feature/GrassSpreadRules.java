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
}
