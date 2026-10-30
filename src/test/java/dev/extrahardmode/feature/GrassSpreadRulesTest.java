package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GrassSpreadRulesTest {
    @Test
    void defaultRateAllowsOneQuarterOfSpreadRolls() {
        assertEquals(25, GrassSpreadRules.DEFAULT_PERCENT);
        int allowed = 0;
        for (int roll = 0; roll < 100; roll++) {
            if (GrassSpreadRules.allowSpread(GrassSpreadRules.DEFAULT_PERCENT, roll)) {
                allowed++;
            }
        }
        assertEquals(25, allowed);
        assertTrue(GrassSpreadRules.allowSpread(24, 23));
        assertFalse(GrassSpreadRules.allowSpread(24, 24));
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
