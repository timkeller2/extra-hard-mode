package dev.extrahardmode.feature;

/**
 * Heal up to the entity's current max health, which can exceed the vanilla 20.
 * Minecraft-free so JUnit can cover the clamp.
 */
public final class HealthRules {
    public static final float VANILLA_MAX = 20.0F;

    private HealthRules() {}

    public static boolean canHeal(float health, float maxHealth) {
        return health > 0.0F && health < maxHealth;
    }

    /** Current health after applying {@code amount}, never above {@code maxHealth}. */
    public static float afterHeal(float health, float amount, float maxHealth) {
        if (health <= 0.0F) {
            return health;
        }
        float next = health + Math.max(0.0F, amount);
        float max = Math.max(0.0F, maxHealth);
        return Math.min(next, max);
    }

    /**
     * Re-apply NBT health after equipment has raised max health. Vanilla
     * {@code setHealth} clamps to the pre-armor max of 20 while player data loads.
     */
    public static float restoreLoadedHealth(float current, float loaded, float maxHealth) {
        float max = Math.max(0.0F, maxHealth);
        float restored = Math.min(Math.max(0.0F, loaded), max);
        return Math.max(Math.max(0.0F, current), restored);
    }

    public static boolean stillWaitingForMaxHealth(float loaded, float maxHealth) {
        return loaded > maxHealth;
    }
}
