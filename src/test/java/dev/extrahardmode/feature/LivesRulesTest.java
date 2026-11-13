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
    void deathStatesLivesLeftOrHeartsDown() {
        assertEquals("You have 3 lives remaining.", LivesRules.deathMessage(3));
        assertEquals("You have 1 life remaining.", LivesRules.deathMessage(1));
        assertEquals("You have 0 lives remaining.", LivesRules.deathMessage(0));
        assertEquals("You are down 1 heart.", LivesRules.deathMessage(-1));
        assertEquals("You are down 4 hearts.", LivesRules.deathMessage(-4));
        String text = String.join(" ", EhmHelp.resolve("combat").lines());
        assertTrue(text.contains("above -1 says how many lives remain"));
        assertTrue(text.contains("you are down that many hearts"));
        assertFalse(text.contains("You lose"));
        assertFalse(text.contains("You win"));
    }

    @Test
    void levelAbove32BuysAMissingLife() {
        assertFalse(LivesRules.aboveRecoveryLevel(32));
        assertTrue(LivesRules.aboveRecoveryLevel(33));
        assertEquals(0, LivesRules.recoveryCost(5, 5, 10_000));
        assertEquals(0, LivesRules.recoveryCost(1, -1, 10_000));
        assertEquals(0, LivesRules.recoveryCost(2, 5, 0));
        assertEquals(500, LivesRules.recoveryCost(4, 5, 10_000));
        assertEquals(750, LivesRules.recoveryCost(3, 5, 10_000));
        assertEquals(1000, LivesRules.recoveryCost(2, 5, 10_000));
        assertEquals(2000, LivesRules.recoveryCost(-2, 5, 10_000));
        assertEquals(40, LivesRules.recoveryCost(2, 5, 40));
        String text = String.join(" ", EhmHelp.resolve("combat").lines());
        assertTrue(text.contains("Past level 32"));
        assertTrue(text.contains("250 experience plus 250 for each life below full"));
    }
}
