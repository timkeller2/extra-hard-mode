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
    void weightCostsOneTwoOrFourPointsPerItem() {
        assertEquals(15, NetherPortalRules.COOLDOWN_SECONDS);
        assertEquals(300, NetherPortalRules.COOLDOWN_TICKS);
        assertEquals(1, NetherPortalRules.ONE_KIND_WEIGHT);
        assertEquals(2, NetherPortalRules.TWO_KINDS_WEIGHT);
        assertEquals(4, NetherPortalRules.THREE_KINDS_WEIGHT);
        assertEquals(1, NetherPortalRules.weightPerFuel(0));
        assertEquals(1, NetherPortalRules.weightPerFuel(1));
        assertEquals(2, NetherPortalRules.weightPerFuel(2));
        assertEquals(4, NetherPortalRules.weightPerFuel(3));
        assertEquals(4, NetherPortalRules.weightPerFuel(9));
        assertEquals(0, NetherPortalRules.travelCost(0, 3));
        assertEquals(0, NetherPortalRules.travelCost(-2, 1));
        assertEquals(1, NetherPortalRules.travelCost(0.5, 1));
        assertEquals(1, NetherPortalRules.travelCost(1, 1));
        assertEquals(2, NetherPortalRules.travelCost(1.1, 1));
        assertEquals(18, NetherPortalRules.travelCost(18, 1));
        assertEquals(9, NetherPortalRules.travelCost(18, 2));
        assertEquals(5, NetherPortalRules.travelCost(18, 3));
        assertEquals(1, NetherPortalRules.travelCost(4, 3));
        assertEquals(2, NetherPortalRules.travelCost(4.1, 3));
        assertEquals(6, NetherPortalRules.travelCost(22.5, 3));
        assertEquals(8, NetherPortalRules.travelCost(8, 0));
        assertEquals(8, NetherPortalRules.travelCost(8, 1));
        assertEquals(4, NetherPortalRules.travelCost(8, 2));
        assertEquals(2, NetherPortalRules.travelCost(8, 3));
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
    void threeKindsSpendSixItemsForANormalLoad() {
        List<Stack> slots = List.of(
                new Stack(0, LapisPortalRules.QUARTZ, 2),
                new Stack(1, LapisPortalRules.REDSTONE, 5),
                new Stack(40, LapisPortalRules.AMETHYST, 5));
        int kinds = LapisPortalRules.fuelKinds(slots);
        assertEquals(3, kinds);
        int cost = NetherPortalRules.travelCost(22.5, kinds);
        assertEquals(6, cost);
        List<Stack> paid = LapisPortalRules.pay(slots, cost);
        assertNotNull(paid);
        assertEquals(5, LapisPortalRules.total(slots, LapisPortalRules.REDSTONE));
        assertEquals(3, LapisPortalRules.total(paid, LapisPortalRules.REDSTONE));
        assertEquals(3, LapisPortalRules.total(paid, LapisPortalRules.AMETHYST));
        assertEquals(0, LapisPortalRules.total(paid, LapisPortalRules.QUARTZ));
    }

    @Test
    void threeKindsQuarterTheCostComparedWithOne() {
        List<Stack> oneKind = List.of(new Stack(2, LapisPortalRules.QUARTZ, 10));
        List<Stack> twoKinds = List.of(
                new Stack(2, LapisPortalRules.QUARTZ, 10),
                new Stack(3, LapisPortalRules.REDSTONE, 1));
        List<Stack> threeKinds = List.of(
                new Stack(2, LapisPortalRules.QUARTZ, 10),
                new Stack(3, LapisPortalRules.REDSTONE, 1),
                new Stack(40, LapisPortalRules.AMETHYST, 1));
        assertEquals(8, NetherPortalRules.travelCost(8, LapisPortalRules.fuelKinds(oneKind)));
        assertEquals(4, NetherPortalRules.travelCost(8, LapisPortalRules.fuelKinds(twoKinds)));
        assertEquals(2, NetherPortalRules.travelCost(8, LapisPortalRules.fuelKinds(threeKinds)));
        List<Stack> paid = LapisPortalRules.pay(threeKinds, 2);
        assertNotNull(paid);
        assertEquals(9, LapisPortalRules.total(paid, LapisPortalRules.QUARTZ));
        assertEquals(0, LapisPortalRules.total(paid, LapisPortalRules.REDSTONE));
        assertEquals(1, LapisPortalRules.total(paid, LapisPortalRules.AMETHYST));
    }
}
