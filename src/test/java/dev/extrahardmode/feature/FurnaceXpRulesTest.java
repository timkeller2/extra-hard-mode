package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FurnaceXpRulesTest {
    @Test
    void milliScaleMatchesVanillaFractions() {
        assertEquals(1000, FurnaceXpRules.MILLI_PER_XP);
        assertEquals(2, FurnaceXpRules.FURNACE_RESULT_SLOT);
        assertEquals(700, FurnaceXpRules.toMilli(0.7F));
        assertEquals(100, FurnaceXpRules.toMilli(0.1F));
        assertEquals(0, FurnaceXpRules.toMilli(0.0F));
        assertEquals(0, FurnaceXpRules.toMilli(-1.0F));
        assertEquals(50, FurnaceXpRules.AUTOMATION_BONUS_PERCENT);
        assertEquals(1050, FurnaceXpRules.withAutomationBonus(700));
        assertEquals(0, FurnaceXpRules.withAutomationBonus(0));
        assertEquals(1500, FurnaceXpRules.withAutomationBonus(1000));
    }

    @Test
    void poolFollowsMovedItems() {
        assertEquals(0, FurnaceXpRules.transferMilli(0, 1, 8));
        assertEquals(0, FurnaceXpRules.transferMilli(800, 0, 8));
        assertEquals(100, FurnaceXpRules.transferMilli(800, 1, 8));
        assertEquals(800, FurnaceXpRules.transferMilli(800, 8, 8));
        assertEquals(800, FurnaceXpRules.transferMilli(800, 9, 8));
        assertEquals(400, FurnaceXpRules.transferMilli(800, 4, 8));
    }

    @Test
    void orbRollMatchesCreateExperience() {
        assertEquals(0, FurnaceXpRules.orbsFromMilli(0, 0.0F));
        assertEquals(1, FurnaceXpRules.orbsFromMilli(1000, 0.0F));
        assertEquals(0, FurnaceXpRules.orbsFromMilli(700, 0.8F));
        assertEquals(1, FurnaceXpRules.orbsFromMilli(700, 0.6F));
        assertEquals(44, FurnaceXpRules.orbsFromMilli(44800, 0.9F));
        assertEquals(45, FurnaceXpRules.orbsFromMilli(44800, 0.1F));
    }
}
