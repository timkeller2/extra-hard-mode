package dev.extrahardmode.item;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FragileToolsTest {
    @Test
    void woodStoneCopperDurabilityIsOneThird() {
        assertEquals(19, FragileTools.scaledDurability(59));
        assertEquals(43, FragileTools.scaledDurability(131));
        assertEquals(63, FragileTools.scaledDurability(190));
        assertEquals(1, FragileTools.scaledDurability(2));
        assertEquals(1, FragileTools.scaledDurability(1));
        assertEquals(1, FragileTools.scaledDurability(0));
    }

    @Test
    void existingWearScalesWithNewMax() {
        assertEquals(10, FragileTools.scaledDamage(30, 19));
        assertEquals(19, FragileTools.scaledDamage(59, 19));
        assertEquals(0, FragileTools.scaledDamage(0, 19));
    }
}
