package dev.extrahardmode.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PhysicsBudgetTest {
    @Test
    void liveCapOverflowsToSetBlock() {
        assertFalse(PhysicsBudget.overflowToSetBlock(127, PhysicsBudget.MAX_LIVE_EHM_FALLING));
        assertTrue(PhysicsBudget.overflowToSetBlock(128, PhysicsBudget.MAX_LIVE_EHM_FALLING));
        assertTrue(PhysicsBudget.overflowToSetBlock(200, PhysicsBudget.MAX_LIVE_EHM_FALLING));
    }

    @Test
    void queueDepthDropsOldest() {
        assertFalse(PhysicsBudget.shouldDropOldest(4095, PhysicsBudget.MAX_QUEUE_DEPTH));
        assertTrue(PhysicsBudget.shouldDropOldest(4096, PhysicsBudget.MAX_QUEUE_DEPTH));
    }

    @Test
    void conversionsBudgetIs64() {
        assertEquals(64, PhysicsBudget.CONVERSIONS_PER_TICK);
        assertEquals(64, PhysicsBudget.conversionsThisTick(80, PhysicsBudget.CONVERSIONS_PER_TICK));
        assertEquals(24, PhysicsBudget.conversionsThisTick(24, PhysicsBudget.CONVERSIONS_PER_TICK));
        assertEquals(0, PhysicsBudget.conversionsThisTick(10, 0));
    }

    @Test
    void oneTickPopDoesNotHurt() {
        assertFalse(PhysicsBudget.farEnoughToHurt(0.04));
        assertFalse(PhysicsBudget.farEnoughToHurt(1.0));
        assertTrue(PhysicsBudget.farEnoughToHurt(1.1));
        assertTrue(PhysicsBudget.farEnoughToHurt(4.0));
    }
}
