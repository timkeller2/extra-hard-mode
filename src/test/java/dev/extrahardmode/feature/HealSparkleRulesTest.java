package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HealSparkleRulesTest {
    @Test
    void fullSparklePaysTheWholeCast() {
        float remaining = 7.5F;
        float paid = 0.0F;
        for (long ticksLeft = AbilityRules.HEAL_SPARKLE_TICKS; ticksLeft >= 1L; ticksLeft--) {
            HealSparkleRules.Slice slice = HealSparkleRules.slice(remaining, ticksLeft);
            assertTrue(slice.apply() > 0.0F);
            paid += slice.apply();
            remaining = slice.remaining();
        }
        assertEquals(0.0F, remaining, 0.0001F);
        assertEquals(7.5F, paid, 0.0001F);
    }

    @Test
    void lastTickPaysWhateverIsLeft() {
        HealSparkleRules.Slice slice = HealSparkleRules.slice(1.25F, 1L);
        assertEquals(1.25F, slice.apply(), 0.0F);
        assertEquals(0.0F, slice.remaining(), 0.0F);
    }

    @Test
    void nothingOwedPaysNothing() {
        HealSparkleRules.Slice slice = HealSparkleRules.slice(0.0F, 40L);
        assertEquals(0.0F, slice.apply(), 0.0F);
        assertEquals(0.0F, slice.remaining(), 0.0F);
        slice = HealSparkleRules.slice(2.0F, 0L);
        assertEquals(0.0F, slice.apply(), 0.0F);
        assertEquals(2.0F, slice.remaining(), 0.0F);
    }
}
