package dev.extrahardmode.feature;

/**
 * Milk cooldown and the extra meal that follows. Minecraft-free so JUnit can
 * cover the timing. One Minecraft day is 24000 ticks. The extra meal is a few
 * seconds later and is not the animal's usual bite interval.
 */
public final class CowMilkRules {
    /** Ticks after a milking before the same cow can be milked again. */
    public static final long COOLDOWN_TICKS = 24000L;
    /** Soonest extra meal, two seconds after the milking. */
    public static final int HUNGER_MIN_TICKS = 40;
    /** Width of the extra-meal window. Delays land on 40 through 99 ticks. */
    public static final int HUNGER_SPAN_TICKS = 60;
    /**
     * A meal this late was missed while the cow was unloaded. Drop it. A loaded
     * cow is checked every tick, so a live meal is not this late.
     */
    public static final int HUNGER_STALE_TICKS = 100;

    private CowMilkRules() {}

    /** True while {@code readyAt} is still in the future. Zero means never milked. */
    public static boolean onCooldown(long now, long readyAt) {
        return readyAt > now;
    }

    public static long nextReady(long now) {
        return now + COOLDOWN_TICKS;
    }

    /**
     * Absolute game time of the extra meal. {@code roll} may be any int; the
     * delay stays inside the two-to-five-second window.
     */
    public static long hungerAt(long now, int roll) {
        return now + HUNGER_MIN_TICKS + Math.floorMod(roll, HUNGER_SPAN_TICKS);
    }

    /** True when a pending meal should be attempted. Zero means none pending. */
    public static boolean hungerDue(long now, long hungerAt) {
        return hungerAt > 0L && now >= hungerAt;
    }

    public static boolean hungerStale(long now, long hungerAt) {
        return hungerDue(now, hungerAt) && now - hungerAt >= HUNGER_STALE_TICKS;
    }

    /** Ticks until the cow can be milked. Zero when it is ready, including a ready time of zero. */
    public static int remainingTicks(long now, long readyAt) {
        if (!onCooldown(now, readyAt)) {
            return 0;
        }
        long left = readyAt - now;
        if (left > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) left;
    }

    /**
     * Remaining time rounded up to the next second, such as {@code 20m}, {@code 19m 59s}, or {@code 45s}.
     * One Minecraft day is 20 minutes.
     */
    public static String remainingLabel(int remainingTicks) {
        int seconds = Math.max(1, (int) Math.ceil(Math.max(0, remainingTicks) / 20.0));
        int minutes = seconds / 60;
        int rest = seconds % 60;
        if (minutes <= 0) {
            return rest + "s";
        }
        if (rest == 0) {
            return minutes + "m";
        }
        return minutes + "m " + rest + "s";
    }
}
