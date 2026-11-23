package dev.extrahardmode.feature;

/**
 * Each player may take loot from a monster spawner a few times.
 * Later kills from that same spawner drop nothing.
 */
public final class SpawnerLootRules {
    /** Player kills from one spawner that still drop loot and experience. */
    public static final int DEFAULT_LOOT_KILLS = 8;

    public static final String CONFIG_COMMENT =
            "Player kills from one monster spawner that still drop loot and experience. Each player has their own count for each spawner. Default 8. 0 drops nothing.";

    /** Not from a tracked spawner. */
    public static final int GATE_NONE = 0;
    /** This death drops nothing. A player kill at the cap, or a kill with no player. */
    public static final int GATE_BLOCK = 1;
    /** This player kill still drops, and the count has not been taken yet. */
    public static final int GATE_ALLOW = 2;
    /** This death was allowed and the player's count already includes it. */
    public static final int GATE_COUNTED = 3;

    private SpawnerLootRules() {}

    /** {@code killsAlready} is the number recorded before this kill. {@code allowance} is the configured cap. */
    public static boolean dropsLoot(int killsAlready, int allowance) {
        return killsAlready < Math.max(0, allowance);
    }

    /**
     * An unnatural floor blocks loot for a mob that spawned on its own.
     * Trial spawners and monster spawners are not that gate.
     */
    public static boolean unnaturalFloorBlocksLoot(boolean trialSpawned, boolean spawnerSpawned) {
        return !trialSpawned && !spawnerSpawned;
    }

    public static String blockKey(String dimensionId, long packedPos) {
        return "block|" + (dimensionId == null ? "" : dimensionId) + "|" + packedPos;
    }

    public static String minecartKey(String uuid) {
        return "cart|" + (uuid == null ? "" : uuid);
    }
}
