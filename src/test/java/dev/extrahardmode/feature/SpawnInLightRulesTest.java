package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpawnInLightRulesTest {
    @Test
    void eachTwentyBlocksBelowTheBandAddsOneLight() {
        assertEquals(11, SpawnInLightRules.allowedLight(11, 48, 48));
        assertEquals(11, SpawnInLightRules.allowedLight(11, 48, 64));
        assertEquals(11, SpawnInLightRules.allowedLight(11, 48, 29));
        assertEquals(12, SpawnInLightRules.allowedLight(11, 48, 28));
        assertEquals(12, SpawnInLightRules.allowedLight(11, 48, 9));
        assertEquals(13, SpawnInLightRules.allowedLight(11, 48, 8));
        assertEquals(14, SpawnInLightRules.allowedLight(11, 48, -12));
    }

    @Test
    void chanceFallsByFivePercentTimesTheBlockLight() {
        assertEquals(100, SpawnInLightRules.spawnChancePercent(100, 0));
        assertEquals(45, SpawnInLightRules.spawnChancePercent(100, 11));
        assertEquals(40, SpawnInLightRules.spawnChancePercent(100, 12));
        assertEquals(35, SpawnInLightRules.spawnChancePercent(100, 13));
        assertEquals(0, SpawnInLightRules.spawnChancePercent(100, 20));
        assertEquals(0, SpawnInLightRules.spawnChancePercent(50, 11));
        assertEquals(100, SpawnInLightRules.spawnChancePercent(100, -3));
        assertTrue(SpawnInLightRules.allow(45, 44));
        assertFalse(SpawnInLightRules.allow(45, 45));
        assertFalse(SpawnInLightRules.allow(0, 0));
        assertTrue(SpawnInLightRules.allow(100, 99));
    }
}
