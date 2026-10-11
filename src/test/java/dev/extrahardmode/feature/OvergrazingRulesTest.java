package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OvergrazingRulesTest {
    @Test
    void supportBlocksDependOnAnimalSize() {
        assertEquals(9, OvergrazingRules.blocksToSupport(false, 9, 4));
        assertEquals(4, OvergrazingRules.blocksToSupport(true, 9, 4));
        assertEquals(1, OvergrazingRules.clampBlocks(0));
        assertEquals(32, OvergrazingRules.clampBlocks(99));
        assertEquals(1, OvergrazingRules.blocksToSupport(true, 9, 0));
    }

    @Test
    void biteIntervalMatchesAPastureOfThatSize() {
        assertEquals(5000, OvergrazingRules.eatIntervalTicks(false, 9, 4));
        assertEquals(14000, OvergrazingRules.eatIntervalTicks(true, 9, 4));
        assertEquals(2500, OvergrazingRules.eatIntervalTicks(false, 18, 4));
        assertEquals(7000, OvergrazingRules.eatIntervalTicks(true, 9, 8));
        assertEquals(45000, OvergrazingRules.eatIntervalTicks(false, 1, 4));
        assertEquals(1406, OvergrazingRules.eatIntervalTicks(false, 99, 4));
    }

    @Test
    void chestEatsShearedGrassBeforeBreedingFood() {
        assertEquals(2, OvergrazingRules.preferredChestIndex(new boolean[] {false, false, true}, new boolean[] {true, true, false}));
        assertEquals(0, OvergrazingRules.preferredChestIndex(new boolean[] {true, false}, new boolean[] {true, true}));
        assertEquals(1, OvergrazingRules.preferredChestIndex(new boolean[] {false, false}, new boolean[] {false, true}));
        assertEquals(-1, OvergrazingRules.preferredChestIndex(new boolean[] {}, new boolean[] {}));
        assertEquals(-1, OvergrazingRules.preferredChestIndex(new boolean[] {false}, new boolean[] {false, true}));
    }

    @Test
    void lookAndStarveAreThirtyThreePercent() {
        assertTrue(OvergrazingRules.lookInChests(0));
        assertTrue(OvergrazingRules.lookInChests(32));
        assertFalse(OvergrazingRules.lookInChests(33));
        assertFalse(OvergrazingRules.lookInChests(99));
        assertTrue(OvergrazingRules.lookInChests(49, 50));
        assertFalse(OvergrazingRules.lookInChests(50, 50));
        assertFalse(OvergrazingRules.lookInChests(0, 0));
        assertTrue(OvergrazingRules.starve(0));
        assertTrue(OvergrazingRules.starve(32));
        assertFalse(OvergrazingRules.starve(33));
    }

    @Test
    void intervalAndRangeClamp() {
        assertEquals(20, OvergrazingRules.clampInterval(0));
        assertEquals(5000, OvergrazingRules.clampInterval(5000));
        assertEquals(1, OvergrazingRules.clampRange(0));
        assertEquals(8, OvergrazingRules.clampRange(8));
        assertEquals(64, OvergrazingRules.clampRange(99));
    }

    @Test
    void biteScheduleIsStaggeredThenSteady() {
        assertEquals(11L, OvergrazingRules.firstCheckAt(10L, 5000, 0));
        assertEquals(14L, OvergrazingRules.firstCheckAt(10L, 4, 3));
        assertFalse(OvergrazingRules.isDue(99L, 0L));
        assertFalse(OvergrazingRules.isDue(5L, 10L));
        assertTrue(OvergrazingRules.isDue(10L, 10L));
        assertEquals(5010L, OvergrazingRules.reschedule(10L, 5000));
        assertTrue(OvergrazingRules.scheduledSoon(10L, 20L, 5000));
        assertFalse(OvergrazingRules.scheduledSoon(10L, 5011L, 5000));
        assertFalse(OvergrazingRules.scheduledSoon(10L, 10L, 5000));
        assertEquals(100, OvergrazingRules.FOOD_FLOAT_TICKS);
    }
}
