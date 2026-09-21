package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ShieldRulesTest {
    @Test
    void absorbedUsesConfiguredPercent() {
        assertEquals(100, ShieldRules.DEFAULT_ABSORB_PERCENT);
        assertEquals(10.0F, ShieldRules.absorbed(10.0F, 100), 1.0E-4F);
        assertEquals(7.5F, ShieldRules.absorbed(10.0F, 75), 1.0E-4F);
        assertEquals(0.0F, ShieldRules.absorbed(10.0F, 0), 1.0E-4F);
        assertEquals(10.0F, ShieldRules.absorbed(10.0F, 140), 1.0E-4F);
        assertEquals(0.0F, ShieldRules.absorbed(0.0F, 100), 1.0E-4F);
        assertEquals(0.0F, ShieldRules.absorbed(-3.0F, 100), 1.0E-4F);
    }

    @Test
    void durabilityHitUsesConfiguredMultiplier() {
        assertEquals(3, ShieldRules.DEFAULT_DURABILITY_MULTIPLIER);
        assertEquals(0, ShieldRules.durabilityHit(0, 3));
        assertEquals(0, ShieldRules.durabilityHit(-4, 3));
        assertEquals(3, ShieldRules.durabilityHit(1, 3));
        assertEquals(30, ShieldRules.durabilityHit(10, 3));
        assertEquals(1, ShieldRules.durabilityHit(1, 1));
        assertEquals(0, ShieldRules.durabilityHit(8, 0));
        assertEquals(100, ShieldRules.durabilityHit(1, 500));
    }
}
