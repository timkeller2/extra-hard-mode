package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AnimalCrowdRulesTest {
    @Test
    void villagersOnlySkipsAnimals() {
        assertFalse(AnimalCrowdRules.animalsCount(true));
    }

    @Test
    void animalsCountWhenTheOptionIsOff() {
        assertTrue(AnimalCrowdRules.animalsCount(false));
    }
}
