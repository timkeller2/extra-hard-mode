package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ExplorationRulesTest {
    @Test
    void spawnAwardIsThreeHundredBlocksHorizontal() {
        assertEquals(300, ExplorationRules.SPAWN_DISTANCE);
        assertEquals(100, ExplorationRules.SPAWN_XP);
        assertFalse(ExplorationRules.shouldAwardSpawn(false, 0, 0));
        assertFalse(ExplorationRules.shouldAwardSpawn(false, 299, 0));
        assertTrue(ExplorationRules.shouldAwardSpawn(false, 300, 0));
        assertTrue(ExplorationRules.shouldAwardSpawn(false, 0, -300));
        assertTrue(ExplorationRules.shouldAwardSpawn(false, 180, 240));
        assertFalse(ExplorationRules.shouldAwardSpawn(false, 180, 239));
        assertFalse(ExplorationRules.shouldAwardSpawn(true, 1000, 0));
    }

    @Test
    void biomeVisitPaysFiftyAndWorldFirstAddsOneHundred() {
        assertEquals(50, ExplorationRules.BIOME_VISIT_XP);
        assertEquals(100, ExplorationRules.WORLD_FIRST_BONUS_XP);
        assertEquals(3, ExplorationRules.HOME_BIOME_XP_PER_POINT);
        assertEquals(0, ExplorationRules.homeBiomeXp(0));
        assertEquals(36, ExplorationRules.homeBiomeXp(12));
        assertEquals(54, ExplorationRules.homeBiomeXp(18));
        assertEquals(0, ExplorationRules.biomeXp(false, false));
        assertEquals(0, ExplorationRules.biomeXp(false, true));
        assertEquals(50, ExplorationRules.biomeXp(true, false));
        assertEquals(150, ExplorationRules.biomeXp(true, true));
        assertTrue(ExplorationRules.unseenBiome(java.util.List.of(), "minecraft:plains"));
        assertFalse(ExplorationRules.unseenBiome(java.util.List.of("minecraft:plains"), "minecraft:plains"));
        assertTrue(ExplorationRules.unseenBiome(java.util.List.of("minecraft:plains"), "minecraft:desert"));
        assertFalse(ExplorationRules.unseenBiome(java.util.List.of(), ""));
        assertFalse(ExplorationRules.unseenBiome(java.util.List.of(), null));
    }

    @Test
    void biomeFallbackNameTitleCasesThePath() {
        assertEquals("Plains", ExplorationRules.biomeFallbackName("plains"));
        assertEquals("Dark Forest", ExplorationRules.biomeFallbackName("dark_forest"));
        assertEquals("Nether Wastes", ExplorationRules.biomeFallbackName("nether_wastes"));
        assertEquals("an unknown land", ExplorationRules.biomeFallbackName(""));
        assertEquals("an unknown land", ExplorationRules.biomeFallbackName(null));
        assertEquals("biome.minecraft.plains", ExplorationRules.biomeTranslationKey("minecraft", "plains"));
    }
}
