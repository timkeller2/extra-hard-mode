package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
                new Stack(0, NetherPortalRules.QUARTZ_BLOCK, 2),
                new Stack(1, NetherPortalRules.REDSTONE_BLOCK, 5),
                new Stack(40, NetherPortalRules.AMETHYST_BLOCK, 5));
        int kinds = LapisPortalRules.fuelKinds(slots, NetherPortalRules.FUELS);
        assertEquals(3, kinds);
        int cost = NetherPortalRules.travelCost(22.5, kinds);
        assertEquals(6, cost);
        List<Stack> paid = LapisPortalRules.pay(slots, cost, NetherPortalRules.FUELS);
        assertNotNull(paid);
        assertEquals(5, LapisPortalRules.total(slots, NetherPortalRules.REDSTONE_BLOCK));
        assertEquals(3, LapisPortalRules.total(paid, NetherPortalRules.REDSTONE_BLOCK));
        assertEquals(3, LapisPortalRules.total(paid, NetherPortalRules.AMETHYST_BLOCK));
        assertEquals(0, LapisPortalRules.total(paid, NetherPortalRules.QUARTZ_BLOCK));
        assertNull(LapisPortalRules.pay(
                List.of(new Stack(0, LapisPortalRules.REDSTONE, 64)), 1, NetherPortalRules.FUELS));
    }

    @Test
    void threeKindsQuarterTheCostComparedWithOne() {
        List<Stack> oneKind = List.of(new Stack(2, NetherPortalRules.QUARTZ_BLOCK, 10));
        List<Stack> twoKinds = List.of(
                new Stack(2, NetherPortalRules.QUARTZ_BLOCK, 10),
                new Stack(3, NetherPortalRules.REDSTONE_BLOCK, 1));
        List<Stack> threeKinds = List.of(
                new Stack(2, NetherPortalRules.QUARTZ_BLOCK, 10),
                new Stack(3, NetherPortalRules.REDSTONE_BLOCK, 1),
                new Stack(40, NetherPortalRules.AMETHYST_BLOCK, 1));
        assertEquals(8, NetherPortalRules.travelCost(8, LapisPortalRules.fuelKinds(oneKind, NetherPortalRules.FUELS)));
        assertEquals(4, NetherPortalRules.travelCost(8, LapisPortalRules.fuelKinds(twoKinds, NetherPortalRules.FUELS)));
        assertEquals(2, NetherPortalRules.travelCost(8, LapisPortalRules.fuelKinds(threeKinds, NetherPortalRules.FUELS)));
        List<Stack> paid = LapisPortalRules.pay(threeKinds, 2, NetherPortalRules.FUELS);
        assertNotNull(paid);
        assertEquals(9, LapisPortalRules.total(paid, NetherPortalRules.QUARTZ_BLOCK));
        assertEquals(0, LapisPortalRules.total(paid, NetherPortalRules.REDSTONE_BLOCK));
        assertEquals(1, LapisPortalRules.total(paid, NetherPortalRules.AMETHYST_BLOCK));
    }

    @Test
    void overworldRollRaisesTheCounterAndSpendsItOnFriendlies() {
        assertEquals(20, NetherPortalRules.DIE_SIDES);
        assertEquals(64, NetherPortalRules.BACKLASH_RANGE);
        assertEquals(1, NetherPortalRules.afterRoll(0, 1));
        assertEquals(1, NetherPortalRules.afterRoll(0, 20));
        assertEquals(4, NetherPortalRules.afterRoll(4, 4));
        assertEquals(5, NetherPortalRules.afterRoll(4, 5));
        assertEquals(20, NetherPortalRules.afterRoll(19, 20));
        assertEquals(20, NetherPortalRules.afterRoll(20, 20));
        assertEquals(3.0F, NetherPortalRules.travelerShare(10.0F, new float[] {4.0F, 3.0F}));
        assertEquals(0.0F, NetherPortalRules.travelerShare(10.0F, new float[] {4.0F, 3.0F, 100.0F}));
        assertEquals(5.0F, NetherPortalRules.travelerShare(5.0F, new float[] {}));
        assertEquals(5.0F, NetherPortalRules.travelerShare(5.0F, null));
        assertEquals(0.0F, NetherPortalRules.travelerShare(0.0F, new float[] {8.0F}));
        assertEquals("2 quartz blocks and 3 redstone blocks", NetherPortalRules.spentPhrase(3, 2, 0));
        assertEquals("1 amethyst block", NetherPortalRules.spentPhrase(0, 0, 1));
        assertEquals(
                "2 quartz blocks, 3 redstone blocks, and 4 amethyst blocks",
                NetherPortalRules.spentPhrase(3, 2, 4));
        String text = String.join(" ", EhmHelp.resolve("portals").lines());
        assertTrue(text.contains("redstone blocks, quartz blocks, and amethyst blocks"));
        assertTrue(text.contains("rolls a d20"));
        assertTrue(text.contains("within 64 blocks"));
        assertTrue(text.contains("happy ghast"));
        assertTrue(text.contains("amethyst shards"));
        assertTrue(NetherPortalRules.backlashAnimal("minecraft:wolf"));
        assertTrue(NetherPortalRules.backlashAnimal("minecraft:trader_llama"));
        assertTrue(NetherPortalRules.backlashAnimal("minecraft:happy_ghast"));
        assertEquals(28, NetherPortalRules.BACKLASH_ANIMALS.size());
        assertFalse(NetherPortalRules.backlashAnimal("minecraft:player"));
        assertFalse(NetherPortalRules.backlashAnimal("minecraft:villager"));
        assertFalse(NetherPortalRules.backlashAnimal("minecraft:wandering_trader"));
        assertFalse(NetherPortalRules.backlashAnimal("minecraft:iron_golem"));
        assertFalse(NetherPortalRules.backlashAnimal("minecraft:copper_golem"));
        assertFalse(NetherPortalRules.backlashAnimal("minecraft:snow_golem"));
        assertFalse(NetherPortalRules.backlashAnimal("minecraft:squid"));
        assertFalse(NetherPortalRules.backlashAnimal("minecraft:axolotl"));
        assertFalse(NetherPortalRules.backlashAnimal("minecraft:tadpole"));
        assertFalse(NetherPortalRules.backlashAnimal("minecraft:allay"));
        assertFalse(NetherPortalRules.backlashAnimal("minecraft:bat"));
        assertFalse(NetherPortalRules.backlashAnimal(null));
    }
}
