package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
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
        assertTrue(InhabitantRules.volumeOk(400));
        assertFalse(InhabitantRules.volumeOk(401));
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
        assertEquals(0, pass.isolation());
        InhabitantRules.GateResult distant = InhabitantRules.gates(true, 40, true, true, true, true, true, ok, 0, 2);
        assertEquals(pass.score() + 2, distant.score());
        assertEquals(2, distant.isolation());
        assertTrue(InhabitantRules.inspectFallback(distant).contains("Distance adds 2."));
        InhabitantRules.GateResult nearer = InhabitantRules.gates(true, 40, true, true, true, true, true, ok, 0, 1);
        assertEquals(1, nearer.isolation());
        assertTrue(InhabitantRules.inspectFallback(nearer).contains("Distance adds 1."));
        assertTrue(InhabitantRules.inspectFallback(open).contains("Missing"));
    }

    @Test
    void spacingAndDawnChance() {
        assertEquals(20, InhabitantRules.SPACING);
        assertFalse(InhabitantRules.farEnough(19, 0, InhabitantRules.SPACING));
        assertTrue(InhabitantRules.farEnough(20, 0, InhabitantRules.SPACING));
        assertTrue(InhabitantRules.farEnough(0, 20, InhabitantRules.SPACING));
        assertFalse(InhabitantRules.farEnough(47, 0, 48));
        assertTrue(InhabitantRules.farEnough(48, 0, 48));
        assertTrue(InhabitantRules.farEnough(0, 48, 48));
        assertEquals(0, InhabitantRules.isolationPoints(50L * 50L));
        assertEquals(1, InhabitantRules.isolationPoints(50L * 50L + 1L));
        assertEquals(1, InhabitantRules.isolationPoints(100L * 100L));
        assertEquals(2, InhabitantRules.isolationPoints(100L * 100L + 1L));
        assertEquals(2, InhabitantRules.isolationPoints(Long.MAX_VALUE));
        assertEquals(0, InhabitantRules.isolationPoints(0L));
        assertEquals(1, InhabitantRules.isolationPoints(InhabitantRules.horizontalDistanceSquared(36, 36)));
        assertEquals(35, InhabitantRules.VILLAGER_CLEARANCE);
        assertFalse(InhabitantRules.clearOfVillager(34, 0, 0, 35));
        assertTrue(InhabitantRules.clearOfVillager(35, 0, 0, 35));
        assertFalse(InhabitantRules.clearOfVillager(0, -34, 0, 35));
        assertTrue(InhabitantRules.clearOfVillager(0, 0, 35, 35));
        assertFalse(InhabitantRules.clearOfVillager(20, 20, 20, 35));
        assertTrue(InhabitantRules.clearOfVillager(21, 21, 21, 35));
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
        assertEquals(32, InhabitantRules.haulerStack(0));
        assertEquals(32, InhabitantRules.haulerStack(-3));
        assertEquals(30, InhabitantRules.haulerStack(1));
        assertEquals(28, InhabitantRules.haulerStack(2));
        assertEquals(16, InhabitantRules.haulerStack(8));
        assertEquals(16, InhabitantRules.haulerStack(9));
        assertEquals(16, InhabitantRules.haulerStack(20));
        for (int storage : new int[] {0, 1, 8, 20}) {
            int stack = InhabitantRules.haulerStack(storage);
            for (InhabitantRules.TradeListing listing :
                    InhabitantRules.defaultListings(InhabitantRules.HAULER, storage, false)) {
                assertEquals(stack, listing.count(), listing.itemId());
                assertTrue(listing.buy());
                assertEquals(1, listing.emeralds());
            }
            for (InhabitantRules.TradeListing listing :
                    InhabitantRules.extraListings(InhabitantRules.HAULER, storage, false)) {
                assertEquals(stack, listing.count(), listing.itemId());
                assertTrue(listing.buy());
                assertEquals(1, listing.emeralds());
            }
        }
        assertEquals(16, InhabitantRules.farmWheatBuy(false));
        assertEquals(8, InhabitantRules.farmWheatBuy(true));
        assertEquals("Alden", InhabitantRules.pickName(0));
        assertEquals(128, InhabitantRules.nameCount());
        assertEquals("Tobias", InhabitantRules.pickName(63));
        assertEquals("Albert", InhabitantRules.pickName(64));
        assertEquals("Luther", InhabitantRules.pickName(127));
        assertEquals(InhabitantRules.pickName(0), InhabitantRules.pickName(128));
        Set<String> residentNames = new HashSet<>();
        for (int roll = 0; roll < 256; roll++) {
            residentNames.add(InhabitantRules.pickName(roll));
        }
        assertEquals(128, residentNames.size());
        assertEquals("Bram", InhabitantRules.pickFreshName(List.of("Alden"), 0));
        List<String> almost = new ArrayList<>();
        for (int i = 0; i < InhabitantRules.nameCount() - 1; i++) {
            almost.add(InhabitantRules.pickName(i));
        }
        assertEquals(InhabitantRules.pickName(127), InhabitantRules.pickFreshName(almost, 0));
        assertEquals(InhabitantRules.pickName(127), InhabitantRules.pickFreshName(almost, 9));
        List<String> all = new ArrayList<>(almost);
        all.add(InhabitantRules.pickName(127));
        assertTrue(InhabitantRules.namesExhausted(all));
        assertFalse(InhabitantRules.namesExhausted(almost));
        assertEquals(InhabitantRules.pickName(3), InhabitantRules.pickFreshName(all, 3));
        assertEquals(List.of("Alden", "Bram"), InhabitantRules.namesInUse(List.of("Alden"), List.of("Bram", "Mira")));
        assertEquals(List.of("Alden", "Cedric"), InhabitantRules.nextNameCycle(List.of("Alden", "Mira"), "Cedric"));
        assertTrue(InhabitantRules.listedName("Alden"));
        assertFalse(InhabitantRules.listedName("Mira"));
        assertFalse(InhabitantRules.listedName("Alder"));
        assertFalse(InhabitantRules.listedName(""));
        assertFalse(InhabitantRules.listedName(null));
        assertEquals(InhabitantRules.nameForHome("bed-1"), InhabitantRules.nameForHome("bed-1"));
        assertEquals(InhabitantRules.pickName(0), InhabitantRules.nameForHome(null));
        assertTrue(InhabitantRules.listedName(InhabitantRules.nameForHome("bed-1")));
        assertEquals(
                List.of("hauler", "cook", "farm", "trader", "brewer", "old_miner"),
                InhabitantRules.unseenSpecialties(List.of(), 12));
        assertEquals(
                List.of("hauler", "cook", "farm", "bounty", "trader", "council", "brewer", "old_miner"),
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
        assertFalse(InhabitantRules.specialtyAllowed(InhabitantRules.WEALTHY, 29));
        assertTrue(InhabitantRules.specialtyAllowed(InhabitantRules.WEALTHY, 30));
        assertFalse(InhabitantRules.specialtyAllowed(InhabitantRules.JEWELER, 23));
        assertTrue(InhabitantRules.specialtyAllowed(InhabitantRules.JEWELER, 24));
        assertFalse(InhabitantRules.specialtyAllowed(InhabitantRules.COUNCIL, 17));
        assertTrue(InhabitantRules.specialtyAllowed(InhabitantRules.COUNCIL, 18));
        assertEquals(15, InhabitantRules.movingFee(12, 30));
        assertEquals(15, InhabitantRules.movingFee(30, 12));
        assertEquals(12, InhabitantRules.movingFee(24, 24));
        assertEquals(6, InhabitantRules.movingFee(13, 12));
        assertEquals(0, InhabitantRules.movingFee(0, 1));
        assertEquals(1, InhabitantRules.movingFee(-4, 3));
        assertEquals(0, InhabitantRules.movingFee(-8, -2));
        assertEquals(0, InhabitantRules.residentChunkSpread(0));
        assertEquals(2, InhabitantRules.residentLoadRadius(0));
        assertEquals(0, InhabitantRules.residentChunkSpread(-4));
        assertEquals(1, InhabitantRules.residentChunkSpread(16));
        assertEquals(3, InhabitantRules.residentLoadRadius(16));
        assertEquals(2, InhabitantRules.residentChunkSpread(InhabitantRules.SNAP_HOME_RANGE));
        assertEquals(4, InhabitantRules.residentLoadRadius(InhabitantRules.SNAP_HOME_RANGE));
        assertEquals(3, InhabitantRules.residentChunkSpread(33));
        assertEquals(5, InhabitantRules.residentLoadRadius(33));
        assertTrue(InhabitantRules.unseenSpecialties(List.of(), 24).contains(InhabitantRules.JEWELER));
        assertFalse(InhabitantRules.unseenSpecialties(List.of(), 24).contains(InhabitantRules.WEALTHY));
        assertTrue(InhabitantRules.unseenSpecialties(List.of(), 30).contains(InhabitantRules.WEALTHY));
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
        assertEquals("old miner", InhabitantRules.specialtyFallback(InhabitantRules.OLD_MINER));
        assertEquals("Jack the Old Miner", InhabitantRules.nameplate("Jack", InhabitantRules.OLD_MINER));
        assertEquals("Lumen the Brewer", InhabitantRules.nameplate("Lumen", InhabitantRules.BREWER));
        assertFalse(InhabitantRules.specialtyAllowed(InhabitantRules.OLD_MINER, 11));
        assertTrue(InhabitantRules.specialtyAllowed(InhabitantRules.OLD_MINER, 12));
        assertTrue(InhabitantRules.defaultListings(InhabitantRules.OLD_MINER, 0, false).isEmpty());
        assertTrue(InhabitantRules.extraListings(InhabitantRules.OLD_MINER, 0, false).isEmpty());
        assertFalse(InhabitantRules.specialtyAllowed(InhabitantRules.BREWER, 11));
        assertTrue(InhabitantRules.specialtyAllowed(InhabitantRules.BREWER, 12));
        InhabitantRules.Outfit brewerOutfit = InhabitantRules.pickOutfit(InhabitantRules.BREWER, 0, 0, 0);
        assertEquals(1, brewerOutfit.level());
        assertTrue(InhabitantRules.COMMON_OUTFITS.contains(brewerOutfit.professionId()));
        assertEquals("loot collector", InhabitantRules.specialtyFallback(InhabitantRules.BOUNTY));
        assertEquals("Nia the Loot Collector", InhabitantRules.nameplate("Nia", InhabitantRules.BOUNTY));
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
        assertEquals(12, InhabitantRules.wiseFocusUses(24));
        assertEquals(12, InhabitantRules.wiseFocusUses(25));
        assertEquals(0, InhabitantRules.wiseFocusUses(1));
        assertEquals(0, InhabitantRules.wiseFocusUses(0));
        assertEquals(0, InhabitantRules.wiseFocusUses(-3));
        assertEquals(24, InhabitantRules.wiseEmeraldUses(24));
        assertEquals(25, InhabitantRules.wiseEmeraldUses(25));
        assertEquals(38, InhabitantRules.wiseEmeraldUses(38));
        assertEquals(1, InhabitantRules.wiseEmeraldUses(1));
        assertEquals(0, InhabitantRules.wiseEmeraldUses(0));
        assertEquals(0, InhabitantRules.wiseEmeraldUses(-4));
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
        assertTrue(InhabitantRules.canTeachFocus(1, 30, 30));
        assertFalse(InhabitantRules.canTeachFocus(0, 30, 30));
        assertFalse(InhabitantRules.canTeachFocus(1, 29, 30));
        assertTrue(InhabitantRules.canTeachFocus(1, 0, 0));
        assertEquals(
                "Hold 8 emeralds, for me to teach you a random ability; an ability focus for that ability (1/2 as effective); or 9 redstone blocks for me to deepen your mana learning capacity.",
                InhabitantRules.handRequest(InhabitantRules.wiseAbilityPrice(35), InhabitantRules.redstoneBlockCap(2)));
        assertEquals(
                "Hold 1 emerald, for me to teach you a random ability; an ability focus for that ability (1/2 as effective); or 1 redstone block for me to deepen your mana learning capacity.",
                InhabitantRules.handRequest(1, 1));
        assertEquals(
                "Hold 0 emeralds, for me to teach you a random ability; an ability focus for that ability (1/2 as effective); or 1 redstone block for me to deepen your mana learning capacity.",
                InhabitantRules.handRequest(InhabitantRules.wiseAbilityPrice(39), InhabitantRules.redstoneBlockCap(-4)));
        assertEquals(
                "Hold 8 emeralds, for me to teach you a random ability; an ability focus for that ability (1/2 as effective); or 9 redstone blocks for me to deepen your mana learning capacity.",
                InhabitantRules.handRequestLine(8, 9));
        assertEquals(
                "I take those emeralds from your inventory or either hand. You can use either or both.",
                InhabitantRules.emeraldPaymentLine());
        assertTrue(InhabitantRules.canGiveRedstone(1, 1));
        assertFalse(InhabitantRules.canGiveRedstone(0, 4));
        assertFalse(InhabitantRules.canGiveRedstone(1, 0));
        assertEquals(1, InhabitantRules.redstoneBlockCap(0));
        assertEquals(4, InhabitantRules.redstoneBlockCap(1));
        assertEquals(9, InhabitantRules.redstoneBlockCap(2));
        assertEquals(16, InhabitantRules.redstoneBlockCap(3));
        assertEquals(1, InhabitantRules.redstoneBlockCap(-4));
        assertEquals(1, InhabitantRules.redstoneBlocksTaken(0, 64));
        assertEquals(4, InhabitantRules.redstoneBlocksTaken(1, 64));
        assertEquals(3, InhabitantRules.redstoneBlocksTaken(1, 3));
        assertEquals(0, InhabitantRules.redstoneBlocksTaken(2, 0));
        assertEquals(1.0, InhabitantRules.capacityGain(1), 1e-9);
        assertEquals(2.0, InhabitantRules.capacityGain(4), 1e-9);
        assertEquals(Math.sqrt(2.0), InhabitantRules.capacityGain(2), 1e-9);
        assertEquals(0.0, InhabitantRules.capacityGain(0), 1e-9);
        assertEquals("1", InhabitantRules.learningLabel(1.0));
        assertEquals("1.4", InhabitantRules.learningLabel(Math.sqrt(2.0)));
        assertEquals("2", InhabitantRules.learningLabel(2.0));
        assertEquals("0", InhabitantRules.learningLabel(0.0));
        assertTrue(InhabitantRules.scaledListings(InhabitantRules.WISE_TEACHER, 30, 0, false, new java.util.Random(1))
                .isEmpty());
        assertTrue(InhabitantRules.scaledListings(InhabitantRules.OLD_MINER, 30, 4, false, new java.util.Random(1))
                .isEmpty());
        for (String ability : AbilityRules.ABILITY_IDS) {
            assertFalse(AbilityRules.teacherFlavor(ability).isEmpty());
            assertFalse(AbilityRules.teacherInstruction(ability).isEmpty());
        }
        assertFalse(InhabitantRules.specialtyAllowed(InhabitantRules.COUNCIL, 12));
        assertTrue(InhabitantRules.specialtyAllowed(InhabitantRules.COUNCIL, 18));
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
                    InhabitantRules.pickSpecialty(empty, 18, roll, InhabitantRules.SPECIALTIES))) {
                council++;
            }
        }
        assertTrue(council >= 42);
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
    void crowdedJobsAreSkippedAndRareJobsGetThreeTimes() {
        assertFalse(InhabitantRules.crowdedOut(InhabitantRules.TRADER, 4, 2));
        assertTrue(InhabitantRules.crowdedOut(InhabitantRules.TRADER, 5, 2));
        assertFalse(InhabitantRules.crowdedOut(InhabitantRules.WISE_TEACHER, 6, 2));
        assertTrue(InhabitantRules.crowdedOut(InhabitantRules.WISE_TEACHER, 7, 2));
        assertFalse(InhabitantRules.crowdedOut(InhabitantRules.COUNCIL, 6, 2));
        assertTrue(InhabitantRules.crowdedOut(InhabitantRules.COUNCIL, 7, 2));
        assertFalse(InhabitantRules.crowdedOut(InhabitantRules.TRADER, 0, 0));
        assertTrue(InhabitantRules.crowdedOut(InhabitantRules.COUNCIL, 1, 0));
        Map<String, Integer> crowdedTrader = Map.of(
                InhabitantRules.HAULER, 1,
                InhabitantRules.COOK, 1,
                InhabitantRules.FARM, 1,
                InhabitantRules.TRADER, 5,
                InhabitantRules.BREWER, 1,
                InhabitantRules.OLD_MINER, 1);
        assertEquals(1, InhabitantRules.lowestPossibleMembers(12, crowdedTrader));
        assertEquals(
                5,
                InhabitantRules.lowestPossibleMembers(
                        12,
                        Map.of(
                                InhabitantRules.HAULER, 5,
                                InhabitantRules.COOK, 5,
                                InhabitantRules.FARM, 5,
                                InhabitantRules.TRADER, 5,
                                InhabitantRules.BREWER, 5,
                                InhabitantRules.OLD_MINER, 5,
                                InhabitantRules.MASTER_ARMORSMITH, 0)));
        InhabitantRules.AmenityCounts empty =
                new InhabitantRules.AmenityCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, false);
        assertEquals(
                InhabitantRules.OLD_MINER,
                InhabitantRules.pickSpecialty(empty, 12, 0, InhabitantRules.SPECIALTIES, crowdedTrader));
        for (int roll = 0; roll < 20; roll++) {
            assertNotEquals(
                    InhabitantRules.TRADER,
                    InhabitantRules.pickSpecialty(empty, 12, roll, InhabitantRules.SPECIALTIES, crowdedTrader));
        }
        Map<String, Integer> twiceTrader = new HashMap<>(crowdedTrader);
        twiceTrader.put(InhabitantRules.TRADER, 2);
        twiceTrader.put(InhabitantRules.HAULER, 1);
        assertEquals(
                InhabitantRules.TRADER,
                InhabitantRules.pickSpecialty(empty, 12, 0, InhabitantRules.SPECIALTIES, twiceTrader));
        assertFalse(InhabitantRules.specialtyBag(empty, 18).contains(InhabitantRules.WEALTHY));
        assertFalse(InhabitantRules.specialtyBag(empty, 18).contains(InhabitantRules.JEWELER));
        assertTrue(InhabitantRules.specialtyBag(empty, 24).contains(InhabitantRules.JEWELER));
        assertFalse(InhabitantRules.specialtyBag(empty, 24).contains(InhabitantRules.WEALTHY));
        assertTrue(InhabitantRules.specialtyBag(empty, 30).contains(InhabitantRules.WEALTHY));
        assertTrue(InhabitantRules.specialtyBag(empty, 12).contains(InhabitantRules.OLD_MINER));
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
        assertEquals(3, InhabitantRules.DEFAULT_TRADE_USES);
        assertEquals(3, InhabitantRules.WEALTHY_TRADE_USES);
        assertEquals(3, InhabitantRules.FARM_PLANT_USES);
        assertEquals(3, InhabitantRules.ARMOR_METAL_BUY_USES);
        assertEquals(3, InhabitantRules.BREWER_POTION_USES);
        assertEquals(3, InhabitantRules.BREWER_INGREDIENT_USES);
        for (String specialty : InhabitantRules.SPECIALTIES) {
            for (InhabitantRules.TradeListing listing : InhabitantRules.defaultListings(specialty, 1, false)) {
                assertTrue(listing.maxUses() >= 1 && listing.maxUses() <= 3, specialty + " " + listing.itemId());
            }
            for (InhabitantRules.TradeListing listing : InhabitantRules.extraListings(specialty, 1, false)) {
                assertTrue(listing.maxUses() >= 1 && listing.maxUses() <= 3, specialty + " " + listing.itemId());
            }
        }
        for (InhabitantRules.TradeListing listing : InhabitantRules.genericExtraListings()) {
            assertTrue(listing.maxUses() >= 1 && listing.maxUses() <= 3, listing.itemId());
        }
        for (boolean master : new boolean[] {false, true}) {
            for (InhabitantRules.TradeListing listing : InhabitantRules.armorUsualListings(master)) {
                assertEquals(1, listing.maxUses());
            }
        }
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
        assertEquals(4, first.size());
        assertEquals(first, same);
        assertEquals(4, other.size());
        long unique = first.stream().map(InhabitantRules.WealthyListing::itemId).distinct().count();
        assertEquals(4, unique);
        assertTrue(InhabitantRules.WEALTHY_POOL.size() >= 8);
        Map<String, InhabitantRules.WealthyListing> wealthy = new HashMap<>();
        for (InhabitantRules.WealthyListing listing : InhabitantRules.WEALTHY_POOL) {
            assertEquals(null, wealthy.put(listing.itemId(), listing));
            if (listing.emeralds() >= 1 && listing.emeralds() <= 3) {
                assertTrue(listing.count() >= 1, listing.itemId());
            }
        }
        assertEquals(6, wealthy.get("minecraft:copper_ingot").count());
        assertEquals(6, wealthy.get("minecraft:lapis_lazuli").count());
        assertEquals(8, wealthy.get("minecraft:redstone").count());
        assertEquals(6, wealthy.get("minecraft:quartz").count());
        assertEquals(1, wealthy.get("minecraft:book").count());
        assertEquals(12, wealthy.get("minecraft:arrow").count());
        assertEquals(4, wealthy.get("minecraft:cooked_beef").count());
        assertEquals(8, wealthy.get("minecraft:oak_log").count());
        assertEquals(8, wealthy.get("minecraft:glass").count());
        assertEquals(6, wealthy.get("minecraft:leather").count());
        assertEquals(8, wealthy.get("minecraft:string").count());
        assertEquals(1, wealthy.get("minecraft:nether_wart").count());
        assertEquals(1, wealthy.get("minecraft:nether_wart").emeralds());
        assertEquals(4, wealthy.get("minecraft:amethyst_shard").count());
        assertEquals(8, wealthy.get("minecraft:coal").count());
        assertEquals(4, wealthy.get("minecraft:lantern").count());
        assertEquals(4, wealthy.get("minecraft:iron_ingot").count());
        assertEquals(2, wealthy.get("minecraft:gold_ingot").count());
        assertEquals(2, wealthy.get("minecraft:obsidian").count());
        assertEquals(4, wealthy.get("minecraft:glowstone").count());
        assertEquals(4, wealthy.get("minecraft:packed_ice").count());
        assertEquals(4, wealthy.get("minecraft:slime_ball").count());
        assertEquals(4, wealthy.get("minecraft:gunpowder").count());
        assertEquals(1, wealthy.get("minecraft:ender_pearl").count());
        assertEquals(1, wealthy.get("minecraft:blaze_rod").count());
        assertEquals(10, wealthy.get("minecraft:experience_bottle").count());
        assertEquals(5, wealthy.get("minecraft:experience_bottle").emeralds());
        assertEquals(1, wealthy.get("minecraft:diamond").count());
        assertEquals(1, wealthy.get("minecraft:iron_block").count());
        assertEquals(1, wealthy.get("minecraft:saddle").count());
        assertEquals(1, wealthy.get("minecraft:golden_apple").count());
        assertEquals(2, wealthy.get("minecraft:phantom_membrane").count());
        assertEquals(1, wealthy.get("minecraft:ghast_tear").count());
        assertEquals(1, wealthy.get("minecraft:name_tag").count());
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
            assertTrue(enchanted.size() >= 1);
            assertTrue(modest.size() <= 4);
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
            assertEquals(1, listing.count(), listing.itemId());
            assertEquals(null, ingredients.put(listing.itemId(), listing.count()));
        }
        assertEquals(1, ingredients.get("minecraft:nether_wart").intValue());
        assertEquals(1, ingredients.get("minecraft:blaze_powder").intValue());
        assertEquals(1, ingredients.get("minecraft:glowstone_dust").intValue());
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
        assertEquals(32, InhabitantRules.JEWELER_DIAMOND_BLOCK_EMERALDS);
        assertEquals(6, InhabitantRules.JEWELER_GOLD_BLOCK_EMERALDS);
        assertEquals(1, stock.get("minecraft:diamond_block").count());
        assertEquals(1, stock.get("minecraft:gold_block").count());
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
    void farmNeighborSellsFromALowerListAndALargerHomeList() {
        assertEquals(3, InhabitantRules.defaultTradeTypeCount(InhabitantRules.FARM));
        Map<String, InhabitantRules.TradeListing> lower = new HashMap<>();
        for (InhabitantRules.TradeListing listing : InhabitantRules.FARM_LOWER_SELLS) {
            assertFalse(listing.buy());
            assertEquals(null, lower.put(listing.itemId(), listing));
        }
        Map<String, InhabitantRules.TradeListing> higher = new HashMap<>();
        for (InhabitantRules.TradeListing listing : InhabitantRules.FARM_HIGHER_SELLS) {
            assertFalse(listing.buy());
            assertEquals(null, higher.put(listing.itemId(), listing));
            assertFalse(lower.containsKey(listing.itemId()));
        }
        assertFarmSell(lower, "minecraft:wheat_seeds", 3, 1);
        assertFarmSell(lower, "minecraft:beetroot_seeds", 3, 1);
        assertFarmSell(lower, "minecraft:carrot", 3, 1);
        assertFarmSell(lower, "minecraft:sweet_berries", 3, 1);
        assertFarmSell(lower, "minecraft:glow_berries", 3, 1);
        assertFarmSell(lower, "minecraft:sugar_cane", 3, 1);
        assertFarmSell(lower, "minecraft:torchflower_seeds", 3, 1);
        assertFarmSell(lower, "minecraft:pitcher_pod", 3, 1);
        assertFarmSell(lower, "minecraft:oak_sapling", 1, 2);
        assertFarmSell(lower, "minecraft:birch_sapling", 1, 2);
        assertFarmSell(lower, "minecraft:spruce_sapling", 1, 2);
        assertFarmSell(lower, "minecraft:bone_meal", 4, 1);
        assertFarmSell(lower, "minecraft:pumpkin", 1, 1);
        assertFarmSell(lower, "minecraft:melon", 1, 1);
        assertFarmSell(lower, "minecraft:beetroot", 12, 1);
        assertFarmSell(higher, "minecraft:cocoa_beans", 1, 1);
        assertFarmSell(higher, "minecraft:pumpkin_seeds", 1, 1);
        assertFarmSell(higher, "minecraft:melon_seeds", 1, 1);
        assertFarmSell(higher, "minecraft:potato", 1, 1);
        assertFarmSell(higher, "minecraft:nether_wart", 1, 1);
        InhabitantRules.TradeListing wheat = InhabitantRules.farmUsualListings().getFirst();
        assertEquals("minecraft:wheat", wheat.itemId());
        assertFalse(wheat.buy());
        assertEquals(8, wheat.count());
        assertEquals(1, wheat.emeralds());
        boolean sawBeetBuy = false;
        for (InhabitantRules.TradeListing listing : InhabitantRules.farmExtraListings(false)) {
            if ("minecraft:beetroot".equals(listing.itemId()) && listing.buy()) {
                assertEquals(16, listing.count());
                sawBeetBuy = true;
            }
            if (higher.containsKey(listing.itemId())) {
                assertEquals(higher.get(listing.itemId()).emeralds(), listing.emeralds());
            }
        }
        assertTrue(sawBeetBuy);
        assertEquals(8, InhabitantRules.farmSideListings(true).getFirst().count());
        Set<String> seenLower = new HashSet<>();
        for (int seed = 0; seed < 400; seed++) {
            List<InhabitantRules.TradeListing> offers =
                    InhabitantRules.scaledListings(InhabitantRules.FARM, 12, 0, false, new Random(seed));
            assertFalse(offers.isEmpty());
            assertTrue(offers.size() <= 2);
            for (InhabitantRules.TradeListing listing : offers) {
                assertFalse(higher.containsKey(listing.itemId()), listing.itemId());
                if ("minecraft:wheat".equals(listing.itemId())) {
                    assertFalse(listing.buy());
                    assertEquals(1, listing.emeralds());
                }
                InhabitantRules.TradeListing priced = lower.get(listing.itemId());
                if (priced != null) {
                    assertFalse(listing.buy());
                    assertEquals(priced.emeralds(), listing.emeralds());
                    seenLower.add(listing.itemId());
                }
            }
        }
        assertEquals(lower.keySet(), seenLower);
        boolean sawHigher = false;
        for (int seed = 0; seed < 200; seed++) {
            List<InhabitantRules.TradeListing> rich =
                    InhabitantRules.scaledListings(InhabitantRules.FARM, 38, 0, false, new Random(seed));
            assertTrue(rich.size() >= 3);
            for (InhabitantRules.TradeListing listing : rich) {
                InhabitantRules.TradeListing priced = higher.get(listing.itemId());
                if (priced != null) {
                    assertFalse(listing.buy());
                    assertEquals(priced.emeralds(), listing.emeralds());
                    sawHigher = true;
                }
            }
        }
        assertTrue(sawHigher);
    }

    private static void assertFarmSell(
            Map<String, InhabitantRules.TradeListing> listings, String itemId, int count, int emeralds) {
        InhabitantRules.TradeListing listing = listings.get(itemId);
        assertTrue(listing != null, itemId);
        assertEquals(count, listing.count(), itemId);
        assertEquals(emeralds, listing.emeralds(), itemId);
    }

    @Test
    void traderBuysBackWhatHeSellsTwoLargerWithNoUses() {
        assertEquals(6, InhabitantRules.traderBuyCount(4));
        assertEquals(2, InhabitantRules.traderBuyCount(0));
        List<InhabitantRules.TradeListing> catalog =
                InhabitantRules.defaultListings(InhabitantRules.TRADER, 0, false);
        assertEquals(4, catalog.size());
        boolean sawBread = false;
        for (InhabitantRules.TradeListing listing : catalog) {
            assertFalse(listing.buy());
            if ("minecraft:bread".equals(listing.itemId())) {
                assertEquals(4, listing.count());
                assertEquals(1, listing.emeralds());
                sawBread = true;
            }
        }
        assertTrue(sawBread);
        List<InhabitantRules.TradeListing> paired = InhabitantRules.traderBuybacks(catalog);
        assertEquals(catalog.size() * 2, paired.size());
        for (int i = 0; i < catalog.size(); i++) {
            InhabitantRules.TradeListing sell = paired.get(i);
            InhabitantRules.TradeListing buy = paired.get(catalog.size() + i);
            assertFalse(sell.buy());
            assertTrue(buy.buy());
            assertEquals(sell.itemId(), buy.itemId());
            assertEquals(sell.count() + InhabitantRules.TRADER_BUY_EXTRA, buy.count());
            assertEquals(sell.emeralds(), buy.emeralds());
            assertEquals(0, buy.maxUses());
        }
        for (int seed = 0; seed < 60; seed++) {
            assertTraderPairs(
                    InhabitantRules.scaledListings(InhabitantRules.TRADER, 12, 0, false, new Random(seed)),
                    true);
        }
        for (int seed = 0; seed < 40; seed++) {
            assertTraderPairs(
                    InhabitantRules.scaledListings(InhabitantRules.TRADER, 38, 0, false, new Random(seed)),
                    false);
        }
    }

    private static void assertTraderPairs(List<InhabitantRules.TradeListing> offers, boolean modest) {
        Map<String, InhabitantRules.TradeListing> sells = new HashMap<>();
        int paired = 0;
        int unpaired = 0;
        for (InhabitantRules.TradeListing listing : offers) {
            if (!listing.buy()) {
                assertEquals(null, sells.put(listing.itemId(), listing), listing.itemId());
                assertTrue(listing.maxUses() >= 1, listing.itemId());
            }
        }
        assertFalse(sells.isEmpty());
        for (InhabitantRules.TradeListing listing : offers) {
            if (!listing.buy()) {
                continue;
            }
            InhabitantRules.TradeListing sell = sells.get(listing.itemId());
            if (sell == null) {
                assertTrue(listing.maxUses() >= 1, listing.itemId());
                assertFalse(modest, listing.itemId());
                unpaired++;
                continue;
            }
            assertEquals(0, listing.maxUses(), listing.itemId());
            assertEquals(sell.count() + InhabitantRules.TRADER_BUY_EXTRA, listing.count(), listing.itemId());
            assertEquals(sell.emeralds(), listing.emeralds(), listing.itemId());
            paired++;
        }
        assertEquals(sells.size(), paired);
        if (modest) {
            assertEquals(0, unpaired);
            assertEquals(sells.size() * 2, offers.size());
            assertTrue(offers.size() <= 4);
        }
    }

    @Test
    void lootCollectorPaysOneEmeraldForHalfTheOldDropCounts() {
        Map<String, InhabitantRules.TradeListing> usual = new HashMap<>();
        for (InhabitantRules.TradeListing listing :
                InhabitantRules.defaultListings(InhabitantRules.BOUNTY, 1, false)) {
            assertEquals(null, usual.put(listing.itemId(), listing));
        }
        assertEquals(3, usual.size());
        assertEquals(4, usual.get("minecraft:spider_eye").count());
        assertEquals(4, usual.get("minecraft:gunpowder").count());
        assertEquals(8, usual.get("minecraft:bone").count());
        Map<String, InhabitantRules.TradeListing> extra = new HashMap<>();
        for (InhabitantRules.TradeListing listing :
                InhabitantRules.extraListings(InhabitantRules.BOUNTY, 1, false)) {
            assertEquals(null, extra.put(listing.itemId(), listing));
        }
        assertEquals(10, extra.size());
        assertEquals(8, extra.get("minecraft:rotten_flesh").count());
        assertEquals(8, extra.get("minecraft:string").count());
        assertEquals(4, extra.get("minecraft:slime_ball").count());
        assertEquals(6, extra.get("minecraft:leather").count());
        assertEquals(12, extra.get("minecraft:arrow").count());
        assertEquals(6, extra.get("minecraft:ink_sac").count());
        assertEquals(2, extra.get("minecraft:phantom_membrane").count());
        assertEquals(2, extra.get("minecraft:ender_pearl").count());
        assertEquals(2, extra.get("minecraft:blaze_rod").count());
        assertEquals(1, extra.get("minecraft:ghast_tear").count());
        for (InhabitantRules.TradeListing listing : usual.values()) {
            assertTrue(listing.buy());
            assertEquals(1, listing.emeralds());
            assertEquals(InhabitantRules.DEFAULT_TRADE_USES, listing.maxUses());
        }
        for (InhabitantRules.TradeListing listing : extra.values()) {
            assertTrue(listing.buy());
            assertEquals(1, listing.emeralds());
            assertEquals(InhabitantRules.DEFAULT_TRADE_USES, listing.maxUses());
        }
    }

    @Test
    void shopStockScalesWithHouseScore() {
        assertEquals(4, InhabitantRules.defaultTradeTypeCount(InhabitantRules.HAULER));
        assertEquals(4, InhabitantRules.defaultTradeTypeCount(InhabitantRules.COOK));
        assertEquals(3, InhabitantRules.defaultTradeTypeCount(InhabitantRules.FARM));
        assertEquals(3, InhabitantRules.defaultTradeTypeCount(InhabitantRules.BOUNTY));
        assertEquals(4, InhabitantRules.defaultTradeTypeCount(InhabitantRules.TRADER));
        assertEquals(4, InhabitantRules.defaultTradeTypeCount(InhabitantRules.ARMORSMITH));
        assertEquals(4, InhabitantRules.defaultTradeTypeCount(InhabitantRules.MASTER_ARMORSMITH));
        assertEquals(4, InhabitantRules.defaultTradeTypeCount(InhabitantRules.WEALTHY));
        assertEquals(4, InhabitantRules.defaultTradeTypeCount(InhabitantRules.JEWELER));
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
            assertTrue(poor.size() <= 2);
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
            assertTrue(rich.size() <= 7);
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
            assertTrue(wealthy.size() >= 1);
            assertTrue(wealthy.size() <= 3);
        }
        for (int seed = 0; seed < 40; seed++) {
            List<InhabitantRules.TradeListing> palaceWealthy =
                    InhabitantRules.scaledListings(InhabitantRules.WEALTHY, 38, 0, false, new Random(seed));
            assertTrue(palaceWealthy.size() >= 4);
            assertTrue(palaceWealthy.size() <= 7);
        }
    }

    @Test
    void dawnWaitsForASurvivalPlayer() {
        assertFalse(InhabitantRules.shouldAttemptDawn(5, 5, true));
        assertFalse(InhabitantRules.shouldAttemptDawn(4, 5, false));
        assertTrue(InhabitantRules.shouldAttemptDawn(4, 5, true));
        assertTrue(InhabitantRules.shouldAttemptDawn(-1, 0, true));
        assertTrue(InhabitantRules.houseScanDue(0L, 0));
        assertFalse(InhabitantRules.houseScanDue(1L, 0));
        assertTrue(InhabitantRules.houseScanDue(1200L, 0));
        assertFalse(InhabitantRules.houseScanDue(0L, 1));
        assertTrue(InhabitantRules.houseScanDue(1199L, 1));
        assertTrue(InhabitantRules.houseScanDue(0L, 1200));
        assertFalse(InhabitantRules.houseScanDue(-1L, 0));
        assertEquals(300, InhabitantRules.EXPIRE_SCAN_TICKS);
        assertEquals(300, InhabitantRules.VISIT_SCAN_TICKS);
        assertEquals(0, Math.floorMod(0L, InhabitantRules.EXPIRE_SCAN_TICKS));
        assertEquals(0, Math.floorMod(300L, InhabitantRules.VISIT_SCAN_TICKS));
        assertEquals(15, Math.floorMod(15L, InhabitantRules.VISIT_SCAN_TICKS));
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

    @Test
    void clockSuggestsOneRemainingPointGain() {
        assertEquals(180_000L, InhabitantRules.HOUSE_HINT_MILLIS);
        InhabitantRules.AmenityCounts empty =
                new InhabitantRules.AmenityCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, false);
        assertEquals(
                List.of(
                        InhabitantRules.HINT_WINDOWS,
                        InhabitantRules.HINT_ART,
                        InhabitantRules.HINT_CARPETS,
                        InhabitantRules.HINT_SEATING,
                        InhabitantRules.HINT_STORAGE,
                        InhabitantRules.HINT_WORKSTATIONS,
                        InhabitantRules.HINT_LIGHTS,
                        InhabitantRules.HINT_PLANTS,
                        InhabitantRules.HINT_BOOKS,
                        InhabitantRules.HINT_BED,
                        InhabitantRules.HINT_KITCHEN_WATER,
                        InhabitantRules.HINT_VOLUME,
                        InhabitantRules.HINT_ROOMS,
                        InhabitantRules.HINT_ISOLATION_NEAR),
                InhabitantRules.openImprovements(empty, 0, 0, 0));
        for (String improvement : InhabitantRules.openImprovements(empty, 0, 0, 0)) {
            String text = InhabitantRules.improvementText(improvement);
            assertFalse(text.isEmpty(), improvement);
            assertTrue(text.endsWith("."), improvement);
        }
        assertEquals("", InhabitantRules.improvementText("roof"));
        assertEquals("", InhabitantRules.improvementText(null));

        InhabitantRules.AmenityCounts maxed =
                new InhabitantRules.AmenityCounts(3, 6, 12, 2, 2, 2, 2, 2, 2, 1, true, true);
        assertTrue(InhabitantRules.openImprovements(maxed, 240, 3, 2).isEmpty());
        assertTrue(InhabitantRules.openImprovements(maxed, 400, 8, 2).isEmpty());
        assertEquals("", InhabitantRules.chooseImprovement(List.of(), InhabitantRules.HINT_ART, 0L, 1L, 0));

        assertFalse(InhabitantRules.openImprovements(empty, 239, 2, 0).contains(InhabitantRules.HINT_ISOLATION_FAR));
        assertTrue(InhabitantRules.openImprovements(empty, 239, 2, 0).contains(InhabitantRules.HINT_VOLUME));
        assertTrue(InhabitantRules.openImprovements(empty, 239, 2, 0).contains(InhabitantRules.HINT_ROOMS));
        assertFalse(InhabitantRules.openImprovements(empty, 240, 3, 0).contains(InhabitantRules.HINT_VOLUME));
        assertFalse(InhabitantRules.openImprovements(empty, 240, 3, 0).contains(InhabitantRules.HINT_ROOMS));
        assertTrue(InhabitantRules.openImprovements(empty, 240, 3, 1).contains(InhabitantRules.HINT_ISOLATION_FAR));
        assertFalse(InhabitantRules.openImprovements(empty, 240, 3, 1).contains(InhabitantRules.HINT_ISOLATION_NEAR));
        assertFalse(InhabitantRules.openImprovements(empty, 240, 3, 2).contains(InhabitantRules.HINT_ISOLATION_FAR));

        InhabitantRules.AmenityCounts windows =
                new InhabitantRules.AmenityCounts(2, 6, 12, 2, 2, 2, 2, 2, 2, 1, true, true);
        assertTrue(InhabitantRules.openImprovements(windows, 240, 3, 2).contains(InhabitantRules.HINT_WINDOWS));
        InhabitantRules.AmenityCounts windowCap =
                new InhabitantRules.AmenityCounts(3, 6, 12, 2, 2, 2, 2, 2, 2, 1, true, true);
        assertFalse(InhabitantRules.openImprovements(windowCap, 240, 3, 2).contains(InhabitantRules.HINT_WINDOWS));
        InhabitantRules.AmenityCounts rugs =
                new InhabitantRules.AmenityCounts(3, 6, 11, 2, 2, 2, 2, 2, 2, 1, true, true);
        assertTrue(InhabitantRules.openImprovements(rugs, 240, 3, 2).contains(InhabitantRules.HINT_CARPETS));
        InhabitantRules.AmenityCounts rugCap =
                new InhabitantRules.AmenityCounts(3, 6, 12, 2, 2, 2, 2, 2, 2, 1, true, true);
        assertFalse(InhabitantRules.openImprovements(rugCap, 240, 3, 2).contains(InhabitantRules.HINT_CARPETS));
        InhabitantRules.AmenityCounts heat =
                new InhabitantRules.AmenityCounts(3, 6, 12, 2, 2, 2, 2, 2, 2, 1, false, true);
        List<String> heatOpen = InhabitantRules.openImprovements(heat, 240, 3, 2);
        assertTrue(heatOpen.contains(InhabitantRules.HINT_KITCHEN_HEAT));
        assertFalse(heatOpen.contains(InhabitantRules.HINT_KITCHEN_WATER));
        InhabitantRules.AmenityCounts water =
                new InhabitantRules.AmenityCounts(3, 6, 12, 2, 2, 2, 2, 2, 2, 1, false, false);
        List<String> waterOpen = InhabitantRules.openImprovements(water, 240, 3, 2);
        assertTrue(waterOpen.contains(InhabitantRules.HINT_KITCHEN_WATER));
        assertFalse(waterOpen.contains(InhabitantRules.HINT_KITCHEN_HEAT));

        List<String> open = List.of(InhabitantRules.HINT_ART, InhabitantRules.HINT_BOOKS, InhabitantRules.HINT_BED);
        long now = 10_000L;
        long until = now + InhabitantRules.HOUSE_HINT_MILLIS;
        assertEquals(
                InhabitantRules.HINT_BOOKS,
                InhabitantRules.chooseImprovement(open, InhabitantRules.HINT_BOOKS, now, until, 0));
        assertTrue(InhabitantRules.keepImprovement(open, InhabitantRules.HINT_BOOKS, until - 1, until));
        assertFalse(InhabitantRules.keepImprovement(open, InhabitantRules.HINT_BOOKS, until, until));
        assertEquals(
                InhabitantRules.HINT_ART,
                InhabitantRules.chooseImprovement(open, InhabitantRules.HINT_BOOKS, until, until, 0));
        List<String> afterBooks = List.of(InhabitantRules.HINT_ART, InhabitantRules.HINT_BED);
        assertEquals(
                InhabitantRules.HINT_BED,
                InhabitantRules.chooseImprovement(afterBooks, InhabitantRules.HINT_BOOKS, now, until, 1));
        assertEquals(InhabitantRules.HINT_BED, InhabitantRules.chooseImprovement(open, "", now, until, -1));

        String bed = InhabitantRules.houseHintBed("minecraft:overworld", 4, 64, -2);
        assertEquals("minecraft:overworld 4 64 -2", bed);
        List<InhabitantRules.HouseHint> saved =
                InhabitantRules.rememberHint(List.of(), bed, InhabitantRules.HINT_CARPETS, now);
        assertEquals(1, saved.size());
        assertEquals(until, saved.getFirst().until());
        String other = InhabitantRules.houseHintBed("minecraft:overworld", 80, 64, -2);
        List<InhabitantRules.HouseHint> both = InhabitantRules.rememberHint(
                saved, other, InhabitantRules.HINT_ART, now + 1_000L);
        assertEquals(InhabitantRules.HINT_CARPETS, InhabitantRules.hintFor(both, bed).improvement());
        assertEquals(until, InhabitantRules.hintFor(both, bed).until());
        List<InhabitantRules.HouseHint> replaced =
                InhabitantRules.rememberHint(both, bed, InhabitantRules.HINT_WINDOWS, now + 1_000L);
        assertEquals(InhabitantRules.HINT_WINDOWS, InhabitantRules.hintFor(replaced, bed).improvement());
        assertEquals(now + 1_000L + InhabitantRules.HOUSE_HINT_MILLIS, InhabitantRules.hintFor(replaced, bed).until());
        assertEquals(InhabitantRules.HINT_ART, InhabitantRules.hintFor(replaced, other).improvement());
        List<InhabitantRules.HouseHint> cleared = InhabitantRules.rememberHint(replaced, bed, "", now + 1_000L);
        assertNull(InhabitantRules.hintFor(cleared, bed));
        assertEquals(InhabitantRules.HINT_ART, InhabitantRules.hintFor(cleared, other).improvement());
        List<InhabitantRules.HouseHint> pruned =
                InhabitantRules.rememberHint(cleared, bed, InhabitantRules.HINT_BED, until + 5_000L);
        assertNull(InhabitantRules.hintFor(pruned, other));
        assertEquals(InhabitantRules.HINT_BED, InhabitantRules.hintFor(pruned, bed).improvement());
    }

    @Test
    void clockNamesThreeNewSuggestionsEachMinecraftDay() {
        assertEquals(3, InhabitantRules.HOUSE_HINTS_PER_DAY);
        assertTrue(InhabitantRules.hintAllowed(null, 4L));
        assertTrue(InhabitantRules.hintAllowed(new InhabitantRules.HouseHintBudget(4L, 2), 4L));
        assertFalse(InhabitantRules.hintAllowed(new InhabitantRules.HouseHintBudget(4L, 3), 4L));
        assertTrue(InhabitantRules.hintAllowed(new InhabitantRules.HouseHintBudget(4L, 3), 5L));
        assertEquals(new InhabitantRules.HouseHintBudget(4L, 1), InhabitantRules.noteHint(null, 4L));
        assertEquals(
                new InhabitantRules.HouseHintBudget(4L, 3),
                InhabitantRules.noteHint(new InhabitantRules.HouseHintBudget(4L, 2), 4L));
        assertEquals(
                new InhabitantRules.HouseHintBudget(5L, 1),
                InhabitantRules.noteHint(new InhabitantRules.HouseHintBudget(4L, 3), 5L));

        List<String> open = List.of(InhabitantRules.HINT_ART, InhabitantRules.HINT_BOOKS, InhabitantRules.HINT_BED);
        long now = 10_000L;
        long until = now + InhabitantRules.HOUSE_HINT_MILLIS;
        InhabitantRules.HouseHintBudget full = new InhabitantRules.HouseHintBudget(4L, 3);
        InhabitantRules.HintLook repeat =
                InhabitantRules.lookHint(open, InhabitantRules.HINT_BOOKS, now, until, 0, full, 4L);
        assertEquals(InhabitantRules.HINT_BOOKS, repeat.improvement());
        assertFalse(repeat.spent());
        assertEquals(full, repeat.budget());

        InhabitantRules.HintLook blocked =
                InhabitantRules.lookHint(open, InhabitantRules.HINT_BOOKS, until, until, 0, full, 4L);
        assertEquals("", blocked.improvement());
        assertFalse(blocked.spent());

        InhabitantRules.HintLook third = InhabitantRules.lookHint(
                open, "", now, 0L, 0, new InhabitantRules.HouseHintBudget(4L, 2), 4L);
        assertEquals(InhabitantRules.HINT_ART, third.improvement());
        assertTrue(third.spent());
        assertEquals(new InhabitantRules.HouseHintBudget(4L, 3), third.budget());

        InhabitantRules.HintLook nextDay = InhabitantRules.lookHint(open, "", now, 0L, 1, full, 5L);
        assertEquals(InhabitantRules.HINT_BOOKS, nextDay.improvement());
        assertTrue(nextDay.spent());
        assertEquals(new InhabitantRules.HouseHintBudget(5L, 1), nextDay.budget());

        InhabitantRules.HintLook none =
                InhabitantRules.lookHint(List.of(), "", now, 0L, 0, new InhabitantRules.HouseHintBudget(4L, 0), 4L);
        assertEquals("", none.improvement());
        assertFalse(none.spent());
        assertEquals(0, none.budget().given());
    }
}
