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
    void aHeadReturnHasHalfHealthAndFasterHarderHands() {
        assertEquals(0.5, ZombieRules.HEAD_HEALTH_FRACTION, 0.0);
        assertEquals(0.30, ZombieRules.HEAD_DAMAGE_BONUS, 0.0);
        assertEquals(0.20, ZombieRules.HEAD_SPEED_BONUS, 0.0);
        assertEquals(10.0F, ZombieRules.headHealth(20.0F), 0.0F);
        assertEquals(0.0F, ZombieRules.headHealth(0.0F), 0.0F);
        assertEquals(0.0F, ZombieRules.headHealth(-4.0F), 0.0F);
        ZombieRules.Hand[] south = ZombieRules.hands(0.0F, 1.95);
        assertEquals(2, south.length);
        assertEquals(1.95 * ZombieRules.HAND_HEIGHT, south[0].y(), 1.0E-9);
        assertEquals(south[0].y(), south[1].y(), 1.0E-9);
        assertEquals(1.95 * ZombieRules.HAND_SIDE, south[0].x(), 1.0E-9);
        assertEquals(-south[0].x(), south[1].x(), 1.0E-9);
        assertEquals(1.95 * ZombieRules.HAND_REACH, south[0].z(), 1.0E-9);
        assertEquals(south[0].z(), south[1].z(), 1.0E-9);
        ZombieRules.Hand[] west = ZombieRules.hands(90.0F, 2.0);
        assertEquals(-2.0 * ZombieRules.HAND_REACH, west[0].x(), 1.0E-9);
        assertEquals(2.0 * ZombieRules.HAND_SIDE, west[0].z(), 1.0E-9);
        assertEquals(-west[0].z(), west[1].z(), 1.0E-9);
        assertEquals(0.0, ZombieRules.hands(0.0F, -3.0)[0].y(), 0.0);
    }

    @Test
    void scaleDeltaMatchesDamageDelta() {
        assertEquals(-0.20, ZombieRules.scaleDelta(0.20), 1.0E-9);
        assertEquals(0.20, ZombieRules.scaleDelta(-0.20), 1.0E-9);
        assertEquals(0.0, ZombieRules.scaleDelta(0.0), 1.0E-9);
        assertEquals(ZombieRules.damageDelta(0.12), ZombieRules.scaleDelta(0.12), 1.0E-9);
    }
}
