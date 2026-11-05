package dev.extrahardmode.feature;

import java.util.Map;

/**
 * When a chunk has enough player kills of hostile mobs, ground at and above
 * {@link #MIN_Y} stops receiving natural, bounty, and hunt spawns.
 * Minecraft-free so JUnit can cover the thresholds.
 */
public final class ChunkClearRules {
    public static final int DEFAULT_KILLS = 100;
    /** Spawns at this block Y and above are the ones a full chunk refuses. */
    public static final int MIN_Y = 48;
    public static final int REWARD_XP = 25;
    public static final int REWARD_EMERALDS = 1;
    /** Players inside the chunk, or this close to the kill, hear that it filled. */
    public static final int NEAR_BLOCKS = 48;

    public static final String CLEARED_MESSAGE = "This area is being cleared for the alliance!";
    public static final String PEACE_MESSAGE = "The alliance holds this ground.";

    public static final String CONFIG_COMMENT =
            "Hostile kills by a player in one chunk. At this many, spawns at Y=48 and above stop: natural spawns, bounties, and wise-teacher hunts. Nearby players gain 25 experience and an emerald. Default 100. -1 turns it off. 0 does too.";

    private ChunkClearRules() {}

    /** -1 turns the count, the spawn block, the reward, and the chime off. 0 does too. */
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

    public static boolean blocksSpawn(int y, int kills, int threshold) {
        return y >= MIN_Y && full(kills, threshold);
    }

    /** Same chunk always counts. Otherwise {@code distanceSquared} is blocks from the kill. */
    public static boolean hearsClearing(boolean sameChunk, double distanceSquared) {
        if (sameChunk) {
            return true;
        }
        double reach = (double) NEAR_BLOCKS * NEAR_BLOCKS;
        return distanceSquared <= reach;
    }

    /**
     * Quiet cue once per player per chunk per Minecraft day.
     * {@code heard} maps a dimension-and-chunk key to the overworld day it was heard.
     * A missing chunk is still due. The same chunk is due again on a later day.
     */
    public static boolean peaceDue(String chunkKey, long today, Map<String, Long> heard) {
        if (chunkKey == null || chunkKey.isEmpty()) {
            return false;
        }
        if (heard == null) {
            return true;
        }
        Long heardDay = heard.get(chunkKey);
        return heardDay == null || heardDay.longValue() != today;
    }
}
