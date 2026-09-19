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
    /** Vanilla torch brightness. Permanent / unstamped torches always use full light. */
    public static final int FULL_LIGHT = 14;
    /** No dimming until this many whole days have burned. */
    public static final int DIM_AFTER_DAYS = 2;
    /** Campfires pull one log from a chest at most this many blocks away. */
    public static final int CAMPFIRE_REFUEL_RANGE = 12;

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

    /** Whole Minecraft days since {@code placedAt}. Negative stamp is 0. */
    public static int daysBurning(long placedAt, long now) {
        if (placedAt < 0L || now <= placedAt) {
            return 0;
        }
        long days = (now - placedAt) / TICKS_PER_DAY;
        if (days > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) days;
    }

    /**
     * {@code 14 - max(0, daysBurning - 2)}, capped at the block's vanilla light.
     * Unstamped and config-permanent torches stay at {@code vanillaLight}.
     */
    public static int lightLevel(int vanillaLight, long placedAt, long now, int burnDays) {
        int vanilla = Math.max(0, vanillaLight);
        if (placedAt < 0L || permanent(burnDays)) {
            return vanilla;
        }
        int dim = FULL_LIGHT - Math.max(0, daysBurning(placedAt, now) - DIM_AFTER_DAYS);
        return Math.max(0, Math.min(vanilla, dim));
    }

    /** Euclidean 3D: a chest at most {@code range} blocks from the campfire. */
    public static boolean chestInRange(int dx, int dy, int dz, int range) {
        int r = Math.max(0, range);
        return distanceSq(dx, dy, dz) <= (long) r * (long) r;
    }

    public static long distanceSq(int dx, int dy, int dz) {
        return (long) dx * dx + (long) dy * dy + (long) dz * dz;
    }

    /**
     * Push {@code placedAt} later by {@code extraDays} so the remaining lifetime
     * grows by that many Minecraft days.
     */
    public static long extendPlacedAt(long placedAt, int extraDays) {
        if (placedAt < 0L || permanent(extraDays)) {
            return placedAt;
        }
        long extra = lifetimeTicks(extraDays);
        if (placedAt > Long.MAX_VALUE - extra) {
            return Long.MAX_VALUE;
        }
        return placedAt + extra;
    }
}
