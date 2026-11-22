package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClockFramesTest {
    @Test
    void aFramedClockIsReportedOnceUntilTheCrosshairLeaves() {
        assertTrue(ClockFrames.shouldTell("frame-a", ""));
        assertFalse(ClockFrames.shouldTell("frame-a", "frame-a"));
        assertTrue(ClockFrames.shouldTell("frame-b", "frame-a"));
        assertFalse(ClockFrames.shouldTell("", "frame-a"));
        assertFalse(ClockFrames.shouldTell("", ""));
        assertEquals("", ClockFrames.framedClockId(null));
    }
}
