package dev.extrahardmode.feature;

import dev.extrahardmode.player.FoodHistory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
     * A bright-green up mark is a craving: the food has left the variety run (the meals that
     * define the level, not the extra repeat slots) and eating it keeps that level, so the run
     * grows. A bright-green minus is the oldest meal still inside that run when the run is 7 or
     * more and that food appears only once, so the run holds. A yellow number is how many meals
     * ago this food was last eaten. Grace still allows one more. Slight means that bite weakens the run.
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
        if (outsideVarietyRun(recent, foodId, level)) {
            return FoodCraving.GREAT;
        }
        if (isDue(recent, foodId, trailing, level)
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

    /**
     * Meals since this food was last eaten, counting that meal as 1. Zero when it is absent.
     */
    public static int mealsAgo(List<String> history, String foodId) {
        if (foodId == null || foodId.isEmpty() || history == null || history.isEmpty()) {
            return 0;
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            if (foodId.equals(history.get(i))) {
                return history.size() - i;
            }
        }
        return 0;
    }

    /** "1 meal ago" or "12 meals ago". Zero is an empty phrase. */
    public static String mealsAgoPhrase(int mealsAgo) {
        if (mealsAgo <= 0) {
            return "";
        }
        if (mealsAgo == 1) {
            return "1 meal ago";
        }
        return mealsAgo + " meals ago";
    }

    /**
     * Oldest meal in the Well Fed window that appears once. Null when Well Fed is off,
     * or every food in that window is repeated.
     */
    public static String oldestUniqueInRun(List<String> history, int wellFed) {
        if (wellFed <= 0 || history == null || history.isEmpty()) {
            return null;
        }
        int window = Math.min(WellFedRules.holdWindow(wellFed), history.size());
        if (window <= 0) {
            return null;
        }
        int start = history.size() - window;
        Map<String, Integer> counts = new HashMap<>();
        for (int i = start; i < history.size(); i++) {
            String id = history.get(i);
            if (id == null || id.isEmpty()) {
                continue;
            }
            counts.merge(id, 1, Integer::sum);
        }
        for (int i = start; i < history.size(); i++) {
            String id = history.get(i);
            if (id != null && !id.isEmpty() && counts.getOrDefault(id, 0) == 1) {
                return id;
            }
        }
        return null;
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
     * Absent from the variety run, and eating it keeps the current level. The variety run is the
     * gain window: 13 meals at Well Fed 3. The longer hold window is that run plus the repeat
     * slots, so a repeat pushes the oldest meal out of the 13 and this becomes true. At level 0
     * the run is the last 7 meals.
     */
    static boolean outsideVarietyRun(List<String> recent, String foodId, int level) {
        if (foodId == null || foodId.isEmpty()) {
            return false;
        }
        if (!FoodHistory.isNovelInLast(recent, foodId, VARIETY_WINDOW)) {
            return false;
        }
        if (!FoodHistory.isNovelInLast(recent, foodId, WellFedRules.gainWindow(level))) {
            return false;
        }
        // A food absent from the whole hold window is a new craving, as before.
        // One that only fell out of the variety run is a craving when eating it keeps the level.
        if (FoodHistory.isNovelInLast(recent, foodId, WellFedRules.holdWindow(level))) {
            return true;
        }
        return eatingKeepsLevel(recent, foodId, level);
    }

    /** True when one more meal of {@code foodId} still satisfies {@code level}. */
    static boolean eatingKeepsLevel(List<String> recent, String foodId, int level) {
        if (level <= 0) {
            return true;
        }
        List<String> after = FoodHistory.record(recent, foodId, WellFedRules.HISTORY_CAP);
        return WellFedRules.holds(after, level);
    }

    /**
     * Oldest meal of the variety run when that run is already 7 or longer. A tail longer than the
     * gain window keeps its due meal on the oldest food still inside the window. Level 0 uses the
     * whole tail, which is the last 7 once a repeat appears.
     */
    static boolean isDue(List<String> recent, String foodId, int trailing, int level) {
        if (foodId == null || trailing < VARIETY_WINDOW || recent == null || recent.size() < trailing) {
            return false;
        }
        int span = trailing;
        if (level > 0) {
            span = Math.min(trailing, WellFedRules.gainWindow(level));
        }
        if (span < VARIETY_WINDOW || recent.size() < span) {
            return false;
        }
        return foodId.equals(recent.get(recent.size() - span));
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
