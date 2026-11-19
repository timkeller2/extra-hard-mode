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
        long now = 1_000_000L;
        long hour = LodestoneChunkRules.HOUR_MILLIS;
        assertEquals(now + hour, LodestoneChunkRules.extendedExpiry(now, 0L));
        assertEquals(now + hour, LodestoneChunkRules.extendedExpiry(now, now - 50L));
        assertEquals(now + 2 * hour, LodestoneChunkRules.extendedExpiry(now, now + hour));
        assertEquals(now + 24 * hour, LodestoneChunkRules.extendedExpiry(now, now + 23 * hour));
        assertEquals(now + 24 * hour, LodestoneChunkRules.extendedExpiry(now, now + 23 * hour + hour / 2));
        assertFalse(LodestoneChunkRules.extendsTime(now, now + 24 * hour));
        assertEquals(now + 24 * hour, LodestoneChunkRules.extendedExpiry(now, now + 24 * hour));
        assertFalse(LodestoneChunkRules.extendsTime(now, now + 25 * hour));
        assertTrue(LodestoneChunkRules.extendsTime(now, now + 23 * hour));
    }

    @Test
    void soonestExpiryIgnoresQuietChunks() {
        long now = 5_000L;
        assertEquals(0L, LodestoneChunkRules.soonestExpiry(now, new long[] {0L, now - 1L}));
        assertEquals(now + 10L, LodestoneChunkRules.soonestExpiry(now, new long[] {now + 40L, 0L, now + 10L}));
        assertEquals(10L, LodestoneChunkRules.remaining(now, now + 10L));
        assertEquals(0L, LodestoneChunkRules.remaining(now, now));
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
