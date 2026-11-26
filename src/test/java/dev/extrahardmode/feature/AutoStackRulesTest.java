package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class AutoStackRulesTest {
    @Test
    void mainInventoryIsTheOnlyEligibleRange() {
        for (int slot = 0; slot < AutoStackRules.HOTBAR_SIZE; slot++) {
            assertFalse(AutoStackRules.eligible(slot, 0, false), "hotbar " + slot);
        }
        for (int slot = AutoStackRules.MAIN_START; slot < AutoStackRules.MAIN_END; slot++) {
            assertTrue(AutoStackRules.eligible(slot, 0, false), "main " + slot);
            assertFalse(AutoStackRules.eligible(slot, 0, true), "empty " + slot);
        }
        assertFalse(AutoStackRules.eligible(36, 0, false));
        assertFalse(AutoStackRules.eligible(39, 0, false));
        assertFalse(AutoStackRules.eligible(40, 0, false));
        assertFalse(AutoStackRules.eligible(41, 0, false));
        assertFalse(AutoStackRules.mainSlot(40));
    }

    @Test
    void altClickTogglesOneMainSlot() {
        int mask = AutoStackRules.toggle(0, 9);
        assertEquals(1, mask);
        assertTrue(AutoStackRules.excluded(mask, 9));
        assertFalse(AutoStackRules.eligible(9, mask, false));
        assertEquals(0, AutoStackRules.toggle(mask, 9));
        assertEquals(0, AutoStackRules.toggle(0, 8));
        assertEquals(0, AutoStackRules.toggle(0, 40));
        int last = AutoStackRules.toggle(0, 35);
        assertEquals(1 << 26, last);
        assertTrue(AutoStackRules.excluded(last, 35));
        assertFalse(AutoStackRules.excluded(last, 34));
    }

    @Test
    void flyingStackLeavesThePlayerAndLandsOnTheChest() {
        assertEquals(0.0f, AutoStackRules.flyProgress(0.0f), 0.001f);
        assertEquals(1.0f, AutoStackRules.flyProgress(AutoStackRules.FLY_TICKS), 0.001f);
        assertEquals(1.0f, AutoStackRules.flyProgress(AutoStackRules.FLY_TICKS + 3), 0.001f);
        assertEquals(0.5f, AutoStackRules.flyProgress(AutoStackRules.FLY_TICKS / 2.0f), 0.001f);
        assertEquals(2.0, AutoStackRules.flyCoord(2.0, 8.0, 0.0f), 0.001);
        assertEquals(5.0, AutoStackRules.flyCoord(2.0, 8.0, 0.5f), 0.001);
        assertEquals(8.0, AutoStackRules.flyCoord(2.0, 8.0, 1.0f), 0.001);
        assertEquals(0.0, AutoStackRules.flyArc(0.0f), 0.001);
        assertEquals(AutoStackRules.FLY_ARC, AutoStackRules.flyArc(AutoStackRules.FLY_TICKS / 2.0f), 0.001);
        assertEquals(0.0, AutoStackRules.flyArc(AutoStackRules.FLY_TICKS), 0.001);
    }

    @Test
    void rangeIsTwelveBlocksEuclidean() {
        assertTrue(AutoStackRules.inRange(0, 0, 0));
        assertTrue(AutoStackRules.inRange(12, 0, 0));
        assertTrue(AutoStackRules.inRange(-12, 0, 0));
        assertTrue(AutoStackRules.inRange(8, 8, 4));
        assertFalse(AutoStackRules.inRange(13, 0, 0));
        assertFalse(AutoStackRules.inRange(9, 8, 0));
        assertFalse(AutoStackRules.inRange(12, 1, 0));
    }

    @Test
    void matchingPartialFillsBeforeAnEmptySlot() {
        var player = List.of(AutoStackRules.Pile.player("cobble", 64, 64, false));
        var chest = List.of(
                AutoStackRules.Pile.chest("", 0, 64),
                AutoStackRules.Pile.chest("cobble", 10, 64),
                AutoStackRules.Pile.chest("dirt", 64, 64));
        var moves = AutoStackRules.plan(player, List.of(chest));
        assertEquals(2, moves.size());
        assertEquals(1, moves.get(0).slot());
        assertEquals(54, moves.get(0).count());
        assertEquals(0, moves.get(1).slot());
        assertEquals(10, moves.get(1).count());
    }

    @Test
    void aChestWithoutThatItemIsSkipped() {
        var player = List.of(AutoStackRules.Pile.player("cobble", 32, 64, false));
        var emptyOfCobble = List.of(
                AutoStackRules.Pile.chest("dirt", 10, 64), AutoStackRules.Pile.chest("", 0, 64));
        var withCobble = List.of(AutoStackRules.Pile.chest("cobble", 60, 64), AutoStackRules.Pile.chest("", 0, 64));
        var moves = AutoStackRules.plan(player, List.of(emptyOfCobble, withCobble));
        assertEquals(2, moves.size());
        assertEquals(1, moves.get(0).chest());
        assertEquals(0, moves.get(0).slot());
        assertEquals(4, moves.get(0).count());
        assertEquals(1, moves.get(1).chest());
        assertEquals(1, moves.get(1).slot());
        assertEquals(28, moves.get(1).count());
    }

    @Test
    void fullStackAndExcludedSlotStayPut() {
        var player = List.of(
                AutoStackRules.Pile.player("cobble", 64, 64, true),
                AutoStackRules.Pile.player("cobble", 10, 64, false),
                AutoStackRules.Pile.player("pearl", 4, 16, false));
        var chest = List.of(
                AutoStackRules.Pile.chest("cobble", 64, 64), AutoStackRules.Pile.chest("pearl", 16, 16));
        assertTrue(AutoStackRules.plan(player, List.of(chest)).isEmpty());
    }

    @Test
    void laterStackUsesRoomTheFirstStackOpened() {
        var player = List.of(
                AutoStackRules.Pile.player("cobble", 64, 64, false),
                AutoStackRules.Pile.player("cobble", 64, 64, false));
        var chest = List.of(AutoStackRules.Pile.chest("cobble", 1, 64), AutoStackRules.Pile.chest("", 0, 64));
        var moves = AutoStackRules.plan(player, List.of(chest));
        assertEquals(3, moves.size());
        assertEquals(0, moves.get(0).source());
        assertEquals(0, moves.get(0).slot());
        assertEquals(63, moves.get(0).count());
        assertEquals(0, moves.get(1).source());
        assertEquals(1, moves.get(1).slot());
        assertEquals(1, moves.get(1).count());
        assertEquals(1, moves.get(2).source());
        assertEquals(1, moves.get(2).slot());
        assertEquals(63, moves.get(2).count());
    }

    @Test
    void hotbarPileAndDifferentItemDoNotMove() {
        var player = List.of(
                new AutoStackRules.Pile("cobble", 64, 64, false, false),
                AutoStackRules.Pile.player("sand", 10, 64, false));
        var chest = List.of(AutoStackRules.Pile.chest("cobble", 1, 64), AutoStackRules.Pile.chest("", 0, 64));
        assertTrue(AutoStackRules.plan(player, List.of(chest)).isEmpty());
    }

    @Test
    void chestSlotTogglesAndClears() {
        long mask = AutoStackRules.toggleChest(0L, 3, 27);
        assertEquals(1L << 3, mask);
        assertTrue(AutoStackRules.chestExcluded(mask, 3));
        assertEquals(0L, AutoStackRules.toggleChest(mask, 3, 27));
        assertEquals(0L, AutoStackRules.toggleChest(0L, 27, 27));
        assertEquals(0L, AutoStackRules.toggleChest(0L, 3, 64));
        assertEquals(1L << 62, AutoStackRules.toggleChest(0L, 62, 63));
        assertEquals(Long.MAX_VALUE, AutoStackRules.chestBits(63));
        assertEquals(0L, AutoStackRules.chestBits(64));
    }

    @Test
    void doubleClickFlipsEveryChestSlotOnce() {
        long original = 0L;
        long afterFirst = AutoStackRules.toggleChest(original, 3, 27);
        long rest = AutoStackRules.toggleChestRest(afterFirst, 27, 3);
        assertEquals(AutoStackRules.toggleChestAll(original, 27), rest);
        assertEquals((1L << 27) - 1L, rest);
        long again = AutoStackRules.toggleChest(rest, 3, 27);
        assertEquals(0L, AutoStackRules.toggleChestRest(again, 27, 3));
    }

    @Test
    void doubleClickFlipsEveryMainSlotOnce() {
        int original = 0;
        int afterFirst = AutoStackRules.toggle(original, 12);
        int rest = AutoStackRules.toggleMainRest(afterFirst, 12);
        assertEquals(AutoStackRules.toggleAllMain(original), rest);
        assertEquals(AutoStackRules.mainBits(), rest);
        int again = AutoStackRules.toggle(rest, 12);
        assertEquals(0, AutoStackRules.toggleMainRest(again, 12));
    }

    @Test
    void excludedChestSlotIsSkippedButStillCountsAsHoldingTheItem() {
        var player = List.of(AutoStackRules.Pile.player("cobble", 10, 64, false));
        var chest = List.of(
                AutoStackRules.Pile.chest("cobble", 5, 64, true),
                AutoStackRules.Pile.chest("", 0, 64, true),
                AutoStackRules.Pile.chest("", 0, 64, false));
        var moves = AutoStackRules.plan(player, List.of(chest));
        assertEquals(1, moves.size());
        assertEquals(2, moves.get(0).slot());
        assertEquals(10, moves.get(0).count());
        var blocked = List.of(
                AutoStackRules.Pile.chest("cobble", 1, 64, true),
                AutoStackRules.Pile.chest("", 0, 64, true));
        assertTrue(AutoStackRules.plan(player, List.of(blocked)).isEmpty());
    }
}
