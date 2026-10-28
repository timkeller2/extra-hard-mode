package dev.extrahardmode.feature;

import dev.extrahardmode.player.FoodHistory;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Well Fed from meals past the first seven different foods. Minecraft-free.
 * Mana rises by 1 for every 3 levels. Melee rises by 1 for every 8 levels.
 * Food regen is 1 second faster for every 3 levels, and never faster than every 5 seconds.
 * Saturation drain falls 1% per level, up to 30%.
 */
public final class WellFedRules {
    public static final int BASE_MEALS = HungerRules.DEFAULT_FOOD_HISTORY;
    /** Stored meals. Well Fed cannot grow past this minus {@link #BASE_MEALS}. */
    public static final int HISTORY_CAP = 64;
    public static final int BASE_FOOD = 20;
    public static final int FOOD_CAP = BASE_FOOD + (HISTORY_CAP - BASE_MEALS);
    /** Food regen never runs more often than once every 5 seconds. */
    public static final int MIN_REGEN_TICKS = 5 * HungerRules.TICKS_PER_SECOND;
    /** One mana level for every this many Well Fed levels, rounded down. */
    public static final int MANA_LEVELS_PER_BONUS = 3;
    /** One melee damage for every this many Well Fed levels, rounded down. */
    public static final int MELEE_LEVELS_PER_BONUS = 8;
    /** One second off food regen for every this many Well Fed levels, rounded down. */
    public static final int REGEN_LEVELS_PER_SECOND = 3;
    /** Saturation exhaustion falls by this fraction of vanilla per Well Fed level. */
    public static final float SATURATION_PER_LEVEL = 0.01F;
    /** Saturation exhaustion never falls by more than this fraction of vanilla. */
    public static final float SATURATION_CAP = 0.30F;

    private WellFedRules() {}

    /** Meals that must all be different to reach this Well Fed level. */
    public static int gainWindow(int level) {
        return BASE_MEALS + Math.max(0, level);
    }

    /**
     * Meals inspected while holding a level: the gain window plus one extra meal per level,
     * which is the duplicate allowance.
     */
    public static int holdWindow(int level) {
        int safe = Math.max(0, level);
        if (safe == 0) {
            return BASE_MEALS;
        }
        return gainWindow(safe) + safe;
    }

    public static int nextLevel(List<String> recent, int previous) {
        int level = Math.max(0, previous);
        int cap = HISTORY_CAP - BASE_MEALS;
        while (level > 0 && !holds(recent, level)) {
            level--;
        }
        while (level < cap && FoodHistory.lastAreAllDifferent(recent, gainWindow(level) + 1)) {
            level++;
        }
        return level;
    }

    /** True when the recent meals still contain no more duplicates than this level allows. */
    public static boolean holds(List<String> recent, int level) {
        if (level <= 0) {
            return true;
        }
        int need = gainWindow(level);
        if (recent == null || recent.size() < need) {
            return false;
        }
        int window = Math.min(holdWindow(level), recent.size());
        return window - distinctInLast(recent, window) <= level;
    }

    public static int distinctInLast(List<String> recent, int window) {
        if (recent == null || recent.isEmpty() || window <= 0) {
            return 0;
        }
        int start = Math.max(0, recent.size() - window);
        Set<String> ids = new HashSet<>();
        for (int i = start; i < recent.size(); i++) {
            String id = recent.get(i);
            if (id != null && !id.isEmpty()) {
                ids.add(id);
            }
        }
        return ids.size();
    }

    /** +1 health point per Well Fed level, with no cap. */
    public static int healthBonus(int wellFed) {
        return Math.max(0, wellFed);
    }

    public static int foodMax(int wellFed) {
        return BASE_FOOD + Math.max(0, wellFed);
    }

    /** +1 attack damage at Well Fed 8, 16, 24, and so on. */
    public static int meleeBonus(int wellFed) {
        return Math.max(0, wellFed) / MELEE_LEVELS_PER_BONUS;
    }

    /** +1 mana level at Well Fed 3, 6, 9, and so on. */
    public static int manaBonus(int wellFed) {
        return Math.max(0, wellFed) / MANA_LEVELS_PER_BONUS;
    }

    public static float saturationMultiplier(int wellFed) {
        float reduction = Math.min(SATURATION_CAP, SATURATION_PER_LEVEL * Math.max(0, wellFed));
        return 1.0F - reduction;
    }

    public static int regenTicks(int configuredTicks, int wellFed) {
        int faster = Math.max(0, wellFed) / REGEN_LEVELS_PER_SECOND;
        int ticks = Math.max(1, configuredTicks) - faster * HungerRules.TICKS_PER_SECOND;
        if (faster <= 0) {
            return Math.max(1, configuredTicks);
        }
        return Math.max(MIN_REGEN_TICKS, ticks);
    }

    public static int penaltyWindow(int wellFed) {
        return holdWindow(wellFed);
    }

    /** Same thresholds as the unfed rule, shifted by one meal per Well Fed level. */
    public static int repeatPenalty(int countInWindow, int wellFed) {
        int level = Math.max(0, wellFed);
        if (countInWindow >= HungerRules.SICK_STREAK + level) {
            return 2;
        }
        if (countInWindow >= HungerRules.TIRED_STREAK + level) {
            return 1;
        }
        return 0;
    }
}
