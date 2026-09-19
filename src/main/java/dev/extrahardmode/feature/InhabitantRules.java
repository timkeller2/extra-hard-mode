package dev.extrahardmode.feature;

import java.util.ArrayList;
import java.util.List;

/**
 * Residence volume, amenity score, spacing, and dawn chance. Minecraft-free for JUnit.
 */
public final class InhabitantRules {
    public static final int MIN_VOLUME = 24;
    public static final int MAX_VOLUME = 250;
    public static final int FILL_CAP = 512;
    public static final int MIN_LIGHT = 8;
    public static final int MIN_SCORE = 12;
    public static final int FINE_SCORE = 18;
    public static final int NOTABLE_SCORE = 24;
    public static final int SPACING = 48;
    public static final int BASE_CHANCE_PERCENT = 8;
    public static final int CHANCE_PER_POINT = 2;
    public static final int MAX_CHANCE_PERCENT = 40;
    public static final int LEAVE_GRACE_DAYS = 3;
    public static final int KILL_COOLDOWN_DAYS = 7;
    public static final int WANDER_RANGE = 16;
    public static final int SNAP_HOME_RANGE = 32;
    /**
     * Standing cells relative to the bed to try first. Horizontal floor spots
     * (2-high rooms) before {@code bed.above()}, which clips a 2-block ceiling.
     */
    public static final int[][] STAND_OFFSETS = {
        {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},
        {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1},
        {2, 0, 0}, {-2, 0, 0}, {0, 0, 2}, {0, 0, -2},
        {0, 1, 0}
    };

    public static final String HAULER = "hauler";
    public static final String COOK = "cook";
    public static final String FARM = "farm";
    public static final String BOUNTY = "bounty";
    public static final String TRADER = "trader";

    public static final List<String> SPECIALTIES = List.of(HAULER, COOK, FARM, BOUNTY, TRADER);

    private static final String[] NAMES = {
        "Alder", "Bram", "Cora", "Dune", "Ellis", "Fern", "Goss", "Hela", "Ivo", "Jules",
        "Kade", "Lumen", "Mira", "Noll", "Osa", "Pell", "Quinn", "Ryn", "Sage", "Tess"
    };

    public record AmenityCounts(
            int windows,
            int art,
            int carpets,
            int seating,
            int storage,
            int workstations,
            int extraLights,
            int plants,
            int books,
            int extraBeds,
            boolean kitchen) {}

    public record GateResult(
            boolean enclosed,
            int volume,
            boolean volumeOk,
            boolean hasBed,
            boolean doorOutside,
            boolean lit,
            boolean roofed,
            boolean farEnough,
            int score,
            List<String> missing) {
        public boolean eligible() {
            return enclosed
                    && volumeOk
                    && hasBed
                    && doorOutside
                    && lit
                    && roofed
                    && farEnough
                    && score >= MIN_SCORE;
        }
    }

    private InhabitantRules() {}

    public static boolean volumeOk(int volume) {
        return volume >= MIN_VOLUME && volume <= MAX_VOLUME;
    }

    /** Fill hit the cap with more air still queued: open to the world or a palace. */
    public static boolean fillOpen(int visited, boolean moreQueued) {
        return visited >= FILL_CAP && moreQueued;
    }

    public static boolean enclosed(int visited, boolean moreQueued) {
        return visited > 0 && !fillOpen(visited, moreQueued);
    }

    /**
     * Doors, trapdoors, and fence gates bound the room; they must not be flood-fill
     * volume or an open door leaks into the world and looks unenclosed.
     */
    public static boolean interiorCell(boolean opening, boolean rug, boolean airOrReplaceable, boolean emptyCollision) {
        if (opening) {
            return false;
        }
        return rug || airOrReplaceable || emptyCollision;
    }

    public static int capped(int value, int cap) {
        return Math.max(0, Math.min(cap, value));
    }

    public static int score(AmenityCounts counts) {
        if (counts == null) {
            return 0;
        }
        int total = 0;
        total += capped(counts.windows() * 2, 6);
        total += capped(counts.art(), 4);
        total += capped(counts.carpets() / 4, 3);
        total += capped(counts.seating(), 2);
        total += capped(counts.storage(), 2);
        total += capped(counts.workstations(), 2);
        total += capped(counts.extraLights(), 2);
        total += capped(counts.plants(), 2);
        total += capped(counts.books(), 2);
        if (counts.extraBeds() > 0) {
            total += 1;
        }
        if (counts.kitchen()) {
            total += 2;
        }
        return total;
    }

    public static List<String> missing(
            boolean enclosed,
            int volume,
            boolean hasBed,
            boolean doorOutside,
            boolean lit,
            boolean roofed,
            boolean farEnough,
            int score) {
        List<String> reasons = new ArrayList<>();
        if (!enclosed) {
            reasons.add("not enclosed");
        } else if (!volumeOk(volume)) {
            if (volume < MIN_VOLUME) {
                reasons.add("too small");
            } else {
                reasons.add("too large");
            }
        }
        if (!hasBed) {
            reasons.add("a bed");
        }
        if (!doorOutside) {
            reasons.add("a door to the outside");
        }
        if (!lit) {
            reasons.add("brighter lighting");
        }
        if (!roofed) {
            reasons.add("a roof");
        }
        if (!farEnough) {
            reasons.add("too close to another resident");
        }
        if (score < MIN_SCORE) {
            reasons.add("more furnishings (see /ehm help homes)");
        }
        return reasons;
    }

    public static GateResult gates(
            boolean enclosed,
            int volume,
            boolean hasBed,
            boolean doorOutside,
            boolean lit,
            boolean roofed,
            boolean farEnough,
            AmenityCounts counts) {
        int scored = score(counts);
        return new GateResult(
                enclosed,
                volume,
                volumeOk(volume),
                hasBed,
                doorOutside,
                lit,
                roofed,
                farEnough,
                scored,
                missing(enclosed, volume, hasBed, doorOutside, lit, roofed, farEnough, scored));
    }

    public static String inspectFallback(GateResult result) {
        if (result == null) {
            return "This is not a room.";
        }
        String head = "This room: " + result.score() + "/" + MIN_SCORE + " points.";
        if (result.eligible()) {
            return head + " Eligible for a resident.";
        }
        if (result.missing().isEmpty()) {
            return head + " Not yet eligible. Type /ehm help homes for the checklist.";
        }
        return head + " Missing: " + String.join(", ", result.missing()) + ".";
    }

    public static List<String> homesHelpLines() {
        return List.of(
                "Homes: Right-click a bed with a clock to score the room. Type /ehm help homes anytime for this list.",
                "Need all of these: an enclosed room (solid walls, floor, and roof; doors and trapdoors are openings, not holes), 24 to 250 interior air blocks, a bed, a door or fence gate that faces outside, block light 8 on every floor tile, and 48 blocks from another occupied home.",
                "Furnishings need 12 points. Windows (glass or panes looking out of the room): 2 each, max 6. Art (paintings and filled item frames): 1 each, max 4. Rugs (wool carpets): 1 point per 4 carpets, max 3. Seating (stairs and slabs): 1 each, max 2.",
                "Storage (chests, barrels, shulker boxes): 1 each, max 2. Workstations (crafting table, furnace, smoker, anvil, and similar): 1 each, max 2. Extra lights (torches, lanterns, glowstone, campfires): 1 each, max 2. Plants (pots, flowers, saplings): 1 each, max 2. Books (bookshelf, lectern): 1 each, max 2. A second bed: +1. Kitchen (a furnace, smoker, or campfire AND a cauldron): +2.",
                "Eligible loaded homes are checked at dawn. Chance starts at 8% at 12 points and rises with extra points (halved in blight). Chests bias a hauler, a kitchen biases a cook, plants or workstations bias a farm neighbor, and a well-furnished room can attract a bounty board. One resident per home.");
    }

    public static boolean farEnough(int dx, int dz, int spacing) {
        long min = Math.max(0, spacing);
        return (long) dx * dx + (long) dz * dz >= min * min;
    }

    public static int spawnChancePercent(int score, boolean blight) {
        return spawnChancePercent(
                score, MIN_SCORE, BASE_CHANCE_PERCENT, CHANCE_PER_POINT, MAX_CHANCE_PERCENT, blight);
    }

    public static int spawnChancePercent(
            int score, int minScore, int base, int perPoint, int cap, boolean blight) {
        int min = Math.max(0, minScore);
        if (score < min) {
            return 0;
        }
        int chance = Math.max(0, base) + Math.max(0, perPoint) * (score - min);
        chance = Math.min(Math.max(0, cap), Math.max(0, chance));
        if (blight) {
            chance /= 2;
        }
        return chance;
    }

    /** {@code roll} is 0–99. */
    public static boolean spawnRoll(int roll, int percent) {
        if (percent <= 0) {
            return false;
        }
        if (percent >= 100) {
            return true;
        }
        return roll < percent;
    }

    public static String pickSpecialty(AmenityCounts counts, int score, int roll) {
        List<String> bag = new ArrayList<>();
        bag.add(TRADER);
        bag.add(TRADER);
        if (counts != null && counts.storage() >= 1) {
            bag.add(HAULER);
            bag.add(HAULER);
        }
        if (counts != null && counts.kitchen()) {
            bag.add(COOK);
            bag.add(COOK);
            bag.add(COOK);
        }
        if (counts != null && (counts.plants() >= 1 || counts.workstations() >= 1)) {
            bag.add(FARM);
            bag.add(FARM);
        }
        if (score >= FINE_SCORE) {
            bag.add(BOUNTY);
        }
        if (score >= NOTABLE_SCORE) {
            bag.add(BOUNTY);
            bag.add(BOUNTY);
        }
        int index = Math.floorMod(roll, bag.size());
        return bag.get(index);
    }

    public static String specialtyFallback(String specialty) {
        if (specialty == null) {
            return "traveler";
        }
        return switch (specialty) {
            case HAULER -> "hauler";
            case COOK -> "cook";
            case FARM -> "farm neighbor";
            case BOUNTY -> "bounty board";
            case TRADER -> "trader";
            default -> "traveler";
        };
    }

    public static String pickName(int roll) {
        return NAMES[Math.floorMod(roll, NAMES.length)];
    }

    /** Cobble (and similar dumps) per emerald. More chests → better rate. */
    public static int haulerStack(int storage) {
        return storage >= 2 ? 16 : 32;
    }

    public static int farmWheatBuy(boolean blight) {
        return blight ? 8 : 16;
    }

    public static long leaveAfterDay(long today) {
        return Math.max(0L, today) + LEAVE_GRACE_DAYS;
    }

    public static long emptyUntilDay(long today) {
        return Math.max(0L, today) + KILL_COOLDOWN_DAYS;
    }

    public static boolean shouldLeave(long today, long leaveAfterDay) {
        return leaveAfterDay >= 0L && today >= leaveAfterDay;
    }

    public static boolean spawnBlocked(long today, long emptyUntilDay) {
        return emptyUntilDay >= 0L && today < emptyUntilDay;
    }

    /**
     * Dawn is a new overworld day and at least one survival player is in this
     * dimension. Otherwise the day is left unconsumed so a later return still
     * gets that dawn's roll.
     */
    public static boolean shouldAttemptDawn(long lastDawnDay, long day, boolean anySurvivalPlayer) {
        return lastDawnDay != day && anySurvivalPlayer;
    }

    /** Two walkable cells (feet and head) over a solid block. */
    public static boolean villagerFits(boolean feetWalkable, boolean headWalkable, boolean solidBelow) {
        return feetWalkable && headWalkable && solidBelow;
    }
}
