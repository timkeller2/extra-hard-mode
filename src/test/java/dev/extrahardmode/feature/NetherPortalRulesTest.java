package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.extrahardmode.feature.LapisPortalRules.Stack;
import java.util.List;
import org.junit.jupiter.api.Test;

class NetherPortalRulesTest {
    @Test
    void weightUsesTheLapisFuelRange() {
        assertEquals(15, NetherPortalRules.COOLDOWN_SECONDS);
        assertEquals(300, NetherPortalRules.COOLDOWN_TICKS);
        assertEquals(0, NetherPortalRules.travelCost(0, 3));
        assertEquals(0, NetherPortalRules.travelCost(-2, 1));
        assertEquals(1, NetherPortalRules.travelCost(0.5, 1));
        assertEquals(1, NetherPortalRules.travelCost(18, 1));
        assertEquals(1, NetherPortalRules.travelCost(18, 2));
        assertEquals(1, NetherPortalRules.travelCost(18, 3));
        assertEquals(1, NetherPortalRules.travelCost(100, 1));
        assertEquals(2, NetherPortalRules.travelCost(100.5, 1));
        assertEquals(2, NetherPortalRules.travelCost(200, 1));
        assertEquals(1, NetherPortalRules.travelCost(200, 2));
        assertEquals(2, NetherPortalRules.travelCost(201, 2));
        assertEquals(1, NetherPortalRules.travelCost(300, 3));
        assertEquals(2, NetherPortalRules.travelCost(301, 3));
        assertEquals(4, NetherPortalRules.travelCost(301, 1));
    }

    @Test
    void cooldownLastsFifteenSeconds() {
        assertEquals(800L, NetherPortalRules.readyAt(500L));
        assertEquals(0, NetherPortalRules.secondsLeft(800L, 800L));
        assertEquals(0, NetherPortalRules.secondsLeft(0L, 1_000L));
        assertEquals(1, NetherPortalRules.secondsLeft(1_001L, 1_000L));
        assertEquals(15, NetherPortalRules.secondsLeft(NetherPortalRules.readyAt(500L), 500L));
        assertEquals("1 second remains.", NetherPortalRules.waitPhrase(1));
        assertEquals("15 seconds remain.", NetherPortalRules.waitPhrase(15));
        assertFalse(NetherPortalRules.repeatTooSoon(0L, 100L));
        assertTrue(NetherPortalRules.repeatTooSoon(100L, 100L));
        assertTrue(NetherPortalRules.repeatTooSoon(100L, 119L));
        assertFalse(NetherPortalRules.repeatTooSoon(100L, 120L));
        assertFalse(NetherPortalRules.repeatTooSoon(200L, 100L));
    }

    @Test
    void aNormalWeightTakesOneItemFromTheLargestStack() {
        List<Stack> slots = List.of(
                new Stack(0, LapisPortalRules.QUARTZ, 2),
                new Stack(1, LapisPortalRules.REDSTONE, 5),
                new Stack(40, LapisPortalRules.AMETHYST, 5));
        int kinds = LapisPortalRules.fuelKinds(slots);
        assertEquals(3, kinds);
        int cost = NetherPortalRules.travelCost(22.5, kinds);
        assertEquals(1, cost);
        List<Stack> paid = LapisPortalRules.pay(slots, cost);
        assertNotNull(paid);
        assertEquals(5, LapisPortalRules.total(slots, LapisPortalRules.REDSTONE));
        assertEquals(4, LapisPortalRules.total(paid, LapisPortalRules.REDSTONE));
        assertEquals(5, LapisPortalRules.total(paid, LapisPortalRules.AMETHYST));
        assertEquals(2, LapisPortalRules.total(paid, LapisPortalRules.QUARTZ));
    }

    @Test
    void threeKindsHalveTheCostOfAHeavyLoadComparedWithOne() {
        List<Stack> oneKind = List.of(new Stack(2, LapisPortalRules.QUARTZ, 10));
        List<Stack> threeKinds = List.of(
                new Stack(2, LapisPortalRules.QUARTZ, 10),
                new Stack(3, LapisPortalRules.REDSTONE, 1),
                new Stack(40, LapisPortalRules.AMETHYST, 1));
        assertEquals(4, NetherPortalRules.travelCost(301, LapisPortalRules.fuelKinds(oneKind)));
        assertEquals(2, NetherPortalRules.travelCost(301, LapisPortalRules.fuelKinds(threeKinds)));
        List<Stack> paid = LapisPortalRules.pay(threeKinds, 2);
        assertNotNull(paid);
        assertEquals(9, LapisPortalRules.total(paid, LapisPortalRules.QUARTZ));
        assertEquals(0, LapisPortalRules.total(paid, LapisPortalRules.REDSTONE));
        assertEquals(1, LapisPortalRules.total(paid, LapisPortalRules.AMETHYST));
    }
}
