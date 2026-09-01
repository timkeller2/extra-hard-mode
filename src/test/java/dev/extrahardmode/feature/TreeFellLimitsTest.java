package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TreeFellLimitsTest {
    @Test
    void chebyshevTwoCoversTwoByTwoJungle() {
        assertTrue(TreeFellLimits.inBounds(0, 0, 0, 1, 0, 1), "opposite 2x2 corner is chebyshev 1");
        assertTrue(TreeFellLimits.inBounds(0, 0, 0, 2, 5, 0), "xz chebyshev 2 is allowed");
        assertFalse(TreeFellLimits.inBounds(0, 0, 0, 3, 5, 0), "xz chebyshev 3 is outside");
    }

    @Test
    void acaciaBendWithinTwoIsInBounds() {
        assertTrue(TreeFellLimits.inBounds(2, 1, 2, 4, 3, 2), "acacia bend of 2 stays in xz envelope");
        assertFalse(TreeFellLimits.inBounds(2, 1, 2, 5, 3, 2), "bend of 3 is a different tree");
    }

    @Test
    void verticalEnvelope() {
        assertTrue(TreeFellLimits.inBounds(0, 10, 0, 0, 8, 0), "maxDown 2");
        assertFalse(TreeFellLimits.inBounds(0, 10, 0, 0, 7, 0), "dy -3 is below the stump");
        assertTrue(TreeFellLimits.inBounds(0, 10, 0, 0, 40, 0), "maxUp 30");
        assertFalse(TreeFellLimits.inBounds(0, 10, 0, 0, 41, 0), "dy 31 is above a tree");
    }

    @Test
    void leafAndLogThresholds() {
        assertEquals(64, TreeFellLimits.MAX_LOGS);
        assertEquals(2, TreeFellLimits.MIN_LOGS);
        assertEquals(4, TreeFellLimits.MIN_ADJACENT_LEAVES);
        assertEquals(2, TreeFellLimits.MAX_CHEBYSHEV_XZ);
    }
}
