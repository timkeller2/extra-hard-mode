package dev.extrahardmode.feature;

/**
 * Diamond Skin damage math. Minecraft-free except the poison-tick marker, which is an identity token.
 */
public final class DiamondSkinRules {
    /** Each point of health damage prevented removes this many seconds. */
    public static final int SECONDS_LOST_PER_DAMAGE = 3;
    public static final int TICKS_PER_SECOND = 20;

    private static final ThreadLocal<Object> POISON_VICTIM = new ThreadLocal<>();

    private DiamondSkinRules() {}

    /** True while {@code victim} is inside the poison effect's own hurt call. */
    public static void beginPoison(Object victim) {
        POISON_VICTIM.set(victim);
    }

    public static void endPoison(Object victim) {
        if (POISON_VICTIM.get() == victim) {
            POISON_VICTIM.remove();
        }
    }

    public static boolean isPoisonVictim(Object victim) {
        return victim != null && POISON_VICTIM.get() == victim;
    }

    /**
     * Health-bar damage left after Diamond Skin. {@code power} is the effective ability level.
     * The hit is divided by that level. Level 1 leaves it unchanged. Below 1 never increases it.
     * Poison, drowning, and suffocation pass {@code bypass}.
     */
    public static float scaleHealthDamage(float healthDamage, double power, boolean bypass) {
        if (bypass || healthDamage <= 0.0F || !(power > 1.0)) {
            return healthDamage;
        }
        return (float) (healthDamage / power);
    }

    public static boolean reduced(float before, float after) {
        return after < before - 0.0001F;
    }

    /** Ticks removed for health damage Diamond Skin kept off the player. */
    public static int durationLossTicks(float absorbedDamage) {
        if (absorbedDamage <= 0.0F) {
            return 0;
        }
        return Math.max(0, Math.round(absorbedDamage * SECONDS_LOST_PER_DAMAGE * TICKS_PER_SECOND));
    }

    public static int remainingAfterAbsorption(int remainingTicks, float absorbedDamage) {
        return Math.max(0, remainingTicks - durationLossTicks(absorbedDamage));
    }
}
