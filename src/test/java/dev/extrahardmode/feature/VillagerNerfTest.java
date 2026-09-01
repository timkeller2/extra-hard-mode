package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.WorldConfig;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class VillagerNerfTest {
    record Trade(String result, boolean diamondGear, boolean mending) {
        static Trade of(String result, boolean diamondGear, boolean mending) {
            return new Trade(result, diamondGear, mending);
        }
    }

    @Test
    void missingKeyOnWorldConfigIsOffAfterRegister() {
        WorldConfig world = new WorldConfig(ExtraHardModeMod.id("testdim"));
        assertTrue(world.isModuleEnabled(Tutorial.ID));
        boolean already = false;
        for (FeatureModule module : ExtraHardModeMod.FEATURES) {
            if (module.id().equals(VillagerNerf.ID)) {
                already = true;
                break;
            }
        }
        if (!already) {
            ExtraHardModeMod.FEATURES.register(new VillagerNerf());
        }
        assertFalse(world.isModuleEnabled(VillagerNerf.ID));
        world.setModuleEnabled(VillagerNerf.ID, true);
        assertTrue(world.isModuleEnabled(VillagerNerf.ID));
        world.setModuleEnabled(VillagerNerf.ID, false);
        assertFalse(world.isModuleEnabled(VillagerNerf.ID));
    }

    @Test
    void applyDropsDiamondPickKeepsWheatMapAndCopperAtNovice() {
        List<Trade> offers = new ArrayList<>(List.of(
                Trade.of("diamond_pickaxe", true, false),
                Trade.of("wheat", false, false),
                Trade.of("mending_book", false, true),
                Trade.of("filled_map", false, false),
                Trade.of("copper_ingot", false, false)));
        VillagerNerf.apply(
                offers,
                Trade::diamondGear,
                Trade::mending,
                VillagerNerf.NOVICE,
                true,
                true,
                true,
                () -> Trade.of("master_mending", false, true));
        assertEquals(List.of("wheat", "filled_map", "copper_ingot"), results(offers));
    }

    @Test
    void applyKeepsJourneymanAndExpertMending() {
        List<Trade> journeyman = new ArrayList<>(List.of(Trade.of("mending_book", false, true), Trade.of("glass", false, false)));
        VillagerNerf.apply(
                journeyman, Trade::diamondGear, Trade::mending, 3, true, true, true, VillagerNerfTest::failMaster);
        assertEquals(List.of("mending_book", "glass"), results(journeyman));

        List<Trade> expert = new ArrayList<>(List.of(Trade.of("mending_book", false, true), Trade.of("clock", false, false)));
        VillagerNerf.apply(expert, Trade::diamondGear, Trade::mending, 4, true, true, true, VillagerNerfTest::failMaster);
        assertEquals(List.of("mending_book", "clock"), results(expert));
    }

    @Test
    void applyMasterInjectsMendingWhenMissingKeepsExisting() {
        List<Trade> missing = new ArrayList<>(List.of(Trade.of("yellow_candle", false, false)));
        VillagerNerf.apply(
                missing,
                Trade::diamondGear,
                Trade::mending,
                VillagerNerf.MASTER,
                true,
                true,
                true,
                () -> Trade.of("master_mending", false, true));
        assertEquals(List.of("yellow_candle", "master_mending"), results(missing));

        List<Trade> existing = new ArrayList<>(List.of(Trade.of("mending_book", false, true)));
        VillagerNerf.apply(
                existing,
                Trade::diamondGear,
                Trade::mending,
                VillagerNerf.MASTER,
                true,
                true,
                true,
                VillagerNerfTest::failMaster);
        assertEquals(List.of("mending_book"), results(existing));
    }

    @Test
    void applyFarmerCartographerCopperStayWhenNotDiamondOrMending() {
        List<Trade> offers = new ArrayList<>(List.of(
                Trade.of("wheat", false, false),
                Trade.of("filled_map", false, false),
                Trade.of("copper_ingot", false, false)));
        VillagerNerf.apply(
                offers,
                Trade::diamondGear,
                Trade::mending,
                VillagerNerf.MASTER,
                false,
                true,
                true,
                VillagerNerfTest::failMaster);
        assertEquals(List.of("wheat", "filled_map", "copper_ingot"), results(offers));
    }

    @Test
    void applyNoOpWhenSubtogglesOff() {
        List<Trade> offers = new ArrayList<>(List.of(
                Trade.of("diamond_pickaxe", true, false), Trade.of("mending_book", false, true)));
        VillagerNerf.apply(
                offers,
                Trade::diamondGear,
                Trade::mending,
                VillagerNerf.NOVICE,
                true,
                false,
                false,
                VillagerNerfTest::failMaster);
        assertEquals(List.of("diamond_pickaxe", "mending_book"), results(offers));
    }

    @Test
    void shouldDropMatchesApplyContract() {
        assertTrue(VillagerNerf.shouldDropOffer(true, false, 5, false, true, true));
        assertFalse(VillagerNerf.shouldDropOffer(true, false, 5, false, false, true));
        assertTrue(VillagerNerf.shouldDropOffer(false, true, VillagerNerf.NOVICE, true, true, true));
        assertTrue(VillagerNerf.shouldDropOffer(false, true, VillagerNerf.APPRENTICE, true, true, true));
        assertFalse(VillagerNerf.shouldDropOffer(false, true, 3, true, true, true));
        assertFalse(VillagerNerf.shouldDropOffer(false, true, 4, true, true, true));
        assertEquals(20, VillagerNerf.MASTER_MENDING_EMERALDS);
        assertFalse(new VillagerNerf().defaultEnabled());
    }

    private static List<String> results(List<Trade> offers) {
        return offers.stream().map(Trade::result).toList();
    }

    private static Trade failMaster() {
        throw new AssertionError("master mending must not be injected");
    }
}
