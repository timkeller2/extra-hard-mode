package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ChunkClearRulesTest {
    @Test
    void defaultStopsGroundSpawnsAtOneHundredKills() {
        assertEquals(100, ChunkClearRules.DEFAULT_KILLS);
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
        assertFalse(ChunkClearRules.justFilled(99, -1));
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
    }

    @Test
    void peaceCueIsOncePerChunkPerMinecraftDay() {
        Map<String, Long> heard = Map.of("minecraft:overworld/1", 0L);
        assertFalse(ChunkClearRules.peaceDue("minecraft:overworld/1", 0L, heard));
        assertTrue(ChunkClearRules.peaceDue("minecraft:overworld/2", 0L, heard));
        assertTrue(ChunkClearRules.peaceDue("minecraft:the_nether/1", 0L, heard));
        assertTrue(ChunkClearRules.peaceDue("minecraft:overworld/1", 1L, heard));
        assertTrue(ChunkClearRules.peaceDue("minecraft:overworld/1", 0L, null));
        assertFalse(ChunkClearRules.peaceDue(null, 0L, heard));
        assertFalse(ChunkClearRules.peaceDue("", 0L, heard));
        assertEquals("This area is being cleared for the alliance!", ChunkClearRules.CLEARED_MESSAGE);
        assertEquals("The alliance holds this ground.", ChunkClearRules.PEACE_MESSAGE);
    }
}
