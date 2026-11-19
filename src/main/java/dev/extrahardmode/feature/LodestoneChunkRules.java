package dev.extrahardmode.feature;

/**
 * How long a redstone block keeps a lodestone's chunks active.
 * Minecraft-free so the hour, the cap, and the look text can be tested.
 */
public final class LodestoneChunkRules {
    /** Chunks in each direction from the lodestone's chunk, including that chunk. */
    public static final int RADIUS = 1;
    public static final long HOUR_MILLIS = 3_600_000L;
    public static final long CAP_MILLIS = 24 * HOUR_MILLIS;

    public static final String CAP_KEY = "tougher.message.lodestone_full";
    public static final String CAP = "These chunks are already active for 24 hours.";

    private LodestoneChunkRules() {}

    /** The lodestone chunk plus the eight around it. */
    public static int coveredChunks() {
        int span = RADIUS * 2 + 1;
        return span * span;
    }

    /**
     * One more real hour on top of the time still left, and never past 24 hours from now.
     * An expiry already in the past counts as now.
     */
    public static long extendedExpiry(long now, long existingExpiry) {
        long base = Math.max(existingExpiry, now);
        return Math.min(base + HOUR_MILLIS, now + CAP_MILLIS);
    }

    /** False when the chunk is already at the 24 hour cap. */
    public static boolean extendsTime(long now, long existingExpiry) {
        return extendedExpiry(now, existingExpiry) > Math.max(existingExpiry, now);
    }

    /** Soonest expiry still in the future, or 0 when none of them are. */
    public static long soonestExpiry(long now, long[] expiries) {
        long best = Long.MAX_VALUE;
        if (expiries != null) {
            for (long expiry : expiries) {
                if (expiry > now && expiry < best) {
                    best = expiry;
                }
            }
        }
        return best == Long.MAX_VALUE ? 0L : best;
    }

    public static long remaining(long now, long expiry) {
        return expiry > now ? expiry - now : 0L;
    }

    /** Empty when no time is left. Shown under the crosshair and after a block is used. */
    public static String lookLabel(long remainingMillis) {
        if (remainingMillis <= 0L) {
            return "";
        }
        long seconds = remainingMillis / 1000L;
        if (seconds < 60L) {
            long shown = Math.max(1L, seconds);
            return shown == 1L ? "Active for 1 second" : "Active for " + shown + " seconds";
        }
        long minutes = seconds / 60L;
        if (minutes < 60L) {
            return minutes == 1L ? "Active for 1 minute" : "Active for " + minutes + " minutes";
        }
        long hours = minutes / 60L;
        long extraMinutes = minutes % 60L;
        String hourPart = hours == 1L ? "1 hour" : hours + " hours";
        if (extraMinutes == 0L) {
            return "Active for " + hourPart;
        }
        String minutePart = extraMinutes == 1L ? "1 minute" : extraMinutes + " minutes";
        return "Active for " + hourPart + " " + minutePart;
    }
}
