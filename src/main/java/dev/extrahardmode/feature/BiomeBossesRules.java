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
    /** Euclidean blocks. A boss must spawn strictly farther than this from every player and chest. */
    public static final int SPAWN_CLEARANCE_BLOCKS = 80;
    /** Added to clearance so block-center vs player feet still clears the radius. */
    public static final int SPAWN_RING_SLACK = 4;
    /** Random extra blocks past the slack, so the search is a ring rather than one circle. */
    public static final int SPAWN_SEARCH_EXTRA = 32;
    /** Brood mothers look a little farther than a vanilla spider (16). */
    public static final double BROOD_FOLLOW_RANGE = 24.0;
    /** Added to movement speed as ADD_MULTIPLIED_BASE. 0.15 is 15% faster. */
    public static final double BROOD_SPEED_BONUS = 0.15;
    /** 5 seconds between spits. */
    public static final int BROOD_SPIT_INTERVAL_TICKS = 100;
    /** 3 seconds of blindness. */
    public static final int BROOD_SPIT_BLIND_TICKS = 60;
    public static final double BROOD_SPIT_DAMAGE_FRACTION = 0.5;
    public static final double BROOD_SPIT_RANGE = 20.0;
    public static final float BROOD_SPIT_VELOCITY = 1.5F;
    /** Lower than a llama's 10 so the shot is a real threat. */
    public static final float BROOD_SPIT_INACCURACY = 4.0F;
    /** Vanilla divisor when the tool is allowed to harvest the block. */
    public static final int CORRECT_TOOL_DIVISOR = 30;
    /** Vanilla divisor when the tool is the wrong one for drops. */
    public static final int WRONG_TOOL_DIVISOR = 100;
    /** How often a boss rechecks whether it can walk to its target. */
    public static final int BOSS_PATH_RECHECK_TICKS = 20;

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

    /**
     * A family that has spawned in this dimension, or been recorded on the campaign, does not spawn again.
     * {@code lastSpawnEpochMs <= 0} means this dimension has never spawned it.
     */
    public static boolean familyAlreadyUsed(long lastSpawnEpochMs, boolean recordedOnCampaign) {
        return recordedOnCampaign || lastSpawnEpochMs > 0L;
    }

    /** True when the distance is {@code blocks} or closer. */
    public static boolean tooClose(double dx, double dy, double dz, int blocks) {
        if (blocks <= 0) {
            return false;
        }
        double min = blocks;
        return dx * dx + dy * dy + dz * dz <= min * min;
    }

    /** Horizontal search distance: clearance, plus slack, plus {@code roll} within {@code extra}. */
    public static int spawnSearchDistance(int roll, int clearance, int extra) {
        int span = Math.max(1, extra);
        return Math.max(0, clearance) + SPAWN_RING_SLACK + Math.floorMod(roll, span);
    }

    /** Half of the boss's attack damage. Zero when the bite does not hurt. */
    public static float spitDamage(double attackDamage) {
        if (attackDamage <= 0.0 || BROOD_SPIT_DAMAGE_FRACTION <= 0.0) {
            return 0.0F;
        }
        return (float) (attackDamage * BROOD_SPIT_DAMAGE_FRACTION);
    }

    /** {@code lastTick <= 0} is not primed yet, so the first check does not fire immediately. */
    public static boolean spitReady(long nowTick, long lastTick, int intervalTicks) {
        if (intervalTicks <= 0) {
            return true;
        }
        if (lastTick <= 0L) {
            return false;
        }
        return nowTick - lastTick >= intervalTicks;
    }

    /**
     * One tick of vanilla mining progress for a tool, without haste, fatigue, or water.
     * {@code hardness < 0} cannot be broken. {@code hardness == 0} finishes in one tick.
     */
    public static float toolDestroyProgress(float hardness, float toolSpeed, boolean correctForDrops) {
        if (Float.isNaN(hardness) || hardness < 0.0F) {
            return 0.0F;
        }
        if (hardness == 0.0F) {
            return 1.0F;
        }
        float speed = Math.max(0.0F, toolSpeed);
        int divisor = correctForDrops ? CORRECT_TOOL_DIVISOR : WRONG_TOOL_DIVISOR;
        return speed / hardness / divisor;
    }
}
