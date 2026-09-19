package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InhabitantRulesTest {
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
                new InhabitantRules.AmenityCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false);
        assertEquals(0, InhabitantRules.score(empty));
        InhabitantRules.AmenityCounts windows =
                new InhabitantRules.AmenityCounts(10, 0, 0, 0, 0, 0, 0, 0, 0, 0, false);
        assertEquals(6, InhabitantRules.score(windows));
        InhabitantRules.AmenityCounts rugs =
                new InhabitantRules.AmenityCounts(0, 0, 40, 0, 0, 0, 0, 0, 0, 0, false);
        assertEquals(3, InhabitantRules.score(rugs));
        InhabitantRules.AmenityCounts furnished =
                new InhabitantRules.AmenityCounts(3, 2, 8, 2, 2, 1, 2, 1, 1, 1, true);
        assertEquals(6 + 2 + 2 + 2 + 2 + 1 + 2 + 1 + 1 + 1 + 2, InhabitantRules.score(furnished));
        InhabitantRules.AmenityCounts art =
                new InhabitantRules.AmenityCounts(0, 10, 0, 0, 0, 0, 0, 0, 0, 0, false);
        assertEquals(6, InhabitantRules.score(art));
        InhabitantRules.AmenityCounts maxed =
                new InhabitantRules.AmenityCounts(10, 10, 40, 10, 10, 10, 10, 10, 10, 1, true);
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
                new InhabitantRules.AmenityCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false);
        InhabitantRules.GateResult closed = InhabitantRules.gates(true, 40, true, true, true, true, true, poor);
        assertFalse(closed.eligible());
        assertTrue(closed.missing().stream().anyMatch(s -> s.contains("more furnishings")));
        assertTrue(InhabitantRules.inspectFallback(closed).contains("/ehm help homes"));
        InhabitantRules.AmenityCounts ok =
                new InhabitantRules.AmenityCounts(3, 2, 8, 1, 1, 1, 1, 1, 1, 0, false);
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
                new InhabitantRules.AmenityCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, true);
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
                List.of("hauler", "cook", "farm", "trader", "council"),
                InhabitantRules.unseenSpecialties(List.of(), 12));
        assertEquals(
                List.of("hauler", "cook", "farm", "bounty", "trader", "wealthy", "council"),
                InhabitantRules.unseenSpecialties(List.of(), 18));
        assertTrue(InhabitantRules.unseenSpecialties(List.of(), 24).contains(InhabitantRules.ARMORSMITH));
        assertTrue(InhabitantRules.unseenSpecialties(List.of(), 30).contains(InhabitantRules.MASTER_ARMORSMITH));
        assertFalse(InhabitantRules.specialtyAllowed(InhabitantRules.ARMORSMITH, 23));
        assertTrue(InhabitantRules.specialtyAllowed(InhabitantRules.ARMORSMITH, 24));
        assertFalse(InhabitantRules.specialtyAllowed(InhabitantRules.MASTER_ARMORSMITH, 29));
        assertTrue(InhabitantRules.specialtyAllowed(InhabitantRules.MASTER_ARMORSMITH, 30));
        assertFalse(InhabitantRules.specialtyAllowed(InhabitantRules.WEALTHY, 17));
        assertTrue(InhabitantRules.specialtyAllowed(InhabitantRules.WEALTHY, 18));
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
        assertTrue(InhabitantRules.specialtyAllowed(InhabitantRules.COUNCIL, 12));
        assertEquals(4, InhabitantRules.defaultTradeTypeCount(InhabitantRules.COUNCIL));
        boolean sawCookAfterAll = false;
        for (int roll = 0; roll < 20; roll++) {
            if (InhabitantRules.COOK.equals(
                    InhabitantRules.pickSpecialty(kitchen, 12, roll, InhabitantRules.SPECIALTIES))) {
                sawCookAfterAll = true;
            }
        }
        assertTrue(sawCookAfterAll);
        InhabitantRules.AmenityCounts empty =
                new InhabitantRules.AmenityCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false);
        int council = 0;
        for (int roll = 0; roll < 80; roll++) {
            if (InhabitantRules.COUNCIL.equals(
                    InhabitantRules.pickSpecialty(empty, 12, roll, InhabitantRules.SPECIALTIES))) {
                council++;
            }
        }
        assertTrue(council >= 50);
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
    void restockEveryThreeDays() {
        assertEquals(3, InhabitantRules.RESTOCK_DAYS);
        assertTrue(InhabitantRules.shouldRestock(-1, 0));
        assertTrue(InhabitantRules.shouldRestock(-1, 10));
        assertFalse(InhabitantRules.shouldRestock(5, 5));
        assertFalse(InhabitantRules.shouldRestock(5, 7));
        assertTrue(InhabitantRules.shouldRestock(5, 8));
        assertTrue(InhabitantRules.shouldRestock(5, 9));
        assertEquals(3, InhabitantRules.restockDays(InhabitantRules.TRADER));
        assertEquals(30, InhabitantRules.restockDays(InhabitantRules.ARMORSMITH));
        assertEquals(30, InhabitantRules.restockDays(InhabitantRules.MASTER_ARMORSMITH));
        assertFalse(InhabitantRules.shouldRestock(0, 29, 30));
        assertTrue(InhabitantRules.shouldRestock(0, 30, 30));
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
    void shopStockScalesWithHouseScore() {
        assertEquals(5, InhabitantRules.defaultTradeTypeCount(InhabitantRules.HAULER));
        assertEquals(5, InhabitantRules.defaultTradeTypeCount(InhabitantRules.COOK));
        assertEquals(3, InhabitantRules.defaultTradeTypeCount(InhabitantRules.FARM));
        assertEquals(3, InhabitantRules.defaultTradeTypeCount(InhabitantRules.BOUNTY));
        assertEquals(4, InhabitantRules.defaultTradeTypeCount(InhabitantRules.TRADER));
        assertEquals(4, InhabitantRules.defaultTradeTypeCount(InhabitantRules.ARMORSMITH));
        assertEquals(4, InhabitantRules.defaultTradeTypeCount(InhabitantRules.MASTER_ARMORSMITH));
        assertEquals(8, InhabitantRules.defaultTradeTypeCount(InhabitantRules.WEALTHY));
        assertEquals(0, InhabitantRules.extraTradePoints(12, InhabitantRules.HAULER));
        assertEquals(25, InhabitantRules.tradeMinPercent(12, InhabitantRules.HAULER));
        assertEquals(50, InhabitantRules.tradeMaxPercent(12, InhabitantRules.HAULER));
        assertEquals(25, InhabitantRules.tradeMinPercent(18, InhabitantRules.WEALTHY));
        assertEquals(25, InhabitantRules.tradeMinPercent(24, InhabitantRules.ARMORSMITH));
        assertEquals(25, InhabitantRules.tradeMinPercent(30, InhabitantRules.MASTER_ARMORSMITH));
        assertEquals(35, InhabitantRules.tradeMinPercent(13, InhabitantRules.HAULER));
        assertEquals(60, InhabitantRules.tradeMaxPercent(13, InhabitantRules.HAULER));
        assertEquals(105, InhabitantRules.tradeMinPercent(20, InhabitantRules.HAULER));
        assertEquals(130, InhabitantRules.tradeMaxPercent(20, InhabitantRules.HAULER));
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
                    InhabitantRules.scaledListings(InhabitantRules.HAULER, 22, 1, false, new Random(seed));
            assertTrue(rich.size() >= 6);
            assertTrue(rich.size() <= 8);
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
            assertTrue(wealthy.size() >= 2);
            assertTrue(wealthy.size() <= 4);
        }
        List<InhabitantRules.TradeListing> palaceWealthy =
                InhabitantRules.scaledListings(InhabitantRules.WEALTHY, 28, 0, false, new Random(1));
        assertTrue(palaceWealthy.size() >= 10);
        assertTrue(palaceWealthy.size() <= 12);
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
