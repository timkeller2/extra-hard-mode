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
    void livesOnRespawnFillsHealthAndSaturation() {
        assertEquals(20.0F, LivesRules.respawnHealth(true, 20.0F, 25), 0.001F);
        assertEquals(18.0F, LivesRules.respawnHealth(true, 18.0F, 25), 0.001F);
        assertEquals(6.0F, LivesRules.respawnHealth(true, 6.0F, 25), 0.001F);
        assertEquals(5.0F, LivesRules.respawnHealth(false, 20.0F, 25), 0.001F);
        assertEquals(20, LivesRules.respawnFood(true, 6, LivesRules.FULL_FOOD));
        assertEquals(28, LivesRules.respawnFood(true, 6, 28));
        assertEquals(6, LivesRules.respawnFood(false, 6, LivesRules.FULL_FOOD));
        assertEquals(0, LivesRules.respawnFood(false, -3, LivesRules.FULL_FOOD));
        assertEquals(20.0F, LivesRules.fullSaturation(20), 0.001F);
        assertEquals(6.0F, LivesRules.fullSaturation(6), 0.001F);
        String text = String.join(" ", EhmHelp.resolve("combat").lines());
        assertTrue(text.contains("full health, a full hunger bar, and full saturation"));
        assertTrue(text.contains("The usual limit on those does not apply"));
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
        assertTrue(text.contains("still needed for full lives"));
        assertTrue(text.contains("the total when it is above 0"));
    }

    @Test
    void recoveryNoticeSaysShortfallAndTotalWhenPositive() {
        assertEquals(2, LivesRules.stillNeeded(3, 5));
        assertEquals(0, LivesRules.stillNeeded(5, 5));
        assertEquals(6, LivesRules.stillNeeded(-1, 5));
        assertEquals(
                "You still need 2 lives to reach full lives. You have 3 lives.",
                LivesRules.recoveryNotice(3, 5));
        assertEquals(
                "You still need 1 life to reach full lives. You have 4 lives.",
                LivesRules.recoveryNotice(4, 5));
        assertEquals(
                "You still need 4 lives to reach full lives. You have 1 life.",
                LivesRules.recoveryNotice(1, 5));
        assertEquals(
                "You still need 0 lives to reach full lives. You have 5 lives.",
                LivesRules.recoveryNotice(5, 5));
        assertEquals("You still need 5 lives to reach full lives.", LivesRules.recoveryNotice(0, 5));
        assertEquals("You still need 6 lives to reach full lives.", LivesRules.recoveryNotice(-1, 5));
        assertEquals(
                "Steve still needs 2 lives to reach full lives. Steve has 3 lives.",
                LivesRules.recoveryNotice(3, 5, "Steve"));
        assertEquals(
                "Steve still needs 1 life to reach full lives. Steve has 4 lives.",
                LivesRules.recoveryNotice(4, 5, "Steve"));
        assertEquals(
                "Steve still needs 4 lives to reach full lives. Steve has 1 life.",
                LivesRules.recoveryNotice(1, 5, "Steve"));
        assertEquals("Steve still needs 5 lives to reach full lives.", LivesRules.recoveryNotice(0, 5, "Steve"));
        assertEquals("Steve still needs 7 lives to reach full lives.", LivesRules.recoveryNotice(-2, 5, "Steve"));
        assertEquals(LivesRules.recoveryNotice(3, 5), LivesRules.recoveryNotice(3, 5, ""));
        assertEquals(LivesRules.recoveryNotice(3, 5), LivesRules.recoveryNotice(3, 5, null));
    }
}
