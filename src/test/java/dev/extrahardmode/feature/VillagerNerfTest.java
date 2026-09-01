package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VillagerNerfTest {
    @Test
    void moduleDefaultsOff() {
        FeatureRegistry registry = new FeatureRegistry();
        registry.register(new Tutorial());
        assertTrue(registry.defaultEnabled(Tutorial.ID));
        assertTrue(registry.defaultEnabled(dev.extrahardmode.ExtraHardModeMod.id("hardened_stone")));
        registry.register(new VillagerNerf());
        assertFalse(registry.defaultEnabled(VillagerNerf.ID));
        assertFalse(new VillagerNerf().defaultEnabled());
    }

    @Test
    void dropsDiamondGearWhenToggleOn() {
        assertTrue(VillagerNerf.shouldDropOffer(true, false, 5, false, true, true));
        assertFalse(VillagerNerf.shouldDropOffer(true, false, 5, false, false, true));
    }

    @Test
    void noviceAndApprenticeLoseMending() {
        assertTrue(VillagerNerf.shouldDropOffer(false, true, VillagerNerf.NOVICE, true, true, true));
        assertTrue(VillagerNerf.shouldDropOffer(false, true, VillagerNerf.APPRENTICE, true, true, true));
        assertFalse(VillagerNerf.shouldDropOffer(false, true, 3, true, true, true));
        assertFalse(VillagerNerf.shouldDropOffer(false, true, 4, true, true, true));
        assertFalse(VillagerNerf.shouldDropOffer(false, true, VillagerNerf.NOVICE, true, true, false));
        assertFalse(VillagerNerf.shouldDropOffer(false, true, VillagerNerf.NOVICE, false, true, true));
    }

    @Test
    void masterMaySellMendingAtDoubleEmeralds() {
        assertTrue(VillagerNerf.shouldOfferMasterMending(true, VillagerNerf.MASTER, false, true));
        assertFalse(VillagerNerf.shouldOfferMasterMending(true, VillagerNerf.MASTER, true, true));
        assertFalse(VillagerNerf.shouldOfferMasterMending(true, 4, false, true));
        assertFalse(VillagerNerf.shouldOfferMasterMending(false, VillagerNerf.MASTER, false, true));
        assertFalse(VillagerNerf.shouldOfferMasterMending(true, VillagerNerf.MASTER, false, false));
        assertEquals(20, VillagerNerf.MASTER_MENDING_EMERALDS);
    }

    @Test
    void leavesFarmerCartographerAndCopperTrades() {
        assertFalse(VillagerNerf.shouldDropOffer(false, false, 1, false, true, true));
        assertFalse(VillagerNerf.shouldDropOffer(false, false, 5, true, true, true));
    }

    @Test
    void moduleOffIsNoOp() {
        assertFalse(VillagerNerf.shouldDropOffer(true, true, VillagerNerf.NOVICE, true, false, false));
        assertFalse(VillagerNerf.shouldOfferMasterMending(true, VillagerNerf.MASTER, false, false));
    }
}
