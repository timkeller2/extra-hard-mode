package dev.extrahardmode.feature;

/**
 * Enchantment-table skill. The requirement is the level on the table button.
 * Minecraft-free so tests can cover the gate without a world.
 */
public final class EnchantingRules {
    public static final int DEFAULT_ABILITY = 3;
    /** A successful enchant this far below the skill, or closer, raises it. */
    public static final int TRAIN_WINDOW = 3;

    private EnchantingRules() {}

    /** An empty slot is 0. Anything above the skill is refused. */
    public static boolean canEnchant(int ability, int requirement) {
        return requirement >= 1 && requirement <= ability;
    }

    /** True when this allowed requirement is close enough to raise the skill. */
    public static boolean trains(int ability, int requirement) {
        return canEnchant(ability, requirement) && ability - requirement <= TRAIN_WINDOW;
    }

    /** One successful table click raises the skill by 1, and never past the int limit. */
    public static int nextAbility(int ability, int requirement) {
        if (!trains(ability, requirement) || ability >= Integer.MAX_VALUE) {
            return ability;
        }
        return ability + 1;
    }
}
