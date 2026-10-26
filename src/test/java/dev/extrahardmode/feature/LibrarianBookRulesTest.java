package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LibrarianBookRulesTest {
    @Test
    void higherRanksKeepEarlierEnchantments() {
        assertEquals(5, LibrarianBookRules.requiredMaxLevel(1));
        assertEquals(4, LibrarianBookRules.requiredMaxLevel(2));
        assertEquals(3, LibrarianBookRules.requiredMaxLevel(3));
        assertEquals(2, LibrarianBookRules.requiredMaxLevel(4));
        assertEquals(1, LibrarianBookRules.requiredMaxLevel(5));
        assertTrue(LibrarianBookRules.enchantmentAllowed(1, 5));
        assertFalse(LibrarianBookRules.enchantmentAllowed(1, 4));
        assertFalse(LibrarianBookRules.enchantmentAllowed(1, 1));
        assertTrue(LibrarianBookRules.enchantmentAllowed(2, 5));
        assertTrue(LibrarianBookRules.enchantmentAllowed(2, 4));
        assertFalse(LibrarianBookRules.enchantmentAllowed(2, 3));
        assertTrue(LibrarianBookRules.enchantmentAllowed(3, 5));
        assertTrue(LibrarianBookRules.enchantmentAllowed(3, 3));
        assertFalse(LibrarianBookRules.enchantmentAllowed(3, 2));
        assertTrue(LibrarianBookRules.enchantmentAllowed(5, 5));
        assertTrue(LibrarianBookRules.enchantmentAllowed(5, 1));
        assertFalse(LibrarianBookRules.enchantmentAllowed(4, 1));
        assertFalse(LibrarianBookRules.enchantmentAllowed(0, 5));
        assertFalse(LibrarianBookRules.enchantmentAllowed(6, 5));
    }

    @Test
    void bookLevelCannotPassTheLibrarian() {
        assertEquals(1, LibrarianBookRules.bookLevel(1, 1, 5, 5));
        assertEquals(5, LibrarianBookRules.bookLevel(5, 1, 5, 5));
        assertEquals(4, LibrarianBookRules.bookLevel(4, 1, 5, 5));
        assertEquals(1, LibrarianBookRules.bookLevel(5, 1, 5, 1));
        assertEquals(2, LibrarianBookRules.bookLevel(2, 1, 4, 4));
        assertEquals(1, LibrarianBookRules.bookLevel(2, 1, 4, 1));
        assertEquals(4, LibrarianBookRules.bookLevel(5, 1, 4, 4));
        assertEquals(0, LibrarianBookRules.bookLevel(1, 1, 4, 4));
        assertEquals(3, LibrarianBookRules.bookLevel(3, 1, 3, 3));
        assertEquals(2, LibrarianBookRules.bookLevel(4, 1, 2, 2));
        assertEquals(0, LibrarianBookRules.bookLevel(4, 1, 1, 1));
        assertEquals(1, LibrarianBookRules.bookLevel(5, 1, 1, 1));
        assertEquals(0, LibrarianBookRules.bookLevel(1, 1, 3, 3));
    }

    @Test
    void emeraldPriceMatchesTheVanillaBookFormula() {
        assertEquals(15, LibrarianBookRules.priceRollBound(1));
        assertEquals(5, LibrarianBookRules.emeraldCost(1, 0, false));
        assertEquals(19, LibrarianBookRules.emeraldCost(1, 14, false));
        assertEquals(10, LibrarianBookRules.emeraldCost(1, 0, true));
        assertEquals(64, LibrarianBookRules.emeraldCost(5, 54, false));
        assertEquals(38, LibrarianBookRules.emeraldCost(1, 14, true));
    }
}
