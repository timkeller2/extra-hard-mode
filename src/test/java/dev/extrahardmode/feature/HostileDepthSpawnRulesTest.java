package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HostileDepthSpawnRulesTest {
    @Test
    void surfaceAndAboveStayDark() {
        assertEquals(0, HostileDepthSpawnRules.extraBlockLight(0));
        assertEquals(0, HostileDepthSpawnRules.extraBlockLight(1));
        assertEquals(0, HostileDepthSpawnRules.extraBlockLight(64));
        assertEquals(0, HostileDepthSpawnRules.extraBlockLight(-14));
    }

    @Test
    void eachFifteenBlocksBelowAddsOne() {
        assertEquals(1, HostileDepthSpawnRules.extraBlockLight(-15));
        assertEquals(1, HostileDepthSpawnRules.extraBlockLight(-29));
        assertEquals(2, HostileDepthSpawnRules.extraBlockLight(-30));
        assertEquals(4, HostileDepthSpawnRules.extraBlockLight(-60));
    }

    @Test
    void depthNeverLowersAHigherVanillaLimit() {
        assertEquals(0, HostileDepthSpawnRules.blockLightLimit(0, 10));
        assertEquals(2, HostileDepthSpawnRules.blockLightLimit(0, -30));
        assertEquals(7, HostileDepthSpawnRules.blockLightLimit(7, -15));
    }
}
