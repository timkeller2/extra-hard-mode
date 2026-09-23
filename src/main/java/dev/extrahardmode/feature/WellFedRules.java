package dev.extrahardmode.feature;

import dev.extrahardmode.player.FoodHistory;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Well Fed from meals past the first seven different foods. Minecraft-free.
 * Levels above {@link #BENEFIT_CAP} still raise hunger, mana, and the grace windows.
 */
public final class WellFedRules {
    public static final int BASE_MEALS = HungerRules.DEFAULT_FOOD_HISTORY;
    public static final int BENEFIT_CAP = 5;
    /** Stored meals. Well Fed cannot grow past this minus {@link #BASE_MEALS}. */
    public static final int HISTORY_CAP = 64;
    public static final int BASE_FOOD = 20;
    public static final int FOOD_CAP = BASE_FOOD + (HISTORY_CAP - BASE_MEALS);
    public static final int MIN_REGEN_TICKS = 10 * HungerRules.TICKS_PER_SECOND;

    private WellFedRules() {}

    public static int benefitLevel(int wellFed) {
        return Math.clamp(wellFed, 0, BENEFIT_CAP);
    }

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

    /** +1 health point per level, stopping at {@link #BENEFIT_CAP}. */
    public static int healthBonus(int wellFed) {
        return benefitLevel(wellFed);
    }

    public static int foodMax(int wellFed) {
        return BASE_FOOD + Math.max(0, wellFed);
    }

    /** +1 attack damage at levels 2 and 4, then no further. */
    public static int meleeBonus(int wellFed) {
        return benefitLevel(wellFed) / 2;
    }

    public static float saturationMultiplier(int wellFed) {
        return 1.0F - 0.02F * benefitLevel(wellFed);
    }

    public static int regenTicks(int configuredTicks, int wellFed) {
        int faster = benefitLevel(wellFed) / 2;
        int ticks = Math.max(1, configuredTicks) - faster * HungerRules.TICKS_PER_SECOND;
        if (faster <= 0) {
            return Math.max(1, configuredTicks);
        }
        return Math.max(MIN_REGEN_TICKS, ticks);
    }

    public static boolean luck(int wellFed) {
        return benefitLevel(wellFed) >= 3;
    }

    /** Poison and hunger effects. 1 at Well Fed 0, 0.5 at Well Fed 5. */
    public static float effectDurationScale(int wellFed) {
        return 1.0F - 0.1F * benefitLevel(wellFed);
    }

    public static int scaleDuration(int duration, int wellFed) {
        if (duration <= 0) {
            return duration;
        }
        return Math.max(1, Math.round(duration * effectDurationScale(wellFed)));
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
