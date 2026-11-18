package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FishingRodRulesTest {
    @Test
    void preventedDurabilityIsNotTripled() {
        assertEquals(3, FishingRodRules.DURABILITY_MULTIPLIER);
        assertEquals(3, FishingRodRules.durabilityLoss(1, 1));
        assertEquals(0, FishingRodRules.durabilityLoss(1, 0));
        assertEquals(6, FishingRodRules.durabilityLoss(2, 2));
        assertEquals(15, FishingRodRules.durabilityLoss(5, 5));
        assertEquals(6, FishingRodRules.durabilityLoss(5, 2));
        assertEquals(0, FishingRodRules.durabilityLoss(0, 0));
        assertEquals(9, FishingRodRules.durabilityLoss(3, 4));
    }
}
