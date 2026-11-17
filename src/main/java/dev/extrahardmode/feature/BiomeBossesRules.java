package dev.extrahardmode.feature;

import java.util.ArrayList;
import java.util.List;

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
    /** Chance a slain boss drops one enchanted heavy sword or axe. Treasure can raise it. */
    public static final int DEFAULT_HEAVY_WEAPON_DROP_PERCENT = 25;
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
    /**
     * Horizontal blocks. The same family can spawn again only when the new site is at least
     * this far from every earlier spawn of that family in the same dimension.
     */
    public static final int FAMILY_SEPARATION_BLOCKS = 500;
    /** Euclidean blocks. A loaded boss shows its named health bar to players this close. */
    public static final double BAR_RANGE_BLOCKS = 64.0;
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
    /** Chebyshev blocks searched when a boss is suffocating inside solid blocks. */
    public static final int UNSTUCK_RADIUS = 16;
    /** Ticks to wait before searching again when no open spot exists. */
    public static final int UNSTUCK_RETRY_TICKS = 20;
    /** Horizontal blocks. Every finale summon stays inside this distance of the death site. */
    public static final double FINALE_SPAWN_RADIUS = 16.0;
    /** Chebyshev blocks searched around a preferred finale spot before a wider search. */
    public static final int FINALE_SEARCH_RADIUS = 8;
    /** Survival players inside this range of the death site can be chosen as a finale target. */
    public static final double FINALE_AGGRO_BLOCKS = 64.0;
    /** The death group repeats this many times at most. Health is not capped. */
    public static final int FINALE_COUNT_CAP = 4;
    /** Vanilla diamond pickaxe destroy speed on stone and ore. */
    public static final float DIAMOND_PICKAXE_SPEED = 8.0F;
    /** A boss mines as if this many diamond pickaxes were swinging, times the boss number. */
    public static final int MINE_PICKAXE_COUNT = 3;
    /** 4 seconds between leaps while a boss is fighting. */
    public static final int BOSS_JUMP_INTERVAL_TICKS = 80;
    /** Peak height of a leap, in blocks. */
    public static final double BOSS_JUMP_HEIGHT = 4.0;
    /** Horizontal leap toward the target, in blocks, after leaving one block of room. */
    public static final double BOSS_JUMP_DISTANCE = 4.0;
    /** Default gravity attribute. Applied after the move, then scaled by the vertical drag. */
    public static final double BOSS_JUMP_GRAVITY = 0.08;
    /** Air vertical drag when the air-drag attribute is zero. */
    public static final double BOSS_JUMP_VERTICAL_DRAG = 0.98;
    /** Air horizontal drag when the air-drag attribute is zero. */
    public static final double BOSS_JUMP_HORIZONTAL_DRAG = 0.91;
    /** Long enough for a 4-block leap to land before fall damage is counted. */
    public static final int BOSS_JUMP_FALL_SAFE_TICKS = 60;
    /** 5 seconds between green rays once a boss is below half health. */
    public static final int BOSS_RAY_INTERVAL_TICKS = 100;
    /** A hit keeps pulling for 5 seconds. */
    public static final int BOSS_RAY_PULL_TICKS = 100;
    /** A blocked ray is shown briefly and does not pull. */
    public static final int BOSS_RAY_MISS_TICKS = 10;
    /** Pull speed. 3 blocks/second is 0.15 blocks/tick. */
    public static final double BOSS_RAY_PULL_BLOCKS_PER_SECOND = 3.0;
    /** The ray only aims at a survival player inside this range. */
    public static final double BOSS_RAY_RANGE = 24.0;
    /** Inside this distance of the boss center, the pull lets the player move. */
    public static final double BOSS_RAY_STOP_DISTANCE = 1.75;
    /** Resend the beam so a player who starts watching mid-pull can see it. */
    public static final int BOSS_RAY_REFRESH_TICKS = 20;
    /** Players within this range are sent the beam. */
    public static final double BOSS_RAY_VIEW_BLOCKS = 64.0;

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

    /** One earlier spawn of a biome-boss family. Distance uses {@code x} and {@code z} only. */
    public record SpawnSite(String dimension, int x, int y, int z) {}

    /**
     * True when a spawn at {@code x,z} in {@code dimension} is {@code separation} blocks or more
     * from every earlier site of that family in the same dimension. Other dimensions are ignored.
     */
    public static boolean clearOfFamilySpawns(
            List<SpawnSite> sites, String dimension, int x, int z, int separation) {
        if (sites == null || dimension == null || dimension.isEmpty()) {
            return true;
        }
        for (SpawnSite site : sites) {
            if (site == null || site.dimension() == null || !dimension.equals(site.dimension())) {
                continue;
            }
            if (!farEnough((double) x - site.x(), (double) z - site.z(), separation)) {
                return false;
            }
        }
        return true;
    }

    /** True when {@code distanceSquared} is within {@link #BAR_RANGE_BLOCKS}. */
    public static boolean withinBarRange(double distanceSquared) {
        if (Double.isNaN(distanceSquared)) {
            return false;
        }
        double range = BAR_RANGE_BLOCKS;
        return distanceSquared <= range * range;
    }

    /**
     * Boss-bar fill, 0 to 1. A non-positive or non-finite max health is an empty bar.
     */
    public static float barProgress(float health, float maxHealth) {
        if (!(maxHealth > 0.0F) || Float.isNaN(health) || Float.isInfinite(health)) {
            return 0.0F;
        }
        float ratio = health / maxHealth;
        if (Float.isNaN(ratio) || ratio <= 0.0F) {
            return 0.0F;
        }
        if (ratio >= 1.0F) {
            return 1.0F;
        }
        return ratio;
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

    /**
     * 1-based boss number. {@code priorDefeats} is how many campaign bosses were already dead
     * when this one spawned, which is what {@code EHM_BOSS_DEFEAT_COUNT} stores.
     */
    public static int livingBossNumber(int priorDefeats) {
        if (priorDefeats >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, priorDefeats + 1);
    }

    /**
     * Destroy speed of a diamond pickaxe, times {@link #MINE_PICKAXE_COUNT}, times the boss number.
     * When the pick is the required tool and the block reports less than one diamond pickaxe,
     * the speed is raised to {@link #DIAMOND_PICKAXE_SPEED} before the multiplier. Dirt and other
     * blocks that do not require a pick keep the reported speed, then take the same multiplier.
     */
    public static float bossBlockMineSpeed(float diamondDestroySpeed, int bossNumber, boolean requiredTool) {
        float speed = Math.max(0.0F, diamondDestroySpeed);
        if (Float.isNaN(speed)) {
            speed = 0.0F;
        }
        if (requiredTool && speed < DIAMOND_PICKAXE_SPEED) {
            speed = DIAMOND_PICKAXE_SPEED;
        }
        int picks = Math.max(0, MINE_PICKAXE_COUNT);
        int number = Math.max(1, bossNumber);
        return speed * picks * number;
    }

    /**
     * Initial vertical velocity whose peak is {@code height}. Matches air travel: the position
     * advances by the current velocity, then {@code vy = (vy - gravity) * verticalDrag}.
     * A boss in water uses fluid travel instead, so it will not reach this peak.
     */
    public static double jumpVelocity(double height) {
        if (!(height > 0.0) || Double.isNaN(height) || Double.isInfinite(height)) {
            return 0.0;
        }
        double lo = 0.0;
        double hi = Math.max(1.0, height);
        while (simulatedJumpPeak(hi) < height && hi < 64.0) {
            hi *= 2.0;
        }
        for (int i = 0; i < 40; i++) {
            double mid = (lo + hi) * 0.5;
            if (simulatedJumpPeak(mid) < height) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return (lo + hi) * 0.5;
    }

    /** Highest point reached from {@code initialVy} using the air-travel step above. */
    public static double simulatedJumpPeak(double initialVy) {
        if (!(initialVy > 0.0) || Double.isNaN(initialVy)) {
            return 0.0;
        }
        double y = 0.0;
        double vy = initialVy;
        double peak = 0.0;
        for (int i = 0; i < 400; i++) {
            y += vy;
            if (y > peak) {
                peak = y;
            } else {
                break;
            }
            vy = (vy - BOSS_JUMP_GRAVITY) * BOSS_JUMP_VERTICAL_DRAG;
        }
        return peak;
    }

    /** Ticks until the leap falls back to the height it left, including the landing tick. */
    public static int jumpAirTicks(double initialVy) {
        if (!(initialVy > 0.0) || Double.isNaN(initialVy)) {
            return 0;
        }
        double y = 0.0;
        double vy = initialVy;
        for (int tick = 1; tick <= 400; tick++) {
            y += vy;
            vy = (vy - BOSS_JUMP_GRAVITY) * BOSS_JUMP_VERTICAL_DRAG;
            if (tick > 1 && y <= 0.0) {
                return tick;
            }
        }
        return 400;
    }

    /**
     * Horizontal speed that covers {@code distance} blocks across {@code airTicks}.
     * Each tick moves by the current speed, then multiplies it by {@link #BOSS_JUMP_HORIZONTAL_DRAG}.
     */
    public static double jumpHorizontalVelocity(double distance, int airTicks) {
        if (!(distance > 0.0) || airTicks <= 0 || Double.isNaN(distance)) {
            return 0.0;
        }
        double drag = BOSS_JUMP_HORIZONTAL_DRAG;
        double series = (1.0 - Math.pow(drag, airTicks)) / (1.0 - drag);
        if (!(series > 1.0E-6)) {
            return 0.0;
        }
        return distance / series;
    }

    /** Leap length toward a target, capped, and shorter by one block so the boss does not land inside them. */
    public static double jumpLeapDistance(double horizontalDistance) {
        if (!(horizontalDistance > 0.0) || Double.isNaN(horizontalDistance)) {
            return 0.0;
        }
        return Math.min(BOSS_JUMP_DISTANCE, Math.max(0.0, horizontalDistance - 1.0));
    }

    /** A passenger never leaps. A hovering boss may leap in the air. Others wait until they are supported. */
    public static boolean canLeap(boolean passenger, boolean groundedOrSwimming, boolean hovering) {
        if (passenger) {
            return false;
        }
        return groundedOrSwimming || hovering;
    }

    /** Strictly below half. Exactly half does not fire. */
    public static boolean belowHalfHealth(float health, float maxHealth) {
        return maxHealth > 0.0F && health < maxHealth * 0.5F;
    }

    public static double pullBlocksPerTick() {
        return BOSS_RAY_PULL_BLOCKS_PER_SECOND / 20.0;
    }

    /**
     * One tick of pull toward the boss center. The result is zeros inside
     * {@link #BOSS_RAY_STOP_DISTANCE} so the caller leaves the player's own velocity alone.
     * Otherwise it is a unit step of {@link #pullBlocksPerTick()} along {@code dx,dy,dz}.
     */
    public static double[] pullStep(double dx, double dy, double dz) {
        double[] none = {0.0, 0.0, 0.0};
        if (Double.isNaN(dx) || Double.isNaN(dy) || Double.isNaN(dz)) {
            return none;
        }
        double distSq = dx * dx + dy * dy + dz * dz;
        double stop = BOSS_RAY_STOP_DISTANCE;
        if (distSq <= stop * stop) {
            return none;
        }
        double dist = Math.sqrt(distSq);
        if (!(dist > 0.0)) {
            return none;
        }
        double step = pullBlocksPerTick();
        return new double[] {dx / dist * step, dy / dist * step, dz / dist * step};
    }

    /** Creatures called when an overworld biome boss dies. One of each kind. */
    public enum FinaleKind {
        PIGLIN,
        ENDERMAN,
        VEX,
        WITCH,
        SKELETON,
        CREEPER,
        ZOMBIE,
        PHANTOM
    }

    /** Horizontal offset from a death site. */
    public record PlaneOffset(double x, double z) {}

    /** One of each finale kind. */
    public static List<FinaleKind> finaleRoster() {
        return List.of(
                FinaleKind.PIGLIN,
                FinaleKind.ENDERMAN,
                FinaleKind.VEX,
                FinaleKind.WITCH,
                FinaleKind.SKELETON,
                FinaleKind.CREEPER,
                FinaleKind.ZOMBIE,
                FinaleKind.PHANTOM);
    }

    /**
     * How many times the group appears. The first boss is one group. Later bosses
     * repeat it, and the fourth and every boss after stay at {@link #FINALE_COUNT_CAP}.
     */
    public static int finaleCountMultiplier(int bossNumber) {
        if (bossNumber <= 1) {
            return 1;
        }
        return Math.min(FINALE_COUNT_CAP, bossNumber);
    }

    /** Health multiplier. The first boss is unchanged. The second doubles it, and it keeps rising. */
    public static int finaleHealthMultiplier(int bossNumber) {
        return Math.max(1, bossNumber);
    }

    /** The base group, repeated for {@link #finaleCountMultiplier(int)}. */
    public static List<FinaleKind> finaleRoster(int bossNumber) {
        List<FinaleKind> base = finaleRoster();
        int times = finaleCountMultiplier(bossNumber);
        List<FinaleKind> roster = new ArrayList<>(base.size() * times);
        for (int copy = 0; copy < times; copy++) {
            roster.addAll(base);
        }
        return List.copyOf(roster);
    }

    /** Zombie, skeleton, and phantom finale mobs do not burn in sunlight. */
    public static boolean finaleIgnoresSun(FinaleKind kind) {
        return kind == FinaleKind.ZOMBIE || kind == FinaleKind.SKELETON || kind == FinaleKind.PHANTOM;
    }

    /**
     * Which in-range player a minion attacks after that list has been shuffled.
     * Minions are dealt around the list, so the group splits across everyone nearby.
     * Negative when nobody is in range.
     */
    public static int finaleTargetSlot(int minionIndex, int playerCount) {
        if (playerCount <= 0) {
            return -1;
        }
        int index = Math.max(0, minionIndex);
        return Math.floorMod(index, playerCount);
    }

    /** Flyers do not need a sturdy block under them. */
    public static boolean finaleWantsFloor(FinaleKind kind) {
        return kind != FinaleKind.VEX && kind != FinaleKind.PHANTOM;
    }

    /**
     * True when a horizontal offset is inside the finale spawn radius, including the edge.
     * A little slack keeps a ring that lands on the radius inside after sine and cosine rounding.
     */
    public static boolean withinFinaleRadius(double dx, double dz) {
        double limit = FINALE_SPAWN_RADIUS * FINALE_SPAWN_RADIUS;
        return dx * dx + dz * dz <= limit + 1.0e-4;
    }

    public static int chebyshev(int dx, int dy, int dz) {
        return Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
    }

    /** True when a block offset is inside the unstuck search, including the boss's own block. */
    public static boolean withinUnstuck(int dx, int dy, int dz) {
        return chebyshev(dx, dy, dz) <= UNSTUCK_RADIUS;
    }

    /**
     * A spot inside {@link #FINALE_SPAWN_RADIUS}. Copies share the circle and sit on
     * rings of 4, 8, 12, and 16 blocks so a large group does not stack on the body.
     */
    public static PlaneOffset finaleOffset(int index, int count) {
        int size = Math.max(1, count);
        int slot = Math.floorMod(index, size);
        double angle = (Math.PI * 2.0 * slot) / size;
        int band = Math.floorMod(slot, 4);
        double radius = FINALE_SPAWN_RADIUS * (band + 1) / 4.0;
        return new PlaneOffset(Math.cos(angle) * radius, Math.sin(angle) * radius);
    }
}
