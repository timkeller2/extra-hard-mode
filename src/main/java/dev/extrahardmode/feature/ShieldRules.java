package dev.extrahardmode.feature;

/**
 * Shield blocks absorb a fraction of incoming damage and wear faster.
 * Minecraft-free so JUnit can cover the mapping.
 */
public final class ShieldRules {
    public static final float ABSORB_FRACTION = 0.75F;
    public static final int DURABILITY_MULTIPLIER = 2;

    private ShieldRules() {}

    /** Damage the shield actually soaks, from the vanilla blocked amount. */
    public static float absorbed(float blocked) {
        if (blocked <= 0.0F) {
            return 0.0F;
        }
        return blocked * ABSORB_FRACTION;
    }

    /** Durability points after Tougher, from the vanilla item-damage result. */
    public static int durabilityHit(int vanilla) {
        if (vanilla <= 0) {
            return 0;
        }
        return vanilla * DURABILITY_MULTIPLIER;
    }
}
