package dev.extrahardmode.feature;

/**
 * Hopper-moved furnace cooking XP. Minecraft-free for JUnit.
 */
public final class FurnaceXpRules {
    public static final int MILLI_PER_XP = 1000;
    public static final int FURNACE_RESULT_SLOT = 2;
    /** Extra cooking XP when a hopper pulls from a furnace. */
    public static final int AUTOMATION_BONUS_PERCENT = 50;
    /** Same as smelting an iron ingot; hopper bone meal uses this plus the automation bonus. */
    public static final float COMPOSTER_EXPERIENCE = 0.7F;

    private FurnaceXpRules() {}

    public static int toMilli(float experience) {
        if (experience <= 0.0F) {
            return 0;
        }
        return Math.round(experience * MILLI_PER_XP);
    }

    public static int withAutomationBonus(int milli) {
        if (milli <= 0) {
            return 0;
        }
        return milli + (milli * AUTOMATION_BONUS_PERCENT) / 100;
    }

    /**
     * Move this many millipoints of a pool with the items that just left.
     * If every item leaves, the whole pool follows.
     */
    public static int transferMilli(int pool, int moved, int sourceCountBefore) {
        if (pool <= 0 || moved <= 0 || sourceCountBefore <= 0) {
            return 0;
        }
        if (moved >= sourceCountBefore) {
            return pool;
        }
        return (int) ((long) pool * (long) moved / (long) sourceCountBefore);
    }

    /** Vanilla {@code createExperience} using a 0–1 roll instead of RandomSource. */
    public static int orbsFromMilli(int milli, float random) {
        if (milli <= 0) {
            return 0;
        }
        float total = milli / (float) MILLI_PER_XP;
        int orbs = (int) Math.floor(total);
        float frac = total - orbs;
        if (frac > 0.0F && random < frac) {
            orbs++;
        }
        return orbs;
    }
}
