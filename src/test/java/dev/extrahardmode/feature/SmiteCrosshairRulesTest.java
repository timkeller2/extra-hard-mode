package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SmiteCrosshairRulesTest {
    @Test
    void goldOnlyForASwordAFullManaAndACreatureInReach() {
        assertTrue(SmiteCrosshairRules.highlight(true, 1.0, 3.0, true));
        assertTrue(SmiteCrosshairRules.highlight(true, 4.5, 1.0, true));
        assertFalse(SmiteCrosshairRules.highlight(false, 5.0, 3.0, true));
        assertFalse(SmiteCrosshairRules.highlight(true, 0.99, 3.0, true));
        assertFalse(SmiteCrosshairRules.highlight(true, 0.25, 3.0, true));
        assertFalse(SmiteCrosshairRules.highlight(true, 1.0, 0.0, true));
        assertFalse(SmiteCrosshairRules.highlight(true, 1.0, 3.0, false));
        assertEquals(SmiteCrosshairRules.GOLD, SmiteCrosshairRules.color(true));
        assertEquals(SmiteCrosshairRules.WHITE, SmiteCrosshairRules.color(false));
        assertEquals(AbilityReadyRules.LEARNED_LEVEL_COLOR, SmiteCrosshairRules.GOLD);
    }
}
