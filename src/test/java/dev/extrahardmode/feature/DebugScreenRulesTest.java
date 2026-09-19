package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DebugScreenRulesTest {
    @Test
    void f3OffByDefaultWhenEhmIsOn() {
        assertFalse(DebugScreenRules.allowed(true, false, false));
        assertTrue(DebugScreenRules.allowed(true, false, true));
        assertTrue(DebugScreenRules.allowed(true, true, false));
        assertTrue(DebugScreenRules.allowed(false, false, false));
    }
}
