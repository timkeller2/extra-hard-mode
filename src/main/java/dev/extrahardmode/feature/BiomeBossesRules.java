package dev.extrahardmode.feature;

/**
 * Biome-boss spawn gates. Minecraft-free so JUnit can cover distance, cooldown, and rolls.
 */
public final class BiomeBossesRules {
    public static final int DEFAULT_MIN_DISTANCE = 300;
    public static final int DEFAULT_COOLDOWN_HOURS = 8;
    public static final int DEFAULT_SPAWN_CHANCE_PERCENT = 1;
    /** 20 ticks/second × 60 = one minute between checks. */
    public static final int DEFAULT_CHECK_INTERVAL_TICKS = 1200;
    public static final double DEFAULT_SCALE = 1.6;
    public static final int DEFAULT_HEALTH_MULTIPLIER_MIN = 6;
    public static final int DEFAULT_HEALTH_MULTIPLIER_MAX = 12;
    public static final double DEFAULT_ATTACK_MULTIPLIER = 1.75;
    public static final int DEFAULT_HEAVY_ARMOR_DROP_PERCENT = 25;
    public static final int DEFAULT_BONUS_XP = 40;
    /** Leather, chain, gold, iron, diamond. */
    public static final int ARMOR_TIER_COUNT = 5;
    public static final int DEFAULT_DISTANCE_STEP_BLOCKS = 25;
    public static final double DEFAULT_DIFFICULTY_PERCENT_PER_STEP = 1.0;
    /** Treasure % rises this many times as fast as difficulty %. */
    public static final double DEFAULT_TREASURE_PERCENT_SCALE = 3.0;
    /** Each defeated boss multiplies the next boss's difficulty and treasure. */
    public static final double DEFEAT_COMPOUND = 1.3;
    public static final int THIRD_MILESTONE = 3;
    public static final int CREDITS_MILESTONE = 7;
    public static final int TITLE_FADE_IN_TICKS = 10;
    public static final int TITLE_STAY_TICKS = 80;
    public static final int TITLE_FADE_OUT_TICKS = 20;
    public static final int SEVENTH_TITLE_STAY_TICKS = 120;
    public static final int CREDITS_DELAY_TICKS = 140;

    private BiomeBossesRules() {}

    public static boolean farEnough(double dx, double dz, int minDistance) {
        long min = Math.max(0, minDistance);
        return dx * dx + dz * dz >= (double) min * (double) min;
    }

    public static long cooldownMillis(int hours) {
        return Math.max(0, hours) * 3_600_000L;
    }

    public static boolean cooldownElapsed(long nowMs, long lastSpawnMs, long cooldownMs) {
        if (lastSpawnMs <= 0L || cooldownMs <= 0L) {
            return true;
        }
        return nowMs - lastSpawnMs >= cooldownMs;
    }

    /** {@code roll} is 0–99 from {@code nextInt(100)}. */
    public static boolean spawnRoll(int roll, int percent) {
        if (percent <= 0) {
            return false;
        }
        if (percent >= 100) {
            return true;
        }
        return roll < percent;
    }

    public static double extraMultiplierAmount(double multiplier) {
        return Math.max(0.0, multiplier - 1.0);
    }

    /**
     * Inclusive random multiplier. {@code roll} is any non-negative int; the result is in
     * {@code [min, max]} (bounds swapped if needed, floored at 1).
     */
    public static int randomHealthMultiplier(int roll, int min, int max) {
        int lo = Math.max(1, Math.min(min, max));
        int hi = Math.max(lo, Math.max(min, max));
        int span = hi - lo + 1;
        return lo + Math.floorMod(roll, span);
    }

    /** 0 leather … 4 diamond. */
    public static int armorTier(int roll) {
        return Math.floorMod(roll, ARMOR_TIER_COUNT);
    }

    public static double horizontalDistance(double dx, double dz) {
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Complete 25-block (or {@code stepBlocks}) increments past {@code minDistance}. */
    public static int distanceSteps(double distance, int minDistance, int stepBlocks) {
        int min = Math.max(0, minDistance);
        int step = Math.max(1, stepBlocks);
        double extra = distance - min;
        if (extra <= 0.0) {
            return 0;
        }
        return (int) (extra / step);
    }

    public static double difficultyPercent(int steps, double percentPerStep) {
        return Math.max(0, steps) * Math.max(0.0, percentPerStep);
    }

    /** ADD_MULTIPLIED_TOTAL amount: +1% → 0.01. */
    public static double difficultyBonusAmount(double difficultyPercent) {
        return Math.max(0.0, difficultyPercent) / 100.0;
    }

    public static double treasureMultiplier(double difficultyPercent, double treasurePercentScale) {
        return 1.0 + Math.max(0.0, difficultyPercent) * Math.max(0.0, treasurePercentScale) / 100.0;
    }

    /** {@code 1.3 ^ defeated}. Applied on top of distance difficulty and treasure. */
    public static double defeatCompound(int defeated) {
        if (defeated <= 0) {
            return 1.0;
        }
        return Math.pow(DEFEAT_COMPOUND, defeated);
    }

    public static double treasureWithDefeats(
            double difficultyPercent, double treasurePercentScale, int defeated) {
        return treasureMultiplier(difficultyPercent, treasurePercentScale) * defeatCompound(defeated);
    }

    public static boolean isThirdMilestone(int defeated) {
        return defeated == THIRD_MILESTONE;
    }

    public static boolean isCreditsMilestone(int defeated) {
        return defeated == CREDITS_MILESTONE;
    }

    /** Stack counts. Baseline (multiplier 1) keeps {@code base}; nearer bosses stay at or below farther ones. */
    public static int scaleCount(int base, double multiplier) {
        if (base <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.round(base * Math.max(0.0, multiplier)));
    }

    public static int scalePercent(int percent, double multiplier) {
        long scaled = Math.round(percent * Math.max(0.0, multiplier));
        return (int) Math.max(0L, Math.min(100L, scaled));
    }

    public static int scaleXp(int base, double multiplier) {
        return Math.max(0, (int) Math.round(base * Math.max(0.0, multiplier)));
    }
}
