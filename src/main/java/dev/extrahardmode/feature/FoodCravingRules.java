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
     * Gold lengthens a run of 6 or more different meals. Green is a food outside the Well Fed
     * window, or the oldest meal of a run that is already 7 or longer. Full steel blue is a repeat
     * inside that window that grace still allows. The small steel-blue mark is only novel.
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
        if (extendsRewardedStreak(recent, foodId, trailing)) {
            return FoodCraving.GREAT;
        }
        if (isDue(recent, foodId, trailing) || outsideWellFedStreak(recent, foodId, level)) {
            return FoodCraving.CRAVING;
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
            case SLIGHT -> 0xF07AA0B8;
            case GRACE -> 0xF0B7D0E0;
            case CRAVING -> 0xF03E8A48;
            case GREAT -> 0xF0D4A017;
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

    /** This meal is not in the current run, and the run would then be at least 7. */
    static boolean extendsRewardedStreak(List<String> recent, String foodId, int trailing) {
        if (trailing + 1 < VARIETY_WINDOW || inTrailing(recent, foodId, trailing)) {
            return false;
        }
        return true;
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

    /** Absent from the Well Fed hold window. Level 0 has no window longer than the last 7. */
    static boolean outsideWellFedStreak(List<String> recent, String foodId, int level) {
        if (level <= 0 || foodId == null || foodId.isEmpty()) {
            return false;
        }
        return FoodHistory.isNovelInLast(recent, foodId, WellFedRules.holdWindow(level));
    }

    private static boolean inTrailing(List<String> recent, String foodId, int trailing) {
        if (recent == null || foodId == null || trailing <= 0) {
            return false;
        }
        int start = Math.max(0, recent.size() - trailing);
        for (int i = start; i < recent.size(); i++) {
            if (foodId.equals(recent.get(i))) {
                return true;
            }
        }
        return false;
    }
}
