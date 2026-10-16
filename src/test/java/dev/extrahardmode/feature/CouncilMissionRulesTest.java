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
        assertEquals(1, CouncilMissionRules.unlockedTier(2));
        assertEquals(2, CouncilMissionRules.unlockedTier(4));
        assertEquals(3, CouncilMissionRules.unlockedTier(6));
        assertEquals(3, CouncilMissionRules.unlockedTier(99));
        assertTrue(CouncilMissionRules.mobsAtTier(1).stream()
                .anyMatch(mob -> "minecraft:witch".equals(mob.id())));
        assertTrue(CouncilMissionRules.mobsAtTier(3).stream()
                .anyMatch(mob -> "minecraft:ravager".equals(mob.id())));
        CouncilMissionRules.Mob first = CouncilMissionRules.pickMob(0, 0);
        assertEquals(0, first.tier());
        CouncilMissionRules.Mob rare = CouncilMissionRules.pickMob(6, 0);
        assertEquals(3, rare.tier());
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
        assertEquals(5, CouncilMissionRules.MAX_OPEN_MISSIONS);
        assertFalse(CouncilMissionRules.expired(10, 23));
        assertTrue(CouncilMissionRules.expired(10, 24));
        assertFalse(CouncilMissionRules.expired(-1, 99));
        assertEquals(7, CouncilMissionRules.daysUntilNextMission(10, 10));
        assertEquals(1, CouncilMissionRules.daysUntilNextMission(10, 16));
        assertEquals(0, CouncilMissionRules.daysUntilNextMission(10, 17));
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
