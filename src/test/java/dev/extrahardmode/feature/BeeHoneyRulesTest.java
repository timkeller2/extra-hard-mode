package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BeeHoneyRulesTest {
    private static final int[] SIXTEEN_SINGLES = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};

    @Test
    void defaultRateKeepsOneFifthOfDeliveries() {
        assertEquals(20, BeeHoneyRules.DEFAULT_PERCENT);
        assertEquals(29, BeeHoneyRules.HARVEST_RANGE);
        int kept = 0;
        for (int roll = 0; roll < 100; roll++) {
            kept += BeeHoneyRules.honeyIncrement(1, BeeHoneyRules.DEFAULT_PERCENT, roll);
        }
        assertEquals(20, kept);
        assertEquals(1, BeeHoneyRules.honeyIncrement(1, 20, 19));
        assertEquals(0, BeeHoneyRules.honeyIncrement(1, 20, 20));
        assertEquals(2, BeeHoneyRules.honeyIncrement(2, 20, 0));
        assertEquals(0, BeeHoneyRules.honeyIncrement(2, 20, 20));
    }

    @Test
    void flowerBonusIsTheRoundedSumOfSquareRoots() {
        assertEquals(0, BeeHoneyRules.flowerBonus(null));
        assertEquals(0, BeeHoneyRules.flowerBonus(new int[0]));
        assertEquals(0, BeeHoneyRules.flowerBonus(new int[] {0, -3}));
        assertEquals(4, BeeHoneyRules.flowerBonus(new int[] {16}));
        assertEquals(16, BeeHoneyRules.flowerBonus(SIXTEEN_SINGLES));
        assertEquals(3, BeeHoneyRules.flowerBonus(new int[] {9}));
        assertEquals(2, BeeHoneyRules.flowerBonus(new int[] {4}));
        assertEquals(1, BeeHoneyRules.flowerBonus(new int[] {1}));
        assertEquals(3, BeeHoneyRules.flowerBonus(new int[] {1, 1, 1}));
        assertEquals(1, BeeHoneyRules.flowerBonus(new int[] {2}));
        assertEquals(3, BeeHoneyRules.flowerBonus(new int[] {2, 2}));
    }

    @Test
    void fillPercentAddsTheBonusAndCaps() {
        assertEquals(20, BeeHoneyRules.fillPercent(20, new int[0]));
        assertEquals(24, BeeHoneyRules.fillPercent(20, new int[] {16}));
        assertEquals(36, BeeHoneyRules.fillPercent(20, SIXTEEN_SINGLES));
        assertEquals(23, BeeHoneyRules.fillPercent(20, new int[] {1, 1, 1}));
        assertEquals(0, BeeHoneyRules.fillPercent(0, new int[] {16, 16, 16}));
        assertEquals(0, BeeHoneyRules.fillPercent(-4, new int[] {9}));
        assertEquals(100, BeeHoneyRules.fillPercent(100, new int[] {1}));
        assertEquals(100, BeeHoneyRules.fillPercent(90, SIXTEEN_SINGLES));
        assertEquals(100, BeeHoneyRules.fillPercent(150, new int[] {16}));
    }

    @Test
    void reportedPercentUsesTheStoredDayThenTheFlowers() {
        assertEquals(0, BeeHoneyRules.reportedPercent(0, true, 40, new int[] {16}));
        assertEquals(100, BeeHoneyRules.reportedPercent(100, false, 0, new int[] {1}));
        assertEquals(36, BeeHoneyRules.reportedPercent(20, true, 36, new int[] {1}));
        assertEquals(100, BeeHoneyRules.reportedPercent(20, true, 140, new int[] {1}));
        assertEquals(24, BeeHoneyRules.reportedPercent(20, false, 36, new int[] {16}));
        assertEquals(20, BeeHoneyRules.reportedPercent(20, true, 0, new int[0]));
    }

    @Test
    void tallFlowersCountOnce() {
        assertTrue(BeeHoneyRules.countsHalf(false, false, false));
        assertTrue(BeeHoneyRules.countsHalf(false, false, true));
        assertTrue(BeeHoneyRules.countsHalf(true, true, true));
        assertFalse(BeeHoneyRules.countsHalf(true, true, false));
        assertTrue(BeeHoneyRules.countsHalf(false, true, false));
        assertFalse(BeeHoneyRules.countsHalf(false, true, true));
    }

    @Test
    void fullRateKeepsTheUsualGainAndZeroKeepsNone() {
        assertEquals(1, BeeHoneyRules.honeyIncrement(1, 100, 99));
        assertEquals(2, BeeHoneyRules.honeyIncrement(2, 100, 0));
        assertEquals(1, BeeHoneyRules.honeyIncrement(1, 150, 50));
        assertEquals(0, BeeHoneyRules.honeyIncrement(1, 0, 0));
        assertEquals(0, BeeHoneyRules.honeyIncrement(2, -5, 0));
        assertEquals(0, BeeHoneyRules.honeyIncrement(0, 100, 0));
        assertEquals(0, BeeHoneyRules.honeyIncrement(-1, 100, 0));
    }
}
