package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CowMilkRulesTest {
    @Test
    void cooldownIsOneMinecraftDay() {
        assertEquals(24000L, CowMilkRules.COOLDOWN_TICKS);
        assertEquals(24000L, CowMilkRules.nextReady(0L));
        assertEquals(25000L, CowMilkRules.nextReady(1000L));
        assertFalse(CowMilkRules.onCooldown(1000L, 0L));
        assertFalse(CowMilkRules.onCooldown(1000L, 1000L));
        assertTrue(CowMilkRules.onCooldown(1000L, 1001L));
        long ready = CowMilkRules.nextReady(10000L);
        assertTrue(CowMilkRules.onCooldown(ready - 1L, ready));
        assertFalse(CowMilkRules.onCooldown(ready, ready));
    }

    @Test
    void extraMealIsAFewSecondsAndNotTheGrazeInterval() {
        assertEquals(1040L, CowMilkRules.hungerAt(1000L, 0));
        assertEquals(1099L, CowMilkRules.hungerAt(1000L, 59));
        assertEquals(1040L, CowMilkRules.hungerAt(1000L, 60));
        assertEquals(1041L, CowMilkRules.hungerAt(1000L, -59));
        long soonest = CowMilkRules.hungerAt(0L, 0);
        long latest = CowMilkRules.hungerAt(0L, CowMilkRules.HUNGER_SPAN_TICKS - 1);
        assertTrue(soonest >= CowMilkRules.HUNGER_MIN_TICKS);
        assertTrue(latest - soonest < 100L);
        assertTrue(latest < OvergrazingRules.DEFAULT_LARGE_INTERVAL_TICKS);
        assertFalse(CowMilkRules.hungerDue(10L, 0L));
        assertFalse(CowMilkRules.hungerDue(1039L, 1040L));
        assertTrue(CowMilkRules.hungerDue(1040L, 1040L));
        assertFalse(CowMilkRules.hungerStale(1040L, 1040L));
        assertFalse(CowMilkRules.hungerStale(1139L, 1040L));
        assertTrue(CowMilkRules.hungerStale(1140L, 1040L));
    }

    @Test
    void lookLabelRoundsTheWaitUpToTheNextSecond() {
        assertEquals(0, CowMilkRules.remainingTicks(0L, 0L));
        assertEquals(0, CowMilkRules.remainingTicks(1000L, 1000L));
        assertEquals(1, CowMilkRules.remainingTicks(1000L, 1001L));
        assertEquals(24000, CowMilkRules.remainingTicks(0L, 24000L));
        assertEquals("20m", CowMilkRules.remainingLabel(24000));
        assertEquals("20m", CowMilkRules.remainingLabel(23981));
        assertEquals("19m 59s", CowMilkRules.remainingLabel(23980));
        assertEquals("1m", CowMilkRules.remainingLabel(1200));
        assertEquals("1m", CowMilkRules.remainingLabel(1181));
        assertEquals("59s", CowMilkRules.remainingLabel(1180));
        assertEquals("1s", CowMilkRules.remainingLabel(1));
        assertEquals("1s", CowMilkRules.remainingLabel(20));
        assertEquals("2s", CowMilkRules.remainingLabel(21));
    }
}
