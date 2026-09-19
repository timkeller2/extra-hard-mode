package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BoatRulesTest {
    @Test
    void occupiedBoatBreaksAfterMoreThanThreeBlocks() {
        assertEquals(3.0, BoatRules.BREAK_FALL_DISTANCE, 1.0E-9);
        assertFalse(BoatRules.shouldBreak(3.0, true, true));
        assertTrue(BoatRules.shouldBreak(3.01, true, true));
        assertTrue(BoatRules.shouldBreak(20.0, true, true));
        assertFalse(BoatRules.shouldBreak(10.0, false, true));
        assertFalse(BoatRules.shouldBreak(10.0, true, false));
        assertFalse(BoatRules.shouldBreak(0.0, true, true));
    }
}
