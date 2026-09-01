package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.extrahardmode.player.DamageTracker;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MoreMonstersTest {
    @Test
    void packMultiplierDoublesCount() {
        assertEquals(2, MoreMonsters.scalePackCount(1, 2));
        assertEquals(8, MoreMonsters.scalePackCount(4, 2));
        assertEquals(4, MoreMonsters.scalePackCount(4, 1));
        assertEquals(0, MoreMonsters.scalePackCount(0, 2));
    }

    @Test
    void clusterCapScalesWithPackMultiplier() {
        assertEquals(8, MoreMonsters.scalePackCount(4, 2));
    }

    @Test
    void environmentalMajorityBlocksLoot() {
        DamageTracker mostlyEnv = DamageTracker.EMPTY.addEnvironmental(12.0F).addPlayer(4.0F);
        assertTrue(mostlyEnv.mostlyEnvironmental());
        DamageTracker mostlyPlayer = DamageTracker.EMPTY.addEnvironmental(4.0F).addPlayer(12.0F);
        assertFalse(mostlyPlayer.mostlyEnvironmental());
        assertFalse(DamageTracker.EMPTY.mostlyEnvironmental());
    }

    @Test
    void visitedSectionsAreDimensionScoped() {
        Map<String, LongLinkedOpenHashSet> visits = new HashMap<>();
        SpawnInLight.addVisit(visits, "minecraft:overworld", 1L, SpawnInLight.VISITED_CAP);
        SpawnInLight.addVisit(visits, "minecraft:the_nether", 2L, SpawnInLight.VISITED_CAP);
        assertTrue(visits.get("minecraft:overworld").contains(1L));
        assertFalse(visits.get("minecraft:overworld").contains(2L));
        assertTrue(visits.get("minecraft:the_nether").contains(2L));
        assertFalse(visits.get("minecraft:the_nether").contains(1L));
    }
}
