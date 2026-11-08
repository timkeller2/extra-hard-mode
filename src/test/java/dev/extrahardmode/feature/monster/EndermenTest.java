package dev.extrahardmode.feature.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EndermenTest {
    @Test
    void cooldownIsThreeSeconds() {
        assertFalse(Endermen.onCooldown(60, 0));
        assertTrue(Endermen.onCooldown(59, 0));
        assertTrue(Endermen.onCooldown(120, 61));
        assertFalse(Endermen.onCooldown(121, 61));
    }

    @Test
    void pullsWhenThePathIsBlockedOrTheSpaceIsTwoBlocksTall() {
        assertTrue(Endermen.pullsPlayer(true, false));
        assertTrue(Endermen.pullsPlayer(false, true));
        assertFalse(Endermen.pullsPlayer(false, false));
        assertTrue(Endermen.isTwoBlocksTall(true, false));
        assertTrue(Endermen.isTwoBlocksTall(false, true));
        assertFalse(Endermen.isTwoBlocksTall(false, false));
        assertEquals(5, Endermen.RANGE_BLOCKS);
    }
}
