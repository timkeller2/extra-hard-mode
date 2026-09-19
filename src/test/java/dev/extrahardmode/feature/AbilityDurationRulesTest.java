package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AbilityDurationRulesTest {
    @Test
    void warnOnlyInLastFifteenSecondsWithoutAutoContinue() {
        assertEquals(300, AbilityDurationRules.WARN_TICKS);
        assertFalse(AbilityDurationRules.warn(301, false));
        assertTrue(AbilityDurationRules.warn(300, false));
        assertTrue(AbilityDurationRules.warn(1, false));
        assertFalse(AbilityDurationRules.warn(0, false));
        assertFalse(AbilityDurationRules.warn(1, true));
        assertFalse(AbilityDurationRules.warn(300, true));
    }

    @Test
    void alphaFadesOnlyWhenWarning() {
        assertEquals(1.0F, AbilityDurationRules.alpha(400, false), 1e-6);
        assertEquals(1.0F, AbilityDurationRules.alpha(10, true), 1e-6);
        assertEquals(1.0F, AbilityDurationRules.alpha(300, false), 1e-6);
        assertTrue(AbilityDurationRules.alpha(1, false) < 0.3F);
        float mid = AbilityDurationRules.alpha(150, false);
        assertTrue(mid > 0.25F && mid < 1.0F);
    }

    @Test
    void blinksWhenWarning() {
        assertTrue(AbilityDurationRules.visible(400, false, 0L));
        assertTrue(AbilityDurationRules.visible(10, true, 7L));
        assertTrue(AbilityDurationRules.visible(20, false, 0L));
        assertFalse(AbilityDurationRules.visible(20, false, 4L));
        assertFalse(AbilityDurationRules.visible(0, false, 0L));
    }

    @Test
    void argbKeepsRgbAndAppliesAlpha() {
        assertEquals(0xFF00FF00, AbilityDurationRules.argb(0x00FF00, 1.0F));
        assertEquals(0x4000FF00, AbilityDurationRules.argb(0x00FF00, 0.25F));
    }
}
