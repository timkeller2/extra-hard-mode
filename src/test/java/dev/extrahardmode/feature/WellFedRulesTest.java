package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WellFedRulesTest {
    @Test
    void eightDifferentMealsReachLevelOneAndOneRepeatHolds() {
        List<String> meals = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            meals.add("food" + i);
        }
        assertEquals(1, WellFedRules.nextLevel(meals, 0));
        meals.add("food0");
        assertEquals(1, WellFedRules.nextLevel(meals, 1));
        meals.add("food0");
        assertEquals(1, WellFedRules.nextLevel(meals, 1));
        meals.add("food0");
        assertEquals(0, WellFedRules.nextLevel(meals, 1));
    }

    @Test
    void benefitsStopAtFiveWhileHungerAndGraceContinue() {
        assertEquals(5, WellFedRules.healthBonus(5));
        assertEquals(9, WellFedRules.healthBonus(9));
        assertEquals(2, WellFedRules.meleeBonus(5));
        assertEquals(2, WellFedRules.meleeBonus(9));
        assertEquals(0.90F, WellFedRules.saturationMultiplier(5), 0.001F);
        assertEquals(0.90F, WellFedRules.saturationMultiplier(9), 0.001F);
        assertTrue(WellFedRules.luck(3));
        assertFalse(WellFedRules.luck(2));
        assertEquals(25, WellFedRules.foodMax(5));
        assertEquals(29, WellFedRules.foodMax(9));
        assertEquals(260, WellFedRules.regenTicks(300, 5));
        assertEquals(260, WellFedRules.regenTicks(300, 9));
        assertEquals(200, WellFedRules.regenTicks(180, 5));
        assertEquals(0.5F, WellFedRules.effectDurationScale(5), 0.001F);
        assertFalse(WellFedRules.novelFoodMana(true, 1, true, 0, 5));
        assertTrue(WellFedRules.novelFoodMana(true, 1, false, 2, 5));
        assertFalse(WellFedRules.novelFoodMana(true, 1, false, 5, 5));
        assertFalse(WellFedRules.novelFoodMana(true, 1, false, 6, 5));
        assertFalse(WellFedRules.novelFoodMana(true, 0, false, 0, 5));
        assertEquals(0.5F, WellFedRules.effectDurationScale(8), 0.001F);
        assertEquals(0, WellFedRules.repeatPenalty(5, 1));
        assertEquals(1, WellFedRules.repeatPenalty(6, 1));
        assertEquals(17, WellFedRules.holdWindow(5));
        assertEquals(23, WellFedRules.holdWindow(8));
    }

    @Test
    void halfShadowIsOnlyTheLastOddIcon() {
        assertFalse(HudCapacityRules.halfShadow(20, 9));
        assertTrue(HudCapacityRules.halfShadow(21, 10));
        assertFalse(HudCapacityRules.halfShadow(21, 9));
        assertTrue(HudCapacityRules.halfShadow(1, 0));
        assertFalse(HudCapacityRules.halfShadow(2, 0));
    }
}
