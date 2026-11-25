package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChunkClearRulesTest {
    @Test
    void defaultStopsGroundSpawnsAtTwentyKills() {
        assertEquals(20, ChunkClearRules.DEFAULT_KILLS);
        assertEquals(48, ChunkClearRules.MIN_Y);
        assertEquals(25, ChunkClearRules.REWARD_XP);
        assertEquals(1, ChunkClearRules.REWARD_EMERALDS);
        assertFalse(ChunkClearRules.blocksSpawn(47, 100, 100));
        assertFalse(ChunkClearRules.blocksSpawn(48, 99, 100));
        assertTrue(ChunkClearRules.blocksSpawn(48, 100, 100));
        assertTrue(ChunkClearRules.blocksSpawn(120, 100, 100));
        assertFalse(ChunkClearRules.blocksSpawn(80, 500, 0));
        assertFalse(ChunkClearRules.blocksSpawn(80, 500, -1));
        assertFalse(ChunkClearRules.blocksSpawn(80, 1, -4));
        assertTrue(ChunkClearRules.blocksHostileSpawn(48, 100, 100, true));
        assertFalse(ChunkClearRules.blocksHostileSpawn(48, 100, 100, false));
        assertFalse(ChunkClearRules.blocksHostileSpawn(48, 99, 100, true));
        assertFalse(ChunkClearRules.blocksHostileSpawn(47, 100, 100, true));
        assertFalse(ChunkClearRules.justFilled(99, -1));
        assertFalse(ChunkClearRules.blocksSpawn(48, 19, ChunkClearRules.DEFAULT_KILLS));
        assertTrue(ChunkClearRules.blocksSpawn(48, 20, ChunkClearRules.DEFAULT_KILLS));
    }

    @Test
    void onlyTheCrossingKillAnnounces() {
        assertFalse(ChunkClearRules.justFilled(98, 100));
        assertTrue(ChunkClearRules.justFilled(99, 100));
        assertFalse(ChunkClearRules.justFilled(100, 100));
        assertTrue(ChunkClearRules.justFilled(0, 1));
        assertFalse(ChunkClearRules.justFilled(0, 0));
    }

    @Test
    void playersInTheChunkOrWithinFortyEightBlocksHearIt() {
        assertTrue(ChunkClearRules.hearsClearing(true, 10_000.0));
        assertTrue(ChunkClearRules.hearsClearing(false, 48.0 * 48.0));
        assertFalse(ChunkClearRules.hearsClearing(false, 48.0 * 48.0 + 0.1));
        assertTrue(ChunkClearRules.hearsKiller(0.0));
        assertTrue(ChunkClearRules.hearsKiller(48.0 * 48.0));
        assertFalse(ChunkClearRules.hearsKiller(48.0 * 48.0 + 0.1));
        assertFalse(ChunkClearRules.hearsKiller(-1.0));
    }

    @Test
    void laterMultiplesClearTheNextNeighbor() {
        assertFalse(ChunkClearRules.spreads(19, 20));
        assertTrue(ChunkClearRules.justFilled(19, 20));
        assertFalse(ChunkClearRules.spreads(20, 20));
        assertTrue(ChunkClearRules.spreads(39, 20));
        assertFalse(ChunkClearRules.justFilled(39, 20));
        assertTrue(ChunkClearRules.spreads(59, 20));
        assertFalse(ChunkClearRules.spreads(0, 1));
        assertTrue(ChunkClearRules.spreads(1, 1));
        assertFalse(ChunkClearRules.spreads(39, 0));
        assertFalse(ChunkClearRules.spreads(39, -1));
        assertEquals(8, ChunkClearRules.neighborCount());
        assertEquals(3, ChunkClearRules.neighborChunkX(3, 0));
        assertEquals(2, ChunkClearRules.neighborChunkZ(3, 0));
        assertEquals(4, ChunkClearRules.neighborChunkX(3, 1));
        assertEquals(3, ChunkClearRules.neighborChunkZ(3, 1));
        assertEquals(3, ChunkClearRules.neighborChunkX(3, 2));
        assertEquals(4, ChunkClearRules.neighborChunkZ(3, 2));
        assertEquals(2, ChunkClearRules.neighborChunkX(3, 3));
        assertEquals(3, ChunkClearRules.neighborChunkZ(3, 3));
        assertEquals(4, ChunkClearRules.neighborChunkX(3, 4));
        assertEquals(2, ChunkClearRules.neighborChunkZ(3, 4));
        assertEquals(2, ChunkClearRules.neighborChunkX(3, 5));
        assertEquals(2, ChunkClearRules.neighborChunkZ(3, 5));
        assertEquals(4, ChunkClearRules.neighborChunkX(3, 6));
        assertEquals(4, ChunkClearRules.neighborChunkZ(3, 6));
        assertEquals(2, ChunkClearRules.neighborChunkX(3, 7));
        assertEquals(4, ChunkClearRules.neighborChunkZ(3, 7));
        assertEquals(0, ChunkClearRules.firstUncleared(new boolean[] {false, false, true, true}));
        assertEquals(1, ChunkClearRules.firstUncleared(new boolean[] {true, false, false, false}));
        assertEquals(-1, ChunkClearRules.firstUncleared(new boolean[] {true, true, true, true}));
        assertEquals(4, ChunkClearRules.firstUncleared(new boolean[] {true, true, true, true, false, true, true, true}));
        assertEquals(-1, ChunkClearRules.firstUncleared(new boolean[] {true, true, true, true, true, true, true, true}));
        assertEquals(-1, ChunkClearRules.firstUncleared(null));
        assertEquals("This area is being cleared for the alliance!", ChunkClearRules.CLEARED_MESSAGE);
        assertEquals("The alliance holds this ground.", ChunkClearRules.PEACE_MESSAGE);
    }
}
