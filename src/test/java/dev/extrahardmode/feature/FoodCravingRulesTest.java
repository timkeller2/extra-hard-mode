package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class FoodCravingRulesTest {
    @Test
    void aNewDietIsAGreatCravingAndHidesNonFood() {
        assertEquals(FoodCraving.GREAT, FoodCravingRules.classify(List.of(), 0, "minecraft:apple"));
        assertEquals(FoodCraving.GREAT, FoodCravingRules.classify(null, 0, "minecraft:apple"));
        assertEquals(FoodCraving.NORMAL, FoodCravingRules.classify(List.of("minecraft:apple"), 0, ""));
        assertEquals(FoodCraving.NORMAL, FoodCravingRules.classify(List.of("minecraft:apple"), 0, null));
    }

    @Test
    void recentFoodIsPlainUnlessItKeepsTheStreakOrSitsOutsideBothWindows() {
        List<String> seven = List.of("a", "b", "c", "d", "e", "f", "g");
        assertEquals(FoodCraving.NORMAL, FoodCravingRules.classify(seven, 0, "d"));
        assertEquals(FoodCraving.GREAT, FoodCravingRules.classify(seven, 0, "a"));
        assertEquals(FoodCraving.GREAT, FoodCravingRules.classify(seven, 0, "h"));
        assertEquals(FoodCraving.GREAT, FoodCravingRules.classify(List.of("a", "b", "c"), 0, "d"));
        assertEquals(FoodCraving.GREAT, FoodCravingRules.classify(List.of("a", "b", "c", "d", "e", "f"), 0, "g"));
        assertEquals(FoodCraving.NORMAL, FoodCravingRules.classify(List.of("a", "b", "c", "d", "e", "f"), 0, "f"));
    }

    @Test
    void sickOutlinesFollowThePenaltyAndWellFedDelaysThem() {
        List<String> four = List.of("a", "a", "a", "a", "b", "b");
        assertEquals(FoodCraving.SICK, FoodCravingRules.classify(four, 0, "a"));
        assertEquals(FoodCraving.NORMAL, FoodCravingRules.classify(four, 0, "b"));
        List<String> six = List.of("a", "a", "a", "a", "a", "a");
        assertEquals(FoodCraving.VERY_SICK, FoodCravingRules.classify(six, 0, "a"));
        assertEquals(FoodCraving.SICK, FoodCravingRules.classify(six, 1, "a"));
        assertEquals(FoodCraving.VERY_SICK, FoodCravingRules.classify(List.of("a", "a", "a", "a", "a", "a", "a"), 0, "a"));
    }

    @Test
    void wellFedSplitsSlightCravingFromALongerStreak() {
        List<String> ten = List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
        assertEquals(FoodCraving.GREAT, FoodCravingRules.classify(ten, 1, "k"));
        assertEquals(FoodCraving.GREAT, FoodCravingRules.classify(ten, 1, "a"));
        assertEquals(FoodCraving.GRACE, FoodCravingRules.classify(ten, 1, "b"));
        assertEquals(FoodCraving.NORMAL, FoodCravingRules.classify(ten, 1, "j"));
        assertEquals(FoodCraving.GREAT, FoodCravingRules.classify(ten, 0, "a"));
        assertEquals(FoodCraving.GREAT, FoodCravingRules.classify(ten, 0, "b"));
        List<String> spared = List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "a");
        assertEquals(FoodCraving.GREAT, FoodCravingRules.classify(spared, 1, "b"));
        assertEquals(FoodCraving.GRACE, FoodCravingRules.classify(spared, 1, "c"));
        List<String> tight = List.of("x", "a", "b", "c", "d", "e", "f", "g", "h", "a");
        assertEquals(FoodCraving.GREAT, FoodCravingRules.classify(tight, 1, "b"));
        assertEquals(FoodCraving.GRACE, FoodCravingRules.classify(tight, 1, "x"));
        assertEquals(FoodCraving.GREAT, FoodCravingRules.classify(tight, 0, "x"));
        List<String> over = List.of("z", "x", "b", "c", "d", "e", "f", "g", "h", "a", "a");
        assertEquals(FoodCraving.SLIGHT, FoodCravingRules.classify(over, 1, "b"));
        List<String> broken = List.of("a", "b", "c", "d", "e", "f", "g", "h", "h", "h");
        assertEquals(FoodCraving.SLIGHT, FoodCravingRules.classify(broken, 1, "a"));
        assertEquals(FoodCraving.GREAT, FoodCravingRules.classify(broken, 1, "z"));
        List<String> repeatedDue = List.of("b", "b", "c", "d", "e", "f", "g", "h", "i", "j");
        assertEquals(FoodCraving.GRACE, FoodCravingRules.classify(repeatedDue, 1, "b"));
        List<String> pastGrace = List.of("b", "b", "b", "d", "e", "f", "g", "h", "i", "j");
        assertEquals(FoodCraving.SLIGHT, FoodCravingRules.classify(pastGrace, 1, "b"));
    }

    @Test
    void aPenaltyBeatsAFoodThatWouldAlsoExtendTheStreak() {
        List<String> meals = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            meals.add("apple");
        }
        for (int i = 0; i < 8; i++) {
            meals.add("f" + i);
        }
        assertEquals(FoodCraving.SICK, FoodCravingRules.classify(meals, 3, "apple"));
        assertTrue(FoodCravingRules.trailingDifferent(meals) >= FoodCravingRules.VARIETY_WINDOW);
    }

    @Test
    void outlinedCravingsUseDistinctColors() {
        assertEquals(0, FoodCravingRules.outlineArgb(FoodCraving.NORMAL));
        assertEquals(0, FoodCravingRules.outlineArgb(null));
        assertNotEquals(0, FoodCravingRules.outlineArgb(FoodCraving.VERY_SICK));
        assertNotEquals(
                FoodCravingRules.outlineArgb(FoodCraving.VERY_SICK),
                FoodCravingRules.outlineArgb(FoodCraving.SICK));
        assertNotEquals(
                FoodCravingRules.outlineArgb(FoodCraving.SLIGHT),
                FoodCravingRules.outlineArgb(FoodCraving.GRACE));
        assertNotEquals(
                FoodCravingRules.outlineArgb(FoodCraving.SLIGHT),
                FoodCravingRules.outlineArgb(FoodCraving.GREAT));
        assertNotEquals(
                FoodCravingRules.outlineArgb(FoodCraving.GRACE),
                FoodCravingRules.outlineArgb(FoodCraving.GREAT));
    }
}
