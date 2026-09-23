package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FireBreakRulesTest {
    @Test
    void breakingABurningBlockIgnitesExceptEmptyHandFire() {
        assertFalse(FireBreakRules.igniteOnBreak(false, false, false));
        assertFalse(FireBreakRules.igniteOnBreak(false, false, true));
        assertFalse(FireBreakRules.igniteOnBreak(true, false, true));
        assertTrue(FireBreakRules.igniteOnBreak(true, false, false));
        assertTrue(FireBreakRules.igniteOnBreak(false, true, true));
        assertTrue(FireBreakRules.igniteOnBreak(false, true, false));
    }
}
