package dev.extrahardmode.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Residence volume, amenity score, spacing, and dawn chance. Minecraft-free for JUnit.
 */
public final class InhabitantRules {
    public static final int MIN_VOLUME = 24;
    public static final int MAX_VOLUME = 300;
    public static final int FILL_CAP = 512;
    public static final int MIN_LIGHT = 8;
    public static final int MIN_SCORE = 12;
    public static final int FINE_SCORE = 18;
    public static final int NOTABLE_SCORE = 24;
    public static final int MASTER_SCORE = 30;
    public static final int ART_SCORE_CAP = 6;
    public static final int VOLUME_PER_POINT = 48;
    public static final int VOLUME_POINT_CAP = 5;
    public static final int ROOM_POINT_CAP = 3;
    public static final int SPACING = 48;
    public static final int BASE_CHANCE_PERCENT = 8;
    public static final int CHANCE_PER_POINT = 2;
    public static final int MAX_CHANCE_PERCENT = 40;
    public static final int LEAVE_GRACE_DAYS = 3;
    public static final int KILL_COOLDOWN_DAYS = 7;
    /** Days between restocking inhabitant merchant uses. Armor uses {@link #ARMOR_RESTOCK_DAYS}. */
    public static final int RESTOCK_DAYS = 14;
    public static final int ARMOR_RESTOCK_DAYS = 30;
    public static final int ARMOR_TRADE_USES = 1;
    public static final int WEALTHY_OFFER_COUNT = 8;
    public static final int WEALTHY_TRADE_USES = 4;
    /**
     * Shop roll top is {@link #TRADE_UPPER_AT_FLOOR}% at this score and
     * {@link #TRADE_UPPER_AT_CAP}% at {@link #MAX_HOUSE_SCORE}. Lower scores use the floor.
     */
    public static final int TRADE_SCORE_FLOOR = 14;
    public static final int TRADE_UPPER_AT_FLOOR = 50;
    public static final int TRADE_UPPER_AT_CAP = 175;
    /** Furnishings 30 + volume 5 + rooms 3. */
    public static final int MAX_HOUSE_SCORE = 38;
    public static final int MAX_TRADE_TYPES = 24;
    public static final int MASTER_ARMOR_PRICE_FACTOR = 4;
    public static final int IRON_BOOTS_EMERALDS = 12;
    public static final int IRON_HELMET_EMERALDS = 15;
    public static final int IRON_LEGGINGS_EMERALDS = 21;
    public static final int IRON_CHESTPLATE_EMERALDS = 24;
    public static final int WANDER_RANGE = 16;
    public static final int SNAP_HOME_RANGE = 32;
    /** Player visit scan around a bed, in blocks. */
    public static final int VISIT_RADIUS = 48;
    /** One arrival roll per this many overworld days. */
    public static final int ARRIVAL_INTERVAL_DAYS = 1;
    public static final int MAX_CATCH_UP_ROLLS = 365;
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
    public static final String WEALTHY = "wealthy";
    public static final String ARMORSMITH = "armorsmith";
    public static final String MASTER_ARMORSMITH = "master_armorsmith";
    public static final String WISE_TEACHER = "wise_teacher";
    public static final String COUNCIL = "council";
    /** Emeralds a wise teacher charges to teach their one ability. */
    public static final int WISE_ABILITY_EMERALDS = 48;

    public static final List<String> SPECIALTIES = List.of(
            HAULER,
            COOK,
            FARM,
            BOUNTY,
            TRADER,
            WEALTHY,
            ARMORSMITH,
            MASTER_ARMORSMITH,
            WISE_TEACHER,
            COUNCIL);

    public record WealthyListing(String itemId, int count, int emeralds) {}

    /**
     * One inhabitant listing. {@code buy} true: the player sells {@code count} of the
     * item for one emerald. False: the player pays {@code emeralds} for {@code count}.
     */
    public record TradeListing(String itemId, int count, int emeralds, int maxUses, boolean buy) {
        public TradeListing withUses(int uses) {
            return new TradeListing(itemId, count, emeralds, Math.max(1, uses), buy);
        }
    }

    public static final List<WealthyListing> WEALTHY_POOL = List.of(
            new WealthyListing("minecraft:iron_ingot", 8, 2),
            new WealthyListing("minecraft:gold_ingot", 4, 2),
            new WealthyListing("minecraft:copper_ingot", 12, 1),
            new WealthyListing("minecraft:diamond", 1, 8),
            new WealthyListing("minecraft:iron_block", 1, 6),
            new WealthyListing("minecraft:ender_pearl", 2, 3),
            new WealthyListing("minecraft:name_tag", 1, 10),
            new WealthyListing("minecraft:saddle", 1, 6),
            new WealthyListing("minecraft:golden_apple", 1, 4),
            new WealthyListing("minecraft:obsidian", 4, 2),
            new WealthyListing("minecraft:lapis_lazuli", 12, 1),
            new WealthyListing("minecraft:redstone", 16, 1),
            new WealthyListing("minecraft:quartz", 12, 1),
            new WealthyListing("minecraft:book", 2, 1),
            new WealthyListing("minecraft:experience_bottle", 2, 5),
            new WealthyListing("minecraft:arrow", 24, 1),
            new WealthyListing("minecraft:cooked_beef", 8, 1),
            new WealthyListing("minecraft:oak_log", 16, 1),
            new WealthyListing("minecraft:glass", 16, 1),
            new WealthyListing("minecraft:glowstone", 8, 2),
            new WealthyListing("minecraft:packed_ice", 8, 2),
            new WealthyListing("minecraft:slime_ball", 8, 2),
            new WealthyListing("minecraft:leather", 12, 1),
            new WealthyListing("minecraft:string", 16, 1),
            new WealthyListing("minecraft:gunpowder", 8, 2),
            new WealthyListing("minecraft:blaze_rod", 2, 3),
            new WealthyListing("minecraft:ghast_tear", 1, 8),
            new WealthyListing("minecraft:nether_wart", 8, 1),
            new WealthyListing("minecraft:amethyst_shard", 8, 1),
            new WealthyListing("minecraft:coal", 16, 1),
            new WealthyListing("minecraft:lantern", 8, 1),
            new WealthyListing("minecraft:phantom_membrane", 2, 4));

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
        total += capped(counts.art(), ART_SCORE_CAP);
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

    public static int volumePoints(int volume) {
        return capped(Math.max(0, volume) / VOLUME_PER_POINT, VOLUME_POINT_CAP);
    }

    public static int roomPoints(int rooms) {
        return capped(rooms, ROOM_POINT_CAP);
    }

    public static int score(AmenityCounts counts, int volume, int rooms) {
        return score(counts) + volumePoints(volume) + roomPoints(rooms);
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
            reasons.add("more furnishings (see /tougher help homes)");
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
        return gates(enclosed, volume, hasBed, doorOutside, lit, roofed, farEnough, counts, 0);
    }

    public static GateResult gates(
            boolean enclosed,
            int volume,
            boolean hasBed,
            boolean doorOutside,
            boolean lit,
            boolean roofed,
            boolean farEnough,
            AmenityCounts counts,
            int rooms) {
        int scored = score(counts, volume, rooms);
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
            return head + " Not yet eligible. Type /tougher help homes for the checklist.";
        }
        return head + " Missing: " + String.join(", ", result.missing()) + ".";
    }

    public static List<String> homesIntroLines() {
        return List.of(
                "Homes: Right-click a bed with a clock to score the room. Type /tougher help homes anytime for this list.",
                "Homes: An enclosed, well-lit room with a bed, a door, and furnishings may attract one traveler. They dislike crowding.",
                "Homes: Subjects: rooms, furnishings, residents, shops, bounties. Each is also /tougher help <subject>.",
                "Homes: Residents may buy spare blocks, trade food, post bounties, keep a shop, teach a mana ability, or sell armor.",
                "Homes: Staff can toggle residents with /tougher set inhabitants true (requires operator).");
    }

    public static List<String> homesRoomLines() {
        return List.of(
                "Rooms: Need an enclosed room (solid walls, floor, and roof; doors and trapdoors are openings, not holes).",
                "Rooms: Also need 24 to 300 interior air blocks, a bed, and a door or fence gate that faces outside.",
                "Rooms: Block light 8 on every floor tile, and stay 48 blocks from another occupied home.");
    }

    public static List<String> homesFurnishingLines() {
        return List.of(
                "Furnishings: Furnishings need 12 points. Each 48 interior air blocks: 1 point, max 5.",
                "Furnishings: Each enclosed room connected by doors, trapdoors, or fence gates: 1 point, max 3.",
                "Furnishings: Windows (glass or panes looking out of the room): 2 each, max 6.",
                "Furnishings: Art (paintings and filled item frames): 1 each, max 6. Rugs (wool carpets): 1 point per 4 carpets, max 3.",
                "Furnishings: Seating (stairs and slabs): 1 each, max 2. Storage (chests, barrels, shulker boxes): 1 each, max 2.",
                "Furnishings: Workstations (crafting table, furnace, smoker, anvil, and similar): 1 each, max 2.",
                "Furnishings: Extra lights (torches, lanterns, glowstone, campfires): 1 each, max 2. Plants (pots, flowers, saplings): 1 each, max 2.",
                "Furnishings: Books (bookshelf, lectern): 1 each, max 2. A second bed: +1.",
                "Furnishings: Kitchen (a furnace, smoker, or campfire AND a cauldron): +2.");
    }

    public static List<String> homesResidentLines() {
        return List.of(
                "Residents: Eligible loaded homes are checked at dawn. An empty eligible home is timestamped the day it becomes inhabitable.",
                "Residents: When you visit later, each missed day is rolled once (same chances as dawn), then the timestamp is set to today.",
                "Residents: Houses in unloaded chunks can still fill. Chance starts at 8% at 12 points and rises with extra points (halved in blight).",
                "Residents: Chests bias a hauler, a kitchen biases a cook, and plants or workstations bias a farm neighbor.",
                "Residents: 18 points can attract a bounty board or wealthy trader, 24 an armorsmith or a wise teacher, and 30 a master armorsmith.",
                "Residents: A council member can appear in any eligible home. New residents prefer a type that has not appeared yet.",
                "Residents: The house must be able to host that type. Once every type has spawned, the usual furnishing biases apply.",
                "Residents: Council members are three times as likely as the most common other type. One resident per home.",
                "Residents: Most residents restock every 14 Minecraft days; armorsmiths restock every 30 days.",
                "Residents: Look at a resident who is waiting to restock to see how long is left, in days and hours like a torch.");
    }

    public static List<String> homesShopLines() {
        return List.of(
                "Shops: Shop size follows house points. The top of the roll is 50% at 14 points and 175% at 38, the highest score.",
                "Shops: The bottom of that roll is half the top. A 14-point home rolls 25-50% of the usual trade types and of each listing's uses.",
                "Shops: Houses under 14 points use that same 25-50% band. There is at least one use of each listing that is rolled.",
                "Shops: Each listing rolls its uses separately. Extra types beyond the usual shop are random trades.");
    }

    public static List<String> homesBountyLines() {
        return List.of(
                "Bounties: Council members give each player a personal kill bounty when you check in.",
                "Bounties: The first hunt is 6-18 common mobs; each success is 30% larger and rarer. You have 7 Minecraft days.",
                "Bounties: Finishing grants XP equal to one quarter of the slain mobs' health plus 25, with 10% more per extra house point.",
                "Bounties: You also get a congratulations message and a sound. Return to collect emeralds equal to one tenth of that XP.",
                "Bounties: Collecting plays a sound and offers a new bounty. Zombie Bounty 3/12 sits above the bottom-left ability icons.");
    }

    public static List<String> homesHelpLines() {
        List<String> lines = new ArrayList<>();
        lines.addAll(homesIntroLines());
        lines.addAll(homesRoomLines());
        lines.addAll(homesFurnishingLines());
        lines.addAll(homesResidentLines());
        lines.addAll(homesShopLines());
        lines.addAll(homesBountyLines());
        return List.copyOf(lines);
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

    public static List<String> unseenSpecialties(Collection<String> alreadySpawned) {
        return unseenSpecialties(alreadySpawned, Integer.MAX_VALUE);
    }

    public static List<String> unseenSpecialties(Collection<String> alreadySpawned, int score) {
        List<String> unseen = new ArrayList<>();
        for (String type : SPECIALTIES) {
            if (alreadySpawned != null && alreadySpawned.contains(type)) {
                continue;
            }
            if (!specialtyAllowed(type, score)) {
                continue;
            }
            unseen.add(type);
        }
        return unseen;
    }

    public static int minScoreForSpecialty(String type) {
        if (type == null) {
            return Integer.MAX_VALUE;
        }
        return switch (type) {
            case MASTER_ARMORSMITH -> MASTER_SCORE;
            case ARMORSMITH, WISE_TEACHER -> NOTABLE_SCORE;
            case WEALTHY, BOUNTY -> FINE_SCORE;
            default -> MIN_SCORE;
        };
    }

    public static boolean specialtyAllowed(String type, int score) {
        return score >= minScoreForSpecialty(type);
    }

    public static String pickSpecialty(AmenityCounts counts, int score, int roll) {
        return pickSpecialty(counts, score, roll, List.of());
    }

    /**
     * Prefers a specialty that has never spawned and that this house can host.
     * Once every eligible type has appeared, uses the amenity-weighted bag.
     */
    public static String pickSpecialty(
            AmenityCounts counts, int score, int roll, Collection<String> alreadySpawned) {
        List<String> unseen = unseenSpecialties(alreadySpawned, score);
        if (!unseen.isEmpty()) {
            return unseen.get(Math.floorMod(roll, unseen.size()));
        }
        return pickSpecialtyFromBag(counts, score, roll);
    }

    static String pickSpecialtyFromBag(AmenityCounts counts, int score, int roll) {
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
            bag.add(WEALTHY);
            bag.add(WEALTHY);
        }
        if (score >= NOTABLE_SCORE) {
            bag.add(BOUNTY);
            bag.add(BOUNTY);
            bag.add(ARMORSMITH);
            bag.add(ARMORSMITH);
            bag.add(WISE_TEACHER);
            bag.add(WISE_TEACHER);
        }
        if (score >= MASTER_SCORE) {
            bag.add(MASTER_ARMORSMITH);
            bag.add(MASTER_ARMORSMITH);
            bag.add(MASTER_ARMORSMITH);
        }
        if (specialtyAllowed(COUNCIL, score)) {
            HashMap<String, Integer> freq = new HashMap<>();
            int maxOther = 1;
            for (String type : bag) {
                int n = freq.merge(type, 1, Integer::sum);
                if (n > maxOther) {
                    maxOther = n;
                }
            }
            int copies = CouncilMissionRules.councilBagCopies(maxOther);
            for (int i = 0; i < copies; i++) {
                bag.add(COUNCIL);
            }
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
            case WEALTHY -> "wealthy trader";
            case ARMORSMITH -> "armorsmith";
            case MASTER_ARMORSMITH -> "master armorsmith";
            case WISE_TEACHER -> "wise teacher";
            case COUNCIL -> "council member";
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
     * Arrival rolls owed since the home was timestamped. Zero until the first
     * stamp (the day it became inhabitable) and when {@code today} is not later.
     */
    public static int missedArrivalRolls(long lastRollDay, long today) {
        if (lastRollDay < 0L || today <= lastRollDay) {
            return 0;
        }
        long missed = today - lastRollDay;
        if (missed > MAX_CATCH_UP_ROLLS) {
            return MAX_CATCH_UP_ROLLS;
        }
        return (int) missed;
    }

    /** Which ability this teacher offers. Stable for a given roll. */
    public static String pickWiseAbility(int roll) {
        List<String> ids = AbilityRules.ABILITY_IDS;
        return ids.get(Math.floorMod(roll, ids.size()));
    }

    public static boolean shouldTeachAbility(boolean alreadyKnown, int emeralds, int cost) {
        return !alreadyKnown && emeralds >= Math.max(1, cost);
    }

    /** Sneak-right-click, once per player per teacher. */
    public static boolean shouldTeachMana(boolean sneaking, boolean hasDiamondBlock, boolean hasLapisBlock, boolean already) {
        return sneaking && hasDiamondBlock && hasLapisBlock && !already;
    }

    public static boolean alreadyTookManaLesson(Collection<String> teacherIds, String teacherId) {
        return teacherId != null && !teacherId.isEmpty() && teacherIds != null && teacherIds.contains(teacherId);
    }

    public static int restockDays(String specialty) {
        if (ARMORSMITH.equals(specialty) || MASTER_ARMORSMITH.equals(specialty)) {
            return ARMOR_RESTOCK_DAYS;
        }
        return RESTOCK_DAYS;
    }

    public static int armorEmeralds(boolean master, String slot) {
        int iron = switch (slot == null ? "" : slot) {
            case "boots" -> IRON_BOOTS_EMERALDS;
            case "helmet" -> IRON_HELMET_EMERALDS;
            case "leggings" -> IRON_LEGGINGS_EMERALDS;
            case "chestplate" -> IRON_CHESTPLATE_EMERALDS;
            default -> 0;
        };
        return master ? iron * MASTER_ARMOR_PRICE_FACTOR : iron;
    }

    /** Unique listings from {@link #WEALTHY_POOL}; {@code seed} picks which eight. */
    public static List<WealthyListing> pickWealthyListings(int seed) {
        List<WealthyListing> pool = new ArrayList<>(WEALTHY_POOL);
        int n = pool.size();
        int want = Math.min(WEALTHY_OFFER_COUNT, n);
        for (int i = 0; i < want; i++) {
            int j = i + Math.floorMod(seed * 31 + i * 17, n - i);
            WealthyListing swap = pool.get(i);
            pool.set(i, pool.get(j));
            pool.set(j, swap);
        }
        return List.copyOf(pool.subList(0, want));
    }

    public static int extraTradePoints(int score, String specialty) {
        return Math.max(0, score - minScoreForSpecialty(specialty == null || specialty.isEmpty() ? TRADER : specialty));
    }

    /**
     * Top of the shop roll. 50% at {@link #TRADE_SCORE_FLOOR} points, 175% at
     * {@link #MAX_HOUSE_SCORE}. Scores outside that span clamp.
     */
    public static int tradeMaxPercent(int score) {
        int clamped = Math.clamp(score, TRADE_SCORE_FLOOR, MAX_HOUSE_SCORE);
        int span = MAX_HOUSE_SCORE - TRADE_SCORE_FLOOR;
        int rise = TRADE_UPPER_AT_CAP - TRADE_UPPER_AT_FLOOR;
        return TRADE_UPPER_AT_FLOOR + (int) Math.round((clamped - TRADE_SCORE_FLOOR) * (rise / (double) span));
    }

    /** Bottom of the shop roll: half the top, rounded. */
    public static int tradeMinPercent(int score) {
        return (int) Math.round(tradeMaxPercent(score) / 2.0);
    }

    /** Inclusive roll in {@code [minPercent, maxPercent]}. */
    public static int rollTradePercent(int minPercent, int maxPercent, int roll) {
        int min = Math.max(0, minPercent);
        int max = Math.max(min, maxPercent);
        return min + Math.floorMod(roll, max - min + 1);
    }

    /** {@code defaultAmount} scaled by {@code percent}, at least 1 when the default is positive. */
    public static int scaledAmount(int defaultAmount, int percent) {
        if (defaultAmount <= 0) {
            return 0;
        }
        long scaled = Math.round(defaultAmount * (Math.max(0, percent) / 100.0));
        if (scaled > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, (int) scaled);
    }

    public static int defaultTradeTypeCount(String specialty) {
        if (WEALTHY.equals(specialty)) {
            return WEALTHY_OFFER_COUNT;
        }
        return defaultListings(specialty, 0, false).size();
    }

    public static List<TradeListing> defaultListings(String specialty, int storage, boolean blight) {
        String type = specialty == null || specialty.isEmpty() ? TRADER : specialty;
        int stack = haulerStack(storage);
        return switch (type) {
            case HAULER -> List.of(
                    buyOf("minecraft:cobblestone", stack, 4),
                    buyOf("minecraft:dirt", stack, 4),
                    buyOf("minecraft:gravel", stack, 4),
                    buyOf("minecraft:cobbled_deepslate", stack, 4),
                    buyOf("minecraft:netherrack", stack, 4));
            case COOK -> List.of(
                    buyOf("minecraft:cooked_beef", 8, 4),
                    buyOf("minecraft:cooked_porkchop", 8, 4),
                    buyOf("minecraft:cooked_chicken", 8, 4),
                    buyOf("minecraft:baked_potato", 8, 4),
                    sellOf("minecraft:rabbit_stew", 1, 4, 4));
            case FARM -> List.of(
                    buyOf("minecraft:wheat", farmWheatBuy(blight), 6),
                    sellOf("minecraft:wheat_seeds", 4, 1, 6),
                    sellOf("minecraft:oak_sapling", 1, 2, 4));
            case BOUNTY -> List.of(
                    buyOf("minecraft:spider_eye", 8, 6),
                    buyOf("minecraft:gunpowder", 8, 6),
                    buyOf("minecraft:bone", 16, 6));
            case ARMORSMITH -> armorListings(false);
            case MASTER_ARMORSMITH -> armorListings(true);
            case WISE_TEACHER -> List.of();
            case COUNCIL -> List.of(
                    sellOf("minecraft:paper", 12, 1, 8),
                    sellOf("minecraft:book", 1, 3, 4),
                    sellOf("minecraft:compass", 1, 5, 4),
                    sellOf("minecraft:clock", 1, 5, 4));
            case WEALTHY -> wealthyTradeListings().subList(0, Math.min(WEALTHY_OFFER_COUNT, WEALTHY_POOL.size()));
            default -> List.of(
                    sellOf("minecraft:bread", 4, 1, 8),
                    sellOf("minecraft:coal", 8, 1, 8),
                    sellOf("minecraft:book", 1, 4, 4),
                    sellOf("minecraft:white_wool", 8, 1, 8));
        };
    }

    public static List<TradeListing> extraListings(String specialty, int storage, boolean blight) {
        String type = specialty == null || specialty.isEmpty() ? TRADER : specialty;
        int stack = haulerStack(storage);
        return switch (type) {
            case HAULER -> List.of(
                    buyOf("minecraft:andesite", stack, 4),
                    buyOf("minecraft:diorite", stack, 4),
                    buyOf("minecraft:granite", stack, 4),
                    buyOf("minecraft:tuff", stack, 4),
                    buyOf("minecraft:sand", stack, 4),
                    buyOf("minecraft:red_sand", stack, 4),
                    buyOf("minecraft:mossy_cobblestone", stack, 4),
                    buyOf("minecraft:stone", stack, 4),
                    buyOf("minecraft:deepslate", stack, 4),
                    buyOf("minecraft:basalt", stack, 4),
                    buyOf("minecraft:blackstone", stack, 4),
                    buyOf("minecraft:end_stone", stack, 4),
                    buyOf("minecraft:soul_sand", stack, 4),
                    buyOf("minecraft:calcite", stack, 4));
            case COOK -> List.of(
                    buyOf("minecraft:cooked_mutton", 8, 4),
                    buyOf("minecraft:cooked_rabbit", 8, 4),
                    buyOf("minecraft:cooked_cod", 8, 4),
                    buyOf("minecraft:cooked_salmon", 8, 4),
                    buyOf("minecraft:bread", 8, 4),
                    buyOf("minecraft:cookie", 16, 4),
                    sellOf("minecraft:mushroom_stew", 1, 3, 4),
                    sellOf("minecraft:beetroot_soup", 1, 3, 4),
                    sellOf("minecraft:pumpkin_pie", 1, 3, 4),
                    sellOf("minecraft:honey_bottle", 1, 4, 4));
            case FARM -> List.of(
                    sellOf("minecraft:beetroot_seeds", 4, 1, 6),
                    sellOf("minecraft:pumpkin_seeds", 4, 1, 6),
                    sellOf("minecraft:melon_seeds", 4, 1, 6),
                    sellOf("minecraft:carrot", 8, 1, 6),
                    sellOf("minecraft:potato", 8, 1, 6),
                    sellOf("minecraft:birch_sapling", 1, 2, 4),
                    sellOf("minecraft:spruce_sapling", 1, 2, 4),
                    sellOf("minecraft:bone_meal", 4, 1, 6),
                    buyOf("minecraft:beetroot", farmWheatBuy(blight), 6),
                    buyOf("minecraft:apple", 8, 6),
                    buyOf("minecraft:sweet_berries", 16, 6),
                    buyOf("minecraft:sugar_cane", 16, 6));
            case BOUNTY -> List.of(
                    buyOf("minecraft:rotten_flesh", 16, 6),
                    buyOf("minecraft:string", 16, 6),
                    buyOf("minecraft:slime_ball", 8, 6),
                    buyOf("minecraft:leather", 12, 6),
                    buyOf("minecraft:arrow", 24, 6),
                    buyOf("minecraft:ink_sac", 12, 6),
                    buyOf("minecraft:phantom_membrane", 4, 6),
                    buyOf("minecraft:ender_pearl", 4, 6),
                    buyOf("minecraft:blaze_rod", 4, 6),
                    buyOf("minecraft:ghast_tear", 2, 6));
            case ARMORSMITH -> List.of(
                    sellOf("minecraft:iron_ingot", 8, 2, ARMOR_TRADE_USES),
                    sellOf("minecraft:shield", 1, 5, ARMOR_TRADE_USES),
                    sellOf("minecraft:iron_nugget", 16, 1, ARMOR_TRADE_USES),
                    sellOf("minecraft:lava_bucket", 1, 3, ARMOR_TRADE_USES),
                    sellOf("minecraft:chainmail_helmet", 1, 8, ARMOR_TRADE_USES),
                    sellOf("minecraft:chainmail_boots", 1, 6, ARMOR_TRADE_USES));
            case WISE_TEACHER -> List.of();
            case MASTER_ARMORSMITH -> List.of(
                    sellOf("minecraft:diamond", 1, 8, ARMOR_TRADE_USES),
                    sellOf("minecraft:diamond_horse_armor", 1, 14, ARMOR_TRADE_USES),
                    sellOf("minecraft:netherite_scrap", 1, 16, ARMOR_TRADE_USES),
                    sellOf("minecraft:shield", 1, 8, ARMOR_TRADE_USES));
            case COUNCIL -> List.of(
                    sellOf("minecraft:map", 1, 4, 4),
                    sellOf("minecraft:name_tag", 1, 8, 4),
                    sellOf("minecraft:writable_book", 1, 4, 4),
                    sellOf("minecraft:item_frame", 4, 1, 8),
                    sellOf("minecraft:lantern", 4, 1, 8));
            case WEALTHY -> List.of();
            default -> List.of(
                    sellOf("minecraft:stick", 16, 1, 8),
                    sellOf("minecraft:paper", 12, 1, 8),
                    sellOf("minecraft:glass", 8, 1, 8),
                    sellOf("minecraft:brick", 8, 1, 8),
                    sellOf("minecraft:iron_ingot", 4, 2, 8),
                    sellOf("minecraft:copper_ingot", 8, 1, 8),
                    sellOf("minecraft:lantern", 4, 1, 8),
                    sellOf("minecraft:arrow", 16, 1, 8),
                    sellOf("minecraft:oak_log", 8, 1, 8),
                    sellOf("minecraft:torch", 8, 1, 8),
                    sellOf("minecraft:leather", 8, 1, 8),
                    sellOf("minecraft:name_tag", 1, 8, 4));
        };
    }

    public static List<TradeListing> genericExtraListings() {
        return List.of(
                sellOf("minecraft:apple", 8, 1, 6),
                sellOf("minecraft:stick", 16, 1, 8),
                sellOf("minecraft:string", 12, 1, 8),
                sellOf("minecraft:feather", 12, 1, 8),
                sellOf("minecraft:flint", 8, 1, 8),
                sellOf("minecraft:clay_ball", 16, 1, 8),
                sellOf("minecraft:paper", 8, 1, 8),
                sellOf("minecraft:glass_bottle", 8, 1, 8),
                sellOf("minecraft:bowl", 8, 1, 8),
                sellOf("minecraft:kelp", 16, 1, 8),
                sellOf("minecraft:bamboo", 16, 1, 8),
                sellOf("minecraft:egg", 8, 1, 6),
                sellOf("minecraft:charcoal", 8, 1, 8),
                sellOf("minecraft:raw_copper", 8, 1, 8),
                sellOf("minecraft:raw_iron", 4, 2, 6),
                buyOf("minecraft:rotten_flesh", 16, 8),
                buyOf("minecraft:poisonous_potato", 8, 8),
                buyOf("minecraft:cactus", 16, 8),
                buyOf("minecraft:kelp", 32, 8),
                buyOf("minecraft:bamboo", 32, 8));
    }

    /**
     * House-score shop: type count and per-listing uses roll from half the top
     * percent up to that top. The top is 50% at 14 points and 175% at 38.
     * Extra types beyond the usual shop are random leftover listings.
     */
    public static List<TradeListing> scaledListings(
            String specialty, int score, int storage, boolean blight, Random random) {
        Random rng = random == null ? new Random(0L) : random;
        String type = specialty == null || specialty.isEmpty() ? TRADER : specialty;
        if (WISE_TEACHER.equals(type)) {
            return List.of();
        }
        List<TradeListing> defaults;
        List<TradeListing> extras;
        if (WEALTHY.equals(type)) {
            List<TradeListing> pool = new ArrayList<>(wealthyTradeListings());
            Collections.shuffle(pool, rng);
            int n = Math.min(WEALTHY_OFFER_COUNT, pool.size());
            defaults = new ArrayList<>(pool.subList(0, n));
            extras = new ArrayList<>(pool.subList(n, pool.size()));
        } else {
            defaults = new ArrayList<>(defaultListings(type, storage, blight));
            extras = new ArrayList<>(extraListings(type, storage, blight));
            Collections.shuffle(defaults, rng);
        }
        extras.addAll(genericExtraListings());
        Collections.shuffle(extras, rng);
        int minPercent = tradeMinPercent(score);
        int maxPercent = tradeMaxPercent(score);
        int typePercent = rollTradePercent(minPercent, maxPercent, rng.nextInt());
        int want = Math.min(MAX_TRADE_TYPES, scaledAmount(Math.max(1, defaults.size()), typePercent));
        List<TradeListing> chosen = new ArrayList<>();
        Set<String> used = new HashSet<>();
        int takeDefaults = Math.min(want, defaults.size());
        for (int i = 0; i < takeDefaults; i++) {
            TradeListing listing = defaults.get(i);
            if (used.add(listing.itemId())) {
                chosen.add(listing);
            }
        }
        for (int i = 0; i < extras.size() && chosen.size() < want; i++) {
            TradeListing listing = extras.get(i);
            if (used.add(listing.itemId())) {
                chosen.add(listing);
            }
        }
        List<TradeListing> scaled = new ArrayList<>(chosen.size());
        for (TradeListing listing : chosen) {
            int usePercent = rollTradePercent(minPercent, maxPercent, rng.nextInt());
            scaled.add(listing.withUses(scaledAmount(listing.maxUses(), usePercent)));
        }
        return scaled;
    }

    static List<TradeListing> armorListings(boolean master) {
        int uses = ARMOR_TRADE_USES;
        String prefix = master ? "tougher:heavy_diamond_" : "tougher:heavy_iron_";
        return List.of(
                sellOf(prefix + "boots", 1, armorEmeralds(master, "boots"), uses),
                sellOf(prefix + "helmet", 1, armorEmeralds(master, "helmet"), uses),
                sellOf(prefix + "leggings", 1, armorEmeralds(master, "leggings"), uses),
                sellOf(prefix + "chestplate", 1, armorEmeralds(master, "chestplate"), uses));
    }

    static List<TradeListing> wealthyTradeListings() {
        List<TradeListing> listings = new ArrayList<>(WEALTHY_POOL.size());
        for (WealthyListing listing : WEALTHY_POOL) {
            listings.add(sellOf(listing.itemId(), listing.count(), listing.emeralds(), WEALTHY_TRADE_USES));
        }
        return listings;
    }

    static TradeListing buyOf(String itemId, int count, int uses) {
        return new TradeListing(itemId, count, 1, uses, true);
    }

    static TradeListing sellOf(String itemId, int count, int emeralds, int uses) {
        return new TradeListing(itemId, count, emeralds, uses, false);
    }

    /**
     * Ticks of overworld time until the dawn that will restock this resident.
     * {@code 0} means that dawn is already here and will run now.
     * A resident saved before restock days existed ({@code lastRestockDay < 0}), or one whose period has
     * already elapsed, waits for the next dawn that has not run yet. If today's dawn already ran, that is
     * tomorrow.
     */
    public static int restockRemainingTicks(long dayTime, long lastRestockDay, long lastDawnDay, int restockDays) {
        long time = Math.max(0L, dayTime);
        long day = CropGrowthRules.dayIndex(time);
        int period = Math.max(1, restockDays);
        boolean due = lastRestockDay < 0L || day >= lastRestockDay + period;
        long nextDay = due ? (lastDawnDay == day ? day + 1L : day) : lastRestockDay + period;
        long remaining = nextDay * CropGrowthRules.TICKS_PER_DAY - time;
        if (remaining <= 0L) {
            return 0;
        }
        if (remaining > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) remaining;
    }

    /** True when trades have never restocked, or {@code days} have passed. */
    public static boolean shouldRestock(long lastRestockDay, long day) {
        return shouldRestock(lastRestockDay, day, RESTOCK_DAYS);
    }

    public static boolean shouldRestock(long lastRestockDay, long day, int days) {
        if (lastRestockDay < 0L) {
            return true;
        }
        return day >= lastRestockDay + Math.max(1, days);
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
