package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LapisShieldRulesTest {
    @Test
    void absorbIsCappedByTheHitAndTheLevel() {
        assertEquals(0.0F, LapisShieldRules.absorb(0.0F, 4.0F), 1.0E-4F);
        assertEquals(0.0F, LapisShieldRules.absorb(-2.0F, 4.0F), 1.0E-4F);
        assertEquals(0.0F, LapisShieldRules.absorb(5.0F, 0.0F), 1.0E-4F);
        assertEquals(3.0F, LapisShieldRules.absorb(3.0F, 5.0F), 1.0E-4F);
        assertEquals(2.5F, LapisShieldRules.absorb(10.0F, 2.5F), 1.0E-4F);
    }

    @Test
    void oneLapisCoversThreePointsRoundedUp() {
        assertEquals(3, LapisShieldRules.DAMAGE_PER_LAPIS);
        assertEquals(0, LapisShieldRules.lapisCost(0.0F));
        assertEquals(0, LapisShieldRules.lapisCost(-1.0F));
        assertEquals(1, LapisShieldRules.lapisCost(0.2F));
        assertEquals(1, LapisShieldRules.lapisCost(3.0F));
        assertEquals(2, LapisShieldRules.lapisCost(3.1F));
        assertEquals(2, LapisShieldRules.lapisCost(6.0F));
        assertEquals(3, LapisShieldRules.lapisCost(6.1F));
    }

    @Test
    void unpaidAbsorbLeavesTheHitIntact() {
        assertEquals(6.0F, LapisShieldRules.damageAfterAbsorb(6.0F, 0.0F), 1.0E-4F);
        assertEquals(2.5F, LapisShieldRules.damageAfterAbsorb(6.0F, 3.5F), 1.0E-4F);
        assertEquals(0.0F, LapisShieldRules.damageAfterAbsorb(2.0F, 2.0F), 1.0E-4F);
        assertEquals(0.0F, LapisShieldRules.damageAfterAbsorb(2.0F, 5.0F), 1.0E-4F);
    }
}
