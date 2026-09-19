package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HealthRulesTest {
    @Test
    void canHealUsesAdjustedMaxNotTwenty() {
        assertFalse(HealthRules.canHeal(20.0F, 20.0F));
        assertTrue(HealthRules.canHeal(20.0F, 34.0F));
        assertTrue(HealthRules.canHeal(19.0F, 20.0F));
        assertFalse(HealthRules.canHeal(34.0F, 34.0F));
        assertFalse(HealthRules.canHeal(0.0F, 34.0F));
    }

    @Test
    void afterHealFillsPastVanillaMax() {
        assertEquals(20.0F, HealthRules.afterHeal(18.0F, 4.0F, 20.0F), 1.0E-4F);
        assertEquals(22.0F, HealthRules.afterHeal(18.0F, 4.0F, 34.0F), 1.0E-4F);
        assertEquals(34.0F, HealthRules.afterHeal(32.0F, 8.0F, 34.0F), 1.0E-4F);
        assertEquals(20.0F, HealthRules.afterHeal(20.0F, 5.0F, 20.0F), 1.0E-4F);
        assertEquals(25.0F, HealthRules.afterHeal(20.0F, 5.0F, 34.0F), 1.0E-4F);
        assertEquals(0.0F, HealthRules.afterHeal(0.0F, 10.0F, 34.0F), 1.0E-4F);
    }

    @Test
    void restoreLoadedHealthUnclampsAfterArmorRaisesMax() {
        assertEquals(30.0F, HealthRules.restoreLoadedHealth(20.0F, 30.0F, 34.0F), 1.0E-4F);
        assertEquals(20.0F, HealthRules.restoreLoadedHealth(20.0F, 30.0F, 20.0F), 1.0E-4F);
        assertEquals(15.0F, HealthRules.restoreLoadedHealth(15.0F, 15.0F, 34.0F), 1.0E-4F);
        assertEquals(34.0F, HealthRules.restoreLoadedHealth(20.0F, 40.0F, 34.0F), 1.0E-4F);
        assertTrue(HealthRules.stillWaitingForMaxHealth(30.0F, 20.0F));
        assertFalse(HealthRules.stillWaitingForMaxHealth(30.0F, 34.0F));
    }
}
