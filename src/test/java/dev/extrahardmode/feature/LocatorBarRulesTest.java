package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LocatorBarRulesTest {
    @Test
    void onlyAGoldenCarrotArmsTheBar() {
        assertTrue(LocatorBarRules.isGoldenCarrot("minecraft:golden_carrot"));
        assertFalse(LocatorBarRules.isGoldenCarrot("minecraft:carrot"));
        assertFalse(LocatorBarRules.armed(false, false, 14.4F));
        assertTrue(LocatorBarRules.armed(false, true, 14.4F));
        assertFalse(LocatorBarRules.armed(false, true, 0.0F));
    }

    @Test
    void barStaysUntilSaturationIsGone() {
        assertTrue(LocatorBarRules.armed(true, false, 0.1F));
        assertFalse(LocatorBarRules.armed(true, false, 0.0F));
        assertFalse(LocatorBarRules.armed(true, true, -1.0F));
    }

    @Test
    void barOnSeesOthersAndBarOffSeesNobody() {
        assertTrue(LocatorBarRules.receives(true));
        assertFalse(LocatorBarRules.receives(false));
    }

    @Test
    void helpMentionsTheGoldenCarrotWindow() {
        String hunger = String.join(" ", EhmHelp.resolve("hunger").lines());
        assertTrue(hunger.contains("golden carrot"));
        assertTrue(hunger.contains("saturation"));
        assertTrue(hunger.contains("their bar is on too"));
    }
}
