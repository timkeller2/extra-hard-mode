package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BiomeBossesRulesTest {
    @Test
    void farEnoughUsesHorizontalDistance() {
        assertFalse(BiomeBossesRules.farEnough(0, 0, 300));
        assertFalse(BiomeBossesRules.farEnough(299, 0, 300));
        assertTrue(BiomeBossesRules.farEnough(300, 0, 300));
        assertTrue(BiomeBossesRules.farEnough(200, 250, 300));
        assertFalse(BiomeBossesRules.farEnough(180, 180, 300));
    }

    @Test
    void cooldownIsPerFamilyWallClock() {
        assertEquals(28_800_000L, BiomeBossesRules.cooldownMillis(8));
        assertTrue(BiomeBossesRules.cooldownElapsed(1000L, 0L, 28_800_000L));
        assertFalse(BiomeBossesRules.cooldownElapsed(1_000L, 500L, 28_800_000L));
        assertTrue(BiomeBossesRules.cooldownElapsed(28_800_500L, 500L, 28_800_000L));
        assertTrue(BiomeBossesRules.cooldownElapsed(10L, 1L, 0L));
    }

    @Test
    void spawnRollUsesPercent() {
        assertFalse(BiomeBossesRules.spawnRoll(0, 0));
        assertTrue(BiomeBossesRules.spawnRoll(0, 1));
        assertFalse(BiomeBossesRules.spawnRoll(1, 1));
        assertTrue(BiomeBossesRules.spawnRoll(99, 100));
    }

    @Test
    void extraMultiplierAmountIsDeltaFromOne() {
        assertEquals(5.0, BiomeBossesRules.extraMultiplierAmount(6.0), 0.001);
        assertEquals(11.0, BiomeBossesRules.extraMultiplierAmount(12.0), 0.001);
        assertEquals(0.0, BiomeBossesRules.extraMultiplierAmount(1.0), 0.001);
        assertEquals(0.0, BiomeBossesRules.extraMultiplierAmount(0.5), 0.001);
    }

    @Test
    void healthMultiplierIsInclusiveSixToTwelve() {
        assertEquals(6, BiomeBossesRules.randomHealthMultiplier(0, 6, 12));
        assertEquals(12, BiomeBossesRules.randomHealthMultiplier(6, 6, 12));
        assertEquals(6, BiomeBossesRules.randomHealthMultiplier(7, 6, 12));
        assertEquals(9, BiomeBossesRules.randomHealthMultiplier(3, 6, 12));
        assertEquals(6, BiomeBossesRules.randomHealthMultiplier(0, 12, 6));
    }

    @Test
    void armorTierSpansLeatherToDiamond() {
        assertEquals(0, BiomeBossesRules.armorTier(0));
        assertEquals(4, BiomeBossesRules.armorTier(4));
        assertEquals(0, BiomeBossesRules.armorTier(5));
        assertEquals(2, BiomeBossesRules.armorTier(7));
    }

    @Test
    void distanceStepsStartAfterMinDistance() {
        assertEquals(0, BiomeBossesRules.distanceSteps(300, 300, 25));
        assertEquals(0, BiomeBossesRules.distanceSteps(324.9, 300, 25));
        assertEquals(1, BiomeBossesRules.distanceSteps(325, 300, 25));
        assertEquals(10, BiomeBossesRules.distanceSteps(550, 300, 25));
        assertEquals(20, BiomeBossesRules.distanceSteps(800, 300, 25));
        assertEquals(0, BiomeBossesRules.distanceSteps(100, 300, 25));
    }

    @Test
    void difficultyIsOnePercentPerStep() {
        assertEquals(0.0, BiomeBossesRules.difficultyPercent(0, 1.0), 0.001);
        assertEquals(1.0, BiomeBossesRules.difficultyPercent(1, 1.0), 0.001);
        assertEquals(10.0, BiomeBossesRules.difficultyPercent(10, 1.0), 0.001);
        assertEquals(0.10, BiomeBossesRules.difficultyBonusAmount(10.0), 0.0001);
        assertEquals(0.0, BiomeBossesRules.difficultyBonusAmount(0.0), 0.0001);
    }

    @Test
    void treasureRisesThreeTimesAsFastAsDifficulty() {
        assertEquals(1.0, BiomeBossesRules.treasureMultiplier(0.0, 3.0), 0.001);
        assertEquals(1.03, BiomeBossesRules.treasureMultiplier(1.0, 3.0), 0.001);
        assertEquals(1.30, BiomeBossesRules.treasureMultiplier(10.0, 3.0), 0.001);
        assertEquals(1.60, BiomeBossesRules.treasureMultiplier(20.0, 3.0), 0.001);
    }

    @Test
    void eachDefeatCompoundsDifficultyAndTreasureByThirtyPercent() {
        assertEquals(1.0, BiomeBossesRules.defeatCompound(0), 1e-9);
        assertEquals(1.0, BiomeBossesRules.defeatCompound(-2), 1e-9);
        assertEquals(1.3, BiomeBossesRules.defeatCompound(1), 1e-9);
        assertEquals(1.69, BiomeBossesRules.defeatCompound(2), 1e-9);
        assertEquals(Math.pow(1.3, 7), BiomeBossesRules.defeatCompound(7), 1e-9);
        assertEquals(0.3, BiomeBossesRules.extraMultiplierAmount(1.3), 1e-9);
        assertEquals(0.69, BiomeBossesRules.extraMultiplierAmount(1.69), 1e-9);
        assertEquals(1.3, BiomeBossesRules.treasureWithDefeats(0.0, 3.0, 1), 1e-9);
        assertEquals(1.69, BiomeBossesRules.treasureWithDefeats(10.0, 3.0, 1), 1e-9);
        assertEquals(1.3 * Math.pow(1.3, 2), BiomeBossesRules.treasureWithDefeats(10.0, 3.0, 2), 1e-9);
        assertTrue(BiomeBossesRules.isThirdMilestone(3));
        assertFalse(BiomeBossesRules.isThirdMilestone(2));
        assertFalse(BiomeBossesRules.isThirdMilestone(4));
        assertTrue(BiomeBossesRules.isCreditsMilestone(7));
        assertFalse(BiomeBossesRules.isCreditsMilestone(6));
        assertFalse(BiomeBossesRules.isCreditsMilestone(8));
        assertEquals(1.3, BiomeBossesRules.DEFEAT_COMPOUND, 1e-9);
        assertEquals(3, BiomeBossesRules.THIRD_MILESTONE);
        assertEquals(7, BiomeBossesRules.CREDITS_MILESTONE);
        assertEquals(140, BiomeBossesRules.CREDITS_DELAY_TICKS);
    }

    @Test
    void scaleCountKeepsBaselineAndGrowsFartherOut() {
        assertEquals(4, BiomeBossesRules.scaleCount(4, 1.0));
        assertEquals(6, BiomeBossesRules.scaleCount(4, 1.5));
        assertEquals(1, BiomeBossesRules.scaleCount(1, 1.0));
        assertEquals(2, BiomeBossesRules.scaleCount(1, 1.6));
        assertEquals(0, BiomeBossesRules.scaleCount(0, 2.0));
        assertEquals(40, BiomeBossesRules.scalePercent(25, 1.6));
        assertEquals(100, BiomeBossesRules.scalePercent(25, 5.0));
        assertEquals(40, BiomeBossesRules.scaleXp(40, 1.0));
        assertEquals(52, BiomeBossesRules.scaleXp(40, 1.3));
    }
}
