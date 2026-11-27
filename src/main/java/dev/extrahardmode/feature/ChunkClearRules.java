package dev.extrahardmode.feature;

/**
 * When a chunk has enough player kills of hostile mobs, ground at and above
 * {@link #MIN_Y} stops receiving hostile spawns. Neutral and friendly mobs still spawn.
 * Reaching that count again clears one uncleared chunk around it, edges then corners.
 * That chunk plus the eight around it is nine.
 * Minecraft-free so JUnit can cover the thresholds.
 */
public final class ChunkClearRules {
    public static final int DEFAULT_KILLS = 20;
    /** Hostile spawns at this block Y and above are the ones a full chunk refuses. */
    public static final int MIN_Y = 48;
    public static final int REWARD_XP = 25;
    public static final int REWARD_EMERALDS = 1;
    /** Players inside the chunk, or this close to the kill, hear that it filled. */
    public static final int NEAR_BLOCKS = 48;

    public static final String CLEARED_MESSAGE = "More area is being cleared by the alliance!";
    /** Shown when monsters.chunkClearGoal is off. The number is the total cleared areas. */
    public static final String COUNT_MESSAGE = "More area is being cleared by the alliance! %s areas have been cleared.";
    /** Shown when a win goal is set. The numbers are cleared areas, then the goal. */
    public static final String GOAL_MESSAGE = "%s out of %s areas have been cleared for the alliance!";
    public static final String WON_MESSAGE = "You Won!";
    public static final String WON_SUBTITLE = "You may keep playing.";
    /** Cleared areas that win. 0 is off. */
    public static final int DEFAULT_GOAL = 0;
    /** Ticks after You Won! before the credits scroll. Five seconds. */
    public static final int CREDITS_DELAY_TICKS = 100;
    public static final int VICTORY_FIREWORKS = 4;
    public static final int TITLE_FADE_IN = 10;
    public static final int TITLE_STAY = 60;
    public static final int TITLE_FADE_OUT = 20;
    public static final String GOAL_CONFIG_COMMENT =
            "Cleared areas that win the game. 0 is off. The overworld file is the one that counts. Every dimension's cleared chunks add to the total. The clear that reaches the goal says You Won!, plays fireworks, and rolls the credits a few seconds later. Players keep playing. /tougher areas sets this.";
    /** How long the shield keeps rising, and how far it climbs. */
    public static final int RISE_TICKS = 50;
    public static final double RISE_BLOCKS = 12.0;
    /** Players this far from the rise still see the shield. */
    public static final int SEE_BLOCKS = 80;
    /** Tooltip on the corner diamond while the player stands in a cleared chunk. */
    public static final String PEACE_MESSAGE = "The alliance holds this ground.";
    /**
     * North, east, south, west, then northeast, northwest, southeast, southwest.
     * The first uncleared one is the next spread. The source chunk plus these eight is nine.
     */
    private static final int[] NEIGHBOR_DX = {0, 1, 0, -1, 1, -1, 1, -1};
    private static final int[] NEIGHBOR_DZ = {-1, 0, 1, 0, -1, -1, 1, 1};

    public static final String CONFIG_COMMENT =
            "Hostile kills by a player in one chunk. At this many, hostile spawns at Y=48 and above stop. Each time that many more kills land, one uncleared chunk around it is cleared: north, east, south, west, then northeast, northwest, southeast, and southwest. That chunk can clear nine in all. Neutral and friendly mobs still spawn. Nearby players gain 25 experience and an emerald. Default 20. -1 turns it off. 0 does too.";

    private ChunkClearRules() {}

    /** -1 turns the count, the spawn block, the spread, the reward, and the diamond off. 0 does too. */
    public static boolean enabled(int threshold) {
        return threshold > 0;
    }

    public static boolean full(int kills, int threshold) {
        return enabled(threshold) && kills >= threshold;
    }

    /** True when the kill that moves the count from {@code killsBefore} reaches the threshold. */
    public static boolean justFilled(int killsBefore, int threshold) {
        return enabled(threshold) && killsBefore < threshold && killsBefore + 1 >= threshold;
    }

    /**
     * True when this kill reaches the threshold again: twice, three times, and so on.
     * The first time is {@link #justFilled}, not a spread.
     */
    public static boolean spreads(int killsBefore, int threshold) {
        if (!enabled(threshold) || killsBefore < threshold) {
            return false;
        }
        int after = killsBefore + 1;
        return after > threshold && after % threshold == 0;
    }

    public static int neighborCount() {
        return NEIGHBOR_DX.length;
    }

    public static int neighborChunkX(int chunkX, int index) {
        return chunkX + NEIGHBOR_DX[index];
    }

    public static int neighborChunkZ(int chunkZ, int index) {
        return chunkZ + NEIGHBOR_DZ[index];
    }

    /**
     * First neighbor that is not already full. Order is north, east, south, west, then
     * northeast, northwest, southeast, southwest. -1 when all eight are full.
     */
    public static int firstUncleared(boolean[] neighborFull) {
        if (neighborFull == null) {
            return -1;
        }
        int count = Math.min(neighborFull.length, NEIGHBOR_DX.length);
        for (int i = 0; i < count; i++) {
            if (!neighborFull[i]) {
                return i;
            }
        }
        return -1;
    }

    public static boolean blocksSpawn(int y, int kills, int threshold) {
        return y >= MIN_Y && full(kills, threshold);
    }

    /** A full chunk refuses this spawn only when the mob is hostile. */
    public static boolean blocksHostileSpawn(int y, int kills, int threshold, boolean hostile) {
        return hostile && blocksSpawn(y, kills, threshold);
    }

    /** Same chunk always counts. Otherwise {@code distanceSquared} is blocks from the kill. */
    public static boolean hearsClearing(boolean sameChunk, double distanceSquared) {
        if (sameChunk) {
            return true;
        }
        double reach = (double) NEAR_BLOCKS * NEAR_BLOCKS;
        return distanceSquared <= reach;
    }

    /** Spread reward. {@code distanceSquared} is blocks from the killing player. */
    public static boolean hearsKiller(double distanceSquared) {
        if (distanceSquared < 0.0) {
            return false;
        }
        double reach = (double) NEAR_BLOCKS * NEAR_BLOCKS;
        return distanceSquared <= reach;
    }

    /**
     * The player stands in the chunk that just became clear, or in one of the eight around it.
     */
    public static boolean witnessesClear(int playerChunkX, int playerChunkZ, int clearedChunkX, int clearedChunkZ) {
        return Math.abs(playerChunkX - clearedChunkX) <= 1 && Math.abs(playerChunkZ - clearedChunkZ) <= 1;
    }

    /** 0 at the start and 1 once {@link #RISE_TICKS} have passed. Smooth in between. */
    public static float riseProgress(float ageTicks) {
        if (ageTicks <= 0.0F || RISE_TICKS <= 0) {
            return 0.0F;
        }
        float t = ageTicks / RISE_TICKS;
        if (t >= 1.0F) {
            return 1.0F;
        }
        return t * t * (3.0F - 2.0F * t);
    }

    public static double riseHeight(float ageTicks) {
        return RISE_BLOCKS * riseProgress(ageTicks);
    }

    public static boolean victoryEnabled(int goal) {
        return goal > 0;
    }

    /** True when this clear is the one that first reaches the goal. */
    public static boolean crossesGoal(int clearedBefore, int cleared, int goal) {
        return victoryEnabled(goal) && clearedBefore < goal && cleared >= goal;
    }

    public static String countMessage(int cleared) {
        return CLEARED_MESSAGE + " " + Math.max(0, cleared) + " areas have been cleared.";
    }

    public static String goalMessage(int cleared, int goal) {
        return Math.max(0, cleared) + " out of " + Math.max(0, goal) + " areas have been cleared for the alliance!";
    }

    public static String progress(int cleared, int goal) {
        if (victoryEnabled(goal)) {
            return goalMessage(cleared, goal);
        }
        return countMessage(cleared);
    }
}
