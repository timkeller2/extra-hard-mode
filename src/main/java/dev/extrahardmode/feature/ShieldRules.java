package dev.extrahardmode.feature;

/**
 * Shield blocks absorb a fraction of incoming damage and wear faster.
 * Minecraft-free so JUnit can cover the mapping.
 */
public final class ShieldRules {
    /** Percent of vanilla blocked damage the shield actually soaks. 100 blocks it all. */
    public static final int DEFAULT_ABSORB_PERCENT = 100;
    /** Vanilla shield durability loss is multiplied by this. */
    public static final int DEFAULT_DURABILITY_MULTIPLIER = 3;
    public static final int MAX_DURABILITY_MULTIPLIER = 100;

    private ShieldRules() {}

    public static int clampAbsorbPercent(int percent) {
        return Math.max(0, Math.min(100, percent));
    }

    public static int clampDurabilityMultiplier(int multiplier) {
        return Math.max(0, Math.min(MAX_DURABILITY_MULTIPLIER, multiplier));
    }

    /** Damage the shield actually soaks, from the vanilla blocked amount. */
    public static float absorbed(float blocked, int absorbPercent) {
        if (blocked <= 0.0F) {
            return 0.0F;
        }
        return blocked * (clampAbsorbPercent(absorbPercent) / 100.0F);
    }

    /** Durability points after Tougher, from the vanilla item-damage result. */
    public static int durabilityHit(int vanilla, int multiplier) {
        if (vanilla <= 0) {
            return 0;
        }
        long hit = (long) vanilla * clampDurabilityMultiplier(multiplier);
        if (hit > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) hit;
    }
}
