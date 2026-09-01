package dev.extrahardmode.feature.monster;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HorsesTest {
    @Test
    void caveBandBlocksChestsBelow48() {
        assertTrue(Horses.shouldBlockChest(47, true, Horses.DEFAULT_BLOCK_CHEST_BELOW_Y));
        assertFalse(Horses.shouldBlockChest(48, true, Horses.DEFAULT_BLOCK_CHEST_BELOW_Y));
        assertFalse(Horses.shouldBlockChest(63, true, Horses.DEFAULT_BLOCK_CHEST_BELOW_Y));
    }

    @Test
    void siblingBooleanDisablesYGate() {
        assertFalse(Horses.shouldBlockChest(0, false, 48));
        assertFalse(Horses.shouldBlockChest(47, false, 48));
        assertTrue(Horses.shouldBlockChest(47, true, 48));
    }

    @Test
    void minValueDisablesYGate() {
        assertFalse(Horses.belowChestLimit(-64, Integer.MIN_VALUE));
        assertFalse(Horses.belowChestLimit(0, Integer.MIN_VALUE));
        assertFalse(Horses.shouldBlockChest(0, true, Integer.MIN_VALUE));
    }
}
