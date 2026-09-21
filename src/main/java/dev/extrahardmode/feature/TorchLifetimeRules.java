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
    /** Torches pull coal or charcoal from a chest at most this many blocks away. */
    public static final int TORCH_REFUEL_RANGE = 16;
    /** Copper torches last this many times as long as a regular torch. */
    public static final int COPPER_DURATION_FACTOR = 2;
    /** Minecraft hours in a day; 24000 ticks / 24. */
    public static final long TICKS_PER_HOUR = 1000L;
    /** Crosshair remaining-time color. */
    public static final int LIGHT_LOOK_COLOR = 0xFFFFC14A;
    /** Crosshair color when the light never burns out. */
    public static final int LIGHT_LOOK_PERMANENT_COLOR = 0xFFFFE082;

    private TorchLifetimeRules() {}

    public static boolean isCopperTorchId(String blockId) {
        return "minecraft:copper_torch".equals(blockId) || "minecraft:copper_wall_torch".equals(blockId);
    }

    /** Copper lasts {@link #COPPER_DURATION_FACTOR} times {@code baseDays}. Permanent stays 0. */
    public static int burnDaysFor(int baseDays, boolean copper) {
        int clamped = clampDays(baseDays);
        if (clamped <= 0 || !copper) {
            return clamped;
        }
        long doubled = (long) clamped * COPPER_DURATION_FACTOR;
        if (doubled > MAX_DAYS) {
            return MAX_DAYS;
        }
        return (int) doubled;
    }

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

    public static boolean isTorchFuelItemId(String itemId) {
        return "minecraft:coal".equals(itemId) || "minecraft:charcoal".equals(itemId);
    }

    /**
     * Remaining burn ticks. {@code -1} means permanent (no stamp or config days is 0).
     */
    public static int remainingTicks(long placedAt, long now, int days) {
        if (placedAt < 0L || permanent(days)) {
            return -1;
        }
        long elapsed = now <= placedAt ? 0L : now - placedAt;
        long remaining = lifetimeTicks(days) - elapsed;
        if (remaining <= 0L) {
            return 0;
        }
        if (remaining >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) remaining;
    }

    /** Crosshair label: {@code Permanent}, {@code 6d 12h}, {@code 6d}, {@code 12h}, or {@code <1h}. */
    public static String remainingLabel(int remainingTicks) {
        if (remainingTicks < 0) {
            return "Permanent";
        }
        int days = remainingTicks / (int) TICKS_PER_DAY;
        int hours = (remainingTicks % (int) TICKS_PER_DAY) / (int) TICKS_PER_HOUR;
        if (days > 0 && hours > 0) {
            return days + "d " + hours + "h";
        }
        if (days > 0) {
            return days + "d";
        }
        if (hours > 0) {
            return hours + "h";
        }
        return "<1h";
    }

    public static int lightLookColor(int remainingTicks) {
        return remainingTicks < 0 ? LIGHT_LOOK_PERMANENT_COLOR : LIGHT_LOOK_COLOR;
    }
}
