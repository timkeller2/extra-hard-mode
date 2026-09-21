package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TorchLifetimeRulesTest {
    @Test
    void zeroDaysIsPermanent() {
        assertTrue(TorchLifetimeRules.permanent(0));
        assertTrue(TorchLifetimeRules.permanent(-4));
        assertFalse(TorchLifetimeRules.permanent(1));
        assertEquals(0, TorchLifetimeRules.clampDays(-1));
        assertEquals(7, TorchLifetimeRules.clampDays(7));
        assertEquals(TorchLifetimeRules.MAX_DAYS, TorchLifetimeRules.clampDays(TorchLifetimeRules.MAX_DAYS + 50));
    }

    @Test
    void sevenDaysIsOneHundredSixtyEightThousandTicks() {
        assertEquals(24000L, TorchLifetimeRules.lifetimeTicks(1));
        assertEquals(168000L, TorchLifetimeRules.lifetimeTicks(7));
        assertEquals(Long.MAX_VALUE, TorchLifetimeRules.lifetimeTicks(0));
    }

    @Test
    void unstampedAndPermanentNeverExpire() {
        assertFalse(TorchLifetimeRules.expired(-1L, 999_999L, 7));
        assertFalse(TorchLifetimeRules.expired(0L, Long.MAX_VALUE / 4, 0));
        assertFalse(TorchLifetimeRules.expired(100L, 100L + 23999L, 1));
        assertTrue(TorchLifetimeRules.expired(100L, 100L + 24000L, 1));
        assertTrue(TorchLifetimeRules.expired(0L, 168000L, 7));
        assertFalse(TorchLifetimeRules.expired(0L, 167999L, 7));
    }

    @Test
    void lightDimsAfterTwoDaysAndPermanentStaysFull() {
        assertEquals(14, TorchLifetimeRules.FULL_LIGHT);
        assertEquals(2, TorchLifetimeRules.DIM_AFTER_DAYS);
        assertEquals(0, TorchLifetimeRules.daysBurning(-1L, 99_000L));
        assertEquals(0, TorchLifetimeRules.daysBurning(0L, 23999L));
        assertEquals(1, TorchLifetimeRules.daysBurning(0L, 24000L));
        assertEquals(2, TorchLifetimeRules.daysBurning(0L, 48000L));
        assertEquals(3, TorchLifetimeRules.daysBurning(0L, 72000L));
        assertEquals(14, TorchLifetimeRules.lightLevel(14, 0L, 0L, 7));
        assertEquals(14, TorchLifetimeRules.lightLevel(14, 0L, 47999L, 7));
        assertEquals(14, TorchLifetimeRules.lightLevel(14, 0L, 48000L, 7));
        assertEquals(13, TorchLifetimeRules.lightLevel(14, 0L, 72000L, 7));
        assertEquals(12, TorchLifetimeRules.lightLevel(14, 0L, 96000L, 7));
        assertEquals(10, TorchLifetimeRules.lightLevel(14, 0L, 168000L - 1L, 7));
        assertEquals(14, TorchLifetimeRules.lightLevel(14, -1L, 999_999L, 7));
        assertEquals(14, TorchLifetimeRules.lightLevel(14, 0L, 999_999L, 0));
        assertEquals(10, TorchLifetimeRules.lightLevel(10, 0L, 72000L, 7));
        assertEquals(10, TorchLifetimeRules.lightLevel(10, 0L, 168000L - 1L, 7));
    }

    @Test
    void campfireChestRangeIsTwelveBlocks() {
        assertEquals(12, TorchLifetimeRules.CAMPFIRE_REFUEL_RANGE);
        assertTrue(TorchLifetimeRules.chestInRange(12, 0, 0, 12));
        assertTrue(TorchLifetimeRules.chestInRange(0, -12, 0, 12));
        assertTrue(TorchLifetimeRules.chestInRange(8, 8, 4, 12));
        assertFalse(TorchLifetimeRules.chestInRange(13, 0, 0, 12));
        assertFalse(TorchLifetimeRules.chestInRange(9, 8, 0, 12));
        assertEquals(144L, TorchLifetimeRules.distanceSq(12, 0, 0));
    }

    @Test
    void campfireRefuelAddsAnotherBurnPeriod() {
        assertTrue(TorchLifetimeRules.expired(0L, 168000L, 7));
        long extended = TorchLifetimeRules.extendPlacedAt(0L, 7);
        assertEquals(168000L, extended);
        assertFalse(TorchLifetimeRules.expired(extended, 168000L, 7));
        assertFalse(TorchLifetimeRules.expired(extended, 168000L + 167999L, 7));
        assertTrue(TorchLifetimeRules.expired(extended, 336000L, 7));
        assertEquals(0L, TorchLifetimeRules.extendPlacedAt(0L, 0));
        assertEquals(-1L, TorchLifetimeRules.extendPlacedAt(-1L, 7));
        assertEquals(16, TorchLifetimeRules.TORCH_REFUEL_RANGE);
        assertTrue(TorchLifetimeRules.chestInRange(16, 0, 0, 16));
        assertFalse(TorchLifetimeRules.chestInRange(17, 0, 0, 16));
        assertTrue(TorchLifetimeRules.isTorchFuelItemId("minecraft:coal"));
        assertTrue(TorchLifetimeRules.isTorchFuelItemId("minecraft:charcoal"));
        assertFalse(TorchLifetimeRules.isTorchFuelItemId("minecraft:oak_log"));
    }

    @Test
    void copperTorchLastsTwiceAsLong() {
        assertEquals(2, TorchLifetimeRules.COPPER_DURATION_FACTOR);
        assertTrue(TorchLifetimeRules.isCopperTorchId("minecraft:copper_torch"));
        assertTrue(TorchLifetimeRules.isCopperTorchId("minecraft:copper_wall_torch"));
        assertFalse(TorchLifetimeRules.isCopperTorchId("minecraft:torch"));
        assertFalse(TorchLifetimeRules.isCopperTorchId("minecraft:wall_torch"));
        assertEquals(7, TorchLifetimeRules.burnDaysFor(7, false));
        assertEquals(14, TorchLifetimeRules.burnDaysFor(7, true));
        assertEquals(0, TorchLifetimeRules.burnDaysFor(0, true));
        assertEquals(TorchLifetimeRules.MAX_DAYS, TorchLifetimeRules.burnDaysFor(TorchLifetimeRules.MAX_DAYS, true));
        assertFalse(TorchLifetimeRules.expired(0L, 167999L, 7));
        assertTrue(TorchLifetimeRules.expired(0L, 168000L, 7));
        assertFalse(TorchLifetimeRules.expired(0L, 335999L, 14));
        assertTrue(TorchLifetimeRules.expired(0L, 336000L, 14));
    }

    @Test
    void remainingTicksIsPermanentWhenUnstampedOrConfigPermanent() {
        assertEquals(-1, TorchLifetimeRules.remainingTicks(-1L, 999_999L, 7));
        assertEquals(-1, TorchLifetimeRules.remainingTicks(0L, 100L, 0));
        assertEquals(24000, TorchLifetimeRules.remainingTicks(0L, 0L, 1));
        assertEquals(1, TorchLifetimeRules.remainingTicks(0L, 23999L, 1));
        assertEquals(0, TorchLifetimeRules.remainingTicks(0L, 24000L, 1));
        assertEquals(168000, TorchLifetimeRules.remainingTicks(0L, 0L, 7));
        assertEquals(12000, TorchLifetimeRules.remainingTicks(0L, 156000L, 7));
        assertEquals(14 * 24000, TorchLifetimeRules.remainingTicks(0L, 0L, 14));
    }

    @Test
    void remainingLabelShowsPermanentAndDaysHours() {
        assertEquals("Permanent", TorchLifetimeRules.remainingLabel(-1));
        assertEquals("7d", TorchLifetimeRules.remainingLabel(7 * 24000));
        assertEquals("6d 12h", TorchLifetimeRules.remainingLabel(6 * 24000 + 12 * 1000));
        assertEquals("3h", TorchLifetimeRules.remainingLabel(3000));
        assertEquals("<1h", TorchLifetimeRules.remainingLabel(500));
        assertEquals("<1h", TorchLifetimeRules.remainingLabel(0));
        assertEquals(TorchLifetimeRules.LIGHT_LOOK_PERMANENT_COLOR, TorchLifetimeRules.lightLookColor(-1));
        assertEquals(TorchLifetimeRules.LIGHT_LOOK_COLOR, TorchLifetimeRules.lightLookColor(24000));
    }
}
