package dev.extrahardmode.feature;

/** Resident health, hearts, and when a zombie may convert one. Minecraft-free. */
public final class ResidentCombatRules {
    public static final int BASE_HEALTH = 40;
    /** One real minute. */
    public static final int REGEN_TICKS = 1200;
    public static final float ZOMBIE_BELOW = 5.0F;
    public static final float BOW_BONUS = 3.0F;
    public static final float SWORD_DAMAGE = 6.0F;
    public static final double SHOOT_RANGE = 12.0;
    public static final double MELEE_RANGE = 2.8;
    public static final double CHASE_RANGE = 8.0;
    public static final int SHOOT_COOLDOWN = 40;
    public static final int MELEE_COOLDOWN = 20;
    /** How long a close hit keeps them on the sword. */
    public static final int MELEE_WINDOW = 200;

    private ResidentCombatRules() {}

    public static float maxHealth(int housePoints) {
        return BASE_HEALTH + Math.max(0, housePoints);
    }

    /** One heart for every 10 health, and at least one while they are alive. */
    public static int heartIcons(float health) {
        if (health <= 0.0F) {
            return 0;
        }
        return Math.max(1, (int) (health / 10.0F));
    }

    /** A zombie may convert them only after they are already below 5 health. */
    public static boolean canZombify(float healthBeforeHit) {
        return healthBeforeHit < ZOMBIE_BELOW;
    }
}
