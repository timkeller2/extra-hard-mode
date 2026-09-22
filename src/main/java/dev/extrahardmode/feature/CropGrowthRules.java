package dev.extrahardmode.feature;

import java.util.Random;

/**
 * Crop maturity duration. {@code 100} is vanilla speed, {@code 200} takes twice as long,
 * {@code 50} is twice as fast. Default Tougher is {@code 300}.
 */
public final class CropGrowthRules {
    public static final int VANILLA_DURATION_PERCENT = 100;
    public static final int DEFAULT_DURATION_PERCENT = 300;
    /**
     * Fastest allowed duration after season and soil. 100 is vanilla speed;
     * good soil cannot make plants grow faster than vanilla.
     */
    public static final int MIN_DURATION_PERCENT = VANILLA_DURATION_PERCENT;
    /** Sugar cane random ticks fire 1/10 as often as vanilla. */
    public static final int SUGAR_CANE_DURATION_PERCENT = 1000;
    /** Nether wart random ticks fire 1/20 as often as vanilla. */
    public static final int NETHER_WART_DURATION_PERCENT = 2000;
    /** Sapling random ticks fire 1/10 as often as vanilla. */
    public static final int TREE_DURATION_PERCENT = 1000;
    /** Leaf loot keeps this percent of sapling counts (50 = half). */
    public static final int SAPLING_DROP_KEEP_PERCENT = 50;
    /** Pumpkin and melon fruit attempts fire 1/10 as often as vanilla. */
    public static final int STEM_FRUIT_DURATION_PERCENT = 1000;
    /** Extra chance the vine becomes a weed after each fruit, starting at 0%. */
    public static final int STEM_VINE_DEATH_PERCENT_PER_FRUIT = 5;
    public static final String MELON_SEEDS_ID = "minecraft:melon_seeds";
    public static final String PUMPKIN_SEEDS_ID = "minecraft:pumpkin_seeds";
    /** Vanilla wait after a full composter before bone meal is ready, in ticks. */
    public static final int VANILLA_COMPOSTER_READY_DELAY = 20;
    /** Ready-to-harvest wait is vanilla × this (fill rate is unchanged). */
    public static final int COMPOSTER_SPEED_DIVISOR = 200;
    /** Soil crop-loss modifier = this − (hoe quality × {@link #HOE_SOIL_PER_QUALITY}). */
    public static final int HOE_SOIL_BASE = 10;
    public static final int HOE_SOIL_PER_QUALITY = 5;
    /** Added to the soil modifier when a crop is harvested without a hoe. */
    public static final int HAND_HARVEST_SOIL_INCREASE = 5;
    /** Added when the same crop is planted again on a plot. */
    public static final int SAME_CROP_REPLANT_INCREASE = 5;
    /** Durability a hoe pays to harvest a crop vanilla would break for free. */
    public static final int HOE_INSTANT_HARVEST_DAMAGE = 1;
    /** Hoe till/harvest step when the stored modifier improves (gets better). */
    public static final int HOE_WORK_STEP = 3;
    /** Shown soil modifier gained when bone meal is used on a plant. */
    public static final int BONE_MEAL_SOIL_IMPROVE = 5;
    /** Worse-direction hoe step is 1/10 of the gap, clamped to these. */
    public static final int HOE_WORK_STEP_WORSE_MIN = 1;
    public static final int HOE_WORK_STEP_WORSE_MAX = 4;
    /** Negative hoe targets (diamond/gold/netherite) are multiplied by this. */
    public static final int HOE_SOIL_GOOD_FACTOR = 2;
    /** First till: 1d10 subtracted from the water score, within this Chebyshev range. */
    public static final int FIRST_TILL_DIE = 10;
    public static final int FIRST_TILL_WATER_RANGE = 2;
    /** Water sources add this much, then each source past it subtracts one, down to 0. */
    public static final int FIRST_TILL_WATER_PEAK = 50;
    /** Shown first-till bonus per hoe quality point. Wood/stone 0 … netherite 10. */
    public static final int FIRST_TILL_HOE_BONUS_PER_QUALITY = 2;
    /** Vanilla sugar cane AGE when a new segment is placed. */
    public static final int SUGAR_CANE_GROW_AGE = 15;
    /** Hoe look-at HUD: green at or above 0, red below. */
    public static final int SOIL_LOOK_COLOR_GOOD = 0xFF00FF00;
    public static final int SOIL_LOOK_COLOR_BAD = 0xFFFF0000;

    private CropGrowthRules() {}

    public static int clampDurationPercent(int durationPercent) {
        return Math.max(1, durationPercent);
    }

    /** When duration ≥ 100, this many vanilla randomTicks should run (0 or 1). */
    public static boolean allowVanillaRandomTick(int durationPercent, Random random) {
        int duration = clampDurationPercent(durationPercent);
        if (duration <= VANILLA_DURATION_PERCENT) {
            return true;
        }
        return random.nextInt(duration) < VANILLA_DURATION_PERCENT;
    }

    /**
     * Extra {@code randomTick} passes after the vanilla one when duration &lt; 100.
     * Duration 50 → one extra pass (2×).
     */
    public static int extraRandomTicks(int durationPercent, Random random) {
        int duration = clampDurationPercent(durationPercent);
        if (duration >= VANILLA_DURATION_PERCENT) {
            return 0;
        }
        double factor = (double) VANILLA_DURATION_PERCENT / (double) duration;
        int extra = (int) Math.floor(factor) - 1;
        double frac = factor - Math.floor(factor);
        if (frac > 0.0 && random.nextDouble() < frac) {
            extra++;
        }
        return Math.max(0, extra);
    }

    public static int composterReadyDelay(int vanillaDelay) {
        int delay = Math.max(1, vanillaDelay);
        return delay * COMPOSTER_SPEED_DIVISOR;
    }

    /**
     * Chance the pumpkin/melon vine dies after this fruit appears. 0% on the
     * first fruit, then +5% per fruit already grown, capped at 100%.
     */
    public static int stemVineDeathChancePercent(int fruitsAlreadyGrown) {
        return Math.min(100, Math.max(0, fruitsAlreadyGrown) * STEM_VINE_DEATH_PERCENT_PER_FRUIT);
    }

    public static boolean stemVineDies(int fruitsAlreadyGrown, Random random) {
        int chance = stemVineDeathChancePercent(fruitsAlreadyGrown);
        if (chance <= 0) {
            return false;
        }
        if (chance >= 100) {
            return true;
        }
        return random.nextInt(100) < chance;
    }

    public static boolean isStemSeedItemId(String itemId) {
        return MELON_SEEDS_ID.equals(itemId) || PUMPKIN_SEEDS_ID.equals(itemId);
    }

    /**
     * Expected {@code count * keepPercent / 100} items. Remainder is a chance
     * of one extra, so a single sapling is kept half the time at 50%.
     */
    public static int scaledDropCount(int count, int keepPercent, Random random) {
        int n = Math.max(0, count);
        int keep = Math.clamp(keepPercent, 0, 100);
        if (n == 0 || keep >= 100) {
            return n;
        }
        if (keep <= 0) {
            return 0;
        }
        int whole = (n * keep) / 100;
        int rem = (n * keep) % 100;
        if (rem > 0 && random.nextInt(100) < rem) {
            whole++;
        }
        return whole;
    }

    public static final long TICKS_PER_DAY = 24000L;
    /** Loss and growth swing 1 percentage point of the configured loss rate per Minecraft day. */
    public static final double SEASON_STEP_PERCENT = 1.0;
    /** Seasonal high end is this many times the configured base loss (25 → 75). */
    public static final int SEASON_MAX_MULTIPLIER = 3;
    /** Bees stay in hive while seasonal loss is strictly above this. */
    public static final double BEE_INACTIVE_LOSS_RATE = 60.0;
    /** Cows, sheep, and pigs drop 1 less meat while seasonal loss is strictly above this. */
    public static final double LARGE_ANIMAL_MEAT_LOSS_RATE = 25.0;
    public static final int LARGE_ANIMAL_MEAT_REDUCE_MID = 1;
    public static final int LARGE_ANIMAL_MEAT_REDUCE_HARSH = 2;
    public static final int CHICKEN_MEAT_REDUCE_HARSH = 1;
    public static final int MEAT_DROP_MIN = 0;

    public static long dayIndex(long dayTime) {
        if (dayTime < 0L) {
            return 0L;
        }
        return dayTime / TICKS_PER_DAY;
    }

    /**
     * Seasonal crop-death percent. Starts at {@code normalLoss}, falls 1 per day to
     * ½ normal (easier early game), then rises to {@link #SEASON_MAX_MULTIPLIER}×
     * normal, then repeats between those bounds.
     */
    public static double seasonalLossRate(double normalLoss, long dayIndex) {
        return seasonalLossRate(normalLoss, dayIndex, true);
    }

    /**
     * {@code changingSeasons} false always returns the base rate. True swings
     * from half to {@link #SEASON_MAX_MULTIPLIER} times that rate.
     */
    public static double seasonalLossRate(double normalLoss, long dayIndex, boolean changingSeasons) {
        double normal = Math.max(0.0, normalLoss);
        if (!changingSeasons) {
            return Math.min(100.0, normal);
        }
        if (normal == 0.0) {
            return 0.0;
        }
        int start = Math.max(0, (int) Math.round(normal));
        int max = Math.min(100, Math.max(start, start * SEASON_MAX_MULTIPLIER));
        int min = Math.max(0, (int) Math.round(normal / 2.0));
        if (min > max) {
            min = max;
        }
        long day = Math.max(0L, dayIndex);
        long firstDown = start - min;
        if (day <= firstDown) {
            return start - day;
        }
        long after = day - firstDown;
        long swing = max - min;
        if (swing <= 0L) {
            return min;
        }
        long period = swing * 2L;
        long pos = after % period;
        if (pos <= swing) {
            return min + pos;
        }
        return max - (pos - swing);
    }

    /**
     * Crop duration scaled so its ratio to the configured duration matches
     * seasonal loss / normal loss. Higher duration is slower growth.
     */
    public static int seasonalDurationPercent(int baseDurationPercent, double normalLoss, long dayIndex) {
        return durationPercent(baseDurationPercent, normalLoss, seasonalLossRate(normalLoss, dayIndex), 0);
    }

    /**
     * Duration using seasonal crop loss plus the plant's soil modifier
     * (stored; shown modifier is the negation). Positive stored modifier (worse
     * soil) slows growth; negative speeds it. Never faster than
     * {@link #MIN_DURATION_PERCENT}.
     */
    public static int durationPercent(
            int baseDurationPercent, double normalLoss, double seasonalLoss, int soilModifier) {
        double normal = Math.max(0.0, normalLoss);
        double loss = Math.max(0.0, seasonalLoss + soilModifier);
        double factor = normal <= 0.0 ? 1.0 : loss / normal;
        int duration = (int) Math.round(baseDurationPercent * factor);
        return Math.max(MIN_DURATION_PERCENT, clampDurationPercent(duration));
    }

    public static boolean beesInactive(double seasonalLoss) {
        return seasonalLoss > BEE_INACTIVE_LOSS_RATE;
    }

    public static int largeAnimalMeatReduce(double seasonalLoss) {
        if (seasonalLoss > BEE_INACTIVE_LOSS_RATE) {
            return LARGE_ANIMAL_MEAT_REDUCE_HARSH;
        }
        if (seasonalLoss > LARGE_ANIMAL_MEAT_LOSS_RATE) {
            return LARGE_ANIMAL_MEAT_REDUCE_MID;
        }
        return 0;
    }

    public static int chickenMeatReduce(double seasonalLoss) {
        return seasonalLoss > BEE_INACTIVE_LOSS_RATE ? CHICKEN_MEAT_REDUCE_HARSH : 0;
    }

    /** Meat drops cannot go below {@link #MEAT_DROP_MIN}. */
    public static int reducedMeatCount(int count, int reduce) {
        if (count <= 0) {
            return 0;
        }
        return Math.max(MEAT_DROP_MIN, count - Math.max(0, reduce));
    }

    /**
     * Shrinks stack counts from the end so their sum becomes {@link #reducedMeatCount}.
     * Zeroed entries should be removed by the caller.
     */
    public static void reduceStackCounts(int[] counts, int reduce) {
        if (counts == null || counts.length == 0 || reduce <= 0) {
            return;
        }
        int total = 0;
        for (int count : counts) {
            total += Math.max(0, count);
        }
        int extra = total - reducedMeatCount(total, reduce);
        for (int i = counts.length - 1; i >= 0 && extra > 0; i--) {
            int have = Math.max(0, counts[i]);
            int take = Math.min(have, extra);
            counts[i] = have - take;
            extra -= take;
        }
    }

    public static String lossRateLabel(double lossRate) {
        double value = Math.max(0.0, lossRate);
        if (Math.abs(value - Math.round(value)) < 1.0E-6) {
            return Long.toString(Math.round(value));
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    /**
     * Vanilla skips tool damage when the block's destroy speed is 0. A hoe
     * used to harvest those crops still wears.
     */
    public static boolean chargeHoeForInstantHarvest(boolean hoe, float destroySpeed) {
        return hoe && destroySpeed == 0.0F;
    }

    /**
     * Extra crop-loss percent from hoe quality (Let it grow material bonus).
     * Wood/stone 10, copper 5, iron 0, diamond −10, gold −20, netherite −30.
     */
    public static int hoeSoilModifier(int hoeQuality) {
        int value = HOE_SOIL_BASE - Math.max(0, hoeQuality) * HOE_SOIL_PER_QUALITY;
        if (value < 0) {
            return value * HOE_SOIL_GOOD_FACTOR;
        }
        return value;
    }

    public static int afterHandHarvest(int modifier) {
        return modifier + HAND_HARVEST_SOIL_INCREASE;
    }

    public static int afterSameCropReplant(int modifier) {
        return modifier + SAME_CROP_REPLANT_INCREASE;
    }

    public static boolean isSameCrop(String previousId, String plantedId) {
        return plantedId != null && !plantedId.isEmpty() && plantedId.equals(previousId);
    }

    /**
     * Floating-number color: green when the shown value is 0 or improved, red when
     * it got worse. Inspect (no before) uses the current shown value.
     */
    public static boolean modifierChangeIsGood(int storedAfter, Integer storedBefore) {
        if (storedBefore == null) {
            return displayedModifier(storedAfter) >= 0;
        }
        return displayedDelta(storedAfter, storedBefore) >= 0;
    }

    /**
     * Unworked soil is 0. A hoe does not snap to its target; it moves
     * {@link #hoeWorkStep(int, int)} points toward {@link #hoeSoilModifier(int)},
     * and will not overshoot.
     */
    public static int afterHoeWork(int current, int hoeQuality) {
        int target = hoeSoilModifier(hoeQuality);
        return stepToward(current, target, hoeWorkStep(current, target));
    }

    /**
     * Improving (stored drops) uses {@link #HOE_WORK_STEP}. Getting worse uses
     * 1/10 of the gap to the target, clamped to
     * {@link #HOE_WORK_STEP_WORSE_MIN}–{@link #HOE_WORK_STEP_WORSE_MAX}.
     */
    public static int hoeWorkStep(int current, int target) {
        if (target <= current) {
            return HOE_WORK_STEP;
        }
        int tenth = (int) Math.round((target - current) / 10.0);
        return Math.clamp(tenth, HOE_WORK_STEP_WORSE_MIN, HOE_WORK_STEP_WORSE_MAX);
    }

    /**
     * Water sources within range. Counts up to {@link #FIRST_TILL_WATER_PEAK}, then each extra
     * source subtracts one from that peak, and the result does not go below 0.
     */
    public static int waterSourceScore(int sources) {
        int count = Math.max(0, sources);
        if (count <= FIRST_TILL_WATER_PEAK) {
            return count;
        }
        int over = count - FIRST_TILL_WATER_PEAK;
        return Math.max(0, FIRST_TILL_WATER_PEAK - over);
    }

    /**
     * Stored modifier on first till. Shown value is the negation: water score, minus 1d10,
     * plus {@link #FIRST_TILL_HOE_BONUS_PER_QUALITY} per hoe quality point.
     */
    public static int firstTillModifier(int d10, int waterSources, int hoeQuality) {
        int roll = Math.clamp(d10, 1, FIRST_TILL_DIE);
        int hoe = Math.max(0, hoeQuality) * FIRST_TILL_HOE_BONUS_PER_QUALITY;
        return roll - waterSourceScore(waterSources) - hoe;
    }

    /** Let it grow: stored modifier drops by {@link #HOE_WORK_STEP} (shown value rises by 3). */
    public static int afterGrowWork(int current) {
        return current - HOE_WORK_STEP;
    }

    /** Bone meal on a plant: stored modifier drops by {@link #BONE_MEAL_SOIL_IMPROVE} (shown value rises by 5). */
    public static int afterBoneMeal(int current) {
        return current - BONE_MEAL_SOIL_IMPROVE;
    }

    public static int stepToward(int current, int target, int step) {
        int max = Math.max(0, step);
        int delta = target - current;
        if (delta > max) {
            return current + max;
        }
        if (delta < -max) {
            return current - max;
        }
        return target;
    }

    public static int clampLossChance(int chance) {
        return Math.clamp(chance, 0, 100);
    }

    public static int lossChance(double seasonalLoss, int soilModifier) {
        return clampLossChance((int) Math.round(seasonalLoss) + soilModifier);
    }

    public static boolean rollLoss(int chance, Random random) {
        int clamped = clampLossChance(chance);
        if (clamped <= 0) {
            return false;
        }
        if (clamped >= 100) {
            return true;
        }
        return random.nextInt(100) < clamped;
    }

    /** True when a 1-high cane at max age would place a second segment. */
    public static boolean sugarCaneGrowsSecondSegment(int age, boolean airAbove, boolean belowIsCane) {
        return age == SUGAR_CANE_GROW_AGE && airAbove && !belowIsCane;
    }

    /** Positive stored modifiers are a crop-loss penalty (shown negated). */
    public static boolean modifierIsPenalty(int stored) {
        return stored > 0;
    }

    /** Shown to the player: positive is good. */
    public static int displayedModifier(int stored) {
        return -stored;
    }

    /** Displayed change: stored −18 then hand +5 → stored −13 is shown 13 with −5. */
    public static int displayedDelta(int storedAfter, int storedBefore) {
        return displayedModifier(storedAfter) - displayedModifier(storedBefore);
    }

    public static String percentLabel(int displayed) {
        return displayed + "%";
    }

    public static int soilLookColor(int displayed) {
        return displayed < 0 ? SOIL_LOOK_COLOR_BAD : SOIL_LOOK_COLOR_GOOD;
    }

    /** Empty hand, hoes, bone meal, and plantable seeds/seed-crops. */
    public static boolean showsSoilLook(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return true;
        }
        String path = itemId;
        int slash = itemId.indexOf(':');
        if (slash >= 0) {
            path = itemId.substring(slash + 1);
        }
        if ("bone_meal".equals(path) || path.endsWith("_hoe") || "hoe".equals(path)) {
            return true;
        }
        return switch (path) {
            case "wheat_seeds",
                    "pumpkin_seeds",
                    "melon_seeds",
                    "beetroot_seeds",
                    "torchflower_seeds",
                    "pitcher_pod",
                    "potato",
                    "carrot",
                    "nether_wart",
                    "cocoa_beans",
                    "sugar_cane" ->
                true;
            default -> false;
        };
    }

    public static String signedPercentLabel(int displayed) {
        if (displayed > 0) {
            return "+" + displayed + "%";
        }
        return displayed + "%";
    }

    public static String modifierLabel(int stored) {
        int shown = displayedModifier(stored);
        if (shown > 0) {
            return "+" + shown;
        }
        return Integer.toString(shown);
    }

    /** Current shown modifier and, in parentheses, how much it just changed. */
    public static String modifierChangeLabel(int storedAfter, int storedBefore) {
        return percentLabel(displayedModifier(storedAfter))
                + " ("
                + signedPercentLabel(displayedDelta(storedAfter, storedBefore))
                + ")";
    }
}
