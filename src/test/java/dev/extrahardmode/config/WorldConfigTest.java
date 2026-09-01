package dev.extrahardmode.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WorldConfigTest {
    @Test
    void clampPercent() {
        assertEquals(0, WorldConfig.clampPercent(-4));
        assertEquals(100, WorldConfig.clampPercent(140));
        assertEquals(20, WorldConfig.clampPercent(20));
        assertEquals(0, WorldConfig.clampPercent(0));
        assertEquals(100, WorldConfig.clampPercent(100));
    }
}
