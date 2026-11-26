package dev.extrahardmode.feature;

import java.util.ArrayList;
import java.util.List;

/**
 * Auto-stack decisions. Minecraft-free so JUnit can cover them.
 *
 * <p>The hotbar is the first 9 player slots. The main inventory is the next 27
 * (slots 9 through 35). Offhand is 40 and equipment starts at 36, so those
 * indexes are never eligible.
 */
public final class AutoStackRules {
    public static final int RANGE = 12;
    /** Ticks a flying stack takes to reach its chest. */
    public static final int FLY_TICKS = 12;
    /** Highest point of the arc, in blocks, halfway along the flight. */
    public static final double FLY_ARC = 0.45;
    public static final int HOTBAR_SIZE = 9;
    public static final int MAIN_SLOTS = 27;
    public static final int MAIN_START = HOTBAR_SIZE;
    public static final int MAIN_END = MAIN_START + MAIN_SLOTS;

    private AutoStackRules() {}

    /** A main-inventory slot. Hotbar, offhand, and equipment are not. */
    public static boolean mainSlot(int inventoryIndex) {
        return inventoryIndex >= MAIN_START && inventoryIndex < MAIN_END;
    }

    /** Bit for a main-inventory slot, or -1 when that slot cannot be excluded. */
    public static int bit(int inventoryIndex) {
        if (!mainSlot(inventoryIndex)) {
            return -1;
        }
        return inventoryIndex - MAIN_START;
    }

    public static boolean excluded(int mask, int inventoryIndex) {
        int bit = bit(inventoryIndex);
        return bit >= 0 && (mask & (1 << bit)) != 0;
    }

    /** Flips one main-inventory slot. Any other index leaves the mask unchanged. */
    public static int toggle(int mask, int inventoryIndex) {
        int bit = bit(inventoryIndex);
        if (bit < 0) {
            return mask;
        }
        return mask ^ (1 << bit);
    }

    /** All 27 main-inventory bits. */
    public static int mainBits() {
        return (1 << MAIN_SLOTS) - 1;
    }

    /** Flips every main-inventory slot. */
    public static int toggleAllMain(int mask) {
        return mask ^ mainBits();
    }

    /**
     * The first click of an Alt-double-click already flipped this main slot.
     * Flip the rest so every main slot changes once.
     */
    public static int toggleMainRest(int mask, int inventoryIndex) {
        return toggle(toggleAllMain(mask), inventoryIndex);
    }

    /** A slot inside a chest. Size is at most 63 so a shift never uses bit 64. */
    public static boolean chestSlot(int slot, int size) {
        return size > 0 && size <= 63 && slot >= 0 && slot < size;
    }

    public static boolean chestExcluded(long mask, int slot) {
        return slot >= 0 && slot <= 62 && (mask & (1L << slot)) != 0;
    }

    /** Flips one chest slot. An index outside that chest leaves the mask unchanged. */
    public static long toggleChest(long mask, int slot, int size) {
        if (!chestSlot(slot, size)) {
            return mask;
        }
        return mask ^ (1L << slot);
    }

    /** Bits 0 through size-1. Size 63 is bits 0 through 62. A larger size is none. */
    public static long chestBits(int size) {
        if (size <= 0 || size > 63) {
            return 0L;
        }
        if (size == 63) {
            return Long.MAX_VALUE;
        }
        return (1L << size) - 1L;
    }

    public static long clampChest(long mask, int size) {
        return mask & chestBits(size);
    }

    /** Flips every slot in a chest of this size. */
    public static long toggleChestAll(long mask, int size) {
        return mask ^ chestBits(size);
    }

    /**
     * The first click of an Alt-double-click already flipped this chest slot.
     * Flip the rest so every slot in that chest changes once.
     */
    public static long toggleChestRest(long mask, int size, int slot) {
        return toggleChest(toggleChestAll(mask, size), slot, size);
    }

    public static boolean eligible(int inventoryIndex, int mask, boolean empty) {
        return mainSlot(inventoryIndex) && !empty && !excluded(mask, inventoryIndex);
    }

    /** 0 at the player, 1 at the chest. The middle of the flight is halfway. */
    public static float flyProgress(float ageTicks) {
        float t = flightT(ageTicks);
        return t * t * (3.0f - 2.0f * t);
    }

    public static double flyCoord(double from, double to, float progress) {
        return from + (to - from) * progress;
    }

    /** Rises through the middle of the flight and meets the chest at the end. */
    public static double flyArc(float ageTicks) {
        return Math.sin(flightT(ageTicks) * Math.PI) * FLY_ARC;
    }

    private static float flightT(float ageTicks) {
        if (ageTicks <= 0.0f) {
            return 0.0f;
        }
        if (ageTicks >= FLY_TICKS) {
            return 1.0f;
        }
        return ageTicks / FLY_TICKS;
    }

    /** Euclidean distance from the player block to a chest block. */
    public static boolean inRange(int dx, int dy, int dz) {
        long reach = RANGE;
        long distance = (long) dx * dx + (long) dy * dy + (long) dz * dz;
        return distance <= reach * reach;
    }

    /**
     * One stack. A blank key is empty. {@code movable} is a player slot that
     * auto-stack may take from. {@code max} is how many of that item the slot can hold.
     */
    public record Pile(String key, int count, int max, boolean movable, boolean excluded) {
        public Pile {
            key = key == null ? "" : key;
            count = Math.max(0, count);
            max = Math.max(0, max);
        }

        public static Pile player(String key, int count, int max, boolean excluded) {
            return new Pile(key, count, max, true, excluded);
        }

        public static Pile chest(String key, int count, int max) {
            return chest(key, count, max, false);
        }

        public static Pile chest(String key, int count, int max, boolean excluded) {
            return new Pile(key, count, max, false, excluded);
        }
    }

    /** One transfer. {@code source} is an index into the player pile list. */
    public record Move(int source, int chest, int slot, int count) {}

    /**
     * Plan deposits. Chests are tried in list order. Inside a chest, matching
     * partial stacks fill before empty slots. A chest that does not already
     * hold the item is left alone, and an excluded or immovable pile stays put.
     * An excluded chest slot is not a destination. An item sitting in one still
     * counts as the chest holding that item.
     */
    public static List<Move> plan(List<Pile> sources, List<List<Pile>> chests) {
        if (sources == null || sources.isEmpty() || chests == null || chests.isEmpty()) {
            return List.of();
        }
        int[] left = new int[sources.size()];
        for (int i = 0; i < sources.size(); i++) {
            left[i] = sources.get(i).count();
        }
        int chestCount = chests.size();
        int[][] have = new int[chestCount][];
        String[][] keys = new String[chestCount][];
        for (int c = 0; c < chestCount; c++) {
            List<Pile> slots = chests.get(c);
            int size = slots == null ? 0 : slots.size();
            have[c] = new int[size];
            keys[c] = new String[size];
            for (int s = 0; s < size; s++) {
                Pile pile = slots.get(s);
                have[c][s] = pile.count();
                keys[c][s] = pile.count() > 0 ? pile.key() : "";
            }
        }
        List<Move> moves = new ArrayList<>();
        for (int i = 0; i < sources.size(); i++) {
            Pile source = sources.get(i);
            if (!source.movable() || source.excluded() || source.key().isEmpty() || left[i] <= 0) {
                continue;
            }
            for (int c = 0; c < chestCount && left[i] > 0; c++) {
                if (!chestHas(chests.get(c), source.key())) {
                    continue;
                }
                List<Pile> slots = chests.get(c);
                int size = slots == null ? 0 : slots.size();
                for (int s = 0; s < size && left[i] > 0; s++) {
                    if (slots.get(s).excluded() || have[c][s] <= 0 || !source.key().equals(keys[c][s])) {
                        continue;
                    }
                    int room = room(source.max(), slots.get(s).max(), have[c][s]);
                    add(moves, left, have, i, c, s, room);
                }
                for (int s = 0; s < size && left[i] > 0; s++) {
                    if (slots.get(s).excluded() || have[c][s] > 0) {
                        continue;
                    }
                    int room = room(source.max(), slots.get(s).max(), 0);
                    if (add(moves, left, have, i, c, s, room)) {
                        keys[c][s] = source.key();
                    }
                }
            }
        }
        return List.copyOf(moves);
    }

    private static boolean chestHas(List<Pile> slots, String key) {
        if (slots == null || key == null || key.isEmpty()) {
            return false;
        }
        for (Pile pile : slots) {
            if (pile.count() > 0 && key.equals(pile.key())) {
                return true;
            }
        }
        return false;
    }

    private static int room(int sourceMax, int destMax, int destCount) {
        int cap = Math.min(sourceMax, destMax);
        return Math.max(0, cap - Math.max(0, destCount));
    }

    private static boolean add(List<Move> moves, int[] left, int[][] have, int source, int chest, int slot, int room) {
        int moved = Math.min(left[source], Math.max(0, room));
        if (moved <= 0) {
            return false;
        }
        left[source] -= moved;
        have[chest][slot] += moved;
        moves.add(new Move(source, chest, slot, moved));
        return true;
    }
}
