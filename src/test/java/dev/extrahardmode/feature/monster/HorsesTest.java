package dev.extrahardmode.feature.monster;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HorsesTest {
    @Test
    void caveBandBlocksChestsBelow48() {
        assertTrue(Horses.belowChestLimit(47, Horses.DEFAULT_BLOCK_CHEST_BELOW_Y));
        assertFalse(Horses.belowChestLimit(48, Horses.DEFAULT_BLOCK_CHEST_BELOW_Y));
        assertFalse(Horses.belowChestLimit(63, Horses.DEFAULT_BLOCK_CHEST_BELOW_Y));
    }

    @Test
    void minValueDisablesYGate() {
        assertFalse(Horses.belowChestLimit(-64, Integer.MIN_VALUE));
        assertFalse(Horses.belowChestLimit(0, Integer.MIN_VALUE));
    }
}
