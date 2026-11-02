package dev.extrahardmode.feature;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Player chests a trader reads. Minecraft-free so JUnit can cover it.
 *
 * <p>A sign that says SELL or BUY, as a whole line, marks the chest. SELL is
 * read from the top left across each row. An emerald stack sets the price for
 * new items after it. The first stack of an item sets how many move in one
 * trade. Later stacks of that item are stock and are sold first. BUY uses the
 * same prices above the bottom row. The bottom row is the emerald wallet.
 * There is no separate use cap: a deal stays up for as many trades as the
 * chest can pay for and still hold.
 */
public final class PlayerShopRules {
    public static final String EMERALD = "minecraft:emerald";
    public static final int ROW = 9;
    public static final int STACK = 64;

    private PlayerShopRules() {}

    /** One chest slot. An empty slot has a blank item id and a count of 0. */
    public record Slot(int index, String itemId, int count) {
        public Slot withCount(int next) {
            if (next <= 0) {
                return new Slot(index, "", 0);
            }
            return new Slot(index, itemId, next);
        }
    }

    /**
     * One priced item. {@code stockIndexes} are later SELL stacks, in read
     * order. BUY listings have none. Those stacks are not a second offer.
     */
    public record Listing(String itemId, int price, int quantity, int definingIndex, List<Integer> stockIndexes) {}

    /** A saved chest. The chest position is the same for both halves. */
    public record Shop(
            String dimension,
            int chestX,
            int chestY,
            int chestZ,
            int signX,
            int signY,
            int signZ,
            String owner,
            String mode) {

        public boolean sameChest(Shop other) {
            return other != null
                    && dimension != null
                    && dimension.equals(other.dimension)
                    && chestX == other.chestX
                    && chestY == other.chestY
                    && chestZ == other.chestZ;
        }

        public boolean sameSign(Shop other) {
            return other != null
                    && dimension != null
                    && dimension.equals(other.dimension)
                    && signX == other.signX
                    && signY == other.signY
                    && signZ == other.signZ;
        }
    }

    /** Chest position and mode carried on a trade result. */
    public record Marker(String mode, String dimension, int x, int y, int z) {}

    /** First whole line that is SELL or BUY. Blank lines are skipped. */
    public static String signMode(List<String> lines) {
        if (lines == null) {
            return null;
        }
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            String trimmed = line.trim();
            if (trimmed.equalsIgnoreCase("SELL")) {
                return "sell";
            }
            if (trimmed.equalsIgnoreCase("BUY")) {
                return "buy";
            }
        }
        return null;
    }

    public static String key(Shop shop) {
        if (shop == null
                || shop.dimension == null
                || shop.dimension.isEmpty()
                || shop.dimension.indexOf('|') >= 0
                || shop.owner == null
                || shop.owner.isEmpty()
                || shop.owner.indexOf('|') >= 0
                || (!"sell".equals(shop.mode) && !"buy".equals(shop.mode))) {
            return null;
        }
        return shop.dimension
                + "|"
                + shop.chestX
                + "|"
                + shop.chestY
                + "|"
                + shop.chestZ
                + "|"
                + shop.signX
                + "|"
                + shop.signY
                + "|"
                + shop.signZ
                + "|"
                + shop.owner
                + "|"
                + shop.mode;
    }

    public static Shop parse(String raw) {
        if (raw == null) {
            return null;
        }
        String[] parts = raw.split("\\|", -1);
        if (parts.length != 9 || parts[0].isEmpty() || parts[7].isEmpty()) {
            return null;
        }
        if (!"sell".equals(parts[8]) && !"buy".equals(parts[8])) {
            return null;
        }
        try {
            return new Shop(
                    parts[0],
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]),
                    Integer.parseInt(parts[4]),
                    Integer.parseInt(parts[5]),
                    Integer.parseInt(parts[6]),
                    parts[7],
                    parts[8]);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static String markerKey(String mode, String dimension, int x, int y, int z) {
        if (dimension == null || dimension.isEmpty() || dimension.indexOf('|') >= 0) {
            return null;
        }
        if (!"sell".equals(mode) && !"buy".equals(mode)) {
            return null;
        }
        return mode + "|" + dimension + "|" + x + "|" + y + "|" + z;
    }

    public static Marker parseMarker(String raw) {
        if (raw == null) {
            return null;
        }
        String[] parts = raw.split("\\|", -1);
        if (parts.length != 5 || parts[1].isEmpty()) {
            return null;
        }
        if (!"sell".equals(parts[0]) && !"buy".equals(parts[0])) {
            return null;
        }
        try {
            return new Marker(
                    parts[0],
                    parts[1],
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]),
                    Integer.parseInt(parts[4]));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static int bottomStart(int size) {
        if (size <= 0) {
            return 0;
        }
        return size - Math.min(ROW, size);
    }

    public static int aboveStart(int size) {
        int bottom = bottomStart(size);
        return bottom - Math.min(ROW, bottom);
    }

    /**
     * Priced items in read order. SELL scans every slot. BUY scans only above
     * the bottom row, and a repeated item does not add another listing.
     */
    public static List<Listing> listings(List<Slot> slots, boolean sell) {
        if (slots == null || slots.isEmpty()) {
            return List.of();
        }
        int limit = sell ? slots.size() : bottomStart(slots.size());
        int price = 0;
        Map<String, Draft> found = new LinkedHashMap<>();
        for (int i = 0; i < limit; i++) {
            Slot slot = slots.get(i);
            if (empty(slot)) {
                continue;
            }
            if (EMERALD.equals(slot.itemId())) {
                price = Math.clamp(slot.count(), 1, STACK);
                continue;
            }
            if (price <= 0) {
                continue;
            }
            Draft existing = found.get(slot.itemId());
            if (existing == null) {
                found.put(slot.itemId(), new Draft(slot.itemId(), price, slot.count(), i));
            } else if (sell) {
                existing.stock.add(i);
            }
        }
        List<Listing> listings = new ArrayList<>();
        for (Draft draft : found.values()) {
            if (draft.price <= 0 || draft.quantity <= 0) {
                continue;
            }
            listings.add(new Listing(
                    draft.itemId, draft.price, draft.quantity, draft.defining, List.copyOf(draft.stock)));
        }
        return List.copyOf(listings);
    }

    /** Trades the chest can fund right now. Stock and emerald room are the only limits. */
    public static int sellUses(List<Slot> slots, Listing listing) {
        if (listing == null || listing.price() <= 0 || listing.quantity() <= 0) {
            return 0;
        }
        int byGoods = goods(slots, listing) / listing.quantity();
        int byRoom = emeraldRoom(slots) / listing.price();
        return Math.min(byGoods, byRoom);
    }

    /** Trades the wallet can fund, and that the bottom two rows can hold. */
    public static int buyUses(List<Slot> slots, Listing listing, int itemMax) {
        if (listing == null || listing.price() <= 0 || listing.quantity() <= 0 || itemMax <= 0) {
            return 0;
        }
        int byWallet = bottomEmeralds(slots) / listing.price();
        int byRoom = itemRoom(slots, listing, itemMax) / listing.quantity();
        return Math.min(byWallet, byRoom);
    }

    public static boolean canSell(List<Slot> slots, Listing listing, int quantity, int price) {
        return listing != null
                && quantity > 0
                && price > 0
                && goods(slots, listing) >= quantity
                && emeraldRoom(slots) >= price;
    }

    public static boolean canBuy(List<Slot> slots, Listing listing, int quantity, int price, int itemMax) {
        return listing != null
                && quantity > 0
                && price > 0
                && itemMax > 0
                && bottomEmeralds(slots) >= price
                && itemRoom(slots, listing, itemMax) >= quantity;
    }

    public static List<Slot> afterSell(List<Slot> slots, Listing listing) {
        if (listing == null) {
            return null;
        }
        return afterSell(slots, listing, listing.quantity(), listing.price());
    }

    /**
     * Removes {@code quantity} of the item, stock stacks first and the defining
     * stack last, then places {@code price} emeralds at the bottom right.
     */
    public static List<Slot> afterSell(List<Slot> slots, Listing listing, int quantity, int price) {
        if (!canSell(slots, listing, quantity, price)) {
            return null;
        }
        List<Slot> next = copy(slots);
        int need = quantity;
        List<Integer> order = new ArrayList<>();
        if (listing.stockIndexes() != null) {
            order.addAll(listing.stockIndexes());
        }
        order.add(listing.definingIndex());
        for (int index : order) {
            if (need <= 0) {
                break;
            }
            if (index < 0 || index >= next.size()) {
                return null;
            }
            Slot slot = next.get(index);
            if (empty(slot) || !listing.itemId().equals(slot.itemId())) {
                continue;
            }
            int take = Math.min(need, slot.count());
            next.set(index, slot.withCount(slot.count() - take));
            need -= take;
        }
        if (need > 0) {
            return null;
        }
        int pay = price;
        for (int i = next.size() - 1; i >= bottomStart(next.size()) && pay > 0; i--) {
            Slot slot = next.get(i);
            if (empty(slot)) {
                int put = Math.min(pay, STACK);
                next.set(i, new Slot(i, EMERALD, put));
                pay -= put;
            } else if (EMERALD.equals(slot.itemId()) && slot.count() < STACK) {
                int put = Math.min(pay, STACK - slot.count());
                next.set(i, slot.withCount(slot.count() + put));
                pay -= put;
            }
        }
        if (pay > 0) {
            return null;
        }
        return List.copyOf(next);
    }

    public static List<Slot> afterBuy(List<Slot> slots, Listing listing, int itemMax) {
        if (listing == null) {
            return null;
        }
        return afterBuy(slots, listing, listing.quantity(), listing.price(), itemMax);
    }

    /**
     * Spends {@code price} emeralds from the bottom row, right to left, and
     * puts {@code quantity} items on the bottom row, then the row above it.
     * Sample stacks are not changed.
     */
    public static List<Slot> afterBuy(List<Slot> slots, Listing listing, int quantity, int price, int itemMax) {
        if (!canBuy(slots, listing, quantity, price, itemMax)) {
            return null;
        }
        List<Slot> next = copy(slots);
        int pay = price;
        for (int i = next.size() - 1; i >= bottomStart(next.size()) && pay > 0; i--) {
            Slot slot = next.get(i);
            if (empty(slot) || !EMERALD.equals(slot.itemId())) {
                continue;
            }
            int take = Math.min(pay, slot.count());
            next.set(i, slot.withCount(slot.count() - take));
            pay -= take;
        }
        if (pay > 0) {
            return null;
        }
        int give = place(next, bottomStart(next.size()), next.size(), listing, itemMax, quantity, false);
        give = place(next, aboveStart(next.size()), bottomStart(next.size()), listing, itemMax, give, true);
        if (give > 0) {
            return null;
        }
        return List.copyOf(next);
    }

    public static int emeraldRoom(List<Slot> slots) {
        if (slots == null) {
            return 0;
        }
        int room = 0;
        for (int i = bottomStart(slots.size()); i < slots.size(); i++) {
            Slot slot = slots.get(i);
            if (empty(slot)) {
                room += STACK;
            } else if (EMERALD.equals(slot.itemId())) {
                room += Math.max(0, STACK - slot.count());
            }
        }
        return room;
    }

    public static int bottomEmeralds(List<Slot> slots) {
        if (slots == null) {
            return 0;
        }
        int total = 0;
        for (int i = bottomStart(slots.size()); i < slots.size(); i++) {
            Slot slot = slots.get(i);
            if (!empty(slot) && EMERALD.equals(slot.itemId())) {
                total += slot.count();
            }
        }
        return total;
    }

    public static int itemRoom(List<Slot> slots, Listing listing, int itemMax) {
        if (slots == null || listing == null || itemMax <= 0 || listing.itemId() == null) {
            return 0;
        }
        int room = 0;
        int bottom = bottomStart(slots.size());
        for (int i = bottom; i < slots.size(); i++) {
            room += openSpace(slots.get(i), listing.itemId(), itemMax);
        }
        for (int i = aboveStart(slots.size()); i < bottom; i++) {
            if (i <= listing.definingIndex()) {
                continue;
            }
            room += openSpace(slots.get(i), listing.itemId(), itemMax);
        }
        return room;
    }

    public static boolean empty(Slot slot) {
        return slot == null || slot.count() <= 0 || slot.itemId() == null || slot.itemId().isEmpty();
    }

    private static int goods(List<Slot> slots, Listing listing) {
        int total = countAt(slots, listing.definingIndex(), listing.itemId());
        if (listing.stockIndexes() == null) {
            return total;
        }
        for (int index : listing.stockIndexes()) {
            total += countAt(slots, index, listing.itemId());
        }
        return total;
    }

    private static int countAt(List<Slot> slots, int index, String itemId) {
        if (slots == null || index < 0 || index >= slots.size() || itemId == null) {
            return 0;
        }
        Slot slot = slots.get(index);
        if (empty(slot) || !itemId.equals(slot.itemId())) {
            return 0;
        }
        return slot.count();
    }

    private static int openSpace(Slot slot, String itemId, int itemMax) {
        if (itemMax <= 0 || itemId == null) {
            return 0;
        }
        if (empty(slot)) {
            return itemMax;
        }
        if (itemId.equals(slot.itemId())) {
            return Math.max(0, itemMax - slot.count());
        }
        return 0;
    }

    private static int place(
            List<Slot> slots, int from, int to, Listing listing, int itemMax, int give, boolean onlyAfterDefining) {
        for (int i = from; i < to && give > 0; i++) {
            if (onlyAfterDefining && i <= listing.definingIndex()) {
                continue;
            }
            Slot slot = slots.get(i);
            int space = openSpace(slot, listing.itemId(), itemMax);
            if (space <= 0) {
                continue;
            }
            int put = Math.min(give, space);
            if (empty(slot)) {
                slots.set(i, new Slot(i, listing.itemId(), put));
            } else {
                slots.set(i, slot.withCount(slot.count() + put));
            }
            give -= put;
        }
        return give;
    }

    private static List<Slot> copy(List<Slot> slots) {
        List<Slot> next = new ArrayList<>(slots.size());
        for (int i = 0; i < slots.size(); i++) {
            Slot slot = slots.get(i);
            if (empty(slot)) {
                next.add(new Slot(i, "", 0));
            } else {
                next.add(new Slot(i, slot.itemId(), slot.count()));
            }
        }
        return next;
    }

    private static final class Draft {
        final String itemId;
        final int price;
        final int quantity;
        final int defining;
        final List<Integer> stock = new ArrayList<>();

        Draft(String itemId, int price, int quantity, int defining) {
            this.itemId = itemId;
            this.price = price;
            this.quantity = quantity;
            this.defining = defining;
        }
    }
}
