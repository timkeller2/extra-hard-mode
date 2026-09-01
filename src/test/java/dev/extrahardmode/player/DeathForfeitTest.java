package dev.extrahardmode.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DeathForfeitTest {
    @Test
    void tenPercentOfTwentyStacksIsTwo() {
        assertEquals(2, DeathForfeit.stacksToRemove(20, 10));
    }

    @Test
    void tenPercentRoundsDownButAtLeastOne() {
        assertEquals(1, DeathForfeit.stacksToRemove(5, 10));
        assertEquals(1, DeathForfeit.stacksToRemove(9, 10));
        assertEquals(1, DeathForfeit.stacksToRemove(10, 10));
    }

    @Test
    void zeroPercentRemovesNothing() {
        assertEquals(0, DeathForfeit.stacksToRemove(10, 0));
    }

    @Test
    void emptyInventoryRemovesNothing() {
        assertEquals(0, DeathForfeit.stacksToRemove(0, 10));
    }

    @Test
    void cannotRemoveMoreThanPresent() {
        assertEquals(3, DeathForfeit.stacksToRemove(3, 100));
    }
}
