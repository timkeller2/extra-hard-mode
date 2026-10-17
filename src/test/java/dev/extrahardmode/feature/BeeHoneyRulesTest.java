package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BeeHoneyRulesTest {
    @Test
    void defaultRateKeepsOneQuarterOfDeliveries() {
        assertEquals(25, BeeHoneyRules.DEFAULT_PERCENT);
        int kept = 0;
        for (int roll = 0; roll < 100; roll++) {
            kept += BeeHoneyRules.honeyIncrement(1, BeeHoneyRules.DEFAULT_PERCENT, roll);
        }
        assertEquals(25, kept);
        assertEquals(1, BeeHoneyRules.honeyIncrement(1, 25, 24));
        assertEquals(0, BeeHoneyRules.honeyIncrement(1, 25, 25));
        assertEquals(2, BeeHoneyRules.honeyIncrement(2, 25, 0));
        assertEquals(0, BeeHoneyRules.honeyIncrement(2, 25, 25));
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
