package dev.extrahardmode.feature;

/**
 * Overgrazing thresholds and rolls. Minecraft-free so JUnit can cover the math.
 *
 * <p>Bite timing was chosen against vanilla grass spread. A grass block tries to
 * spread on a lit random tick (default speed 3, and only while surface light
 * stays at least 9, which is about 13,700 ticks of each day). At that full
 * rate, one flat pasture of {@link #DEFAULT_LARGE_BLOCKS} blocks keeps up with
 * a large animal at {@link #DEFAULT_LARGE_INTERVAL_TICKS}, and
 * {@link #DEFAULT_SMALL_BLOCKS} blocks keep up with a chicken or rabbit at
 * {@link #DEFAULT_SMALL_INTERVAL_TICKS}. {@link GrassSpreadRules} defaults to
 * a quarter of that spread, so the same pasture shrinks. Raising the block
 * count makes that animal eat sooner, so it needs more grass.
 */
public final class OvergrazingRules {
    public static final int DEFAULT_CHEST_RANGE = 20;
    public static final int DEFAULT_GRASS_RANGE = 6;
    public static final int DEFAULT_LARGE_BLOCKS = 9;
    public static final int DEFAULT_SMALL_BLOCKS = 4;
    /** Ticks between bites when a large animal is supported by {@link #DEFAULT_LARGE_BLOCKS} blocks. */
    public static final int DEFAULT_LARGE_INTERVAL_TICKS = 5000;
    /** Ticks between bites when a small animal is supported by {@link #DEFAULT_SMALL_BLOCKS} blocks. */
    public static final int DEFAULT_SMALL_INTERVAL_TICKS = 14000;
    public static final int MAX_BLOCKS = 32;
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

    public static int clampBlocks(int blocks) {
        if (blocks <= 0) {
            return 1;
        }
        return Math.min(MAX_BLOCKS, blocks);
    }

    public static int blocksToSupport(boolean small, int largeBlocks, int smallBlocks) {
        return clampBlocks(small ? smallBlocks : largeBlocks);
    }

    /**
     * Ticks between bites. Fewer supporting blocks means a slower animal, so one
     * pasture block can keep up.
     */
    public static int eatIntervalTicks(boolean small, int largeBlocks, int smallBlocks) {
        int blocks = blocksToSupport(small, largeBlocks, smallBlocks);
        long perDefault = small
                ? (long) DEFAULT_SMALL_INTERVAL_TICKS * DEFAULT_SMALL_BLOCKS
                : (long) DEFAULT_LARGE_INTERVAL_TICKS * DEFAULT_LARGE_BLOCKS;
        long scaled = perDefault / blocks;
        if (scaled > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return clampInterval((int) scaled);
    }

    /**
     * Which gathered chest stack to eat. Sheared short grass wins over breeding
     * food, even when the breeding food is in an earlier slot. Returns -1 when
     * nothing qualifies. The shorter array limits the scan.
     */
    public static int preferredChestIndex(boolean[] storedGrass, boolean[] breedingFood) {
        int size = Math.min(storedGrass.length, breedingFood.length);
        for (int i = 0; i < size; i++) {
            if (storedGrass[i]) {
                return i;
            }
        }
        for (int i = 0; i < size; i++) {
            if (breedingFood[i]) {
                return i;
            }
        }
        return -1;
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

    /** True when {@code scheduled} is still inside this animal's bite interval. */
    public static boolean scheduledSoon(long now, long scheduled, int interval) {
        return scheduled > now && scheduled <= now + (long) clampInterval(interval);
    }

    public static long reschedule(long now, int interval) {
        return now + clampInterval(interval);
    }
}
