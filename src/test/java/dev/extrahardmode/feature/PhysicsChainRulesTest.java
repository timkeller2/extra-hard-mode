package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PhysicsChainRulesTest {
    @Test
    void rangeIsFiftyBlocksInAStraightLine() {
        assertEquals(50, PhysicsChainRules.MAX_RANGE);
        assertEquals(4, PhysicsChainRules.HOP_DELAY_TICKS);
        assertTrue(PhysicsChainRules.withinRange(0, 0, 0, 0, 0, 0));
        assertTrue(PhysicsChainRules.withinRange(0, 64, 0, 50, 64, 0));
        assertTrue(PhysicsChainRules.withinRange(0, 0, 0, 0, -50, 0));
        assertTrue(PhysicsChainRules.withinRange(10, 20, 30, -40, 20, 30));
        assertTrue(PhysicsChainRules.withinRange(0, 0, 0, 30, 40, 0));
        assertFalse(PhysicsChainRules.withinRange(0, 0, 0, 51, 0, 0));
        assertFalse(PhysicsChainRules.withinRange(0, 0, 0, 50, 1, 0));
        assertFalse(PhysicsChainRules.withinRange(0, 0, 0, 30, 40, 1));
        assertFalse(PhysicsChainRules.withinRange(0, 0, 0, 36, 36, 0));
    }
}
