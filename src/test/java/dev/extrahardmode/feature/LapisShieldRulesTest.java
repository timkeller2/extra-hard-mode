package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LapisShieldRulesTest {
    @Test
    void absorbIsCappedByTheHitAndTheLevel() {
        assertEquals(0.0F, LapisShieldRules.absorb(0.0F, 4.0F), 1.0E-4F);
        assertEquals(0.0F, LapisShieldRules.absorb(-2.0F, 4.0F), 1.0E-4F);
        assertEquals(0.0F, LapisShieldRules.absorb(5.0F, 0.0F), 1.0E-4F);
        assertEquals(3.0F, LapisShieldRules.absorb(3.0F, 5.0F), 1.0E-4F);
        assertEquals(2.5F, LapisShieldRules.absorb(10.0F, 2.5F), 1.0E-4F);
    }

    @Test
    void fullBlockSkipsHarmfulEffectsOnlyWhenTheHitFits() {
        assertTrue(LapisShieldRules.blocksHarmfulEffects(0.0F, 4.0F));
        assertTrue(LapisShieldRules.blocksHarmfulEffects(4.0F, 4.0F));
        assertFalse(LapisShieldRules.blocksHarmfulEffects(4.1F, 4.0F));
        assertFalse(LapisShieldRules.blocksHarmfulEffects(-1.0F, 4.0F));
        assertFalse(LapisShieldRules.blocksHarmfulEffects(1.0F, -1.0F));
    }

    @Test
    void fuelRateFollowsHowManyKindsAreOnHand() {
        assertEquals(3, LapisShieldRules.ONE_KIND_POINTS);
        assertEquals(5, LapisShieldRules.TWO_KIND_POINTS);
        assertEquals(7, LapisShieldRules.THREE_KIND_POINTS);
        assertEquals(0, LapisShieldRules.fuelCost(0.0F, 1));
        assertEquals(0, LapisShieldRules.fuelCost(3.0F, 0));
        assertEquals(1, LapisShieldRules.fuelCost(0.2F, 1));
        assertEquals(1, LapisShieldRules.fuelCost(3.0F, 1));
        assertEquals(2, LapisShieldRules.fuelCost(3.1F, 1));
        assertEquals(2, LapisShieldRules.fuelCost(6.0F, 1));
        assertEquals(1, LapisShieldRules.fuelCost(5.0F, 2));
        assertEquals(2, LapisShieldRules.fuelCost(5.1F, 2));
        assertEquals(1, LapisShieldRules.fuelCost(7.0F, 3));
        assertEquals(2, LapisShieldRules.fuelCost(7.1F, 3));
        assertEquals(3, LapisShieldRules.fuelCost(20.0F, 3));
        assertEquals(0, LapisShieldRules.durationUnits(0.9));
        assertEquals(2, LapisShieldRules.durationUnits(2.7));
        assertEquals(10, LapisShieldRules.durationUnits(10.0));
        assertEquals(20, LapisShieldRules.DURATION_FUEL_SECONDS);
        assertEquals(400, LapisShieldRules.TICKS_PER_MINUTE);
        assertEquals(0.0, LapisShieldRules.durationMinutes(0), 1.0E-9);
        assertEquals(0, LapisShieldRules.durationUnits(LapisShieldRules.durationMinutes(399)));
        assertEquals(1.0, LapisShieldRules.durationMinutes(LapisShieldRules.TICKS_PER_MINUTE), 1.0E-9);
        assertEquals(1, LapisShieldRules.durationUnits(LapisShieldRules.durationMinutes(400)));
        assertEquals(2.7, LapisShieldRules.durationMinutes(400 * 2 + 280), 1.0E-9);
        assertEquals(2, LapisShieldRules.durationUnits(LapisShieldRules.durationMinutes(400 * 2 + 280)));
        assertEquals(800L, LapisShieldRules.minuteTicks(2));
        assertEquals(0L, LapisShieldRules.minuteTicks(0));
        assertEquals(10, LapisShieldRules.ABSORB_EXTRA_SECONDS);
        assertEquals(200L, LapisShieldRules.absorbDurationTicks(0));
        assertEquals(600L, LapisShieldRules.absorbDurationTicks(1));
        assertEquals(1000L, LapisShieldRules.absorbDurationTicks(2));
    }

    @Test
    void paymentCyclesLargestAmountFirst() {
        assertNull(LapisShieldRules.plan(4.0F, 0, 0, 0.0));
        assertNull(LapisShieldRules.plan(4.0F, 0, 0, 0.9));
        assertNull(LapisShieldRules.plan(0.0F, 10, 10, 10.0));
        assertNull(LapisShieldRules.plan(3.0F, 0, 0, LapisShieldRules.durationMinutes(399)));

        LapisShieldRules.Payment only = LapisShieldRules.plan(6.0F, 4, 0, 0.0);
        assertEquals(new LapisShieldRules.Payment(2, 0, 0), only);

        LapisShieldRules.Payment minutesOnly = LapisShieldRules.plan(3.0F, 0, 0, 2.7);
        assertEquals(new LapisShieldRules.Payment(0, 0, 1), minutesOnly);
        assertNull(LapisShieldRules.plan(6.1F, 0, 0, 2.7));
        assertEquals(
                new LapisShieldRules.Payment(0, 0, 1),
                LapisShieldRules.plan(3.0F, 0, 0, LapisShieldRules.durationMinutes(400)));

        LapisShieldRules.Payment tie = LapisShieldRules.plan(7.0F, 10, 10, 10.0);
        assertEquals(new LapisShieldRules.Payment(1, 0, 0), tie);

        LapisShieldRules.Payment three = LapisShieldRules.plan(20.0F, 1, 10, 10.0);
        assertEquals(new LapisShieldRules.Payment(1, 1, 1), three);

        LapisShieldRules.Payment five = LapisShieldRules.plan(35.0F, 1, 10, 10.0);
        assertEquals(new LapisShieldRules.Payment(1, 2, 2), five);

        LapisShieldRules.Payment skip = LapisShieldRules.plan(40.0F, 1, 10, 10.0);
        assertEquals(new LapisShieldRules.Payment(1, 3, 2), skip);

        LapisShieldRules.Payment minutesLead = LapisShieldRules.plan(5.0F, 0, 10, 10.7);
        assertEquals(new LapisShieldRules.Payment(0, 0, 1), minutesLead);

        LapisShieldRules.Payment twoKinds = LapisShieldRules.plan(5.1F, 1, 1, 0.4);
        assertEquals(new LapisShieldRules.Payment(1, 1, 0), twoKinds);
        assertNull(LapisShieldRules.plan(10.1F, 1, 1, 0.0));
    }

    @Test
    void unpaidAbsorbLeavesTheHitIntact() {
        assertEquals(6.0F, LapisShieldRules.damageAfterAbsorb(6.0F, 0.0F), 1.0E-4F);
        assertEquals(2.5F, LapisShieldRules.damageAfterAbsorb(6.0F, 3.5F), 1.0E-4F);
        assertEquals(0.0F, LapisShieldRules.damageAfterAbsorb(2.0F, 2.0F), 1.0E-4F);
        assertEquals(0.0F, LapisShieldRules.damageAfterAbsorb(2.0F, 5.0F), 1.0E-4F);
    }
}
