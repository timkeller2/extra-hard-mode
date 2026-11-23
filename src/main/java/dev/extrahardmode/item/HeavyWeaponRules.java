package dev.extrahardmode.item;

/**
 * Heavy swords and axes. Minecraft-free so JUnit can cover the table.
 * Durability is three times the material. Attack damage is the vanilla base plus 2.
 * Heavy swords and axes swing 10% slower than the vanilla weapon.
 */
public final class HeavyWeaponRules {
    public static final int DURABILITY_MULTIPLIER = 3;
    public static final float DAMAGE_BONUS = 2.0F;

    /** Player base attack speed. An item's speed modifier is added to this. */
    public static final float BASE_ATTACK_SPEED = 4.0F;
    /** Fraction a heavy sword or axe swings slower than that vanilla weapon. */
    public static final float ATTACK_SLOWDOWN = 0.10F;

    /** Vanilla sword attack component. Every sword uses this base. */
    public static final float SWORD_ATTACK = 3.0F;
    /** Vanilla sword attack-speed modifier. Heavy swords use {@link #swordSpeed()}. */
    public static final float SWORD_SPEED = -2.4F;

    /** Vanilla axe attack components. Speeds differ by material. Heavy axes use {@link #axeSpeed(float)}. */
    public static final float COPPER_AXE_ATTACK = 7.0F;
    public static final float COPPER_AXE_SPEED = -3.2F;
    public static final float IRON_AXE_ATTACK = 6.0F;
    public static final float IRON_AXE_SPEED = -3.1F;
    public static final float GOLD_AXE_ATTACK = 6.0F;
    public static final float GOLD_AXE_SPEED = -3.0F;
    public static final float DIAMOND_AXE_ATTACK = 5.0F;
    public static final float DIAMOND_AXE_SPEED = -3.0F;
    public static final float NETHERITE_AXE_ATTACK = 5.0F;
    public static final float NETHERITE_AXE_SPEED = -3.0F;

    /** Vanilla tool-material durability, before the heavy multiplier. */
    public static final int COPPER_DURABILITY = 190;
    public static final int IRON_DURABILITY = 250;
    public static final int GOLD_DURABILITY = 32;
    public static final int DIAMOND_DURABILITY = 1561;
    public static final int NETHERITE_DURABILITY = 2031;

    private HeavyWeaponRules() {}

    public static int durability(int vanilla) {
        return vanilla * DURABILITY_MULTIPLIER;
    }

    public static float swordAttack() {
        return SWORD_ATTACK + DAMAGE_BONUS;
    }

    /** Attacks per second of a vanilla sword. */
    public static float vanillaSwordAttacksPerSecond() {
        return BASE_ATTACK_SPEED + SWORD_SPEED;
    }

    /** Attacks per second of a heavy sword, 10% under a vanilla sword. */
    public static float swordAttacksPerSecond() {
        return vanillaSwordAttacksPerSecond() * (1.0F - ATTACK_SLOWDOWN);
    }

    /** Attack-speed attribute modifier for a heavy sword. */
    public static float swordSpeed() {
        return swordAttacksPerSecond() - BASE_ATTACK_SPEED;
    }

    public static float axeAttack(float vanillaBase) {
        return vanillaBase + DAMAGE_BONUS;
    }

    /** Attacks per second of a vanilla axe with that speed modifier. */
    public static float vanillaAxeAttacksPerSecond(float vanillaSpeed) {
        return BASE_ATTACK_SPEED + vanillaSpeed;
    }

    /** Attacks per second of a heavy axe, 10% under that vanilla axe. */
    public static float axeAttacksPerSecond(float vanillaSpeed) {
        return vanillaAxeAttacksPerSecond(vanillaSpeed) * (1.0F - ATTACK_SLOWDOWN);
    }

    /** Attack-speed attribute modifier for a heavy axe. */
    public static float axeSpeed(float vanillaSpeed) {
        return axeAttacksPerSecond(vanillaSpeed) - BASE_ATTACK_SPEED;
    }
}
