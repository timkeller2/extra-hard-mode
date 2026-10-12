package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SourceMoveRulesTest {
    @Test
    void sealedHoleAllowsSourceWhenNothingAboveIsRequired() {
        assertTrue(SourceMoveRules.allowsSource(true, true, true, true, true, false, false));
    }

    @Test
    void openSideDeniesSource() {
        assertFalse(SourceMoveRules.allowsSource(true, true, true, false, true, false, false));
    }

    @Test
    void openFloorDeniesSource() {
        assertFalse(SourceMoveRules.allowsSource(false, true, true, true, true, false, false));
    }

    @Test
    void neighborSourceDeniesSealedHole() {
        assertFalse(SourceMoveRules.allowsSource(true, true, true, true, true, true, false));
    }

    @Test
    void receptacleAllowsSourceWithOpenSides() {
        assertTrue(SourceMoveRules.allowsSource(false, false, false, false, false, false, true));
    }

    @Test
    void receptacleBesideSourceDeniesSource() {
        assertFalse(SourceMoveRules.allowsSource(true, true, true, true, true, true, true));
    }
}
