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
    void biomeVisitPaysOneHundredAndWorldFirstAddsOneHundred() {
        assertEquals(100, ExplorationRules.BIOME_VISIT_XP);
        assertEquals(100, ExplorationRules.WORLD_FIRST_BONUS_XP);
        assertEquals(0, ExplorationRules.biomeXp(false, false));
        assertEquals(0, ExplorationRules.biomeXp(false, true));
        assertEquals(100, ExplorationRules.biomeXp(true, false));
        assertEquals(200, ExplorationRules.biomeXp(true, true));
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
