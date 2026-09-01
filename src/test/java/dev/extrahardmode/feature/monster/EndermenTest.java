package dev.extrahardmode.feature.monster;

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
    void cheeseIsLosOrTwoHighRoof() {
        assertTrue(Endermen.isTwoHighRoof(true, false, false));
        assertTrue(Endermen.isTwoHighRoof(false, true, false));
        assertTrue(Endermen.isTwoHighRoof(false, false, true));
        assertFalse(Endermen.isTwoHighRoof(false, false, false));
    }
}
