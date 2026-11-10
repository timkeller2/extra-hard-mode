package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MissionTimerRulesTest {
    @Test
    void labelCountsMinecraftDaysAndHours() {
        assertEquals("(2d 3h)", MissionTimerRules.label(24, 525_000L));
        assertEquals("(14d 0h)", MissionTimerRules.label(14, 0L));
        assertEquals("(0d 1h)", MissionTimerRules.label(1, 23_000L));
        assertEquals("(0d 0h)", MissionTimerRules.label(1, 23_001L));
        assertEquals("(0d 0h)", MissionTimerRules.label(1, 24_000L));
        assertEquals("", MissionTimerRules.label(-1, 0L));
        assertEquals("(14d 0h)", MissionTimerRules.label(14, -5L));
    }

    @Test
    void appendPutsTheTimerAtTheEnd() {
        assertEquals("Ada: 6 Zombies (2d 3h)", MissionTimerRules.append("Ada: 6 Zombies", 24, 525_000L));
        assertEquals("Jack: Iron East", MissionTimerRules.append("Jack: Iron East", -1, 0L));
        assertEquals("", MissionTimerRules.append("", 14, 0L));
        assertEquals("", MissionTimerRules.append(null, 14, 0L));
    }
}
