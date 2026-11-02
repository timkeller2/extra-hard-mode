package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GrassSpreadRulesTest {
    @Test
    void staticRateAllowsThatManySpreadRolls() {
        int allowed = 0;
        for (int roll = 0; roll < 100; roll++) {
            if (GrassSpreadRules.allowSpread(25, roll)) {
                allowed++;
            }
        }
        assertEquals(25, allowed);
        assertTrue(GrassSpreadRules.allowSpread(24, 23));
        assertFalse(GrassSpreadRules.allowSpread(24, 24));
    }

    @Test
    void defaultFollowsCropLossAndZeroStaysOff() {
        assertEquals(-1, GrassSpreadRules.DEFAULT_PERCENT);
        assertEquals(-1, GrassSpreadRules.configuredPercent(-1));
        assertEquals(0, GrassSpreadRules.configuredPercent(0));
        assertEquals(0, GrassSpreadRules.configuredPercent(-5));
        assertEquals(40, GrassSpreadRules.configuredPercent(40));
        assertEquals(100, GrassSpreadRules.configuredPercent(150));
        assertEquals(50, GrassSpreadRules.effectivePercent(-1, 25));
        assertEquals(62, GrassSpreadRules.effectivePercent(-1, 12.6));
        assertEquals(63, GrassSpreadRules.effectivePercent(-1, 12.5));
        assertEquals(75, GrassSpreadRules.effectivePercent(-1, 0));
        assertEquals(0, GrassSpreadRules.effectivePercent(-1, 75));
        assertEquals(0, GrassSpreadRules.effectivePercent(-1, 90));
        assertEquals(0, GrassSpreadRules.effectivePercent(0, 10));
        assertEquals(40, GrassSpreadRules.effectivePercent(40, 90));
        assertEquals(100, GrassSpreadRules.effectivePercent(150, 0));
        assertFalse(GrassSpreadRules.allowSpread(GrassSpreadRules.effectivePercent(-1, 80), 0));
        assertTrue(GrassSpreadRules.allowSpread(GrassSpreadRules.effectivePercent(-1, 25), 49));
        assertFalse(GrassSpreadRules.allowSpread(GrassSpreadRules.effectivePercent(-1, 25), 50));
    }

    @Test
    void fullRateAlwaysSpreadsAndZeroNeverDoes() {
        assertTrue(GrassSpreadRules.allowSpread(100, 0));
        assertTrue(GrassSpreadRules.allowSpread(100, 99));
        assertTrue(GrassSpreadRules.allowSpread(150, 50));
        assertFalse(GrassSpreadRules.allowSpread(0, 0));
        assertFalse(GrassSpreadRules.allowSpread(-5, 0));
    }

    @Test
    void openCoverLetsFencesSpreadAndStillBlocksASealedTop() {
        assertTrue(GrassSpreadRules.openCover(false, false, false));
        assertFalse(GrassSpreadRules.openCover(true, false, false));
        assertFalse(GrassSpreadRules.openCover(false, true, false));
        assertFalse(GrassSpreadRules.openCover(false, false, true));
        assertEquals(15, GrassSpreadRules.spreadLight(4, 15, 0, 0, 0, 0));
        assertEquals(9, GrassSpreadRules.spreadLight(2, 0, 9, 3, 1, 0));
        assertEquals(3, GrassSpreadRules.spreadLight(3, 1, 0, 2, 0, 0));
    }
}
