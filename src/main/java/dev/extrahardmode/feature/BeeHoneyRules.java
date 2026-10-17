package dev.extrahardmode.feature;

import dev.extrahardmode.module.EntityHelper;

/**
 * How much honey a returning bee adds. Minecraft-free so JUnit can cover the roll.
 *
 * <p>100 is the usual rate. 0 never fills. The default is one quarter. A rare double
 * fill stays inside that same roll.
 */
public final class BeeHoneyRules {
    public static final int DEFAULT_PERCENT = 25;

    public static final String CONFIG_COMMENT =
            "Honey a bee adds when it returns to a hive or nest. 100 is the usual rate. 25 is a quarter. 0 never fills. Shearing amount is hiveHoneycombCount.";

    private BeeHoneyRules() {}

    /**
     * Honey levels to add for this delivery. {@code vanillaIncrement} is the usual
     * gain. {@code roll} is 0–99 from {@code nextInt(100)}.
     */
    public static int honeyIncrement(int vanillaIncrement, int percent, int roll) {
        if (vanillaIncrement <= 0) {
            return 0;
        }
        return EntityHelper.percent(roll, percent) ? vanillaIncrement : 0;
    }
}
