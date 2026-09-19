package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InhabitantRulesTest {
    @Test
    void volumeAndEnclosure() {
        assertFalse(InhabitantRules.volumeOk(23));
        assertTrue(InhabitantRules.volumeOk(24));
        assertTrue(InhabitantRules.volumeOk(80));
        assertTrue(InhabitantRules.volumeOk(250));
        assertFalse(InhabitantRules.volumeOk(251));
        assertTrue(InhabitantRules.fillOpen(512, true));
        assertFalse(InhabitantRules.fillOpen(512, false));
        assertFalse(InhabitantRules.fillOpen(250, true));
        assertTrue(InhabitantRules.enclosed(40, false));
        assertFalse(InhabitantRules.enclosed(512, true));
        assertFalse(InhabitantRules.enclosed(0, false));
        assertFalse(InhabitantRules.interiorCell(true, false, true, true));
        assertTrue(InhabitantRules.interiorCell(false, false, true, false));
        assertTrue(InhabitantRules.interiorCell(false, true, false, false));
        assertTrue(InhabitantRules.interiorCell(false, false, false, true));
        assertFalse(InhabitantRules.interiorCell(false, false, false, false));
    }

    @Test
    void amenityScoreCaps() {
        InhabitantRules.AmenityCounts empty =
                new InhabitantRules.AmenityCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false);
        assertEquals(0, InhabitantRules.score(empty));
        InhabitantRules.AmenityCounts windows =
                new InhabitantRules.AmenityCounts(10, 0, 0, 0, 0, 0, 0, 0, 0, 0, false);
        assertEquals(6, InhabitantRules.score(windows));
        InhabitantRules.AmenityCounts rugs =
                new InhabitantRules.AmenityCounts(0, 0, 40, 0, 0, 0, 0, 0, 0, 0, false);
        assertEquals(3, InhabitantRules.score(rugs));
        InhabitantRules.AmenityCounts furnished =
                new InhabitantRules.AmenityCounts(3, 2, 8, 2, 2, 1, 2, 1, 1, 1, true);
        assertEquals(6 + 2 + 2 + 2 + 2 + 1 + 2 + 1 + 1 + 1 + 2, InhabitantRules.score(furnished));
        assertEquals(12, InhabitantRules.MIN_SCORE);
    }

    @Test
    void gatesNeedScoreAndBasics() {
        InhabitantRules.AmenityCounts poor =
                new InhabitantRules.AmenityCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false);
        InhabitantRules.GateResult closed = InhabitantRules.gates(true, 40, true, true, true, true, true, poor);
        assertFalse(closed.eligible());
        assertTrue(closed.missing().stream().anyMatch(s -> s.contains("more furnishings")));
        assertTrue(InhabitantRules.inspectFallback(closed).contains("/ehm help homes"));
        InhabitantRules.AmenityCounts ok =
                new InhabitantRules.AmenityCounts(3, 2, 8, 1, 1, 1, 1, 1, 1, 0, false);
        assertTrue(InhabitantRules.score(ok) >= InhabitantRules.MIN_SCORE);
        InhabitantRules.GateResult pass = InhabitantRules.gates(true, 40, true, true, true, true, true, ok);
        assertTrue(pass.eligible());
        assertTrue(pass.missing().isEmpty());
        InhabitantRules.GateResult open = InhabitantRules.gates(false, 40, true, true, true, true, true, ok);
        assertFalse(open.eligible());
        assertTrue(open.missing().contains("not enclosed"));
        assertTrue(InhabitantRules.inspectFallback(pass).contains("Eligible"));
        assertTrue(InhabitantRules.inspectFallback(open).contains("Missing"));
    }

    @Test
    void spacingAndDawnChance() {
        assertFalse(InhabitantRules.farEnough(47, 0, 48));
        assertTrue(InhabitantRules.farEnough(48, 0, 48));
        assertTrue(InhabitantRules.farEnough(0, 48, 48));
        assertEquals(0, InhabitantRules.spawnChancePercent(11, false));
        assertEquals(8, InhabitantRules.spawnChancePercent(12, false));
        assertEquals(20, InhabitantRules.spawnChancePercent(18, false));
        assertEquals(40, InhabitantRules.spawnChancePercent(40, false));
        assertEquals(10, InhabitantRules.spawnChancePercent(18, true));
        assertTrue(InhabitantRules.spawnRoll(0, 8));
        assertFalse(InhabitantRules.spawnRoll(8, 8));
        assertFalse(InhabitantRules.spawnRoll(0, 0));
    }

    @Test
    void specialtyAndEconomy() {
        InhabitantRules.AmenityCounts kitchen =
                new InhabitantRules.AmenityCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, true);
        boolean sawCook = false;
        for (int roll = 0; roll < 20; roll++) {
            if (InhabitantRules.COOK.equals(InhabitantRules.pickSpecialty(kitchen, 12, roll))) {
                sawCook = true;
            }
        }
        assertTrue(sawCook);
        assertEquals("hauler", InhabitantRules.specialtyFallback(InhabitantRules.HAULER));
        assertEquals("farm neighbor", InhabitantRules.specialtyFallback(InhabitantRules.FARM));
        assertEquals(32, InhabitantRules.haulerStack(1));
        assertEquals(16, InhabitantRules.haulerStack(2));
        assertEquals(16, InhabitantRules.farmWheatBuy(false));
        assertEquals(8, InhabitantRules.farmWheatBuy(true));
        assertEquals("Alder", InhabitantRules.pickName(0));
        assertEquals(InhabitantRules.pickName(0), InhabitantRules.pickName(20));
    }

    @Test
    void moduleDefaultsOff() {
        assertFalse(new Inhabitants().defaultEnabled());
        assertEquals("inhabitants", Inhabitants.ID.getPath());
    }

    @Test
    void leaveAndKillTimers() {
        assertEquals(13, InhabitantRules.leaveAfterDay(10));
        assertEquals(17, InhabitantRules.emptyUntilDay(10));
        assertFalse(InhabitantRules.shouldLeave(12, 13));
        assertTrue(InhabitantRules.shouldLeave(13, 13));
        assertTrue(InhabitantRules.spawnBlocked(16, 17));
        assertFalse(InhabitantRules.spawnBlocked(17, 17));
        assertFalse(InhabitantRules.spawnBlocked(10, -1));
    }

    @Test
    void dawnWaitsForASurvivalPlayer() {
        assertFalse(InhabitantRules.shouldAttemptDawn(5, 5, true));
        assertFalse(InhabitantRules.shouldAttemptDawn(4, 5, false));
        assertTrue(InhabitantRules.shouldAttemptDawn(4, 5, true));
        assertTrue(InhabitantRules.shouldAttemptDawn(-1, 0, true));
    }

    @Test
    void villagerNeedsTwoHighFloorSpace() {
        assertTrue(InhabitantRules.villagerFits(true, true, true));
        assertFalse(InhabitantRules.villagerFits(true, false, true));
        assertFalse(InhabitantRules.villagerFits(false, true, true));
        assertFalse(InhabitantRules.villagerFits(true, true, false));
        assertEquals(0, InhabitantRules.STAND_OFFSETS[0][1]);
        int[] last = InhabitantRules.STAND_OFFSETS[InhabitantRules.STAND_OFFSETS.length - 1];
        assertEquals(0, last[0]);
        assertEquals(1, last[1]);
        assertEquals(0, last[2]);
    }
}
