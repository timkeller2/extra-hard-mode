package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.extrahardmode.feature.LapisPortalRules.Facing;
import dev.extrahardmode.feature.LapisPortalRules.Offset;
import dev.extrahardmode.feature.LapisPortalRules.Place;
import dev.extrahardmode.feature.LapisPortalRules.Stack;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class LapisPortalRulesTest {
    @Test
    void channelIsTheLargestLapisStack() {
        assertEquals(0, LapisPortalRules.channel(List.of()));
        assertEquals(0, LapisPortalRules.channel(null));
        assertEquals(
                3,
                LapisPortalRules.channel(List.of(
                        new Stack(0, LapisPortalRules.LAPIS_LAZULI, 3),
                        new Stack(1, LapisPortalRules.LAPIS_LAZULI, 1),
                        new Stack(2, LapisPortalRules.LAPIS_LAZULI, 3),
                        new Stack(3, "minecraft:lapis_block", 64),
                        new Stack(4, LapisPortalRules.REDSTONE, 64))));
        assertEquals(
                64,
                LapisPortalRules.channel(List.of(
                        new Stack(0, LapisPortalRules.LAPIS_LAZULI, 64),
                        new Stack(1, LapisPortalRules.LAPIS_LAZULI, 64))));
    }

    @Test
    void frameLeavesTheFacingSideOpenAndPutsTheChestBehindTheUpperDoor() {
        for (Facing facing : Facing.values()) {
            List<Offset> lapis = LapisPortalRules.lapisOffsets(facing);
            assertEquals(8, lapis.size());
            assertEquals(8, new HashSet<>(lapis).size());
            assertTrue(lapis.contains(new Offset(0, -1, 0)));
            assertTrue(lapis.contains(new Offset(0, 2, 0)));
            assertFalse(lapis.contains(new Offset(facing.x, -1, facing.z)));
            assertFalse(lapis.contains(new Offset(facing.x, 0, facing.z)));
            assertFalse(lapis.contains(new Offset(0, 0, 0)));
            assertFalse(lapis.contains(new Offset(0, 1, 0)));
            Offset chest = LapisPortalRules.chestOffset(facing);
            Facing back = facing.opposite();
            assertEquals(new Offset(back.x, 1, back.z), chest);
            assertFalse(lapis.contains(chest));
        }
        assertEquals(new Offset(0, 1, -1), LapisPortalRules.chestOffset(Facing.SOUTH));
        assertNull(LapisPortalRules.chestOffset(null));
        assertTrue(LapisPortalRules.lapisOffsets(null).isEmpty());
    }

    @Test
    void costIsOnePerHundredBlocksAndHalvesWhenBothFuelsArePresent() {
        assertEquals(100, LapisPortalRules.BLOCKS_PER_FUEL);
        assertEquals(0, LapisPortalRules.travelCost(0, false));
        assertEquals(0, LapisPortalRules.travelCost(0, true));
        assertEquals(0, LapisPortalRules.travelCost(-5, false));
        assertEquals(1, LapisPortalRules.travelCost(1, false));
        assertEquals(1, LapisPortalRules.travelCost(100, false));
        assertEquals(2, LapisPortalRules.travelCost(100.0001, false));
        assertEquals(2, LapisPortalRules.travelCost(101, false));
        assertEquals(2, LapisPortalRules.travelCost(200, false));
        assertEquals(1, LapisPortalRules.travelCost(1, true));
        assertEquals(1, LapisPortalRules.travelCost(100, true));
        assertEquals(1, LapisPortalRules.travelCost(200, true));
        assertEquals(1, LapisPortalRules.travelCost(300, true));
        assertEquals(2, LapisPortalRules.travelCost(400, true));
        assertEquals(2, LapisPortalRules.travelCost(500, true));
        assertEquals(1.0, LapisPortalRules.travelDistance(false, 0), 0.0);
        assertEquals(0.0, LapisPortalRules.travelDistance(true, 0), 0.0);
        assertEquals(80.0, LapisPortalRules.travelDistance(false, 80), 0.0);
        assertEquals(100.0, LapisPortalRules.distance(0, 64, 0, 0, 164, 0), 0.0001);
        assertEquals(5.0, LapisPortalRules.distance(0, 0, 0, 3, 4, 0), 0.0001);
        assertEquals(2_000_000_000d, LapisPortalRules.distance(1_000_000_000, 0, 0, -1_000_000_000, 0, 0), 1.0);
    }

    @Test
    void paymentTakesTheLargerTotalThenQuartzWhenTheyMatch() {
        List<Stack> slots = List.of(
                new Stack(0, LapisPortalRules.REDSTONE, 10),
                new Stack(1, LapisPortalRules.REDSTONE, 2),
                new Stack(2, LapisPortalRules.QUARTZ, 8),
                new Stack(3, LapisPortalRules.LAPIS_LAZULI, 3),
                new Stack(4, "minecraft:dirt", 64));
        assertTrue(LapisPortalRules.bothFuels(slots));
        assertNull(LapisPortalRules.pay(slots, 21));
        assertEquals(10, slots.get(0).count());
        List<Stack> paid = LapisPortalRules.pay(slots, 5);
        assertEquals(6, count(paid, 0));
        assertEquals(2, count(paid, 1));
        assertEquals(7, count(paid, 2));
        assertEquals(3, count(paid, 3));
        assertEquals(64, count(paid, 4));
        assertEquals(4, LapisPortalRules.total(slots, LapisPortalRules.REDSTONE) - LapisPortalRules.total(paid, LapisPortalRules.REDSTONE));
        assertEquals(1, LapisPortalRules.total(slots, LapisPortalRules.QUARTZ) - LapisPortalRules.total(paid, LapisPortalRules.QUARTZ));
    }

    @Test
    void equalTotalsSpendQuartzFirstAndTiesUseTheLowestSlot() {
        List<Stack> equal = List.of(
                new Stack(0, LapisPortalRules.REDSTONE, 10),
                new Stack(3, LapisPortalRules.QUARTZ, 5),
                new Stack(1, LapisPortalRules.QUARTZ, 5));
        List<Stack> one = LapisPortalRules.pay(equal, 1);
        assertEquals(10, count(one, 0));
        assertEquals(4, count(one, 1));
        assertEquals(5, count(one, 3));
        List<Stack> three = LapisPortalRules.pay(equal, 3);
        assertEquals(9, count(three, 0));
        assertEquals(4, count(three, 1));
        assertEquals(4, count(three, 3));

        List<Stack> split = List.of(
                new Stack(1, LapisPortalRules.REDSTONE, 3),
                new Stack(2, LapisPortalRules.REDSTONE, 3),
                new Stack(0, LapisPortalRules.QUARTZ, 5));
        List<Stack> fromTotal = LapisPortalRules.pay(split, 1);
        assertEquals(2, count(fromTotal, 1));
        assertEquals(3, count(fromTotal, 2));
        assertEquals(5, count(fromTotal, 0));

        List<Stack> tied = List.of(new Stack(5, LapisPortalRules.REDSTONE, 4), new Stack(2, LapisPortalRules.REDSTONE, 4));
        List<Stack> lowest = LapisPortalRules.pay(tied, 1);
        assertEquals(3, count(lowest, 2));
        assertEquals(4, count(lowest, 5));
    }

    @Test
    void oneFuelSpendsThatFuelAndAShortChestPaysNothing() {
        assertFalse(LapisPortalRules.bothFuels(List.of(new Stack(0, LapisPortalRules.REDSTONE, 4))));
        assertFalse(LapisPortalRules.bothFuels(List.of(new Stack(0, LapisPortalRules.QUARTZ, 4))));
        List<Stack> dust = LapisPortalRules.pay(List.of(new Stack(0, LapisPortalRules.REDSTONE, 4)), 3);
        assertEquals(1, count(dust, 0));
        List<Stack> quartz = LapisPortalRules.pay(List.of(new Stack(0, LapisPortalRules.QUARTZ, 3)), 2);
        assertEquals(1, count(quartz, 0));
        assertNull(LapisPortalRules.pay(List.of(new Stack(0, LapisPortalRules.QUARTZ, 1)), 2));
        assertNull(LapisPortalRules.pay(List.of(new Stack(0, LapisPortalRules.LAPIS_LAZULI, 64)), 1));
        List<Stack> free = LapisPortalRules.pay(List.of(new Stack(0, LapisPortalRules.REDSTONE, 4)), 0);
        assertEquals(4, count(free, 0));
    }

    @Test
    void spentPhraseNamesWhatTheChestLost() {
        assertEquals("3 redstone dust and 2 nether quartz", LapisPortalRules.spentPhrase(3, 2));
        assertEquals("3 redstone dust", LapisPortalRules.spentPhrase(3, 0));
        assertEquals("2 nether quartz", LapisPortalRules.spentPhrase(0, 2));
        assertEquals("1 redstone dust and 1 nether quartz", LapisPortalRules.spentPhrase(1, 1));
        assertEquals("", LapisPortalRules.spentPhrase(0, 0));
    }

    @Test
    void partnerRequiresExactlyOneOtherBooth() {
        assertEquals(1, LapisPortalRules.partner(new int[] {3, 3}, 0));
        assertEquals(LapisPortalRules.PARTNER_AMBIGUOUS, LapisPortalRules.partner(new int[] {3, 3, 3}, 0));
        assertEquals(LapisPortalRules.PARTNER_NONE, LapisPortalRules.partner(new int[] {3, 1}, 0));
        assertEquals(LapisPortalRules.PARTNER_NONE, LapisPortalRules.partner(new int[] {0, 0}, 0));
        assertEquals(LapisPortalRules.PARTNER_NONE, LapisPortalRules.partner(new int[] {5}, 0));
        assertEquals(LapisPortalRules.PARTNER_NONE, LapisPortalRules.partner(new int[] {4, 4, 2}, 2));
        assertEquals(1, LapisPortalRules.partner(new int[] {4, 4, 2}, 0));
        assertEquals(LapisPortalRules.PARTNER_NONE, LapisPortalRules.partner(null, 0));
    }

    @Test
    void placeKeyRoundTrips() {
        Place place = new Place("minecraft:the_nether", 4, 70, -12, Facing.WEST);
        assertEquals(place, LapisPortalRules.parse(LapisPortalRules.key(place)));
        assertEquals("minecraft:overworld|1|2|3|south", LapisPortalRules.key(new Place("minecraft:overworld", 1, 2, 3, Facing.SOUTH)));
        assertNull(LapisPortalRules.parse("bad"));
        assertNull(LapisPortalRules.parse("minecraft:overworld|1|2|3|up"));
        assertNull(LapisPortalRules.parse("minecraft:overworld|1|2|no|north"));
        assertNull(LapisPortalRules.key(new Place("a|b", 0, 0, 0, Facing.NORTH)));
        Place turned = new Place("minecraft:overworld", 1, 2, 3, Facing.NORTH);
        assertTrue(place.samePlace(new Place("minecraft:the_nether", 4, 70, -12, Facing.EAST)));
        assertFalse(place.samePlace(turned));
    }

    private static int count(List<Stack> slots, int index) {
        for (Stack slot : slots) {
            if (slot.index() == index) {
                return slot.count();
            }
        }
        return -1;
    }
}
