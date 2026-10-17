package dev.extrahardmode.feature;

/**
 * How bright a visited cave may still spawn an extra hostile, and how often.
 * Minecraft-free so JUnit can cover the depth steps and the light penalty.
 *
 * <p>The base light is the usual cap at the cave band. Every full 20 blocks the
 * player is below that band adds 1. The attempt's chance then falls by 5
 * percentage points for each light level of the chosen block.
 */
public final class SpawnInLightRules {
    public static final int DEPTH_STEP = 20;
    public static final int CHANCE_LOSS_PER_LIGHT = 5;

    private SpawnInLightRules() {}

    /** {@code y} is the player's block Y. At the band, and above it, the result is {@code baseLight}. */
    public static int allowedLight(int baseLight, int bandY, int y) {
        int deeper = bandY - y;
        if (deeper <= 0) {
            return baseLight;
        }
        return baseLight + deeper / DEPTH_STEP;
    }

    /** Chance from 0 to the base percent. {@code light} is the chosen block. */
    public static int spawnChancePercent(int basePercent, int light) {
        int reduced = basePercent - CHANCE_LOSS_PER_LIGHT * Math.max(0, light);
        return Math.max(0, reduced);
    }

    /** {@code roll} is 0–99 from {@code nextInt(100)}. */
    public static boolean allow(int chancePercent, int roll) {
        if (chancePercent <= 0) {
            return false;
        }
        if (chancePercent >= 100) {
            return true;
        }
        return roll < chancePercent;
    }
}
