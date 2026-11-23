package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpawnerLootRulesTest {
    @Test
    void firstEightPlayerKillsDropLoot() {
        assertEquals(8, SpawnerLootRules.DEFAULT_LOOT_KILLS);
        for (int kills = 0; kills < 8; kills++) {
            assertTrue(SpawnerLootRules.dropsLoot(kills, 8), "kills " + kills);
        }
        assertFalse(SpawnerLootRules.dropsLoot(8, 8));
        assertFalse(SpawnerLootRules.dropsLoot(9, 8));
        assertTrue(SpawnerLootRules.dropsLoot(0, 3));
        assertTrue(SpawnerLootRules.dropsLoot(2, 3));
        assertFalse(SpawnerLootRules.dropsLoot(3, 3));
        assertFalse(SpawnerLootRules.dropsLoot(0, 0));
        assertFalse(SpawnerLootRules.dropsLoot(0, -1));
    }

    @Test
    void unnaturalFloorSkipsTrialAndSpawnerMobs() {
        assertFalse(SpawnerLootRules.unnaturalFloorBlocksLoot(true, false));
        assertFalse(SpawnerLootRules.unnaturalFloorBlocksLoot(false, true));
        assertFalse(SpawnerLootRules.unnaturalFloorBlocksLoot(true, true));
        assertTrue(SpawnerLootRules.unnaturalFloorBlocksLoot(false, false));
    }

    @Test
    void eachSpawnerKeepsItsOwnKey() {
        String overworld = SpawnerLootRules.blockKey("minecraft:overworld", 10L);
        String nether = SpawnerLootRules.blockKey("minecraft:the_nether", 10L);
        String other = SpawnerLootRules.blockKey("minecraft:overworld", 11L);
        String cart = SpawnerLootRules.minecartKey("11111111-1111-1111-1111-111111111111");
        String moved = SpawnerLootRules.minecartKey("11111111-1111-1111-1111-111111111111");
        assertNotEquals(overworld, nether);
        assertNotEquals(overworld, other);
        assertNotEquals(overworld, cart);
        assertEquals(cart, moved);
        assertNotEquals(cart, SpawnerLootRules.minecartKey("22222222-2222-2222-2222-222222222222"));
    }
}
