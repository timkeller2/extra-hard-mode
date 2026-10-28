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
    void bonusesFollowTheirOwnRules() {
        assertEquals(5, WellFedRules.healthBonus(5));
        assertEquals(9, WellFedRules.healthBonus(9));
        assertEquals(0, WellFedRules.meleeBonus(7));
        assertEquals(1, WellFedRules.meleeBonus(8));
        assertEquals(1, WellFedRules.meleeBonus(15));
        assertEquals(2, WellFedRules.meleeBonus(16));
        assertEquals(7, WellFedRules.meleeBonus(57));
        assertEquals(0.95F, WellFedRules.saturationMultiplier(5), 0.001F);
        assertEquals(0.91F, WellFedRules.saturationMultiplier(9), 0.001F);
        assertEquals(0.70F, WellFedRules.saturationMultiplier(30), 0.001F);
        assertEquals(0.70F, WellFedRules.saturationMultiplier(57), 0.001F);
        assertEquals(0, WellFedRules.manaBonus(0));
        assertEquals(0, WellFedRules.manaBonus(2));
        assertEquals(1, WellFedRules.manaBonus(3));
        assertEquals(1, WellFedRules.manaBonus(5));
        assertEquals(3, WellFedRules.manaBonus(9));
        assertEquals(25, WellFedRules.foodMax(5));
        assertEquals(29, WellFedRules.foodMax(9));
        assertEquals(300, WellFedRules.regenTicks(300, 0));
        assertEquals(280, WellFedRules.regenTicks(300, 5));
        assertEquals(240, WellFedRules.regenTicks(300, 9));
        assertEquals(100, WellFedRules.regenTicks(300, 30));
        assertEquals(100, WellFedRules.regenTicks(300, 57));
        assertEquals(80, WellFedRules.regenTicks(80, 0));
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
