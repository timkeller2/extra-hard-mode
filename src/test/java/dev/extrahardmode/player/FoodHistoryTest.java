package dev.extrahardmode.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class FoodHistoryTest {
    @Test
    void firstFoodIsNovel() {
        assertTrue(FoodHistory.isNovel(List.of(), "minecraft:bread"));
        assertTrue(FoodHistory.isNovel(null, "minecraft:bread"));
    }

    @Test
    void repeatWithinWindowIsNotNovel() {
        List<String> recent = FoodHistory.record(List.of(), "minecraft:bread", 7);
        recent = FoodHistory.record(recent, "minecraft:apple", 7);
        assertFalse(FoodHistory.isNovel(recent, "minecraft:bread"));
        assertTrue(FoodHistory.isNovel(recent, "minecraft:cooked_beef"));
    }

    @Test
    void eighthDistinctPushesOutTheOldest() {
        List<String> recent = List.of();
        for (int i = 0; i < 7; i++) {
            recent = FoodHistory.record(recent, "food:" + i, 7);
        }
        assertEquals(7, recent.size());
        assertFalse(FoodHistory.isNovel(recent, "food:0"));
        recent = FoodHistory.record(recent, "food:7", 7);
        assertEquals(List.of("food:1", "food:2", "food:3", "food:4", "food:5", "food:6", "food:7"), recent);
        assertTrue(FoodHistory.isNovel(recent, "food:0"));
    }

    @Test
    void countInLastIgnoresOrderInsideTheWindow() {
        assertEquals(0, FoodHistory.countInLast(List.of(), "minecraft:bread", 7));
        assertEquals(1, FoodHistory.countInLast(List.of("minecraft:bread"), "minecraft:bread", 7));
        assertEquals(
                5,
                FoodHistory.countInLast(
                        List.of(
                                "minecraft:bread",
                                "minecraft:apple",
                                "minecraft:bread",
                                "minecraft:bread",
                                "minecraft:apple",
                                "minecraft:bread",
                                "minecraft:bread"),
                        "minecraft:bread",
                        7));
        assertEquals(
                2,
                FoodHistory.countInLast(
                        List.of("minecraft:bread", "minecraft:bread", "minecraft:apple"), "minecraft:bread", 7));
        assertEquals(
                4,
                FoodHistory.countInLast(
                        List.of("old", "a", "a", "a", "a", "b", "c"), "a", 7));
        assertEquals(3, FoodHistory.countInLast(List.of("a", "a", "a", "a"), "a", 3));
    }

    @Test
    void lastAreAllDifferentRequiresAFullUniqueWindow() {
        assertFalse(FoodHistory.lastAreAllDifferent(List.of("a", "b", "c"), 7));
        assertFalse(FoodHistory.lastAreAllDifferent(List.of("a", "b", "c", "d", "e", "f", "a"), 7));
        assertTrue(FoodHistory.lastAreAllDifferent(List.of("a", "b", "c", "d", "e", "f", "g"), 7));
        assertTrue(FoodHistory.lastAreAllDifferent(List.of("x", "a", "b", "c", "d", "e", "f", "g"), 7));
        assertFalse(FoodHistory.lastAreAllDifferent(null, 7));
    }
}
