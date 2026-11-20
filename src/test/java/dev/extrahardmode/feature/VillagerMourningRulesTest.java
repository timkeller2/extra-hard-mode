package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VillagerMourningRulesTest {
    @Test
    void aDeathStartsThreeDaysAndASecondDeathAddsThree() {
        assertEquals(8L, VillagerMourningRules.mournUntil(5L, -1L));
        assertEquals(8L, VillagerMourningRules.mournUntil(5L, 5L));
        assertEquals(8L, VillagerMourningRules.mournUntil(5L, 4L));
        assertTrue(VillagerMourningRules.mourning(5L, 8L));
        assertEquals(3, VillagerMourningRules.daysLeft(5L, 8L));
        assertEquals(2, VillagerMourningRules.daysLeft(6L, 8L));
        assertEquals(1, VillagerMourningRules.daysLeft(7L, 8L));
        assertFalse(VillagerMourningRules.mourning(8L, 8L));
        assertEquals(0, VillagerMourningRules.daysLeft(8L, 8L));

        assertEquals(13L, VillagerMourningRules.mournUntil(5L, 10L));
        assertEquals(8, VillagerMourningRules.daysLeft(5L, 13L));
        assertEquals(15L, VillagerMourningRules.mournUntil(12L, 10L));
        assertEquals(Long.MAX_VALUE, VillagerMourningRules.mournUntil(1L, Long.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, VillagerMourningRules.daysLeft(0L, Long.MAX_VALUE));
    }

    @Test
    void theMessageNamesTheDaysThatRemain() {
        assertEquals(
                "I am in a time of mourning for 3 more days...",
                VillagerMourningRules.message(3));
        assertEquals(
                "I am in a time of mourning for 1 more day...",
                VillagerMourningRules.message(1));
        assertEquals(VillagerMourningRules.MESSAGE_ONE, VillagerMourningRules.message(1));
        assertTrue(VillagerMourningRules.within(0.0));
        assertTrue(VillagerMourningRules.within(64.0 * 64.0));
        assertFalse(VillagerMourningRules.within(64.0 * 64.0 + 0.01));
    }
}
