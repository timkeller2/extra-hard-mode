package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ShieldRulesTest {
    @Test
    void absorbedIsSeventyFivePercentOfBlocked() {
        assertEquals(7.5F, ShieldRules.absorbed(10.0F), 1.0E-4F);
        assertEquals(0.75F, ShieldRules.absorbed(1.0F), 1.0E-4F);
        assertEquals(0.0F, ShieldRules.absorbed(0.0F), 1.0E-4F);
        assertEquals(0.0F, ShieldRules.absorbed(-3.0F), 1.0E-4F);
    }

    @Test
    void durabilityHitDoublesVanilla() {
        assertEquals(0, ShieldRules.durabilityHit(0));
        assertEquals(0, ShieldRules.durabilityHit(-4));
        assertEquals(2, ShieldRules.durabilityHit(1));
        assertEquals(20, ShieldRules.durabilityHit(10));
    }
}
