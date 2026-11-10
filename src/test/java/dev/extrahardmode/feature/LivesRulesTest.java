package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LivesRulesTest {
    @Test
    void livesStartOffAndDeathsCanGoNegative() {
        assertEquals(-1, LivesRules.DEFAULT_LIVES);
        assertFalse(LivesRules.enabled(-1));
        assertFalse(LivesRules.enabled(-4));
        assertTrue(LivesRules.enabled(0));
        assertTrue(LivesRules.enabled(1));
        assertEquals(0, LivesRules.starting(0));
        assertEquals(3, LivesRules.starting(3));
        assertEquals(2, LivesRules.spend(3));
        assertEquals(0, LivesRules.spend(1));
        assertEquals(-1, LivesRules.spend(0));
        assertEquals(-4, LivesRules.spend(-3));
        assertTrue(LivesRules.spentLast(1));
        assertFalse(LivesRules.spentLast(2));
        assertFalse(LivesRules.spentLast(0));
        assertTrue(LivesRules.canRestore(4, 5));
        assertFalse(LivesRules.canRestore(5, 5));
        assertTrue(LivesRules.canRestore(-1, 0));
        assertFalse(LivesRules.canRestore(0, 0));
        assertFalse(LivesRules.canRestore(-3, -1));
    }

    @Test
    void negativeLivesLowerBaseHealthToThreeHearts() {
        assertEquals(20.0, LivesRules.baseHealth(-1, 0));
        assertEquals(20.0, LivesRules.baseHealth(-1, -5));
        assertEquals(20.0, LivesRules.baseHealth(0, 0));
        assertEquals(20.0, LivesRules.baseHealth(3, 3));
        assertEquals(20.0, LivesRules.baseHealth(3, 1));
        assertEquals(20.0, LivesRules.baseHealth(5, 0));
        assertEquals(18.0, LivesRules.baseHealth(5, -1));
        assertEquals(16.0, LivesRules.baseHealth(0, -2));
        assertEquals(6.0, LivesRules.baseHealth(5, -7));
        assertEquals(6.0, LivesRules.baseHealth(5, -8));
        assertEquals(20.0, LivesRules.VANILLA_BASE_HEALTH);
        assertEquals(6.0, LivesRules.MIN_BASE_HEALTH);
    }

    @Test
    void soloLastLifeLosesAndOneSurvivorWins() {
        assertEquals(LivesRules.Verdict.NONE, LivesRules.verdict(1, 1, false));
        assertEquals(LivesRules.Verdict.SOLO_LOSS, LivesRules.verdict(1, 0, true));
        assertEquals(LivesRules.Verdict.NONE, LivesRules.verdict(3, 2, true));
        assertEquals(LivesRules.Verdict.LAST_SURVIVOR, LivesRules.verdict(3, 1, true));
        assertEquals(LivesRules.Verdict.NONE, LivesRules.verdict(3, 0, true));
        assertEquals(LivesRules.Verdict.NONE, LivesRules.verdict(2, 1, false));
        assertEquals(LivesRules.Verdict.NONE, LivesRules.verdict(0, 0, true));
        assertEquals("You lose!", LivesRules.LOSE_SOLO);
        assertEquals("You lose!!!", LivesRules.LOSE_MANY);
        assertEquals("You win!!!", LivesRules.WIN);
        assertEquals(140, LivesRules.CREDITS_DELAY_TICKS);
    }
}
