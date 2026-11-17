package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
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
        assertEquals(25, BiomeBossesRules.DEFAULT_HEAVY_WEAPON_DROP_PERCENT);
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

    @Test
    void sameFamilyCanSpawnAgainFiveHundredBlocksAway() {
        assertEquals(500, BiomeBossesRules.FAMILY_SEPARATION_BLOCKS);
        var here = new BiomeBossesRules.SpawnSite("minecraft:overworld", 0, 64, 0);
        var far = new BiomeBossesRules.SpawnSite("minecraft:overworld", 1000, 70, 0);
        var high = new BiomeBossesRules.SpawnSite("minecraft:overworld", 0, 200, 0);
        List<BiomeBossesRules.SpawnSite> sites = List.of(here);
        assertTrue(BiomeBossesRules.clearOfFamilySpawns(null, "minecraft:overworld", 0, 0, 500));
        assertTrue(BiomeBossesRules.clearOfFamilySpawns(List.of(), "minecraft:overworld", 0, 0, 500));
        assertTrue(BiomeBossesRules.clearOfFamilySpawns(sites, null, 0, 0, 500));
        assertTrue(BiomeBossesRules.clearOfFamilySpawns(sites, "", 0, 0, 500));
        assertTrue(BiomeBossesRules.clearOfFamilySpawns(sites, "minecraft:the_nether", 0, 0, 500));
        assertFalse(BiomeBossesRules.clearOfFamilySpawns(sites, "minecraft:overworld", 499, 0, 500));
        assertTrue(BiomeBossesRules.clearOfFamilySpawns(sites, "minecraft:overworld", 500, 0, 500));
        assertTrue(BiomeBossesRules.clearOfFamilySpawns(sites, "minecraft:overworld", 300, 400, 500));
        assertFalse(BiomeBossesRules.clearOfFamilySpawns(sites, "minecraft:overworld", 299, 400, 500));
        assertFalse(BiomeBossesRules.clearOfFamilySpawns(List.of(high), "minecraft:overworld", 0, 100, 500));
        assertFalse(BiomeBossesRules.clearOfFamilySpawns(List.of(here, far), "minecraft:overworld", 100, 0, 500));
        assertTrue(BiomeBossesRules.clearOfFamilySpawns(List.of(here, far), "minecraft:overworld", 500, 0, 500));
        assertTrue(BiomeBossesRules.clearOfFamilySpawns(
                Arrays.asList(here, null), "minecraft:overworld", 500, 0, 500));
    }

    @Test
    void healthBarShowsWithinSixtyFourBlocks() {
        assertEquals(64.0, BiomeBossesRules.BAR_RANGE_BLOCKS, 0.0);
        assertTrue(BiomeBossesRules.withinBarRange(0.0));
        assertTrue(BiomeBossesRules.withinBarRange(64.0 * 64.0));
        assertFalse(BiomeBossesRules.withinBarRange(64.0 * 64.0 + 0.01));
        assertFalse(BiomeBossesRules.withinBarRange(Double.NaN));
        assertEquals(0.0F, BiomeBossesRules.barProgress(0.0F, 100.0F), 0.0F);
        assertEquals(0.5F, BiomeBossesRules.barProgress(50.0F, 100.0F), 0.0001F);
        assertEquals(1.0F, BiomeBossesRules.barProgress(100.0F, 100.0F), 0.0F);
        assertEquals(1.0F, BiomeBossesRules.barProgress(150.0F, 100.0F), 0.0F);
        assertEquals(0.0F, BiomeBossesRules.barProgress(-5.0F, 100.0F), 0.0F);
        assertEquals(0.0F, BiomeBossesRules.barProgress(10.0F, 0.0F), 0.0F);
        assertEquals(0.0F, BiomeBossesRules.barProgress(10.0F, -1.0F), 0.0F);
        assertEquals(0.0F, BiomeBossesRules.barProgress(Float.NaN, 100.0F), 0.0F);
    }

    @Test
    void spawnClearsEightyBlocksAndSearchesPastThat() {
        assertEquals(80, BiomeBossesRules.SPAWN_CLEARANCE_BLOCKS);
        assertTrue(BiomeBossesRules.tooClose(79.9, 0, 0, 80));
        assertTrue(BiomeBossesRules.tooClose(80, 0, 0, 80));
        assertFalse(BiomeBossesRules.tooClose(80.1, 0, 0, 80));
        assertTrue(BiomeBossesRules.tooClose(0, 0, 0, 80));
        assertTrue(BiomeBossesRules.tooClose(46, 46, 46, 80));
        assertFalse(BiomeBossesRules.tooClose(50, 50, 50, 80));
        assertFalse(BiomeBossesRules.tooClose(10, 0, 0, 0));
        assertEquals(84, BiomeBossesRules.spawnSearchDistance(0, 80, 32));
        assertEquals(115, BiomeBossesRules.spawnSearchDistance(31, 80, 32));
        assertEquals(84, BiomeBossesRules.spawnSearchDistance(32, 80, 32));
    }

    @Test
    void broodSpitIsHalfDamageOnAFiveSecondClock() {
        assertEquals(100, BiomeBossesRules.BROOD_SPIT_INTERVAL_TICKS);
        assertEquals(60, BiomeBossesRules.BROOD_SPIT_BLIND_TICKS);
        assertEquals(0.5, BiomeBossesRules.BROOD_SPIT_DAMAGE_FRACTION, 0.0);
        assertEquals(1.75F, BiomeBossesRules.spitDamage(3.5), 0.0001F);
        assertEquals(0.0F, BiomeBossesRules.spitDamage(0.0), 0.0F);
        assertEquals(0.0F, BiomeBossesRules.spitDamage(-2.0), 0.0F);
        assertFalse(BiomeBossesRules.spitReady(100L, 0L, 100));
        assertFalse(BiomeBossesRules.spitReady(100L, 1L, 100));
        assertTrue(BiomeBossesRules.spitReady(101L, 1L, 100));
        assertTrue(BiomeBossesRules.spitReady(50L, 1L, 0));
        assertEquals(24.0, BiomeBossesRules.BROOD_FOLLOW_RANGE, 0.0);
        assertEquals(0.15, BiomeBossesRules.BROOD_SPEED_BONUS, 0.0);
    }

    @Test
    void ironPickaxeBreaksStoneInEightTicksAndCannotBreakBedrock() {
        assertEquals(0.0F, BiomeBossesRules.toolDestroyProgress(-1.0F, 6.0F, true), 0.0F);
        assertEquals(1.0F, BiomeBossesRules.toolDestroyProgress(0.0F, 6.0F, true), 0.0F);
        float stone = BiomeBossesRules.toolDestroyProgress(1.5F, 6.0F, true);
        assertEquals(6.0F / 1.5F / 30.0F, stone, 0.00001F);
        float accumulated = 0.0F;
        int ticks = 0;
        while (accumulated < 1.0F && ticks < 20) {
            accumulated += stone;
            ticks++;
        }
        assertEquals(8, ticks);
        float obsidian = BiomeBossesRules.toolDestroyProgress(50.0F, 6.0F, false);
        assertEquals(6.0F / 50.0F / 100.0F, obsidian, 0.0000001F);
        assertEquals(1.0F / 0.5F / 30.0F, BiomeBossesRules.toolDestroyProgress(0.5F, 1.0F, true), 0.00001F);
    }

    @Test
    void bossMinesLikeThreeDiamondPicksTimesTheBossNumber() {
        assertEquals(8.0F, BiomeBossesRules.DIAMOND_PICKAXE_SPEED, 0.0F);
        assertEquals(3, BiomeBossesRules.MINE_PICKAXE_COUNT);
        assertEquals(1, BiomeBossesRules.livingBossNumber(0));
        assertEquals(2, BiomeBossesRules.livingBossNumber(1));
        assertEquals(4, BiomeBossesRules.livingBossNumber(3));
        assertEquals(1, BiomeBossesRules.livingBossNumber(-5));
        assertEquals(24.0F, BiomeBossesRules.bossBlockMineSpeed(8.0F, 1, true), 0.0001F);
        assertEquals(48.0F, BiomeBossesRules.bossBlockMineSpeed(8.0F, 2, true), 0.0001F);
        assertEquals(96.0F, BiomeBossesRules.bossBlockMineSpeed(8.0F, 4, true), 0.0001F);
        assertEquals(3.0F, BiomeBossesRules.bossBlockMineSpeed(1.0F, 1, false), 0.0001F);
        assertEquals(12.0F, BiomeBossesRules.bossBlockMineSpeed(1.0F, 4, false), 0.0001F);
        assertEquals(24.0F, BiomeBossesRules.bossBlockMineSpeed(2.0F, 1, true), 0.0001F);
        float stone = BiomeBossesRules.toolDestroyProgress(1.5F, BiomeBossesRules.bossBlockMineSpeed(8.0F, 1, true), true);
        float accumulated = 0.0F;
        int ticks = 0;
        while (accumulated < 1.0F && ticks < 10) {
            accumulated += stone;
            ticks++;
        }
        assertEquals(2, ticks);
    }

    @Test
    void bossJumpPeaksAtFourBlocksAndLeapsFourTowardTheTarget() {
        assertEquals(80, BiomeBossesRules.BOSS_JUMP_INTERVAL_TICKS);
        assertEquals(4.0, BiomeBossesRules.BOSS_JUMP_HEIGHT, 0.0);
        assertEquals(4.0, BiomeBossesRules.BOSS_JUMP_DISTANCE, 0.0);
        double vy = BiomeBossesRules.jumpVelocity(BiomeBossesRules.BOSS_JUMP_HEIGHT);
        assertEquals(4.0, BiomeBossesRules.simulatedJumpPeak(vy), 0.05);
        int air = BiomeBossesRules.jumpAirTicks(vy);
        assertTrue(air > 10);
        double hv = BiomeBossesRules.jumpHorizontalVelocity(4.0, air);
        double traveled = 0.0;
        double vx = hv;
        for (int i = 0; i < air; i++) {
            traveled += vx;
            vx *= BiomeBossesRules.BOSS_JUMP_HORIZONTAL_DRAG;
        }
        assertEquals(4.0, traveled, 0.02);
        assertEquals(4.0, BiomeBossesRules.jumpLeapDistance(20.0), 0.0);
        assertEquals(2.0, BiomeBossesRules.jumpLeapDistance(3.0), 0.0);
        assertEquals(0.0, BiomeBossesRules.jumpLeapDistance(1.0), 0.0);
        assertEquals(0.0, BiomeBossesRules.jumpLeapDistance(0.5), 0.0);
        assertFalse(BiomeBossesRules.canLeap(true, true, true));
        assertTrue(BiomeBossesRules.canLeap(false, true, false));
        assertTrue(BiomeBossesRules.canLeap(false, false, true));
        assertFalse(BiomeBossesRules.canLeap(false, false, false));
        assertEquals(0.0, BiomeBossesRules.jumpVelocity(0.0), 0.0);
    }

    @Test
    void bossRayPullsAtThreeBlocksPerSecondBelowHalfHealth() {
        assertEquals(100, BiomeBossesRules.BOSS_RAY_INTERVAL_TICKS);
        assertEquals(100, BiomeBossesRules.BOSS_RAY_PULL_TICKS);
        assertEquals(10, BiomeBossesRules.BOSS_RAY_MISS_TICKS);
        assertEquals(24.0, BiomeBossesRules.BOSS_RAY_RANGE, 0.0);
        assertEquals(1.75, BiomeBossesRules.BOSS_RAY_STOP_DISTANCE, 0.0);
        assertFalse(BiomeBossesRules.belowHalfHealth(50.0F, 100.0F));
        assertTrue(BiomeBossesRules.belowHalfHealth(49.9F, 100.0F));
        assertFalse(BiomeBossesRules.belowHalfHealth(1.0F, 0.0F));
        assertEquals(0.15, BiomeBossesRules.pullBlocksPerTick(), 0.0000001);
        double[] step = BiomeBossesRules.pullStep(10.0, 0.0, 0.0);
        assertEquals(0.15, step[0], 0.00001);
        assertEquals(0.0, step[1], 0.00001);
        assertEquals(0.0, step[2], 0.00001);
        double[] close = BiomeBossesRules.pullStep(1.75, 0.0, 0.0);
        assertEquals(0.0, close[0], 0.0);
        assertEquals(0.0, close[1], 0.0);
        double[] diag = BiomeBossesRules.pullStep(3.0, 4.0, 0.0);
        assertEquals(0.09, diag[0], 0.00001);
        assertEquals(0.12, diag[1], 0.00001);
        assertEquals(0.0, diag[2], 0.00001);
    }

    @Test
    void stuckBossSearchesSixteenChebyshevBlocks() {
        assertEquals(16, BiomeBossesRules.UNSTUCK_RADIUS);
        assertEquals(20, BiomeBossesRules.UNSTUCK_RETRY_TICKS);
        assertEquals(0, BiomeBossesRules.chebyshev(0, 0, 0));
        assertEquals(1, BiomeBossesRules.chebyshev(1, -1, 0));
        assertEquals(16, BiomeBossesRules.chebyshev(16, -16, 16));
        assertEquals(17, BiomeBossesRules.chebyshev(-17, 3, 2));
        assertTrue(BiomeBossesRules.withinUnstuck(16, 0, -16));
        assertTrue(BiomeBossesRules.withinUnstuck(0, 0, 0));
        assertFalse(BiomeBossesRules.withinUnstuck(17, 0, 0));
    }

    @Test
    void finaleRosterIsOneOfEachAndScalesWithTheBossNumber() {
        var roster = BiomeBossesRules.finaleRoster();
        assertEquals(8, roster.size());
        assertEquals(1, count(roster, BiomeBossesRules.FinaleKind.PIGLIN));
        assertEquals(1, count(roster, BiomeBossesRules.FinaleKind.ENDERMAN));
        assertEquals(1, count(roster, BiomeBossesRules.FinaleKind.VEX));
        assertEquals(1, count(roster, BiomeBossesRules.FinaleKind.WITCH));
        assertEquals(1, count(roster, BiomeBossesRules.FinaleKind.SKELETON));
        assertEquals(1, count(roster, BiomeBossesRules.FinaleKind.CREEPER));
        assertEquals(1, count(roster, BiomeBossesRules.FinaleKind.ZOMBIE));
        assertEquals(1, count(roster, BiomeBossesRules.FinaleKind.PHANTOM));
        assertEquals(BiomeBossesRules.FinaleKind.PIGLIN, roster.get(0));
        assertEquals(BiomeBossesRules.FinaleKind.ENDERMAN, roster.get(1));
        assertEquals(BiomeBossesRules.FinaleKind.PHANTOM, roster.get(roster.size() - 1));
        assertEquals(roster, BiomeBossesRules.finaleRoster(1));
        assertEquals(16, BiomeBossesRules.finaleRoster(2).size());
        assertEquals(24, BiomeBossesRules.finaleRoster(3).size());
        assertEquals(32, BiomeBossesRules.finaleRoster(4).size());
        assertEquals(32, BiomeBossesRules.finaleRoster(7).size());
        assertEquals(4, count(BiomeBossesRules.finaleRoster(4), BiomeBossesRules.FinaleKind.ENDERMAN));
        assertEquals(1, BiomeBossesRules.finaleCountMultiplier(1));
        assertEquals(2, BiomeBossesRules.finaleCountMultiplier(2));
        assertEquals(4, BiomeBossesRules.finaleCountMultiplier(4));
        assertEquals(4, BiomeBossesRules.finaleCountMultiplier(7));
        assertEquals(1, BiomeBossesRules.finaleCountMultiplier(0));
        assertEquals(1, BiomeBossesRules.finaleHealthMultiplier(1));
        assertEquals(2, BiomeBossesRules.finaleHealthMultiplier(2));
        assertEquals(4, BiomeBossesRules.finaleHealthMultiplier(4));
        assertEquals(5, BiomeBossesRules.finaleHealthMultiplier(5));
        assertEquals(7, BiomeBossesRules.finaleHealthMultiplier(7));
        assertEquals(0.0, BiomeBossesRules.extraMultiplierAmount(BiomeBossesRules.finaleHealthMultiplier(1)), 0.0);
        assertEquals(1.0, BiomeBossesRules.extraMultiplierAmount(BiomeBossesRules.finaleHealthMultiplier(2)), 0.0);
        assertEquals(3.0, BiomeBossesRules.extraMultiplierAmount(BiomeBossesRules.finaleHealthMultiplier(4)), 0.0);
        assertTrue(BiomeBossesRules.finaleIgnoresSun(BiomeBossesRules.FinaleKind.ZOMBIE));
        assertTrue(BiomeBossesRules.finaleIgnoresSun(BiomeBossesRules.FinaleKind.SKELETON));
        assertTrue(BiomeBossesRules.finaleIgnoresSun(BiomeBossesRules.FinaleKind.PHANTOM));
        assertFalse(BiomeBossesRules.finaleIgnoresSun(BiomeBossesRules.FinaleKind.ENDERMAN));
        assertFalse(BiomeBossesRules.finaleIgnoresSun(BiomeBossesRules.FinaleKind.PIGLIN));
        assertFalse(BiomeBossesRules.finaleWantsFloor(BiomeBossesRules.FinaleKind.VEX));
        assertFalse(BiomeBossesRules.finaleWantsFloor(BiomeBossesRules.FinaleKind.PHANTOM));
        assertTrue(BiomeBossesRules.finaleWantsFloor(BiomeBossesRules.FinaleKind.ENDERMAN));
        assertTrue(BiomeBossesRules.finaleWantsFloor(BiomeBossesRules.FinaleKind.ZOMBIE));
        assertTrue(BiomeBossesRules.finaleWantsFloor(BiomeBossesRules.FinaleKind.CREEPER));
        assertEquals(16.0, BiomeBossesRules.FINALE_SPAWN_RADIUS, 0.0);
        assertEquals(4, BiomeBossesRules.FINALE_COUNT_CAP);
        assertEquals(8, BiomeBossesRules.FINALE_SEARCH_RADIUS);
        assertEquals(64.0, BiomeBossesRules.FINALE_AGGRO_BLOCKS, 0.0);
        assertEquals(-1, BiomeBossesRules.finaleTargetSlot(0, 0));
        assertEquals(0, BiomeBossesRules.finaleTargetSlot(0, 1));
        assertEquals(0, BiomeBossesRules.finaleTargetSlot(7, 1));
        assertEquals(0, BiomeBossesRules.finaleTargetSlot(0, 3));
        assertEquals(1, BiomeBossesRules.finaleTargetSlot(1, 3));
        assertEquals(2, BiomeBossesRules.finaleTargetSlot(2, 3));
        assertEquals(0, BiomeBossesRules.finaleTargetSlot(3, 3));
        assertEquals(1, BiomeBossesRules.finaleTargetSlot(7, 3));
        int[] dealt = new int[3];
        for (int minion = 0; minion < 8; minion++) {
            dealt[BiomeBossesRules.finaleTargetSlot(minion, 3)]++;
        }
        assertEquals(3, dealt[0]);
        assertEquals(3, dealt[1]);
        assertEquals(2, dealt[2]);
    }

    @Test
    void finaleOffsetStaysInsideSixteenBlocks() {
        for (int count : new int[] {8, 16, 24, 32}) {
            for (int index = 0; index < count; index++) {
                BiomeBossesRules.PlaneOffset spot = BiomeBossesRules.finaleOffset(index, count);
                int band = Math.floorMod(index, 4);
                double expected = BiomeBossesRules.FINALE_SPAWN_RADIUS * (band + 1) / 4.0;
                assertEquals(expected, Math.hypot(spot.x(), spot.z()), 0.0001);
                assertTrue(BiomeBossesRules.withinFinaleRadius(spot.x(), spot.z()));
            }
        }
        BiomeBossesRules.PlaneOffset first = BiomeBossesRules.finaleOffset(0, 8);
        BiomeBossesRules.PlaneOffset next = BiomeBossesRules.finaleOffset(1, 8);
        assertTrue(Math.hypot(first.x() - next.x(), first.z() - next.z()) > 1.0);
        assertEquals(first.x(), BiomeBossesRules.finaleOffset(8, 8).x(), 0.0001);
        assertEquals(first.z(), BiomeBossesRules.finaleOffset(8, 8).z(), 0.0001);
        assertTrue(BiomeBossesRules.withinFinaleRadius(16.0, 0.0));
        assertTrue(BiomeBossesRules.withinFinaleRadius(0.0, -16.0));
        assertTrue(BiomeBossesRules.withinFinaleRadius(0.0, 0.0));
        assertFalse(BiomeBossesRules.withinFinaleRadius(16.1, 0.0));
        assertFalse(BiomeBossesRules.withinFinaleRadius(12.0, 12.0));
    }

    private static long count(List<BiomeBossesRules.FinaleKind> roster, BiomeBossesRules.FinaleKind kind) {
        return roster.stream().filter(entry -> entry == kind).count();
    }
}
