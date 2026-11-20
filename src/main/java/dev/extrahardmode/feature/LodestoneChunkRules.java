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
    /**
     * A gap longer than this between running samples is a startup, a pause, or a shutdown.
     * That gap is not charged.
     */
    public static final long MAX_RUNNING_GAP_MILLIS = 2_500L;

    public static final int MIN_RESIDENCE_SCORE = 12;
    /** Each residence point above {@link #MIN_RESIDENCE_SCORE} adds this many minutes. */
    public static final int BONUS_MINUTES_PER_POINT = 5;
    public static final long MINUTE_MILLIS = 60_000L;

    public static final String CAP_KEY = "tougher.message.lodestone_full";
    public static final String CAP = "These chunks are already active for 24 hours.";
    public static final String NO_HOME_KEY = "tougher.message.lodestone_home";
    public static final String NO_HOME = "This lodestone is not in a residence.";
    public static final String LOW_SCORE_KEY = "tougher.message.lodestone_points";
    public static final String LOW_SCORE = "This residence has %s points. It needs 12.";
    public static final String INVALID_KEY = "tougher.message.lodestone_invalid";
    public static final String INVALID = "This residence has %s points but is not valid yet.";

    private LodestoneChunkRules() {}

    /** The lodestone chunk plus the eight around it. */
    public static int coveredChunks() {
        int span = RADIUS * 2 + 1;
        return span * span;
    }

    /** A valid residence of at least 12 points can fuel a lodestone. */
    public static boolean acceptsResidence(boolean eligible, int score) {
        return eligible && score >= MIN_RESIDENCE_SCORE;
    }

    /**
     * One real hour at 12 points, plus 5 minutes for each point above that.
     * 15 points is 75 minutes. 22 points is 110 minutes.
     */
    public static long fuelMillis(int score) {
        int extra = Math.max(0, score - MIN_RESIDENCE_SCORE);
        long minutes = 60L + (long) extra * BONUS_MINUTES_PER_POINT;
        return minutes * MINUTE_MILLIS;
    }

    /** Adds {@code addedMillis} to the time still left, and never past 24 hours. */
    public static long extendedRemaining(long existingRemaining, long addedMillis) {
        long base = Math.max(0L, existingRemaining);
        long added = Math.max(0L, addedMillis);
        if (base >= CAP_MILLIS || added == 0L) {
            return base;
        }
        long room = CAP_MILLIS - base;
        return base + Math.min(room, added);
    }

    /** False when the chunk is already at the 24 hour cap. */
    public static boolean extendsTime(long existingRemaining) {
        return Math.max(0L, existingRemaining) < CAP_MILLIS;
    }

    /** Soonest time still left, or 0 when none of the chunks have any. */
    public static long soonestRemaining(long[] remainings) {
        long best = Long.MAX_VALUE;
        if (remainings != null) {
            for (long remaining : remainings) {
                if (remaining > 0L && remaining < best) {
                    best = remaining;
                }
            }
        }
        return best == Long.MAX_VALUE ? 0L : best;
    }

    /**
     * Real milliseconds between two samples while players were online.
     * The first sample, a clock step backward, and a long gap charge nothing.
     */
    public static long elapsedRunning(long previousMillis, long nowMillis) {
        if (previousMillis <= 0L || nowMillis <= previousMillis) {
            return 0L;
        }
        long gap = nowMillis - previousMillis;
        return gap <= MAX_RUNNING_GAP_MILLIS ? gap : 0L;
    }

    /** Time left after the server has been running. An idle gap passes {@code elapsedMillis} of 0. */
    public static long afterRunning(long remaining, long elapsedMillis) {
        long left = Math.max(0L, remaining);
        if (elapsedMillis <= 0L || left == 0L) {
            return left;
        }
        return Math.max(0L, left - elapsedMillis);
    }

    /**
     * Older saves stored a wall-clock expiry. A value above the 24 hour cap is that expiry.
     * A smaller value is time left already. Time that passed before this load is dropped once.
     */
    public static long fromStored(long stored, long now) {
        if (stored <= 0L) {
            return 0L;
        }
        if (stored <= CAP_MILLIS) {
            return stored;
        }
        long left = stored - now;
        if (left <= 0L) {
            return 0L;
        }
        return Math.min(left, CAP_MILLIS);
    }

    /**
     * Why a block was not used, or empty when this residence can fuel the lodestone.
     * {@code missing} is the home checklist, such as a door or a roof.
     */
    public static String residenceRefusal(boolean found, boolean eligible, int score, java.util.List<String> missing) {
        if (!found) {
            return NO_HOME;
        }
        int shown = Math.max(0, score);
        if (shown < MIN_RESIDENCE_SCORE) {
            return "This residence has " + shown + " points. It needs 12.";
        }
        if (!eligible) {
            String text = "This residence has " + shown + " points but is not valid yet.";
            if (missing == null || missing.isEmpty()) {
                return text;
            }
            return text + " Missing: " + String.join(", ", missing) + ".";
        }
        return "";
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
