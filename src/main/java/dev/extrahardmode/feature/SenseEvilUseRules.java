package dev.extrahardmode.feature;

/**
 * Click versus eat for a spider eye, and the outline time shown on the duration bar.
 * Minecraft-free so JUnit can cover the split. Vanilla food use lasts 32 ticks.
 */
public final class SenseEvilUseRules {
    /** A release at or under this many ticks is a click. Past this, and short of a finished eat, neither happens. */
    public static final int CLICK_TICKS = 10;

    private SenseEvilUseRules() {}

    /** True for a press that never became an eat, including a same-tick click of 0 ticks. */
    public static boolean click(int heldTicks) {
        return heldTicks >= 0 && heldTicks <= CLICK_TICKS;
    }

    /**
     * A player who cannot eat the eye casts on the press. A player who can eat waits for the release,
     * so a held click can finish as food.
     */
    public static boolean castOnPress(boolean canEat) {
        return !canEat;
    }

    /** A short release casts only when the eye was being eaten. A finished eat never reaches this path. */
    public static boolean castOnRelease(boolean canEat, int heldTicks) {
        return canEat && click(heldTicks);
    }

    /** Ticks left on the outline. Zero when {@code until} is not strictly after {@code now}. */
    public static int remainingTicks(long until, long now) {
        if (!WiseLessonRules.senseOutlineActive(until, now)) {
            return 0;
        }
        long left = until - now;
        if (left > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) left;
    }

    /** Bar length for this cast. The stored duration stays full width at the start and does not shrink the track. */
    public static int barMax(int remaining, int durationTicks) {
        return Math.max(Math.max(0, remaining), Math.max(0, durationTicks));
    }
}
