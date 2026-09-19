package dev.extrahardmode.feature;

/**
 * Burn lifetime for newly placed torches. Minecraft-free so JUnit can cover the math.
 * Missing place time (legacy / worldgen) is always permanent.
 */
public final class TorchLifetimeRules {
    public static final int DEFAULT_DAYS = 7;
    public static final int MAX_DAYS = 3650;
    public static final long TICKS_PER_DAY = 24000L;
    public static final int SCAN_INTERVAL_TICKS = 20;
    public static final int MAX_EXPIRE_PER_TICK = 16;

    private TorchLifetimeRules() {}

    public static int clampDays(int days) {
        if (days <= 0) {
            return 0;
        }
        return Math.min(MAX_DAYS, days);
    }

    public static boolean permanent(int days) {
        return clampDays(days) <= 0;
    }

    public static long lifetimeTicks(int days) {
        int clamped = clampDays(days);
        if (clamped <= 0) {
            return Long.MAX_VALUE;
        }
        return clamped * TICKS_PER_DAY;
    }

    /**
     * {@code placedAt} is dimension game time. Negative means no stamp (permanent).
     */
    public static boolean expired(long placedAt, long now, int days) {
        if (placedAt < 0L || permanent(days)) {
            return false;
        }
        return now - placedAt >= lifetimeTicks(days);
    }
}
