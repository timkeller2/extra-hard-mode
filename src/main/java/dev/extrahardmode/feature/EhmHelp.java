package dev.extrahardmode.feature;

import java.util.ArrayList;
import java.util.List;

/**
 * Player-facing Extra Hard Mode help text. Minecraft-free so JUnit can cover it.
 */
public final class EhmHelp {
    private EhmHelp() {}

    public static final String FEATURE_HELP_PREFIX = "extrahardmode.help.feature.";
    public static final String HOMES_HELP_PREFIX = "extrahardmode.help.homes.";
    public static final String COMMANDS_KEY = "extrahardmode.command.help";
    public static final String COMMANDS_FALLBACK =
            "Staff (operator): /ehm version|enabled [world]|reload|debug|bypass|set <module> <bool>|enable|disable";

    public static List<String> featureHelpLines() {
        return List.of(
                "Extra Hard Mode — harder survival. Type /ehm ability help for mana abilities, or /ehm help homes for residences.",
                "Mining: Stone and deepslate need an iron or better pickaxe. Breaking nearby coal or ore can soften stone. Wood, stone, and copper tools last one-third as long.",
                "Building: You cannot place blocks in mid-air. Torches need airflow and a firm block; campfires also need airflow. Placed torches burn out after 7 Minecraft days (set burn days to 0 to keep them) and dim after 2 days (light 14 − (days burning − 2)). If they find coal or charcoal in a chest within 16 blocks, they consume one and last 30 more days. Copper torches last twice as long and refill for 60 days at a time. Permanent torches stay full-bright. Placed campfires also burn out after 7 days and disappear, unless they pull a log from a chest within 12 blocks, which adds 7 more days. You cannot carry a torch, lantern, or glowstone in your off hand. Bucketed water evaporates; use ice if you need a water source. A boat that falls more than 3 blocks with you in it breaks and drops nothing.",
                "Hunger: Your hunger drops while you move or act, not while AFK. A food not in your last 7 eaten gives +1 hunger, or +1 saturation if you are already full. Seven different foods in the last 7 meals gives an extra +1 hunger and saturation and 3 experience. The same food 5 times in the last 7 meals restores less; 7 of the last 7 restores even less.",
                "Farming: Crops grow slowly. Sugar cane is 1/10 vanilla speed. Nether wart grows at 1/20 vanilla speed. When a one-block cane would grow a second segment, it may become a weed at the current crop loss rate plus the soil under the cane. Trees take 10 times as long to grow. Leaves drop half as many saplings. Pumpkins and melons fruit at 1/10 vanilla rate; after each fruit the vine may become a weed, starting at 0% and rising 5% per fruit. Broken pumpkin and melon vines drop no seeds. Shearing a full hive gives 1 honeycomb. Animals wait 6 times as long to breed again. When 16 breedable animals are within 8 blocks, they overgraze: once a day a third of them may eat breeding food from a reachable chest within 20 blocks, or starve without dropping meat if none is left. Chickens lay eggs twice as slowly. Villagers take 8 times as long to restock their trades. Crop loss and growth follow a season: loss falls 1% per day from the normal rate down to half, then rises to three times, then repeats; growth speed scales by that seasonal loss plus the soil modifier of the plant's own plot, never faster than vanilla. Ripe food crops and second-segment sugar cane die at that seasonal loss plus the plot's soil modifier (51% loss and −25 soil is 26%). Let it grow never causes that death. When the loss rate is above 25%, cows, sheep, and pigs drop 1 less meat. When the loss rate is above 60%, those animals drop 2 less, chickens drop 1 less, bees stay in their hives, and the air turns blighted. Unworked soil starts at 0. Hold a hoe and look at farmland, a crop, or sugar cane to see that plot's shown soil modifier as a small percent (green when at or above 0, red when below 0; positive is good), for example 13%. The first time you till a plot, its shown modifier is (water source blocks within 2) minus 1d10, at most +25. Later tilling or harvesting with a hoe moves that shown value toward the hoe's target (wood/stone −10, copper −5, iron 0, diamond +10, gold +20, netherite +30). If that would make the plot worse, the step is 1/10 of the gap to the target (at least 1, at most 4). If it would improve the plot, the step is 3. Harvesting by any other means subtracts 5. Planting the same crop again on a plot subtracts 5 from the shown modifier. Harvesting sugar cane updates the soil under the cane the same way. Let it grow adds 3 to the soil under each plant it grows. Bone meal on a plant adds 5 to that plot's shown soil modifier. Right-click a clock to see the current loss rate, your mana abilities, the closest slayer achievement, your five closest builder achievements, and progress on the last block you placed. Composters fill normally but take much longer to finish. Hopper output from furnaces stores cooking XP in the destination chest at 50% extra; taking those items grants it. Composters do the same for bone meal, using the same XP as smelting iron.",
                "Combat: Mobs hit harder. Skeletons ignore arrows. Zombies vary ±20% in speed (faster hit softer and look smaller, slower hit harder and look larger) and can rise again. Shields absorb 75% of a blocked hit and take double the durability. Creepers may drop live TNT. Strong biome bosses can appear far from spawn. Each defeated boss makes the next 30% harder and richer. The third is a world event; the seventh rolls the Minecraft credits, and you can keep playing.",
                "Armor: Heavy copper, iron, diamond, and netherite armor add extra hearts (5 / 10 / 20 / 30 for a full set) and last twice as long as the matching vanilla pieces. Healing and food regen fill every heart, not just the first ten.",
                "Mana: Cyan crystals. You regen (mana level + current mana) / 200 per minute; quartz in your inventory speeds it until you reach your mana level, consuming 1 quartz per 2 mana (one crystal) regained. If your mana is below your mana level and below (saturation − 17), you also restore 1 mana per minute for 1 saturation; that restore does not count toward quartz. Past your level you regen at 1/4 speed, up to 20 mana (10 crystals). Achievements can raise your mana level.",
                "Abilities: Hold the catalyst item and right-click (Let it grow on a plant; coal for Let there be light; charcoal for Fire bolt; an arrow for Magic arrow; an iron ingot for Iron Heart; a golden sword for Smite Evil). Redstone dust in your inventory is consumed for +2 on any ability. You can learn up to half your mana level in abilities, rounded up. Unlearned abilities can be used and still gain skill, at −3 power (floored at 1), until you have a free slot to learn them. Hold an item and press ? for details, or type /ehm ability help. /ehm me lists your mana level and ability skill levels.",
                "Achievements: Place many of one block or defeat many of one monster that drops loot. Each achievement gives 50 experience and a diamond. By default only the first player to claim an achievement gets it, and it leaves everyone's upcoming list. You have (your experience level)% chance to gain a mana level. Carrying a diamond block and enough lapis lazuli blocks consumes 1 diamond block plus lapis (starting at 1, then 1 more each time you pay this way) for an extra mana level and sparkles you blue for 6 seconds. If you have a diamond block but not enough lapis, you are told how many lapis blocks you currently need. /ehm achieve lists the 5 closest builder tracks and at most 3 slayer tracks, plus progress on the last block you placed. The first time you visit a biome you gain 100 experience, and 100 more if you are the first player to visit it. The first time you travel 300 blocks from spawn you gain 100 experience.",
                "Homes: An enclosed, well-lit room with a bed, a door, and furnishings (windows, art, rugs) may attract one traveler. They dislike crowding. Right-click a bed with a clock to inspect the room. Type /ehm help homes for the full checklist and furnishing points. Residents may buy spare blocks, trade food, post bounties, keep a small shop, or (in finer homes) sell heavy armor or rare goods. Council members give each player a kill bounty. A house that only just qualifies for that resident stocks 25-50% of the usual listings and uses; each extra point raises that range by 10%, and extra slots are random trades. Most restock every 3 Minecraft days; armorsmiths restock every 30. Staff can toggle it with /ehm set inhabitants true (requires operator).",
                COMMANDS_FALLBACK);
    }

    public static List<String> featureHelpKeys() {
        List<String> lines = featureHelpLines();
        List<String> keys = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            if (i == lines.size() - 1) {
                keys.add(COMMANDS_KEY);
            } else {
                keys.add(FEATURE_HELP_PREFIX + (i + 1));
            }
        }
        return keys;
    }

    public static List<String> abilityHelpLines() {
        List<String> lines = new ArrayList<>();
        lines.add(AbilityRules.powerHelpFallback());
        lines.addAll(AbilityRules.indexHelpLines());
        for (String ability : AbilityRules.ABILITY_IDS) {
            String help = AbilityRules.helpFallback(ability);
            if (help != null) {
                lines.add(help);
            }
        }
        return lines;
    }

    public static List<String> homesHelpLines() {
        return InhabitantRules.homesHelpLines();
    }

    public static List<String> homesHelpKeys() {
        List<String> lines = homesHelpLines();
        List<String> keys = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            keys.add(HOMES_HELP_PREFIX + (i + 1));
        }
        return keys;
    }

    public static List<String> abilityHelpKeys() {
        List<String> keys = new ArrayList<>();
        keys.add(AbilityRules.POWER_HELP_KEY);
        keys.addAll(AbilityRules.indexHelpKeys());
        for (String ability : AbilityRules.ABILITY_IDS) {
            keys.add(AbilityRules.helpKey(ability));
        }
        return keys;
    }
}
