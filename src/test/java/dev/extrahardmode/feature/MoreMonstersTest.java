package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.extrahardmode.player.DamageTracker;
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
    void environmentalMajorityBlocksLoot() {
        DamageTracker mostlyEnv = DamageTracker.EMPTY.addEnvironmental(12.0F).addPlayer(4.0F);
        assertTrue(mostlyEnv.mostlyEnvironmental());
        DamageTracker mostlyPlayer = DamageTracker.EMPTY.addEnvironmental(4.0F).addPlayer(12.0F);
        assertFalse(mostlyPlayer.mostlyEnvironmental());
        assertFalse(DamageTracker.EMPTY.mostlyEnvironmental());
    }
}
