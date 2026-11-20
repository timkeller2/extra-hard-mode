package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CartographyRulesTest {
    @Test
    void distanceGrowsWithTheHouseTheDiceAndFinishedMarks() {
        assertEquals(375, CartographyRules.distance(12, 0, 3, 0.0));
        assertEquals(750, CartographyRules.distance(12, 0, 18, 0.0));
        assertEquals(960, CartographyRules.distance(15, 2, 9, 0.0));
        assertEquals(1200, CartographyRules.distance(15, 2, 9, 1.0));
        assertEquals(720, CartographyRules.distance(15, 2, 9, -1.0));
        assertEquals(488, CartographyRules.distance(12, 1, 3, 0.0));
        assertEquals(1, CartographyRules.distance(0, 0, 0, -1.0));
    }

    @Test
    void threeDiceStayBetweenThreeAndEighteen() {
        assertEquals(3, CartographyRules.threeD6(0, 0, 0));
        assertEquals(18, CartographyRules.threeD6(5, 5, 5));
        assertEquals(12, CartographyRules.threeD6(1, 3, 5));
        assertEquals(6, CartographyRules.dieFace(-1));
    }

    @Test
    void rewardIsOneExperiencePerTenBlocksAndOneEmeraldPerHundred() {
        assertEquals(37, CartographyRules.experience(375));
        assertEquals(3, CartographyRules.emeralds(375));
        assertEquals(48, CartographyRules.experience(488));
        assertEquals(4, CartographyRules.emeralds(488));
        assertEquals(0, CartographyRules.experience(9));
        assertEquals(0, CartographyRules.emeralds(99));
        assertEquals(0, CartographyRules.experience(0));
        assertEquals(0, CartographyRules.emeralds(-4));
    }

    @Test
    void theFirstFinishWaitsAWeekAndEachFurtherFinishAddsThreeDays() {
        assertEquals(0, CartographyRules.cooldownDays(0));
        assertEquals(0, CartographyRules.cooldownDays(-2));
        assertEquals(7, CartographyRules.cooldownDays(1));
        assertEquals(10, CartographyRules.cooldownDays(2));
        assertEquals(13, CartographyRules.cooldownDays(3));
    }

    @Test
    void theMarkIsPlacedOnTheHorizontalAndIgnoresHeight() {
        assertEquals(110, CartographyRules.place(10, 20, 100, 0)[0]);
        assertEquals(20, CartographyRules.place(10, 20, 100, 0)[1]);
        assertEquals(10, CartographyRules.place(10, 20, 100, 90)[0]);
        assertEquals(120, CartographyRules.place(10, 20, 100, 90)[1]);
        assertTrue(CartographyRules.arrived(0, 0, 50, 0));
        assertTrue(CartographyRules.arrived(0, 0, 30, 40));
        assertFalse(CartographyRules.arrived(0, 0, 51, 0));
        assertTrue(CartographyRules.arrived(0, 0, 0, 0));
    }

    @Test
    void directionIsTheFurthestHorizontalAxis() {
        assertEquals("East", CartographyRules.direction(5, 1));
        assertEquals("West", CartographyRules.direction(-5, 1));
        assertEquals("South", CartographyRules.direction(1, 5));
        assertEquals("North", CartographyRules.direction(1, -5));
        assertEquals("East", CartographyRules.direction(3, 3));
        assertEquals("West", CartographyRules.direction(-3, 3));
        assertEquals("Here", CartographyRules.direction(0, 0));
        assertEquals("Jack: East", CartographyRules.markLine("Jack", "East"));
    }

    @Test
    void theLineFollowsThePlayerUntilTheMarkExpiresOrIsReached() {
        CartographyRules.Mission mark = CartographyRules.Mission.blank("c", "Jack")
                .assigned(200, 0, 200, "minecraft:overworld", 0L);
        assertEquals(14L, mark.expireDay());
        assertTrue(mark.active());
        long clock = 0L;
        assertEquals(
                List.of("Jack: East"),
                CartographyRules.hudLines(List.of(mark), "minecraft:overworld", 0, 0, 1L, clock));
        assertEquals(
                List.of("Jack: West"),
                CartographyRules.hudLines(List.of(mark), "minecraft:overworld", 400, 0, 1L, clock));
        assertEquals(
                List.of("Jack: East (2d)"),
                CartographyRules.hudLines(List.of(mark), "minecraft:overworld", 0, 0, 12L, 12L * 24_000L));
        assertTrue(CartographyRules.hudLines(List.of(mark), "minecraft:the_nether", 0, 0, 1L, clock)
                .isEmpty());
        assertTrue(CartographyRules.hudLines(List.of(mark), "minecraft:overworld", 160, 0, 1L, clock)
                .isEmpty());
        assertTrue(CartographyRules.hudLines(List.of(mark), "minecraft:overworld", 0, 0, 14L, 14L * 24_000L)
                .isEmpty());
        CartographyRules.Mission done = mark.completed(2L);
        assertFalse(done.active());
        assertEquals(1, done.successes());
        assertEquals(9L, done.nextDay());
        CartographyRules.Mission again = done.assigned(0, 80, 80, "minecraft:overworld", 9L).completed(9L);
        assertEquals(2, again.successes());
        assertEquals(19L, again.nextDay());
        CartographyRules.Mission missed = mark.expired();
        assertFalse(missed.active());
        assertEquals(0, missed.successes());
        assertEquals(-1L, missed.nextDay());
    }
}
