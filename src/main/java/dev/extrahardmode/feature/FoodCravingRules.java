package dev.extrahardmode.feature;

import dev.extrahardmode.player.FoodHistory;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * How much the player wants a food, from the meals already eaten. Minecraft-free.
 * Worse states win: a penalized food is never also shown as a craving.
 */
public final class FoodCravingRules {
    /** Meals that decide novelty and the seven-food streak. */
    public static final int VARIETY_WINDOW = HungerRules.DEFAULT_FOOD_HISTORY;

    private FoodCravingRules() {}

    /**
     * What eating {@code foodId} next would mean.
     * Black and brown use the same-food penalty, so Well Fed delays them.
     * A bright-green up mark is a craving: the food is outside the Well Fed window and the last 7
     * meals, so the run grows. A bright-green minus is the oldest meal of a run of 7 or more when
     * that food appears only once in the window, so the run holds. A yellow down mark weakens it.
     * The full one is a repeat grace still allows. The small one is only novel.
     */
    public static FoodCraving classify(List<String> history, int wellFed, String foodId) {
        if (foodId == null || foodId.isEmpty()) {
            return FoodCraving.NORMAL;
        }
        int level = Math.max(0, wellFed);
        List<String> recent = history == null ? List.of() : history;
        int penalty = WellFedRules.repeatPenalty(prospectiveCount(recent, foodId, level), level);
        if (penalty >= 2) {
            return FoodCraving.VERY_SICK;
        }
        if (penalty >= 1) {
            return FoodCraving.SICK;
        }
        int trailing = trailingDifferent(recent);
        if (outsideLastMealsAndWellFed(recent, foodId, level)) {
            return FoodCraving.GREAT;
        }
        if (isDue(recent, foodId, trailing)
                && FoodHistory.countInLast(recent, foodId, WellFedRules.holdWindow(level)) <= 1) {
            return FoodCraving.MAINTAIN;
        }
        if (withinGrace(recent, foodId, level)) {
            return FoodCraving.GRACE;
        }
        if (FoodHistory.isNovelInLast(recent, foodId, VARIETY_WINDOW)) {
            return FoodCraving.SLIGHT;
        }
        return FoodCraving.NORMAL;
    }

    /** Outline color, or 0 when the food should look ordinary. */
    public static int outlineArgb(FoodCraving craving) {
        if (craving == null) {
            return 0;
        }
        return switch (craving) {
            case VERY_SICK -> 0xF0141414;
            case SICK -> 0xF06B3E22;
            case SLIGHT -> 0xF0C6A81E;
            case GRACE -> 0xF0FFE566;
            case MAINTAIN -> 0xF055FF55;
            case GREAT -> 0xF055FF55;
            case NORMAL -> 0;
        };
    }

    /** How many of this food the penalty window would hold after one more meal. */
    static int prospectiveCount(List<String> recent, String foodId, int wellFed) {
        List<String> after = FoodHistory.record(recent, foodId, WellFedRules.HISTORY_CAP);
        return FoodHistory.countInLast(after, foodId, WellFedRules.penaltyWindow(wellFed));
    }

    /** All-different meals at the end of the history. */
    static int trailingDifferent(List<String> recent) {
        if (recent == null || recent.isEmpty()) {
            return 0;
        }
        Set<String> seen = new HashSet<>();
        int count = 0;
        for (int i = recent.size() - 1; i >= 0; i--) {
            String id = recent.get(i);
            if (id == null || id.isEmpty() || !seen.add(id)) {
                break;
            }
            count++;
        }
        return count;
    }

    /**
     * Absent from the last 7 meals and from the Well Fed window. At level 0 that window is the
     * same 7 meals.
     */
    static boolean outsideLastMealsAndWellFed(List<String> recent, String foodId, int level) {
        if (foodId == null || foodId.isEmpty()) {
            return false;
        }
        if (!FoodHistory.isNovelInLast(recent, foodId, VARIETY_WINDOW)) {
            return false;
        }
        return FoodHistory.isNovelInLast(recent, foodId, WellFedRules.holdWindow(level));
    }

    /** Oldest meal of a run that is already 7 or longer. Eating it keeps that run. */
    static boolean isDue(List<String> recent, String foodId, int trailing) {
        if (foodId == null || trailing < VARIETY_WINDOW || recent == null || recent.size() < trailing) {
            return false;
        }
        return foodId.equals(recent.get(recent.size() - trailing));
    }

    /**
     * Not in the last 7 meals, but already inside the Well Fed window, and one more copy still
     * fits the duplicate allowance. Level 0 has no longer window and no grace.
     */
    static boolean withinGrace(List<String> recent, String foodId, int level) {
        if (level <= 0 || foodId == null || foodId.isEmpty()) {
            return false;
        }
        if (!FoodHistory.isNovelInLast(recent, foodId, VARIETY_WINDOW)) {
            return false;
        }
        if (FoodHistory.isNovelInLast(recent, foodId, WellFedRules.holdWindow(level))) {
            return false;
        }
        List<String> after = FoodHistory.record(recent, foodId, WellFedRules.HISTORY_CAP);
        return WellFedRules.holds(after, level);
    }
}
