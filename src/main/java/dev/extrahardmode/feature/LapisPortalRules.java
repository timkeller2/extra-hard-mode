package dev.extrahardmode.feature;

import java.util.ArrayList;
import java.util.List;

/**
 * Lapis portal linking and fuel. Minecraft-free so JUnit can cover it.
 *
 * <p>A booth is a door with lapis blocks under it, above it, and on the three
 * sides other than the way the door faces. The chest sits one block behind the
 * top half of the door. The largest lapis lazuli stack is the channel. Fuel is
 * redstone dust and nether quartz from that chest.
 */
public final class LapisPortalRules {
    public static final String LAPIS_LAZULI = "minecraft:lapis_lazuli";
    public static final String REDSTONE = "minecraft:redstone";
    public static final String QUARTZ = "minecraft:quartz";

    /** One fuel item per this many blocks, rounded up. */
    public static final int BLOCKS_PER_FUEL = 100;

    public static final int PARTNER_NONE = -1;
    public static final int PARTNER_AMBIGUOUS = -2;

    private LapisPortalRules() {}

    /** One chest slot. {@code itemId} is a registry id such as {@code minecraft:redstone}. */
    public record Stack(int index, String itemId, int count) {}

    /** Block offset from the lower half of the door. */
    public record Offset(int x, int y, int z) {}

    /** Saved booth, without a Minecraft level. */
    public record Place(String dimension, int x, int y, int z, Facing facing) {
        public boolean samePlace(Place other) {
            return other != null
                    && dimension != null
                    && dimension.equals(other.dimension)
                    && x == other.x
                    && y == other.y
                    && z == other.z;
        }
    }

    /** Horizontal door facing. Steps match Minecraft: south is +Z, east is +X. */
    public enum Facing {
        NORTH(0, -1, "north"),
        EAST(1, 0, "east"),
        SOUTH(0, 1, "south"),
        WEST(-1, 0, "west");

        public final int x;
        public final int z;
        public final String wire;

        Facing(int x, int z, String wire) {
            this.x = x;
            this.z = z;
            this.wire = wire;
        }

        public Facing opposite() {
            return switch (this) {
                case NORTH -> SOUTH;
                case SOUTH -> NORTH;
                case EAST -> WEST;
                case WEST -> EAST;
            };
        }

        public static Facing byName(String name) {
            if (name == null) {
                return null;
            }
            for (Facing facing : values()) {
                if (facing.wire.equals(name)) {
                    return facing;
                }
            }
            return null;
        }
    }

    /** Largest lapis lazuli stack. Zero when the chest has none. Lapis is not fuel. */
    public static int channel(List<Stack> slots) {
        int best = 0;
        if (slots == null) {
            return 0;
        }
        for (Stack slot : slots) {
            if (slot != null && LAPIS_LAZULI.equals(slot.itemId()) && slot.count() > best) {
                best = slot.count();
            }
        }
        return best;
    }

    public static int total(List<Stack> slots, String itemId) {
        if (slots == null || itemId == null) {
            return 0;
        }
        int sum = 0;
        for (Stack slot : slots) {
            if (slot != null && itemId.equals(slot.itemId()) && slot.count() > 0) {
                sum += slot.count();
            }
        }
        return sum;
    }

    /** Both fuels are present before anything is spent. */
    public static boolean bothFuels(List<Stack> slots) {
        return total(slots, REDSTONE) >= 1 && total(slots, QUARTZ) >= 1;
    }

    public static double distance(int x1, int y1, int z1, int x2, int y2, int z2) {
        double dx = (double) x2 - x1;
        double dy = (double) y2 - y1;
        double dz = (double) z2 - z1;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Same coordinates in another dimension still cost a trip. A same-dimension
     * distance of zero stays zero.
     */
    public static double travelDistance(boolean sameDimension, double raw) {
        if (!sameDimension && raw <= 0.0) {
            return 1.0;
        }
        return Math.max(0.0, raw);
    }

    /**
     * {@code ceil(distance / 100)}. When both fuels are present the cost is
     * integer-halved, and that half is at least 1.
     */
    public static int travelCost(double distance, boolean bothFuels) {
        if (distance <= 0.0) {
            return 0;
        }
        int full = (int) Math.ceil(distance / (double) BLOCKS_PER_FUEL);
        if (full < 1) {
            full = 1;
        }
        if (!bothFuels) {
            return full;
        }
        return Math.max(1, full / 2);
    }

    /**
     * Spend {@code cost} fuel items. The material with the higher remaining total
     * is used. Equal totals spend nether quartz. Each item comes off the largest
     * stack of that material, and a tie uses the lowest slot. Returns null when
     * the chest cannot pay. Does not change {@code slots}.
     */
    public static List<Stack> pay(List<Stack> slots, int cost) {
        List<Stack> working = new ArrayList<>();
        if (slots != null) {
            for (Stack slot : slots) {
                if (slot != null) {
                    working.add(slot);
                }
            }
        }
        if (cost <= 0) {
            return List.copyOf(working);
        }
        if (total(working, REDSTONE) + total(working, QUARTZ) < cost) {
            return null;
        }
        for (int left = cost; left > 0; left--) {
            int redstone = total(working, REDSTONE);
            int quartz = total(working, QUARTZ);
            String take = redstone > quartz ? REDSTONE : QUARTZ;
            int index = largest(working, take);
            if (index < 0) {
                return null;
            }
            Stack chosen = working.get(index);
            working.set(index, new Stack(chosen.index(), chosen.itemId(), chosen.count() - 1));
        }
        return List.copyOf(working);
    }

    public static String spentPhrase(int redstone, int quartz) {
        boolean dust = redstone > 0;
        boolean gems = quartz > 0;
        if (dust && gems) {
            return redstone + " redstone dust and " + quartz + " nether quartz";
        }
        if (dust) {
            return redstone + " redstone dust";
        }
        if (gems) {
            return quartz + " nether quartz";
        }
        return "";
    }

    /**
     * Index of the one other booth with the same positive channel.
     * {@link #PARTNER_NONE} or {@link #PARTNER_AMBIGUOUS} when it does not pair.
     */
    public static int partner(int[] channels, int self) {
        if (channels == null || self < 0 || self >= channels.length || channels[self] <= 0) {
            return PARTNER_NONE;
        }
        int found = PARTNER_NONE;
        int want = channels[self];
        for (int i = 0; i < channels.length; i++) {
            if (i == self || channels[i] != want) {
                continue;
            }
            if (found != PARTNER_NONE) {
                return PARTNER_AMBIGUOUS;
            }
            found = i;
        }
        return found;
    }

    /** Eight lapis offsets. The side the door faces is open. */
    public static List<Offset> lapisOffsets(Facing facing) {
        List<Offset> offsets = new ArrayList<>(8);
        if (facing == null) {
            return offsets;
        }
        offsets.add(new Offset(0, -1, 0));
        offsets.add(new Offset(0, 2, 0));
        for (Facing dir : Facing.values()) {
            if (dir == facing) {
                continue;
            }
            offsets.add(new Offset(dir.x, -1, dir.z));
            offsets.add(new Offset(dir.x, 0, dir.z));
        }
        return List.copyOf(offsets);
    }

    /** Chest on the back wall, level with the top half of the door. */
    public static Offset chestOffset(Facing facing) {
        if (facing == null) {
            return null;
        }
        Facing back = facing.opposite();
        return new Offset(back.x, 1, back.z);
    }

    public static String key(Place place) {
        if (place == null
                || place.dimension == null
                || place.dimension.isEmpty()
                || place.dimension.indexOf('|') >= 0
                || place.facing == null) {
            return null;
        }
        return place.dimension()
                + "|"
                + place.x()
                + "|"
                + place.y()
                + "|"
                + place.z()
                + "|"
                + place.facing.wire;
    }

    public static Place parse(String raw) {
        if (raw == null) {
            return null;
        }
        String[] parts = raw.split("\\|", -1);
        if (parts.length != 5 || parts[0].isEmpty()) {
            return null;
        }
        Facing facing = Facing.byName(parts[4]);
        if (facing == null) {
            return null;
        }
        try {
            return new Place(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), facing);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static int largest(List<Stack> slots, String itemId) {
        int best = -1;
        for (int i = 0; i < slots.size(); i++) {
            Stack slot = slots.get(i);
            if (!itemId.equals(slot.itemId()) || slot.count() <= 0) {
                continue;
            }
            if (best < 0) {
                best = i;
                continue;
            }
            Stack current = slots.get(best);
            if (slot.count() > current.count() || (slot.count() == current.count() && slot.index() < current.index())) {
                best = i;
            }
        }
        return best;
    }
}
