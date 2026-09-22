package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.resources.Identifier;
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
    void campfireSharesTorchYCutoff() {
        assertTrue(PlacementRules.denyFlameY(true, 0, -1, true));
        assertFalse(PlacementRules.denyFlameY(true, 0, 0, true));
        assertFalse(PlacementRules.denyFlameY(true, 0, -1, false));
        assertFalse(PlacementRules.denyFlameY(false, 0, -1, true));
    }

    @Test
    void torchSoftDenyHonorsEnable() {
        assertTrue(PlacementRules.denyTorchSoft(true, true));
        assertFalse(PlacementRules.denyTorchSoft(true, false));
        assertFalse(PlacementRules.denyTorchSoft(false, true));
    }

    @Test
    void offhandLightIsDeniedWhenEnabled() {
        assertTrue(PlacementRules.denyOffhandLight(true, true));
        assertFalse(PlacementRules.denyOffhandLight(true, false));
        assertFalse(PlacementRules.denyOffhandLight(false, true));
        assertTrue(PlacementRules.isOffhandLightId("minecraft:torch"));
        assertTrue(PlacementRules.isOffhandLightId("minecraft:soul_torch"));
        assertTrue(PlacementRules.isOffhandLightId("minecraft:copper_torch"));
        assertTrue(PlacementRules.isOffhandLightId("minecraft:lantern"));
        assertTrue(PlacementRules.isOffhandLightId("minecraft:soul_lantern"));
        assertTrue(PlacementRules.isOffhandLightId("minecraft:copper_lantern"));
        assertTrue(PlacementRules.isOffhandLightId("minecraft:glowstone"));
        assertFalse(PlacementRules.isOffhandLightId("minecraft:glowstone_dust"));
        assertFalse(PlacementRules.isOffhandLightId("minecraft:jack_o_lantern"));
        assertFalse(PlacementRules.isOffhandLightId("minecraft:redstone_torch"));
        assertFalse(PlacementRules.isOffhandLightId("minecraft:stick"));
    }

    @Test
    void torchLikeFromPayloadUsesIdsNotLiveTags() {
        Identifier torch = Identifier.parse("minecraft:torch");
        Identifier lantern = Identifier.parse("minecraft:lantern");
        Identifier dirt = Identifier.parse("minecraft:dirt");
        List<Identifier> lights = List.of(torch, lantern);
        assertTrue(PlacementRules.torchLikeFromPayload(true, dirt, dirt, lights));
        assertTrue(PlacementRules.torchLikeFromPayload(false, torch, dirt, lights));
        assertTrue(PlacementRules.torchLikeFromPayload(false, dirt, lantern, lights));
        assertFalse(PlacementRules.torchLikeFromPayload(false, dirt, dirt, lights));
        assertFalse(PlacementRules.torchLikeFromPayload(false, null, null, lights));
    }

    @Test
    void airbornePlacementIncludesJumpingAndTheEdgeClip() {
        assertTrue(PlacementRules.airborneForPlacement(false, true, false, false, false));
        assertTrue(PlacementRules.airborneForPlacement(true, false, false, false, false));
        assertFalse(PlacementRules.airborneForPlacement(true, true, false, false, false));
        assertFalse(PlacementRules.airborneForPlacement(false, false, true, false, false));
        assertFalse(PlacementRules.airborneForPlacement(false, false, false, true, false));
        assertFalse(PlacementRules.airborneForPlacement(false, false, false, false, true));
        assertTrue(PlacementRules.denyAirPlacement(true, true));
        assertFalse(PlacementRules.denyAirPlacement(true, false));
        assertFalse(PlacementRules.denyAirPlacement(false, true));
    }
}
