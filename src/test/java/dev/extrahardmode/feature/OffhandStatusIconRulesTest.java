package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OffhandStatusIconRulesTest {
    @Test
    void crystalSitsAboveTheOffhandAndTheChickenSitsAboveTheCrystal() {
        assertEquals(77, OffhandStatusIconRules.iconX());
        assertEquals(44, OffhandStatusIconRules.rowY(0));
        assertEquals(26, OffhandStatusIconRules.rowY(1));
        assertTrue(OffhandStatusIconRules.rowY(0) < OffhandStatusIconRules.OFFHAND_Y);
        assertEquals(0, OffhandStatusIconRules.crystalRow(true, true));
        assertEquals(1, OffhandStatusIconRules.chickenRow(true, true));
        assertEquals(0, OffhandStatusIconRules.crystalRow(true, false));
        assertEquals(-1, OffhandStatusIconRules.chickenRow(true, false));
        assertEquals(-1, OffhandStatusIconRules.crystalRow(false, true));
        assertEquals(0, OffhandStatusIconRules.chickenRow(false, true));
        assertEquals(-1, OffhandStatusIconRules.crystalRow(false, false));
        assertEquals(-1, OffhandStatusIconRules.chickenRow(false, false));
        assertTrue(OffhandStatusIconRules.hit(77, 44, 77, 44));
        assertTrue(OffhandStatusIconRules.hit(77, 44, 92, 59));
        assertFalse(OffhandStatusIconRules.hit(77, 44, 76, 44));
        assertFalse(OffhandStatusIconRules.hit(77, 44, 77, 60));
        assertFalse(OffhandStatusIconRules.hit(77, 44, 77, 62));
    }

    @Test
    void eachIconShowsOnlyWhenItsValueIsAboveZero() {
        assertFalse(OffhandStatusIconRules.showCapacity(0.0));
        assertFalse(OffhandStatusIconRules.showCapacity(-1.0));
        assertFalse(OffhandStatusIconRules.showCapacity(Double.NaN));
        assertFalse(OffhandStatusIconRules.showCapacity(0.04));
        assertTrue(OffhandStatusIconRules.showCapacity(0.06));
        assertTrue(OffhandStatusIconRules.showCapacity(1.4));
        assertEquals("1.4", OffhandStatusIconRules.capacityLabel(Math.sqrt(2.0)));
        assertEquals("2", OffhandStatusIconRules.capacityLabel(2.0));
        assertFalse(OffhandStatusIconRules.showWellFed(0));
        assertFalse(OffhandStatusIconRules.showWellFed(-3));
        assertTrue(OffhandStatusIconRules.showWellFed(1));
        assertEquals("12", OffhandStatusIconRules.wellFedLabel(12));
    }
}
