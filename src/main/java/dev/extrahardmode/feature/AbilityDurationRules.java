package dev.extrahardmode.feature;

/**
 * Duration HUD warn/fade/blink. Minecraft-free for JUnit.
 */
public final class AbilityDurationRules {
    public static final int WARN_TICKS = AbilityRules.DURATION_WARN_TICKS;
    /** Half-period of the blink, in ticks (visible 4, hidden 4). */
    public static final int BLINK_HALF_TICKS = 4;

    private AbilityDurationRules() {}

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
