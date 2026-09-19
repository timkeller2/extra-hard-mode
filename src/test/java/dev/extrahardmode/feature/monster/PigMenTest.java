package dev.extrahardmode.feature.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PigMenTest {
    @Test
    void lightningCountIsSixtyTwentyTwenty() {
        assertEquals(2, PigMen.lightningCount(0));
        assertEquals(2, PigMen.lightningCount(1));
        assertEquals(3, PigMen.lightningCount(2));
        assertEquals(3, PigMen.lightningCount(3));
        assertEquals(1, PigMen.lightningCount(4));
        assertEquals(1, PigMen.lightningCount(9));
    }

    @Test
    void playerDamageScalesByPercent() {
        assertEquals(7.0F, PigMen.scalePlayerDamage(10.0F, 70), 0.0001F);
        assertEquals(10.0F, PigMen.scalePlayerDamage(10.0F, 100), 0.0001F);
        assertEquals(10.0F, PigMen.scalePlayerDamage(10.0F, 0), 0.0001F);
    }

    @Test
    void netherWartDropsInFortressAndSometimesElsewhere() {
        assertTrue(PigMenRules.dropsNetherWart(true, false, true, true, 25, 99));
        assertFalse(PigMenRules.dropsNetherWart(true, false, true, false, 25, 0));
        assertTrue(PigMenRules.dropsNetherWart(true, false, false, true, 25, 0));
        assertTrue(PigMenRules.dropsNetherWart(true, false, false, true, 25, 24));
        assertFalse(PigMenRules.dropsNetherWart(true, false, false, true, 25, 25));
        assertFalse(PigMenRules.dropsNetherWart(true, false, false, true, 0, 0));
        assertTrue(PigMenRules.dropsNetherWart(true, false, false, true, 100, 99));
        assertFalse(PigMenRules.dropsNetherWart(false, false, false, true, 25, 0));
        assertFalse(PigMenRules.dropsNetherWart(true, true, true, true, 25, 0));
        assertEquals(25, PigMen.DEFAULT_ELSEWHERE_WART_PERCENT);
    }
}
