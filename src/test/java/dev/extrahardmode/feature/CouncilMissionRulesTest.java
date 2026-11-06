package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CouncilMissionRulesTest {
    @Test
    void firstHuntIsSixToEighteenCommonMobs() {
        assertEquals(6, CouncilMissionRules.INITIAL_MIN);
        assertEquals(18, CouncilMissionRules.INITIAL_MAX);
        assertEquals(6, CouncilMissionRules.initialCount(0));
        assertEquals(18, CouncilMissionRules.initialCount(12));
        assertEquals(6, CouncilMissionRules.initialCount(13));
        assertEquals(12, CouncilMissionRules.targetCount(0, 6));
        assertEquals(0, CouncilMissionRules.unlockedTier(0));
        assertEquals(0, CouncilMissionRules.unlockedTier(1));
        for (CouncilMissionRules.Mob mob : CouncilMissionRules.mobsAtTier(0)) {
            assertEquals(0, mob.tier());
        }
        assertTrue(CouncilMissionRules.mobsAtTier(0).stream()
                .anyMatch(mob -> "minecraft:zombie".equals(mob.id())));
        assertFalse(CouncilMissionRules.mobsAtTier(0).stream()
                .anyMatch(mob -> "minecraft:ravager".equals(mob.id())));
    }

    @Test
    void laterHuntsGrowTwentyPercentAndUnlockRarerMobs() {
        assertEquals(1.2, CouncilMissionRules.GROWTH, 0.0);
        assertEquals(7, CouncilMissionRules.nextCount(6));
        assertEquals(12, CouncilMissionRules.nextCount(10));
        assertEquals(22, CouncilMissionRules.nextCount(18));
        assertEquals(0, CouncilMissionRules.unlockedTier(2));
        assertEquals(0, CouncilMissionRules.unlockedTier(3));
        assertEquals(1, CouncilMissionRules.unlockedTier(4));
        assertEquals(1, CouncilMissionRules.unlockedTier(7));
        assertEquals(2, CouncilMissionRules.unlockedTier(8));
        assertEquals(2, CouncilMissionRules.unlockedTier(11));
        assertEquals(3, CouncilMissionRules.unlockedTier(12));
        assertEquals(3, CouncilMissionRules.unlockedTier(99));
        assertTrue(CouncilMissionRules.mobsAtTier(2).stream()
                .anyMatch(mob -> "minecraft:witch".equals(mob.id())));
        assertTrue(CouncilMissionRules.mobsAtTier(3).stream()
                .anyMatch(mob -> "minecraft:ravager".equals(mob.id())));
        CouncilMissionRules.Mob first = CouncilMissionRules.pickMob(0, 0);
        assertEquals(0, first.tier());
        CouncilMissionRules.Mob rare = CouncilMissionRules.pickMob(12, 0);
        assertEquals(3, rare.tier());
        assertEquals("minecraft:ravager", rare.id());
    }

    @Test
    void harderMobsAreFewerAndEachDimensionKeepsItsOwnLadder() {
        assertEquals(4, CouncilMissionRules.TIER_EVERY_COMPLETED);
        assertEquals(20, CouncilMissionRules.REFERENCE_HP);
        assertEquals(0, CouncilMissionRules.healthScaledCount(0, 100));
        assertEquals(12, CouncilMissionRules.healthScaledCount(12, 20));
        assertEquals(12, CouncilMissionRules.healthScaledCount(12, 16));
        assertEquals(6, CouncilMissionRules.healthScaledCount(12, 40));
        assertEquals(2, CouncilMissionRules.healthScaledCount(12, 100));
        assertEquals(1, CouncilMissionRules.healthScaledCount(1, 1000));
        for (CouncilMissionRules.Mob mob : CouncilMissionRules.mobsAtTier(0)) {
            assertTrue(mob.hp() <= CouncilMissionRules.REFERENCE_HP, mob.id());
        }
        assertTrue(CouncilMissionRules.MOBS.stream().anyMatch(mob -> "minecraft:drowned".equals(mob.id())));
        assertTrue(CouncilMissionRules.MOBS.stream().anyMatch(mob -> "minecraft:bogged".equals(mob.id())));
        assertTrue(CouncilMissionRules.MOBS.stream().anyMatch(mob -> "minecraft:breeze".equals(mob.id())));
        assertTrue(CouncilMissionRules.MOBS.stream().anyMatch(mob -> "minecraft:piglin".equals(mob.id())));
        assertTrue(CouncilMissionRules.MOBS.stream().anyMatch(mob -> "minecraft:zombified_piglin".equals(mob.id())));
        assertTrue(CouncilMissionRules.MOBS.stream().anyMatch(mob -> "minecraft:endermite".equals(mob.id())));
        assertFalse(CouncilMissionRules.MOBS.stream().anyMatch(mob -> mob.id().contains("guardian")));
        assertEquals("minecraft:drowned", CouncilMissionRules.pickMob(0, 5).id());
        assertEquals("minecraft:zombified_piglin", CouncilMissionRules.pickMob(0, 0, CouncilMissionRules.NETHER).id());
        assertEquals("minecraft:magma_cube", CouncilMissionRules.pickMob(0, 1, CouncilMissionRules.NETHER).id());
        assertEquals("minecraft:endermite", CouncilMissionRules.pickMob(0, 0, CouncilMissionRules.END).id());
        assertEquals("minecraft:endermite", CouncilMissionRules.pickMob(4, 0, CouncilMissionRules.END).id());
        assertEquals("minecraft:enderman", CouncilMissionRules.pickMob(8, 0, CouncilMissionRules.END).id());
        assertEquals("minecraft:shulker", CouncilMissionRules.pickMob(8, 1, CouncilMissionRules.END).id());
        assertEquals("minecraft:enderman", CouncilMissionRules.pickMob(12, 0, CouncilMissionRules.END).id());
        assertEquals("minecraft:piglin_brute", CouncilMissionRules.pickMob(12, 0, CouncilMissionRules.NETHER).id());
        assertEquals("minecraft:zombie", CouncilMissionRules.pickMob(12, 0, "minecraft:nowhere").id());

        CouncilMissionRules.Mission ladder = new CouncilMissionRules.Mission(
                "", "", 0, 0, 0, -1L, 0, false, 0, 0, 6, 12, "a", "Ada", -1L, 0);
        CouncilMissionRules.Mission hunt = CouncilMissionRules.assign(
                ladder, "a", "Ada", 12, 20, 0, 0, CouncilMissionRules.OVERWORLD);
        assertEquals("minecraft:ravager", hunt.mobId());
        assertEquals(7, CouncilMissionRules.nextCount(6));
        assertEquals(1, hunt.target());
        assertEquals(CouncilMissionRules.healthScaledCount(7, 100), hunt.target());
        assertEquals(6, hunt.lastCompletedTarget());
        CouncilMissionRules.Mission done = hunt.withKills(hunt.target()).asCompleted(10, 1);
        assertEquals(7, done.lastCompletedTarget());
    }

    @Test
    void xpIsQuarterHealthPlusTwentyFiveThenHouseBonus() {
        assertEquals(25, CouncilMissionRules.XP_FLAT);
        assertEquals(85, CouncilMissionRules.baseXp(20, 12));
        assertEquals(73, CouncilMissionRules.baseXp(16, 12));
        assertEquals(85, CouncilMissionRules.scaledXp(85, 0));
        assertEquals(94, CouncilMissionRules.scaledXp(85, 1));
        assertEquals(128, CouncilMissionRules.scaledXp(85, 5));
        assertEquals(85, CouncilMissionRules.completionXp(20, 12, InhabitantRules.MIN_SCORE));
        assertEquals(128, CouncilMissionRules.completionXp(20, 12, InhabitantRules.MIN_SCORE + 5));
        assertEquals(8, CouncilMissionRules.emeraldReward(85));
        assertEquals(12, CouncilMissionRules.emeraldReward(128));
        assertEquals(0, CouncilMissionRules.emeraldReward(9));
        assertEquals(5, CouncilMissionRules.extraHousePoints(17));
        assertEquals(0, CouncilMissionRules.extraHousePoints(12));
    }

    @Test
    void huntsExpireAfterFourteenDaysAndTheNextWaitIsSeven() {
        assertEquals(14, CouncilMissionRules.MISSION_DAYS);
        assertEquals(7, CouncilMissionRules.COOLDOWN_DAYS);
        assertEquals(3, CouncilMissionRules.COOLDOWN_STEP_DAYS);
        assertEquals(5, CouncilMissionRules.MAX_OPEN_MISSIONS);
        assertEquals(7, CouncilMissionRules.cooldownDays(0));
        assertEquals(7, CouncilMissionRules.cooldownDays(-2));
        assertEquals(10, CouncilMissionRules.cooldownDays(1));
        assertEquals(13, CouncilMissionRules.cooldownDays(2));
        assertEquals(16, CouncilMissionRules.cooldownDays(3));
        assertFalse(CouncilMissionRules.expired(10, 23));
        assertTrue(CouncilMissionRules.expired(10, 24));
        assertFalse(CouncilMissionRules.expired(-1, 99));
        assertEquals(7, CouncilMissionRules.daysUntilNextMission(10, 10, 0));
        assertEquals(1, CouncilMissionRules.daysUntilNextMission(10, 16, 0));
        assertEquals(0, CouncilMissionRules.daysUntilNextMission(10, 17, 0));
        assertEquals(10, CouncilMissionRules.daysUntilNextMission(10, 10, 1));
        assertEquals(1, CouncilMissionRules.daysUntilNextMission(10, 19, 1));
        assertEquals(0, CouncilMissionRules.daysUntilNextMission(10, 20, 1));
        assertEquals(13, CouncilMissionRules.daysUntilNextMission(10, 10, 2));
        CouncilMissionRules.Mission active =
                CouncilMissionRules.assign(CouncilMissionRules.NONE, "home-a", "Ada", 12, 10, 0, 0);
        assertTrue(active.active());
        assertEquals(6, active.target());
        assertEquals("minecraft:zombie", active.mobId());
        assertEquals("home-a", active.giverId());
        assertFalse(CouncilMissionRules.expired(active, 23));
        assertTrue(CouncilMissionRules.expired(active, 24));
        assertFalse(CouncilMissionRules.canTakeNext(active, 17));
        assertFalse(CouncilMissionRules.canTakeNext(active, 24));
        CouncilMissionRules.Mission cleared = active.clearHunt();
        assertEquals(10L, cleared.assignedDay());
        assertEquals("home-a", cleared.giverId());
        assertFalse(CouncilMissionRules.canTakeNext(cleared, 16));
        assertTrue(CouncilMissionRules.canTakeNext(cleared, 17));
        CouncilMissionRules.Mission finished =
                active.withKills(active.target()).asCompleted(85, 8).clearHunt();
        assertEquals(1, finished.completedCount());
        assertFalse(CouncilMissionRules.canTakeNext(finished, 19));
        assertTrue(CouncilMissionRules.canTakeNext(finished, 20));
        CouncilMissionRules.Mission twice = CouncilMissionRules.assign(finished, "home-a", "Ada", 12, 20, 0, 0)
                .withKills(7)
                .asCompleted(85, 8)
                .clearHunt();
        assertEquals(2, twice.completedCount());
        assertFalse(CouncilMissionRules.canTakeNext(twice, 32));
        assertTrue(CouncilMissionRules.canTakeNext(twice, 33));
        assertTrue(CouncilMissionRules.canTakeNext(null, 10));
        assertTrue(CouncilMissionRules.canTakeNext(CouncilMissionRules.NONE, 10));
        CouncilMissionRules.Mission unpaid = active.withKills(6).asCompleted(85, 8);
        assertTrue(unpaid.awaitingTurnIn());
        assertFalse(CouncilMissionRules.canTakeNext(unpaid, 30));
        assertEquals("Zombie Bounty 3/12", CouncilMissionRules.hudLabel("Zombie", 3, 12));
        assertEquals("Zombie Bounty 0/6", CouncilMissionRules.hudLabel(active));
        CouncilMissionRules.Mission grown =
                CouncilMissionRules.assign(active.asCompleted(85, 8), "home-a", "Ada", 12, 20, 0, 0);
        assertEquals(7, grown.target());
        assertEquals(1, grown.completedCount());
        assertEquals(6, grown.lastCompletedTarget());
        assertTrue(grown.active());
        assertFalse(grown.awaitingTurnIn());
        assertEquals("", CouncilMissionRules.hudLabel(grown.asCompleted(10, 1)));
        assertEquals(6, CouncilMissionRules.councilBagCopies(2));
        assertEquals(9, CouncilMissionRules.councilBagCopies(3));
        assertEquals(3, CouncilMissionRules.councilBagCopies(0));
        assertEquals(AbilityDurationRules.originY(240) - 9 - 2, CouncilMissionRules.bountyHudY(240, 9));
        assertEquals(CouncilMissionRules.bountyHudY(240, 9), CouncilMissionRules.bountyHudLineY(240, 9, 0, 2));
        assertEquals(
                CouncilMissionRules.bountyHudY(240, 9) - 11, CouncilMissionRules.bountyHudLineY(240, 9, 1, 2));
    }

    @Test
    void eachOpenBountyUsuallyBringsOneOrTwoGroups() {
        assertEquals(13000, CouncilMissionRules.NIGHT_START);
        assertEquals(23000, CouncilMissionRules.NIGHT_END);
        assertEquals(20, CouncilMissionRules.NIGHT_SPAWN_TICKS);
        assertEquals(250, CouncilMissionRules.NIGHT_SPAWN_ONE_IN);
        assertEquals(8, CouncilMissionRules.BOUNTY_SPAWN_MIN_BLOCKS);
        assertEquals(48, CouncilMissionRules.BOUNTY_SPAWN_NEAR_BLOCKS);
        assertTrue(CouncilMissionRules.night(13000));
        assertTrue(CouncilMissionRules.night(22999));
        assertFalse(CouncilMissionRules.night(23000));
        assertFalse(CouncilMissionRules.night(12999));
        assertFalse(CouncilMissionRules.night(0));
        assertTrue(CouncilMissionRules.night(13000L + 24000L));
        assertFalse(CouncilMissionRules.night(-1L));
        assertEquals(500, CouncilMissionRules.nightAttempts());
        assertEquals(2.0, CouncilMissionRules.meanNightSpawns(), 1.0e-9);
        assertTrue(CouncilMissionRules.nightSpawn(0));
        assertTrue(CouncilMissionRules.nightSpawn(250));
        assertTrue(CouncilMissionRules.nightSpawn(-250));
        assertFalse(CouncilMissionRules.nightSpawn(1));
        assertFalse(CouncilMissionRules.nightSpawn(249));
        assertEquals(0, CouncilMissionRules.bountyGroupMin(0));
        assertEquals(0, CouncilMissionRules.bountyGroupMax(-3));
        assertEquals(0, CouncilMissionRules.bountyGroupSize(0, 4));
        assertEquals(1, CouncilMissionRules.bountyGroupMin(1));
        assertEquals(1, CouncilMissionRules.bountyGroupMax(1));
        assertEquals(1, CouncilMissionRules.bountyGroupMin(5));
        assertEquals(1, CouncilMissionRules.bountyGroupMax(5));
        assertEquals(1, CouncilMissionRules.bountyGroupMin(6));
        assertEquals(2, CouncilMissionRules.bountyGroupMax(6));
        assertEquals(2, CouncilMissionRules.bountyGroupMin(12));
        assertEquals(4, CouncilMissionRules.bountyGroupMax(12));
        assertEquals(4, CouncilMissionRules.bountyGroupMin(24));
        assertEquals(8, CouncilMissionRules.bountyGroupMax(24));
        assertEquals(2, CouncilMissionRules.bountyGroupSize(12, 0));
        assertEquals(3, CouncilMissionRules.bountyGroupSize(12, 1));
        assertEquals(4, CouncilMissionRules.bountyGroupSize(12, 2));
        assertEquals(2, CouncilMissionRules.bountyGroupSize(12, 3));
        assertEquals(1, CouncilMissionRules.bountyGroupSize(1, 99));
        assertEquals(5, CouncilMissionRules.bountyGroupSize(24, 1));
        CouncilMissionRules.Mission owed =
                CouncilMissionRules.assign(CouncilMissionRules.NONE, "a", "Ada", 12, 0, 0, 0);
        assertEquals(owed.target(), owed.remaining());
        assertEquals(Math.max(0, owed.target() - 3), owed.withKills(3).remaining());
        assertEquals(0, owed.withKills(owed.target() + 4).remaining());

        int trials = CouncilMissionRules.nightAttempts();
        double chance = 1.0 / CouncilMissionRules.NIGHT_SPAWN_ONE_IN;
        double[] probability = binomial(trials, chance);
        double fourOrMore = 0.0;
        for (int count = 4; count < probability.length; count++) {
            fourOrMore += probability[count];
        }
        double oneOrTwo = probability[1] + probability[2];
        double threeOrMore = 0.0;
        for (int count = 3; count < probability.length; count++) {
            threeOrMore += probability[count];
        }
        assertTrue(oneOrTwo > probability[0]);
        assertTrue(oneOrTwo > threeOrMore);
        assertTrue(fourOrMore < oneOrTwo);
        int mode = 0;
        for (int count = 1; count < probability.length; count++) {
            if (probability[count] > probability[mode]) {
                mode = count;
            }
        }
        assertEquals(2, mode);

        CouncilMissionRules.Mission fresh =
                CouncilMissionRules.assign(CouncilMissionRules.NONE, "a", "Ada", 12, 0, 0, 0);
        assertEquals(-1L, fresh.spawnCountDay());
        assertEquals(0, fresh.spawnReplacements());
        CouncilMissionRules.Mission third = fresh.withReplacement(4).withReplacement(4).withReplacement(4);
        assertEquals(4L, third.spawnCountDay());
        assertEquals(3, third.spawnReplacements());
        assertEquals(1, third.withReplacement(5).spawnReplacements());
        assertEquals(3, third.withKills(2).spawnReplacements());
        assertEquals(4L, third.withKills(2).spawnCountDay());
        assertEquals(0, third.clearHunt().spawnReplacements());
        assertEquals(-1L, third.clearHunt().spawnCountDay());
    }

    /** P(k) for k = 0..n. Log space keeps the 500-trial night from underflowing. */
    private static double[] binomial(int trials, double chance) {
        double[] out = new double[trials + 1];
        double logMiss = Math.log(1.0 - chance);
        double logHit = Math.log(chance);
        double logChoose = 0.0;
        out[0] = Math.exp(trials * logMiss);
        for (int count = 1; count <= trials; count++) {
            logChoose += Math.log(trials - count + 1) - Math.log(count);
            out[count] = Math.exp(logChoose + count * logHit + (trials - count) * logMiss);
        }
        return out;
    }

    @Test
    void eachResidentHoldsOneBountyAndFiveIsTheCap() {
        CouncilMissionRules.Mission first =
                CouncilMissionRules.assign(CouncilMissionRules.NONE, "a", "Ada", 12, 0, 0, 0);
        CouncilMissionRules.Mission second =
                CouncilMissionRules.assign(CouncilMissionRules.NONE, "b", "Bea", 12, 0, 0, 0);
        assertEquals(6, second.target());
        CouncilMissionRules.Mission firstDone = first.withKills(first.target()).asCompleted(1, 1);
        CouncilMissionRules.Mission secondDone = second.withKills(second.target()).asCompleted(1, 1);
        assertEquals(6, firstDone.lastCompletedTarget());
        assertEquals(1, firstDone.completedCount());
        assertEquals(6, secondDone.lastCompletedTarget());
        assertEquals(1, secondDone.completedCount());
        CouncilMissionRules.Mission adaNext =
                CouncilMissionRules.assign(firstDone, "a", "Ada", 12, 1, 0, 0);
        CouncilMissionRules.Mission beaNext =
                CouncilMissionRules.assign(secondDone, "b", "Bea", 12, 1, 0, 0);
        CouncilMissionRules.Mission cara =
                CouncilMissionRules.assign(CouncilMissionRules.NONE, "c", "Cara", 12, 1, 0, 0);
        assertEquals(7, adaNext.target());
        assertEquals(1, adaNext.completedCount());
        assertEquals(7, beaNext.target());
        assertEquals(6, cara.target());
        assertEquals(0, cara.completedCount());

        List<CouncilMissionRules.Mission> board = CouncilMissionRules.put(List.of(), first);
        board = CouncilMissionRules.put(board, second);
        assertEquals(2, CouncilMissionRules.openMissions(board));
        assertEquals(first, CouncilMissionRules.findGiver(board, "a"));
        assertNull(CouncilMissionRules.findGiver(board, "missing"));
        board = CouncilMissionRules.put(board, first.clearHunt());
        assertEquals(1, CouncilMissionRules.openMissions(board));
        assertFalse(CouncilMissionRules.findGiver(board, "a").hasMob());

        List<CouncilMissionRules.Mission> full = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            full = new ArrayList<>(CouncilMissionRules.put(
                    full, CouncilMissionRules.assign(CouncilMissionRules.NONE, "h" + i, "N" + i, 12, 0, i, i)));
        }
        assertTrue(CouncilMissionRules.atCap(full));
        assertEquals(5, CouncilMissionRules.openMissions(full));
        full = CouncilMissionRules.put(full, CouncilMissionRules.findGiver(full, "h0").clearHunt());
        assertFalse(CouncilMissionRules.atCap(full));

        List<CouncilMissionRules.Mission> legacy =
                CouncilMissionRules.claimLegacy(List.of(first.withGiver("", "")), "a", "Ada");
        assertEquals("a", CouncilMissionRules.findGiver(legacy, "a").giverId());
        assertEquals("Ada", CouncilMissionRules.findGiver(legacy, "a").giverName());
        assertNull(CouncilMissionRules.findGiver(legacy, ""));
        List<String> lines = CouncilMissionRules.hudLines(List.of(first, second));
        assertEquals(List.of("Ada: Zombie Bounty 0/6", "Bea: Zombie Bounty 0/6"), lines);
        assertEquals(List.of("Zombie Bounty 0/6"), CouncilMissionRules.hudLines(List.of(first)));
        assertFalse(CouncilMissionRules.worthMigrating(CouncilMissionRules.NONE));
        assertTrue(CouncilMissionRules.worthMigrating(first));
        assertTrue(CouncilMissionRules.worthMigrating(first.clearHunt()));
    }
}
