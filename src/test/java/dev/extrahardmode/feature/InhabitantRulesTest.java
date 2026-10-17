package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InhabitantRulesTest {
    @Test
    void compassDirectionUsesTheRisingSunAsEast() {
        assertEquals("east", InhabitantRules.compassDirection(10, 0));
        assertEquals("west", InhabitantRules.compassDirection(-10, 0));
        assertEquals("south", InhabitantRules.compassDirection(0, 10));
        assertEquals("north", InhabitantRules.compassDirection(0, -10));
        assertEquals("northeast", InhabitantRules.compassDirection(10, -10));
        assertEquals("southeast", InhabitantRules.compassDirection(10, 10));
        assertEquals("southwest", InhabitantRules.compassDirection(-10, 10));
        assertEquals("northwest", InhabitantRules.compassDirection(-10, -10));
        assertEquals("east", InhabitantRules.compassDirection(10, 1));
        assertEquals(null, InhabitantRules.compassDirection(0, 0));
    }

    @Test
    void missingResidentIsClearedOnlyWhenTheHomeAreaCanSeeThem() {
        assertFalse(InhabitantRules.residentMissing(true, true, true));
        assertFalse(InhabitantRules.residentMissing(true, true, false));
        assertTrue(InhabitantRules.residentMissing(true, false, true));
        assertTrue(InhabitantRules.residentMissing(true, false, false));
        assertFalse(InhabitantRules.residentMissing(false, false, false));
        assertTrue(InhabitantRules.residentMissing(false, false, true));
    }

    @Test
    void volumeAndEnclosure() {
        assertFalse(InhabitantRules.volumeOk(23));
        assertTrue(InhabitantRules.volumeOk(24));
        assertTrue(InhabitantRules.volumeOk(80));
        assertTrue(InhabitantRules.volumeOk(250));
        assertTrue(InhabitantRules.volumeOk(300));
        assertFalse(InhabitantRules.volumeOk(301));
        assertTrue(InhabitantRules.fillOpen(512, true));
        assertFalse(InhabitantRules.fillOpen(512, false));
        assertFalse(InhabitantRules.fillOpen(250, true));
        assertTrue(InhabitantRules.enclosed(40, false));
        assertFalse(InhabitantRules.enclosed(512, true));
        assertFalse(InhabitantRules.enclosed(0, false));
        assertFalse(InhabitantRules.interiorCell(true, false, true, true));
        assertTrue(InhabitantRules.interiorCell(false, false, true, false));
        assertTrue(InhabitantRules.interiorCell(false, true, false, false));
        assertTrue(InhabitantRules.interiorCell(false, false, false, true));
        assertFalse(InhabitantRules.interiorCell(false, false, false, false));
    }

    @Test
    void amenityScoreCaps() {
        InhabitantRules.AmenityCounts empty =
                new InhabitantRules.AmenityCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, false);
        assertEquals(0, InhabitantRules.score(empty));
        InhabitantRules.AmenityCounts windows =
                new InhabitantRules.AmenityCounts(10, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, false);
        assertEquals(6, InhabitantRules.score(windows));
        InhabitantRules.AmenityCounts rugs =
                new InhabitantRules.AmenityCounts(0, 0, 40, 0, 0, 0, 0, 0, 0, 0, false, false);
        assertEquals(3, InhabitantRules.score(rugs));
        InhabitantRules.AmenityCounts furnished =
                new InhabitantRules.AmenityCounts(3, 2, 8, 2, 2, 1, 2, 1, 1, 1, true, true);
        assertEquals(6 + 2 + 2 + 2 + 2 + 1 + 2 + 1 + 1 + 1 + 2, InhabitantRules.score(furnished));
        InhabitantRules.AmenityCounts art =
                new InhabitantRules.AmenityCounts(0, 10, 0, 0, 0, 0, 0, 0, 0, 0, false, false);
        assertEquals(6, InhabitantRules.score(art));
        InhabitantRules.AmenityCounts maxed =
                new InhabitantRules.AmenityCounts(10, 10, 40, 10, 10, 10, 10, 10, 10, 1, true, true);
        assertEquals(30, InhabitantRules.score(maxed));
        assertEquals(0, InhabitantRules.volumePoints(47));
        assertEquals(1, InhabitantRules.volumePoints(48));
        assertEquals(1, InhabitantRules.volumePoints(95));
        assertEquals(2, InhabitantRules.volumePoints(96));
        assertEquals(5, InhabitantRules.volumePoints(240));
        assertEquals(5, InhabitantRules.volumePoints(1000));
        assertEquals(0, InhabitantRules.roomPoints(0));
        assertEquals(1, InhabitantRules.roomPoints(1));
        assertEquals(3, InhabitantRules.roomPoints(3));
        assertEquals(3, InhabitantRules.roomPoints(9));
        assertEquals(34, InhabitantRules.score(maxed, 96, 2));
        assertEquals(30, InhabitantRules.MASTER_SCORE);
        assertEquals(12, InhabitantRules.MIN_SCORE);
    }

    @Test
    void gatesNeedScoreAndBasics() {
        InhabitantRules.AmenityCounts poor =
                new InhabitantRules.AmenityCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, false);
        InhabitantRules.GateResult closed = InhabitantRules.gates(true, 40, true, true, true, true, true, poor);
        assertFalse(closed.eligible());
        assertTrue(closed.missing().stream().anyMatch(s -> s.contains("more furnishings")));
        assertTrue(InhabitantRules.inspectFallback(closed).contains("/tougher help homes"));
        InhabitantRules.AmenityCounts ok =
                new InhabitantRules.AmenityCounts(3, 2, 8, 1, 1, 1, 1, 1, 1, 0, false, false);
        assertTrue(InhabitantRules.score(ok) >= InhabitantRules.MIN_SCORE);
        InhabitantRules.GateResult pass = InhabitantRules.gates(true, 40, true, true, true, true, true, ok);
        assertTrue(pass.eligible());
        assertTrue(pass.missing().isEmpty());
        InhabitantRules.GateResult open = InhabitantRules.gates(false, 40, true, true, true, true, true, ok);
        assertFalse(open.eligible());
        assertTrue(open.missing().contains("not enclosed"));
        assertTrue(InhabitantRules.inspectFallback(pass).contains("Eligible"));
        assertTrue(InhabitantRules.inspectFallback(open).contains("Missing"));
    }

    @Test
    void spacingAndDawnChance() {
        assertFalse(InhabitantRules.farEnough(47, 0, 48));
        assertTrue(InhabitantRules.farEnough(48, 0, 48));
        assertTrue(InhabitantRules.farEnough(0, 48, 48));
        assertEquals(0, InhabitantRules.spawnChancePercent(11, false));
        assertEquals(8, InhabitantRules.spawnChancePercent(12, false));
        assertEquals(20, InhabitantRules.spawnChancePercent(18, false));
        assertEquals(40, InhabitantRules.spawnChancePercent(40, false));
        assertEquals(10, InhabitantRules.spawnChancePercent(18, true));
        assertTrue(InhabitantRules.spawnRoll(0, 8));
        assertFalse(InhabitantRules.spawnRoll(8, 8));
        assertFalse(InhabitantRules.spawnRoll(0, 0));
    }

    @Test
    void specialtyAndEconomy() {
        InhabitantRules.AmenityCounts kitchen =
                new InhabitantRules.AmenityCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, true, true);
        boolean sawCook = false;
        for (int roll = 0; roll < 20; roll++) {
            if (InhabitantRules.COOK.equals(InhabitantRules.pickSpecialty(kitchen, 12, roll))) {
                sawCook = true;
            }
        }
        assertTrue(sawCook);
        assertEquals("hauler", InhabitantRules.specialtyFallback(InhabitantRules.HAULER));
        assertEquals("farm neighbor", InhabitantRules.specialtyFallback(InhabitantRules.FARM));
        assertEquals(32, InhabitantRules.haulerStack(1));
        assertEquals(16, InhabitantRules.haulerStack(2));
        assertEquals(16, InhabitantRules.farmWheatBuy(false));
        assertEquals(8, InhabitantRules.farmWheatBuy(true));
        assertEquals("Alder", InhabitantRules.pickName(0));
        assertEquals(InhabitantRules.pickName(0), InhabitantRules.pickName(20));
        assertEquals(
                List.of("hauler", "cook", "farm", "trader", "council", "brewer"),
                InhabitantRules.unseenSpecialties(List.of(), 12));
        assertEquals(
                List.of("hauler", "cook", "farm", "bounty", "trader", "wealthy", "jeweler", "council", "brewer"),
                InhabitantRules.unseenSpecialties(List.of(), 18));
        assertTrue(InhabitantRules.unseenSpecialties(List.of(), 24).contains(InhabitantRules.ARMORSMITH));
        assertTrue(InhabitantRules.unseenSpecialties(List.of(), 24).contains(InhabitantRules.WISE_TEACHER));
        assertFalse(InhabitantRules.specialtyAllowed(InhabitantRules.WISE_TEACHER, 23));
        assertTrue(InhabitantRules.specialtyAllowed(InhabitantRules.WISE_TEACHER, 24));
        assertTrue(InhabitantRules.unseenSpecialties(List.of(), 30).contains(InhabitantRules.MASTER_ARMORSMITH));
        assertFalse(InhabitantRules.specialtyAllowed(InhabitantRules.ARMORSMITH, 23));
        assertTrue(InhabitantRules.specialtyAllowed(InhabitantRules.ARMORSMITH, 24));
        assertFalse(InhabitantRules.specialtyAllowed(InhabitantRules.MASTER_ARMORSMITH, 29));
        assertTrue(InhabitantRules.specialtyAllowed(InhabitantRules.MASTER_ARMORSMITH, 30));
        assertFalse(InhabitantRules.specialtyAllowed(InhabitantRules.WEALTHY, 17));
        assertTrue(InhabitantRules.specialtyAllowed(InhabitantRules.WEALTHY, 18));
        assertFalse(InhabitantRules.specialtyAllowed(InhabitantRules.JEWELER, 17));
        assertTrue(InhabitantRules.specialtyAllowed(InhabitantRules.JEWELER, 18));
        assertTrue(InhabitantRules.unseenSpecialties(InhabitantRules.SPECIALTIES).isEmpty());
        assertEquals(
                InhabitantRules.TRADER,
                InhabitantRules.pickSpecialty(kitchen, 12, 0, List.of("hauler", "cook", "farm", "bounty")));
        assertEquals(
                InhabitantRules.ARMORSMITH,
                InhabitantRules.pickSpecialty(
                        kitchen,
                        24,
                        0,
                        List.of("hauler", "cook", "farm", "bounty", "trader", "wealthy")));
        assertEquals("armorsmith", InhabitantRules.specialtyFallback(InhabitantRules.ARMORSMITH));
        assertEquals("master armorsmith", InhabitantRules.specialtyFallback(InhabitantRules.MASTER_ARMORSMITH));
        assertEquals("wealthy trader", InhabitantRules.specialtyFallback(InhabitantRules.WEALTHY));
        assertEquals("council member", InhabitantRules.specialtyFallback(InhabitantRules.COUNCIL));
        assertEquals("wise teacher", InhabitantRules.specialtyFallback(InhabitantRules.WISE_TEACHER));
        assertEquals("jeweler", InhabitantRules.specialtyFallback(InhabitantRules.JEWELER));
        assertEquals("brewer", InhabitantRules.specialtyFallback(InhabitantRules.BREWER));
        assertEquals("Lumen the Brewer", InhabitantRules.nameplate("Lumen", InhabitantRules.BREWER));
        assertFalse(InhabitantRules.specialtyAllowed(InhabitantRules.BREWER, 11));
        assertTrue(InhabitantRules.specialtyAllowed(InhabitantRules.BREWER, 12));
        InhabitantRules.Outfit brewerOutfit = InhabitantRules.pickOutfit(InhabitantRules.BREWER, 0, 0, 0);
        assertEquals(1, brewerOutfit.level());
        assertTrue(InhabitantRules.COMMON_OUTFITS.contains(brewerOutfit.professionId()));
        assertEquals("drop collector", InhabitantRules.specialtyFallback(InhabitantRules.BOUNTY));
        assertEquals("Nia the Drop Collector", InhabitantRules.nameplate("Nia", InhabitantRules.BOUNTY));
        assertEquals("Mira the Jeweler", InhabitantRules.nameplate("Mira", InhabitantRules.JEWELER));
        assertEquals("Ada the Council Member", InhabitantRules.nameplate("Ada", InhabitantRules.COUNCIL));
        assertEquals("Bram the Master Armorsmith", InhabitantRules.nameplate("Bram", InhabitantRules.MASTER_ARMORSMITH));
        assertEquals("Cora the Farm Neighbor", InhabitantRules.nameplate(" Cora ", InhabitantRules.FARM));
        assertEquals("Traveler the Hauler", InhabitantRules.nameplate("  ", InhabitantRules.HAULER));
        InhabitantRules.Outfit hauler = InhabitantRules.pickOutfit(InhabitantRules.HAULER, 0, 0, 0);
        InhabitantRules.Outfit otherHauler = InhabitantRules.pickOutfit(InhabitantRules.HAULER, 1, 1, 1);
        assertNotEquals(hauler.professionId(), otherHauler.professionId());
        assertNotEquals(hauler.styleId(), otherHauler.styleId());
        assertEquals(1, hauler.level());
        assertEquals(2, otherHauler.level());
        assertFalse(hauler.professionId().contains("armorer"));
        InhabitantRules.Outfit jeweler = InhabitantRules.pickOutfit(InhabitantRules.JEWELER, 0, 2, 1);
        assertEquals("minecraft:librarian", jeweler.professionId());
        assertEquals(3, jeweler.level());
        assertFalse(jeweler.professionId().contains("farmer"));
        InhabitantRules.Outfit councilOutfit = InhabitantRules.pickOutfit(InhabitantRules.COUNCIL, 0, 4, 1);
        assertEquals("minecraft:armorer", councilOutfit.professionId());
        assertEquals("minecraft:snow", councilOutfit.styleId());
        assertEquals(5, councilOutfit.level());
        assertEquals(councilOutfit, InhabitantRules.parseOutfit(InhabitantRules.outfitKey(councilOutfit)));
        assertNull(InhabitantRules.parseOutfit("bad"));
        assertEquals(30, InhabitantRules.WISE_ABILITY_EMERALDS);
        assertEquals(30, InhabitantRules.wiseAbilityPrice(24));
        assertEquals(28, InhabitantRules.wiseAbilityPrice(25));
        assertEquals(30, InhabitantRules.wiseAbilityPrice(12));
        assertEquals(2, InhabitantRules.wiseAbilityPrice(38));
        assertEquals(0, InhabitantRules.wiseAbilityPrice(39));
        assertEquals(AbilityRules.HEAL, InhabitantRules.pickWiseAbility(0));
        assertEquals(AbilityRules.ABILITY_IDS.get(1), InhabitantRules.pickWiseAbility(1));
        assertTrue(InhabitantRules.shouldTeachAbility(false, 30, 30));
        assertFalse(InhabitantRules.shouldTeachAbility(true, 30, 30));
        assertFalse(InhabitantRules.shouldTeachAbility(false, 29, 30));
        assertTrue(InhabitantRules.shouldTeachAbility(false, 0, 0));
        assertTrue(InhabitantRules.shouldTeachMana(true, true, true, false));
        assertFalse(InhabitantRules.shouldTeachMana(true, true, true, true));
        assertFalse(InhabitantRules.shouldTeachMana(false, true, true, false));
        assertTrue(InhabitantRules.alreadyTookManaLesson(List.of("abc"), "abc"));
        assertFalse(InhabitantRules.alreadyTookManaLesson(List.of("abc"), "def"));
        assertTrue(InhabitantRules.scaledListings(InhabitantRules.WISE_TEACHER, 30, 0, false, new java.util.Random(1))
                .isEmpty());
        for (String ability : AbilityRules.ABILITY_IDS) {
            assertFalse(AbilityRules.teacherFlavor(ability).isEmpty());
            assertFalse(AbilityRules.teacherInstruction(ability).isEmpty());
        }
        assertTrue(InhabitantRules.specialtyAllowed(InhabitantRules.COUNCIL, 12));
        assertEquals(4, InhabitantRules.defaultTradeTypeCount(InhabitantRules.COUNCIL));
        boolean sawCookAfterAll = false;
        boolean sawBrewerInKitchen = false;
        for (int roll = 0; roll < 80; roll++) {
            String picked = InhabitantRules.pickSpecialty(kitchen, 12, roll, InhabitantRules.SPECIALTIES);
            if (InhabitantRules.COOK.equals(picked)) {
                sawCookAfterAll = true;
            }
            if (InhabitantRules.BREWER.equals(picked)) {
                sawBrewerInKitchen = true;
            }
        }
        assertTrue(sawCookAfterAll);
        assertTrue(sawBrewerInKitchen);
        InhabitantRules.AmenityCounts cauldronOnly =
                new InhabitantRules.AmenityCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, true);
        assertEquals(0, InhabitantRules.score(cauldronOnly));
        boolean sawBrewer = false;
        for (int roll = 0; roll < 80; roll++) {
            if (InhabitantRules.BREWER.equals(
                    InhabitantRules.pickSpecialty(cauldronOnly, 12, roll, InhabitantRules.SPECIALTIES))) {
                sawBrewer = true;
            }
        }
        assertTrue(sawBrewer);
        InhabitantRules.AmenityCounts empty =
                new InhabitantRules.AmenityCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, false);
        int council = 0;
        for (int roll = 0; roll < 80; roll++) {
            if (InhabitantRules.COUNCIL.equals(
                    InhabitantRules.pickSpecialty(empty, 12, roll, InhabitantRules.SPECIALTIES))) {
                council++;
            }
        }
        assertTrue(council >= 50);
        boolean emptyBrewer = false;
        for (int roll = 0; roll < 80; roll++) {
            if (InhabitantRules.BREWER.equals(
                    InhabitantRules.pickSpecialty(empty, 12, roll, InhabitantRules.SPECIALTIES))) {
                emptyBrewer = true;
            }
        }
        assertFalse(emptyBrewer);
    }

    @Test
    void noviceCostumeStaysWhenTheResidentHasExperience() {
        assertTrue(InhabitantRules.vanillaClearsNoviceCostume(1, 0));
        assertFalse(InhabitantRules.vanillaClearsNoviceCostume(1, InhabitantRules.COSTUME_EXPERIENCE));
        assertFalse(InhabitantRules.vanillaClearsNoviceCostume(2, 0));
        assertEquals(1, InhabitantRules.COSTUME_EXPERIENCE);
    }

    @Test
    void moduleDefaultsOn() {
        assertTrue(new Inhabitants().defaultEnabled());
        assertEquals("inhabitants", Inhabitants.ID.getPath());
    }

    @Test
    void leaveAndKillTimers() {
        assertEquals(13, InhabitantRules.leaveAfterDay(10));
        assertEquals(17, InhabitantRules.emptyUntilDay(10));
        assertFalse(InhabitantRules.shouldLeave(12, 13));
        assertTrue(InhabitantRules.shouldLeave(13, 13));
        assertTrue(InhabitantRules.spawnBlocked(16, 17));
        assertFalse(InhabitantRules.spawnBlocked(17, 17));
        assertFalse(InhabitantRules.spawnBlocked(10, -1));
        assertEquals(0, InhabitantRules.missedArrivalRolls(-1, 10));
        assertEquals(0, InhabitantRules.missedArrivalRolls(5, 5));
        assertEquals(0, InhabitantRules.missedArrivalRolls(5, 4));
        assertEquals(1, InhabitantRules.missedArrivalRolls(5, 6));
        assertEquals(3, InhabitantRules.missedArrivalRolls(5, 8));
        assertEquals(365, InhabitantRules.missedArrivalRolls(0, 10_000));
    }

    @Test
    void restockEveryFourteenDays() {
        assertEquals(14, InhabitantRules.RESTOCK_DAYS);
        assertTrue(InhabitantRules.shouldRestock(-1, 0));
        assertTrue(InhabitantRules.shouldRestock(-1, 10));
        assertFalse(InhabitantRules.shouldRestock(5, 5));
        assertFalse(InhabitantRules.shouldRestock(5, 18));
        assertTrue(InhabitantRules.shouldRestock(5, 19));
        assertTrue(InhabitantRules.shouldRestock(5, 20));
        assertEquals(14, InhabitantRules.restockDays(InhabitantRules.TRADER));
        assertEquals(30, InhabitantRules.restockDays(InhabitantRules.ARMORSMITH));
        assertEquals(30, InhabitantRules.restockDays(InhabitantRules.MASTER_ARMORSMITH));
        assertFalse(InhabitantRules.shouldRestock(0, 29, 30));
        assertTrue(InhabitantRules.shouldRestock(0, 30, 30));
        long day = CropGrowthRules.TICKS_PER_DAY;
        assertEquals(14 * day, InhabitantRules.restockRemainingTicks(5 * day, 5, 5, 14));
        assertEquals(14 * day - 1000, InhabitantRules.restockRemainingTicks(5 * day + 1000, 5, 5, 14));
        assertEquals(1, InhabitantRules.restockRemainingTicks(19 * day - 1, 5, 18, 14));
        assertEquals(day, InhabitantRules.restockRemainingTicks(19 * day, 5, 19, 14));
        assertEquals(day - 1000, InhabitantRules.restockRemainingTicks(19 * day + 1000, 5, 19, 14));
        assertEquals(0, InhabitantRules.restockRemainingTicks(19 * day + 1000, 5, 18, 14));
        assertEquals(day, InhabitantRules.restockRemainingTicks(20 * day, 5, 20, 14));
        assertEquals(30 * day, InhabitantRules.restockRemainingTicks(0, 0, 0, 30));
        assertEquals(day, InhabitantRules.restockRemainingTicks(30 * day, 0, 30, 30));
        assertEquals(day, InhabitantRules.restockRemainingTicks(10 * day, -1, 10, 14));
        assertEquals(day - 1000, InhabitantRules.restockRemainingTicks(10 * day + 1000, -1, 10, 14));
        assertEquals(0, InhabitantRules.restockRemainingTicks(10 * day + 1000, -1, 9, 14));
        assertEquals(day, InhabitantRules.restockRemainingTicks(5 * day, 5, 5, 0));
    }

    @Test
    void armorPricesAndWealthyShop() {
        assertEquals(12, InhabitantRules.armorEmeralds(false, "boots"));
        assertEquals(15, InhabitantRules.armorEmeralds(false, "helmet"));
        assertEquals(21, InhabitantRules.armorEmeralds(false, "leggings"));
        assertEquals(24, InhabitantRules.armorEmeralds(false, "chestplate"));
        assertEquals(48, InhabitantRules.armorEmeralds(true, "boots"));
        assertEquals(60, InhabitantRules.armorEmeralds(true, "helmet"));
        assertEquals(84, InhabitantRules.armorEmeralds(true, "leggings"));
        assertEquals(96, InhabitantRules.armorEmeralds(true, "chestplate"));
        assertEquals(1, InhabitantRules.ARMOR_TRADE_USES);
        assertEquals(6, InhabitantRules.enchantedArmorEmeralds(false, "boots"));
        assertEquals(8, InhabitantRules.enchantedArmorEmeralds(false, "helmet"));
        assertEquals(11, InhabitantRules.enchantedArmorEmeralds(false, "leggings"));
        assertEquals(12, InhabitantRules.enchantedArmorEmeralds(false, "chestplate"));
        assertEquals(12, InhabitantRules.enchantedArmorEmeralds(true, "boots"));
        assertEquals(15, InhabitantRules.enchantedArmorEmeralds(true, "helmet"));
        assertEquals(21, InhabitantRules.enchantedArmorEmeralds(true, "leggings"));
        assertEquals(24, InhabitantRules.enchantedArmorEmeralds(true, "chestplate"));
        assertEquals(20, InhabitantRules.armorEnchantLevel(false));
        assertEquals(30, InhabitantRules.armorEnchantLevel(true));
        List<InhabitantRules.WealthyListing> first = InhabitantRules.pickWealthyListings(1);
        List<InhabitantRules.WealthyListing> same = InhabitantRules.pickWealthyListings(1);
        List<InhabitantRules.WealthyListing> other = InhabitantRules.pickWealthyListings(99);
        assertEquals(8, first.size());
        assertEquals(first, same);
        assertEquals(8, other.size());
        long unique = first.stream().map(InhabitantRules.WealthyListing::itemId).distinct().count();
        assertEquals(8, unique);
        assertTrue(InhabitantRules.WEALTHY_POOL.size() >= 8);
    }

    @Test
    void armorsmithSellsOneHeavyPieceAndEnchantedArmor() {
        assertArmorShop(false);
        assertArmorShop(true);
    }

    private static void assertArmorShop(boolean master) {
        String specialty = master ? InhabitantRules.MASTER_ARMORSMITH : InhabitantRules.ARMORSMITH;
        String heavyPrefix = master ? "tougher:heavy_diamond_" : "tougher:heavy_iron_";
        String plainPrefix = master ? "minecraft:diamond_" : "minecraft:iron_";
        String metal = master ? "minecraft:diamond" : "minecraft:iron_ingot";
        Set<String> slots = Set.of("boots", "helmet", "leggings", "chestplate");
        Set<String> heavySeen = new HashSet<>();
        boolean sawBuy = false;
        boolean sawEveryEnchantedSlot = false;
        for (int seed = 0; seed < 80; seed++) {
            List<InhabitantRules.TradeListing> modest =
                    InhabitantRules.scaledListings(specialty, 24, 0, false, new Random(seed * 1000003L + 104729L));
            int heavy = 0;
            Set<String> enchanted = new HashSet<>();
            for (InhabitantRules.TradeListing listing : modest) {
                if (listing.itemId().startsWith(heavyPrefix)) {
                    heavy++;
                    String slot = listing.itemId().substring(heavyPrefix.length());
                    assertTrue(slots.contains(slot), listing.itemId());
                    assertFalse(listing.buy());
                    assertEquals(0, listing.enchantLevel());
                    assertEquals(1, listing.count());
                    assertEquals(InhabitantRules.ARMOR_TRADE_USES, listing.maxUses());
                    assertEquals(InhabitantRules.armorEmeralds(master, slot), listing.emeralds());
                    heavySeen.add(slot);
                } else if (listing.itemId().startsWith(plainPrefix)) {
                    String slot = listing.itemId().substring(plainPrefix.length());
                    assertTrue(slots.contains(slot), listing.itemId());
                    assertFalse(listing.buy());
                    assertEquals(1, listing.count());
                    assertEquals(InhabitantRules.armorEnchantLevel(master), listing.enchantLevel());
                    assertEquals(InhabitantRules.enchantedArmorEmeralds(master, slot), listing.emeralds());
                    assertTrue(enchanted.add(slot), listing.itemId());
                } else {
                    fail("unexpected listing " + listing.itemId());
                }
            }
            assertEquals(1, heavy);
            assertTrue(enchanted.size() >= 2);
            assertTrue(modest.size() <= 5);
        }
        for (int seed = 0; seed < 120; seed++) {
            List<InhabitantRules.TradeListing> rich =
                    InhabitantRules.scaledListings(specialty, 38, 0, false, new Random(seed * 1000003L + 104729L));
            int heavy = 0;
            Set<String> enchanted = new HashSet<>();
            boolean buy = false;
            Set<String> ids = new HashSet<>();
            for (InhabitantRules.TradeListing listing : rich) {
                assertTrue(ids.add(listing.itemId()), listing.itemId());
                if (listing.itemId().startsWith(heavyPrefix)) {
                    heavy++;
                    String slot = listing.itemId().substring(heavyPrefix.length());
                    assertTrue(slots.contains(slot), listing.itemId());
                    assertFalse(listing.buy());
                    assertEquals(0, listing.enchantLevel());
                    assertEquals(InhabitantRules.armorEmeralds(master, slot), listing.emeralds());
                    heavySeen.add(slot);
                } else if (metal.equals(listing.itemId())) {
                    assertTrue(listing.buy());
                    assertEquals(1, listing.emeralds());
                    assertEquals(0, listing.enchantLevel());
                    buy = true;
                } else if (listing.itemId().startsWith(plainPrefix)) {
                    String slot = listing.itemId().substring(plainPrefix.length());
                    assertTrue(slots.contains(slot), listing.itemId());
                    assertFalse(listing.buy());
                    assertEquals(InhabitantRules.armorEnchantLevel(master), listing.enchantLevel());
                    assertEquals(InhabitantRules.enchantedArmorEmeralds(master, slot), listing.emeralds());
                    assertTrue(enchanted.add(slot), listing.itemId());
                } else {
                    fail("unexpected listing " + listing.itemId());
                }
            }
            assertEquals(1, heavy);
            assertTrue(rich.size() <= 6);
            if (buy) {
                sawBuy = true;
            }
            if (enchanted.size() == 4) {
                sawEveryEnchantedSlot = true;
            }
        }
        assertEquals(slots, heavySeen);
        assertTrue(sawBuy);
        assertTrue(sawEveryEnchantedSlot);
    }

    @Test
    void brewerSellsOneLevelTwoPotionThenCheapIngredients() {
        assertEquals(14, InhabitantRules.restockDays(InhabitantRules.BREWER));
        Set<String> levelTwo = new HashSet<>();
        for (InhabitantRules.BrewPotion potion : InhabitantRules.BREWER_LEVEL_TWO) {
            assertTrue(levelTwo.add(potion.potionId()), potion.potionId());
            assertEquals(potion.family(), InhabitantRules.potionFamily(potion.potionId()));
        }
        Set<String> others = new HashSet<>();
        for (InhabitantRules.BrewPotion potion : InhabitantRules.BREWER_OTHER_POTIONS) {
            assertTrue(others.add(potion.potionId()), potion.potionId());
            assertFalse(levelTwo.contains(potion.potionId()));
            assertEquals(potion.family(), InhabitantRules.potionFamily(potion.potionId()));
        }
        Map<String, Integer> ingredients = new HashMap<>();
        for (InhabitantRules.TradeListing listing : InhabitantRules.brewerIngredientListings()) {
            assertFalse(listing.buy());
            assertEquals(InhabitantRules.BREWER_INGREDIENT_EMERALDS, listing.emeralds());
            assertEquals(InhabitantRules.BREWER_INGREDIENT_USES, listing.maxUses());
            assertTrue(listing.potionId().isEmpty());
            assertEquals(null, ingredients.put(listing.itemId(), listing.count()));
        }
        assertEquals(4, ingredients.get("minecraft:nether_wart").intValue());
        assertEquals(2, ingredients.get("minecraft:blaze_powder").intValue());
        assertEquals(4, ingredients.get("minecraft:glowstone_dust").intValue());
        assertEquals(1, ingredients.get("minecraft:ghast_tear").intValue());
        assertEquals(1, ingredients.get("minecraft:rabbit_foot").intValue());
        assertEquals(1, ingredients.get("minecraft:dragon_breath").intValue());
        assertEquals(18, ingredients.size());
        Set<String> usualFamilies = new HashSet<>();
        for (InhabitantRules.TradeListing listing : InhabitantRules.brewerUsualListings()) {
            assertEquals(1, listing.emeralds());
            assertTrue(usualFamilies.add(InhabitantRules.potionFamily(listing.potionId())));
        }
        assertEquals(4, usualFamilies.size());
        Set<String> featured = new HashSet<>();
        for (int seed = 0; seed < 80; seed++) {
            List<InhabitantRules.TradeListing> modest = InhabitantRules.scaledListings(
                    InhabitantRules.BREWER, 12, 0, false, new Random(seed * 1000003L + 104729L));
            assertTrue(modest.size() >= 1 && modest.size() <= 2, modest.toString());
            assertBrewerShop(modest, levelTwo, others, ingredients.keySet(), true);
            featured.add(modest.get(0).potionId());
        }
        boolean sawIngredient = false;
        for (int seed = 0; seed < 80; seed++) {
            List<InhabitantRules.TradeListing> rich = InhabitantRules.scaledListings(
                    InhabitantRules.BREWER, 38, 0, false, new Random(seed * 1000003L + 104729L));
            assertTrue(rich.size() >= 4 && rich.size() <= 7, rich.toString());
            assertBrewerShop(rich, levelTwo, others, ingredients.keySet(), false);
            featured.add(rich.get(0).potionId());
            if (rich.size() > 4) {
                sawIngredient = true;
            }
        }
        assertTrue(sawIngredient);
        assertEquals(levelTwo, featured);
    }

    private static void assertBrewerShop(
            List<InhabitantRules.TradeListing> offers,
            Set<String> levelTwo,
            Set<String> others,
            Set<String> ingredients,
            boolean modest) {
        Set<String> families = new HashSet<>();
        int levelCount = 0;
        int otherCount = 0;
        for (int i = 0; i < offers.size(); i++) {
            InhabitantRules.TradeListing listing = offers.get(i);
            assertFalse(listing.buy());
            assertEquals(1, listing.emeralds());
            assertEquals(0, listing.enchantLevel());
            if (!listing.potionId().isEmpty()) {
                assertEquals("minecraft:potion", listing.itemId());
                assertTrue(i < 4, listing.potionId());
                String family = InhabitantRules.potionFamily(listing.potionId());
                assertFalse(family.isEmpty(), listing.potionId());
                assertTrue(families.add(family), listing.potionId());
                if (levelTwo.contains(listing.potionId())) {
                    levelCount++;
                    assertEquals(0, i);
                } else {
                    assertTrue(others.contains(listing.potionId()), listing.potionId());
                    otherCount++;
                }
                if (modest) {
                    assertEquals(1, listing.count());
                }
            } else {
                assertTrue(i >= 4, listing.itemId());
                assertTrue(ingredients.contains(listing.itemId()), listing.itemId());
            }
        }
        assertEquals(1, levelCount);
        if (modest) {
            assertTrue(otherCount <= 1);
            assertEquals(offers.size(), levelCount + otherCount);
        } else {
            assertEquals(3, otherCount);
        }
    }

    @Test
    void jewelerSellsGemsAndMetalBlocks() {
        Map<String, InhabitantRules.TradeListing> stock = new HashMap<>();
        for (InhabitantRules.TradeListing listing : InhabitantRules.JEWELER_USUAL_SELLS) {
            assertFalse(listing.buy());
            assertEquals(InhabitantRules.WEALTHY_TRADE_USES, listing.maxUses());
            assertEquals(null, stock.put(listing.itemId(), listing));
        }
        for (InhabitantRules.TradeListing listing : InhabitantRules.JEWELER_EXTRA_SELLS) {
            assertFalse(listing.buy());
            assertEquals(null, stock.put(listing.itemId(), listing));
        }
        assertEquals(5, InhabitantRules.JEWELER_USUAL_SELLS.size());
        assertEquals(1, stock.get("minecraft:diamond").count());
        assertEquals(InhabitantRules.JEWELER_DIAMOND_EMERALDS, stock.get("minecraft:diamond").emeralds());
        assertEquals(InhabitantRules.JEWELER_SMALL_BLOCK_EMERALDS, stock.get("minecraft:redstone_block").emeralds());
        assertEquals(InhabitantRules.JEWELER_GOLD_INGOT_COUNT, stock.get("minecraft:gold_ingot").count());
        assertEquals(InhabitantRules.JEWELER_GOLD_EMERALDS, stock.get("minecraft:gold_ingot").emeralds());
        assertEquals(InhabitantRules.JEWELER_IRON_BLOCK_EMERALDS, stock.get("minecraft:iron_block").emeralds());
        assertEquals(InhabitantRules.JEWELER_SMALL_BLOCK_EMERALDS, stock.get("minecraft:copper_block").emeralds());
        assertEquals(InhabitantRules.JEWELER_DIAMOND_BLOCK_EMERALDS, stock.get("minecraft:diamond_block").emeralds());
        assertEquals(InhabitantRules.JEWELER_GOLD_BLOCK_EMERALDS, stock.get("minecraft:gold_block").emeralds());
        assertEquals(14, InhabitantRules.restockDays(InhabitantRules.JEWELER));
        boolean sawExtra = false;
        for (int seed = 0; seed < 40; seed++) {
            List<InhabitantRules.TradeListing> offers =
                    InhabitantRules.scaledListings(InhabitantRules.JEWELER, 38, 0, false, new Random(seed));
            assertFalse(offers.isEmpty());
            for (InhabitantRules.TradeListing listing : offers) {
                InhabitantRules.TradeListing priced = stock.get(listing.itemId());
                if (priced == null) {
                    continue;
                }
                assertEquals(priced.emeralds(), listing.emeralds());
                if (InhabitantRules.JEWELER_EXTRA_SELLS.stream()
                        .anyMatch(extra -> extra.itemId().equals(listing.itemId()))) {
                    sawExtra = true;
                }
            }
        }
        assertTrue(sawExtra);
    }

    @Test
    void farmNeighborMaySellSeedsAndVegetables() {
        assertEquals(3, InhabitantRules.defaultTradeTypeCount(InhabitantRules.FARM));
        assertTrue(InhabitantRules.FARM_PLANT_SELLS.size() >= 8);
        int cheapest = Integer.MAX_VALUE;
        int dearest = 0;
        Set<String> ids = new HashSet<>();
        for (InhabitantRules.TradeListing listing : InhabitantRules.FARM_PLANT_SELLS) {
            assertFalse(listing.buy());
            assertEquals(1, listing.count());
            assertTrue(listing.emeralds() >= InhabitantRules.FARM_PLANT_MIN_EMERALDS);
            assertTrue(listing.emeralds() <= InhabitantRules.FARM_PLANT_MAX_EMERALDS);
            assertTrue(ids.add(listing.itemId()));
            cheapest = Math.min(cheapest, listing.emeralds());
            dearest = Math.max(dearest, listing.emeralds());
        }
        assertEquals(InhabitantRules.FARM_PLANT_MIN_EMERALDS, cheapest);
        assertEquals(InhabitantRules.FARM_PLANT_MAX_EMERALDS, dearest);
        for (String id : List.of(
                "minecraft:wheat_seeds",
                "minecraft:beetroot_seeds",
                "minecraft:pumpkin_seeds",
                "minecraft:melon_seeds",
                "minecraft:torchflower_seeds",
                "minecraft:pitcher_pod",
                "minecraft:carrot",
                "minecraft:potato",
                "minecraft:nether_wart",
                "minecraft:cocoa_beans",
                "minecraft:sweet_berries",
                "minecraft:glow_berries",
                "minecraft:sugar_cane")) {
            assertTrue(ids.contains(id), id);
        }
        Map<String, Integer> prices = new HashMap<>();
        for (InhabitantRules.TradeListing listing : InhabitantRules.FARM_PLANT_SELLS) {
            prices.put(listing.itemId(), listing.emeralds());
        }
        Set<String> seen = new HashSet<>();
        boolean sawPlantOnSmallFarm = false;
        for (int seed = 0; seed < 200; seed++) {
            List<InhabitantRules.TradeListing> offers =
                    InhabitantRules.scaledListings(InhabitantRules.FARM, 12, 0, false, new Random(seed));
            assertFalse(offers.isEmpty());
            assertTrue(offers.size() <= 2);
            Set<String> offerIds = new HashSet<>();
            for (InhabitantRules.TradeListing listing : offers) {
                assertTrue(offerIds.add(listing.itemId()));
                if (prices.containsKey(listing.itemId())) {
                    assertFalse(listing.buy());
                    assertEquals(1, listing.count());
                    assertEquals(prices.get(listing.itemId()).intValue(), listing.emeralds());
                    seen.add(listing.itemId());
                    sawPlantOnSmallFarm = true;
                }
            }
        }
        assertTrue(sawPlantOnSmallFarm);
        assertEquals(ids, seen);
        boolean sawThirdPlant = false;
        for (int seed = 0; seed < 80; seed++) {
            List<InhabitantRules.TradeListing> rich =
                    InhabitantRules.scaledListings(InhabitantRules.FARM, 38, 0, false, new Random(seed));
            assertTrue(rich.size() >= 3);
            int plants = 0;
            for (InhabitantRules.TradeListing listing : rich) {
                if (prices.containsKey(listing.itemId())) {
                    plants++;
                    assertEquals(prices.get(listing.itemId()).intValue(), listing.emeralds());
                }
            }
            if (plants >= 3) {
                sawThirdPlant = true;
            }
        }
        assertTrue(sawThirdPlant);
    }

    @Test
    void shopStockScalesWithHouseScore() {
        assertEquals(5, InhabitantRules.defaultTradeTypeCount(InhabitantRules.HAULER));
        assertEquals(5, InhabitantRules.defaultTradeTypeCount(InhabitantRules.COOK));
        assertEquals(3, InhabitantRules.defaultTradeTypeCount(InhabitantRules.FARM));
        assertEquals(3, InhabitantRules.defaultTradeTypeCount(InhabitantRules.BOUNTY));
        assertEquals(4, InhabitantRules.defaultTradeTypeCount(InhabitantRules.TRADER));
        assertEquals(5, InhabitantRules.defaultTradeTypeCount(InhabitantRules.ARMORSMITH));
        assertEquals(5, InhabitantRules.defaultTradeTypeCount(InhabitantRules.MASTER_ARMORSMITH));
        assertEquals(8, InhabitantRules.defaultTradeTypeCount(InhabitantRules.WEALTHY));
        assertEquals(5, InhabitantRules.defaultTradeTypeCount(InhabitantRules.JEWELER));
        assertEquals(4, InhabitantRules.defaultTradeTypeCount(InhabitantRules.BREWER));
        assertEquals(0, InhabitantRules.extraTradePoints(12, InhabitantRules.HAULER));
        assertEquals(38, InhabitantRules.MAX_HOUSE_SCORE);
        assertEquals(50, InhabitantRules.tradeMaxPercent(12));
        assertEquals(25, InhabitantRules.tradeMinPercent(12));
        assertEquals(50, InhabitantRules.tradeMaxPercent(14));
        assertEquals(25, InhabitantRules.tradeMinPercent(14));
        assertEquals(71, InhabitantRules.tradeMaxPercent(18));
        assertEquals(36, InhabitantRules.tradeMinPercent(18));
        assertEquals(102, InhabitantRules.tradeMaxPercent(24));
        assertEquals(51, InhabitantRules.tradeMinPercent(24));
        assertEquals(133, InhabitantRules.tradeMaxPercent(30));
        assertEquals(67, InhabitantRules.tradeMinPercent(30));
        assertEquals(175, InhabitantRules.tradeMaxPercent(38));
        assertEquals(88, InhabitantRules.tradeMinPercent(38));
        assertEquals(175, InhabitantRules.tradeMaxPercent(40));
        assertEquals(0.0, InhabitantRules.tradeExperienceBonus(0), 0.0);
        assertEquals(0.0, InhabitantRules.tradeExperienceBonus(-4), 0.0);
        assertEquals(1.0, InhabitantRules.tradeExperienceBonus(1), 0.0);
        assertEquals(2.0, InhabitantRules.tradeExperienceBonus(4), 0.0);
        assertEquals(14.0, InhabitantRules.stockScore(14, 0), 0.0);
        assertEquals(16.0, InhabitantRules.stockScore(14, 4), 0.0);
        assertEquals(55, InhabitantRules.tradeMaxPercent(InhabitantRules.stockScore(14, 1)));
        assertEquals(175, InhabitantRules.tradeMaxPercent(InhabitantRules.stockScore(38, 100)));
        assertEquals(0, InhabitantRules.quantityModifier(14));
        assertEquals(0, InhabitantRules.quantityModifier(0));
        assertEquals(25, InhabitantRules.quantityModifier(38));
        assertEquals(25, InhabitantRules.quantityModifier(90));
        assertEquals(13, InhabitantRules.quantityModifier(26));
        assertEquals(1, InhabitantRules.quantityModifier(InhabitantRules.stockScore(14, 1)));
        assertEquals(-25, InhabitantRules.rollQuantitySwing(0));
        assertEquals(25, InhabitantRules.rollQuantitySwing(50));
        assertEquals(-25, InhabitantRules.quantityDealPercent(-25, 0));
        assertEquals(50, InhabitantRules.quantityDealPercent(25, 25));
        assertEquals(0, InhabitantRules.quantityDealPercent(-25, 25));
        assertEquals(8, InhabitantRules.adjustedQuantity(16, 50, true));
        assertEquals(12, InhabitantRules.adjustedQuantity(8, 50, false));
        assertEquals(20, InhabitantRules.adjustedQuantity(16, -25, true));
        assertEquals(6, InhabitantRules.adjustedQuantity(8, -25, false));
        assertEquals(1, InhabitantRules.adjustedQuantity(1, 25, false));
        assertEquals(2, InhabitantRules.adjustedQuantity(1, 50, false));
        assertEquals(1, InhabitantRules.adjustedQuantity(1, 50, true));
        assertEquals(1, InhabitantRules.scaledAmount(5, 25));
        assertEquals(3, InhabitantRules.scaledAmount(5, 50));
        assertEquals(1, InhabitantRules.scaledAmount(1, 25));
        assertEquals(8, InhabitantRules.scaledAmount(8, 100));
        assertEquals(10, InhabitantRules.scaledAmount(8, 125));
        assertEquals(1, InhabitantRules.scaledAmount(4, 25));
        assertEquals(25, InhabitantRules.rollTradePercent(25, 50, 0));
        assertEquals(50, InhabitantRules.rollTradePercent(25, 50, 25));
        Set<String> haulerDefaults = new HashSet<>();
        for (InhabitantRules.TradeListing listing : InhabitantRules.defaultListings(InhabitantRules.HAULER, 1, false)) {
            haulerDefaults.add(listing.itemId());
        }
        assertEquals(5, haulerDefaults.size());
        for (int seed = 0; seed < 80; seed++) {
            List<InhabitantRules.TradeListing> poor =
                    InhabitantRules.scaledListings(InhabitantRules.HAULER, 12, 1, false, new Random(seed));
            assertFalse(poor.isEmpty());
            assertTrue(poor.size() <= 3);
            Set<String> ids = new HashSet<>();
            for (InhabitantRules.TradeListing listing : poor) {
                assertTrue(ids.add(listing.itemId()));
                assertTrue(haulerDefaults.contains(listing.itemId()));
                assertTrue(listing.maxUses() >= 1);
                assertTrue(listing.maxUses() <= 2);
            }
        }
        boolean sawExtra = false;
        boolean sawSplitUses = false;
        for (int seed = 0; seed < 80; seed++) {
            List<InhabitantRules.TradeListing> rich =
                    InhabitantRules.scaledListings(InhabitantRules.HAULER, 38, 1, false, new Random(seed));
            assertTrue(rich.size() >= 4);
            assertTrue(rich.size() <= 9);
            Set<String> ids = new HashSet<>();
            int firstUses = rich.getFirst().maxUses();
            for (InhabitantRules.TradeListing listing : rich) {
                assertTrue(ids.add(listing.itemId()));
                if (!haulerDefaults.contains(listing.itemId())) {
                    sawExtra = true;
                }
                if (listing.maxUses() != firstUses) {
                    sawSplitUses = true;
                }
            }
            assertEquals(rich.size(), ids.size());
        }
        assertTrue(sawExtra);
        assertTrue(sawSplitUses);
        for (int seed = 0; seed < 40; seed++) {
            List<InhabitantRules.TradeListing> wealthy =
                    InhabitantRules.scaledListings(InhabitantRules.WEALTHY, 18, 0, false, new Random(seed));
            assertTrue(wealthy.size() >= 3);
            assertTrue(wealthy.size() <= 6);
        }
        for (int seed = 0; seed < 40; seed++) {
            List<InhabitantRules.TradeListing> palaceWealthy =
                    InhabitantRules.scaledListings(InhabitantRules.WEALTHY, 38, 0, false, new Random(seed));
            assertTrue(palaceWealthy.size() >= 7);
            assertTrue(palaceWealthy.size() <= 14);
        }
    }

    @Test
    void dawnWaitsForASurvivalPlayer() {
        assertFalse(InhabitantRules.shouldAttemptDawn(5, 5, true));
        assertFalse(InhabitantRules.shouldAttemptDawn(4, 5, false));
        assertTrue(InhabitantRules.shouldAttemptDawn(4, 5, true));
        assertTrue(InhabitantRules.shouldAttemptDawn(-1, 0, true));
    }

    @Test
    void villagerNeedsTwoHighFloorSpace() {
        assertTrue(InhabitantRules.villagerFits(true, true, true));
        assertFalse(InhabitantRules.villagerFits(true, false, true));
        assertFalse(InhabitantRules.villagerFits(false, true, true));
        assertFalse(InhabitantRules.villagerFits(true, true, false));
        assertEquals(0, InhabitantRules.STAND_OFFSETS[0][1]);
        int[] last = InhabitantRules.STAND_OFFSETS[InhabitantRules.STAND_OFFSETS.length - 1];
        assertEquals(0, last[0]);
        assertEquals(1, last[1]);
        assertEquals(0, last[2]);
    }
}
