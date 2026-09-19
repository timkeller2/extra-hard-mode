package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class CropGrowthRulesTest {
    @Test
    void vanillaDurationAlwaysAllowsTheTick() {
        Random random = new Random(1L);
        for (int i = 0; i < 20; i++) {
            assertTrue(CropGrowthRules.allowVanillaRandomTick(100, random));
            assertEquals(0, CropGrowthRules.extraRandomTicks(100, random));
        }
    }

    @Test
    void tripleDurationIsOneInThree() {
        Random random = new Random(2L);
        int allowed = 0;
        for (int i = 0; i < 3000; i++) {
            if (CropGrowthRules.allowVanillaRandomTick(300, random)) {
                allowed++;
            }
        }
        assertTrue(allowed > 800 && allowed < 1200, "got " + allowed);
        assertEquals(0, CropGrowthRules.extraRandomTicks(300, new Random(3L)));
    }

    @Test
    void halfDurationAddsOneExtraPass() {
        assertTrue(CropGrowthRules.allowVanillaRandomTick(50, new Random(4L)));
        assertEquals(1, CropGrowthRules.extraRandomTicks(50, new Random(4L)));
    }

    @Test
    void zeroDurationIsTreatedAsAtLeastOne() {
        assertEquals(1, CropGrowthRules.clampDurationPercent(0));
        assertTrue(CropGrowthRules.allowVanillaRandomTick(0, new Random(0L)));
    }

    @Test
    void sugarCaneDurationIsTenTimesVanilla() {
        assertEquals(1000, CropGrowthRules.SUGAR_CANE_DURATION_PERCENT);
        Random random = new Random(5L);
        int allowed = 0;
        for (int i = 0; i < 10_000; i++) {
            if (CropGrowthRules.allowVanillaRandomTick(CropGrowthRules.SUGAR_CANE_DURATION_PERCENT, random)) {
                allowed++;
            }
        }
        assertTrue(allowed > 800 && allowed < 1200, "got " + allowed);
    }

    @Test
    void netherWartDurationIsTwentyTimesVanilla() {
        assertEquals(2000, CropGrowthRules.NETHER_WART_DURATION_PERCENT);
        Random random = new Random(6L);
        int allowed = 0;
        for (int i = 0; i < 20_000; i++) {
            if (CropGrowthRules.allowVanillaRandomTick(CropGrowthRules.NETHER_WART_DURATION_PERCENT, random)) {
                allowed++;
            }
        }
        assertTrue(allowed > 800 && allowed < 1200, "got " + allowed);
    }

    @Test
    void treeDurationIsTenTimesVanilla() {
        assertEquals(1000, CropGrowthRules.TREE_DURATION_PERCENT);
        Random random = new Random(8L);
        int allowed = 0;
        for (int i = 0; i < 10_000; i++) {
            if (CropGrowthRules.allowVanillaRandomTick(CropGrowthRules.TREE_DURATION_PERCENT, random)) {
                allowed++;
            }
        }
        assertTrue(allowed > 800 && allowed < 1200, "got " + allowed);
    }

    @Test
    void saplingDropsKeepHalfOnAverage() {
        assertEquals(50, CropGrowthRules.SAPLING_DROP_KEEP_PERCENT);
        assertEquals(0, CropGrowthRules.scaledDropCount(0, 50, new Random(1L)));
        assertEquals(1, CropGrowthRules.scaledDropCount(2, 50, new Random(1L)));
        assertEquals(2, CropGrowthRules.scaledDropCount(2, 100, new Random(1L)));
        assertEquals(0, CropGrowthRules.scaledDropCount(5, 0, new Random(1L)));
        Random random = new Random(9L);
        int kept = 0;
        for (int i = 0; i < 10_000; i++) {
            kept += CropGrowthRules.scaledDropCount(1, 50, random);
        }
        assertTrue(kept > 4500 && kept < 5500, "got " + kept);
    }

    @Test
    void stemFruitDurationIsTenTimesVanilla() {
        assertEquals(1000, CropGrowthRules.STEM_FRUIT_DURATION_PERCENT);
        Random random = new Random(6L);
        int allowed = 0;
        for (int i = 0; i < 10_000; i++) {
            if (CropGrowthRules.allowVanillaRandomTick(CropGrowthRules.STEM_FRUIT_DURATION_PERCENT, random)) {
                allowed++;
            }
        }
        assertTrue(allowed > 800 && allowed < 1200, "got " + allowed);
    }

    @Test
    void stemVineWeedChanceStartsAtZeroAndRisesFivePercentPerFruit() {
        assertEquals(0, CropGrowthRules.stemVineDeathChancePercent(0));
        assertEquals(5, CropGrowthRules.stemVineDeathChancePercent(1));
        assertEquals(10, CropGrowthRules.stemVineDeathChancePercent(2));
        assertEquals(100, CropGrowthRules.stemVineDeathChancePercent(20));
        assertEquals(100, CropGrowthRules.stemVineDeathChancePercent(21));
        assertFalse(CropGrowthRules.stemVineDies(0, new Random(1L)));
        assertTrue(CropGrowthRules.stemVineDies(20, new Random(1L)));
        Random random = new Random(7L);
        int died = 0;
        for (int i = 0; i < 10_000; i++) {
            if (CropGrowthRules.stemVineDies(1, random)) {
                died++;
            }
        }
        assertTrue(died > 350 && died < 650, "got " + died);
    }

    @Test
    void pumpkinAndMelonVinesDropNoSeeds() {
        assertTrue(CropGrowthRules.isStemSeedItemId("minecraft:melon_seeds"));
        assertTrue(CropGrowthRules.isStemSeedItemId("minecraft:pumpkin_seeds"));
        assertFalse(CropGrowthRules.isStemSeedItemId("minecraft:wheat_seeds"));
        assertFalse(CropGrowthRules.isStemSeedItemId("minecraft:pumpkin"));
        assertFalse(CropGrowthRules.isStemSeedItemId(null));
    }

    @Test
    void seasonalLossFallsToHalfThenRisesToTriple() {
        assertEquals(3, CropGrowthRules.SEASON_MAX_MULTIPLIER);
        assertEquals(0L, CropGrowthRules.dayIndex(0));
        assertEquals(0L, CropGrowthRules.dayIndex(23999));
        assertEquals(1L, CropGrowthRules.dayIndex(24000));
        assertEquals(25.0, CropGrowthRules.seasonalLossRate(25, 0), 1e-9);
        assertEquals(24.0, CropGrowthRules.seasonalLossRate(25, 1), 1e-9);
        assertEquals(23.0, CropGrowthRules.seasonalLossRate(25, 2), 1e-9);
        assertEquals(13.0, CropGrowthRules.seasonalLossRate(25, 12), 1e-9);
        assertEquals(14.0, CropGrowthRules.seasonalLossRate(25, 13), 1e-9);
        assertEquals(75.0, CropGrowthRules.seasonalLossRate(25, 12 + 62), 1e-9);
        assertEquals(74.0, CropGrowthRules.seasonalLossRate(25, 12 + 63), 1e-9);
        assertEquals(13.0, CropGrowthRules.seasonalLossRate(25, 12 + 124), 1e-9);
        assertEquals(14.0, CropGrowthRules.seasonalLossRate(25, 12 + 125), 1e-9);
        assertEquals("26", CropGrowthRules.lossRateLabel(26.0));
        assertEquals("12.5", CropGrowthRules.lossRateLabel(12.5));
        assertEquals(60.0, CropGrowthRules.BEE_INACTIVE_LOSS_RATE, 1e-9);
        assertFalse(CropGrowthRules.beesInactive(60.0));
        assertTrue(CropGrowthRules.beesInactive(61.0));
        assertFalse(CropGrowthRules.beesInactive(CropGrowthRules.seasonalLossRate(25, 59)));
        assertTrue(CropGrowthRules.beesInactive(CropGrowthRules.seasonalLossRate(25, 60)));
        assertTrue(CropGrowthRules.beesInactive(CropGrowthRules.seasonalLossRate(25, 74)));
        assertTrue(CropGrowthRules.beesInactive(CropGrowthRules.seasonalLossRate(25, 88)));
        assertFalse(CropGrowthRules.beesInactive(CropGrowthRules.seasonalLossRate(25, 89)));
    }

    @Test
    void seasonalMeatReduceScalesWithLoss() {
        assertEquals(0, CropGrowthRules.largeAnimalMeatReduce(25.0));
        assertEquals(1, CropGrowthRules.largeAnimalMeatReduce(26.0));
        assertEquals(1, CropGrowthRules.largeAnimalMeatReduce(60.0));
        assertEquals(2, CropGrowthRules.largeAnimalMeatReduce(61.0));
        assertEquals(0, CropGrowthRules.chickenMeatReduce(60.0));
        assertEquals(1, CropGrowthRules.chickenMeatReduce(61.0));
        assertEquals(0, CropGrowthRules.MEAT_DROP_MIN);
        assertEquals(0, CropGrowthRules.reducedMeatCount(0, 2));
        assertEquals(0, CropGrowthRules.reducedMeatCount(1, 1));
        assertEquals(0, CropGrowthRules.reducedMeatCount(1, 2));
        assertEquals(0, CropGrowthRules.reducedMeatCount(2, 2));
        assertEquals(2, CropGrowthRules.reducedMeatCount(3, 1));
        int[] stacks = {1, 3};
        CropGrowthRules.reduceStackCounts(stacks, 2);
        assertEquals(1, stacks[0]);
        assertEquals(1, stacks[1]);
        int[] one = {1};
        CropGrowthRules.reduceStackCounts(one, 2);
        assertEquals(0, one[0]);
    }

    @Test
    void seasonalDurationMatchesLossRatio() {
        assertEquals(300, CropGrowthRules.seasonalDurationPercent(300, 25, 0));
        assertEquals(288, CropGrowthRules.seasonalDurationPercent(300, 25, 1));
        assertEquals(156, CropGrowthRules.seasonalDurationPercent(300, 25, 12));
        assertEquals(900, CropGrowthRules.seasonalDurationPercent(300, 25, 12 + 62));
        assertEquals(1000, CropGrowthRules.seasonalDurationPercent(1000, 25, 0));
        assertEquals(520, CropGrowthRules.seasonalDurationPercent(1000, 25, 12));
        assertEquals(300, CropGrowthRules.durationPercent(300, 25, 25, 0));
        assertEquals(420, CropGrowthRules.durationPercent(300, 25, 25, 10));
        assertEquals(120, CropGrowthRules.durationPercent(300, 25, 25, -15));
        assertEquals(156, CropGrowthRules.durationPercent(300, 25, 13, 0));
        assertEquals(300, CropGrowthRules.durationPercent(300, 25, 13, 12));
        assertEquals(100, CropGrowthRules.MIN_DURATION_PERCENT);
        assertEquals(100, CropGrowthRules.durationPercent(300, 25, 25, -20));
        assertEquals(100, CropGrowthRules.durationPercent(300, 25, 25, -100));
        assertEquals(100, CropGrowthRules.durationPercent(300, 25, 15, -35));
        assertEquals(100, CropGrowthRules.durationPercent(300, 25, 15, -15));
    }

    @Test
    void composterReadyWaitIsTwoHundredTimesVanilla() {
        assertEquals(20, CropGrowthRules.VANILLA_COMPOSTER_READY_DELAY);
        assertEquals(200, CropGrowthRules.COMPOSTER_SPEED_DIVISOR);
        assertEquals(4000, CropGrowthRules.composterReadyDelay(20));
        assertEquals(200, CropGrowthRules.composterReadyDelay(1));
    }

    @Test
    void hoeSoilModifierUsesLetItGrowQuality() {
        assertEquals(10, CropGrowthRules.hoeSoilModifier(0));
        assertEquals(5, CropGrowthRules.hoeSoilModifier(1));
        assertEquals(0, CropGrowthRules.hoeSoilModifier(2));
        assertEquals(-10, CropGrowthRules.hoeSoilModifier(3));
        assertEquals(-20, CropGrowthRules.hoeSoilModifier(4));
        assertEquals(-30, CropGrowthRules.hoeSoilModifier(5));
        assertEquals(2, CropGrowthRules.HOE_SOIL_GOOD_FACTOR);
        assertEquals(10, CropGrowthRules.hoeSoilModifier(AbilityRules.growHoeBonus("minecraft:wooden_hoe")));
        assertEquals(-10, CropGrowthRules.hoeSoilModifier(AbilityRules.growHoeBonus("minecraft:diamond_hoe")));
        assertEquals(-20, CropGrowthRules.hoeSoilModifier(AbilityRules.growHoeBonus("minecraft:golden_hoe")));
        assertEquals(-30, CropGrowthRules.hoeSoilModifier(AbilityRules.growHoeBonus("minecraft:netherite_hoe")));
        assertEquals(10, CropGrowthRules.afterHandHarvest(5));
        assertEquals(-10, CropGrowthRules.afterHandHarvest(-15));
        assertEquals(5, CropGrowthRules.HAND_HARVEST_SOIL_INCREASE);
        assertEquals("-10", CropGrowthRules.modifierLabel(10));
        assertEquals("0", CropGrowthRules.modifierLabel(0));
        assertEquals("+15", CropGrowthRules.modifierLabel(-15));
        assertEquals(15, CropGrowthRules.displayedModifier(-15));
        assertEquals(-13, CropGrowthRules.afterHandHarvest(-18));
        assertEquals(-5, CropGrowthRules.displayedDelta(-13, -18));
        assertEquals("13%", CropGrowthRules.percentLabel(13));
        assertEquals(CropGrowthRules.SOIL_LOOK_COLOR_GOOD, CropGrowthRules.soilLookColor(13));
        assertEquals(CropGrowthRules.SOIL_LOOK_COLOR_GOOD, CropGrowthRules.soilLookColor(0));
        assertEquals(CropGrowthRules.SOIL_LOOK_COLOR_BAD, CropGrowthRules.soilLookColor(-1));
        assertEquals(CropGrowthRules.SOIL_LOOK_COLOR_BAD, CropGrowthRules.soilLookColor(-13));
        assertEquals("-5%", CropGrowthRules.signedPercentLabel(-5));
        assertEquals("+5%", CropGrowthRules.signedPercentLabel(5));
        assertEquals("0%", CropGrowthRules.signedPercentLabel(0));
        assertEquals(5, CropGrowthRules.SAME_CROP_REPLANT_INCREASE);
        assertEquals(-13, CropGrowthRules.afterSameCropReplant(-18));
        assertTrue(CropGrowthRules.isSameCrop("minecraft:wheat", "minecraft:wheat"));
        assertFalse(CropGrowthRules.isSameCrop("minecraft:wheat", "minecraft:carrots"));
        assertFalse(CropGrowthRules.isSameCrop(null, "minecraft:wheat"));
        assertFalse(CropGrowthRules.isSameCrop("minecraft:wheat", ""));
        assertTrue(CropGrowthRules.modifierChangeIsGood(0, null));
        assertTrue(CropGrowthRules.modifierChangeIsGood(-5, null));
        assertFalse(CropGrowthRules.modifierChangeIsGood(5, null));
        assertTrue(CropGrowthRules.modifierChangeIsGood(-3, 0));
        assertTrue(CropGrowthRules.modifierChangeIsGood(5, 5));
        assertFalse(CropGrowthRules.modifierChangeIsGood(-13, -18));
        assertEquals("13% (-5%)", CropGrowthRules.modifierChangeLabel(-13, -18));
        assertEquals("3% (+3%)", CropGrowthRules.modifierChangeLabel(-3, 0));
        assertEquals("-5% (-5%)", CropGrowthRules.modifierChangeLabel(5, 0));
        assertEquals("0% (0%)", CropGrowthRules.modifierChangeLabel(0, 0));
        assertTrue(CropGrowthRules.modifierIsPenalty(1));
        assertFalse(CropGrowthRules.modifierIsPenalty(0));
        assertFalse(CropGrowthRules.modifierIsPenalty(-5));
        assertEquals(7, CropGrowthRules.firstTillModifier(7, 0));
        assertEquals(-3, CropGrowthRules.firstTillModifier(7, 10));
        assertEquals(-25, CropGrowthRules.firstTillModifier(1, 40));
        assertEquals(1, CropGrowthRules.firstTillModifier(0, 0));
        assertEquals(10, CropGrowthRules.firstTillModifier(99, 0));
        assertEquals(-3, CropGrowthRules.afterGrowWork(0));
        assertEquals(-8, CropGrowthRules.afterGrowWork(-5));
        assertEquals(7, CropGrowthRules.afterGrowWork(10));
        assertEquals(3, CropGrowthRules.HOE_WORK_STEP);
        assertEquals(-5, CropGrowthRules.afterBoneMeal(0));
        assertEquals(-10, CropGrowthRules.afterBoneMeal(-5));
        assertEquals(5, CropGrowthRules.afterBoneMeal(10));
        assertEquals(5, CropGrowthRules.BONE_MEAL_SOIL_IMPROVE);
        assertEquals(1, CropGrowthRules.HOE_WORK_STEP_WORSE_MIN);
        assertEquals(4, CropGrowthRules.HOE_WORK_STEP_WORSE_MAX);
        assertEquals(1, CropGrowthRules.hoeWorkStep(0, 10));
        assertEquals(1, CropGrowthRules.hoeWorkStep(5, 10));
        assertEquals(3, CropGrowthRules.hoeWorkStep(-20, 10));
        assertEquals(4, CropGrowthRules.hoeWorkStep(-30, 10));
        assertEquals(3, CropGrowthRules.hoeWorkStep(10, 0));
        assertEquals(1, CropGrowthRules.afterHoeWork(0, 0));
        assertEquals(6, CropGrowthRules.afterHoeWork(5, 0));
        assertEquals(10, CropGrowthRules.afterHoeWork(10, 0));
        assertEquals(9, CropGrowthRules.afterHoeWork(8, 0));
        assertEquals(10, CropGrowthRules.afterHoeWork(9, 0));
        assertEquals(-17, CropGrowthRules.afterHoeWork(-20, 0));
        assertEquals(-26, CropGrowthRules.afterHoeWork(-30, 0));
        assertEquals(-3, CropGrowthRules.afterHoeWork(0, 5));
        assertEquals(-8, CropGrowthRules.afterHoeWork(-5, 5));
        assertEquals(-18, CropGrowthRules.afterHoeWork(-15, 5));
        assertEquals(-30, CropGrowthRules.afterHoeWork(-30, 5));
        assertEquals(0, CropGrowthRules.afterHoeWork(0, 2));
        assertEquals(2, CropGrowthRules.afterHoeWork(5, 2));
        assertEquals(-4, CropGrowthRules.afterHoeWork(-5, 2));
        assertEquals(7, CropGrowthRules.afterHoeWork(10, 1));
        assertEquals(1, CropGrowthRules.afterHoeWork(0, 1));
        assertEquals(-12, CropGrowthRules.afterHoeWork(-15, 0));
        assertEquals(0, CropGrowthRules.stepToward(0, 0, 5));
    }

    @Test
    void soilModifierChangesLossChance() {
        assertEquals(35, CropGrowthRules.lossChance(25, 10));
        assertEquals(26, CropGrowthRules.lossChance(51, -25));
        assertEquals(10, CropGrowthRules.lossChance(25, -15));
        assertEquals(0, CropGrowthRules.lossChance(5, -15));
        assertEquals(100, CropGrowthRules.lossChance(90, 20));
        assertEquals(28, CropGrowthRules.lossChance(25.4, 3));
    }

    @Test
    void rollLossRespectsChance() {
        assertFalse(CropGrowthRules.rollLoss(0, new Random(1L)));
        assertTrue(CropGrowthRules.rollLoss(100, new Random(1L)));
        int hits = 0;
        Random random = new Random(2L);
        for (int i = 0; i < 200; i++) {
            if (CropGrowthRules.rollLoss(25, random)) {
                hits++;
            }
        }
        assertTrue(hits > 20 && hits < 80, "25% of 200 should land near 50, was " + hits);
    }

    @Test
    void sugarCaneSecondSegmentIsOneHighAtMaxAge() {
        assertTrue(CropGrowthRules.sugarCaneGrowsSecondSegment(15, true, false));
        assertFalse(CropGrowthRules.sugarCaneGrowsSecondSegment(14, true, false));
        assertFalse(CropGrowthRules.sugarCaneGrowsSecondSegment(15, false, false));
        assertFalse(CropGrowthRules.sugarCaneGrowsSecondSegment(15, true, true));
        assertEquals(15, CropGrowthRules.SUGAR_CANE_GROW_AGE);
    }
}
