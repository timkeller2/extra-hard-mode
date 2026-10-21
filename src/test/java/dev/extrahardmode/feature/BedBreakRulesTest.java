package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BedBreakRulesTest {
    @Test
    void configuredBreakDestroysTheBed() {
        assertTrue(BedBreakRules.dropsNothing(true, false));
    }

    @Test
    void offOrBypassKeepsTheBed() {
        assertFalse(BedBreakRules.dropsNothing(false, false));
        assertFalse(BedBreakRules.dropsNothing(true, true));
        assertFalse(BedBreakRules.dropsNothing(false, true));
    }
}
