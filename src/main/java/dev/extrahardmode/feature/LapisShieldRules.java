package dev.extrahardmode.feature;

/**
 * Lapis Shield absorb math. Minecraft-free so the cost can be tested without a world.
 * One lapis lazuli pays for three points blocked. A leftover fraction costs one more.
 */
public final class LapisShieldRules {
    /** Float noise below this does not count as a blocked point. */
    public static final float COST_EPSILON = 0.0001F;
    /** Damage one lapis lazuli pays for. A remainder rounds up to another lapis. */
    public static final int DAMAGE_PER_LAPIS = 3;

    private LapisShieldRules() {}

    /** Damage removed before armor and health. Never more than the hit or the ability level. */
    public static float absorb(float damage, float level) {
        if (!(damage > 0.0F) || !(level > 0.0F)) {
            return 0.0F;
        }
        return Math.min(damage, level);
    }

    /**
     * Lapis lazuli spent for {@code absorbed} damage.
     * Exactly 3 costs 1. 3.1 costs 2. Nothing blocked costs 0.
     */
    public static int lapisCost(float absorbed) {
        if (!(absorbed > COST_EPSILON)) {
            return 0;
        }
        return (int) Math.ceil((absorbed - COST_EPSILON) / DAMAGE_PER_LAPIS);
    }

    /** Hit left after a successful absorb. A zero absorb leaves the hit unchanged. */
    public static float damageAfterAbsorb(float damage, float absorbed) {
        if (!(absorbed > 0.0F)) {
            return damage;
        }
        float left = damage - absorbed;
        return left > 0.0F ? left : 0.0F;
    }
}
