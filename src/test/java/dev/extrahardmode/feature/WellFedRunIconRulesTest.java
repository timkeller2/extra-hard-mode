package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WellFedRunIconRulesTest {
    @Test
    void iconSitsOneSlotAboveTheOffhand() {
        assertEquals(77, WellFedRunIconRules.iconX());
        assertEquals(44, WellFedRunIconRules.iconY());
        assertTrue(WellFedRunIconRules.iconY() < WellFedRunIconRules.OFFHAND_Y);
        assertTrue(WellFedRunIconRules.hit(77, 44));
        assertTrue(WellFedRunIconRules.hit(92, 59));
        assertFalse(WellFedRunIconRules.hit(76, 44));
        assertFalse(WellFedRunIconRules.hit(77, 60));
        assertFalse(WellFedRunIconRules.hit(77, 62));
    }
}
