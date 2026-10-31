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
        assertEquals(0, WiseLessonRules.serviceUses(false, false));
        assertEquals(1, WiseLessonRules.serviceUses(true, false));
        assertEquals(1, WiseLessonRules.serviceUses(false, true));
        assertEquals(2, WiseLessonRules.serviceUses(true, true));
        assertEquals(3, WiseLessonRules.packSize(24, 0));
        assertEquals(6, WiseLessonRules.packSize(24, 1));
        assertEquals(9, WiseLessonRules.packSize(24, 2));
        assertEquals(8, WiseLessonRules.packSize(38, 0));
        assertEquals(16, WiseLessonRules.packSize(38, 1));
        assertEquals(24, WiseLessonRules.packSize(38, 2));
        assertEquals(12, WiseLessonRules.packSize(31, 1));
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
        assertEquals(0, WiseLessonRules.unlockedTier(0));
        assertEquals(0, WiseLessonRules.unlockedTier(2));
        assertEquals(1, WiseLessonRules.unlockedTier(3));
        assertEquals(1, WiseLessonRules.unlockedTier(5));
        assertEquals(2, WiseLessonRules.unlockedTier(6));
        assertEquals(3, WiseLessonRules.unlockedTier(9));
        assertEquals(3, WiseLessonRules.unlockedTier(40));
        assertEquals("minecraft:zombie", WiseLessonRules.pickMob(0, 0).id());
        assertEquals("zombies", WiseLessonRules.pickMob(0, 0).plural());
        assertEquals("minecraft:husk", WiseLessonRules.pickMob(3, 0).id());
        assertEquals("cave spiders", WiseLessonRules.pickMob(3, 2).plural());
        assertEquals("minecraft:enderman", WiseLessonRules.pickMob(6, 0).id());
        assertEquals("vindicators", WiseLessonRules.pickMob(6, 3).plural());
        assertEquals("minecraft:ravager", WiseLessonRules.pickMob(9, 0).id());
        assertEquals("piglin brutes", WiseLessonRules.pickMob(9, 2).plural());
        assertEquals(4, WiseLessonRules.mobsAtTier(0).size());
        assertEquals(6, WiseLessonRules.mobsAtTier(1).size());
        assertEquals(4, WiseLessonRules.mobsAtTier(2).size());
        assertEquals(3, WiseLessonRules.mobsAtTier(3).size());
        for (WiseLessonRules.Mob mob : WiseLessonRules.MOBS) {
            assertFalse(mob.id().contains("drowned"));
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
        assertEquals(List.of("abc|13"), placed.mobs());
        WiseLessonRules.Lesson left = WiseLessonRules.afterSecond(placed, false, true, 0).lesson();
        assertEquals(0, left.dwellTicks());
        assertEquals(-1, left.spawnWait());
    }

    @Test
    void xpIsTripledAndMarksExpireAfterThreeDays() {
        assertEquals(15, WiseLessonRules.scaledXp(5));
        assertEquals(0, WiseLessonRules.scaledXp(0));
        assertEquals(0, WiseLessonRules.scaledXp(-2));
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
}
