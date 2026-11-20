package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LodestoneChunkRulesTest {
    @Test
    void oneHourCoversTheChunkAndEightAroundIt() {
        assertEquals(1, LodestoneChunkRules.RADIUS);
        assertEquals(9, LodestoneChunkRules.coveredChunks());
        assertEquals(3_600_000L, LodestoneChunkRules.HOUR_MILLIS);
        assertEquals(24 * LodestoneChunkRules.HOUR_MILLIS, LodestoneChunkRules.CAP_MILLIS);
    }

    @Test
    void fuelAddsAnHourAndStopsAtADay() {
        long hour = LodestoneChunkRules.HOUR_MILLIS;
        assertEquals(hour, LodestoneChunkRules.extendedRemaining(0L, hour));
        assertEquals(hour, LodestoneChunkRules.extendedRemaining(-50L, hour));
        assertEquals(2 * hour, LodestoneChunkRules.extendedRemaining(hour, hour));
        assertEquals(24 * hour, LodestoneChunkRules.extendedRemaining(23 * hour, hour));
        assertEquals(24 * hour, LodestoneChunkRules.extendedRemaining(23 * hour + hour / 2, hour));
        assertFalse(LodestoneChunkRules.extendsTime(24 * hour));
        assertEquals(24 * hour, LodestoneChunkRules.extendedRemaining(24 * hour, hour));
        assertFalse(LodestoneChunkRules.extendsTime(25 * hour));
        assertTrue(LodestoneChunkRules.extendsTime(23 * hour));
    }

    @Test
    void residencePointsSetTheFuel() {
        long minute = LodestoneChunkRules.MINUTE_MILLIS;
        assertEquals(60 * minute, LodestoneChunkRules.fuelMillis(12));
        assertEquals(75 * minute, LodestoneChunkRules.fuelMillis(15));
        assertEquals(110 * minute, LodestoneChunkRules.fuelMillis(22));
        assertTrue(LodestoneChunkRules.acceptsResidence(true, 12));
        assertTrue(LodestoneChunkRules.acceptsResidence(true, 22));
        assertFalse(LodestoneChunkRules.acceptsResidence(true, 11));
        assertFalse(LodestoneChunkRules.acceptsResidence(false, 15));
        assertEquals(
                "This lodestone is not in a residence.",
                LodestoneChunkRules.residenceRefusal(false, false, 0, java.util.List.of()));
        assertEquals(
                "This residence has 9 points. It needs 12.",
                LodestoneChunkRules.residenceRefusal(true, false, 9, java.util.List.of("more furnishings (see /tougher help homes)")));
        assertEquals(
                "This residence has 15 points but is not valid yet. Missing: a door to the outside.",
                LodestoneChunkRules.residenceRefusal(true, false, 15, java.util.List.of("a door to the outside")));
        assertEquals("", LodestoneChunkRules.residenceRefusal(true, true, 15, java.util.List.of()));
    }

    @Test
    void soonestRemainingIgnoresQuietChunks() {
        assertEquals(0L, LodestoneChunkRules.soonestRemaining(new long[] {0L, -1L}));
        assertEquals(10L, LodestoneChunkRules.soonestRemaining(new long[] {40L, 0L, 10L}));
    }

    @Test
    void idleAndOfflineTimeDoesNotRunDown() {
        long hour = LodestoneChunkRules.HOUR_MILLIS;
        long now = 5_000_000_000_000L;
        assertEquals(0L, LodestoneChunkRules.elapsedRunning(0L, now));
        assertEquals(1_000L, LodestoneChunkRules.elapsedRunning(now - 1_000L, now));
        assertEquals(0L, LodestoneChunkRules.elapsedRunning(now - 60_000L, now));
        assertEquals(hour, LodestoneChunkRules.afterRunning(hour, 0L));
        assertEquals(hour - 1_000L, LodestoneChunkRules.afterRunning(hour, 1_000L));
        assertEquals(0L, LodestoneChunkRules.afterRunning(400L, 1_000L));
        assertEquals(hour, LodestoneChunkRules.fromStored(now + hour, now));
        assertEquals(0L, LodestoneChunkRules.fromStored(now - 10L, now));
        assertEquals(LodestoneChunkRules.CAP_MILLIS, LodestoneChunkRules.fromStored(now + hour * 30, now));
        assertEquals(hour, LodestoneChunkRules.fromStored(hour, now));
    }

    @Test
    void lookLabelCountsDownTheSoonestHour() {
        long hour = LodestoneChunkRules.HOUR_MILLIS;
        assertEquals("", LodestoneChunkRules.lookLabel(0L));
        assertEquals("Active for 1 second", LodestoneChunkRules.lookLabel(1L));
        assertEquals("Active for 45 seconds", LodestoneChunkRules.lookLabel(45_000L));
        assertEquals("Active for 1 minute", LodestoneChunkRules.lookLabel(60_000L));
        assertEquals("Active for 12 minutes", LodestoneChunkRules.lookLabel(12 * 60_000L));
        assertEquals("Active for 1 hour", LodestoneChunkRules.lookLabel(hour));
        assertEquals("Active for 1 hour 1 minute", LodestoneChunkRules.lookLabel(hour + 60_000L));
        assertEquals("Active for 23 hours 5 minutes", LodestoneChunkRules.lookLabel(23 * hour + 5 * 60_000L));
        assertEquals("These chunks are already active for 24 hours.", LodestoneChunkRules.CAP);
    }
}
