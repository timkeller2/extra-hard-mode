package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChunkHeldHudRulesTest {
    @Test
    void diamondSitsInTheTopLeftCorner() {
        assertEquals(8, ChunkHeldHudRules.width());
        assertEquals(8, ChunkHeldHudRules.height());
        assertEquals(4, ChunkHeldHudRules.x());
        assertEquals(4, ChunkHeldHudRules.y());
        assertTrue(ChunkHeldHudRules.hit(4, 4));
        assertTrue(ChunkHeldHudRules.hit(11, 11));
        assertFalse(ChunkHeldHudRules.hit(3, 4));
        assertFalse(ChunkHeldHudRules.hit(12, 4));
        assertFalse(ChunkHeldHudRules.hit(4, 12));
        assertEquals("The alliance holds this ground.", ChunkClearRules.PEACE_MESSAGE);
    }
}