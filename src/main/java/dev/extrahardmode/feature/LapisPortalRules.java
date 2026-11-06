package dev.extrahardmode.feature;

import java.util.ArrayList;
import java.util.List;

/**
 * Lapis portal linking and fuel. Minecraft-free so JUnit can cover it.
 *
 * <p>A booth is a door with lapis under it, above it, and beside both halves
 * on both sides and on the side the door faces. The opening, opposite that
 * facing, stays clear. The chest sits on top of the facing lapis column. The
 * largest lapis lazuli stack is the channel. Fuel is redstone dust, nether
 * quartz, and amethyst shards from that chest.
 */
public final class LapisPortalRules {
    public static final String LAPIS_LAZULI = "minecraft:lapis_lazuli";
    public static final String REDSTONE = "minecraft:redstone";
    public static final String QUARTZ = "minecraft:quartz";
    public static final String AMETHYST = "minecraft:amethyst_shard";

    /** Tie order when the largest stacks match: nether quartz, redstone dust, amethyst shards. */
    public static final List<String> FUELS = List.of(QUARTZ, REDSTONE, AMETHYST);

    /** Blocks one fuel item covers when the chest holds one fuel kind. */
    public static final int BLOCKS_PER_FUEL = 100;

    public static final int PARTNER_NONE = -1;
    public static final int PARTNER_AMBIGUOUS = -2;
    /** Players this close to either booth hear the thunder. */
    public static final int THUNDER_BLOCKS = 200;
    /** Volume above 1 extends the thunder without making it louder than 1. 16 × this reaches {@link #THUNDER_BLOCKS}. */
    public static final float THUNDER_VOLUME = THUNDER_BLOCKS / 16.0F;
    public static final int SICKNESS_SECONDS = 10;
    public static final int SICKNESS_TICKS = SICKNESS_SECONDS * 20;
    public static final int RECHARGE_SECONDS = 15;
    public static final int RECHARGE_TICKS = RECHARGE_SECONDS * 20;

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

    /** Fuel kinds in the chest before anything is spent. Lapis is not a kind. */
    public static int fuelKinds(List<Stack> slots) {
        int kinds = 0;
        for (String fuel : FUELS) {
            if (total(slots, fuel) >= 1) {
                kinds++;
            }
        }
        return kinds;
    }

    /** 100 blocks for one kind, 200 for two, and 300 for all three. An empty chest still prices at 100. */
    public static int blocksPerFuel(int kinds) {
        int present = Math.max(1, Math.min(FUELS.size(), kinds));
        return BLOCKS_PER_FUEL * present;
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

    /** Euclidean distance. Exactly {@link #THUNDER_BLOCKS} still hears it. */
    public static boolean hearsThunder(double blocks) {
        return blocks >= 0.0 && blocks <= THUNDER_BLOCKS;
    }

    /** Game time when this booth may travel again. */
    public static long readyAt(long now) {
        return now + RECHARGE_TICKS;
    }

    public static boolean recharging(long readyAt, long now) {
        return readyAt > now;
    }

    /** Whole seconds still to wait, rounded up. Zero when the booth is ready. */
    public static int secondsLeft(long readyAt, long now) {
        long ticks = readyAt - now;
        if (ticks <= 0L) {
            return 0;
        }
        return (int) ((ticks + 19L) / 20L);
    }

    public static String rechargeWait(int seconds) {
        int left = Math.max(0, seconds);
        if (left == 1) {
            return "1 second remains.";
        }
        return left + " seconds remain.";
    }

    /** Booth identity without the door facing, so a turned door keeps its recharge. */
    public static String boothKey(String dimension, int x, int y, int z) {
        if (dimension == null || dimension.isEmpty()) {
            return null;
        }
        return dimension + "|" + x + "|" + y + "|" + z;
    }

    /**
     * {@code ceil(distance / blocksPerFuel(kinds))}, and at least 1 when the distance is positive.
     * {@code kinds} is how many fuel kinds are already in the chest.
     */
    public static int travelCost(double distance, int kinds) {
        if (distance <= 0.0) {
            return 0;
        }
        int cost = (int) Math.ceil(distance / (double) blocksPerFuel(kinds));
        return Math.max(1, cost);
    }

    /**
     * Present fuels, largest stack first. Equal stacks follow {@link #FUELS}: nether quartz, then
     * redstone dust, then amethyst shards.
     */
    public static List<String> fuelOrder(List<Stack> slots) {
        List<String> present = new ArrayList<>();
        for (String fuel : FUELS) {
            if (total(slots, fuel) >= 1) {
                present.add(fuel);
            }
        }
        present.sort((left, right) -> {
            int byStack = Integer.compare(largestCount(slots, right), largestCount(slots, left));
            if (byStack != 0) {
                return byStack;
            }
            return Integer.compare(FUELS.indexOf(left), FUELS.indexOf(right));
        });
        return List.copyOf(present);
    }

    /**
     * Spend {@code cost} fuel items, alternating kinds in {@link #fuelOrder}. A kind that runs out
     * is skipped. Each item comes off the largest stack of that kind, and a tie uses the lowest
     * slot. Returns null when the chest cannot pay. Does not change {@code slots}.
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
        int held = 0;
        for (String fuel : FUELS) {
            held += total(working, fuel);
        }
        if (held < cost) {
            return null;
        }
        List<String> order = new ArrayList<>(fuelOrder(working));
        int cursor = 0;
        for (int left = cost; left > 0; left--) {
            String take = nextFuel(working, order, cursor);
            int index = take == null ? -1 : largest(working, take);
            if (index < 0) {
                return null;
            }
            Stack chosen = working.get(index);
            working.set(index, new Stack(chosen.index(), chosen.itemId(), chosen.count() - 1));
            cursor = (order.indexOf(take) + 1) % order.size();
        }
        return List.copyOf(working);
    }

    public static String spentPhrase(int redstone, int quartz, int amethyst) {
        List<String> parts = new ArrayList<>();
        if (quartz > 0) {
            parts.add(quartz + " nether quartz");
        }
        if (redstone > 0) {
            parts.add(redstone + " redstone dust");
        }
        if (amethyst > 0) {
            parts.add(amethyst + (amethyst == 1 ? " amethyst shard" : " amethyst shards"));
        }
        if (parts.isEmpty()) {
            return "";
        }
        if (parts.size() == 1) {
            return parts.get(0);
        }
        if (parts.size() == 2) {
            return parts.get(0) + " and " + parts.get(1);
        }
        return parts.get(0) + ", " + parts.get(1) + ", and " + parts.get(2);
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

    /**
     * Eight lapis offsets. Posts stand beside both halves on both sides and on
     * the side the door faces. The opening, opposite that facing, stays clear.
     */
    public static List<Offset> lapisOffsets(Facing facing) {
        List<Offset> offsets = new ArrayList<>(8);
        if (facing == null) {
            return offsets;
        }
        offsets.add(new Offset(0, -1, 0));
        offsets.add(new Offset(0, 2, 0));
        Facing entrance = facing.opposite();
        for (Facing dir : Facing.values()) {
            if (dir == entrance) {
                continue;
            }
            offsets.add(new Offset(dir.x, 0, dir.z));
            offsets.add(new Offset(dir.x, 1, dir.z));
        }
        return List.copyOf(offsets);
    }

    /** Chest on top of the lapis column on the side the door faces. */
    public static Offset chestOffset(Facing facing) {
        if (facing == null) {
            return null;
        }
        return new Offset(facing.x, 2, facing.z);
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

    private static String nextFuel(List<Stack> slots, List<String> order, int start) {
        if (order.isEmpty()) {
            return null;
        }
        for (int step = 0; step < order.size(); step++) {
            String fuel = order.get(Math.floorMod(start + step, order.size()));
            if (total(slots, fuel) > 0) {
                return fuel;
            }
        }
        return null;
    }

    private static int largestCount(List<Stack> slots, String itemId) {
        int best = 0;
        if (slots == null) {
            return 0;
        }
        for (Stack slot : slots) {
            if (slot != null && itemId.equals(slot.itemId()) && slot.count() > best) {
                best = slot.count();
            }
        }
        return best;
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
