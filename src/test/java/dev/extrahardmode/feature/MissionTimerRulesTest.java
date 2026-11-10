package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MissionTimerRulesTest {
    @Test
    void labelShowsDaysUntilTheLastDayThenHours() {
        assertEquals("(2d)", MissionTimerRules.label(24, 525_000L));
        assertEquals("(3d)", MissionTimerRules.label(4, 1_000L));
        assertEquals("(13h)", MissionTimerRules.label(1, 11_000L));
        assertEquals("(1h)", MissionTimerRules.label(1, 23_000L));
        assertEquals("(0h)", MissionTimerRules.label(1, 23_001L));
        assertEquals("(0h)", MissionTimerRules.label(1, 24_000L));
        assertEquals("", MissionTimerRules.label(4, 0L));
        assertEquals("", MissionTimerRules.label(14, 0L));
        assertEquals("", MissionTimerRules.label(14, -5L));
        assertEquals("", MissionTimerRules.label(-1, 0L));
    }

    @Test
    void appendPutsTheTimerAtTheEnd() {
        assertEquals("Ada: 6 Zombies (2d)", MissionTimerRules.append("Ada: 6 Zombies", 24, 525_000L));
        assertEquals("Ada: 6 Zombies (13h)", MissionTimerRules.append("Ada: 6 Zombies", 1, 11_000L));
        assertEquals("Ada: 6 Zombies", MissionTimerRules.append("Ada: 6 Zombies", 14, 0L));
        assertEquals("Jack: Iron East", MissionTimerRules.append("Jack: Iron East", -1, 0L));
        assertEquals("", MissionTimerRules.append("", 1, 11_000L));
        assertEquals("", MissionTimerRules.append(null, 1, 11_000L));
    }
}
