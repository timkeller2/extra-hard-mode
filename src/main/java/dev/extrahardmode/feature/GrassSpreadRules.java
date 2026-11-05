package dev.extrahardmode.feature;

import dev.extrahardmode.module.EntityHelper;

/**
 * How often grass spreads onto dirt. Minecraft-free so JUnit can cover the roll.
 *
 * <p>{@code -1} follows the current crop loss. {@code 0} never spreads. A positive
 * number is that static percent of the usual spread.
 */
public final class GrassSpreadRules {
    /** Config value that computes the spread from the current crop loss rate. */
    public static final int FOLLOW_LOSS = -1;
    public static final int DEFAULT_PERCENT = FOLLOW_LOSS;
    /** Spread percent at zero crop loss when the config follows that loss. */
    public static final int FOLLOW_BASE = 40;

    public static final String CONFIG_COMMENT =
            "Percent of the usual grass spread onto dirt. -1, the default, is 40 minus half the current crop loss rate, and never below 0. 0 never spreads. A positive number is that static percent. 100 is the usual rate.";

    private GrassSpreadRules() {}

    /**
     * Stored config. {@code -1} follows crop loss. Any other value at or below 0
     * disables spread. A positive value is capped at 100.
     */
    public static int configuredPercent(int raw) {
        if (raw == FOLLOW_LOSS) {
            return FOLLOW_LOSS;
        }
        if (raw <= 0) {
            return 0;
        }
        return Math.min(100, raw);
    }

    /**
     * Percent passed to {@link #allowSpread}. {@code -1} is {@link #FOLLOW_BASE}
     * minus half of {@code currentLossRate}, rounded, and never below 0.
     */
    public static int effectivePercent(int configured, double currentLossRate) {
        int percent = configuredPercent(configured);
        if (percent != FOLLOW_LOSS) {
            return percent;
        }
        long spread = Math.round(FOLLOW_BASE - 0.5 * Math.max(0.0, currentLossRate));
        if (spread <= 0L) {
            return 0;
        }
        return spread >= 100L ? 100 : (int) spread;
    }

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
