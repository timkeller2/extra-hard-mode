package dev.extrahardmode.item;

/**
 * Heavy swords and axes. Minecraft-free so JUnit can cover the table.
 * Durability is three times the material. Attack damage is the vanilla base plus 2.
 * Attack speed stays the vanilla speed for that weapon.
 */
public final class HeavyWeaponRules {
    public static final int DURABILITY_MULTIPLIER = 3;
    public static final float DAMAGE_BONUS = 2.0F;

    /** Vanilla sword attack component. Every sword uses this base. */
    public static final float SWORD_ATTACK = 3.0F;
    public static final float SWORD_SPEED = -2.4F;

    /** Vanilla axe attack components. Speeds differ by material. */
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

    public static float axeAttack(float vanillaBase) {
        return vanillaBase + DAMAGE_BONUS;
    }
}
