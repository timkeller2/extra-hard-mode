package dev.extrahardmode.feature.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BlazesTest {
    @Test
    void nearBedrockIsYAtOrBelowCutoff() {
        assertTrue(Blazes.shouldReplaceNearBedrock(-56, true, -56));
        assertTrue(Blazes.shouldReplaceNearBedrock(-64, true, -56));
        assertFalse(Blazes.shouldReplaceNearBedrock(-55, true, -56));
        assertFalse(Blazes.shouldReplaceNearBedrock(0, true, -56));
    }

    @Test
    void nearBedrockEnableAndMinValueDisable() {
        assertFalse(Blazes.shouldReplaceNearBedrock(-64, false, -56));
        assertFalse(Blazes.shouldReplaceNearBedrock(-64, true, Integer.MIN_VALUE));
    }

    @Test
    void percentChanceBounds() {
        assertFalse(Blazes.percentChance(0, 0));
        assertTrue(Blazes.percentChance(100, 99));
        assertTrue(Blazes.percentChance(50, 0));
        assertTrue(Blazes.percentChance(50, 49));
        assertFalse(Blazes.percentChance(50, 50));
    }

    @Test
    void splitChanceDecaysByGeneration() {
        assertEquals(25, Blazes.splitPercent(25, 1));
        assertEquals(12, Blazes.splitPercent(25, 2));
        assertEquals(8, Blazes.splitPercent(25, 3));
        assertEquals(0, Blazes.splitPercent(25, 0));
        assertEquals(0, Blazes.splitPercent(0, 1));
    }

    @Test
    void fireOnlyAboveHalfHealth() {
        assertTrue(Blazes.shouldDropFire(11.0F, 20.0F));
        assertFalse(Blazes.shouldDropFire(10.0F, 20.0F));
        assertFalse(Blazes.shouldDropFire(0.0F, 20.0F));
        assertFalse(Blazes.shouldDropFire(10.0F, 0.0F));
    }
}
