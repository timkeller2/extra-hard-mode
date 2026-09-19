package dev.extrahardmode.feature;

/**
 * Duration HUD warn/fade/blink. Minecraft-free for JUnit.
 */
public final class AbilityDurationRules {
    public static final int WARN_TICKS = AbilityRules.DURATION_WARN_TICKS;
    /** Half-period of the blink, in ticks (visible 4, hidden 4). */
    public static final int BLINK_HALF_TICKS = 4;
    /** Item icon size; duration bars match this width and sit under the icon. */
    public static final int ICON_SIZE = 16;
    public static final int BAR_WIDTH = ICON_SIZE;
    public static final int BAR_HEIGHT = 3;
    public static final int BAR_OUTLINE = 1;
    public static final int ICON_BAR_GAP = 1;
    public static final int COLUMN_GAP = 4;
    public static final int LEFT_MARGIN = 8;
    public static final int BOTTOM_MARGIN = 8;

    private AbilityDurationRules() {}

    public static int columnHeight() {
        return ICON_SIZE + ICON_BAR_GAP + BAR_HEIGHT + 2 * BAR_OUTLINE;
    }

    public static int originX() {
        return LEFT_MARGIN;
    }

    public static int originY(int guiHeight) {
        return guiHeight - BOTTOM_MARGIN - columnHeight();
    }

    public static int iconX(int originX, int index) {
        return originX + index * (BAR_WIDTH + COLUMN_GAP);
    }

    public static int barY(int originY) {
        return originY + ICON_SIZE + ICON_BAR_GAP;
    }

    public static int innerBarWidth() {
        return Math.max(1, BAR_WIDTH - 2 * BAR_OUTLINE);
    }

    public static int filledWidth(int remainingTicks, int maxTicks) {
        if (remainingTicks <= 0 || maxTicks <= 0) {
            return 0;
        }
        int inner = innerBarWidth();
        int filled = Math.max(1, Math.round(inner * (remainingTicks / (float) maxTicks)));
        return Math.min(inner, filled);
    }

    public static boolean warn(int remainingTicks, boolean autoContinue) {
        return remainingTicks > 0 && remainingTicks <= WARN_TICKS && !autoContinue;
    }

    /**
     * 1 while safe, down to 0.25 in the last 15 seconds when it cannot auto-continue.
     */
    public static float alpha(int remainingTicks, boolean autoContinue) {
        if (!warn(remainingTicks, autoContinue)) {
            return 1.0F;
        }
        float t = remainingTicks / (float) WARN_TICKS;
        return 0.25F + 0.75F * Math.max(0.0F, Math.min(1.0F, t));
    }

    public static boolean visible(int remainingTicks, boolean autoContinue, long gameTime) {
        if (remainingTicks <= 0) {
            return false;
        }
        if (!warn(remainingTicks, autoContinue)) {
            return true;
        }
        return Math.floorMod(gameTime / BLINK_HALF_TICKS, 2L) == 0L;
    }

    public static int argb(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        return (a << 24) | (rgb & 0x00FFFFFF);
    }
}
