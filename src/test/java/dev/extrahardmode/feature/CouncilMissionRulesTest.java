package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void laterHuntsGrowThirtyPercentAndUnlockRarerMobs() {
        assertEquals(8, CouncilMissionRules.nextCount(6));
        assertEquals(13, CouncilMissionRules.nextCount(10));
        assertEquals(23, CouncilMissionRules.nextCount(18));
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
    void huntsExpireAfterSevenDaysAndHudFormats() {
        assertEquals(7, CouncilMissionRules.MISSION_DAYS);
        assertFalse(CouncilMissionRules.expired(10, 16));
        assertTrue(CouncilMissionRules.expired(10, 17));
        assertFalse(CouncilMissionRules.expired(-1, 99));
        CouncilMissionRules.Mission active = CouncilMissionRules.assign(
                CouncilMissionRules.NONE, 12, 10, 0, 0);
        assertTrue(active.active());
        assertEquals(6, active.target());
        assertEquals("minecraft:zombie", active.mobId());
        assertFalse(CouncilMissionRules.expired(active, 16));
        assertTrue(CouncilMissionRules.expired(active, 17));
        assertEquals("Zombie Bounty 3/12", CouncilMissionRules.hudLabel("Zombie", 3, 12));
        assertEquals("Zombie Bounty 0/6", CouncilMissionRules.hudLabel(active));
        CouncilMissionRules.Mission grown = CouncilMissionRules.assign(
                active.asCompleted(85, 8), 12, 20, 0, 0);
        assertEquals(8, grown.target());
        assertEquals(1, grown.completedCount());
        assertEquals(6, grown.lastCompletedTarget());
        assertTrue(grown.active());
        assertFalse(grown.awaitingTurnIn());
        assertEquals("", CouncilMissionRules.hudLabel(grown.asCompleted(10, 1)));
        assertEquals(6, CouncilMissionRules.councilBagCopies(2));
        assertEquals(9, CouncilMissionRules.councilBagCopies(3));
        assertEquals(3, CouncilMissionRules.councilBagCopies(0));
        assertEquals(
                AbilityDurationRules.originY(240) - 9 - 2,
                CouncilMissionRules.bountyHudY(240, 9));
    }
}
