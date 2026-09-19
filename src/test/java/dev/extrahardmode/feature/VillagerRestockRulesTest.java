package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VillagerRestockRulesTest {
    @Test
    void restockTimersAreEightTimesVanilla() {
        assertEquals(8, VillagerRestockRules.MULTIPLIER);
        assertEquals(2400L, VillagerRestockRules.VANILLA_BETWEEN_RESTOCKS_TICKS);
        assertEquals(12000L, VillagerRestockRules.VANILLA_CATCH_UP_TICKS);
        assertEquals(19200L, VillagerRestockRules.betweenRestocksTicks());
        assertEquals(96000L, VillagerRestockRules.catchUpTicks());
        assertEquals(19200L, VillagerRestockRules.scaleTicks(2400L));
    }

    @Test
    void restockPeriodIsEightDays() {
        assertFalse(VillagerRestockRules.restockPeriodElapsed(0, 0));
        assertFalse(VillagerRestockRules.restockPeriodElapsed(1, 1));
        assertFalse(VillagerRestockRules.restockPeriodElapsed(8, 1));
        assertTrue(VillagerRestockRules.restockPeriodElapsed(9, 1));
        assertTrue(VillagerRestockRules.restockPeriodElapsed(10, 2));
    }
}
