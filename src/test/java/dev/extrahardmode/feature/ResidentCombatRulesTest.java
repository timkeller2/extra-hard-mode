package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ResidentCombatRulesTest {
    @Test
    void healthIsFortyPlusHousePoints() {
        assertEquals(40.0F, ResidentCombatRules.maxHealth(0));
        assertEquals(64.0F, ResidentCombatRules.maxHealth(24));
        assertEquals(78.0F, ResidentCombatRules.maxHealth(38));
    }

    @Test
    void heartsAreOnePerTenHealthEvenWhenFull() {
        assertEquals(0, ResidentCombatRules.heartIcons(0));
        assertEquals(1, ResidentCombatRules.heartIcons(9));
        assertEquals(4, ResidentCombatRules.heartIcons(40));
        assertEquals(5, ResidentCombatRules.heartIcons(52));
        assertEquals(7, ResidentCombatRules.heartIcons(78));
    }

    @Test
    void zombiesConvertOnlyBelowFiveHealth() {
        assertFalse(ResidentCombatRules.canZombify(5.0F));
        assertFalse(ResidentCombatRules.canZombify(40.0F));
        assertTrue(ResidentCombatRules.canZombify(4.9F));
        assertEquals(3.0F, ResidentCombatRules.BOW_BONUS);
    }
}
