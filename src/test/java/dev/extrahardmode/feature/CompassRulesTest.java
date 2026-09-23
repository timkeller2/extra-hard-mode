package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CompassRulesTest {
    @Test
    void sneakPointsAtThePlayersOwnSpawn() {
        assertTrue(CompassRules.pointAtPersonalSpawn(true, true, true, true));
        assertFalse(CompassRules.pointAtPersonalSpawn(false, true, true, true));
        assertFalse(CompassRules.pointAtPersonalSpawn(true, false, true, true));
        assertFalse(CompassRules.pointAtPersonalSpawn(true, true, false, false));
        assertFalse(CompassRules.pointAtPersonalSpawn(true, true, true, false));
    }
}
