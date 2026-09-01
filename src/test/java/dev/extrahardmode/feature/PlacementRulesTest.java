package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlacementRulesTest {
    @Test
    void torchYDenyUsesEnableBooleanNotSentinel() {
        assertTrue(PlacementRules.denyTorchY(true, 0, -1));
        assertFalse(PlacementRules.denyTorchY(true, 0, 0));
        assertFalse(PlacementRules.denyTorchY(true, 0, 12));
        assertFalse(PlacementRules.denyTorchY(false, 0, -64));
        assertFalse(PlacementRules.denyTorchY(false, 0, -1));
    }

    @Test
    void torchSoftDenyHonorsEnable() {
        assertTrue(PlacementRules.denyTorchSoft(true, true));
        assertFalse(PlacementRules.denyTorchSoft(true, false));
        assertFalse(PlacementRules.denyTorchSoft(false, true));
    }
}
