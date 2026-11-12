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
    void frameEnclosesBothHalvesExceptTheOpeningAndPutsTheChestOnTheFacingColumn() {
        for (Facing facing : Facing.values()) {
            List<Offset> lapis = LapisPortalRules.lapisOffsets(facing);
            assertEquals(8, lapis.size());
            assertEquals(8, new HashSet<>(lapis).size());
            assertTrue(lapis.contains(new Offset(0, -1, 0)));
            assertTrue(lapis.contains(new Offset(0, 2, 0)));
            assertFalse(lapis.contains(new Offset(0, 0, 0)));
            assertFalse(lapis.contains(new Offset(0, 1, 0)));
            Facing entrance = facing.opposite();
            assertFalse(lapis.contains(new Offset(entrance.x, -1, entrance.z)));
            assertFalse(lapis.contains(new Offset(entrance.x, 0, entrance.z)));
            assertFalse(lapis.contains(new Offset(entrance.x, 1, entrance.z)));
            for (Facing dir : Facing.values()) {
                if (dir == entrance) {
                    continue;
                }
                assertTrue(lapis.contains(new Offset(dir.x, 0, dir.z)));
                assertTrue(lapis.contains(new Offset(dir.x, 1, dir.z)));
                assertFalse(lapis.contains(new Offset(dir.x, -1, dir.z)));
            }
            Offset chest = LapisPortalRules.chestOffset(facing);
            assertEquals(new Offset(facing.x, 2, facing.z), chest);
            assertFalse(lapis.contains(chest));
        }
        assertNull(LapisPortalRules.chestOffset(null));
        assertTrue(LapisPortalRules.lapisOffsets(null).isEmpty());
    }

    @Test
    void witherfenBoothsMatchTheWestAndSouthFrames() {
        assertEquals(
                List.of(
                        new Offset(0, -1, 0),
                        new Offset(0, 2, 0),
                        new Offset(0, 0, -1),
                        new Offset(0, 1, -1),
                        new Offset(0, 0, 1),
                        new Offset(0, 1, 1),
                        new Offset(-1, 0, 0),
                        new Offset(-1, 1, 0)),
                LapisPortalRules.lapisOffsets(Facing.WEST));
        assertEquals(new Offset(-1, 2, 0), LapisPortalRules.chestOffset(Facing.WEST));
        assertEquals(
                List.of(
                        new Offset(0, -1, 0),
                        new Offset(0, 2, 0),
                        new Offset(1, 0, 0),
                        new Offset(1, 1, 0),
                        new Offset(0, 0, 1),
                        new Offset(0, 1, 1),
                        new Offset(-1, 0, 0),
                        new Offset(-1, 1, 0)),
                LapisPortalRules.lapisOffsets(Facing.SOUTH));
        assertEquals(new Offset(0, 2, 1), LapisPortalRules.chestOffset(Facing.SOUTH));
    }

    @Test
    void eachFuelKindAddsOneHundredBlocksAndTheCostIsAtLeastOne() {
        assertEquals(100, LapisPortalRules.BLOCKS_PER_FUEL);
        assertEquals(100, LapisPortalRules.blocksPerFuel(0));
        assertEquals(100, LapisPortalRules.blocksPerFuel(1));
        assertEquals(200, LapisPortalRules.blocksPerFuel(2));
        assertEquals(400, LapisPortalRules.blocksPerFuel(3));
        assertEquals(400, LapisPortalRules.blocksPerFuel(9));
        assertEquals(400, LapisPortalRules.THREE_KINDS_BLOCKS);
        assertEquals(0, LapisPortalRules.travelCost(0, 3));
        assertEquals(0, LapisPortalRules.travelCost(-5, 1));
        assertEquals(1, LapisPortalRules.travelCost(1, 1));
        assertEquals(1, LapisPortalRules.travelCost(100, 1));
        assertEquals(2, LapisPortalRules.travelCost(100.0001, 1));
        assertEquals(2, LapisPortalRules.travelCost(101, 1));
        assertEquals(2, LapisPortalRules.travelCost(200, 1));
        assertEquals(1, LapisPortalRules.travelCost(1, 2));
        assertEquals(1, LapisPortalRules.travelCost(200, 2));
        assertEquals(2, LapisPortalRules.travelCost(200.0001, 2));
        assertEquals(2, LapisPortalRules.travelCost(201, 2));
        assertEquals(3, LapisPortalRules.travelCost(500, 2));
        assertEquals(1, LapisPortalRules.travelCost(1, 3));
        assertEquals(1, LapisPortalRules.travelCost(300, 3));
        assertEquals(1, LapisPortalRules.travelCost(400, 3));
        assertEquals(2, LapisPortalRules.travelCost(401, 3));
        assertEquals(2, LapisPortalRules.travelCost(800, 3));
        assertEquals(1, LapisPortalRules.travelCost(1, 0));
        assertEquals(1.0, LapisPortalRules.travelDistance(false, 0), 0.0);
        assertEquals(0.0, LapisPortalRules.travelDistance(true, 0), 0.0);
        assertEquals(80.0, LapisPortalRules.travelDistance(false, 80), 0.0);
        assertEquals(100.0, LapisPortalRules.distance(0, 64, 0, 0, 164, 0), 0.0001);
        assertEquals(5.0, LapisPortalRules.distance(0, 0, 0, 3, 4, 0), 0.0001);
        assertEquals(2_000_000_000d, LapisPortalRules.distance(1_000_000_000, 0, 0, -1_000_000_000, 0, 0), 1.0);
    }

    @Test
    void paymentAlternatesKindsFromTheLargestStack() {
        List<Stack> slots = List.of(
                new Stack(0, LapisPortalRules.REDSTONE, 10),
                new Stack(1, LapisPortalRules.REDSTONE, 2),
                new Stack(2, LapisPortalRules.QUARTZ, 8),
                new Stack(3, LapisPortalRules.LAPIS_LAZULI, 3),
                new Stack(4, "minecraft:dirt", 64),
                new Stack(5, LapisPortalRules.AMETHYST, 1));
        assertEquals(3, LapisPortalRules.fuelKinds(slots));
        assertEquals(
                List.of(LapisPortalRules.REDSTONE, LapisPortalRules.QUARTZ, LapisPortalRules.AMETHYST),
                LapisPortalRules.fuelOrder(slots));
        assertNull(LapisPortalRules.pay(slots, 22));
        assertEquals(10, slots.get(0).count());
        List<Stack> paid = LapisPortalRules.pay(slots, 5);
        assertEquals(8, count(paid, 0));
        assertEquals(2, count(paid, 1));
        assertEquals(6, count(paid, 2));
        assertEquals(3, count(paid, 3));
        assertEquals(64, count(paid, 4));
        assertEquals(0, count(paid, 5));
        assertEquals(2, LapisPortalRules.total(slots, LapisPortalRules.REDSTONE) - LapisPortalRules.total(paid, LapisPortalRules.REDSTONE));
        assertEquals(2, LapisPortalRules.total(slots, LapisPortalRules.QUARTZ) - LapisPortalRules.total(paid, LapisPortalRules.QUARTZ));
        assertEquals(1, LapisPortalRules.total(slots, LapisPortalRules.AMETHYST) - LapisPortalRules.total(paid, LapisPortalRules.AMETHYST));
    }

    @Test
    void equalLargestStacksStartWithQuartzAndATieUsesTheLowestSlot() {
        List<Stack> equal = List.of(
                new Stack(0, LapisPortalRules.REDSTONE, 10),
                new Stack(3, LapisPortalRules.QUARTZ, 10),
                new Stack(1, LapisPortalRules.AMETHYST, 10));
        assertEquals(
                List.of(LapisPortalRules.QUARTZ, LapisPortalRules.REDSTONE, LapisPortalRules.AMETHYST),
                LapisPortalRules.fuelOrder(equal));
        List<Stack> one = LapisPortalRules.pay(equal, 1);
        assertEquals(10, count(one, 0));
        assertEquals(9, count(one, 3));
        assertEquals(10, count(one, 1));
        List<Stack> four = LapisPortalRules.pay(equal, 4);
        assertEquals(9, count(four, 0));
        assertEquals(8, count(four, 3));
        assertEquals(9, count(four, 1));

        List<Stack> smallerStack = List.of(
                new Stack(1, LapisPortalRules.REDSTONE, 3),
                new Stack(2, LapisPortalRules.REDSTONE, 3),
                new Stack(0, LapisPortalRules.QUARTZ, 5));
        assertEquals(List.of(LapisPortalRules.QUARTZ, LapisPortalRules.REDSTONE), LapisPortalRules.fuelOrder(smallerStack));
        List<Stack> fromStack = LapisPortalRules.pay(smallerStack, 1);
        assertEquals(4, count(fromStack, 0));
        assertEquals(3, count(fromStack, 1));
        assertEquals(3, count(fromStack, 2));

        List<Stack> tied = List.of(new Stack(5, LapisPortalRules.REDSTONE, 4), new Stack(2, LapisPortalRules.REDSTONE, 4));
        List<Stack> lowest = LapisPortalRules.pay(tied, 1);
        assertEquals(3, count(lowest, 2));
        assertEquals(4, count(lowest, 5));
    }

    @Test
    void oneFuelSpendsThatFuelAndASpentKindIsSkipped() {
        assertEquals(1, LapisPortalRules.fuelKinds(List.of(new Stack(0, LapisPortalRules.REDSTONE, 4))));
        assertEquals(1, LapisPortalRules.fuelKinds(List.of(new Stack(0, LapisPortalRules.AMETHYST, 4))));
        assertEquals(0, LapisPortalRules.fuelKinds(List.of(new Stack(0, LapisPortalRules.LAPIS_LAZULI, 64))));
        List<Stack> dust = LapisPortalRules.pay(List.of(new Stack(0, LapisPortalRules.REDSTONE, 4)), 3);
        assertEquals(1, count(dust, 0));
        List<Stack> quartz = LapisPortalRules.pay(List.of(new Stack(0, LapisPortalRules.QUARTZ, 3)), 2);
        assertEquals(1, count(quartz, 0));
        List<Stack> mixed = List.of(
                new Stack(0, LapisPortalRules.QUARTZ, 1),
                new Stack(1, LapisPortalRules.REDSTONE, 4),
                new Stack(2, LapisPortalRules.AMETHYST, 4));
        List<Stack> skipped = LapisPortalRules.pay(mixed, 4);
        assertEquals(0, count(skipped, 0));
        assertEquals(2, count(skipped, 1));
        assertEquals(3, count(skipped, 2));
        assertNull(LapisPortalRules.pay(List.of(new Stack(0, LapisPortalRules.QUARTZ, 1)), 2));
        assertNull(LapisPortalRules.pay(List.of(new Stack(0, LapisPortalRules.LAPIS_LAZULI, 64)), 1));
        List<Stack> free = LapisPortalRules.pay(List.of(new Stack(0, LapisPortalRules.REDSTONE, 4)), 0);
        assertEquals(4, count(free, 0));
    }

    @Test
    void spentPhraseNamesWhatTheChestLost() {
        assertEquals("2 nether quartz and 3 redstone dust", LapisPortalRules.spentPhrase(3, 2, 0));
        assertEquals("3 redstone dust", LapisPortalRules.spentPhrase(3, 0, 0));
        assertEquals("2 nether quartz", LapisPortalRules.spentPhrase(0, 2, 0));
        assertEquals("1 nether quartz and 1 redstone dust", LapisPortalRules.spentPhrase(1, 1, 0));
        assertEquals("1 amethyst shard", LapisPortalRules.spentPhrase(0, 0, 1));
        assertEquals(
                "2 nether quartz, 3 redstone dust, and 4 amethyst shards",
                LapisPortalRules.spentPhrase(3, 2, 4));
        assertEquals("", LapisPortalRules.spentPhrase(0, 0, 0));
    }

    @Test
    void remainPhraseNamesFuelLeftInThePayingChest() {
        assertEquals("6 redstone dust remain in the chest you left", LapisPortalRules.remainPhrase(6, 0, 0));
        assertEquals("1 amethyst shard remains in the chest you left", LapisPortalRules.remainPhrase(0, 0, 1));
        assertEquals("1 nether quartz remains in the chest you left", LapisPortalRules.remainPhrase(0, 1, 0));
        assertEquals(
                "2 nether quartz and 3 redstone dust remain in the chest you left",
                LapisPortalRules.remainPhrase(3, 2, 0));
        assertEquals(
                "2 nether quartz, 3 redstone dust, and 4 amethyst shards remain in the chest you left",
                LapisPortalRules.remainPhrase(3, 2, 4));
        assertEquals("No fuel remains in the chest you left", LapisPortalRules.remainPhrase(0, 0, 0));
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
    void aTripThundersForTwoHundredBlocksAndRestsBothBooths() {
        assertEquals(200, LapisPortalRules.THUNDER_BLOCKS);
        assertEquals(12.5F, LapisPortalRules.THUNDER_VOLUME, 0.0F);
        assertEquals(10, LapisPortalRules.SICKNESS_SECONDS);
        assertEquals(200, LapisPortalRules.SICKNESS_TICKS);
        assertEquals(15, LapisPortalRules.RECHARGE_SECONDS);
        assertEquals(300, LapisPortalRules.RECHARGE_TICKS);
        assertTrue(LapisPortalRules.hearsThunder(0));
        assertTrue(LapisPortalRules.hearsThunder(200));
        assertFalse(LapisPortalRules.hearsThunder(200.01));
        assertFalse(LapisPortalRules.hearsThunder(-1));
        assertEquals(310L, LapisPortalRules.readyAt(10L));
        assertTrue(LapisPortalRules.recharging(310L, 309L));
        assertFalse(LapisPortalRules.recharging(310L, 310L));
        assertEquals(15, LapisPortalRules.secondsLeft(310L, 10L));
        assertEquals(1, LapisPortalRules.secondsLeft(310L, 309L));
        assertEquals(1, LapisPortalRules.secondsLeft(310L, 290L));
        assertEquals(2, LapisPortalRules.secondsLeft(310L, 289L));
        assertEquals(0, LapisPortalRules.secondsLeft(310L, 310L));
        assertEquals("1 second remains.", LapisPortalRules.rechargeWait(1));
        assertEquals("15 seconds remain.", LapisPortalRules.rechargeWait(15));
        assertEquals("0 seconds remain.", LapisPortalRules.rechargeWait(0));
        assertEquals(
                "minecraft:the_nether|4|70|-12",
                LapisPortalRules.boothKey("minecraft:the_nether", 4, 70, -12));
        assertNull(LapisPortalRules.boothKey("", 0, 0, 0));
        assertNull(LapisPortalRules.boothKey(null, 0, 0, 0));
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
