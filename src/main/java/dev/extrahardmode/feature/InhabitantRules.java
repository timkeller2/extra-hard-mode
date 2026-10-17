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
    /** One diamond. Matches a wealthy trader. */
    public static final int JEWELER_DIAMOND_EMERALDS = 8;
    /** Four gold ingots, matching a wealthy trader. */
    public static final int JEWELER_GOLD_INGOT_COUNT = 4;
    public static final int JEWELER_GOLD_EMERALDS = 2;
    /** One iron block. Nine ingots would be about 2 emeralds; the block costs more. */
    public static final int JEWELER_IRON_BLOCK_EMERALDS = 6;
    /** One copper block or one redstone block. Each is a small stack of the loose item, sold as a block. */
    public static final int JEWELER_SMALL_BLOCK_EMERALDS = 2;
    /** Nine diamonds at {@link #JEWELER_DIAMOND_EMERALDS} each. */
    public static final int JEWELER_DIAMOND_BLOCK_EMERALDS = 72;
    /** Nine gold ingots, with the same kind of premium as an iron block. */
    public static final int JEWELER_GOLD_BLOCK_EMERALDS = 12;
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
    /** Usual farm shop sells this many plant listings, drawn from {@link #FARM_PLANT_SELLS}. */
    public static final int FARM_USUAL_SELLS = 2;
    public static final int FARM_PLANT_USES = 4;
    /** One seed or plantable vegetable. The pool uses both ends. */
    public static final int FARM_PLANT_MIN_EMERALDS = 6;
    public static final int FARM_PLANT_MAX_EMERALDS = 16;
    public static final int MASTER_ARMOR_PRICE_FACTOR = 4;
    public static final int IRON_BOOTS_EMERALDS = 12;
    public static final int IRON_HELMET_EMERALDS = 15;
    public static final int IRON_LEGGINGS_EMERALDS = 21;
    public static final int IRON_CHESTPLATE_EMERALDS = 24;
    /** Enchanted iron sits under heavy iron. Half of 12, 15, 21, and 24, with the halves rounded. */
    public static final int ENCHANTED_IRON_BOOTS_EMERALDS = 6;
    public static final int ENCHANTED_IRON_HELMET_EMERALDS = 8;
    public static final int ENCHANTED_IRON_LEGGINGS_EMERALDS = 11;
    public static final int ENCHANTED_IRON_CHESTPLATE_EMERALDS = 12;
    /** Enchanting-table power applied to an armorsmith's iron armor. */
    public static final int ARMOR_ENCHANT_LEVEL = 20;
    /** Enchanting-table power applied to a master armorsmith's diamond armor. */
    public static final int MASTER_ARMOR_ENCHANT_LEVEL = 30;
    /** Iron ingots an armorsmith buys for one emerald. */
    public static final int ARMOR_IRON_BUY_COUNT = 4;
    /** Diamonds a master armorsmith buys for one emerald. */
    public static final int ARMOR_DIAMOND_BUY_COUNT = 1;
    /** Uses on the metal buy. Armor sales stay at {@link #ARMOR_TRADE_USES}. */
    public static final int ARMOR_METAL_BUY_USES = 4;
    /** Drinkable potions, including the level 2 bottle, cost one emerald. */
    public static final int BREWER_POTION_EMERALDS = 1;
    public static final int BREWER_POTION_USES = 4;
    /** Other potion effects kept in the usual shop, besides the level 2 bottle. */
    public static final int BREWER_USUAL_OTHERS = 3;
    /** Brewing ingredients are one emerald. The count on each listing is the bargain. */
    public static final int BREWER_INGREDIENT_EMERALDS = 1;
    public static final int BREWER_INGREDIENT_USES = 8;
    /** Copies in the arrival bag when the home has a cauldron. */
    public static final int BREWER_BAG_COPIES = 3;
    private static final List<String> ARMOR_SLOTS = List.of("boots", "helmet", "leggings", "chestplate");
    public static final int WANDER_RANGE = 16;
    public static final int SNAP_HOME_RANGE = 32;

    /**
     * The stored resident is gone. A loaded body that is not alive counts immediately.
     * An unloaded body counts only when the area around the bed is loaded, so a
     * resident in an unloaded chunk is not erased just because nobody is nearby.
     */
    public static boolean residentMissing(boolean found, boolean alive, boolean homeAreaLoaded) {
        if (found) {
            return !alive;
        }
        return homeAreaLoaded;
    }
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
    public static final String BREWER = "brewer";
    public static final String FARM = "farm";
    public static final String BOUNTY = "bounty";
    public static final String TRADER = "trader";
    public static final String WEALTHY = "wealthy";
    public static final String ARMORSMITH = "armorsmith";
    public static final String MASTER_ARMORSMITH = "master_armorsmith";
    public static final String WISE_TEACHER = "wise_teacher";
    public static final String JEWELER = "jeweler";
    public static final String COUNCIL = "council";
    /** Emeralds a wise teacher charges in a 24-point home. Each point above that takes {@link #WISE_PRICE_PER_POINT} off, down to free. */
    public static final int WISE_ABILITY_EMERALDS = 30;
    public static final int WISE_PRICE_PER_POINT = 2;

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
            JEWELER,
            COUNCIL,
            BREWER);

    public record WealthyListing(String itemId, int count, int emeralds) {}

    /**
     * One inhabitant listing. {@code buy} true: the player sells {@code count} of the
     * item for one emerald. False: the player pays {@code emeralds} for {@code count}.
     * {@code enchantLevel} above 0 asks for a traded-equipment enchantment at that power.
     * {@code potionId} is a potion registry id, such as {@code minecraft:strong_healing}.
     */
    public record TradeListing(
            String itemId, int count, int emeralds, int maxUses, boolean buy, int enchantLevel, String potionId) {
        public TradeListing {
            if (potionId == null) {
                potionId = "";
            }
        }

        /** Potions share one item id, so the potion id keeps those listings apart. */
        public String tradeKey() {
            return potionId.isEmpty() ? itemId : "potion:" + potionId;
        }

        public TradeListing withUses(int uses) {
            return new TradeListing(itemId, count, emeralds, Math.max(1, uses), buy, enchantLevel, potionId);
        }

        public TradeListing withCount(int nextCount) {
            return new TradeListing(itemId, Math.max(0, nextCount), emeralds, maxUses, buy, enchantLevel, potionId);
        }
    }

    /** One brewed effect. {@code family} keeps a shop from selling two strengths of the same potion. */
    public record BrewPotion(String potionId, String family) {}

    /** Level 2 potions. A restock keeps one rolled bottle from this list. */
    public static final List<BrewPotion> BREWER_LEVEL_TWO = List.of(
            new BrewPotion("minecraft:strong_healing", "healing"),
            new BrewPotion("minecraft:strong_swiftness", "swiftness"),
            new BrewPotion("minecraft:strong_strength", "strength"),
            new BrewPotion("minecraft:strong_leaping", "leaping"),
            new BrewPotion("minecraft:strong_regeneration", "regeneration"),
            new BrewPotion("minecraft:strong_harming", "harming"),
            new BrewPotion("minecraft:strong_poison", "poison"),
            new BrewPotion("minecraft:strong_turtle_master", "turtle_master"),
            new BrewPotion("minecraft:strong_slowness", "slowness"));

    /**
     * One other strength from each family, plus effects that have no level 2 bottle.
     * The usual shop keeps up to {@link #BREWER_USUAL_OTHERS} of these, never the featured family.
     */
    public static final List<BrewPotion> BREWER_OTHER_POTIONS = List.of(
            new BrewPotion("minecraft:healing", "healing"),
            new BrewPotion("minecraft:long_swiftness", "swiftness"),
            new BrewPotion("minecraft:long_strength", "strength"),
            new BrewPotion("minecraft:long_leaping", "leaping"),
            new BrewPotion("minecraft:long_regeneration", "regeneration"),
            new BrewPotion("minecraft:harming", "harming"),
            new BrewPotion("minecraft:long_poison", "poison"),
            new BrewPotion("minecraft:long_turtle_master", "turtle_master"),
            new BrewPotion("minecraft:long_slowness", "slowness"),
            new BrewPotion("minecraft:long_fire_resistance", "fire_resistance"),
            new BrewPotion("minecraft:long_night_vision", "night_vision"),
            new BrewPotion("minecraft:long_invisibility", "invisibility"),
            new BrewPotion("minecraft:long_water_breathing", "water_breathing"),
            new BrewPotion("minecraft:long_slow_falling", "slow_falling"),
            new BrewPotion("minecraft:long_weakness", "weakness"));

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

    /**
     * Seeds and plantable vegetables a farm neighbor may sell. One item, and the
     * emerald cost is hefty next to buying a stack of cobble.
     */
    public static final List<TradeListing> FARM_PLANT_SELLS = List.of(
            sellOf("minecraft:wheat_seeds", 1, 6, FARM_PLANT_USES),
            sellOf("minecraft:beetroot_seeds", 1, 6, FARM_PLANT_USES),
            sellOf("minecraft:carrot", 1, 8, FARM_PLANT_USES),
            sellOf("minecraft:potato", 1, 8, FARM_PLANT_USES),
            sellOf("minecraft:sweet_berries", 1, 8, FARM_PLANT_USES),
            sellOf("minecraft:sugar_cane", 1, 8, FARM_PLANT_USES),
            sellOf("minecraft:cocoa_beans", 1, 8, FARM_PLANT_USES),
            sellOf("minecraft:glow_berries", 1, 12, FARM_PLANT_USES),
            sellOf("minecraft:pumpkin_seeds", 1, 12, FARM_PLANT_USES),
            sellOf("minecraft:melon_seeds", 1, 12, FARM_PLANT_USES),
            sellOf("minecraft:nether_wart", 1, 12, FARM_PLANT_USES),
            sellOf("minecraft:torchflower_seeds", 1, FARM_PLANT_MAX_EMERALDS, FARM_PLANT_USES),
            sellOf("minecraft:pitcher_pod", 1, FARM_PLANT_MAX_EMERALDS, FARM_PLANT_USES));

    /** Goods a jeweler keeps in the usual shop. Larger houses draw the rest from {@link #JEWELER_EXTRA_SELLS}. */
    public static final List<TradeListing> JEWELER_USUAL_SELLS = List.of(
            sellOf("minecraft:diamond", 1, JEWELER_DIAMOND_EMERALDS, WEALTHY_TRADE_USES),
            sellOf("minecraft:redstone_block", 1, JEWELER_SMALL_BLOCK_EMERALDS, WEALTHY_TRADE_USES),
            sellOf("minecraft:gold_ingot", JEWELER_GOLD_INGOT_COUNT, JEWELER_GOLD_EMERALDS, WEALTHY_TRADE_USES),
            sellOf("minecraft:iron_block", 1, JEWELER_IRON_BLOCK_EMERALDS, WEALTHY_TRADE_USES),
            sellOf("minecraft:copper_block", 1, JEWELER_SMALL_BLOCK_EMERALDS, WEALTHY_TRADE_USES));

    /** Gem and metal listings a larger jeweler adds after the usual shop. */
    public static final List<TradeListing> JEWELER_EXTRA_SELLS = List.of(
            sellOf("minecraft:lapis_lazuli", 12, 1, WEALTHY_TRADE_USES),
            sellOf("minecraft:lapis_block", 1, JEWELER_SMALL_BLOCK_EMERALDS, WEALTHY_TRADE_USES),
            sellOf("minecraft:amethyst_shard", 8, 1, WEALTHY_TRADE_USES),
            sellOf("minecraft:amethyst_block", 1, 1, WEALTHY_TRADE_USES),
            sellOf("minecraft:quartz", 12, 1, WEALTHY_TRADE_USES),
            sellOf("minecraft:quartz_block", 1, 1, WEALTHY_TRADE_USES),
            sellOf("minecraft:gold_nugget", 9, 1, WEALTHY_TRADE_USES),
            sellOf("minecraft:gold_block", 1, JEWELER_GOLD_BLOCK_EMERALDS, WEALTHY_TRADE_USES),
            sellOf("minecraft:diamond_block", 1, JEWELER_DIAMOND_BLOCK_EMERALDS, WEALTHY_TRADE_USES),
            sellOf("minecraft:iron_ingot", 8, 2, WEALTHY_TRADE_USES),
            sellOf("minecraft:copper_ingot", 12, 1, WEALTHY_TRADE_USES));

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
            boolean kitchen,
            boolean cauldron) {}

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
                "Residents: The name over their head is their name and occupation, such as Mira the Jeweler.",
                "Residents: Clothes are a villager outfit, so two jewelers may differ. Finer residents wear higher-ranked clothes.",
                "Residents: Eligible loaded homes are checked at dawn. An empty eligible home is timestamped the day it becomes inhabitable.",
                "Residents: When you visit later, each missed day is rolled once (same chances as dawn), then the timestamp is set to today.",
                "Residents: Houses in unloaded chunks can still fill. Chance starts at 8% at 12 points and rises with extra points (halved in blight).",
                "Residents: Chests bias a hauler, a kitchen biases a cook, and plants or workstations bias a farm neighbor.",
                "Residents: A cauldron favors a brewer, and a brewer can come to a 12-point home.",
                "Residents: 18 points can attract a drop collector, a wealthy trader, or a jeweler, 24 an armorsmith or a wise teacher, and 30 a master armorsmith.",
                "Residents: A council member can appear in any eligible home. New residents prefer a type that has not appeared yet.",
                "Residents: The house must be able to host that type. Once every type has spawned, the usual furnishing biases apply.",
                "Residents: Council members are three times as likely as the most common other type. One resident per home.",
                "Residents: Most residents restock every 14 Minecraft days; armorsmiths restock every 30 days.",
                "Residents: Look at a resident who is waiting to restock to see how long is left, in days and hours like a torch.",
                "Residents: They have 40 health plus their house points, and regain 1 health each minute.",
                "Residents: Looking at one shows hearts, one per 10 health, beside the restock time.",
                "Residents: They shoot mobs that come within 12 blocks and are after them. The bow hits 3 harder than a normal arrow.",
                "Residents: A close attack makes them switch to an iron sword. A zombie cannot turn them unless they are already below 5 health.",
                "Residents: A home stops listing someone who has died or is no longer around the bed.");
    }

    public static List<String> homesShopLines() {
        return List.of(
                "Shops: Shop size follows house points. The top of the roll is 50% at 14 points and 175% at 38, the highest score.",
                "Shops: The bottom of that roll is half the top. A 14-point home rolls 25-50% of the usual trade types and of each listing's uses.",
                "Shops: Houses under 14 points use that same 25-50% band. There is at least one use of each listing that is rolled.",
                "Shops: Each listing rolls its uses separately. Extra types beyond the usual shop are random trades.",
                "Shops: Each trade adds 1 trading experience. A stock change adds the square root of that experience to the house points.",
                "Shops: Each stock change rolls item amounts once from -25% to +25%, then adds 0 to 25 for house points and trading experience.",
                "Shops: That total adds items when you buy and subtracts the items required when you sell to the resident. Emerald prices stay the same.",
                "Shops: A farm neighbor may sell one seed or plantable vegetable for 6 to 16 emeralds.",
                "Shops: A jeweler sells a diamond for 8 emeralds, 4 gold ingots for 2, an iron block for 6, and a copper or redstone block for 2.",
                "Shops: Larger jewelers also sell lapis, amethyst, quartz, gold nuggets, and blocks of gold, lapis, amethyst, quartz, and diamond. A diamond block is 72 emeralds.",
                "Shops: An armorsmith sells one heavy iron piece each restock, and the piece can change. Other sales are enchanted iron armor, each a different slot.",
                "Shops: A larger iron shop may buy iron. A master armorsmith sells one heavy diamond piece the same way, plus enchanted diamond armor, and may buy diamonds.",
                "Shops: Heavy iron costs 12, 15, 21, or 24 emeralds for boots, a helmet, leggings, or a chestplate. Heavy diamond costs four times that.",
                "Shops: Enchanted iron armor costs 6, 8, 11, or 12 emeralds for those slots. Enchanted diamond armor costs the same as heavy iron.",
                "Shops: A brewer sells one level 2 potion each restock, and that potion can change. Up to 3 other potions cost 1 emerald.",
                "Shops: A larger brewer sells brewing ingredients for 1 emerald, such as nether wart, blaze powder, glowstone, and ghast tears.");
    }

    public static List<String> homesBountyLines() {
        return List.of(
                "Bounties: Council members give each player a personal kill bounty when you check in.",
                "Bounties: The first hunt from a resident is 6-18 common mobs. Each success with that resident makes the next 20% larger. Every 3 successes, their next hunt is rarer.",
                "Bounties: You have 14 Minecraft days. A bounty that runs out is not replaced in that conversation.",
                "Bounties: The next bounty from that resident waits 7 Minecraft days after the last one was given, and only after you finish the previous one.",
                "Bounties: Each council member can give you one. You can hold 5 at a time, counting hunts and unpaid emerald turn-ins.",
                "Bounties: A kill counts for every open bounty on that mob.",
                "Bounties: Finishing grants XP equal to one quarter of the slain mobs' health plus 25, with 10% more per extra house point.",
                "Bounties: You also get a congratulations message and a sound. Return to that resident for emeralds equal to one tenth of that XP.",
                "Bounties: Collecting plays a sound. If 7 days have passed, that resident offers a new bounty.",
                "Bounties: One bounty reads Zombie Bounty 3/12 above the ability icons. Two or more name the resident and stack upward.",
                "Bounties: A drop collector warns you when a biome boss is out, and names the compass direction from that resident. East is the rising sun.");
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
            case WEALTHY, BOUNTY, JEWELER -> FINE_SCORE;
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
        if (counts != null && counts.cauldron()) {
            for (int i = 0; i < BREWER_BAG_COPIES; i++) {
                bag.add(BREWER);
            }
        }
        if (counts != null && (counts.plants() >= 1 || counts.workstations() >= 1)) {
            bag.add(FARM);
            bag.add(FARM);
        }
        if (score >= FINE_SCORE) {
            bag.add(BOUNTY);
            bag.add(WEALTHY);
            bag.add(WEALTHY);
            bag.add(JEWELER);
            bag.add(JEWELER);
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

    /** Work clothes. Stone or iron rank. */
    public static final List<String> COMMON_OUTFITS = List.of(
            "minecraft:farmer",
            "minecraft:fisherman",
            "minecraft:shepherd",
            "minecraft:fletcher",
            "minecraft:butcher",
            "minecraft:mason",
            "minecraft:leatherworker");
    /** Nicer coats and robes. Iron through emerald rank. */
    public static final List<String> FINE_OUTFITS = List.of(
            "minecraft:librarian",
            "minecraft:cartographer",
            "minecraft:cleric",
            "minecraft:toolsmith",
            "minecraft:weaponsmith",
            "minecraft:leatherworker");
    /** Smith and scholar clothes. Gold through diamond rank. */
    public static final List<String> NOTABLE_OUTFITS = List.of(
            "minecraft:armorer",
            "minecraft:weaponsmith",
            "minecraft:toolsmith",
            "minecraft:cleric",
            "minecraft:librarian");
    /** The same fine clothes, kept at emerald or diamond rank. */
    public static final List<String> ELITE_OUTFITS = List.of(
            "minecraft:armorer",
            "minecraft:weaponsmith",
            "minecraft:cleric",
            "minecraft:librarian");
    public static final List<String> OUTFIT_STYLES = List.of(
            "minecraft:plains",
            "minecraft:desert",
            "minecraft:jungle",
            "minecraft:savanna",
            "minecraft:snow",
            "minecraft:swamp",
            "minecraft:taiga");

    /** One rolled costume. {@code level} is the villager rank badge, from 1 to 5. */
    public record Outfit(String professionId, String styleId, int level) {}

    /**
     * Vanilla drops a level-1 profession that has no job block when villager
     * experience is 0, and that wipe clears the trade list. Residents keep at
     * least this much experience so the costume and the shop stay.
     */
    public static final int COSTUME_EXPERIENCE = 1;

    public static boolean vanillaClearsNoviceCostume(int level, int villagerXp) {
        return level <= 1 && villagerXp <= 0;
    }

    public static List<String> outfitProfessions(String specialty) {
        return switch (specialty == null ? "" : specialty) {
            case MASTER_ARMORSMITH, COUNCIL -> ELITE_OUTFITS;
            case ARMORSMITH, WISE_TEACHER -> NOTABLE_OUTFITS;
            case WEALTHY, BOUNTY, JEWELER -> FINE_OUTFITS;
            default -> COMMON_OUTFITS;
        };
    }

    public static int outfitLevelMin(String specialty) {
        return switch (specialty == null ? "" : specialty) {
            case MASTER_ARMORSMITH, COUNCIL -> 4;
            case ARMORSMITH, WISE_TEACHER -> 3;
            case WEALTHY, BOUNTY, JEWELER -> 2;
            default -> 1;
        };
    }

    public static int outfitLevelMax(String specialty) {
        return switch (specialty == null ? "" : specialty) {
            case MASTER_ARMORSMITH, COUNCIL -> 5;
            case ARMORSMITH, WISE_TEACHER -> 5;
            case WEALTHY, BOUNTY, JEWELER -> 4;
            default -> 2;
        };
    }

    public static Outfit pickOutfit(String specialty, int professionRoll, int styleRoll, int levelRoll) {
        List<String> professions = outfitProfessions(specialty);
        int min = outfitLevelMin(specialty);
        int span = Math.max(1, outfitLevelMax(specialty) - min + 1);
        return new Outfit(
                professions.get(Math.floorMod(professionRoll, professions.size())),
                OUTFIT_STYLES.get(Math.floorMod(styleRoll, OUTFIT_STYLES.size())),
                min + Math.floorMod(levelRoll, span));
    }

    public static String outfitKey(Outfit outfit) {
        if (outfit == null) {
            return "";
        }
        return outfit.professionId() + "|" + outfit.styleId() + "|" + outfit.level();
    }

    public static Outfit parseOutfit(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String[] parts = key.split("\\|", -1);
        if (parts.length != 3 || parts[0].isEmpty() || parts[1].isEmpty()) {
            return null;
        }
        try {
            int level = Integer.parseInt(parts[2]);
            if (level < 1 || level > 5) {
                return null;
            }
            return new Outfit(parts[0], parts[1], level);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Floating name, such as {@code Mira the Jeweler}. */
    public static String nameplate(String name, String specialty) {
        String who = name == null || name.isBlank() ? "Traveler" : name.trim();
        return who + " the " + titleWords(specialtyFallback(specialty));
    }

    static String titleWords(String words) {
        if (words == null || words.isEmpty()) {
            return "Traveler";
        }
        StringBuilder out = new StringBuilder(words.length());
        boolean cap = true;
        for (int i = 0; i < words.length(); i++) {
            char c = words.charAt(i);
            if (Character.isWhitespace(c)) {
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

    public static String specialtyFallback(String specialty) {
        if (specialty == null) {
            return "traveler";
        }
        return switch (specialty) {
            case HAULER -> "hauler";
            case COOK -> "cook";
            case BREWER -> "brewer";
            case FARM -> "farm neighbor";
            case BOUNTY -> "drop collector";
            case TRADER -> "trader";
            case WEALTHY -> "wealthy trader";
            case ARMORSMITH -> "armorsmith";
            case MASTER_ARMORSMITH -> "master armorsmith";
            case WISE_TEACHER -> "wise teacher";
            case JEWELER -> "jeweler";
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

    /** Price for this house. {@code 0} means the lesson is free. */
    public static int wiseAbilityPrice(int houseScore) {
        int above = Math.max(0, houseScore - NOTABLE_SCORE);
        return Math.max(0, WISE_ABILITY_EMERALDS - above * WISE_PRICE_PER_POINT);
    }

    /** True when this lesson has not already been bought and the player can pay. Already knowing it still qualifies. */
    public static boolean shouldTeachAbility(boolean alreadyTaught, int emeralds, int cost) {
        return !alreadyTaught && emeralds >= Math.max(0, cost);
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

    /** Enchanted diamond armor uses the heavy iron price for that slot. */
    public static int enchantedArmorEmeralds(boolean master, String slot) {
        if (master) {
            return armorEmeralds(false, slot);
        }
        return switch (slot == null ? "" : slot) {
            case "boots" -> ENCHANTED_IRON_BOOTS_EMERALDS;
            case "helmet" -> ENCHANTED_IRON_HELMET_EMERALDS;
            case "leggings" -> ENCHANTED_IRON_LEGGINGS_EMERALDS;
            case "chestplate" -> ENCHANTED_IRON_CHESTPLATE_EMERALDS;
            default -> 0;
        };
    }

    public static int armorEnchantLevel(boolean master) {
        return master ? MASTER_ARMOR_ENCHANT_LEVEL : ARMOR_ENCHANT_LEVEL;
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
        return tradeMaxPercent((double) score);
    }

    /**
     * Same roll as {@link #tradeMaxPercent(int)}. A fractional score is the house
     * points plus the square root of the resident's trading experience.
     */
    public static int tradeMaxPercent(double score) {
        double clamped = Math.clamp(score, (double) TRADE_SCORE_FLOOR, (double) MAX_HOUSE_SCORE);
        double span = MAX_HOUSE_SCORE - TRADE_SCORE_FLOOR;
        double rise = TRADE_UPPER_AT_CAP - TRADE_UPPER_AT_FLOOR;
        return TRADE_UPPER_AT_FLOOR + (int) Math.round((clamped - TRADE_SCORE_FLOOR) * (rise / span));
    }

    /** Bottom of the shop roll: half the top, rounded. */
    public static int tradeMinPercent(int score) {
        return tradeMinPercent((double) score);
    }

    public static int tradeMinPercent(double score) {
        return (int) Math.round(tradeMaxPercent(score) / 2.0);
    }

    /** Square root of completed trades. Zero trades add nothing. */
    public static double tradeExperienceBonus(int trades) {
        if (trades <= 0) {
            return 0.0;
        }
        return Math.sqrt(trades);
    }

    /** House points plus {@link #tradeExperienceBonus(int)}, used when stock is rolled. */
    public static double stockScore(int housePoints, int trades) {
        return housePoints + tradeExperienceBonus(trades);
    }

    /** Lowest random piece of an item-amount roll. */
    public static final int QUANTITY_ROLL_MIN = -25;
    /** Highest random piece of an item-amount roll. */
    public static final int QUANTITY_ROLL_MAX = 25;
    /** House and trading-experience piece of an item-amount roll, at {@link #MAX_HOUSE_SCORE}. */
    public static final int QUANTITY_MODIFIER_MAX = 25;

    /**
     * 0 at {@link #TRADE_SCORE_FLOOR} and {@link #QUANTITY_MODIFIER_MAX} at {@link #MAX_HOUSE_SCORE}.
     * The score is house points plus the square root of trading experience.
     */
    public static int quantityModifier(double score) {
        double clamped = Math.clamp(score, (double) TRADE_SCORE_FLOOR, (double) MAX_HOUSE_SCORE);
        double span = MAX_HOUSE_SCORE - TRADE_SCORE_FLOOR;
        return (int) Math.round((clamped - TRADE_SCORE_FLOOR) * (QUANTITY_MODIFIER_MAX / span));
    }

    /** Inclusive roll from {@link #QUANTITY_ROLL_MIN} to {@link #QUANTITY_ROLL_MAX}. */
    public static int rollQuantitySwing(int roll) {
        int span = QUANTITY_ROLL_MAX - QUANTITY_ROLL_MIN + 1;
        return QUANTITY_ROLL_MIN + Math.floorMod(roll, span);
    }

    /** Random swing plus the house and experience modifier. About -25 to +50. */
    public static int quantityDealPercent(int swing, int modifier) {
        int swingClamped = Math.clamp(swing, QUANTITY_ROLL_MIN, QUANTITY_ROLL_MAX);
        int modifierClamped = Math.clamp(modifier, 0, QUANTITY_MODIFIER_MAX);
        return swingClamped + modifierClamped;
    }

    /**
     * A purchase ({@code buy} false) gains {@code percent} items. A sale to the resident
     * ({@code buy} true) requires {@code percent} fewer items. At least 1 when the default is positive.
     */
    public static int adjustedQuantity(int count, int percent, boolean buy) {
        if (count <= 0) {
            return 0;
        }
        int applied = buy ? -percent : percent;
        long scaled = Math.round(count * ((100.0 + applied) / 100.0));
        if (scaled > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, (int) scaled);
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
            case FARM -> farmUsualListings(blight);
            case BREWER -> brewerUsualListings();
            case BOUNTY -> List.of(
                    buyOf("minecraft:spider_eye", 8, 6),
                    buyOf("minecraft:gunpowder", 8, 6),
                    buyOf("minecraft:bone", 16, 6));
            case ARMORSMITH -> armorUsualListings(false);
            case MASTER_ARMORSMITH -> armorUsualListings(true);
            case WISE_TEACHER -> List.of();
            case COUNCIL -> List.of(
                    sellOf("minecraft:paper", 12, 1, 8),
                    sellOf("minecraft:book", 1, 3, 4),
                    sellOf("minecraft:compass", 1, 5, 4),
                    sellOf("minecraft:clock", 1, 5, 4));
            case WEALTHY -> wealthyTradeListings().subList(0, Math.min(WEALTHY_OFFER_COUNT, WEALTHY_POOL.size()));
            case JEWELER -> JEWELER_USUAL_SELLS;
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
            case FARM -> farmExtraListings(blight);
            case BREWER -> brewerIngredientListings();
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
            case ARMORSMITH -> List.of(armorMetalBuy(false));
            case WISE_TEACHER -> List.of();
            case MASTER_ARMORSMITH -> List.of(armorMetalBuy(true));
            case COUNCIL -> List.of(
                    sellOf("minecraft:map", 1, 4, 4),
                    sellOf("minecraft:name_tag", 1, 8, 4),
                    sellOf("minecraft:writable_book", 1, 4, 4),
                    sellOf("minecraft:item_frame", 4, 1, 8),
                    sellOf("minecraft:lantern", 4, 1, 8));
            case WEALTHY -> List.of();
            case JEWELER -> JEWELER_EXTRA_SELLS;
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
     * An armorsmith keeps one rolled heavy piece, then enchanted armor of that metal.
     * A larger roll may add a buy of iron or diamonds. Generic goods are not added.
     * A brewer keeps one rolled level 2 potion, then up to three other potions.
     * A larger roll may add brewing ingredients. Generic goods are not added.
     * Item amounts share one roll from -25% to +25%, plus 0 to 25 from the score.
     * Purchases gain that percent; sales to the resident require that percent fewer.
     */
    public static List<TradeListing> scaledListings(
            String specialty, double score, int storage, boolean blight, Random random) {
        Random rng = random == null ? new Random(0L) : random;
        String type = specialty == null || specialty.isEmpty() ? TRADER : specialty;
        if (WISE_TEACHER.equals(type)) {
            return List.of();
        }
        List<TradeListing> defaults;
        List<TradeListing> extras;
        boolean addGeneric = true;
        if (WEALTHY.equals(type)) {
            List<TradeListing> pool = new ArrayList<>(wealthyTradeListings());
            Collections.shuffle(pool, rng);
            int n = Math.min(WEALTHY_OFFER_COUNT, pool.size());
            defaults = new ArrayList<>(pool.subList(0, n));
            extras = new ArrayList<>(pool.subList(n, pool.size()));
        } else if (FARM.equals(type)) {
            List<TradeListing> plants = new ArrayList<>(FARM_PLANT_SELLS);
            Collections.shuffle(plants, rng);
            int usualSells = Math.min(FARM_USUAL_SELLS, plants.size());
            defaults = new ArrayList<>();
            defaults.add(buyOf("minecraft:wheat", farmWheatBuy(blight), 6));
            defaults.addAll(plants.subList(0, usualSells));
            Collections.shuffle(defaults, rng);
            extras = new ArrayList<>(plants.subList(usualSells, plants.size()));
            extras.addAll(farmSideListings(blight));
        } else if (ARMORSMITH.equals(type) || MASTER_ARMORSMITH.equals(type)) {
            boolean master = MASTER_ARMORSMITH.equals(type);
            List<TradeListing> heavy = new ArrayList<>(armorListings(master));
            Collections.shuffle(heavy, rng);
            List<TradeListing> enchanted = new ArrayList<>(enchantedArmorListings(master));
            Collections.shuffle(enchanted, rng);
            defaults = new ArrayList<>();
            defaults.add(heavy.getFirst());
            defaults.addAll(enchanted);
            extras = new ArrayList<>(extraListings(type, storage, blight));
            addGeneric = false;
        } else if (BREWER.equals(type)) {
            List<BrewPotion> levelTwo = new ArrayList<>(BREWER_LEVEL_TWO);
            Collections.shuffle(levelTwo, rng);
            BrewPotion featured = levelTwo.getFirst();
            List<BrewPotion> others = new ArrayList<>();
            for (BrewPotion potion : BREWER_OTHER_POTIONS) {
                if (!potion.family().equals(featured.family())) {
                    others.add(potion);
                }
            }
            Collections.shuffle(others, rng);
            defaults = new ArrayList<>();
            defaults.add(potionSell(featured));
            int usual = Math.min(BREWER_USUAL_OTHERS, others.size());
            for (int i = 0; i < usual; i++) {
                defaults.add(potionSell(others.get(i)));
            }
            extras = new ArrayList<>(extraListings(type, storage, blight));
            addGeneric = false;
        } else {
            defaults = new ArrayList<>(defaultListings(type, storage, blight));
            extras = new ArrayList<>(extraListings(type, storage, blight));
            Collections.shuffle(defaults, rng);
        }
        if (addGeneric) {
            extras.addAll(genericExtraListings());
        }
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
            if (used.add(listing.tradeKey())) {
                chosen.add(listing);
            }
        }
        for (int i = 0; i < extras.size() && chosen.size() < want; i++) {
            TradeListing listing = extras.get(i);
            if (used.add(listing.tradeKey())) {
                chosen.add(listing);
            }
        }
        int quantityPercent = quantityDealPercent(rollQuantitySwing(rng.nextInt()), quantityModifier(score));
        List<TradeListing> scaled = new ArrayList<>(chosen.size());
        for (TradeListing listing : chosen) {
            int usePercent = rollTradePercent(minPercent, maxPercent, rng.nextInt());
            int count = adjustedQuantity(listing.count(), quantityPercent, listing.buy());
            scaled.add(listing.withCount(count).withUses(scaledAmount(listing.maxUses(), usePercent)));
        }
        return scaled;
    }

    /** Wheat buy plus the first plant sells. The live shop draws those sells at random. */
    static List<TradeListing> farmUsualListings(boolean blight) {
        List<TradeListing> listings = new ArrayList<>();
        listings.add(buyOf("minecraft:wheat", farmWheatBuy(blight), 6));
        int sells = Math.min(FARM_USUAL_SELLS, FARM_PLANT_SELLS.size());
        listings.addAll(FARM_PLANT_SELLS.subList(0, sells));
        return List.copyOf(listings);
    }

    /** Plant sells past the usual pair, then saplings, bone meal, and crop buys. */
    static List<TradeListing> farmExtraListings(boolean blight) {
        List<TradeListing> listings = new ArrayList<>();
        if (FARM_PLANT_SELLS.size() > FARM_USUAL_SELLS) {
            listings.addAll(FARM_PLANT_SELLS.subList(FARM_USUAL_SELLS, FARM_PLANT_SELLS.size()));
        }
        listings.addAll(farmSideListings(blight));
        return List.copyOf(listings);
    }

    static List<TradeListing> farmSideListings(boolean blight) {
        return List.of(
                sellOf("minecraft:oak_sapling", 1, 2, 4),
                sellOf("minecraft:birch_sapling", 1, 2, 4),
                sellOf("minecraft:spruce_sapling", 1, 2, 4),
                sellOf("minecraft:bone_meal", 4, 1, 6),
                buyOf("minecraft:beetroot", farmWheatBuy(blight), 6),
                buyOf("minecraft:apple", 8, 6));
    }

    /** One heavy piece of each slot. A restock keeps a single rolled piece from this list. */
    static List<TradeListing> armorListings(boolean master) {
        String prefix = master ? "tougher:heavy_diamond_" : "tougher:heavy_iron_";
        List<TradeListing> listings = new ArrayList<>();
        for (String slot : ARMOR_SLOTS) {
            listings.add(sellOf(prefix + slot, 1, armorEmeralds(master, slot), ARMOR_TRADE_USES));
        }
        return List.copyOf(listings);
    }

    /** Enchanted vanilla armor, one listing per slot, of the metal this smith works. */
    static List<TradeListing> enchantedArmorListings(boolean master) {
        String prefix = master ? "minecraft:diamond_" : "minecraft:iron_";
        int level = armorEnchantLevel(master);
        List<TradeListing> listings = new ArrayList<>();
        for (String slot : ARMOR_SLOTS) {
            listings.add(sellEnchantedOf(
                    prefix + slot, 1, enchantedArmorEmeralds(master, slot), ARMOR_TRADE_USES, level));
        }
        return List.copyOf(listings);
    }

    /**
     * Usual catalog size: one heavy piece plus enchanted armor of every slot.
     * Boots stand in for the heavy piece. The live shop rolls that piece.
     */
    static List<TradeListing> armorUsualListings(boolean master) {
        List<TradeListing> listings = new ArrayList<>();
        listings.add(armorListings(master).getFirst());
        listings.addAll(enchantedArmorListings(master));
        return List.copyOf(listings);
    }

    static TradeListing armorMetalBuy(boolean master) {
        if (master) {
            return buyOf("minecraft:diamond", ARMOR_DIAMOND_BUY_COUNT, ARMOR_METAL_BUY_USES);
        }
        return buyOf("minecraft:iron_ingot", ARMOR_IRON_BUY_COUNT, ARMOR_METAL_BUY_USES);
    }

    static List<TradeListing> wealthyTradeListings() {
        List<TradeListing> listings = new ArrayList<>(WEALTHY_POOL.size());
        for (WealthyListing listing : WEALTHY_POOL) {
            listings.add(sellOf(listing.itemId(), listing.count(), listing.emeralds(), WEALTHY_TRADE_USES));
        }
        return listings;
    }

    static TradeListing buyOf(String itemId, int count, int uses) {
        return new TradeListing(itemId, count, 1, uses, true, 0, "");
    }

    static TradeListing sellOf(String itemId, int count, int emeralds, int uses) {
        return new TradeListing(itemId, count, emeralds, uses, false, 0, "");
    }

    static TradeListing sellEnchantedOf(String itemId, int count, int emeralds, int uses, int enchantLevel) {
        return new TradeListing(itemId, count, emeralds, uses, false, Math.max(1, enchantLevel), "");
    }

    static TradeListing potionSell(BrewPotion potion) {
        return new TradeListing(
                "minecraft:potion", 1, BREWER_POTION_EMERALDS, BREWER_POTION_USES, false, 0, potion.potionId());
    }

    /** First level 2 potion, then other effects whose family differs. The live shop rolls both. */
    static List<TradeListing> brewerUsualListings() {
        List<TradeListing> listings = new ArrayList<>();
        BrewPotion featured = BREWER_LEVEL_TWO.getFirst();
        listings.add(potionSell(featured));
        int added = 0;
        for (BrewPotion potion : BREWER_OTHER_POTIONS) {
            if (potion.family().equals(featured.family())) {
                continue;
            }
            listings.add(potionSell(potion));
            added++;
            if (added >= BREWER_USUAL_OTHERS) {
                break;
            }
        }
        return List.copyOf(listings);
    }

    /** Brewing ingredients a larger brewer sells after the potion listings. */
    static List<TradeListing> brewerIngredientListings() {
        return List.of(
                sellOf("minecraft:nether_wart", 4, BREWER_INGREDIENT_EMERALDS, BREWER_INGREDIENT_USES),
                sellOf("minecraft:blaze_powder", 2, BREWER_INGREDIENT_EMERALDS, BREWER_INGREDIENT_USES),
                sellOf("minecraft:glowstone_dust", 4, BREWER_INGREDIENT_EMERALDS, BREWER_INGREDIENT_USES),
                sellOf("minecraft:redstone", 4, BREWER_INGREDIENT_EMERALDS, BREWER_INGREDIENT_USES),
                sellOf("minecraft:sugar", 4, BREWER_INGREDIENT_EMERALDS, BREWER_INGREDIENT_USES),
                sellOf("minecraft:glass_bottle", 4, BREWER_INGREDIENT_EMERALDS, BREWER_INGREDIENT_USES),
                sellOf("minecraft:spider_eye", 4, BREWER_INGREDIENT_EMERALDS, BREWER_INGREDIENT_USES),
                sellOf("minecraft:gunpowder", 2, BREWER_INGREDIENT_EMERALDS, BREWER_INGREDIENT_USES),
                sellOf("minecraft:magma_cream", 2, BREWER_INGREDIENT_EMERALDS, BREWER_INGREDIENT_USES),
                sellOf("minecraft:fermented_spider_eye", 2, BREWER_INGREDIENT_EMERALDS, BREWER_INGREDIENT_USES),
                sellOf("minecraft:glistering_melon_slice", 1, BREWER_INGREDIENT_EMERALDS, BREWER_INGREDIENT_USES),
                sellOf("minecraft:golden_carrot", 1, BREWER_INGREDIENT_EMERALDS, BREWER_INGREDIENT_USES),
                sellOf("minecraft:pufferfish", 1, BREWER_INGREDIENT_EMERALDS, BREWER_INGREDIENT_USES),
                sellOf("minecraft:ghast_tear", 1, BREWER_INGREDIENT_EMERALDS, BREWER_INGREDIENT_USES),
                sellOf("minecraft:rabbit_foot", 1, BREWER_INGREDIENT_EMERALDS, BREWER_INGREDIENT_USES),
                sellOf("minecraft:phantom_membrane", 1, BREWER_INGREDIENT_EMERALDS, BREWER_INGREDIENT_USES),
                sellOf("minecraft:turtle_scute", 1, BREWER_INGREDIENT_EMERALDS, BREWER_INGREDIENT_USES),
                sellOf("minecraft:dragon_breath", 1, BREWER_INGREDIENT_EMERALDS, BREWER_INGREDIENT_USES));
    }

    /** Family of a catalogued potion, or empty when the id is not one a brewer sells. */
    public static String potionFamily(String potionId) {
        if (potionId == null || potionId.isEmpty()) {
            return "";
        }
        for (BrewPotion potion : BREWER_LEVEL_TWO) {
            if (potion.potionId().equals(potionId)) {
                return potion.family();
            }
        }
        for (BrewPotion potion : BREWER_OTHER_POTIONS) {
            if (potion.potionId().equals(potionId)) {
                return potion.family();
            }
        }
        return "";
    }

    private static final String[] COMPASS_DIRECTIONS = {
        "east", "southeast", "south", "southwest", "west", "northwest", "north", "northeast"
    };

    /**
     * Eight-way compass name for a step of {@code east} (+X, the rising sun) and {@code south} (+Z).
     * Returns null when the two places are the same.
     */
    public static String compassDirection(double east, double south) {
        if (east * east + south * south < 1.0e-8) {
            return null;
        }
        double angle = Math.toDegrees(Math.atan2(south, east));
        if (angle < 0.0) {
            angle += 360.0;
        }
        int index = (int) Math.floor((angle + 22.5) / 45.0) % COMPASS_DIRECTIONS.length;
        return COMPASS_DIRECTIONS[index];
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
