package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UnsafeBedsTest {
    @Test
    void netherAndEndBedsVanish() {
        assertTrue(UnsafeBeds.vanishes(true, true));
        assertFalse(UnsafeBeds.vanishes(false, true));
        assertFalse(UnsafeBeds.vanishes(true, false));
    }
}
