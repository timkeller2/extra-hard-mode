package dev.extrahardmode.feature;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ore leads for an old miner. Minecraft-free for JUnit.
 * Only ores found in his chunk and the eight around it can be named.
 * The draw is the easiest of those he has not finished. Finishing an easier ore lets the next harder one there join the list.
 * A finished ore stays out until every ore in that ground has been finished.
 * An expired lead that the player never reached was never finished, so that ore can be drawn again.
 */
public final class OldMinerRules {
    public static final int INTERVAL_DAYS = 7;
    /** One emerald in the main hand for each this many house points, rounded down. */
    public static final int FEE_POINTS = 8;
    public static final int ARRIVE_BLOCKS = 3;
    public static final int ARRIVE_DIST_SQR = ARRIVE_BLOCKS * ARRIVE_BLOCKS;
    public static final int OFFER_DIVISOR = 5;
    /** Easiest Detect Ore families first. A mission list is a slice of the ones present in the miner's ground. */
    public static final List<String> ORES = List.of(
            "coal",
            "quartz",
            "copper",
            "iron",
            "redstone",
            "lapis",
            "gold",
            "emerald",
            "diamond",
            "ancient_debris");

    private static final int[][] NEIGHBORS = {
        {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };

    private OldMinerRules() {}

    /** One emerald for each {@link #FEE_POINTS} house points, rounded down. A short house is free. */
    public static int leadFee(int housePoints) {
        if (housePoints <= 0) {
            return 0;
        }
        return housePoints / FEE_POINTS;
    }

    /** A fee of zero is already paid. Otherwise the main hand must be emeralds enough for {@code fee}. */
    public static boolean holdsLeadFee(boolean emeraldItem, int count, int fee) {
        if (fee <= 0) {
            return true;
        }
        return emeraldItem && count >= fee;
    }

    /** What he says before the emeralds are in hand. The direction stays back. */
    public static String priceLine(String name, String family, int fee) {
        String who = name == null || name.isBlank() ? "Miner" : name.trim();
        int cost = Math.max(0, fee);
        String hold = cost == 1 ? "Hold 1 emerald." : "Hold " + cost + " emeralds.";
        return who + ": " + oreTitle(family) + ". " + hold;
    }

    /** Shown when the emeralds leave the main hand. */
    public static String paidLine(int fee) {
        int cost = Math.max(0, fee);
        if (cost == 1) {
            return "You pay 1 emerald.";
        }
        return "You pay " + cost + " emeralds.";
    }

    /** Ceiling of {@code (house + successes) / 5}, capped at the ore list. Zero when that sum is not positive. */
    public static int offerCount(int houseValue, int successes) {
        long sum = (long) houseValue + Math.max(0, successes);
        if (sum <= 0) {
            return 0;
        }
        long count = (sum + OFFER_DIVISOR - 1) / OFFER_DIVISOR;
        return (int) Math.min(count, ORES.size());
    }

    /** Detect-order families from {@code present}. Ores that are not in the ground are left out. */
    public static List<String> oresPresent(Collection<String> present) {
        if (present == null || present.isEmpty()) {
            return List.of();
        }
        Set<String> found = new HashSet<>();
        for (String ore : present) {
            if (ore != null && !ore.isEmpty()) {
                found.add(ore);
            }
        }
        List<String> ordered = new ArrayList<>();
        for (String ore : ORES) {
            if (found.contains(ore)) {
                ordered.add(ore);
            }
        }
        return List.copyOf(ordered);
    }

    /**
     * Ores he can name now. The length is {@link #offerCount}. They are the easiest ores still in the ground
     * that this player has not finished. Finishing an easier ore lets the next harder ore there take its place.
     * When every ore in the ground has been finished, the cycle starts over.
     */
    public static List<String> missionOres(
            int houseValue, int successes, Collection<String> present, Collection<String> finished) {
        int want = offerCount(houseValue, successes);
        List<String> ground = oresPresent(present);
        if (want <= 0 || ground.isEmpty()) {
            return List.of();
        }
        Set<String> done = finishedPresent(ground, finished);
        if (done.size() >= ground.size()) {
            done = Set.of();
        }
        List<String> open = new ArrayList<>();
        for (String ore : ground) {
            if (!done.contains(ore)) {
                open.add(ore);
            }
        }
        if (open.isEmpty()) {
            return List.of();
        }
        return List.copyOf(open.subList(0, Math.min(want, open.size())));
    }

    public static List<String> offerPool(int houseValue, int successes, Collection<String> present) {
        return missionOres(houseValue, successes, present, List.of());
    }

    /**
     * One draw from {@link #missionOres}. {@code poolSize} is that list's length, which is how many extra blocks the vein gains.
     * {@code attempted} ores were already missing in this same offer and are not drawn again.
     */
    public record Offer(String ore, int poolSize, List<String> finished) {}

    public static Offer pickOffer(
            int houseValue,
            int successes,
            Collection<String> present,
            List<String> finished,
            Set<String> attempted,
            int roll) {
        List<String> ground = oresPresent(present);
        List<String> missions = missionOres(houseValue, successes, ground, finished);
        if (missions.isEmpty()) {
            return null;
        }
        List<String> open = new ArrayList<>();
        for (String ore : missions) {
            if (attempted != null && attempted.contains(ore)) {
                continue;
            }
            open.add(ore);
        }
        if (open.isEmpty()) {
            return null;
        }
        Set<String> done = finishedPresent(ground, finished);
        List<String> kept;
        if (done.size() >= ground.size()) {
            kept = List.of();
        } else {
            kept = new ArrayList<>();
            for (String ore : ground) {
                if (done.contains(ore)) {
                    kept.add(ore);
                }
            }
        }
        return new Offer(open.get(Math.floorMod(roll, open.size())), missions.size(), List.copyOf(kept));
    }

    private static Set<String> finishedPresent(List<String> ground, Collection<String> finished) {
        Set<String> inGround = new HashSet<>(ground);
        LinkedHashSet<String> done = new LinkedHashSet<>();
        if (finished == null) {
            return done;
        }
        for (String ore : finished) {
            if (ore != null && inGround.contains(ore)) {
                done.add(ore);
            }
        }
        return done;
    }

    public record Spot(int x, int y, int z) {}

    /**
     * Largest 6-connected group. Equal sizes prefer the group whose closest block is nearer the bed,
     * then the closer block with the lower x, then z, then y.
     * Callers pass one ore family, so deepslate and stone variants are already the same group.
     */
    public static List<Spot> largestVein(List<Spot> spots, int bedX, int bedY, int bedZ) {
        if (spots == null || spots.isEmpty()) {
            return List.of();
        }
        Map<Long, Spot> byPos = new java.util.LinkedHashMap<>();
        for (Spot spot : spots) {
            if (spot == null) {
                continue;
            }
            byPos.putIfAbsent(pack(spot.x(), spot.y(), spot.z()), spot);
        }
        if (byPos.isEmpty()) {
            return List.of();
        }
        Set<Long> visited = new HashSet<>();
        List<Spot> best = List.of();
        long bestDist = Long.MAX_VALUE;
        int bestX = 0;
        int bestY = 0;
        int bestZ = 0;
        boolean any = false;
        for (Spot start : byPos.values()) {
            long startKey = pack(start.x(), start.y(), start.z());
            if (!visited.add(startKey)) {
                continue;
            }
            List<Spot> cluster = new ArrayList<>();
            ArrayDeque<Spot> queue = new ArrayDeque<>();
            queue.add(start);
            while (!queue.isEmpty()) {
                Spot spot = queue.removeFirst();
                cluster.add(spot);
                for (int[] delta : NEIGHBORS) {
                    int nx = spot.x() + delta[0];
                    int ny = spot.y() + delta[1];
                    int nz = spot.z() + delta[2];
                    long key = pack(nx, ny, nz);
                    Spot neighbor = byPos.get(key);
                    if (neighbor == null || !visited.add(key)) {
                        continue;
                    }
                    queue.add(neighbor);
                }
            }
            Spot near = closest(cluster, bedX, bedY, bedZ);
            long dist = distSqr(near.x(), near.y(), near.z(), bedX, bedY, bedZ);
            boolean better = !any
                    || cluster.size() > best.size()
                    || (cluster.size() == best.size()
                            && (dist < bestDist
                                    || (dist == bestDist && nearer(near.x(), near.y(), near.z(), bestX, bestY, bestZ))));
            if (better) {
                any = true;
                best = cluster;
                bestDist = dist;
                bestX = near.x();
                bestY = near.y();
                bestZ = near.z();
            }
        }
        return List.copyOf(best);
    }

    /** Closest block of the vein to the bed. Same tie break as {@link #largestVein}. */
    public static Spot anchor(List<Spot> vein, int bedX, int bedY, int bedZ) {
        return closest(vein, bedX, bedY, bedZ);
    }

    public static boolean arrived(int px, int py, int pz, int vx, int vy, int vz) {
        return distSqr(px, py, pz, vx, vy, vz) <= ARRIVE_DIST_SQR;
    }

    /**
     * Single furthest axis from the player to the vein. Positive x is East, positive z is South, positive y is Up.
     * Ties prefer East or West, then South or North, then Up or Down.
     */
    public static String direction(int dx, int dy, int dz) {
        int ax = Math.abs(dx);
        int ay = Math.abs(dy);
        int az = Math.abs(dz);
        if (ax == 0 && ay == 0 && az == 0) {
            return "Here";
        }
        if (ax >= az && ax >= ay) {
            return dx > 0 ? "East" : "West";
        }
        if (az >= ay) {
            return dz > 0 ? "South" : "North";
        }
        return dy > 0 ? "Up" : "Down";
    }

    public static String oreTitle(String family) {
        String label = AbilityRules.oreFamilyLabel(family);
        if (label == null || label.isEmpty() || "ore".equals(label)) {
            return "Ore";
        }
        StringBuilder out = new StringBuilder(label.length());
        boolean cap = true;
        for (int i = 0; i < label.length(); i++) {
            char c = label.charAt(i);
            if (c == ' ') {
                out.append(c);
                cap = true;
            } else if (cap) {
                out.append(Character.toUpperCase(c));
                cap = false;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    public static String leadLine(String name, String family, String direction) {
        String who = name == null || name.isBlank() ? "Miner" : name.trim();
        return who + ": " + oreTitle(family) + " " + direction;
    }

    /**
     * Block id to place when growing a vein into {@code hostId}, or empty when that block stays as it is.
     * Quartz and ancient debris grow only into netherrack, blackstone, or basalt.
     */
    public static String oreBlockFor(String family, String hostId) {
        if (family == null || family.isEmpty() || hostId == null || hostId.isEmpty()) {
            return "";
        }
        String host = path(hostId);
        boolean deep = "deepslate".equals(host) || "tuff".equals(host);
        boolean stone = "stone".equals(host) || "granite".equals(host) || "diorite".equals(host) || "andesite".equals(host);
        boolean nether = "netherrack".equals(host) || "blackstone".equals(host) || "basalt".equals(host);
        if ("ancient_debris".equals(family)) {
            return nether ? "minecraft:ancient_debris" : "";
        }
        if ("quartz".equals(family)) {
            return nether ? "minecraft:nether_quartz_ore" : "";
        }
        if (!stoneFamily(family)) {
            return "";
        }
        if ("gold".equals(family) && nether) {
            return "minecraft:nether_gold_ore";
        }
        if (deep) {
            return "minecraft:deepslate_" + family + "_ore";
        }
        if (stone) {
            return "minecraft:" + family + "_ore";
        }
        return "";
    }

    public static List<String> parseFinished(String raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<String> ores = new ArrayList<>();
        for (String part : raw.split(",")) {
            if (!part.isEmpty()) {
                ores.add(part);
            }
        }
        return List.copyOf(ores);
    }

    /** Unique families in detect order, then anything else in encounter order. */
    public static String formatFinished(Collection<String> ores) {
        if (ores == null || ores.isEmpty()) {
            return "";
        }
        LinkedHashSet<String> have = new LinkedHashSet<>();
        for (String ore : ores) {
            if (ore != null && !ore.isEmpty()) {
                have.add(ore);
            }
        }
        StringBuilder out = new StringBuilder();
        for (String ore : ORES) {
            if (!have.remove(ore)) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(',');
            }
            out.append(ore);
        }
        for (String extra : have) {
            if (!out.isEmpty()) {
                out.append(',');
            }
            out.append(extra);
        }
        return out.toString();
    }

    public static long daysUntil(long today, long nextDay) {
        return Math.max(0L, nextDay - today);
    }

    public static List<String> hudLines(
            List<Lead> leads, String dimension, int px, int py, int pz, long today, long clockTime) {
        if (leads == null || leads.isEmpty()) {
            return List.of();
        }
        List<Lead> active = new ArrayList<>();
        for (Lead lead : leads) {
            if (lead == null || !lead.active()) {
                continue;
            }
            if (lead.expireDay() >= 0L && today >= lead.expireDay()) {
                continue;
            }
            if (dimension == null || !dimension.equals(lead.dimension())) {
                continue;
            }
            if (arrived(px, py, pz, lead.x(), lead.y(), lead.z())) {
                continue;
            }
            active.add(lead);
        }
        active.sort(Comparator.comparing(Lead::minerName, Comparator.nullsFirst(String::compareTo))
                .thenComparing(Lead::minerId, Comparator.nullsFirst(String::compareTo)));
        List<String> lines = new ArrayList<>(active.size());
        for (Lead lead : active) {
            lines.add(MissionTimerRules.append(
                    leadLine(
                            lead.minerName(),
                            lead.ore(),
                            direction(lead.x() - px, lead.y() - py, lead.z() - pz)),
                    lead.expireDay(),
                    clockTime));
        }
        return List.copyOf(lines);
    }

    /**
     * One player's lead from one old miner. {@code finished} is the cycle of ores already completed.
     * {@code ore} is empty when no vein is chosen. A quoted vein keeps a negative expire day until the fee is paid.
     * The next offer waits until {@code nextDay} even if this one ends early.
     */
    public record Lead(
            String minerId,
            String minerName,
            int successes,
            String finished,
            String ore,
            int x,
            int y,
            int z,
            String dimension,
            long givenDay,
            long expireDay,
            long nextDay,
            int fee) {
        public Lead {
            minerId = minerId == null ? "" : minerId;
            minerName = minerName == null ? "" : minerName;
            finished = finished == null ? "" : finished;
            ore = ore == null ? "" : ore;
            dimension = dimension == null ? "" : dimension;
            successes = Math.max(0, successes);
            fee = Math.max(0, fee);
        }

        public static Lead blank(String minerId, String minerName) {
            return new Lead(minerId, minerName, 0, "", "", 0, 0, 0, "", -1L, -1L, -1L, 0);
        }

        /** A paid lead. A quoted vein keeps {@code expireDay} negative until the emeralds are paid. */
        public boolean active() {
            return !ore.isEmpty() && expireDay >= 0L;
        }

        /** The ore is chosen and the direction is still unpaid. */
        public boolean quoted() {
            return !ore.isEmpty() && expireDay < 0L;
        }

        public List<String> finishedOres() {
            return parseFinished(finished);
        }

        public Lead named(String minerId, String minerName) {
            return new Lead(
                    minerId, minerName, successes, finished, ore, x, y, z, dimension, givenDay, expireDay, nextDay, fee);
        }

        public Lead withProgress(int successes, List<String> finishedOres) {
            return new Lead(
                    minerId,
                    minerName,
                    successes,
                    formatFinished(finishedOres),
                    "",
                    0,
                    0,
                    0,
                    "",
                    -1L,
                    -1L,
                    nextDay,
                    0);
        }

        public Lead assigned(String ore, int x, int y, int z, String dimension, long today) {
            long due = today + INTERVAL_DAYS;
            return new Lead(
                    minerId, minerName, successes, finished, ore == null ? "" : ore, x, y, z, dimension, today, due, due, fee);
        }

        /** The vein is chosen. The direction waits until {@link #paid}. */
        public Lead quoted(String ore, int x, int y, int z, String dimension, long today, int fee) {
            return new Lead(
                    minerId,
                    minerName,
                    successes,
                    finished,
                    ore == null ? "" : ore,
                    x,
                    y,
                    z,
                    dimension == null ? "" : dimension,
                    today,
                    -1L,
                    nextDay,
                    fee);
        }

        /** The emeralds were in hand. The 7 days start now. */
        public Lead paid(long today) {
            if (!quoted()) {
                return this;
            }
            long due = today + INTERVAL_DAYS;
            return new Lead(minerId, minerName, successes, finished, ore, x, y, z, dimension, today, due, due, fee);
        }

        public Lead waiting(long today) {
            return new Lead(
                    minerId, minerName, successes, finished, "", 0, 0, 0, "", givenDay, -1L, today + INTERVAL_DAYS, 0);
        }

        /** The player reached the vein. That ore stays finished for this cycle. */
        public Lead completed() {
            if (!active()) {
                return this;
            }
            List<String> done = new ArrayList<>(finishedOres());
            done.add(ore);
            return new Lead(
                    minerId,
                    minerName,
                    successes + 1,
                    formatFinished(done),
                    "",
                    0,
                    0,
                    0,
                    "",
                    givenDay,
                    -1L,
                    nextDay,
                    0);
        }

        /** Seven days passed without a visit. The ore is not finished, so it can be offered again. */
        public Lead expired() {
            if (!active()) {
                return this;
            }
            return new Lead(minerId, minerName, successes, finished, "", 0, 0, 0, "", givenDay, -1L, nextDay, 0);
        }
    }

    private static boolean stoneFamily(String family) {
        return switch (family) {
            case "coal", "copper", "iron", "redstone", "lapis", "gold", "emerald", "diamond" -> true;
            default -> false;
        };
    }

    private static String path(String id) {
        int slash = id.indexOf(':');
        return slash >= 0 ? id.substring(slash + 1) : id;
    }

    private static Spot closest(List<Spot> spots, int bedX, int bedY, int bedZ) {
        Spot best = null;
        long bestDist = Long.MAX_VALUE;
        if (spots == null) {
            return null;
        }
        for (Spot spot : spots) {
            if (spot == null) {
                continue;
            }
            long dist = distSqr(spot.x(), spot.y(), spot.z(), bedX, bedY, bedZ);
            if (best == null
                    || dist < bestDist
                    || (dist == bestDist && nearer(spot.x(), spot.y(), spot.z(), best.x(), best.y(), best.z()))) {
                best = spot;
                bestDist = dist;
            }
        }
        return best;
    }

    private static boolean nearer(int x, int y, int z, int otherX, int otherY, int otherZ) {
        if (x != otherX) {
            return x < otherX;
        }
        if (z != otherZ) {
            return z < otherZ;
        }
        return y < otherY;
    }

    private static long distSqr(int x1, int y1, int z1, int x2, int y2, int z2) {
        long dx = (long) x1 - x2;
        long dy = (long) y1 - y2;
        long dz = (long) z1 - z2;
        return dx * dx + dy * dy + dz * dz;
    }

    /** Vanilla block-pos packing. Y uses 12 bits, x and z use 26. */
    private static long pack(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) y & 0xFFFL) << 26 | ((long) z & 0x3FFFFFFL);
    }
}
