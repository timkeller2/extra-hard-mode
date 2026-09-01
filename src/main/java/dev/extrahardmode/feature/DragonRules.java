package dev.extrahardmode.feature;

/**
 * Minecraft-free Glydia rules. Defaults match DESIGN.md (auto-respawn off, health 800).
 */
public final class DragonRules {
    public static final int DEFAULT_HEALTH = 800;
    public static final int VANILLA_HEALTH = 200;
    public static final int DEFAULT_HEAL_PERCENT = 25;
    public static final float FIREBALL_POWER = 2.0F;
    public static final String END_CRYSTAL = "minecraft:end_crystal";
    public static final String ENDER_CHEST = "minecraft:ender_chest";
    public static final String CHORUS_FLOWER = "minecraft:chorus_flower";

    private DragonRules() {}

    /** Vanilla crystals own respawn unless this is explicitly on. */
    public static boolean shouldAutoRespawn(
            boolean autoRespawn, boolean dragonKilled, boolean ritualInProgress, boolean dragonAlive) {
        return autoRespawn && dragonKilled && !ritualInProgress && !dragonAlive;
    }

    public static float healAmount(float maxHealth, int percent) {
        if (maxHealth <= 0.0F || percent <= 0) {
            return 0.0F;
        }
        return maxHealth * (percent / 100.0F);
    }

    public static boolean shouldFillHealth(double previousMax, float currentHealth, double newMax) {
        if (newMax <= 0.0) {
            return false;
        }
        if (previousMax == newMax) {
            return false;
        }
        return currentHealth >= previousMax - 0.01F;
    }

    public static boolean allowEndPlace(String itemId) {
        return END_CRYSTAL.equals(itemId) || ENDER_CHEST.equals(itemId) || CHORUS_FLOWER.equals(itemId);
    }

    /**
     * End no-build. Empty hand and allowlisted items pass so crystals/chests still work.
     * {@code placementItem} includes BlockItem, buckets/fluids, flint and steel, boats, eggs, etc.
     */
    public static boolean denyEndUse(
            boolean noBuilding,
            boolean inEnd,
            boolean bypass,
            boolean emptyHand,
            boolean allowlisted,
            boolean placementItem) {
        if (!noBuilding || !inEnd || bypass) {
            return false;
        }
        if (emptyHand || allowlisted) {
            return false;
        }
        return placementItem;
    }

    public enum MinionRoll {
        BLAZE,
        ZOMBIE_VILLAGERS,
        SKELETONS,
        ENDERMAN,
        NONE
    }

    /**
     * Default table uses {@code random(100)}. Alternative uses {@code random(150)} and may roll nothing.
     */
    public static MinionRoll roll(int random, boolean alternative) {
        if (alternative) {
            if (random < 0 || random >= 100) {
                return MinionRoll.NONE;
            }
            if (random < 10) {
                return MinionRoll.BLAZE;
            }
            if (random < 50) {
                return MinionRoll.ZOMBIE_VILLAGERS;
            }
            if (random < 80) {
                return MinionRoll.SKELETONS;
            }
            return MinionRoll.ENDERMAN;
        }
        if (random < 0 || random >= 100) {
            return MinionRoll.NONE;
        }
        if (random < 40) {
            return MinionRoll.BLAZE;
        }
        if (random < 70) {
            return MinionRoll.ZOMBIE_VILLAGERS;
        }
        return MinionRoll.ENDERMAN;
    }
}
