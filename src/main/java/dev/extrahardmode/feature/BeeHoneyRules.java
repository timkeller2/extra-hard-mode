package dev.extrahardmode.feature;

import dev.extrahardmode.module.EntityHelper;

/**
 * How much honey a returning bee adds. Minecraft-free so JUnit can cover the roll.
 *
 * <p>The configured percent is the base. Each flower type near the hive adds the
 * square root of its count, summed and rounded once. 100 always fills. 0 never fills.
 * A rare double fill stays inside that same roll.
 */
public final class BeeHoneyRules {
    public static final int DEFAULT_PERCENT = 20;

    /** Vanilla bees with a hive turn back once they are this far from it (48 − 24). */
    public static final int HIVE_STAY_BLOCKS = 24;

    /** Vanilla flower search is a Manhattan box of this radius. */
    public static final int FLOWER_SEARCH_BLOCKS = 5;

    /** Blocks from the hive a bee can still reach a flower: stay distance plus the search. */
    public static final int HARVEST_RANGE = HIVE_STAY_BLOCKS + FLOWER_SEARCH_BLOCKS;

    public static final String CONFIG_COMMENT =
            "Base percent of the usual honey a bee adds when it returns. Each flower type within 29 blocks adds the square root of its count, rounded once, up to 100. Counted once a Minecraft day. 20 is the default. 100 always fills. 0 never fills. Shearing amount is hiveHoneycombCount.";

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
     * Honey levels to add for this delivery. {@code vanillaIncrement} is the usual
     * gain. {@code roll} is 0–99 from {@code nextInt(100)}.
     */
    public static int honeyIncrement(int vanillaIncrement, int percent, int roll) {
        if (vanillaIncrement <= 0) {
            return 0;
        }
        return EntityHelper.percent(roll, percent) ? vanillaIncrement : 0;
    }
}
