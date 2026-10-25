package dev.extrahardmode.feature;

/**
 * Floating count of actions left until the next builder or slayer tier.
 * Minecraft-free for JUnit.
 */
public final class AchievementFloatRules {
    public static final int DURATION_MILLIS = 3000;
    /** How far the number travels left and down, in blocks, over the full duration. */
    public static final float DRIFT_BLOCKS = 0.55f;
    /** Half of a vanilla nameplate so the count stays small. */
    public static final float TEXT_SCALE = 0.0125f;
    /** Opaque golden brown. */
    public static final int COLOR = 0xFFC68642;

    private AchievementFloatRules() {}

    /** How far in front of the eyes the sneak preview floats, in blocks. */
    public static final double SNEAK_AHEAD_BLOCKS = 1.5;

    /**
     * Builder counts appear when sneak starts while a placeable block is held.
     * Holding sneak does not repeat it. Slayer counts are not gated here.
     */
    public static boolean showPlacementCount(boolean wasSneaking, boolean sneaking, boolean holdingPlaceable) {
        return holdingPlaceable && sneaking && !wasSneaking;
    }

    /** Actions still needed. Zero when this track has no further tier. */
    public static int remaining(int nextTarget, int count) {
        if (nextTarget <= 0 || count < 0 || count >= nextTarget) {
            return 0;
        }
        return nextTarget - count;
    }

    public static boolean alive(long elapsedMillis) {
        return elapsedMillis >= 0 && elapsedMillis < DURATION_MILLIS;
    }

    /** Screen-left and screen-down offset in blocks. Zero before the float starts. */
    public static float driftBlocks(long elapsedMillis) {
        if (elapsedMillis <= 0) {
            return 0f;
        }
        float progress = Math.min(1f, elapsedMillis / (float) DURATION_MILLIS);
        return DRIFT_BLOCKS * progress;
    }
}
