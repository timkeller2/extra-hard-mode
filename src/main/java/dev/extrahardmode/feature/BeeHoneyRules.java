package dev.extrahardmode.feature;

import dev.extrahardmode.module.EntityHelper;

/**
 * How much honey a returning bee adds. Minecraft-free so JUnit can cover the roll.
 *
 * <p>The configured percent is the base. Each flower type near the hive adds the
 * square root of its count, summed and rounded once. Each other hive or nest within
 * {@link #HIVE_CROWD_BLOCKS} then subtracts {@link #HIVE_CROWD_PENALTY}. That result
 * stays at {@link #HIVE_CROWD_FLOOR} when the rate started higher. 0 never fills.
 * A rare double fill stays inside that same roll.
 */
public final class BeeHoneyRules {
    public static final int DEFAULT_PERCENT = 20;
    /** Each other hive or nest inside this sphere lowers the fill percent. */
    public static final int HIVE_CROWD_BLOCKS = 30;
    public static final int HIVE_CROWD_PENALTY = 10;
    /** A higher rate is not lowered past this by nearby hives. */
    public static final int HIVE_CROWD_FLOOR = 4;

    /** Vanilla bees with a hive turn back once they are this far from it (48 − 24). */
    public static final int HIVE_STAY_BLOCKS = 24;

    /** Vanilla flower search is a Manhattan box of this radius. */
    public static final int FLOWER_SEARCH_BLOCKS = 5;

    /** Blocks from the hive a bee can still reach a flower: stay distance plus the search. */
    public static final int HARVEST_RANGE = HIVE_STAY_BLOCKS + FLOWER_SEARCH_BLOCKS;

    public static final String CONFIG_COMMENT =
            "Base percent of the usual honey a bee adds when it returns. Each flower type within 29 blocks adds the square root of its count, rounded once, up to 100. Each other hive or nest within 30 blocks subtracts 10, and a higher rate stays at least 4. Counted once a Minecraft day. 20 is the default. 0 never fills. Shearing amount is hiveHoneycombCount.";

    private BeeHoneyRules() {}

    /**
     * One tall flower counts once. A sunflower counts on its upper half. Any other
     * tall flower counts on its lower half. A single-block flower always counts.
     */
    public static boolean countsHalf(boolean sunflower, boolean doublePlant, boolean upperHalf) {
        if (!doublePlant) {
            return true;
        }
        return sunflower == upperHalf;
    }

    /** Rounded sum of the square root of each positive count. One type of 16 is 4. Sixteen singles are 16. */
    public static int flowerBonus(int[] counts) {
        if (counts == null || counts.length == 0) {
            return 0;
        }
        double sum = 0.0;
        for (int count : counts) {
            if (count > 0) {
                sum += Math.sqrt(count);
            }
        }
        return (int) Math.round(sum);
    }

    /**
     * Fill chance for one hive. {@code basePercent} of 0 never fills. Otherwise the
     * base, capped at 100, plus {@link #flowerBonus}, capped at 100.
     */
    public static int fillPercent(int basePercent, int[] counts) {
        if (basePercent <= 0) {
            return 0;
        }
        int base = Math.min(basePercent, 100);
        int total = base + flowerBonus(counts);
        if (total > 100) {
            return 100;
        }
        return total;
    }

    /**
     * The percent a clock reports for one hive. A base of 0 is 0, and 100 or more is 100.
     * A rate already stored for this Minecraft day is that hive's production. Otherwise the
     * flowers are counted now and the day is left unstored.
     */
    public static int reportedPercent(int basePercent, boolean storedToday, int storedPercent, int[] counts) {
        if (basePercent <= 0) {
            return 0;
        }
        if (basePercent >= 100) {
            return 100;
        }
        if (storedToday && storedPercent > 0) {
            return Math.min(100, storedPercent);
        }
        return fillPercent(basePercent, counts);
    }

    /**
     * Honey levels to add for this delivery. {@code vanillaIncrement} is the usual
     * gain. {@code roll} is 0–99 from {@code nextInt(100)}.
     */
    public static int honeyIncrement(int vanillaIncrement, int percent, int roll) {
        if (vanillaIncrement <= 0) {
            return 0;
        }
        return EntityHelper.percent(roll, percent) ? vanillaIncrement : 0;
    }

    /**
     * Nearby hives lower a fill percent. {@code otherHives} is the count of other
     * hives and nests within {@link #HIVE_CROWD_BLOCKS}. A percent already at or below
     * {@link #HIVE_CROWD_FLOOR} stays there. No other hive leaves the percent unchanged.
     */
    public static int crowdedPercent(int percent, int otherHives) {
        if (percent <= HIVE_CROWD_FLOOR || otherHives <= 0) {
            return percent;
        }
        long reduced = (long) percent - (long) HIVE_CROWD_PENALTY * otherHives;
        if (reduced < HIVE_CROWD_FLOOR) {
            return HIVE_CROWD_FLOOR;
        }
        return (int) reduced;
    }

    /** True when a block this many steps away is inside the hive-crowd sphere, including the edge. */
    public static boolean withinHiveCrowd(int dx, int dy, int dz) {
        long reach = (long) HIVE_CROWD_BLOCKS * HIVE_CROWD_BLOCKS;
        long distance = (long) dx * dx + (long) dy * dy + (long) dz * dz;
        return distance <= reach;
    }
}
