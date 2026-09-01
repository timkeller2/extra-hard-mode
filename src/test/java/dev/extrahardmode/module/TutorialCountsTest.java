package dev.extrahardmode.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TutorialCountsTest {
    @Test
    void incrementsUntilMaxThenStops() {
        Map<String, Integer> counts = new HashMap<>();
        assertTrue(TutorialCounts.tryIncrement(counts, "zombie_slow", 3));
        assertTrue(TutorialCounts.tryIncrement(counts, "zombie_slow", 3));
        assertTrue(TutorialCounts.tryIncrement(counts, "zombie_slow", 3));
        assertFalse(TutorialCounts.tryIncrement(counts, "zombie_slow", 3));
        assertEquals(3, TutorialCounts.shown(counts, "zombie_slow"));
    }

    @Test
    void onceKindStopsAfterFirstShow() {
        Map<String, Integer> counts = new HashMap<>();
        assertTrue(TutorialCounts.tryIncrement(counts, MessageId.HEAVY_INVENTORY.id(), 1));
        assertFalse(TutorialCounts.tryIncrement(counts, MessageId.HEAVY_INVENTORY.id(), 1));
    }

    @Test
    void zeroMaxNeverShows() {
        Map<String, Integer> counts = new HashMap<>();
        assertFalse(TutorialCounts.tryIncrement(counts, "zombie_slow", 0));
        assertEquals(0, TutorialCounts.shown(counts, "zombie_slow"));
    }

    @Test
    void silentNodesOnlyOnPluginYmlFive() {
        int silent = 0;
        for (MessageId id : MessageId.values()) {
            if (id.hasSilentNode()) {
                silent++;
            }
        }
        assertEquals(6, silent);
        assertTrue(MessageId.STONE_MINING_HELP.hasSilentNode());
        assertTrue(MessageId.NO_PLACING_ORE_AGAINST_STONE.hasSilentNode());
        assertTrue(MessageId.REALISTIC_BUILDING.hasSilentNode());
        assertTrue(MessageId.REALISTIC_BUILDING_BENEATH.hasSilentNode());
        assertTrue(MessageId.LIMITED_TORCH_PLACEMENT.hasSilentNode());
        assertTrue(MessageId.NO_TORCHES_HERE.hasSilentNode());
        assertFalse(MessageId.NO_CRAFTING_MELON_SEEDS.hasSilentNode());
        assertFalse(MessageId.HEAVY_INVENTORY.hasSilentNode());
        assertFalse(MessageId.ZOMBIE_SLOW.hasSilentNode());
        assertFalse(MessageId.DRAGON_CHALLENGE.hasSilentNode());
        assertFalse(MessageId.DRAGON_DEFEAT.hasSilentNode());
    }

    @Test
    void catalogLooksUpById() {
        assertEquals(MessageId.STONE_MINING_HELP, MessageId.byId("stone_mining_help"));
        assertNotNull(MessageId.byId("no_torches_here"));
    }

    @Test
    void extrasWithoutSilentAreOnceOrBroadcast() {
        assertEquals(MessageId.Kind.ONCE, MessageId.NO_CRAFTING_MELON_SEEDS.kind());
        assertEquals(MessageId.Kind.ONCE, MessageId.HEAVY_INVENTORY.kind());
        assertEquals(MessageId.Kind.ONCE, MessageId.ZOMBIE_SLOW.kind());
        assertEquals(MessageId.Kind.BROADCAST, MessageId.DRAGON_CHALLENGE.kind());
        assertEquals(MessageId.Kind.BROADCAST, MessageId.DRAGON_DEFEAT.kind());
        assertEquals(MessageId.Kind.ACTION_BAR, MessageId.STONE_MINING_HELP.kind());
    }

    @Test
    void copyIsIndependent() {
        Map<String, Integer> stored = Map.of("zombie_slow", 2);
        Map<String, Integer> copy = TutorialCounts.mutableCopy(stored);
        copy.put("zombie_slow", 3);
        assertEquals(2, stored.get("zombie_slow"));
        assertEquals(3, copy.get("zombie_slow"));
    }
}
