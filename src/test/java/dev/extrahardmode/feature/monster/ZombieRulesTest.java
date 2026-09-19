package dev.extrahardmode.feature.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ZombieRulesTest {
    @Test
    void speedDeltaMapsUnitOntoPlusOrMinusTwentyPercent() {
        assertEquals(-0.20, ZombieRules.speedDelta(0.0), 1.0E-9);
        assertEquals(0.0, ZombieRules.speedDelta(0.5), 1.0E-9);
        assertEquals(0.20, ZombieRules.speedDelta(1.0), 1.0E-9);
        assertEquals(-0.10, ZombieRules.speedDelta(0.25), 1.0E-9);
        assertEquals(0.10, ZombieRules.speedDelta(0.75), 1.0E-9);
    }

    @Test
    void speedDeltaClampsOutsideUnitRange() {
        assertEquals(-0.20, ZombieRules.speedDelta(-2.0), 1.0E-9);
        assertEquals(0.20, ZombieRules.speedDelta(3.0), 1.0E-9);
    }

    @Test
    void damageDeltaNegatesSpeedPercent() {
        assertEquals(-0.20, ZombieRules.damageDelta(0.20), 1.0E-9);
        assertEquals(0.20, ZombieRules.damageDelta(-0.20), 1.0E-9);
        assertEquals(0.0, ZombieRules.damageDelta(0.0), 1.0E-9);
        assertEquals(0.07, ZombieRules.damageDelta(-0.07), 1.0E-9);
        assertEquals(-ZombieRules.speedDelta(0.8), ZombieRules.damageDelta(ZombieRules.speedDelta(0.8)), 1.0E-9);
    }

    @Test
    void scaleDeltaMatchesDamageDelta() {
        assertEquals(-0.20, ZombieRules.scaleDelta(0.20), 1.0E-9);
        assertEquals(0.20, ZombieRules.scaleDelta(-0.20), 1.0E-9);
        assertEquals(0.0, ZombieRules.scaleDelta(0.0), 1.0E-9);
        assertEquals(ZombieRules.damageDelta(0.12), ZombieRules.scaleDelta(0.12), 1.0E-9);
    }
}
