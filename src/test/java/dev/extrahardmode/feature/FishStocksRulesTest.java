package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FishStocksRulesTest {
    @Test
    void healthySchoolUsesOneThird() {
        assertEquals(33, FishStocksRules.spawnRatePercent(3, 3, 33, 5));
        assertEquals(33, FishStocksRules.spawnRatePercent(8, 3, 33, 5));
    }

    @Test
    void scarceSchoolUsesOneTwentieth() {
        assertEquals(5, FishStocksRules.spawnRatePercent(0, 3, 33, 5));
        assertEquals(5, FishStocksRules.spawnRatePercent(2, 3, 33, 5));
    }

    @Test
    void allowSpawnHonorsRoll() {
        assertTrue(FishStocksRules.allowSpawn(3, 3, 33, 5, 0));
        assertTrue(FishStocksRules.allowSpawn(3, 3, 33, 5, 32));
        assertFalse(FishStocksRules.allowSpawn(3, 3, 33, 5, 33));
        assertTrue(FishStocksRules.allowSpawn(0, 3, 33, 5, 4));
        assertFalse(FishStocksRules.allowSpawn(0, 3, 33, 5, 5));
    }

    @Test
    void biteWaitTriples() {
        assertEquals(300, FishStocksRules.scaleWait(100, 3));
        assertEquals(1, FishStocksRules.scaleWait(1, 1));
    }
}
