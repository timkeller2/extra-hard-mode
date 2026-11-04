package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.extrahardmode.feature.PlayerShopRules.Listing;
import dev.extrahardmode.feature.PlayerShopRules.Shop;
import dev.extrahardmode.feature.PlayerShopRules.Slot;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlayerShopRulesTest {
    private static final String IRON = "minecraft:iron_block";
    private static final String GOLD = "minecraft:gold_ingot";
    private static final String DIAMOND = "minecraft:diamond";

    @Test
    void signTakesTheFirstWholeSellOrBuyLine() {
        assertEquals("sell", PlayerShopRules.signMode(List.of("SELL")));
        assertEquals("buy", PlayerShopRules.signMode(List.of("  buy  ")));
        assertEquals("sell", PlayerShopRules.signMode(List.of("", "Sell", "BUY")));
        assertEquals("buy", PlayerShopRules.signMode(List.of("hello", "BUY", "SELL")));
        assertNull(PlayerShopRules.signMode(List.of("SELL NOW", "buying")));
        assertNull(PlayerShopRules.signMode(null));
    }

    @Test
    void shopKeyRoundTrips() {
        Shop shop = new Shop("minecraft:overworld", 1, 64, -3, 1, 65, -3, "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", "sell");
        assertEquals(shop, PlayerShopRules.parse(PlayerShopRules.key(shop)));
        assertNull(PlayerShopRules.parse(PlayerShopRules.key(shop) + "|extra"));
        assertNull(PlayerShopRules.key(new Shop("bad|dim", 0, 0, 0, 0, 0, 0, "owner", "sell")));
        assertEquals(
                "buy|minecraft:the_nether|4|5|6",
                PlayerShopRules.markerKey("buy", "minecraft:the_nether", 4, 5, 6));
        assertEquals("buy", PlayerShopRules.parseMarker("buy|minecraft:the_nether|4|5|6").mode());
        assertEquals(5, PlayerShopRules.parseMarker("buy|minecraft:the_nether|4|5|6").y());
        assertNull(PlayerShopRules.parseMarker("keep|minecraft:overworld|0|0|0"));
    }

    @Test
    void sellReadsPricesAndSellsExtraStacksFirst() {
        List<Slot> slots = chest(
                slot(0, PlayerShopRules.EMERALD, 2),
                slot(1, IRON, 1),
                slot(2, IRON, 20));
        List<Listing> listings = PlayerShopRules.listings(slots, true);
        assertEquals(1, listings.size());
        Listing iron = listings.get(0);
        assertEquals(IRON, iron.itemId());
        assertEquals(2, iron.price());
        assertEquals(1, iron.quantity());
        assertEquals(1, iron.definingIndex());
        assertEquals(List.of(2), iron.stockIndexes());
        assertEquals(21, PlayerShopRules.sellUses(slots, iron));

        List<Slot> once = PlayerShopRules.afterSell(slots, iron);
        assertEquals(2, count(once, 0));
        assertEquals(1, count(once, 1));
        assertEquals(19, count(once, 2));
        assertEquals(2, count(once, 26));
        assertEquals(PlayerShopRules.EMERALD, once.get(26).itemId());

        List<Slot> current = slots;
        Listing opened = iron;
        for (int i = 0; i < 20; i++) {
            current = PlayerShopRules.afterSell(current, opened, opened.quantity(), opened.price());
        }
        assertEquals(1, count(current, 1));
        assertEquals(0, count(current, 2));
        current = PlayerShopRules.afterSell(current, opened, opened.quantity(), opened.price());
        assertEquals(0, count(current, 1));
        assertEquals(42, count(current, 26));
    }

    @Test
    void sellUsesFollowTheChestRatherThanASmallCap() {
        List<Slot> slots = chest(
                slot(0, PlayerShopRules.EMERALD, 1),
                slot(1, IRON, 1),
                slot(2, IRON, 64),
                slot(3, IRON, 64),
                slot(4, IRON, 10));
        Listing iron = PlayerShopRules.listings(slots, true).get(0);
        assertEquals(139, PlayerShopRules.sellUses(slots, iron));
        assertTrue(PlayerShopRules.sellUses(slots, iron) > 3);
    }

    @Test
    void laterEmeraldChangesOnlyNewItemsAndBlockedSlotsAreLeftAlone() {
        List<Slot> slots = chest(
                slot(0, DIAMOND, 1),
                slot(1, PlayerShopRules.EMERALD, 2),
                slot(2, IRON, 1),
                slot(3, PlayerShopRules.EMERALD, 5),
                slot(4, GOLD, 3),
                slot(5, IRON, 8),
                slot(26, IRON, 10));
        List<Listing> listings = PlayerShopRules.listings(slots, true);
        assertEquals(2, listings.size());
        assertEquals(2, listings.get(0).price());
        assertEquals(IRON, listings.get(0).itemId());
        assertEquals(List.of(5, 26), listings.get(0).stockIndexes());
        assertEquals(5, listings.get(1).price());
        assertEquals(GOLD, listings.get(1).itemId());
        assertEquals(3, listings.get(1).quantity());

        List<Slot> sold = PlayerShopRules.afterSell(slots, listings.get(1));
        assertEquals(10, count(sold, 26));
        assertEquals(IRON, sold.get(26).itemId());
        assertEquals(5, count(sold, 25));
        assertEquals(PlayerShopRules.EMERALD, sold.get(25).itemId());
        assertEquals(1, count(sold, 0));
    }

    @Test
    void fullBottomRowPutsTheEmeraldsInAnEmptySlotAboveIt() {
        List<Slot> slots = chest(slot(0, PlayerShopRules.EMERALD, 1), slot(1, IRON, 4));
        for (int i = 18; i <= 26; i++) {
            slots.set(i, slot(i, "minecraft:cobble", 1));
        }
        Listing iron = PlayerShopRules.listings(slots, true).get(0);
        assertEquals(1, PlayerShopRules.sellUses(slots, iron));
        List<Slot> sold = PlayerShopRules.afterSell(slots, iron);
        assertEquals(1, count(sold, 0));
        assertEquals(PlayerShopRules.EMERALD, sold.get(0).itemId());
        assertEquals(0, count(sold, 1));
        assertEquals(1, count(sold, 17));
        assertEquals(PlayerShopRules.EMERALD, sold.get(17).itemId());
    }

    @Test
    void aFullChestWithNoEmptySlotCannotSell() {
        List<Slot> slots = chest(slot(0, PlayerShopRules.EMERALD, 1), slot(1, IRON, 4));
        for (int i = 2; i <= 26; i++) {
            slots.set(i, slot(i, "minecraft:cobble", 1));
        }
        assertEquals(0, PlayerShopRules.sellUses(slots, PlayerShopRules.listings(slots, true).get(0)));
        assertNull(PlayerShopRules.afterSell(slots, PlayerShopRules.listings(slots, true).get(0)));
    }

    @Test
    void bottomTwoRowsDoNotPriceASellChest() {
        List<Slot> pricedOnlyBelow = chest(
                slot(1, IRON, 4),
                slot(10, PlayerShopRules.EMERALD, 9),
                slot(26, PlayerShopRules.EMERALD, 3));
        assertTrue(PlayerShopRules.listings(pricedOnlyBelow, true).isEmpty());

        List<Slot> slots = chest(
                slot(0, PlayerShopRules.EMERALD, 2),
                slot(1, IRON, 4),
                slot(10, PlayerShopRules.EMERALD, 9),
                slot(12, GOLD, 1));
        List<Listing> listings = PlayerShopRules.listings(slots, true);
        assertEquals(2, listings.size());
        assertEquals(2, listings.get(0).price());
        assertEquals(IRON, listings.get(0).itemId());
        assertEquals(2, listings.get(1).price());
        assertEquals(GOLD, listings.get(1).itemId());
    }

    @Test
    void saleEmeraldsFillBottomRightStacksBeforeEmptySlots() {
        List<Slot> slots = chest(
                slot(0, PlayerShopRules.EMERALD, 2),
                slot(1, IRON, 1),
                slot(20, PlayerShopRules.EMERALD, 10),
                slot(26, PlayerShopRules.EMERALD, 60));
        Listing iron = PlayerShopRules.listings(slots, true).get(0);
        List<Slot> sold = PlayerShopRules.afterSell(slots, iron);
        assertEquals(62, count(sold, 26));
        assertEquals(10, count(sold, 20));
        assertEquals(0, count(sold, 25));
        assertEquals(2, count(sold, 0));

        slots.set(26, slot(26, PlayerShopRules.EMERALD, 63));
        sold = PlayerShopRules.afterSell(slots, iron);
        assertEquals(64, count(sold, 26));
        assertEquals(11, count(sold, 20));
        assertEquals(0, count(sold, 25));

        slots.set(26, slot(26, PlayerShopRules.EMERALD, 64));
        slots.set(20, slot(20, PlayerShopRules.EMERALD, 64));
        sold = PlayerShopRules.afterSell(slots, iron);
        assertEquals(2, count(sold, 25));
        assertEquals(PlayerShopRules.EMERALD, sold.get(25).itemId());
        assertEquals(2, count(sold, 0));
    }

    @Test
    void saleEmeraldsDoNotJoinAPriceStack() {
        List<Slot> slots = chest(slot(0, PlayerShopRules.EMERALD, 3), slot(1, IRON, 1));
        for (int i = 9; i <= 26; i++) {
            slots.set(i, slot(i, "minecraft:cobble", 1));
        }
        List<Slot> sold = PlayerShopRules.afterSell(slots, PlayerShopRules.listings(slots, true).get(0));
        assertEquals(3, count(sold, 0));
        assertEquals(3, count(sold, 8));
        assertEquals(PlayerShopRules.EMERALD, sold.get(8).itemId());
    }

    @Test
    void doubleChestKeepsPricesAboveTheBottomTwoRows() {
        List<Slot> slots = new ArrayList<>();
        for (int i = 0; i < 54; i++) {
            slots.add(new Slot(i, "", 0));
        }
        slots.set(0, slot(0, PlayerShopRules.EMERALD, 4));
        slots.set(1, slot(1, IRON, 2));
        slots.set(35, slot(35, PlayerShopRules.EMERALD, 6));
        slots.set(36, slot(36, GOLD, 1));
        slots.set(40, slot(40, PlayerShopRules.EMERALD, 12));
        slots.set(53, slot(53, PlayerShopRules.EMERALD, 7));
        List<Listing> listings = PlayerShopRules.listings(slots, true);
        assertEquals(2, listings.size());
        assertEquals(4, listings.get(0).price());
        assertEquals(6, listings.get(1).price());
        assertEquals(GOLD, listings.get(1).itemId());
        List<Slot> sold = PlayerShopRules.afterSell(slots, listings.get(0));
        assertEquals(11, count(sold, 53));
        assertEquals(12, count(sold, 40));
        assertEquals(4, count(sold, 0));
        assertEquals(6, count(sold, 35));
    }

    @Test
    void buyPaysFromTheBottomRowAndKeepsTheSample() {
        List<Slot> slots = chest(
                slot(0, PlayerShopRules.EMERALD, 3),
                slot(1, IRON, 4),
                slot(2, IRON, 10),
                slot(18, PlayerShopRules.EMERALD, 5),
                slot(26, PlayerShopRules.EMERALD, 3));
        List<Listing> listings = PlayerShopRules.listings(slots, false);
        assertEquals(1, listings.size());
        Listing iron = listings.get(0);
        assertEquals(3, iron.price());
        assertEquals(4, iron.quantity());
        assertTrue(iron.stockIndexes().isEmpty());
        assertEquals(2, PlayerShopRules.buyUses(slots, iron, 64));

        List<Slot> bought = PlayerShopRules.afterBuy(slots, iron, 64);
        assertEquals(3, count(bought, 0));
        assertEquals(4, count(bought, 1));
        assertEquals(10, count(bought, 2));
        assertEquals(5, count(bought, 18));
        assertEquals(PlayerShopRules.EMERALD, bought.get(18).itemId());
        assertEquals(0, count(bought, 26));
        assertEquals(4, count(bought, 19));
        assertEquals(IRON, bought.get(19).itemId());
    }

    @Test
    void buyUsesTheRowAboveWhenTheBottomRowIsEmeralds() {
        List<Slot> slots = chest(slot(0, PlayerShopRules.EMERALD, 1), slot(1, GOLD, 2), slot(26, PlayerShopRules.EMERALD, 8));
        for (int i = 18; i <= 25; i++) {
            slots.set(i, slot(i, PlayerShopRules.EMERALD, 64));
        }
        Listing gold = PlayerShopRules.listings(slots, false).get(0);
        assertEquals(288, PlayerShopRules.buyUses(slots, gold, 64));
        List<Slot> bought = PlayerShopRules.afterBuy(slots, gold, 64);
        assertEquals(2, count(bought, 1));
        assertEquals(7, count(bought, 26));
        assertEquals(2, count(bought, 9));
        assertEquals(GOLD, bought.get(9).itemId());
    }

    @Test
    void buyStopsWhenTheWalletOrTheRoomRunsOut() {
        List<Slot> slots = chest(slot(0, PlayerShopRules.EMERALD, 4), slot(1, IRON, 2), slot(26, PlayerShopRules.EMERALD, 5));
        Listing iron = PlayerShopRules.listings(slots, false).get(0);
        assertEquals(1, PlayerShopRules.buyUses(slots, iron, 64));
        for (int i = 9; i <= 26; i++) {
            if (i == 26) {
                continue;
            }
            slots.set(i, slot(i, "minecraft:cobble", 1));
        }
        iron = PlayerShopRules.listings(slots, false).get(0);
        assertEquals(0, PlayerShopRules.buyUses(slots, iron, 64));
        assertNull(PlayerShopRules.afterBuy(slots, iron, 64));
    }

    @Test
    void bottomEmeraldsDoNotPriceABuyChest() {
        List<Slot> slots = chest(slot(1, IRON, 1), slot(26, PlayerShopRules.EMERALD, 64));
        assertTrue(PlayerShopRules.listings(slots, false).isEmpty());
        assertEquals(64, PlayerShopRules.bottomEmeralds(slots));
    }

    private static List<Slot> chest(Slot... filled) {
        List<Slot> slots = new ArrayList<>();
        for (int i = 0; i < 27; i++) {
            slots.add(new Slot(i, "", 0));
        }
        for (Slot slot : filled) {
            slots.set(slot.index(), slot);
        }
        return slots;
    }

    private static Slot slot(int index, String itemId, int count) {
        return new Slot(index, itemId, count);
    }

    private static int count(List<Slot> slots, int index) {
        Slot slot = slots.get(index);
        return PlayerShopRules.empty(slot) ? 0 : slot.count();
    }
}
