package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class WiseLessonRulesTest {
    @Test
    void packGrowsFromTheHomeAndFromServices() {
        assertEquals(3, WiseLessonRules.baseSize(24));
        assertEquals(3, WiseLessonRules.baseSize(0));
        assertEquals(3, WiseLessonRules.baseSize(23.9));
        assertEquals(8, WiseLessonRules.baseSize(38));
        assertEquals(8, WiseLessonRules.baseSize(100));
        assertEquals(6, WiseLessonRules.baseSize(31));
        assertEquals(8, WiseLessonRules.baseSize(InhabitantRules.stockScore(24, 196)));
        assertEquals(3, WiseLessonRules.baseSize(InhabitantRules.stockScore(24, 0)));
        assertEquals(0, WiseLessonRules.huntGrowth(0));
        assertEquals(2, WiseLessonRules.huntGrowth(2));
        assertEquals(0, WiseLessonRules.huntGrowth(-1));
        assertEquals(3, WiseLessonRules.packSize(24, 0));
        assertEquals(5, WiseLessonRules.packSize(24, 1));
        assertEquals(6, WiseLessonRules.packSize(24, 2));
        assertEquals(8, WiseLessonRules.packSize(38, 0));
        assertEquals(12, WiseLessonRules.packSize(38, 1));
        assertEquals(6, WiseLessonRules.packSize(31, 0));
        assertEquals(200, WiseLessonRules.scaledDistance(200, 0));
        assertEquals(500, WiseLessonRules.scaledDistance(500, 0));
        assertEquals(266, WiseLessonRules.scaledDistance(200, 1));
        assertEquals(332, WiseLessonRules.scaledDistance(200, 2));
    }

    @Test
    void distanceDirectionAndSpawnGapsStayInRange() {
        assertEquals(200, WiseLessonRules.distance(0));
        assertEquals(500, WiseLessonRules.distance(300));
        assertEquals(200, WiseLessonRules.distance(301));
        assertEquals(500, WiseLessonRules.distance(-1));
        assertEquals("north", WiseLessonRules.direction(0));
        assertEquals("south", WiseLessonRules.direction(1));
        assertEquals("east", WiseLessonRules.direction(2));
        assertEquals("west", WiseLessonRules.direction(3));
        assertEquals("north", WiseLessonRules.direction(4));
        assertEquals("west", WiseLessonRules.direction(-1));
        assertEquals(100, WiseLessonRules.spawnInterval(0));
        assertEquals(300, WiseLessonRules.spawnInterval(200));
        assertEquals(100, WiseLessonRules.spawnInterval(201));
        assertEquals(20, WiseLessonRules.spawnRadius(0));
        assertEquals(40, WiseLessonRules.spawnRadius(20));
        assertEquals(20, WiseLessonRules.spawnRadius(21));
    }

    @Test
    void earlyHuntsAreTheEasyCreatures() {
        assertEquals(4, WiseLessonRules.TIER_EVERY);
        assertEquals(0, WiseLessonRules.unlockedTier(0));
        assertEquals(0, WiseLessonRules.unlockedTier(3));
        assertEquals(1, WiseLessonRules.unlockedTier(4));
        assertEquals(1, WiseLessonRules.unlockedTier(7));
        assertEquals(2, WiseLessonRules.unlockedTier(8));
        assertEquals(2, WiseLessonRules.unlockedTier(11));
        assertEquals(3, WiseLessonRules.unlockedTier(12));
        assertEquals(3, WiseLessonRules.unlockedTier(40));
        assertEquals("minecraft:zombie", WiseLessonRules.pickMob(0, 0).id());
        assertEquals("zombies", WiseLessonRules.pickMob(0, 0).plural());
        assertEquals("minecraft:drowned", WiseLessonRules.pickMob(0, 4).id());
        assertEquals("minecraft:husk", WiseLessonRules.pickMob(4, 0).id());
        assertEquals("cave spiders", WiseLessonRules.pickMob(4, 2).plural());
        assertEquals("minecraft:enderman", WiseLessonRules.pickMob(8, 0).id());
        assertEquals("vindicators", WiseLessonRules.pickMob(8, 1).plural());
        assertEquals("minecraft:ravager", WiseLessonRules.pickMob(12, 0).id());
        assertEquals("piglin brutes", WiseLessonRules.pickMob(12, 0, WiseLessonRules.NETHER).plural());
        assertEquals("minecraft:zombified_piglin", WiseLessonRules.pickMob(0, 0, WiseLessonRules.NETHER).id());
        assertEquals("minecraft:endermite", WiseLessonRules.pickMob(0, 0, WiseLessonRules.END).id());
        assertEquals("minecraft:endermite", WiseLessonRules.pickMob(4, 0, WiseLessonRules.END).id());
        assertEquals("minecraft:enderman", WiseLessonRules.pickMob(12, 0, WiseLessonRules.END).id());
        assertEquals("minecraft:zombie", WiseLessonRules.pickMob(0, 0, "minecraft:nowhere").id());
        assertEquals(8, WiseLessonRules.mobsAtTier(0).size());
        assertEquals(9, WiseLessonRules.mobsAtTier(1).size());
        assertEquals(3, WiseLessonRules.mobsAtTier(2).size());
        assertEquals(3, WiseLessonRules.mobsAtTier(3).size());
        for (WiseLessonRules.Mob mob : WiseLessonRules.MOBS) {
            assertFalse(mob.id().contains("phantom"));
            assertFalse(mob.id().contains("ghast"));
            assertFalse(mob.id().contains("guardian"));
            assertFalse(mob.id().contains("silverfish"));
            assertFalse(mob.id().contains("slime"));
            assertFalse(mob.id().contains("magma"));
            assertFalse(mob.id().contains("shulker"));
            assertFalse(mob.id().contains("hoglin"));
        }
    }

    @Test
    void statusLineNamesThePackAndFlipsWhenCloseEnough() {
        assertEquals(
                "Alden the Wise Teacher: 5 zombies east",
                WiseLessonRules.statusLine("Alden the Wise Teacher", 5, "zombies", "east", false));
        assertEquals(
                "Alden the Wise Teacher: 5 zombies near here",
                WiseLessonRules.statusLine("Alden the Wise Teacher", 5, "zombies", "east", true));
        assertTrue(WiseLessonRules.atRange("minecraft:overworld", "minecraft:overworld", 0, 0, 200, 0, 200));
        assertFalse(WiseLessonRules.atRange("minecraft:overworld", "minecraft:overworld", 0, 0, 199, 0, 200));
        assertFalse(WiseLessonRules.atRange("minecraft:overworld", "minecraft:the_nether", 0, 0, 500, 0, 200));
        WiseLessonRules.Lesson lesson = hunt();
        assertEquals(
                List.of("Alden the Wise Teacher: 5 zombies east"),
                WiseLessonRules.hudLines(List.of(lesson), "minecraft:overworld", 0, 0));
        assertEquals(
                List.of("Alden the Wise Teacher: 5 zombies near here"),
                WiseLessonRules.hudLines(List.of(lesson), "minecraft:overworld", 250, 0));
        WiseLessonRules.Lesson done = lesson.withActive(false);
        assertTrue(WiseLessonRules.hudLines(List.of(done), "minecraft:overworld", 250, 0).isEmpty());
    }

    @Test
    void offerWaitsThirtyDaysAndNightIsTheDarkWindow() {
        assertTrue(WiseLessonRules.offerDue(-1, 0));
        assertFalse(WiseLessonRules.offerDue(10, 39));
        assertTrue(WiseLessonRules.offerDue(10, 40));
        assertEquals(14, WiseLessonRules.MISSION_DAYS);
        assertFalse(WiseLessonRules.expired(-1, 99));
        assertFalse(WiseLessonRules.expired(10, 23));
        assertTrue(WiseLessonRules.expired(10, 24));
        assertFalse(WiseLessonRules.offerDue(10, 24));
        WiseLessonRules.Lesson open = hunt();
        assertSame(open, WiseLessonRules.endIfExpired(open, 23));
        WiseLessonRules.Lesson ended = WiseLessonRules.endIfExpired(open, 24);
        assertFalse(ended.active());
        assertEquals(10L, ended.assignedDay());
        assertFalse(WiseLessonRules.expired(ended, 24));
        assertSame(ended, WiseLessonRules.endIfExpired(ended, 40));
        assertFalse(WiseLessonRules.afterSecond(ended, true, true, 0).attemptSpawn());
        WiseLessonRules.Lesson other = new WiseLessonRules.Lesson(
                "other",
                "Bran the Wise Teacher",
                "minecraft:overworld",
                0,
                0,
                "minecraft:skeleton",
                "skeletons",
                4,
                300,
                "west",
                12,
                0,
                0,
                -1,
                true,
                List.of());
        assertEquals(
                List.of(
                        "Alden the Wise Teacher: 5 zombies east",
                        "Bran the Wise Teacher: 4 skeletons west"),
                WiseLessonRules.hudLines(List.of(open, other), "minecraft:overworld", 0, 0));
        assertEquals(
                List.of("Bran the Wise Teacher: 4 skeletons west"),
                WiseLessonRules.hudLines(List.of(ended, other), "minecraft:overworld", 0, 0));
        assertFalse(WiseLessonRules.nightTime(12999));
        assertTrue(WiseLessonRules.nightTime(13000));
        assertTrue(WiseLessonRules.nightTime(22999));
        assertFalse(WiseLessonRules.nightTime(23000));
        assertFalse(WiseLessonRules.nightTime(0));
        assertTrue(WiseLessonRules.nightTime(24000L + 13000L));
        assertTrue(WiseLessonRules.spawnGate(true, true, false));
        assertTrue(WiseLessonRules.spawnGate(true, false, true));
        assertFalse(WiseLessonRules.spawnGate(true, false, false));
        assertFalse(WiseLessonRules.spawnGate(false, true, true));
    }

    @Test
    void dwellThenAFreshGapBeforeTheFirstSpawn() {
        WiseLessonRules.Lesson lesson = hunt();
        assertSame(lesson, WiseLessonRules.afterSecond(lesson, false, true, 0).lesson());
        for (int i = 0; i < 119; i++) {
            WiseLessonRules.Second step = WiseLessonRules.afterSecond(lesson, true, false, 0);
            assertFalse(step.attemptSpawn());
            lesson = step.lesson();
        }
        assertEquals(2380, lesson.dwellTicks());
        WiseLessonRules.Second done = WiseLessonRules.afterSecond(lesson, true, false, 0);
        assertFalse(done.attemptSpawn());
        assertEquals(2400, done.lesson().dwellTicks());
        assertEquals(100, done.lesson().spawnWait());
        assertSame(done.lesson(), WiseLessonRules.afterSecond(done.lesson(), true, false, 99).lesson());
        lesson = done.lesson();
        for (int i = 0; i < 4; i++) {
            WiseLessonRules.Second step = WiseLessonRules.afterSecond(lesson, true, true, 50);
            assertFalse(step.attemptSpawn());
            lesson = step.lesson();
        }
        WiseLessonRules.Second go = WiseLessonRules.afterSecond(lesson, true, true, 50);
        assertTrue(go.attemptSpawn());
        assertEquals(0, go.lesson().spawnWait());
        WiseLessonRules.Lesson waited = WiseLessonRules.afterAttempt(go.lesson(), false, null, 0);
        assertEquals(0, waited.spawned());
        assertEquals(100, waited.spawnWait());
        WiseLessonRules.Lesson placed =
                WiseLessonRules.afterAttempt(go.lesson(), true, WiseLessonRules.mark("abc", 13), 0);
        assertEquals(1, placed.spawned());
        assertEquals(20, placed.spawnWait());
        assertEquals(List.of("abc|13"), placed.mobs());
        WiseLessonRules.Lesson left = WiseLessonRules.afterSecond(placed, false, true, 0).lesson();
        assertEquals(0, left.dwellTicks());
        assertEquals(-1, left.spawnWait());
    }

    @Test
    void xpIsTripledAndMarksExpireAfterThreeDays() {
        assertEquals(15, WiseLessonRules.scaledXp(5));
        assertEquals(50, WiseLessonRules.scaledXp(5, true));
        assertEquals(0, WiseLessonRules.scaledXp(0));
        assertEquals(0, WiseLessonRules.scaledXp(0, true));
        assertEquals(0, WiseLessonRules.scaledXp(-2));
        assertEquals(3, WiseLessonRules.xpFactor(false));
        assertEquals(10, WiseLessonRules.xpFactor(true));
        assertEquals(2, WiseLessonRules.lootRolls(false));
        assertEquals(3, WiseLessonRules.lootRolls(true));
        assertEquals(2, WiseLessonRules.lootRolls(false, false));
        assertEquals(3, WiseLessonRules.lootRolls(false, true));
        assertEquals(3, WiseLessonRules.lootRolls(true, false));
        assertTrue(WiseLessonRules.dropsEmeralds(true, false));
        assertTrue(WiseLessonRules.dropsEmeralds(false, true));
        assertFalse(WiseLessonRules.dropsEmeralds(false, false));
        assertTrue(WiseLessonRules.leadSpawn(0));
        assertFalse(WiseLessonRules.leadSpawn(1));
        assertFalse(WiseLessonRules.finalSpawn(0, 1));
        assertFalse(WiseLessonRules.finalSpawn(0, 8));
        assertTrue(WiseLessonRules.finalSpawn(7, 8));
        assertFalse(WiseLessonRules.finalSpawn(6, 8));
        assertTrue(WiseLessonRules.finalSpawn(1, 2));
        assertEquals(0.5, WiseLessonRules.markHealthBonus(0));
        assertEquals(0.75, WiseLessonRules.markHealthBonus(25));
        assertEquals(1.0, WiseLessonRules.markHealthBonus(50));
        assertEquals(0.5, WiseLessonRules.markHealthBonus(51));
        assertEquals(1.0, WiseLessonRules.markHealthBonus(-1));
        assertEquals(4.0, WiseLessonRules.leadHealthBonus());
        assertEquals(10, WiseLessonRules.FINAL_HEALTH_FACTOR);
        assertEquals(9.0, WiseLessonRules.finalHealthBonus());
        assertEquals(0.5, WiseLessonRules.finalDamageBonus());
        assertEquals(0.25, WiseLessonRules.leadScaleBonus());
        assertEquals(100, WiseLessonRules.wholeHealth(100.2));
        assertEquals(0, WiseLessonRules.wholeHealth(0));
        assertEquals(5, WiseLessonRules.leadEmeralds(100, 0));
        assertEquals(10, WiseLessonRules.leadEmeralds(100, 5));
        assertEquals(5, WiseLessonRules.leadEmeralds(100, 6));
        assertEquals(7, WiseLessonRules.leadEmeralds(100, 2));
        assertEquals(4, WiseLessonRules.leadEmeralds(80, 0));
        assertEquals(8, WiseLessonRules.leadEmeralds(80, 4));
        assertEquals(7, WiseLessonRules.leadEmeralds(130, 0));
        assertEquals(13, WiseLessonRules.leadEmeralds(130, 6));
        assertEquals(0, WiseLessonRules.leadEmeralds(0, 4));
        assertEquals(0, WiseLessonRules.leadEmeralds(-5, 1));
        assertEquals(10, WiseLessonRules.leadEmeralds(200, 0));
        assertEquals(20, WiseLessonRules.leadEmeralds(200, 10));
        assertEquals(10, WiseLessonRules.leadEmeralds(200, 11));
        assertEquals(0, WiseLessonRules.burstCount(0));
        assertEquals(1, WiseLessonRules.burstCount(3));
        assertEquals(2, WiseLessonRules.burstCount(5));
        assertEquals(4, WiseLessonRules.burstCount(8));
        assertEquals(12, WiseLessonRules.burstCount(24));
        assertEquals(2, WiseLessonRules.spawnsThisSecond(0, 8));
        assertEquals(1, WiseLessonRules.spawnsThisSecond(2, 8));
        assertEquals(1, WiseLessonRules.spawnsThisSecond(3, 8));
        assertEquals(1, WiseLessonRules.spawnsThisSecond(4, 8));
        assertEquals(0, WiseLessonRules.spawnsThisSecond(8, 8));
        assertEquals(4, WiseLessonRules.spawnsThisSecond(0, 24));
        assertEquals(4, WiseLessonRules.spawnsThisSecond(4, 24));
        assertEquals(4, WiseLessonRules.spawnsThisSecond(8, 24));
        assertEquals(1, WiseLessonRules.spawnsThisSecond(12, 24));
        assertEquals(20, WiseLessonRules.retryWait(1, 5, 0));
        assertEquals(100, WiseLessonRules.retryWait(2, 5, 0));
        assertEquals(100, WiseLessonRules.retryWait(0, 5, 0));
        assertEquals(0.8, WiseLessonRules.leadKnockbackResistance());
        assertTrue(WiseLessonRules.showMist(true, false, false));
        assertFalse(WiseLessonRules.showMist(false, false, false));
        assertTrue(WiseLessonRules.showMist(false, true, false));
        assertTrue(WiseLessonRules.showMist(false, false, true));
        assertFalse(WiseLessonRules.showOutline(false, false));
        assertTrue(WiseLessonRules.showOutline(true, false));
        assertTrue(WiseLessonRules.showOutline(false, true));
        assertFalse(WiseLessonRules.senseOutlineActive(-1L, 100L));
        assertFalse(WiseLessonRules.senseOutlineActive(100L, 100L));
        assertTrue(WiseLessonRules.senseOutlineActive(101L, 100L));
        WiseLessonRules.Lesson hunted = hunt().withSpawn(3, 40, List.of("aaa|13", "bbb|13"), true);
        assertEquals(4, WiseLessonRules.marksLeft(hunted));
        assertEquals(
                List.of("Alden the Wise Teacher: 4 zombies east"),
                WiseLessonRules.hudLines(List.of(hunted), "minecraft:overworld", 0, 0));
        WiseLessonRules.Lesson oneLess = hunted.withoutMob("aaa");
        assertEquals(3, WiseLessonRules.marksLeft(oneLess));
        assertTrue(oneLess.active());
        assertEquals(13, WiseLessonRules.expireDay(10));
        WiseLessonRules.Mark mark = WiseLessonRules.parseMark("abc|13");
        assertEquals("abc", mark.uuid());
        assertEquals(13L, mark.expireDay());
        assertNull(WiseLessonRules.parseMark("nope"));
        WiseLessonRules.Lesson lesson = hunt().withSpawn(5, 40, List.of("abc|13"), true);
        assertSame(lesson, lesson.withoutMob("missing"));
        WiseLessonRules.Lesson clear = lesson.withoutMob("abc");
        assertFalse(clear.active());
        assertTrue(clear.mobs().isEmpty());
    }

    @Test
    void anExplodedCreeperReturnsToTheQueueWithItsRole() {
        assertEquals("lead", WiseLessonRules.respawnRole(true, false));
        assertEquals("lead", WiseLessonRules.respawnRole(true, true));
        assertEquals("final", WiseLessonRules.respawnRole(false, true));
        assertEquals("mark", WiseLessonRules.respawnRole(false, false));
        assertEquals("lead", WiseLessonRules.enqueueRespawn(null, "lead"));
        assertEquals("lead", WiseLessonRules.enqueueRespawn("", "lead"));
        assertEquals("lead", WiseLessonRules.enqueueRespawn("lead", null));
        assertEquals("lead", WiseLessonRules.enqueueRespawn("lead", ""));
        assertEquals("lead,final", WiseLessonRules.enqueueRespawn("lead", "final"));
        assertEquals("lead,final,mark", WiseLessonRules.enqueueRespawn("lead,final", "mark"));
        assertEquals("lead", WiseLessonRules.peekRespawn("lead,final"));
        assertEquals("final", WiseLessonRules.peekRespawn(WiseLessonRules.pollRespawn("lead,final")));
        assertEquals("", WiseLessonRules.pollRespawn("lead"));
        assertEquals("", WiseLessonRules.pollRespawn(null));
        assertNull(WiseLessonRules.peekRespawn(null));
        assertNull(WiseLessonRules.peekRespawn(""));
        assertTrue(WiseLessonRules.leadForSpawn(5, "lead"));
        assertFalse(WiseLessonRules.finalForSpawn(5, 6, "lead"));
        assertFalse(WiseLessonRules.leadSpawn(5));
        assertTrue(WiseLessonRules.finalSpawn(5, 6));
        assertFalse(WiseLessonRules.leadForSpawn(0, "final"));
        assertTrue(WiseLessonRules.finalForSpawn(0, 6, "final"));
        assertFalse(WiseLessonRules.leadForSpawn(0, "mark"));
        assertFalse(WiseLessonRules.finalForSpawn(5, 6, "mark"));
        assertTrue(WiseLessonRules.leadForSpawn(0, null));
        assertTrue(WiseLessonRules.leadForSpawn(0, ""));
        assertFalse(WiseLessonRules.leadForSpawn(3, null));
        assertTrue(WiseLessonRules.finalForSpawn(5, 6, null));
        assertTrue(WiseLessonRules.finalForSpawn(5, 6, ""));
        assertFalse(WiseLessonRules.finalForSpawn(0, 1, null));
        assertFalse(WiseLessonRules.finalForSpawn(0, 1, "lead"));
        assertNull(WiseLessonRules.requeueExploded(null));

        List<String> pack = List.of("lead|3", "a|3", "b|3", "c|3", "d|3", "final|3");
        WiseLessonRules.Lesson full = creeperHunt(6, 6, 2400, 40, pack);
        assertEquals(6, WiseLessonRules.marksLeft(full));
        WiseLessonRules.Lesson leadBack = WiseLessonRules.requeueExploded(full.withoutMob("lead"));
        assertEquals(5, leadBack.spawned());
        assertEquals(2400, leadBack.dwellTicks());
        assertEquals(40, leadBack.spawnWait());
        assertTrue(leadBack.active());
        assertEquals(List.of("a|3", "b|3", "c|3", "d|3", "final|3"), leadBack.mobs());
        assertEquals(6, WiseLessonRules.marksLeft(leadBack));
        assertTrue(WiseLessonRules.leadForSpawn(leadBack.spawned(), "lead"));
        assertFalse(WiseLessonRules.finalForSpawn(leadBack.spawned(), leadBack.size(), "lead"));

        String queue = WiseLessonRules.enqueueRespawn(null, WiseLessonRules.respawnRole(true, false));
        WiseLessonRules.Lesson both = WiseLessonRules.requeueExploded(leadBack.withoutMob("final"));
        queue = WiseLessonRules.enqueueRespawn(queue, WiseLessonRules.respawnRole(false, true));
        assertEquals(4, both.spawned());
        assertTrue(both.active());
        assertEquals(40, both.spawnWait());
        assertEquals(6, WiseLessonRules.marksLeft(both));
        assertEquals("lead", WiseLessonRules.peekRespawn(queue));
        assertTrue(WiseLessonRules.leadForSpawn(both.spawned(), WiseLessonRules.peekRespawn(queue)));
        queue = WiseLessonRules.pollRespawn(queue);
        assertEquals("final", WiseLessonRules.peekRespawn(queue));
        assertFalse(WiseLessonRules.leadForSpawn(4, WiseLessonRules.peekRespawn(queue)));
        assertTrue(WiseLessonRules.finalForSpawn(4, both.size(), WiseLessonRules.peekRespawn(queue)));

        WiseLessonRules.Lesson bursting = creeperHunt(6, 3, 2400, 20, List.of("a|3", "b|3", "c|3"));
        WiseLessonRules.Lesson ordinary = WiseLessonRules.requeueExploded(bursting.withoutMob("b"));
        assertEquals(2, ordinary.spawned());
        assertEquals(6, WiseLessonRules.marksLeft(ordinary));
        assertEquals(6, WiseLessonRules.marksLeft(bursting));
        assertFalse(WiseLessonRules.leadForSpawn(ordinary.spawned(), "mark"));
        assertFalse(WiseLessonRules.finalForSpawn(ordinary.spawned(), ordinary.size(), "mark"));

        WiseLessonRules.Lesson lastStanding = creeperHunt(6, 6, 100, 80, List.of("only|3"));
        WiseLessonRules.Lesson cleared = lastStanding.withoutMob("only");
        assertFalse(cleared.active());
        WiseLessonRules.Lesson revived = WiseLessonRules.requeueExploded(cleared);
        assertTrue(revived.active());
        assertEquals(5, revived.spawned());
        assertEquals(80, revived.spawnWait());
        assertEquals(100, revived.dwellTicks());
        assertEquals(1, WiseLessonRules.marksLeft(lastStanding));
        assertEquals(1, WiseLessonRules.marksLeft(revived));

        WiseLessonRules.Lesson solo = creeperHunt(1, 1, 50, 15, List.of("c|3"));
        WiseLessonRules.Lesson soloGone = solo.withoutMob("c");
        assertFalse(soloGone.active());
        WiseLessonRules.Lesson soloBack = WiseLessonRules.requeueExploded(soloGone);
        assertTrue(soloBack.active());
        assertEquals(0, soloBack.spawned());
        assertEquals(15, soloBack.spawnWait());
        assertEquals(50, soloBack.dwellTicks());
        assertEquals(1, WiseLessonRules.marksLeft(soloBack));
        assertEquals(1, WiseLessonRules.marksLeft(solo));
        assertTrue(WiseLessonRules.leadForSpawn(soloBack.spawned(), "lead"));
        assertFalse(WiseLessonRules.finalForSpawn(soloBack.spawned(), soloBack.size(), "lead"));
        assertFalse(WiseLessonRules.finalSpawn(0, 1));
    }

    private static WiseLessonRules.Lesson creeperHunt(
            int size, int spawned, int dwell, int wait, List<String> mobs) {
        return new WiseLessonRules.Lesson(
                "teacher",
                "Alden the Wise Teacher",
                "minecraft:overworld",
                0,
                0,
                "minecraft:creeper",
                "creepers",
                size,
                200,
                "east",
                10,
                spawned,
                dwell,
                wait,
                true,
                mobs);
    }

    @Test
    void bossHintsStayWithTheTeacherUntilTheNextHunt() {
        assertEquals("", WiseLessonRules.joinNames(null));
        assertEquals("", WiseLessonRules.joinNames(List.of()));
        assertEquals("Plains", WiseLessonRules.joinNames(List.of("Plains")));
        assertEquals("Plains and Forest", WiseLessonRules.joinNames(List.of("Plains", "Forest")));
        assertEquals(
                "Plains, Forest, and Desert",
                WiseLessonRules.joinNames(List.of("Plains", "Forest", "Desert")));

        List<String> two = List.of("minecraft:plains", "minecraft:forest");
        WiseLessonRules.BossHint shortList = WiseLessonRules.nextBossHint(null, "teacher", 10L, two, 99);
        assertFalse(shortList.chosen());
        assertEquals(two, shortList.biomes());
        assertFalse(WiseLessonRules.choosesBossHint(null, "teacher", 10L, 3));
        assertFalse(WiseLessonRules.choosesBossHint(shortList, "teacher", 10L, 2));

        List<String> three = List.of("a", "b", "c");
        WiseLessonRules.BossHint exact = WiseLessonRules.nextBossHint(null, "teacher", 10L, three, 7);
        assertFalse(exact.chosen());
        assertEquals(three, exact.biomes());
        WiseLessonRules.BossHint shrunk = WiseLessonRules.nextBossHint(exact, "teacher", 10L, List.of("a", "c"), 7);
        assertFalse(shrunk.chosen());
        assertEquals(List.of("a", "c"), shrunk.biomes());

        List<String> five = List.of("a", "b", "c", "d", "e");
        assertTrue(WiseLessonRules.choosesBossHint(shortList, "teacher", 10L, five.size()));
        WiseLessonRules.BossHint picked = WiseLessonRules.nextBossHint(null, "teacher", 10L, five, 0);
        assertTrue(picked.chosen());
        assertEquals(10L, picked.assignedDay());
        assertEquals(List.of("c", "b", "a"), picked.biomes());
        assertFalse(WiseLessonRules.choosesBossHint(picked, "teacher", 10L, five.size()));
        assertEquals(picked.biomes(), WiseLessonRules.nextBossHint(picked, "teacher", 10L, five, 1).biomes());

        WiseLessonRules.BossHint grown = WiseLessonRules.nextBossHint(shortList, "teacher", 10L, five, 0);
        assertTrue(grown.chosen());
        assertEquals(List.of("c", "b", "a"), grown.biomes());

        List<String> withoutMiddle = List.of("a", "c", "d", "e", "f");
        WiseLessonRules.BossHint dropped = WiseLessonRules.nextBossHint(picked, "teacher", 10L, withoutMiddle, 99);
        assertTrue(dropped.chosen());
        assertEquals(List.of("c", "a"), dropped.biomes());

        List<String> others = List.of("x", "y", "z", "w");
        WiseLessonRules.BossHint emptied = WiseLessonRules.nextBossHint(picked, "teacher", 10L, others, 3);
        assertTrue(emptied.chosen());
        assertTrue(emptied.biomes().isEmpty());
        assertFalse(WiseLessonRules.choosesBossHint(emptied, "teacher", 10L, others.size()));
        WiseLessonRules.BossHint stillEmpty = WiseLessonRules.nextBossHint(emptied, "teacher", 10L, others, 4);
        assertTrue(stillEmpty.chosen());
        assertTrue(stillEmpty.biomes().isEmpty());

        assertTrue(WiseLessonRules.choosesBossHint(picked, "teacher", 40L, five.size()));
        WiseLessonRules.BossHint later = WiseLessonRules.nextBossHint(picked, "teacher", 40L, five, 1);
        assertTrue(later.chosen());
        assertEquals(List.of("d", "c", "a"), later.biomes());

        assertTrue(WiseLessonRules.choosesBossHint(picked, "other", 10L, five.size()));
        WiseLessonRules.BossHint otherTeacher = WiseLessonRules.nextBossHint(picked, "other", 10L, five, 1);
        assertEquals("other", otherTeacher.teacherId());
        assertEquals(List.of("d", "c", "a"), otherTeacher.biomes());

        WiseLessonRules.BossHint none = WiseLessonRules.nextBossHint(null, null, 10L, null, 0);
        assertFalse(none.chosen());
        assertEquals("", none.teacherId());
        assertTrue(none.biomes().isEmpty());
    }

    private static WiseLessonRules.Lesson hunt() {
        return new WiseLessonRules.Lesson(
                "teacher",
                "Alden the Wise Teacher",
                "minecraft:overworld",
                0,
                0,
                "minecraft:zombie",
                "zombies",
                5,
                200,
                "east",
                10,
                0,
                0,
                -1,
                true,
                List.of());
    }

    @Test
    void aKillPastHalfCountsAndADespawnDoesNot() {
        assertFalse(WiseLessonRules.moreThanHalf(2, 5));
        assertTrue(WiseLessonRules.moreThanHalf(3, 5));
        assertFalse(WiseLessonRules.moreThanHalf(2, 4));
        assertTrue(WiseLessonRules.moreThanHalf(3, 4));
        assertTrue(WiseLessonRules.moreThanHalf(1, 1));
        assertFalse(WiseLessonRules.moreThanHalf(0, 1));
        WiseLessonRules.KillCredit first = WiseLessonRules.creditKill(0, 5, false);
        assertEquals(1, first.killed());
        assertFalse(first.grantOffer());
        WiseLessonRules.KillCredit third = WiseLessonRules.creditKill(2, 5, false);
        assertEquals(3, third.killed());
        assertTrue(third.grantOffer());
        assertFalse(WiseLessonRules.creditKill(2, 5, true).grantOffer());
        assertFalse(WiseLessonRules.creditKill(1, 4, false).grantOffer());
        assertTrue(WiseLessonRules.creditKill(2, 4, false).grantOffer());
        assertTrue(WiseLessonRules.creditKill(0, 1, false).grantOffer());
    }
}
