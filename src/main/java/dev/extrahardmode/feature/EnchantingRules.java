package dev.extrahardmode.feature;

/**
 * Enchantment-table skill. The requirement is the level on the table button.
 * Minecraft-free so tests can cover the gate without a world.
 */
public final class EnchantingRules {
    public static final int DEFAULT_ABILITY = 3;
    /** A successful enchant this far below the skill, or closer, may raise it. */
    public static final int TRAIN_WINDOW = 3;
    /** Faces on the skill die. A face lower than the current skill does not raise it. */
    public static final int SKILL_DIE = 40;

    private EnchantingRules() {}

    /** An empty slot is 0. Anything above the skill is refused. */
    public static boolean canEnchant(int ability, int requirement) {
        return requirement >= 1 && requirement <= ability;
    }

    /** True when this allowed requirement is close enough to raise the skill. */
    public static boolean trains(int ability, int requirement) {
        return canEnchant(ability, requirement) && ability - requirement <= TRAIN_WINDOW;
    }

    /** One face of a d40. {@code roll} may be any int. Faces run from 1 through {@link #SKILL_DIE}. */
    public static int skillDieFace(int roll) {
        return 1 + Math.floorMod(roll, SKILL_DIE);
    }

    /** True when the face is not lower than the current skill. */
    public static boolean skillRollGrants(int ability, int roll) {
        return skillDieFace(roll) >= ability;
    }

    /**
     * A close requirement raises the skill by 1 when the d40 is not lower than that skill.
     * The skill never passes the int limit.
     */
    public static int nextAbility(int ability, int requirement, int roll) {
        if (!trains(ability, requirement) || ability >= Integer.MAX_VALUE) {
            return ability;
        }
        if (!skillRollGrants(ability, roll)) {
            return ability;
        }
        return ability + 1;
    }
}
