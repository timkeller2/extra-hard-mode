package dev.extrahardmode.feature;

/**
 * Overgrazing thresholds and rolls. Minecraft-free so JUnit can cover the math.
 */
public final class OvergrazingRules {
    public static final int DEFAULT_INTERVAL_TICKS = 24000;
    public static final int DEFAULT_CHEST_RANGE = 20;
    public static final int DEFAULT_GRASS_RANGE = 6;
    public static final int DEFAULT_LARGE_MARKS = 9;
    public static final int DEFAULT_SMALL_MARKS = 4;
    public static final int MAX_MARKS = 32;
    public static final int LOOK_CHANCE_PERCENT = 33;
    public static final int STARVE_CHANCE_PERCENT = 33;
    public static final int FOOD_FLOAT_TICKS = 100;
    public static final int TICK_STRIDE = 20;
    public static final int MIN_INTERVAL_TICKS = 20;
    public static final int MAX_RANGE = 64;
    public static final int MAX_PATH_CHECKS = 64;

    private OvergrazingRules() {}

    public static int clampInterval(int ticks) {
        return Math.max(MIN_INTERVAL_TICKS, ticks);
    }

    public static int clampRange(int range) {
        if (range <= 0) {
            return 1;
        }
        return Math.min(MAX_RANGE, range);
    }

    public static int clampMarks(int marks) {
        if (marks <= 0) {
            return 1;
        }
        return Math.min(MAX_MARKS, marks);
    }

    public static int marksNeeded(boolean small, int largeMarks, int smallMarks) {
        return clampMarks(small ? smallMarks : largeMarks);
    }

    public static boolean unmarkedToday(int markedDay, int currentDay) {
        return markedDay != currentDay;
    }

    /** {@code roll} is 0–99. */
    public static boolean lookInChests(int roll) {
        return lookInChests(roll, LOOK_CHANCE_PERCENT);
    }

    /** {@code roll} is 0–99. */
    public static boolean lookInChests(int roll, int percent) {
        return chance(roll, percent);
    }

    /** {@code roll} is 0–99. */
    public static boolean starve(int roll) {
        return chance(roll, STARVE_CHANCE_PERCENT);
    }

    public static boolean chance(int roll, int percent) {
        if (percent <= 0) {
            return false;
        }
        if (percent >= 100) {
            return true;
        }
        return roll < percent;
    }

    public static long firstCheckAt(long now, int interval, int randomOffset) {
        int span = clampInterval(interval);
        int offset = Math.floorMod(randomOffset, span);
        return now + 1L + offset;
    }

    public static boolean isDue(long now, long scheduled) {
        return scheduled > 0L && now >= scheduled;
    }

    public static long reschedule(long now, int interval) {
        return now + clampInterval(interval);
    }
}
