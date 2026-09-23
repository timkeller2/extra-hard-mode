package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class EhmHelpTest {
    @Test
    void featureHelpCoversManaAndAbilities() {
        var lines = EhmHelp.featureHelpLines();
        assertEquals(lines.size(), EhmHelp.featureHelpKeys().size());
        assertTrue(lines.getFirst().contains("/tougher ability help"));
        for (String line : lines) {
            assertTrue(line.length() <= EhmHelp.MAX_LINE_CHARS, line);
        }
        assertTrue(String.join(" ", lines).contains("1/4"));
        assertTrue(String.join(" ", lines).contains("/ 200 per minute"));
        assertTrue(String.join(" ", lines).contains("saturation"));
        assertTrue(String.join(" ", lines).contains("Well Fed"));
        assertTrue(String.join(" ", lines).contains("17"));
        assertTrue(String.join(" ", lines).contains("1 quartz per 2 mana"));
        assertTrue(String.join(" ", lines).contains("20 mana"));
        assertTrue(String.join(" ", lines).contains("/tougher ability help"));
        assertTrue(String.join(" ", lines).contains("/tougher me"));
        assertFalse(String.join(" ", lines).contains("diversification"));
        assertTrue(String.join(" ", lines).contains("closest slayer"));
        assertTrue(String.join(" ", lines).contains("five closest builder"));
        assertTrue(String.join(" ", lines).contains("last block you placed"));
        assertTrue(String.join(" ", lines).contains("Stone and deepslate"));
        assertFalse(String.join(" ", lines).contains("tuff"));
        assertTrue(String.join(" ", lines).contains("Let it grow"));
        assertTrue(String.join(" ", lines).contains("Let there be light"));
        assertTrue(String.join(" ", lines).contains("charcoal for Fire bolt"));
        assertTrue(String.join(" ", lines).contains("an arrow for Magic arrow"));
        assertTrue(String.join(" ", lines).contains("an iron ingot for Iron Heart"));
        assertTrue(String.join(" ", lines).contains("a golden sword for Smite Evil"));
        assertTrue(String.join(" ", lines).contains("+2 on any ability"));
        assertTrue(String.join(" ", lines).contains("half your mana level"));
        assertTrue(String.join(" ", lines).contains("wise teacher"));
        assertTrue(String.join(" ", lines).contains("30 emeralds"));
        assertTrue(String.join(" ", lines).contains("down to free"));
        assertTrue(String.join(" ", lines).contains("does not count toward abilities you learn yourself"));
        assertTrue(String.join(" ", lines).contains("does not explode"));
        assertTrue(String.join(" ", lines).contains("lapis lazuli block"));
        assertTrue(String.join(" ", lines).contains("Redstone dust"));
        assertTrue(String.join(" ", lines).contains("Pumpkins and melons"));
        assertTrue(String.join(" ", lines).contains("drop no seeds"));
        assertTrue(String.join(" ", lines).contains("6 times as long to breed"));
        assertTrue(String.join(" ", lines).contains("overgraze"));
        assertTrue(String.join(" ", lines).contains("9 patches"));
        assertTrue(String.join(" ", lines).contains("chicken or rabbit"));
        assertTrue(String.join(" ", lines).contains("starve without dropping meat"));
        assertTrue(String.join(" ", lines).contains("lay eggs 6 times"));
        assertTrue(String.join(" ", lines).contains("Trees take 10 times"));
        assertTrue(String.join(" ", lines).contains("Nether wart grows at 1/20"));
        assertTrue(String.join(" ", lines).contains("half as many saplings"));
        assertTrue(String.join(" ", lines).contains("clock"));
        assertTrue(String.join(" ", lines).contains("falls 1% per day"));
        assertTrue(String.join(" ", lines).contains("changing seasons off"));
        assertTrue(String.join(" ", lines).contains("base crop loss rate"));
        assertTrue(String.join(" ", lines).contains("soil modifier of the plant's own plot"));
        assertTrue(String.join(" ", lines).contains("never faster than vanilla"));
        assertTrue(String.join(" ", lines).contains("51% loss and"));
        assertTrue(String.join(" ", lines).contains("Let it grow never causes that death"));
        assertTrue(String.join(" ", lines).contains("three times"));
        assertTrue(String.join(" ", lines).contains("above 60%"));
        assertTrue(String.join(" ", lines).contains("bees stay"));
        assertTrue(String.join(" ", lines).contains("1 less meat"));
        assertTrue(String.join(" ", lines).contains("chickens drop 1 less"));
        assertFalse(String.join(" ", lines).contains("minimum 1"));
        assertTrue(String.join(" ", lines).contains("1 honeycomb"));
        assertTrue(String.join(" ", lines).contains("second segment"));
        assertTrue(String.join(" ", lines).contains("8 times as long to restock"));
        assertTrue(String.join(" ", lines).contains("positive is good"));
        assertTrue(String.join(" ", lines).contains("13%"));
        assertTrue(String.join(" ", lines).contains("Look at farmland"));
        assertTrue(String.join(" ", lines).contains("Look at a torch"));
        assertTrue(String.join(" ", lines).contains("bone meal"));
        assertTrue(String.join(" ", lines).contains("small percent"));
        assertTrue(String.join(" ", lines).contains("red when below 0"));
        assertTrue(String.join(" ", lines).contains("soil under the cane"));
        assertTrue(String.join(" ", lines).contains("starts at 0"));
        assertTrue(String.join(" ", lines).contains("1d10"));
        assertTrue(String.join(" ", lines).contains("water source"));
        assertTrue(String.join(" ", lines).contains("diamond +10"));
        assertTrue(String.join(" ", lines).contains("gold +20"));
        assertTrue(String.join(" ", lines).contains("netherite +30"));
        assertTrue(String.join(" ", lines).contains("1/10 of the gap"));
        assertTrue(String.join(" ", lines).contains("at most 4"));
        assertTrue(String.join(" ", lines).contains("the step is 3"));
        assertTrue(String.join(" ", lines).contains("subtracts 5"));
        assertTrue(String.join(" ", lines).contains("same crop again"));
        assertTrue(String.join(" ", lines).contains("Let it grow adds 3"));
        assertTrue(String.join(" ", lines).contains("Bone meal on a plant adds 5"));
        assertTrue(String.join(" ", lines).contains("cooking XP"));
        assertTrue(String.join(" ", lines).contains("50% extra"));
        assertTrue(String.join(" ", lines).contains("Composters do the same for bone meal"));
        assertTrue(String.join(" ", lines).contains("smelting iron"));
        assertTrue(String.join(" ", lines).contains("campfires also need airflow"));
        assertTrue(String.join(" ", lines).contains("burn out after 7 Minecraft days"));
        assertTrue(String.join(" ", lines).contains("less than 3 days"));
        assertTrue(String.join(" ", lines).contains("coal or charcoal"));
        assertTrue(String.join(" ", lines).contains("add 40 days"));
        assertTrue(String.join(" ", lines).contains("80 on a copper torch"));
        assertTrue(String.join(" ", lines).contains("does not make it permanent"));
        assertTrue(String.join(" ", lines).contains("Copper torches last twice as long"));
        assertTrue(String.join(" ", lines).contains("remaining time"));
        assertTrue(String.join(" ", lines).contains("Permanent if it never burns out"));
        assertTrue(String.join(" ", lines).contains("Redstone torches are permanent"));
        assertTrue(String.join(" ", lines).contains("dim after 2 days"));
        assertTrue(String.join(" ", lines).contains("pull a log"));
        assertTrue(String.join(" ", lines).contains("12 blocks"));
        assertTrue(String.join(" ", lines).contains("7 more days"));
        assertTrue(String.join(" ", lines).contains("off hand"));
        assertTrue(String.join(" ", lines).contains("falls more than 3 blocks"));
        assertTrue(String.join(" ", lines).contains("drops nothing"));
        assertTrue(String.join(" ", lines).contains("Breaking a block that is on fire"));
        assertTrue(String.join(" ", lines).contains("50 experience"));
        assertTrue(String.join(" ", lines).contains("diamond block"));
        assertTrue(String.join(" ", lines).contains("1 more each time"));
        assertTrue(String.join(" ", lines).contains("how many lapis"));
        assertTrue(String.join(" ", lines).contains("sparkles you blue"));
        assertTrue(String.join(" ", lines).contains("50 experience and a diamond"));
        assertTrue(String.join(" ", lines).contains("visit a biome you gain 50 experience"));
        assertTrue(String.join(" ", lines).contains("first player to visit it"));
        assertTrue(String.join(" ", lines).contains("habitable home in a biome"));
        assertTrue(String.join(" ", lines).contains("house points × 3"));
        assertTrue(String.join(" ", lines).contains("300 blocks from spawn"));
        assertTrue(String.join(" ", lines).contains("Everyone can earn every achievement"));
        assertTrue(String.join(" ", lines).contains("25 extra experience"));
        assertTrue(String.join(" ", lines).contains("3 experience"));
        assertTrue(String.join(" ", lines).contains("faster hit softer"));
        assertTrue(String.join(" ", lines).contains("1% per 10 blocks"));
        assertTrue(String.join(" ", lines).contains("1000 blocks or farther"));
        assertTrue(String.join(" ", lines).contains("look smaller"));
        assertTrue(String.join(" ", lines).contains("Shields absorb 100%"));
        assertTrue(String.join(" ", lines).contains("3 times the durability"));
        assertTrue(String.join(" ", lines).contains("appears once"));
        assertTrue(String.join(" ", lines).contains("does not return"));
        assertTrue(String.join(" ", lines).contains("80 blocks"));
        assertTrue(String.join(" ", lines).contains("brief blind"));
        assertTrue(String.join(" ", lines).contains("iron pickaxe speed"));
        assertTrue(String.join(" ", lines).contains("explosions.tnt.firePercent"));
        assertTrue(String.join(" ", lines).contains("20% of the blocks it hits"));
        assertTrue(String.join(" ", lines).contains("30% harder"));
        assertTrue(String.join(" ", lines).contains("credits"));
        assertTrue(String.join(" ", lines).contains("twice as long"));
        assertTrue(String.join(" ", lines).contains("one traveler"));
        assertTrue(String.join(" ", lines).contains("/tougher help homes"));
        assertTrue(String.join(" ", lines).contains("/tougher set inhabitants true"));
        assertTrue(String.join(" ", lines).contains("armorsmiths restock every 30"));
        assertTrue(String.join(" ", lines).contains("25-50%"));
        assertTrue(String.join(" ", lines).contains("random trades"));
        assertTrue(String.join(" ", lines).contains("Council members"));
        assertTrue(String.join(" ", lines).contains("kill bounty"));
        assertTrue(String.join(" ", lines).contains("requires operator"));
    }

    @Test
    void homesHelpListsChecklistAndPoints() {
        var lines = EhmHelp.homesHelpLines();
        assertEquals(lines.size(), EhmHelp.homesHelpKeys().size());
        assertTrue(lines.size() > 7);
        for (String line : lines) {
            assertTrue(line.length() <= EhmHelp.MAX_LINE_CHARS, line);
        }
        String all = String.join(" ", lines);
        assertTrue(all.contains("/tougher help homes"));
        assertTrue(all.contains("24 to 300"));
        assertTrue(all.contains("12 points"));
        assertTrue(all.contains("Each 48 interior air"));
        assertTrue(all.contains("enclosed room"));
        assertTrue(all.contains("Eligible loaded homes"));
        assertTrue(all.contains("timestamped"));
        assertTrue(all.contains("missed day"));
        assertTrue(all.contains("unloaded chunks"));
        assertTrue(all.contains("Most residents restock every 14 Minecraft days"));
        assertTrue(all.contains("armorsmiths restock every 30"));
        assertTrue(all.contains("waiting to restock"));
        assertTrue(all.contains("has not appeared yet"));
        assertTrue(all.contains("wealthy trader"));
        assertTrue(all.contains("master armorsmith"));
        assertTrue(all.contains("25-50%"));
        assertTrue(all.contains("175%"));
        assertTrue(all.contains("random trades"));
        assertTrue(all.contains("council member"));
        assertTrue(all.contains("three times as likely"));
        assertTrue(all.contains("Zombie Bounty 3/12"));
        assertTrue(all.contains("Windows"));
        assertTrue(all.contains("Kitchen"));
        assertTrue(all.contains("clock"));
    }

    @Test
    void abilityHelpListsEveryAbility() {
        var lines = EhmHelp.abilityHelpLines();
        var keys = EhmHelp.abilityHelpKeys();
        assertEquals(lines.size(), keys.size());
        assertEquals(AbilityRules.powerHelpLines().size() + AbilityRules.indexHelpLines().size(), lines.size());
        assertTrue(lines.getFirst().startsWith("Base power:"));
        String index = String.join(" ", lines);
        assertTrue(index.contains("Paper: Healing"));
        assertTrue(index.contains("Golden sword: Smite Evil"));
        assertTrue(index.contains("/tougher ability help <name>"));
        assertTrue(lines.stream().noneMatch(line -> line.startsWith("Healing:")));
        for (String ability : AbilityRules.ABILITY_IDS) {
            var page = EhmHelp.abilityPage(ability);
            assertEquals(page.lines().size(), page.keys().size());
            assertTrue(page.lines().stream().anyMatch(line -> line.contains(":")));
            for (String line : page.lines()) {
                assertTrue(line.length() <= EhmHelp.MAX_LINE_CHARS, line);
            }
        }
        assertTrue(EhmHelp.resolve("healing").lines().stream().anyMatch(line -> line.startsWith("Healing:")));
        assertTrue(EhmHelp.resolve("iron heart").lines().stream().anyMatch(line -> line.startsWith("Iron Heart:")));
        assertTrue(EhmHelp.resolve("fire bolt").lines().stream().anyMatch(line -> line.startsWith("Fire bolt:")));
        assertTrue(EhmHelp.resolve("magic arrow").lines().stream().anyMatch(line -> line.contains("does not ignite")));
        assertTrue(EhmHelp.resolve("flight").lines().stream().anyMatch(line -> line.startsWith("Flight:")));
        assertTrue(EhmHelp.resolve("grow").lines().stream().anyMatch(line -> line.contains("Unbreaking can skip that wear")));
        assertTrue(EhmHelp.resolve("power mine").lines().stream().anyMatch(line -> line.startsWith("Power mining:")));
        assertTrue(EhmHelp.resolve("detect ore").lines().stream().anyMatch(line -> line.startsWith("Detect ore:")));
        assertTrue(EhmHelp.resolve("slow").lines().stream().anyMatch(line -> line.startsWith("Slow:")));
        assertTrue(EhmHelp.resolve("sense evil").lines().stream().anyMatch(line -> line.startsWith("Sense Evil:")));
        assertTrue(EhmHelp.resolve("smite evil").lines().stream().anyMatch(line -> line.startsWith("Smite Evil:")));
        assertTrue(EhmHelp.resolve("light").lines().stream().anyMatch(line -> line.startsWith("Let there be light:")));
        assertTrue(EhmHelp.resolve("ability fire bolt").lines().stream().anyMatch(line -> line.startsWith("Fire bolt:")));
    }

    @Test
    void topicsStaySeparateAndShort() {
        var farming = EhmHelp.resolve("farming");
        String farmText = String.join(" ", farming.lines());
        assertTrue(farmText.contains("overgraze"));
        assertTrue(farmText.contains("1d10"));
        assertTrue(farmText.contains("bone meal"));
        assertFalse(farmText.contains("Stone and deepslate"));
        assertTrue(farming.lines().size() < 40);
        assertEquals(null, EhmHelp.resolve("nope"));
        assertEquals(null, EhmHelp.resolve("  "));
        var mining = EhmHelp.resolve("mining");
        assertFalse(String.join(" ", mining.lines()).contains("overgraze"));
        for (String topic : EhmHelp.topicSuggestions()) {
            var page = EhmHelp.resolve(topic);
            assertTrue(page != null && !page.lines().isEmpty(), topic);
            for (String line : page.lines()) {
                assertTrue(line.length() <= EhmHelp.MAX_LINE_CHARS, topic + ": " + line);
            }
        }
    }

    @Test
    void languageFileDoesNotRestoreLongHelp() throws Exception {
        Map<String, String> lang = langEntries();
        assertFalse(lang.containsKey("tougher.help.feature.5"));
        assertFalse(lang.containsKey("tougher.help.homes.5"));
        assertFalse(lang.containsKey("tougher.ability.heal.help"));
        assertFalse(lang.containsKey("tougher.ability.power.help"));
        var pages = new java.util.ArrayList<EhmHelp.Page>();
        pages.add(EhmHelp.abilityIndex());
        for (String ability : AbilityRules.ABILITY_IDS) {
            pages.add(EhmHelp.abilityPage(ability));
        }
        for (String topic : EhmHelp.topicSuggestions()) {
            pages.add(EhmHelp.resolve(topic));
        }
        for (EhmHelp.Page page : pages) {
            for (int i = 0; i < page.lines().size(); i++) {
                String key = page.keys().get(i);
                String line = page.lines().get(i);
                if (lang.containsKey(key)) {
                    assertEquals(line, lang.get(key), key);
                }
                assertTrue(line.length() <= EhmHelp.MAX_LINE_CHARS, key);
            }
        }
        for (var entry : lang.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("tougher.help.") || key.contains(".help")) {
                assertTrue(entry.getValue().length() <= EhmHelp.MAX_LINE_CHARS, key);
            }
        }
    }

    private static Map<String, String> langEntries() throws Exception {
        try (var in = EhmHelp.class.getResourceAsStream("/assets/tougher/lang/en_us.json")) {
            assertNotNull(in);
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Matcher matcher = Pattern.compile("\"(tougher\\.[^\"]+)\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
                    .matcher(text);
            Map<String, String> entries = new HashMap<>();
            while (matcher.find()) {
                entries.put(matcher.group(1), matcher.group(2));
            }
            return entries;
        }
    }
}
