package dev.extrahardmode.feature;

/**
 * Villager trade restock timers. Minecraft-free so JUnit can cover the 8× delay.
 */
public final class VillagerRestockRules {
    public static final int MULTIPLIER = 8;
    /** Minimum ticks between the two restocks in a restock period. Vanilla is 2400 (2 minutes). */
    public static final long VANILLA_BETWEEN_RESTOCKS_TICKS = 2400L;
    /** Ticks after a restock before catch-up treats the villager as overdue. Vanilla is 12000. */
    public static final long VANILLA_CATCH_UP_TICKS = 12000L;

    private VillagerRestockRules() {}

    public static long scaleTicks(long vanillaTicks) {
        long ticks = Math.max(0L, vanillaTicks);
        long scaled = ticks * (long) MULTIPLIER;
        return scaled < 0L ? Long.MAX_VALUE : scaled;
    }

    public static long betweenRestocksTicks() {
        return scaleTicks(VANILLA_BETWEEN_RESTOCKS_TICKS);
    }

    public static long catchUpTicks() {
        return scaleTicks(VANILLA_CATCH_UP_TICKS);
    }

    /** Vanilla resets restock count on a new day; Extra Hard Mode waits this many days. */
    public static boolean restockPeriodElapsed(long currentDay, long lastCheckDay) {
        if (lastCheckDay <= 0L) {
            return false;
        }
        return currentDay >= lastCheckDay + MULTIPLIER;
    }
}
