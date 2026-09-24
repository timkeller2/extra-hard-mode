package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HungerRulesTest {
    @Test
    void stillWhenDeltaIsZero() {
        assertFalse(HungerRules.isMoving(0.0, 0.0, 0.0));
        assertTrue(HungerRules.isMoving(0.02, 0.0, 0.0));
        assertTrue(HungerRules.isMoving(0.0, 0.1, 0.0));
        // Walking ~4.3 m/s is ~0.215 blocks/tick; that must count as moving.
        assertTrue(HungerRules.isMoving(0.215, 0.0, 0.0));
    }

    @Test
    void exhaustionPerTickIsPerSecondOverTwenty() {
        assertEquals(4.0F / 30.0F, HungerRules.DEFAULT_MOVING_EXHAUSTION_PER_SECOND, 1.0E-6F);
        assertEquals(600.0F, 80.0F / HungerRules.DEFAULT_MOVING_EXHAUSTION_PER_SECOND, 0.01F);
        assertEquals(0.0125F, HungerRules.exhaustionPerTick(0.25F), 1.0E-6F);
        assertEquals(
                HungerRules.DEFAULT_MOVING_EXHAUSTION_PER_SECOND / 20.0F,
                HungerRules.exhaustionPerTick(HungerRules.DEFAULT_MOVING_EXHAUSTION_PER_SECOND),
                1.0E-6F);
    }

    @Test
    void lookingAndKeysCountAsActivity() {
        assertFalse(HungerRules.isLooking(0.0F, 0.0F));
        assertTrue(HungerRules.isLooking(1.0F, 0.0F));
        assertTrue(HungerRules.isLooking(0.0F, -0.2F));
        assertFalse(HungerRules.hasControlInput(false, false, false, false, false, false, false));
        assertTrue(HungerRules.hasControlInput(true, false, false, false, false, false, false));
        assertTrue(HungerRules.hasControlInput(false, false, false, false, true, false, false));
    }

    @Test
    void afkOnlyAfterTimeoutWithNoActivity() {
        assertEquals(600, HungerRules.activityTimeoutTicks(30));
        assertFalse(HungerRules.isAfk(100, 100L, 600));
        assertFalse(HungerRules.isAfk(699, 100L, 600));
        assertTrue(HungerRules.isAfk(700, 100L, 600));
        assertTrue(HungerRules.isAfk(100, null, 600));
        assertFalse(HungerRules.isAfk(10_000, 0L, 0));
        assertEquals(-2.0F, HungerRules.wrapDegrees(358.0F), 0.001F);
    }

    @Test
    void defaultSlowRegenIsFifteenSeconds() {
        assertEquals(300, HungerRules.DEFAULT_SLOW_REGEN_TICKS);
        assertEquals(15, HungerRules.DEFAULT_SLOW_REGEN_TICKS / HungerRules.TICKS_PER_SECOND);
        assertEquals(80, HungerRules.VANILLA_SLOW_REGEN_TICKS);
    }

    @Test
    void starvationIsOneFifthVanillaRate() {
        assertEquals(80, HungerRules.VANILLA_STARVE_TICKS);
        assertEquals(5, HungerRules.STARVE_SLOW_DIVISOR);
        assertEquals(400, HungerRules.starveTicks());
    }

    @Test
    void varietyAddsHungerUnlessFull() {
        assertEquals(11, HungerRules.foodAfterVarietyBonus(10, true));
        assertEquals(10, HungerRules.foodAfterVarietyBonus(10, false));
        assertEquals(20, HungerRules.foodAfterVarietyBonus(20, true));
    }

    @Test
    void varietyAddsSaturationOnlyWhenFull() {
        assertEquals(5.0F, HungerRules.saturationAfterVarietyBonus(18, 5.0F, true), 0.001F);
        assertEquals(6.0F, HungerRules.saturationAfterVarietyBonus(20, 5.0F, true), 0.001F);
        assertEquals(5.0F, HungerRules.saturationAfterVarietyBonus(20, 5.0F, false), 0.001F);
        assertEquals(20.0F, HungerRules.saturationAfterVarietyBonus(20, 20.0F, true), 0.001F);
    }

    @Test
    void uniqueWindowAddsHungerAndSaturation() {
        assertEquals(11, HungerRules.foodAfterUniqueWindowBonus(10, true));
        assertEquals(10, HungerRules.foodAfterUniqueWindowBonus(10, false));
        assertEquals(20, HungerRules.foodAfterUniqueWindowBonus(20, true));
        assertEquals(6.0F, HungerRules.saturationAfterUniqueWindowBonus(20, 5.0F, true), 0.001F);
        assertEquals(5.0F, HungerRules.saturationAfterUniqueWindowBonus(20, 5.0F, false), 0.001F);
        assertEquals(10.0F, HungerRules.saturationAfterUniqueWindowBonus(10, 10.0F, true), 0.001F);
        assertEquals(3, HungerRules.UNIQUE_WINDOW_EXPERIENCE);
        assertEquals(3, HungerRules.uniqueWindowExperience(true));
        assertEquals(0, HungerRules.uniqueWindowExperience(false));
    }

    @Test
    void repeatFoodPenaltyStartsAtFiveAndWorsensAtSeven() {
        assertEquals(0, HungerRules.repeatFoodPenalty(1));
        assertEquals(0, HungerRules.repeatFoodPenalty(4));
        assertEquals(1, HungerRules.repeatFoodPenalty(5));
        assertEquals(1, HungerRules.repeatFoodPenalty(6));
        assertEquals(2, HungerRules.repeatFoodPenalty(7));
        assertEquals(2, HungerRules.repeatFoodPenalty(20));
        assertEquals(9, HungerRules.foodAfterRepeatPenalty(10, 1));
        assertEquals(8, HungerRules.foodAfterRepeatPenalty(10, 2));
        assertEquals(0, HungerRules.foodAfterRepeatPenalty(1, 2));
        assertEquals(4.0F, HungerRules.saturationAfterRepeatPenalty(9, 5.0F, 1), 0.001F);
        assertEquals(8.0F, HungerRules.saturationAfterRepeatPenalty(8, 10.0F, 2), 0.001F);
    }

    @Test
    void foodRegenContinuesPastVanillaMaxHealth() {
        assertTrue(HungerRules.canRegenHealth(18, 19.0F, 20.0F));
        assertTrue(HungerRules.canEatMore(20, 25));
        assertFalse(HungerRules.canEatMore(25, 25));
        assertFalse(HungerRules.canEatMore(20, 20));
        assertFalse(HungerRules.canRegenHealth(18, 20.0F, 20.0F));
        assertTrue(HungerRules.canRegenHealth(20, 20.0F, 34.0F));
        assertFalse(HungerRules.canRegenHealth(17, 20.0F, 34.0F));
        assertFalse(HungerRules.canRegenHealth(20, 34.0F, 34.0F));
    }
}
