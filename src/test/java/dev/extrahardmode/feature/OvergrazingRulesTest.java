package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OvergrazingRulesTest {
    @Test
    void crowdNeedsSixteen() {
        assertFalse(OvergrazingRules.overcrowded(15, OvergrazingRules.CROWD_THRESHOLD));
        assertTrue(OvergrazingRules.overcrowded(16, OvergrazingRules.CROWD_THRESHOLD));
        assertTrue(OvergrazingRules.overcrowded(40, OvergrazingRules.CROWD_THRESHOLD));
    }

    @Test
    void lookAndStarveAreThirtyThreePercent() {
        assertTrue(OvergrazingRules.lookInChests(0));
        assertTrue(OvergrazingRules.lookInChests(32));
        assertFalse(OvergrazingRules.lookInChests(33));
        assertFalse(OvergrazingRules.lookInChests(99));
        assertTrue(OvergrazingRules.starve(0));
        assertTrue(OvergrazingRules.starve(32));
        assertFalse(OvergrazingRules.starve(33));
    }

    @Test
    void intervalAndRangeClamp() {
        assertEquals(20, OvergrazingRules.clampInterval(0));
        assertEquals(24000, OvergrazingRules.clampInterval(24000));
        assertEquals(1, OvergrazingRules.clampRange(0));
        assertEquals(8, OvergrazingRules.clampRange(8));
        assertEquals(64, OvergrazingRules.clampRange(99));
    }

    @Test
    void dailyScheduleIsStaggeredThenSteady() {
        assertEquals(11L, OvergrazingRules.firstCheckAt(10L, 24000, 0));
        assertEquals(14L, OvergrazingRules.firstCheckAt(10L, 4, 3));
        assertFalse(OvergrazingRules.isDue(99L, 0L));
        assertFalse(OvergrazingRules.isDue(5L, 10L));
        assertTrue(OvergrazingRules.isDue(10L, 10L));
        assertEquals(24010L, OvergrazingRules.reschedule(10L, 24000));
        assertEquals(100, OvergrazingRules.FOOD_FLOAT_TICKS);
    }
}
