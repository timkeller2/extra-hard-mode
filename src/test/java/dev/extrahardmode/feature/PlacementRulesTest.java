package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
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

    @Test
    void denyPillarOnlyTheBlockUnderFeet() {
        BlockPos underFeet = new BlockPos(4, 10, 8);
        assertTrue(PlacementRules.denyPillar(true, false, underFeet, underFeet));
        assertFalse(PlacementRules.denyPillar(true, false, underFeet.above(), underFeet));
        assertFalse(PlacementRules.denyPillar(true, false, underFeet.below(), underFeet));
        assertFalse(PlacementRules.denyPillar(true, true, underFeet, underFeet));
        assertFalse(PlacementRules.denyPillar(false, false, underFeet, underFeet));
    }
}
