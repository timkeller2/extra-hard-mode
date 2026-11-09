package dev.extrahardmode.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Residence volume, amenity score, spacing, and dawn chance. Minecraft-free for JUnit.
 */
public final class InhabitantRules {
    public static final int MIN_VOLUME = 24;
    public static final int MAX_VOLUME = 400;
    public static final int FILL_CAP = 512;
    public static final int MIN_LIGHT = 5;
    public static final int MIN_SCORE = 12;
    public static final int FINE_SCORE = 18;
    public static final int NOTABLE_SCORE = 24;
    public static final int MASTER_SCORE = 30;
    public static final int ART_SCORE_CAP = 6;
    public static final int VOLUME_PER_POINT = 48;
    public static final int VOLUME_POINT_CAP = 5;
    public static final int ROOM_POINT_CAP = 3;
    public static final int SPACING = 20;
    /** More than this horizontal distance from the nearest other occupied home gives 1 point. */
    public static final int ISOLATION_NEAR_BLOCKS = 50;
    /** More than this distance gives 2 points instead of 1. */
    public static final int ISOLATION_FAR_BLOCKS = 100;
    /** How long one clock suggestion stays on a bed, in real milliseconds. */
    public static final long HOUSE_HINT_MILLIS = 3L * 60L * 1000L;
    public static final String HINT_WINDOWS = "windows";
    public static final String HINT_ART = "art";
    public static final String HINT_CARPETS = "carpets";
    public static final String HINT_SEATING = "seating";
    public static final String HINT_STORAGE = "storage";
    public static final String HINT_WORKSTATIONS = "workstations";
    public static final String HINT_LIGHTS = "lights";
    public static final String HINT_PLANTS = "plants";
    public static final String HINT_BOOKS = "books";
    public static final String HINT_BED = "bed";
    public static final String HINT_KITCHEN_HEAT = "kitchen_heat";
    public static final String HINT_KITCHEN_WATER = "kitchen_water";
    public static final String HINT_VOLUME = "volume";
    public static final String HINT_ROOMS = "rooms";
    public static final String HINT_ISOLATION_NEAR = "isolation_near";
    public static final String HINT_ISOLATION_FAR = "isolation_far";
    /** A new resident will not appear when a villager is closer than this, in blocks. */
    public static final int VILLAGER_CLEARANCE = 35;
    public static final int BASE_CHANCE_PERCENT = 8;
    public static final int CHANCE_PER_POINT = 2;
    public static final int MAX_CHANCE_PERCENT = 40;
    public static final int LEAVE_GRACE_DAYS = 3;
    public static final int KILL_COOLDOWN_DAYS = 7;
    /** Days between restocking inhabitant merchant uses. Armor uses {@link #ARMOR_RESTOCK_DAYS}. */
    public static final int RESTOCK_DAYS = 14;
    public static final int ARMOR_RESTOCK_DAYS = 30;
    public static final int ARMOR_TRADE_USES = 1;
    /** Base uses on a resident listing. A catalog value above this is stored as this. */
    public static final int DEFAULT_TRADE_USES = 3;
    /** Usual shop size. A longer catalog keeps this many and treats the rest as extras. */
    public static final int USUAL_TRADE_TYPES = 4;
    /** Hauler buy stack with no storage. Each storage block takes {@link #HAULER_PER_STORAGE} off, down to {@link #HAULER_STACK_MIN}. */
    public static final int HAULER_STACK = 32;
    public static final int HAULER_STACK_MIN = 16;
    public static final int HAULER_PER_STORAGE = 2;
    /** A trader buys two more of each thing he sells. Those buys start with no uses. */
    public static final int TRADER_BUY_EXTRA = 2;
    public static final int WEALTHY_OFFER_COUNT = USUAL_TRADE_TYPES;
    public static final int WEALTHY_TRADE_USES = DEFAULT_TRADE_USES;
    /** One diamond. Matches a wealthy trader. */
    public static final int JEWELER_DIAMOND_EMERALDS = 8;
    /** Four gold ingots for 2 emeralds. */
    public static final int JEWELER_GOLD_INGOT_COUNT = 4;
    public static final int JEWELER_GOLD_EMERALDS = 2;
    /** One iron block. Nine ingots would be about 2 emeralds; the block costs more. */
    public static final int JEWELER_IRON_BLOCK_EMERALDS = 6;
    /** One copper block or one redstone block. Each is a small stack of the loose item, sold as a block. */
    public static final int JEWELER_SMALL_BLOCK_EMERALDS = 2;
    /** One diamond block from a jeweler. */
    public static final int JEWELER_DIAMOND_BLOCK_EMERALDS = 32;
    /** One gold block from a jeweler. Same emerald price as an iron block. */
    public static final int JEWELER_GOLD_BLOCK_EMERALDS = 6;
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
    /** Usual farm shop sells this many listings, drawn from {@link #FARM_LOWER_SELLS}. */
    public static final int FARM_USUAL_SELLS = 2;
    public static final int FARM_PLANT_USES = DEFAULT_TRADE_USES;
    /** Wheat a farm neighbor sells for one emerald. */
    public static final int FARM_WHEAT_COUNT = 8;
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
    public static final int ARMOR_METAL_BUY_USES = DEFAULT_TRADE_USES;
    /** Drinkable potions, including the level 2 bottle, cost one emerald. */
    public static final int BREWER_POTION_EMERALDS = 1;
    public static final int BREWER_POTION_USES = DEFAULT_TRADE_USES;
    /** Other potion effects kept in the usual shop, besides the level 2 bottle. */
    public static final int BREWER_USUAL_OTHERS = 3;
    /** Brewing ingredients are one emerald. The count on each listing is the bargain. */
    public static final int BREWER_INGREDIENT_EMERALDS = 1;
    public static final int BREWER_INGREDIENT_USES = DEFAULT_TRADE_USES;
    /** Copies in the arrival bag when the home has a cauldron. */
    public static final int BREWER_BAG_COPIES = 3;
    /**
     * A job is skipped when it has more living residents than this multiple of the smallest job
     * this home can host.
     */
    public static final int CROWD_MULTIPLE = 2;
    /** Wise teachers and council members use this higher multiple before they are skipped. */
    public static final int RARE_CROWD_MULTIPLE = 3;
    private static final List<String> ARMOR_SLOTS = List.of("boots", "helmet", "leggings", "chestplate");
    public static final int WANDER_RANGE = 16;
    public static final int SNAP_HOME_RANGE = 32;
    /**
     * Chunk levels at or below this are entity-ticking. A ticket aimed at a full chunk
     * has to go this many levels lower before the center chunk loads its entities.
     */
    public static final int ENTITY_TICKET_EXTRA = 2;

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
    public static final String OLD_MINER = "old_miner";
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
            BREWER,
            OLD_MINER);

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
            new WealthyListing("minecraft:iron_ingot", 4, 2),
            new WealthyListing("minecraft:gold_ingot", 2, 2),
            new WealthyListing("minecraft:copper_ingot", 6, 1),
            new WealthyListing("minecraft:diamond", 1, 8),
            new WealthyListing("minecraft:iron_block", 1, 6),
            new WealthyListing("minecraft:ender_pearl", 1, 3),
            new WealthyListing("minecraft:name_tag", 1, 10),
            new WealthyListing("minecraft:saddle", 1, 6),
            new WealthyListing("minecraft:golden_apple", 1, 4),
            new WealthyListing("minecraft:obsidian", 2, 2),
            new WealthyListing("minecraft:lapis_lazuli", 6, 1),
            new WealthyListing("minecraft:redstone", 8, 1),
            new WealthyListing("minecraft:quartz", 6, 1),
            new WealthyListing("minecraft:book", 1, 1),
            new WealthyListing("minecraft:experience_bottle", 10, 5),
            new WealthyListing("minecraft:arrow", 12, 1),
            new WealthyListing("minecraft:cooked_beef", 4, 1),
            new WealthyListing("minecraft:oak_log", 8, 1),
            new WealthyListing("minecraft:glass", 8, 1),
            new WealthyListing("minecraft:glowstone", 4, 2),
            new WealthyListing("minecraft:packed_ice", 4, 2),
            new WealthyListing("minecraft:slime_ball", 4, 2),
            new WealthyListing("minecraft:leather", 6, 1),
            new WealthyListing("minecraft:string", 8, 1),
            new WealthyListing("minecraft:gunpowder", 4, 2),
            new WealthyListing("minecraft:blaze_rod", 1, 3),
            new WealthyListing("minecraft:ghast_tear", 1, 8),
            new WealthyListing("minecraft:nether_wart", 1, 1),
            new WealthyListing("minecraft:amethyst_shard", 4, 1),
            new WealthyListing("minecraft:coal", 8, 1),
            new WealthyListing("minecraft:lantern", 4, 1),
            new WealthyListing("minecraft:phantom_membrane", 2, 4));

    /**
     * Crops a smaller farm can sell, besides wheat. The usual shop draws {@link #FARM_USUAL_SELLS} of these.
     * Seeds, carrots, berries, pods, and sugar cane are 3 for 1 emerald. A sapling is 1 for 2.
     */
    public static final List<TradeListing> FARM_LOWER_SELLS = List.of(
            sellOf("minecraft:wheat_seeds", 3, 1, FARM_PLANT_USES),
            sellOf("minecraft:beetroot_seeds", 3, 1, FARM_PLANT_USES),
            sellOf("minecraft:carrot", 3, 1, FARM_PLANT_USES),
            sellOf("minecraft:sweet_berries", 3, 1, FARM_PLANT_USES),
            sellOf("minecraft:glow_berries", 3, 1, FARM_PLANT_USES),
            sellOf("minecraft:sugar_cane", 3, 1, FARM_PLANT_USES),
            sellOf("minecraft:torchflower_seeds", 3, 1, FARM_PLANT_USES),
            sellOf("minecraft:pitcher_pod", 3, 1, FARM_PLANT_USES),
            sellOf("minecraft:oak_sapling", 1, 2, FARM_PLANT_USES),
            sellOf("minecraft:birch_sapling", 1, 2, FARM_PLANT_USES),
            sellOf("minecraft:spruce_sapling", 1, 2, FARM_PLANT_USES),
            sellOf("minecraft:bone_meal", 4, 1, FARM_PLANT_USES),
            sellOf("minecraft:pumpkin", 1, 1, FARM_PLANT_USES),
            sellOf("minecraft:melon", 1, 1, FARM_PLANT_USES),
            sellOf("minecraft:beetroot", 12, 1, FARM_PLANT_USES));

    /** Sales that wait for a larger home. Each is one item for one emerald. */
    public static final List<TradeListing> FARM_HIGHER_SELLS = List.of(
            sellOf("minecraft:cocoa_beans", 1, 1, FARM_PLANT_USES),
            sellOf("minecraft:pumpkin_seeds", 1, 1, FARM_PLANT_USES),
            sellOf("minecraft:melon_seeds", 1, 1, FARM_PLANT_USES),
            sellOf("minecraft:potato", 1, 1, FARM_PLANT_USES),
            sellOf("minecraft:nether_wart", 1, 1, FARM_PLANT_USES));

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
        "Alden", "Bram", "Cedric", "Darren", "Edmund", "Felix", "Gavin", "Harold", "Ivo", "Ivan",
        "Kade", "Leon", "Marcus", "Nolan", "Oscar", "Peter", "Russell", "Stuart", "Samuel", "Thomas",
        "Arthur", "Bennett", "Calvin", "Derek", "Edward", "Franklin", "George", "Henry", "Isaac", "James",
        "Kenneth", "Lawrence", "Martin", "Nicholas", "Oliver", "Patrick", "Quentin", "Robert", "Simon", "Theodore",
        "Victor", "Walter", "Xavier", "Yusuf", "Zachary", "Adrian", "Blake", "Conrad", "Dominic", "Ernest",
        "Frederick", "Grant", "Hugh", "Ian", "Jasper", "Keith", "Lewis", "Miles", "Nathan", "Owen",
        "Paul", "Reid", "Seth", "Tobias",
        "Albert", "Alfred", "Ambrose", "Amos", "Andrew", "Angus", "Archibald", "Arnold",
        "Bartholomew", "Basil", "Benedict", "Bernard", "Bertram", "Boris", "Brendan", "Bruce",
        "Carl", "Cecil", "Charles", "Chester", "Clarence", "Clark", "Clifford", "Clyde",
        "Dennis", "Desmond", "Douglas", "Duncan", "Dwight", "Earl", "Edgar", "Edwin",
        "Elias", "Elliot", "Elmer", "Emmett", "Eric", "Eugene", "Everett", "Ezra",
        "Floyd", "Francis", "Frank", "Fred", "Gilbert", "Gordon", "Graham", "Gregory",
        "Harvey", "Hector", "Herbert", "Howard", "Hugo", "Irving", "Jack", "Jacob",
        "Julian", "Julius", "Leo", "Lionel", "Lloyd", "Louis", "Lucas", "Luther"
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
            int isolation,
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

    /** One sticky clock suggestion for one bed. {@code until} is epoch milliseconds. */
    public record HouseHint(String bed, String improvement, long until) {}

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
        return gates(enclosed, volume, hasBed, doorOutside, lit, roofed, farEnough, counts, rooms, 0);
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
            int rooms,
            int isolation) {
        int distancePoints = Math.max(0, Math.min(2, isolation));
        int scored = score(counts, volume, rooms) + distancePoints;
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
                distancePoints,
                missing(enclosed, volume, hasBed, doorOutside, lit, roofed, farEnough, scored));
    }

    public static String inspectFallback(GateResult result) {
        if (result == null) {
            return "This is not a room.";
        }
        String distance = switch (result.isolation()) {
            case 1 -> " Distance adds 1.";
            case 2 -> " Distance adds 2.";
            default -> "";
        };
        String head = "This room: " + result.score() + "/" + MIN_SCORE + " points." + distance;
        if (result.eligible()) {
            return head + " Eligible for a resident.";
        }
        if (result.missing().isEmpty()) {
            return head + " Not yet eligible. Type /tougher help homes for the checklist.";
        }
        return head + " Missing: " + String.join(", ", result.missing()) + ".";
    }

    /**
     * Point gains still available. A category at its cap is left out, and so is anything
     * that does not raise the score, such as a roof or a brighter floor.
     */
    public static List<String> openImprovements(AmenityCounts counts, int volume, int rooms, int isolation) {
        if (counts == null) {
            counts = new AmenityCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, false);
        }
        List<String> open = new ArrayList<>();
        if (counts.windows() < 3) {
            open.add(HINT_WINDOWS);
        }
        if (counts.art() < ART_SCORE_CAP) {
            open.add(HINT_ART);
        }
        if (counts.carpets() < 12) {
            open.add(HINT_CARPETS);
        }
        if (counts.seating() < 2) {
            open.add(HINT_SEATING);
        }
        if (counts.storage() < 2) {
            open.add(HINT_STORAGE);
        }
        if (counts.workstations() < 2) {
            open.add(HINT_WORKSTATIONS);
        }
        if (counts.extraLights() < 2) {
            open.add(HINT_LIGHTS);
        }
        if (counts.plants() < 2) {
            open.add(HINT_PLANTS);
        }
        if (counts.books() < 2) {
            open.add(HINT_BOOKS);
        }
        if (counts.extraBeds() <= 0) {
            open.add(HINT_BED);
        }
        if (!counts.kitchen()) {
            open.add(counts.cauldron() ? HINT_KITCHEN_HEAT : HINT_KITCHEN_WATER);
        }
        if (volumePoints(volume) < VOLUME_POINT_CAP) {
            open.add(HINT_VOLUME);
        }
        if (roomPoints(rooms) < ROOM_POINT_CAP) {
            open.add(HINT_ROOMS);
        }
        int distance = Math.max(0, Math.min(2, isolation));
        if (distance < 1) {
            open.add(HINT_ISOLATION_NEAR);
        } else if (distance < 2) {
            open.add(HINT_ISOLATION_FAR);
        }
        return List.copyOf(open);
    }

    /** True while the saved suggestion still raises points and its 3 minutes have not passed. */
    public static boolean keepImprovement(List<String> open, String remembered, long now, long until) {
        return open != null
                && remembered != null
                && !remembered.isEmpty()
                && now < until
                && open.contains(remembered);
    }

    /**
     * The saved suggestion while it still applies and {@code now} is before {@code until}.
     * Otherwise one of {@code open}, chosen by {@code roll}. An empty list returns "".
     */
    public static String chooseImprovement(List<String> open, String remembered, long now, long until, int roll) {
        if (open == null || open.isEmpty()) {
            return "";
        }
        if (keepImprovement(open, remembered, now, until)) {
            return remembered;
        }
        return open.get(Math.floorMod(roll, open.size()));
    }

    public static String houseHintBed(String dimension, int x, int y, int z) {
        String dim = dimension == null ? "" : dimension;
        return dim + " " + x + " " + y + " " + z;
    }

    public static HouseHint hintFor(List<HouseHint> hints, String bed) {
        if (hints == null || bed == null || bed.isEmpty()) {
            return null;
        }
        for (HouseHint hint : hints) {
            if (hint != null && bed.equals(hint.bed())) {
                return hint;
            }
        }
        return null;
    }

    /**
     * Stores {@code improvement} on {@code bed} until 3 minutes after {@code now}.
     * An empty improvement clears that bed. Other beds stay until their own time ends.
     */
    public static List<HouseHint> rememberHint(List<HouseHint> current, String bed, String improvement, long now) {
        List<HouseHint> next = new ArrayList<>();
        if (current != null) {
            for (HouseHint hint : current) {
                if (hint == null || hint.bed() == null || hint.bed().isEmpty()) {
                    continue;
                }
                if (hint.bed().equals(bed) || hint.until() <= now) {
                    continue;
                }
                next.add(hint);
            }
        }
        if (bed != null && !bed.isEmpty() && improvement != null && !improvement.isEmpty()) {
            next.add(new HouseHint(bed, improvement, now + HOUSE_HINT_MILLIS));
        }
        return List.copyOf(next);
    }

    /** What the clock suggests. Unknown ids return "". */
    public static String improvementText(String improvement) {
        if (improvement == null) {
            return "";
        }
        return switch (improvement) {
            case HINT_WINDOWS -> "Add a window of glass or panes that looks out of the room.";
            case HINT_ART -> "Add a painting or a filled item frame.";
            case HINT_CARPETS -> "Add wool carpets. Every 4 carpets adds 1 point, up to 12 carpets.";
            case HINT_SEATING -> "Add a seat made from stairs or a slab.";
            case HINT_STORAGE -> "Add a chest, barrel, or shulker box.";
            case HINT_WORKSTATIONS -> "Add a crafting table, furnace, smoker, anvil, or similar workstation.";
            case HINT_LIGHTS -> "Add a torch, lantern, glowstone, or campfire.";
            case HINT_PLANTS -> "Add a flower pot, flower, or sapling.";
            case HINT_BOOKS -> "Add a bookshelf or a lectern.";
            case HINT_BED -> "Add a second bed.";
            case HINT_KITCHEN_HEAT -> "Add a furnace, smoker, or campfire beside the cauldron.";
            case HINT_KITCHEN_WATER -> "Add a cauldron beside a furnace, smoker, or campfire.";
            case HINT_VOLUME -> "Enlarge the interior. Every 48 air blocks adds 1 point, up to 5.";
            case HINT_ROOMS -> "Add another enclosed room joined by a door, trapdoor, or fence gate.";
            case HINT_ISOLATION_NEAR -> "Place this home more than 50 blocks from the nearest other occupied home.";
            case HINT_ISOLATION_FAR -> "Place this home more than 100 blocks from the nearest other occupied home.";
            default -> "";
        };
    }

    public static List<String> homesIntroLines() {
        return List.of(
                "Homes: Right-click a bed with a clock to score the room. Type /tougher help homes anytime for this list.",
                "Homes: The clock names one improvement that would still raise the score. It stays for 3 minutes, or changes at once when that improvement no longer adds points.",
                "Homes: An enclosed, well-lit room with a bed, a door, and furnishings may attract one traveler. They dislike crowding.",
                "Homes: Subjects: rooms, furnishings, residents, shops, bounties. Each is also /tougher help <subject>.",
                "Homes: Residents may buy spare blocks, trade food, post bounties, keep a shop, teach a mana ability, or sell armor.",
                "Homes: Staff can toggle residents with /tougher set inhabitants true (requires operator).");
    }

    public static List<String> homesRoomLines() {
        return List.of(
                "Rooms: Need an enclosed room (solid walls, floor, and roof; doors and trapdoors are openings, not holes).",
                "Rooms: Also need 24 to 400 interior air blocks, a bed, and a door or fence gate that faces outside.",
                "Rooms: Block light 5 on every floor tile, and stay 20 blocks from another occupied home.",
                "Rooms: More than 50 blocks from the nearest other occupied home gives +1 point. More than 100 blocks gives +2 instead.");
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
                "Residents: The name over their head is their name and occupation, such as Alden the Jeweler. Each name is used once before any name is used again.",
                "Residents: Clock a resident, then clock another. They trade homes at once, even far apart. Each refuses a home below their job.",
                "Residents: A loot collector or council member needs 18. A jeweler, armorsmith, or wise teacher needs 24. A wealthy trader or master armorsmith needs 30. Everyone else needs 12.",
                "Residents: The trade costs half the points of the finer home, rounded down, in emeralds. Their shop stays until the next restock.",
                "Residents: A council member keeps the hunts already finished with you when they trade homes.",
                "Residents: Clothes are a villager outfit, so two jewelers may differ. Finer residents wear higher-ranked clothes.",
                "Residents: A resident will not appear within 35 blocks of a villager. Other residents do not count.",
                "Residents: Eligible loaded homes are checked at dawn. An empty eligible home is timestamped the day it becomes inhabitable.",
                "Residents: When you visit later, each missed day is rolled once (same chances as dawn), then the timestamp is set to today.",
                "Residents: Houses in unloaded chunks can still fill. Chance starts at 8% at 12 points and rises with extra points (halved in blight).",
                "Residents: Chests bias a hauler, a kitchen biases a cook, and plants or workstations bias a farm neighbor.",
                "Residents: A cauldron favors a brewer, and a brewer can come to a 12-point home.",
                "Residents: An old miner can come to a 12-point home.",
                "Ore leads: Speak to an old miner for a lead. He does not keep a shop. Every 7 Minecraft days he names the largest vein in his chunk and the eight around it.",
                "Ore leads: He adds more of that ore beside the vein. He names only ores found in his chunk and the eight around it.",
                "Ore leads: The list is the easiest of those ores he has not finished. Its length is the home's points plus finished leads with him, divided by 5, rounded up.",
                "Ore leads: Finishing an easier ore lets the next harder ore there join the list. He will not repeat an ore until every ore there has been named.",
                "Ore leads: A lead you never finish can be named again.",
                "Ore leads: The line reads Jack: Iron East. The direction is North, South, East, West, Up, or Down, whichever is farthest, and it changes as you move.",
                "Ore leads: Within 3 blocks the lead is finished. It expires after 7 Minecraft days. Each player has a separate lead, and one miner can lead several players.",
                "Residents: 18 points can attract a loot collector or a council member, 24 a jeweler, an armorsmith, or a wise teacher, and 30 a wealthy trader or a master armorsmith.",
                "Residents: New residents prefer a type that has not appeared yet.",
                "Residents: A job with more than twice as many residents as the smallest job this home can host is skipped.",
                "Residents: Wise teachers and council members are skipped only past three times that number.",
                "Residents: The house must be able to host that type. Once every type has spawned, the usual furnishing biases apply.",
                "Residents: Council members are three times as likely as the most common other type. One resident per home.",
                "Residents: Most residents restock every 14 Minecraft days; armorsmiths restock every 30 days.",
                "Residents: Look at a resident who is waiting to restock to see how long is left, in days and hours like a torch.",
                "Residents: They have 40 health plus their house points, and regain 1 health each minute.",
                "Residents: Looking at one shows hearts, one per 10 health, beside the restock time.",
                "Residents: After a mob hits them, they shoot mobs within 12 blocks that are after them. The bow hits 3 harder than a normal arrow.",
                "Residents: A close attack makes them switch to an iron sword. A zombie cannot turn them unless they are already below 5 health.",
                "Residents: A home stops listing someone who has died or is no longer around the bed.",
                "Lessons: Speak to a wise teacher and he tells of a hunt every 30 Minecraft days.",
                "Lessons: While he waits to give another hunt, he names up to 3 explored biomes that can still spawn a boss. If more than 3 can, he picks 3 and keeps them until that wait ends.",
                "Lessons: The pack is a 3 to 8 base from the home and his experience, plus 50% of that base for each hunt you have finished with him, rounded.",
                "Lessons: The first hunts are easier. Every 4 hunts, they are harder. It lies 200 to 500 blocks north, south, east, or west, plus 33% of that for each of those hunts, rounded.",
                "Lessons: He only sends creatures from the dimension where he lives.",
                "Lessons: His name, how many are left, and the direction show on your screen. That number falls as you kill them. At that distance it says near here.",
                "Lessons: Come back closer than that distance and the direction returns.",
                "Lessons: After 2 minutes out there, on land, at night or below ground, half appear within a few seconds. The rest spawn every 5 to 15 seconds, 20 to 40 blocks out.",
                "Lessons: Each mark has 50% to 100% more health. They give triple experience and double loot, and they always drop both. They vanish after 3 Minecraft days.",
                "Lessons: A mark in your line of sight gives off a red mist, and other players can see it.",
                "Lessons: A golden carrot mists every mark until you are not saturated. Sense Evil does that for 1 minute times its level.",
                "Lessons: A golden carrot or Sense Evil also shows a white outline through blocks.",
                "Lessons: Zombies, skeletons, drowned, strays, bogged, zombie villagers, and phantoms on a hunt do not burn in sunlight.",
                "Lessons: The first creature is slightly larger, has 5 times the health, and resists knockback.",
                "Lessons: It drops 10 times the experience and 3 times the loot, plus emeralds from 5% to 10% of that health.",
                "Lessons: The last creature has twice the first creature's health and hits 50% harder. It drops 3 times the loot and emeralds from 5% to 10% of its health.",
                "Lessons: A creeper that explodes goes back on the spawn queue. A first or last creeper returns with that same role.",
                "Lessons: An unfinished hunt ends after 14 Minecraft days. Each wise teacher can give you a different hunt, and each player has his own.");
    }

    public static List<String> homesShopLines() {
        return List.of(
                "Shops: Shop size follows house points. The top of the roll is 50% at 14 points and 175% at 38. Higher scores stay there.",
                "Shops: The bottom of that roll is half the top. A 14-point home rolls 25-50% of the usual trade types and of each listing's uses.",
                "Shops: Houses under 14 points use that same 25-50% band. There is at least one use of each listing that is rolled.",
                "Shops: Each listing rolls its uses separately. Extra types beyond the usual shop are random trades.",
                "Shops: Each trade adds 1 trading experience. A stock change adds the square root of that experience to the house points.",
                "Shops: Each stock change rolls item amounts once from -25% to +25%, then adds 0 to 25 for house points and trading experience.",
                "Shops: That total adds items when you buy and subtracts the items required when you sell to the resident. Emerald prices stay the same.",
                "Shops: A hauler pays 1 emerald for 32 of a block. Each chest, barrel, or shulker box takes 2 off that stack, down to 16.",
                "Shops: A farm neighbor sells 8 wheat for 1 emerald. Seeds, carrots, berries, pods, and sugar cane are 3 for 1 emerald.",
                "Shops: A smaller home may sell a sapling for 2 emeralds, 4 bone meal for 1, 1 pumpkin for 1, 1 melon for 1, or 12 beetroot for 1.",
                "Shops: Potatoes, pumpkin seeds, melon seeds, cocoa beans, and nether wart need a larger home. Each is 1 for 1 emerald.",
                "Shops: A trader also buys each thing he sells. The buy stack is 2 larger and pays the same number of emeralds.",
                "Shops: Those buys start with no uses. Buying from him adds 1 use to each of them. Selling him anything spends 1 use on each of them.",
                "Shops: A trader or a wealthy trader also offers other players' chests. A sign on the chest that says SELL or BUY is what he reads.",
                "Shops: A SELL chest is read from the top left. An emerald stack sets the price, and the next new item stack sets how many he sells for it.",
                "Shops: Later stacks of that same item are extra stock. He sells those before the stack that set the amount. Your own chest is not offered to you.",
                "Shops: Emeralds in the bottom two rows of a SELL chest do not set a price.",
                "Shops: Sale emeralds fill emerald stacks in those rows from the bottom right. If those stacks are full or missing, they fill the bottom-right empty slot.",
                "Shops: He keeps selling while the chest has enough items and room for the emeralds.",
                "Shops: A BUY chest uses the same prices above the bottom row. The bottom row holds the emeralds he pays with.",
                "Shops: Bought items go on that bottom row, or the row above it. He keeps buying while those emeralds last and the chest has room.",
                "Shops: A trade from another player's chest gives no experience to you or to the resident.",
                "Shops: A jeweler sells a diamond for 8 emeralds, 4 gold ingots for 2, an iron block for 6, and a copper or redstone block for 2.",
                "Shops: Larger jewelers also sell lapis, amethyst, quartz, gold nuggets, and blocks of gold, lapis, amethyst, quartz, and diamond.",
                "Shops: A gold block is 6 emeralds and a diamond block is 32.",
                "Shops: An armorsmith sells one heavy iron piece each restock, and the piece can change. Other sales are enchanted iron armor, each a different slot.",
                "Shops: A larger iron shop may buy iron. A master armorsmith sells one heavy diamond piece the same way, plus enchanted diamond armor, and may buy diamonds.",
                "Shops: Heavy iron costs 12, 15, 21, or 24 emeralds for boots, a helmet, leggings, or a chestplate. Heavy diamond costs four times that.",
                "Shops: Enchanted iron armor costs 6, 8, 11, or 12 emeralds for those slots. Enchanted diamond armor costs the same as heavy iron.",
                "Shops: A brewer sells one level 2 potion each restock, and that potion can change. Up to 3 other potions cost 1 emerald.",
                "Shops: A larger brewer sells one of each brewing ingredient for 1 emerald, such as nether wart, blaze powder, glowstone, and ghast tears.",
                "Shops: A loot collector pays 1 emerald for 4 spider eyes, 4 gunpowder, or 8 bones.",
                "Shops: A larger loot collector also pays 1 emerald for 8 rotten flesh, 8 string, 4 slimeballs, 6 leather, or 12 arrows.",
                "Shops: That shop also pays 1 emerald for 6 ink sacs, 2 phantom membranes, 2 ender pearls, 2 blaze rods, or 1 ghast tear.");
    }

    public static List<String> homesBountyLines() {
        return List.of(
                "Bounties: Council members give each player a personal kill bounty when you check in.",
                "Bounties: The first hunt from a resident is 6-18 common mobs. Each success with that resident makes the next 20% larger. Every 4 successes, their next hunt is rarer.",
                "Bounties: A mob with more than 20 health is fewer creatures, in proportion to that health.",
                "Bounties: The resident only sends mobs from the dimension where they live. Guardians are left out.",
                "Bounties: You have 14 Minecraft days. A bounty that runs out is not replaced in that conversation.",
                "Bounties: The next bounty from that resident waits 7 Minecraft days after the last one was given, plus 3 per finished bounty with them, and only after you finish the previous one.",
                "Bounties: Each council member can give you one. You can hold 5 at a time, counting hunts and unpaid emerald turn-ins.",
                "Bounties: A kill counts for every open bounty on that mob.",
                "Bounties: At night, each open bounty can send a group within 48 blocks, on or above a solid block dark enough for a monster.",
                "Bounties: Each second of that night, each open bounty has a 1 in 250 chance. One bounty usually brings 1 or 2 groups. Zero, or 3 or more, is less common.",
                "Bounties: Each creature stays until the next Minecraft day. A group is at least 1, from 1/6 of those still to kill through 1/3. Each open bounty rolls on its own.",
                "Bounties: Finishing grants XP equal to one quarter of the slain mobs' health plus 25, with 10% more per extra house point.",
                "Bounties: You also get a congratulations message and a sound. Return to that resident for emeralds equal to one tenth of that XP.",
                "Bounties: Collecting plays a sound. When that wait is over, that resident offers a new bounty.",
                "Bounties: A bounty reads Ada: 9 Zombies above the ability icons. The line is the council member, how many are left, and the mob. Lines stack upward.",
                "Bounties: A loot collector warns you when a biome boss is out, and names the compass direction from that resident. East is the rising sun.");
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

    /**
     * Chunks from the bed that can still hold a resident who is at most {@code snapBlocks} away.
     * A bed on the far edge of its chunk pushes that resident one chunk further.
     */
    public static int residentChunkSpread(int snapBlocks) {
        int blocks = Math.max(0, snapBlocks);
        return (15 + blocks) / 16;
    }

    /**
     * Ticket radius that entity-ticks every chunk {@link #residentChunkSpread(int)} can name.
     * Vanilla's full-chunk ticket level is two above entity ticking, so the radius adds that.
     */
    public static int residentLoadRadius(int snapBlocks) {
        return residentChunkSpread(snapBlocks) + ENTITY_TICKET_EXTRA;
    }

    public static boolean farEnough(int dx, int dz, int spacing) {
        long min = Math.max(0, spacing);
        return horizontalDistanceSquared(dx, dz) >= min * min;
    }

    public static long horizontalDistanceSquared(int dx, int dz) {
        return (long) dx * dx + (long) dz * dz;
    }

    /**
     * House points for distance to the nearest other occupied home. No other home,
     * and any gap past {@link #ISOLATION_FAR_BLOCKS}, is 2. Past
     * {@link #ISOLATION_NEAR_BLOCKS} is 1. An equal distance does not qualify.
     */
    public static int isolationPoints(long nearestDistanceSquared) {
        long far = (long) ISOLATION_FAR_BLOCKS * ISOLATION_FAR_BLOCKS;
        long near = (long) ISOLATION_NEAR_BLOCKS * ISOLATION_NEAR_BLOCKS;
        if (nearestDistanceSquared > far) {
            return 2;
        }
        if (nearestDistanceSquared > near) {
            return 1;
        }
        return 0;
    }

    /** True when a villager at this offset is far enough for a resident to appear. */
    public static boolean clearOfVillager(double dx, double dy, double dz, int clearance) {
        double min = Math.max(0, clearance);
        return dx * dx + dy * dy + dz * dz >= min * min;
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
            case MASTER_ARMORSMITH, WEALTHY -> MASTER_SCORE;
            case ARMORSMITH, WISE_TEACHER, JEWELER -> NOTABLE_SCORE;
            case BOUNTY, COUNCIL -> FINE_SCORE;
            default -> MIN_SCORE;
        };
    }

    public static boolean specialtyAllowed(String type, int score) {
        return score >= minScoreForSpecialty(type);
    }

    /**
     * Emeralds to trade two residents' homes. Half the finer home's points, rounded down.
     */
    public static int movingFee(int sourceScore, int destinationScore) {
        int finer = Math.max(0, Math.max(sourceScore, destinationScore));
        return finer / 2;
    }

    public static String pickSpecialty(AmenityCounts counts, int score, int roll) {
        return pickSpecialty(counts, score, roll, List.of());
    }

    /**
     * Prefers a specialty that has never spawned and that this house can host.
     * Once every eligible type has appeared, uses the amenity-weighted bag.
     * A job with more living residents than {@link #CROWD_MULTIPLE} times the smallest eligible job
     * is left out. Wise teachers and council members use {@link #RARE_CROWD_MULTIPLE}.
     */
    public static String pickSpecialty(
            AmenityCounts counts, int score, int roll, Collection<String> alreadySpawned) {
        return pickSpecialty(counts, score, roll, alreadySpawned, Map.of());
    }

    public static String pickSpecialty(
            AmenityCounts counts,
            int score,
            int roll,
            Collection<String> alreadySpawned,
            Map<String, Integer> members) {
        int lowest = lowestPossibleMembers(score, members);
        List<String> unseen = new ArrayList<>();
        for (String type : unseenSpecialties(alreadySpawned, score)) {
            if (!crowdedOut(type, memberCount(members, type), lowest)) {
                unseen.add(type);
            }
        }
        if (!unseen.isEmpty()) {
            return unseen.get(Math.floorMod(roll, unseen.size()));
        }
        List<String> open = new ArrayList<>();
        for (String type : specialtyBag(counts, score)) {
            if (!crowdedOut(type, memberCount(members, type), lowest)) {
                open.add(type);
            }
        }
        if (open.isEmpty()) {
            for (String type : SPECIALTIES) {
                if (specialtyAllowed(type, score) && !crowdedOut(type, memberCount(members, type), lowest)) {
                    open.add(type);
                }
            }
        }
        if (open.isEmpty()) {
            open = specialtyBag(counts, score);
        }
        return open.get(Math.floorMod(roll, open.size()));
    }

    /** Living residents of {@code type}. A missing entry counts as none. */
    public static int memberCount(Map<String, Integer> members, String type) {
        if (members == null || type == null) {
            return 0;
        }
        Integer count = members.get(type);
        return count == null ? 0 : Math.max(0, count);
    }

    /** Fewest living residents among the jobs this score can host. */
    public static int lowestPossibleMembers(int score, Map<String, Integer> members) {
        int lowest = Integer.MAX_VALUE;
        boolean any = false;
        for (String type : SPECIALTIES) {
            if (!specialtyAllowed(type, score)) {
                continue;
            }
            any = true;
            lowest = Math.min(lowest, memberCount(members, type));
        }
        return any ? lowest : 0;
    }

    /**
     * True when {@code members} is above twice the smallest eligible job, or above three times
     * for a wise teacher or a council member.
     */
    public static boolean crowdedOut(String type, int members, int lowest) {
        int multiple = WISE_TEACHER.equals(type) || COUNCIL.equals(type) ? RARE_CROWD_MULTIPLE : CROWD_MULTIPLE;
        long floor = Math.max(0, lowest);
        return Math.max(0, members) > floor * multiple;
    }

    static List<String> specialtyBag(AmenityCounts counts, int score) {
        List<String> bag = new ArrayList<>();
        bag.add(TRADER);
        bag.add(TRADER);
        bag.add(OLD_MINER);
        bag.add(OLD_MINER);
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
        }
        if (score >= NOTABLE_SCORE) {
            bag.add(BOUNTY);
            bag.add(BOUNTY);
            bag.add(JEWELER);
            bag.add(JEWELER);
            bag.add(ARMORSMITH);
            bag.add(ARMORSMITH);
            bag.add(WISE_TEACHER);
            bag.add(WISE_TEACHER);
        }
        if (score >= MASTER_SCORE) {
            bag.add(WEALTHY);
            bag.add(WEALTHY);
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
        return bag;
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

    /** Floating name, such as {@code Alden the Jeweler}. */
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
            case BOUNTY -> "loot collector";
            case TRADER -> "trader";
            case WEALTHY -> "wealthy trader";
            case ARMORSMITH -> "armorsmith";
            case MASTER_ARMORSMITH -> "master armorsmith";
            case WISE_TEACHER -> "wise teacher";
            case JEWELER -> "jeweler";
            case COUNCIL -> "council member";
            case OLD_MINER -> "old miner";
            default -> "traveler";
        };
    }

    public static String pickName(int roll) {
        return NAMES[Math.floorMod(roll, NAMES.length)];
    }

    /**
     * Names already given this cycle and names still saved on homes.
     * Only catalog names count. Order is the used list, then the home list.
     */
    public static List<String> namesInUse(Collection<String> used, Collection<String> homeNames) {
        LinkedHashSet<String> taken = new LinkedHashSet<>();
        addListedNames(taken, used);
        addListedNames(taken, homeNames);
        return List.copyOf(taken);
    }

    /**
     * A name that is not in {@code blocked}. When every catalog name is blocked, the roll
     * may repeat one, because every name has already been used.
     */
    public static String pickFreshName(Collection<String> blocked, int roll) {
        List<String> open = new ArrayList<>();
        Set<String> taken = new HashSet<>();
        addListedNames(taken, blocked);
        for (String name : NAMES) {
            if (!taken.contains(name)) {
                open.add(name);
            }
        }
        if (open.isEmpty()) {
            return pickName(roll);
        }
        return open.get(Math.floorMod(roll, open.size()));
    }

    /** True when {@code blocked} already holds every catalog name. */
    public static boolean namesExhausted(Collection<String> blocked) {
        Set<String> taken = new HashSet<>();
        addListedNames(taken, blocked);
        return taken.size() >= NAMES.length;
    }

    /**
     * The set saved after a finished cycle. Living catalog names stay reserved, and so does
     * the name just chosen.
     */
    public static List<String> nextNameCycle(Collection<String> livingNames, String chosen) {
        LinkedHashSet<String> next = new LinkedHashSet<>();
        addListedNames(next, livingNames);
        if (listedName(chosen)) {
            next.add(chosen);
        }
        return List.copyOf(next);
    }

    private static void addListedNames(Set<String> into, Collection<String> names) {
        if (names == null) {
            return;
        }
        for (String name : names) {
            if (listedName(name)) {
                into.add(name);
            }
        }
    }

    public static int nameCount() {
        return NAMES.length;
    }

    /** True when {@code name} is one of the male names new residents are drawn from. */
    public static boolean listedName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        for (String candidate : NAMES) {
            if (candidate.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /** Stable male name for a home that still has an older name. */
    public static String nameForHome(String homeId) {
        return pickName(homeId == null ? 0 : homeId.hashCode());
    }

    /** Cobble (and similar dumps) per emerald. Each storage block takes 2 off the stack of 32, down to 16. */
    public static int haulerStack(int storage) {
        int blocks = Math.max(0, storage);
        return Math.max(HAULER_STACK_MIN, HAULER_STACK - HAULER_PER_STORAGE * blocks);
    }

    /** Items a trader asks for when buying back a stack he is currently selling. */
    public static int traderBuyCount(int sellCount) {
        return Math.max(0, sellCount) + TRADER_BUY_EXTRA;
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

    /** Uses when a focus names the ability. Half the house points, rounded down. */
    public static int wiseFocusUses(int houseScore) {
        return Math.max(0, houseScore) / 2;
    }

    /** Uses when the player hands over emeralds and the teacher picks the ability. The house points. */
    public static int wiseEmeraldUses(int houseScore) {
        return Math.max(0, houseScore);
    }

    /** Price for this house. {@code 0} means the lesson is free. */
    public static int wiseAbilityPrice(int houseScore) {
        int above = Math.max(0, houseScore - NOTABLE_SCORE);
        return Math.max(0, WISE_ABILITY_EMERALDS - above * WISE_PRICE_PER_POINT);
    }

    /**
     * What he asks the player to hold. One emerald or one redstone block stays singular.
     * A free lesson says 0 emeralds.
     */
    public static String handRequest(int price, int redstoneCap) {
        int emeralds = Math.max(0, price);
        int cap = Math.max(1, redstoneCap);
        String emeraldWords = emeralds == 1 ? "1 emerald" : emeralds + " emeralds";
        String blockWords = cap == 1 ? "1 redstone block" : cap + " redstone blocks";
        return "Hold "
                + emeraldWords
                + ", for me to teach you a random ability; an ability focus for that ability (1/2 as effective); or "
                + blockWords
                + " for me to deepen your mana learning capacity.";
    }

    /** What he says to hold when a lesson is offered and does not start. */
    public static String handRequestLine(int price, int redstoneCap) {
        return handRequest(price, redstoneCap);
    }

    /** Emeralds for the price may come from the pack, either hand, or both. */
    public static String emeraldPaymentLine() {
        return "I take those emeralds from your inventory or either hand. You can use either or both.";
    }

    /** True when this lesson has not already been bought and the player can pay. Already knowing it still qualifies. */
    public static boolean shouldTeachAbility(boolean alreadyTaught, int emeralds, int cost) {
        return !alreadyTaught && emeralds >= Math.max(0, cost);
    }

    /** An offered lesson and enough emeralds. Stored mana is not required. */
    public static boolean canTeachFocus(int offers, int emeralds, int cost) {
        return offers > 0 && emeralds >= Math.max(0, cost);
    }

    /** An offered lesson and at least one redstone block. Stored mana is not required. */
    public static boolean canGiveRedstone(int offers, int redstoneBlocks) {
        return offers > 0 && redstoneBlocks >= 1;
    }

    /**
     * Blocks this gift may take. The first gift takes 1, the second up to 4, then 9, 16, and so on.
     * {@code previousGifts} is how many gifts this teacher has already accepted.
     */
    public static int redstoneBlockCap(int previousGifts) {
        long n = Math.max(0, previousGifts) + 1L;
        long squared = n * n;
        if (squared > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) squared;
    }

    /** Blocks actually taken: the cap, or every block held when that is fewer. None when none are held. */
    public static int redstoneBlocksTaken(int previousGifts, int held) {
        if (held <= 0) {
            return 0;
        }
        return Math.min(held, redstoneBlockCap(previousGifts));
    }

    /** Added to mana learning capacity. The square root of the blocks this gift actually took. */
    public static double capacityGain(int blocksTaken) {
        if (blocksTaken <= 0) {
            return 0.0;
        }
        return Math.sqrt(blocksTaken);
    }

    /** Whole numbers stay whole. Other values show one decimal. */
    public static String learningLabel(double value) {
        if (Double.isNaN(value) || value <= 0.0) {
            return "0";
        }
        double tenths = Math.rint(value * 10.0) / 10.0;
        if (Math.abs(tenths - Math.rint(tenths)) < 1.0e-6) {
            return Long.toString(Math.round(tenths));
        }
        return String.format(Locale.ROOT, "%.1f", tenths);
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

    /** Unique listings from {@link #WEALTHY_POOL}; {@code seed} picks the usual shop. */
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
        int listed = WEALTHY.equals(specialty)
                ? Math.min(WEALTHY_OFFER_COUNT, WEALTHY_POOL.size())
                : defaultListings(specialty, 0, false).size();
        if (listed <= 0) {
            return 0;
        }
        return Math.min(USUAL_TRADE_TYPES, listed);
    }

    public static List<TradeListing> defaultListings(String specialty, int storage, boolean blight) {
        String type = specialty == null || specialty.isEmpty() ? TRADER : specialty;
        int stack = haulerStack(storage);
        return switch (type) {
            case HAULER -> List.of(
                    buyOf("minecraft:cobblestone", stack, 3),
                    buyOf("minecraft:dirt", stack, 3),
                    buyOf("minecraft:gravel", stack, 3),
                    buyOf("minecraft:cobbled_deepslate", stack, 3),
                    buyOf("minecraft:netherrack", stack, 3));
            case COOK -> List.of(
                    buyOf("minecraft:cooked_beef", 8, 3),
                    buyOf("minecraft:cooked_porkchop", 8, 3),
                    buyOf("minecraft:cooked_chicken", 8, 3),
                    buyOf("minecraft:baked_potato", 8, 3),
                    sellOf("minecraft:rabbit_stew", 1, 4, 3));
            case FARM -> farmUsualListings();
            case BREWER -> brewerUsualListings();
            case BOUNTY -> List.of(
                    buyOf("minecraft:spider_eye", 4, 3),
                    buyOf("minecraft:gunpowder", 4, 3),
                    buyOf("minecraft:bone", 8, 3));
            case ARMORSMITH -> armorUsualListings(false);
            case MASTER_ARMORSMITH -> armorUsualListings(true);
            case WISE_TEACHER, OLD_MINER -> List.of();
            case COUNCIL -> List.of(
                    sellOf("minecraft:paper", 12, 1, 3),
                    sellOf("minecraft:book", 1, 3, 3),
                    sellOf("minecraft:compass", 1, 5, 3),
                    sellOf("minecraft:clock", 1, 5, 3));
            case WEALTHY -> wealthyTradeListings().subList(0, Math.min(WEALTHY_OFFER_COUNT, WEALTHY_POOL.size()));
            case JEWELER -> JEWELER_USUAL_SELLS;
            default -> List.of(
                    sellOf("minecraft:bread", 4, 1, 3),
                    sellOf("minecraft:coal", 8, 1, 3),
                    sellOf("minecraft:book", 1, 4, 3),
                    sellOf("minecraft:white_wool", 8, 1, 3));
        };
    }

    public static List<TradeListing> extraListings(String specialty, int storage, boolean blight) {
        String type = specialty == null || specialty.isEmpty() ? TRADER : specialty;
        int stack = haulerStack(storage);
        return switch (type) {
            case HAULER -> List.of(
                    buyOf("minecraft:andesite", stack, 3),
                    buyOf("minecraft:diorite", stack, 3),
                    buyOf("minecraft:granite", stack, 3),
                    buyOf("minecraft:tuff", stack, 3),
                    buyOf("minecraft:sand", stack, 3),
                    buyOf("minecraft:red_sand", stack, 3),
                    buyOf("minecraft:mossy_cobblestone", stack, 3),
                    buyOf("minecraft:stone", stack, 3),
                    buyOf("minecraft:deepslate", stack, 3),
                    buyOf("minecraft:basalt", stack, 3),
                    buyOf("minecraft:blackstone", stack, 3),
                    buyOf("minecraft:end_stone", stack, 3),
                    buyOf("minecraft:soul_sand", stack, 3),
                    buyOf("minecraft:calcite", stack, 3));
            case COOK -> List.of(
                    buyOf("minecraft:cooked_mutton", 8, 3),
                    buyOf("minecraft:cooked_rabbit", 8, 3),
                    buyOf("minecraft:cooked_cod", 8, 3),
                    buyOf("minecraft:cooked_salmon", 8, 3),
                    buyOf("minecraft:bread", 8, 3),
                    buyOf("minecraft:cookie", 16, 3),
                    sellOf("minecraft:mushroom_stew", 1, 3, 3),
                    sellOf("minecraft:beetroot_soup", 1, 3, 3),
                    sellOf("minecraft:pumpkin_pie", 1, 3, 3),
                    sellOf("minecraft:honey_bottle", 1, 4, 3));
            case FARM -> farmExtraListings(blight);
            case BREWER -> brewerIngredientListings();
            case BOUNTY -> List.of(
                    buyOf("minecraft:rotten_flesh", 8, 3),
                    buyOf("minecraft:string", 8, 3),
                    buyOf("minecraft:slime_ball", 4, 3),
                    buyOf("minecraft:leather", 6, 3),
                    buyOf("minecraft:arrow", 12, 3),
                    buyOf("minecraft:ink_sac", 6, 3),
                    buyOf("minecraft:phantom_membrane", 2, 3),
                    buyOf("minecraft:ender_pearl", 2, 3),
                    buyOf("minecraft:blaze_rod", 2, 3),
                    buyOf("minecraft:ghast_tear", 1, 3));
            case ARMORSMITH -> List.of(armorMetalBuy(false));
            case WISE_TEACHER, OLD_MINER -> List.of();
            case MASTER_ARMORSMITH -> List.of(armorMetalBuy(true));
            case COUNCIL -> List.of(
                    sellOf("minecraft:map", 1, 4, 3),
                    sellOf("minecraft:name_tag", 1, 8, 3),
                    sellOf("minecraft:writable_book", 1, 4, 3),
                    sellOf("minecraft:item_frame", 4, 1, 3),
                    sellOf("minecraft:lantern", 4, 1, 3));
            case WEALTHY -> List.of();
            case JEWELER -> JEWELER_EXTRA_SELLS;
            default -> List.of(
                    sellOf("minecraft:stick", 16, 1, 3),
                    sellOf("minecraft:paper", 12, 1, 3),
                    sellOf("minecraft:glass", 8, 1, 3),
                    sellOf("minecraft:brick", 8, 1, 3),
                    sellOf("minecraft:iron_ingot", 4, 2, 3),
                    sellOf("minecraft:copper_ingot", 8, 1, 3),
                    sellOf("minecraft:lantern", 4, 1, 3),
                    sellOf("minecraft:arrow", 16, 1, 3),
                    sellOf("minecraft:oak_log", 8, 1, 3),
                    sellOf("minecraft:torch", 8, 1, 3),
                    sellOf("minecraft:leather", 8, 1, 3),
                    sellOf("minecraft:name_tag", 1, 8, 3));
        };
    }

    public static List<TradeListing> genericExtraListings() {
        return List.of(
                sellOf("minecraft:apple", 8, 1, 3),
                sellOf("minecraft:stick", 16, 1, 3),
                sellOf("minecraft:string", 12, 1, 3),
                sellOf("minecraft:feather", 12, 1, 3),
                sellOf("minecraft:flint", 8, 1, 3),
                sellOf("minecraft:clay_ball", 16, 1, 3),
                sellOf("minecraft:paper", 8, 1, 3),
                sellOf("minecraft:glass_bottle", 8, 1, 3),
                sellOf("minecraft:bowl", 8, 1, 3),
                sellOf("minecraft:kelp", 16, 1, 3),
                sellOf("minecraft:bamboo", 16, 1, 3),
                sellOf("minecraft:egg", 8, 1, 3),
                sellOf("minecraft:charcoal", 8, 1, 3),
                sellOf("minecraft:raw_copper", 8, 1, 3),
                sellOf("minecraft:raw_iron", 4, 2, 3),
                buyOf("minecraft:rotten_flesh", 16, 3),
                buyOf("minecraft:poisonous_potato", 8, 3),
                buyOf("minecraft:cactus", 16, 3),
                buyOf("minecraft:kelp", 32, 3),
                buyOf("minecraft:bamboo", 32, 3));
    }

    /**
     * House-score shop: type count and per-listing uses roll from half the top
     * percent up to that top. The top is 50% at 14 points and 175% at 38.
     * Extra types beyond the usual shop are random leftover listings.
     * An armorsmith keeps one rolled heavy piece, then enchanted armor of that metal.
     * A larger roll may add another piece or a buy of iron or diamonds. Generic goods are not added.
     * A brewer keeps one rolled level 2 potion, then up to three other potions.
     * A larger roll may add brewing ingredients. Generic goods are not added.
     * Usual shops keep at most {@link #USUAL_TRADE_TYPES} kinds. Further catalog listings are extras.
     * Item amounts share one roll from -25% to +25%, plus 0 to 25 from the score.
     * Purchases gain that percent; sales to the resident require that percent fewer.
     */
    public static List<TradeListing> scaledListings(
            String specialty, double score, int storage, boolean blight, Random random) {
        Random rng = random == null ? new Random(0L) : random;
        String type = specialty == null || specialty.isEmpty() ? TRADER : specialty;
        if (WISE_TEACHER.equals(type) || OLD_MINER.equals(type)) {
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
            List<TradeListing> plants = new ArrayList<>(FARM_LOWER_SELLS);
            Collections.shuffle(plants, rng);
            int usualSells = Math.min(FARM_USUAL_SELLS, plants.size());
            defaults = new ArrayList<>();
            defaults.add(farmWheatSell());
            defaults.addAll(plants.subList(0, usualSells));
            Collections.shuffle(defaults, rng);
            extras = new ArrayList<>(plants.subList(usualSells, plants.size()));
            extras.addAll(FARM_HIGHER_SELLS);
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
        retainUsualKinds(defaults, extras, rng);
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
        if (TRADER.equals(type)) {
            return traderBuybacks(scaled);
        }
        return scaled;
    }

    /**
     * One buy for each thing this trader is selling. The stack is {@link #TRADER_BUY_EXTRA}
     * larger, the emerald price matches the sale, and the buy starts with no uses.
     */
    static List<TradeListing> traderBuybacks(List<TradeListing> offers) {
        List<TradeListing> paired = new ArrayList<>(offers.size() * 2);
        paired.addAll(offers);
        for (TradeListing listing : offers) {
            if (listing.buy()) {
                continue;
            }
            paired.add(new TradeListing(
                    listing.itemId(),
                    traderBuyCount(listing.count()),
                    Math.max(1, listing.emeralds()),
                    0,
                    true,
                    0,
                    listing.potionId()));
        }
        return List.copyOf(paired);
    }

    /**
     * Keeps the front of {@code defaults}, at most {@link #USUAL_TRADE_TYPES}.
     * The rest join {@code extras}. A heavy piece or level 2 potion stays when it was placed first.
     */
    static void retainUsualKinds(List<TradeListing> defaults, List<TradeListing> extras, Random rng) {
        if (defaults.size() <= USUAL_TRADE_TYPES) {
            return;
        }
        extras.addAll(defaults.subList(USUAL_TRADE_TYPES, defaults.size()));
        defaults.subList(USUAL_TRADE_TYPES, defaults.size()).clear();
        Collections.shuffle(extras, rng);
    }

    /** Eight wheat for one emerald. */
    static TradeListing farmWheatSell() {
        return sellOf("minecraft:wheat", FARM_WHEAT_COUNT, 1, FARM_PLANT_USES);
    }

    /** Wheat sale plus the first lower-list sells. The live shop draws those sells at random. */
    static List<TradeListing> farmUsualListings() {
        List<TradeListing> listings = new ArrayList<>();
        listings.add(farmWheatSell());
        int sells = Math.min(FARM_USUAL_SELLS, FARM_LOWER_SELLS.size());
        listings.addAll(FARM_LOWER_SELLS.subList(0, sells));
        return List.copyOf(listings);
    }

    /** Lower-list sells past the usual pair, then the larger-home sales, then crop buys. */
    static List<TradeListing> farmExtraListings(boolean blight) {
        List<TradeListing> listings = new ArrayList<>();
        if (FARM_LOWER_SELLS.size() > FARM_USUAL_SELLS) {
            listings.addAll(FARM_LOWER_SELLS.subList(FARM_USUAL_SELLS, FARM_LOWER_SELLS.size()));
        }
        listings.addAll(FARM_HIGHER_SELLS);
        listings.addAll(farmSideListings(blight));
        return List.copyOf(listings);
    }

    /** Crop buys that stay with the larger home. Beetroot follows the old wheat quantity, including blight. */
    static List<TradeListing> farmSideListings(boolean blight) {
        return List.of(
                buyOf("minecraft:beetroot", farmWheatBuy(blight), 3),
                buyOf("minecraft:apple", 8, 3));
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
     * One heavy piece plus enchanted armor of every slot. Boots stand in for the heavy piece.
     * The live shop keeps {@link #USUAL_TRADE_TYPES}, with that heavy piece first.
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
        return new TradeListing(itemId, count, 1, defaultUses(uses), true, 0, "");
    }

    static TradeListing sellOf(String itemId, int count, int emeralds, int uses) {
        return new TradeListing(itemId, count, emeralds, defaultUses(uses), false, 0, "");
    }

    static TradeListing sellEnchantedOf(String itemId, int count, int emeralds, int uses, int enchantLevel) {
        return new TradeListing(itemId, count, emeralds, defaultUses(uses), false, Math.max(1, enchantLevel), "");
    }

    /** Armor sales stay at 1. Every other catalog value above {@link #DEFAULT_TRADE_USES} is stored as that. */
    static int defaultUses(int uses) {
        if (uses <= DEFAULT_TRADE_USES) {
            return uses;
        }
        return DEFAULT_TRADE_USES;
    }

    static TradeListing potionSell(BrewPotion potion) {
        return new TradeListing(
                "minecraft:potion", 1, BREWER_POTION_EMERALDS, defaultUses(BREWER_POTION_USES), false, 0, potion.potionId());
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

    /** One of each brewing ingredient. A larger brewer sells these for 1 emerald. */
    static List<TradeListing> brewerIngredientListings() {
        return List.of(
                sellOf("minecraft:nether_wart", 1, BREWER_INGREDIENT_EMERALDS, BREWER_INGREDIENT_USES),
                sellOf("minecraft:blaze_powder", 1, BREWER_INGREDIENT_EMERALDS, BREWER_INGREDIENT_USES),
                sellOf("minecraft:glowstone_dust", 1, BREWER_INGREDIENT_EMERALDS, BREWER_INGREDIENT_USES),
                sellOf("minecraft:redstone", 1, BREWER_INGREDIENT_EMERALDS, BREWER_INGREDIENT_USES),
                sellOf("minecraft:sugar", 1, BREWER_INGREDIENT_EMERALDS, BREWER_INGREDIENT_USES),
                sellOf("minecraft:glass_bottle", 1, BREWER_INGREDIENT_EMERALDS, BREWER_INGREDIENT_USES),
                sellOf("minecraft:spider_eye", 1, BREWER_INGREDIENT_EMERALDS, BREWER_INGREDIENT_USES),
                sellOf("minecraft:gunpowder", 1, BREWER_INGREDIENT_EMERALDS, BREWER_INGREDIENT_USES),
                sellOf("minecraft:magma_cream", 1, BREWER_INGREDIENT_EMERALDS, BREWER_INGREDIENT_USES),
                sellOf("minecraft:fermented_spider_eye", 1, BREWER_INGREDIENT_EMERALDS, BREWER_INGREDIENT_USES),
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

    /** One real minute. Each loaded home is rescanned on its own tick inside this window. */
    public static final int HOUSE_SCAN_TICKS = 1200;

    /** Fifteen seconds. Lesson creatures and last night's bounty creatures are removed on this pass. */
    public static final int EXPIRE_SCAN_TICKS = 300;

    /** Fifteen seconds. Beds near a survival player are considered on this pass. */
    public static final int VISIT_SCAN_TICKS = 300;

    public static boolean houseScanDue(long gameTime, int homeIndex) {
        int phase = Math.max(0, homeIndex);
        return Math.floorMod(gameTime + phase, HOUSE_SCAN_TICKS) == 0;
    }

    /** Two walkable cells (feet and head) over a solid block. */
    public static boolean villagerFits(boolean feetWalkable, boolean headWalkable, boolean solidBelow) {
        return feetWalkable && headWalkable && solidBelow;
    }
}
