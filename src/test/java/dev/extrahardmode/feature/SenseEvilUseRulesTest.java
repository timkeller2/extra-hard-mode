package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SenseEvilUseRulesTest {
    @Test
    void clickIsAtMostHalfASecondIncludingASameTickPress() {
        assertEquals(10, SenseEvilUseRules.CLICK_TICKS);
        assertFalse(SenseEvilUseRules.click(-1));
        assertTrue(SenseEvilUseRules.click(0));
        assertTrue(SenseEvilUseRules.click(10));
        assertFalse(SenseEvilUseRules.click(11));
        assertFalse(SenseEvilUseRules.click(32));
    }

    @Test
    void hungryReleaseCastsOnlyOnAClickAndAFullPlayerCastsOnThePress() {
        assertTrue(SenseEvilUseRules.castOnPress(false));
        assertFalse(SenseEvilUseRules.castOnPress(true));
        assertFalse(SenseEvilUseRules.castOnRelease(false, 0));
        assertFalse(SenseEvilUseRules.castOnRelease(false, 10));
        assertTrue(SenseEvilUseRules.castOnRelease(true, 0));
        assertTrue(SenseEvilUseRules.castOnRelease(true, 10));
        assertFalse(SenseEvilUseRules.castOnRelease(true, 11));
        assertFalse(SenseEvilUseRules.castOnRelease(true, 32));
    }

    @Test
    void outlineRemainingMatchesTheMistWindow() {
        assertEquals(0, SenseEvilUseRules.remainingTicks(-1L, 100L));
        assertEquals(0, SenseEvilUseRules.remainingTicks(100L, 100L));
        assertEquals(1, SenseEvilUseRules.remainingTicks(101L, 100L));
        assertEquals(1200, SenseEvilUseRules.remainingTicks(1300L, 100L));
        assertEquals(Integer.MAX_VALUE, SenseEvilUseRules.remainingTicks(Long.MAX_VALUE, 0L));
    }

    @Test
    void barMaxKeepsTheCastDurationWhenTimeRemains() {
        assertEquals(1200, SenseEvilUseRules.barMax(1200, 1200));
        assertEquals(1200, SenseEvilUseRules.barMax(100, 1200));
        assertEquals(1300, SenseEvilUseRules.barMax(1300, 1200));
        assertEquals(0, SenseEvilUseRules.barMax(0, 0));
        assertEquals(50, SenseEvilUseRules.barMax(-4, 50));
    }
}
