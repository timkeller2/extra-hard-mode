package dev.extrahardmode.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Player-facing Tougher help text. Minecraft-free so JUnit can cover it.
 *
 * <p>Each line is one chat message. Minecraft cannot scroll inside a message
 * and only keeps about 100 wrapped lines, so a line stays short and a subject
 * is its own topic.
 */
public final class EhmHelp {
    /** About three wrapped lines at a narrower-than-default chat width. */
    public static final int MAX_LINE_CHARS = 180;

    public static final String HOMES_HELP_PREFIX = "tougher.help.homes.";
    public static final String COMMANDS_KEY = "tougher.command.help";
    public static final String COMMANDS_FALLBACK =
            "Staff (operator): /tougher version|enabled [world]|reload|debug|bypass|set <module> <bool>|enable|disable (/ehm still works)";
    public static final String UNKNOWN_TOPIC_KEY = "tougher.help.unknown";
    public static final String UNKNOWN_TOPIC_FALLBACK =
            "Unknown help topic '%s'. Type /tougher help for the topic list.";
    public static final String UNKNOWN_ABILITY_KEY = "tougher.ability.unknown";
    public static final String UNKNOWN_ABILITY_FALLBACK =
            "Unknown ability '%s'. Type /tougher ability help for the list.";

    private EhmHelp() {}

    /** One chat page: translation keys aligned with fallback lines. */
    public record Page(List<String> keys, List<String> lines) {
        public Page {
            keys = List.copyOf(keys);
            lines = List.copyOf(lines);
        }

        public static Page of(String keyPrefix, List<String> lines) {
            List<String> keys = new ArrayList<>(lines.size());
            for (int i = 0; i < lines.size(); i++) {
                keys.add(keyPrefix + (i + 1));
            }
            return new Page(keys, lines);
        }

        public static Page concat(Page... pages) {
            List<String> keys = new ArrayList<>();
            List<String> lines = new ArrayList<>();
            for (Page page : pages) {
                if (page == null) {
                    continue;
                }
                keys.addAll(page.keys);
                lines.addAll(page.lines);
            }
            return new Page(keys, lines);
        }
    }

    public static List<String> topicSuggestions() {
        List<String> topics = new ArrayList<>();
        topics.addAll(List.of(
                "mining",
                "building",
                "torches",
                "hunger",
                "farming",
                "crops",
                "animals",
                "seasons",
                "soil",
                "processing",
                "combat",
                "armor",
                "mana",
                "abilities",
                "achievements",
                "homes",
                "rooms",
                "furnishings",
                "residents",
                "shops",
                "bounties"));
        topics.addAll(AbilityRules.abilityTopics());
        return List.copyOf(topics);
    }

    /** {@code /tougher} and {@code /tougher help}: the topic list, not every rule. */
    public static Page overview() {
        return Page.of(
                "tougher.help.overview.",
                List.of(
                        "Tougher — harder survival. Type /tougher help <topic>, or /tougher ability help.",
                        "Topics: mining, building, torches, hunger, farming, combat, armor, mana, abilities, achievements, homes.",
                        "Farming subjects: crops, animals, seasons, soil, processing.",
                        "Home subjects: rooms, furnishings, residents, shops, bounties. Checklist: /tougher help homes.",
                        "Mana abilities: /tougher ability help, or /tougher ability help <name>. /tougher me lists your skills.",
                        "Staff (operator): /tougher commands."));
    }

    /**
     * A help topic, or one mana ability when {@code topic} names one
     * ({@code healing}, {@code fire bolt}, {@code ability slow}).
     */
    public static Page resolve(String topic) {
        if (topic == null || topic.isBlank()) {
            return null;
        }
        String key = normalize(topic);
        if (key.startsWith("ability ")) {
            key = key.substring("ability ".length()).trim();
        }
        String ability = AbilityRules.abilityByTopic(key);
        if (ability != null) {
            return abilityPage(ability);
        }
        return subject(key);
    }

    public static Page abilityIndex() {
        return Page.concat(
                new Page(AbilityRules.powerHelpKeys(), AbilityRules.powerHelpLines()),
                new Page(AbilityRules.indexHelpKeys(), AbilityRules.indexHelpLines()));
    }

    public static Page abilityPage(String ability) {
        List<String> parts = AbilityRules.helpParts(ability);
        if (parts == null || parts.isEmpty()) {
            return null;
        }
        return Page.concat(
                new Page(AbilityRules.powerHelpKeys(), AbilityRules.powerHelpLines()),
                new Page(AbilityRules.helpKeys(ability), parts));
    }

    /** Every subject, for tests. {@code /tougher} does not send this. */
    public static List<String> featureHelpLines() {
        return featureHelp().lines();
    }

    public static List<String> featureHelpKeys() {
        return featureHelp().keys();
    }

    public static List<String> abilityHelpLines() {
        return abilityIndex().lines();
    }

    public static List<String> abilityHelpKeys() {
        return abilityIndex().keys();
    }

    public static List<String> homesHelpLines() {
        return homes().lines();
    }

    public static List<String> homesHelpKeys() {
        return homes().keys();
    }

    private static Page featureHelp() {
        return Page.concat(
                overview(),
                mining(),
                building(),
                torches(),
                hunger(),
                crops(),
                animals(),
                seasons(),
                soil(),
                processing(),
                combat(),
                armor(),
                mana(),
                abilities(),
                achievements(),
                homes());
    }

    private static Page subject(String key) {
        return switch (key) {
            case "mining" -> mining();
            case "building", "build" -> building();
            case "torches", "torch", "campfire", "campfires" -> torches();
            case "hunger", "food" -> hunger();
            case "crops", "crop", "plants", "trees" -> crops();
            case "animals", "animal", "livestock", "grazing", "overgrazing" -> animals();
            case "seasons", "season" -> seasons();
            case "soil" -> soil();
            case "processing", "compost", "composters", "furnace", "furnaces" -> processing();
            case "farming", "farm" -> Page.concat(crops(), animals(), seasons(), soil(), processing());
            case "combat", "mobs", "shields", "bosses" -> combat();
            case "armor" -> armor();
            case "mana" -> mana();
            case "abilities", "ability" -> abilities();
            case "achievements", "achievement", "achieve" -> achievements();
            case "homes", "home", "residence", "residences" -> homes();
            case "rooms", "room" -> Page.of("tougher.help.rooms.", InhabitantRules.homesRoomLines());
            case "furnishings", "furnishing", "furniture" ->
                Page.of("tougher.help.furnishings.", InhabitantRules.homesFurnishingLines());
            case "residents", "resident" ->
                Page.of("tougher.help.residents.", InhabitantRules.homesResidentLines());
            case "shops", "shop" -> Page.of("tougher.help.shops.", InhabitantRules.homesShopLines());
            case "bounties", "bounty" -> Page.of("tougher.help.bounties.", InhabitantRules.homesBountyLines());
            default -> null;
        };
    }

    private static Page mining() {
        return Page.of(
                "tougher.help.mining.",
                List.of(
                        "Mining: Stone and deepslate need an iron or better pickaxe. Breaking nearby coal or ore can soften stone.",
                        "Mining: Wood, stone, and copper tools last one-third as long."));
    }

    private static Page building() {
        return Page.of(
                "tougher.help.building.",
                List.of(
                        "Building: You cannot place blocks while in the air, even from the edge of a block.",
                        "Building: You cannot carry a torch, lantern, or glowstone in your off hand.",
                        "Building: Bucketed water evaporates; use ice if you need a water source.",
                        "Building: A boat that falls more than 3 blocks with you in it breaks and drops nothing.",
                        "Building: Torches and campfires have their own topic. Type /tougher help torches."));
    }

    private static Page torches() {
        return Page.of(
                "tougher.help.torches.",
                List.of(
                        "Torches: Placed torches need airflow and a firm block, and campfires also need airflow.",
                        "Torches: Placed torches burn out after 7 Minecraft days (set burn days to 0 to keep them) and dim after 2 days.",
                        "Torches: Light is 14 − (days burning − 2). Copper torches last twice as long.",
                        "Torches: Right-click a torch with coal or charcoal to add 40 days (80 on a copper torch). One item is consumed. That does not make it permanent.",
                        "Torches: Look at a torch, copper torch, or campfire to see remaining time (Permanent if it never burns out).",
                        "Torches: Breaking a torch with less than 3 days left destroys it. Permanent torches still drop.",
                        "Torches: Permanent torches stay full-bright.",
                        "Torches: Placed campfires also burn out after 7 days and disappear, unless they pull a log from a chest within 12 blocks.",
                        "Torches: Pulling that log adds 7 more days."));
    }

    private static Page hunger() {
        return Page.of(
                "tougher.help.hunger.",
                List.of(
                        "Hunger: Your hunger drops while you move or act, not while AFK.",
                        "Hunger: A food not in your last 7 eaten gives +1 hunger, or +1 saturation if you are already full.",
                        "Hunger: Seven different foods in the last 7 meals gives an extra +1 hunger and saturation and 3 experience.",
                        "Hunger: The same food 5 times in the last 7 meals restores less; 7 of the last 7 restores even less."));
    }

    private static Page crops() {
        return Page.of(
                "tougher.help.crops.",
                List.of(
                        "Crops: Crops grow slowly. Sugar cane is 1/10 vanilla speed. Nether wart grows at 1/20 vanilla speed.",
                        "Crops: When a one-block cane would grow a second segment, it may become a weed at the current crop loss rate plus the soil under the cane.",
                        "Crops: Trees take 10 times as long to grow. Leaves drop half as many saplings.",
                        "Crops: Pumpkins and melons fruit at 1/10 vanilla rate. After each fruit the vine may become a weed, starting at 0% and rising 5% per fruit.",
                        "Crops: Broken pumpkin and melon vines drop no seeds. Shearing a full hive gives 1 honeycomb."));
    }

    private static Page animals() {
        return Page.of(
                "tougher.help.animals.",
                List.of(
                        "Animals: Animals wait 6 times as long to breed again. Chickens lay eggs 6 times as slowly.",
                        "Animals: Once a day livestock overgraze by claiming nearby grass they can walk to within 6 blocks.",
                        "Animals: A large animal claims 9 patches. A chicken or rabbit claims 4. A patch is only claimed once that day.",
                        "Animals: If they cannot claim enough grass they mark none, then have a 33% chance to eat breeding food from a chest.",
                        "Animals: That chest must be reachable within 20 blocks. Otherwise they starve without dropping meat if none is left.",
                        "Animals: Villagers take 8 times as long to restock their trades."));
    }

    private static Page seasons() {
        return Page.of(
                "tougher.help.seasons.",
                List.of(
                        "Seasons: By default crop loss and growth follow a season. Loss falls 1% per day from the normal rate down to half.",
                        "Seasons: Loss then rises to three times the normal rate, then repeats. Turn changing seasons off to keep the base crop loss rate instead.",
                        "Seasons: Growth speed scales by that seasonal loss plus the soil modifier of the plant's own plot, never faster than vanilla.",
                        "Seasons: Ripe food crops and second-segment sugar cane die at that seasonal loss plus the plot's soil modifier.",
                        "Seasons: For example, 51% loss and −25 soil is 26%. Let it grow never causes that death.",
                        "Seasons: When the loss rate is above 25%, cows, sheep, and pigs drop 1 less meat.",
                        "Seasons: When the loss rate is above 60%, those animals drop 2 less, chickens drop 1 less, and bees stay in their hives.",
                        "Seasons: Above 60% loss, the air turns blighted."));
    }

    private static Page soil() {
        return Page.of(
                "tougher.help.soil.",
                List.of(
                        "Soil: Unworked soil starts at 0. Look at farmland, a crop, or sugar cane to see that plot's shown soil modifier.",
                        "Soil: It is a small percent, green when at or above 0, red when below 0; positive is good, for example 13%.",
                        "Soil: The first till adds water sources within 2, up to 50. Past 50, each extra source subtracts 1 from 50, down to 0.",
                        "Soil: A 1d10 is then subtracted, and twice the hoe quality is added: wood/stone 0, copper 2, iron 4, diamond 6, gold 8, netherite 10.",
                        "Soil: Later tilling or harvesting with a hoe moves that shown value toward the hoe's target.",
                        "Soil: Hoe targets: wood/stone −10, copper −5, iron 0, diamond +10, gold +20, netherite +30.",
                        "Soil: If that would make the plot worse, the step is 1/10 of the gap to the target (at least 1, at most 4).",
                        "Soil: If it would improve the plot, the step is 3. Harvesting by any other means subtracts 5.",
                        "Soil: Planting the same crop again on a plot subtracts 5 from the shown modifier.",
                        "Soil: Harvesting sugar cane updates the soil under the cane the same way.",
                        "Soil: Let it grow adds 3 to the soil under each plant it grows. Bone meal on a plant adds 5 to that plot's shown soil modifier.",
                        "Soil: Right-click a clock to see the current loss rate and your mana abilities.",
                        "Soil: The clock also lists the closest slayer achievement, your five closest builder achievements, and the last block you placed."));
    }

    private static Page processing() {
        return Page.of(
                "tougher.help.processing.",
                List.of(
                        "Farming: Composters fill normally but take much longer to finish.",
                        "Farming: Hopper output from furnaces stores cooking XP in the destination chest at 50% extra; taking those items grants it.",
                        "Farming: Composters do the same for bone meal, using the same XP as smelting iron."));
    }

    private static Page combat() {
        return Page.of(
                "tougher.help.combat.",
                List.of(
                        "Combat: Mobs hit harder. Skeletons ignore arrows.",
                        "Combat: Skeleton and bogged special shots only happen based on how far they spawned from world spawn: 1% per 10 blocks.",
                        "Combat: Those special shots always happen at 1000 blocks or farther.",
                        "Combat: Zombies vary ±20% in speed (faster hit softer and look smaller, slower hit harder and look larger).",
                        "Combat: Zombies can rise again. Shields absorb 100% of a blocked hit and take 3 times the durability.",
                        "Combat: Creepers may drop live TNT. Strong biome bosses can appear far from spawn.",
                        "Combat: Each biome boss appears once. After it is defeated, it does not return in that game.",
                        "Combat: Bosses do not spawn within 80 blocks of a player or a chest.",
                        "Combat: Brood mothers chase when hit. Every few seconds they spit for half damage and a brief blind.",
                        "Combat: A biome boss breaks blocks in its way at iron pickaxe speed when that is how it reaches you.",
                        "Combat: Each defeated boss makes the next 30% harder and richer. The third is a world event.",
                        "Combat: The seventh rolls the Minecraft credits, and you can keep playing."));
    }

    private static Page armor() {
        return Page.of(
                "tougher.help.armor.",
                List.of(
                        "Armor: Heavy copper, iron, diamond, and netherite armor add extra hearts (5 / 10 / 20 / 30 for a full set).",
                        "Armor: Those pieces last twice as long as the matching vanilla pieces.",
                        "Armor: Healing and food regen fill every heart, not just the first ten."));
    }

    private static Page mana() {
        return Page.of(
                "tougher.help.mana.",
                List.of(
                        "Mana: Cyan crystals. You regen (mana level + current mana) / 200 per minute.",
                        "Mana: Quartz in your inventory speeds regen until you reach your mana level, consuming 1 quartz per 2 mana (one crystal).",
                        "Mana: If your mana is below your mana level and below (saturation − 17), you also restore 1 mana per minute for 1 saturation.",
                        "Mana: That saturation restore does not count toward quartz.",
                        "Mana: Past your level you regen at 1/4 speed, up to 20 mana (10 crystals). Achievements can raise your mana level."));
    }

    private static Page abilities() {
        return Page.of(
                "tougher.help.abilities.",
                List.of(
                        "Abilities: Hold the catalyst and right-click. Let it grow on a plant; coal for Let there be light.",
                        "Abilities: Use charcoal for Fire bolt, an arrow for Magic arrow, an iron ingot for Iron Heart, or a golden sword for Smite Evil.",
                        "Abilities: Redstone dust in your inventory is consumed for +2 on any ability.",
                        "Abilities: You can learn up to half your mana level in abilities, rounded up.",
                        "Abilities: Unlearned abilities can be used and still gain skill, at −3 power (floored at 1), until you have a free slot to learn them.",
                        "Abilities: A wise teacher in a 24-point home can teach one ability for 48 emeralds, ignoring that limit.",
                        "Abilities: Sneak while carrying a diamond block and a lapis lazuli block and that teacher raises your mana level once.",
                        "Abilities: Hold an item and press ? for details, or type /tougher ability help <name>.",
                        "Abilities: /tougher me lists your mana level and ability skill levels."));
    }

    private static Page achievements() {
        return Page.of(
                "tougher.help.achievements.",
                List.of(
                        "Achievements: Place many of one block or defeat many of one monster that drops loot.",
                        "Achievements: Each achievement gives 50 experience and a diamond. Everyone can earn every achievement.",
                        "Achievements: By default the first player to claim one server-wide gets 25 extra experience.",
                        "Achievements: You have (your experience level)% chance to gain a mana level.",
                        "Achievements: A diamond block plus enough lapis lazuli blocks buys an extra mana level and sparkles you blue for 6 seconds.",
                        "Achievements: That cost is 1 diamond block plus lapis, starting at 1, then 1 more each time you pay this way.",
                        "Achievements: If you have a diamond block but not enough lapis, you are told how many lapis blocks you currently need.",
                        "Achievements: /tougher achieve lists the 5 closest builder tracks and at most 3 slayer tracks, plus the last block you placed.",
                        "Achievements: The first time you visit a biome you gain 50 experience, and 100 more if you are the first player to visit it.",
                        "Achievements: The first time you build a habitable home in a biome you gain (house points × 3) experience.",
                        "Achievements: The first time you travel 300 blocks from spawn you gain 100 experience."));
    }

    private static Page homes() {
        return Page.concat(
                Page.of(HOMES_HELP_PREFIX + "intro.", InhabitantRules.homesIntroLines()),
                Page.of("tougher.help.rooms.", InhabitantRules.homesRoomLines()),
                Page.of("tougher.help.furnishings.", InhabitantRules.homesFurnishingLines()),
                Page.of("tougher.help.residents.", InhabitantRules.homesResidentLines()),
                Page.of("tougher.help.shops.", InhabitantRules.homesShopLines()),
                Page.of("tougher.help.bounties.", InhabitantRules.homesBountyLines()));
    }

    private static String normalize(String topic) {
        String key = topic.trim().toLowerCase(Locale.ROOT).replace('-', ' ').replace('_', ' ');
        while (key.contains("  ")) {
            key = key.replace("  ", " ");
        }
        return key;
    }
}
