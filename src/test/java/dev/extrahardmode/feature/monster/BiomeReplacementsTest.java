package dev.extrahardmode.feature.monster;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BiomeReplacementsTest {
    @Test
    void percentZeroNeverHits() {
        assertFalse(BiomeReplacements.hitsPercent(0, 0));
        assertFalse(BiomeReplacements.hitsPercent(0, 99));
    }

    @Test
    void percentHundredAlwaysHits() {
        assertTrue(BiomeReplacements.hitsPercent(100, 0));
        assertTrue(BiomeReplacements.hitsPercent(100, 99));
    }

    @Test
    void percentIsExclusiveUpperBound() {
        assertTrue(BiomeReplacements.hitsPercent(1, 0));
        assertFalse(BiomeReplacements.hitsPercent(1, 1));
        assertTrue(BiomeReplacements.hitsPercent(20, 19));
        assertFalse(BiomeReplacements.hitsPercent(20, 20));
    }
}
