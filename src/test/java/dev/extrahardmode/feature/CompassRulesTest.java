package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void facingNameUsesYawWithEastAsTheRisingSun() {
        assertEquals("South", CompassRules.facingName(0));
        assertEquals("West", CompassRules.facingName(90));
        assertEquals("North", CompassRules.facingName(180));
        assertEquals("East", CompassRules.facingName(-90));
        assertEquals("East", CompassRules.facingName(270));
        assertEquals("Southwest", CompassRules.facingName(45));
        assertEquals("Southeast", CompassRules.facingName(-45));
        assertEquals("Northwest", CompassRules.facingName(135));
        assertEquals("Northeast", CompassRules.facingName(-135));
    }
}
