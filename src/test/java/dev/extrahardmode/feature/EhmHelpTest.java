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
        assertTrue(String.join(" ", lines).contains("One sparkle appears for each point of mana"));
        assertTrue(String.join(" ", lines).contains("saturation"));
        assertTrue(String.join(" ", lines).contains("Well Fed"));
        assertTrue(String.join(" ", lines).contains("at least 1 hunger"));
        assertTrue(String.join(" ", lines).contains("small mark in the top right"));
        assertTrue(String.join(" ", lines).contains("stack on the cursor"));
        assertTrue(String.join(" ", lines).contains("small yellow down mark is a slight craving"));
        assertTrue(String.join(" ", lines).contains("yellow down mark is within grace"));
        assertTrue(String.join(" ", lines).contains("the run weakens"));
        assertTrue(String.join(" ", lines).contains("green minus maintains"));
        assertTrue(String.join(" ", lines).contains("appears only once"));
        assertTrue(String.join(" ", lines).contains("green up mark is a craving"));
        assertTrue(String.join(" ", lines).contains("outside the Well Fed window and the last 7 meals"));
        assertTrue(String.join(" ", lines).contains("175, 275, 425, and 600"));
        assertTrue(String.join(" ", lines).contains("pay 1 through 7 emeralds"));
        assertTrue(String.join(" ", lines).contains("inside a bundle"));
        assertTrue(String.join(" ", lines).contains("or a shop"));
        assertTrue(String.join(" ", lines).contains("saturation does not fall"));
        assertTrue(String.join(" ", lines).contains("Below 18"));
        assertTrue(String.join(" ", lines).contains("does not consume saturation"));
        assertTrue(String.join(" ", lines).contains("casts Battery"));
        assertTrue(String.join(" ", lines).contains("costs no mana"));
        assertTrue(String.join(" ", lines).contains("square root of that level"));
        assertTrue(String.join(" ", lines).contains("stops at your mana level"));
        assertTrue(String.join(" ", lines).contains("spends 1 block, or 2 from a stack"));
        assertTrue(String.join(" ", lines).contains("casts Lapis Shield"));
        assertTrue(String.join(" ", lines).contains("1 minute per level"));
        assertTrue(String.join(" ", lines).contains("before armor"));
        assertTrue(String.join(" ", lines).contains("Every 3 points blocked"));
        assertTrue(String.join(" ", lines).contains("rounded up"));
        assertFalse(String.join(" ", lines).contains("1 quartz per mana"));
        assertTrue(String.join(" ", lines).contains("40 mana"));
        assertTrue(String.join(" ", lines).contains("/tougher ability help"));
        assertTrue(String.join(" ", lines).contains("/tougher me"));
        assertFalse(String.join(" ", lines).contains("diversification"));
        assertTrue(String.join(" ", lines).contains("/tougher achieve"));
        assertTrue(String.join(" ", lines).contains("last block you placed"));
        assertTrue(String.join(" ", lines).contains("A finished single tier is left off"));
        assertFalse(String.join(" ", lines).contains("The clock also lists"));
        assertTrue(String.join(" ", lines).contains("Stone and deepslate"));
        assertFalse(String.join(" ", lines).contains("tuff"));
        assertTrue(String.join(" ", lines).contains("Let it grow"));
        assertFalse(String.join(" ", lines).contains("Let there be light"));
        assertTrue(String.join(" ", lines).contains("An arrow casts Magic arrow"));
        assertTrue(String.join(" ", lines).contains("adds +2 and the burn"));
        assertFalse(String.join(" ", lines).contains("Fire bolt"));
        assertTrue(String.join(" ", lines).contains("Iron ingot: Iron Heart"));
        assertTrue(String.join(" ", lines).contains("Golden sword: Smite Evil"));
        assertTrue(String.join(" ", lines).contains("costs 1/4 mana"));
        assertTrue(String.join(" ", lines).contains("crosshair turns gold"));
        assertTrue(String.join(" ", lines).contains("casts Empower"));
        assertTrue(String.join(" ", lines).contains("does not renew on its own"));
        assertTrue(String.join(" ", lines).contains("square root of that level"));
        assertTrue(String.join(" ", lines).contains("half your mana level"));
        assertTrue(String.join(" ", lines).contains("effective level is at least 1"));
        assertTrue(String.join(" ", lines).contains("wise teacher"));
        assertTrue(String.join(" ", lines).contains("30 emeralds"));
        assertTrue(String.join(" ", lines).contains("down to free"));
        assertTrue(String.join(" ", lines).contains("does not count toward abilities you learn yourself"));
        assertTrue(String.join(" ", lines).contains("each house point and each point of his trading experience"));
        assertTrue(String.join(" ", lines).contains("does not explode"));
        assertTrue(String.join(" ", lines).contains("lapis lazuli block"));
        assertTrue(String.join(" ", lines).contains("Redstone dust"));
        assertTrue(String.join(" ", lines).contains("Pumpkins and melons"));
        assertTrue(String.join(" ", lines).contains("drop no seeds"));
        assertTrue(String.join(" ", lines).contains("6 times as long to breed"));
        assertTrue(String.join(" ", lines).contains("75 minus the current loss"));
        assertTrue(String.join(" ", lines).contains("0 never spreads"));
        assertTrue(String.join(" ", lines).contains("fence, gate, door, wall"));
        assertTrue(String.join(" ", lines).contains("Well Fed or ready gold pushes your level past 40"));
        assertTrue(String.join(" ", lines).contains("overgraze"));
        assertTrue(String.join(" ", lines).contains("9 grass blocks"));
        assertTrue(String.join(" ", lines).contains("chicken or rabbit"));
        assertTrue(String.join(" ", lines).contains("short grass"));
        assertTrue(String.join(" ", lines).contains("drop no meat"));
        assertTrue(String.join(" ", lines).contains("regrow their wool"));
        assertTrue(String.join(" ", lines).contains("lay eggs 6 times"));
        assertTrue(String.join(" ", lines).contains("Trees take 10 times"));
        assertTrue(String.join(" ", lines).contains("Nether wart grows at 1/20"));
        assertTrue(String.join(" ", lines).contains("1 or 2 beans"));
        assertTrue(String.join(" ", lines).contains("diamond 10, gold 20, netherite 30"));
        assertTrue(String.join(" ", lines).contains("plus the tree"));
        assertTrue(String.join(" ", lines).contains("Each other pod on that tree is -2%"));
        assertTrue(String.join(" ", lines).contains("up to +20%"));
        assertTrue(String.join(" ", lines).contains("half as many saplings"));
        assertTrue(String.join(" ", lines).contains("twice the crop loss rate"));
        assertTrue(String.join(" ", lines).contains("At 50% loss or higher, none drop"));
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
        assertTrue(String.join(" ", lines).contains("Every 20 blocks you are deeper adds 1"));
        assertTrue(String.join(" ", lines).contains("5% times that block's light"));
        assertTrue(String.join(" ", lines).contains("fills a hive or nest"));
        assertTrue(String.join(" ", lines).contains("beeHoneyPercent"));
        assertTrue(String.join(" ", lines).contains("square root"));
        assertTrue(String.join(" ", lines).contains("29 blocks"));
        assertTrue(String.join(" ", lines).contains("stored once a day"));
        assertTrue(String.join(" ", lines).contains("0 never fills"));
        assertTrue(String.join(" ", lines).contains("second segment"));
        assertTrue(String.join(" ", lines).contains("Bees do not"));
        assertTrue(String.join(" ", lines).contains("8 times as long to restock"));
        assertTrue(String.join(" ", lines).contains("rounded-up eighth"));
        assertTrue(String.join(" ", lines).contains("cannot exceed that librarian's rank"));
        assertTrue(String.join(" ", lines).contains("positive is good"));
        assertTrue(String.join(" ", lines).contains("13%"));
        assertTrue(String.join(" ", lines).contains("Look at farmland"));
        assertTrue(String.join(" ", lines).contains("Look at a torch"));
        assertTrue(String.join(" ", lines).contains("iron block in the middle"));
        assertTrue(String.join(" ", lines).contains("chiseled stone bricks"));
        assertTrue(String.join(" ", lines).contains("bone meal"));
        assertTrue(String.join(" ", lines).contains("small percent"));
        assertTrue(String.join(" ", lines).contains("red when below 0"));
        assertTrue(String.join(" ", lines).contains("soil under the cane"));
        assertTrue(String.join(" ", lines).contains("starts at 0"));
        assertTrue(String.join(" ", lines).contains("1d10"));
        assertTrue(String.join(" ", lines).contains("water source"));
        assertTrue(String.join(" ", lines).contains("Bucketed water and lava are not sources"));
        assertTrue(String.join(" ", lines).contains("diamond +10"));
        assertTrue(String.join(" ", lines).contains("gold +20"));
        assertTrue(String.join(" ", lines).contains("netherite +30"));
        assertTrue(String.join(" ", lines).contains("Each Efficiency level adds 0.25"));
        assertTrue(String.join(" ", lines).contains("netherite 5"));
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
        assertTrue(String.join(" ", lines).contains("lose 1 light for every 5 blocks below"));
        assertTrue(String.join(" ", lines).contains("remaining time"));
        assertTrue(String.join(" ", lines).contains("Permanent if it never burns out"));
        assertTrue(String.join(" ", lines).contains("Redstone torches are permanent"));
        assertTrue(String.join(" ", lines).contains("dim after 2 days"));
        assertTrue(String.join(" ", lines).contains("pull a log"));
        assertTrue(String.join(" ", lines).contains("12 blocks"));
        assertTrue(String.join(" ", lines).contains("7 more days"));
        assertTrue(String.join(" ", lines).contains("off hand"));
        assertTrue(String.join(" ", lines).contains("even from the edge of a block"));
        assertTrue(String.join(" ", lines).contains("suspends that limit"));
        assertTrue(String.join(" ", lines).contains("falls more than 3 blocks"));
        assertTrue(String.join(" ", lines).contains("drops nothing"));
        assertTrue(String.join(" ", lines).contains("Breaking a block that is on fire"));
        assertTrue(String.join(" ", lines).contains("50 experience"));
        assertTrue(String.join(" ", lines).contains("diamond block"));
        assertFalse(String.join(" ", lines).contains("buys an extra mana level"));
        assertFalse(String.join(" ", lines).contains("1 more each time"));
        assertFalse(String.join(" ", lines).contains("how many lapis"));
        assertFalse(String.join(" ", lines).contains("sparkles you blue"));
        assertTrue(String.join(" ", lines).contains("75 experience"));
        assertTrue(String.join(" ", lines).contains("then 2, then 3 emeralds"));
        assertTrue(String.join(" ", lines).contains("A single tier of 64 pays 2"));
        assertTrue(String.join(" ", lines).contains("enchanted heavy sword or axe"));
        assertTrue(String.join(" ", lines).contains("They trade homes at once"));
        assertTrue(String.join(" ", lines).contains("half the points of the finer home"));
        assertTrue(String.join(" ", lines).contains("Wise-teacher levels add 2 to that roll"));
        assertTrue(String.join(" ", lines).contains("adds 2 to the achievement die"));
        assertTrue(String.join(" ", lines).contains("visit a biome you gain 50 experience"));
        assertTrue(String.join(" ", lines).contains("first player to visit it"));
        assertTrue(String.join(" ", lines).contains("habitable home in a biome"));
        assertTrue(String.join(" ", lines).contains("within 250 blocks"));
        assertTrue(String.join(" ", lines).contains("house points × 3"));
        assertTrue(String.join(" ", lines).contains("on its right at the same scale"));
        assertTrue(String.join(" ", lines).contains("highest first"));
        assertTrue(String.join(" ", lines).contains("Hold a book"));
        assertTrue(String.join(" ", lines).contains("yellow seconds"));
        assertTrue(String.join(" ", lines).contains("golden when you have learned"));
        assertTrue(String.join(" ", lines).contains("plantable vegetable"));
        assertTrue(String.join(" ", lines).contains("6 to 16 emeralds"));
        assertTrue(String.join(" ", lines).contains("Pumpkin and melon seeds are 6"));
        assertTrue(String.join(" ", lines).contains("nether wart is 4"));
        assertTrue(String.join(" ", lines).contains("down to 16"));
        assertTrue(String.join(" ", lines).contains("Glow berries are 3 for 1 emerald"));
        assertTrue(String.join(" ", lines).contains("one of each brewing ingredient"));
        assertTrue(String.join(" ", lines).contains("every 30 Minecraft days"));
        assertTrue(String.join(" ", lines).contains("keeps them until that wait ends"));
        assertTrue(String.join(" ", lines).contains("near here"));
        assertTrue(String.join(" ", lines).contains("triple experience"));
        assertTrue(String.join(" ", lines).contains("5 times the health"));
        assertTrue(String.join(" ", lines).contains("white outline through blocks"));
        assertTrue(String.join(" ", lines).contains("outlined through solid blocks"));
        assertTrue(String.join(" ", lines).contains("twice the first creature's health"));
        assertTrue(String.join(" ", lines).contains("emeralds from 10% to 20% of its health"));
        assertTrue(String.join(" ", lines).contains("10 times the experience"));
        assertTrue(String.join(" ", lines).contains("An unfinished hunt ends after 14 Minecraft days"));
        assertTrue(String.join(" ", lines).contains("each player has his own"));
        assertTrue(String.join(" ", lines).contains("Those buys start with no uses"));
        assertTrue(String.join(" ", lines).contains("adds 1 use to each"));
        assertTrue(String.join(" ", lines).contains("spends 1 use on each"));
        assertTrue(String.join(" ", lines).contains("300 blocks from spawn"));
        assertTrue(String.join(" ", lines).contains("Everyone can earn every achievement"));
        assertTrue(String.join(" ", lines).contains("awards once at 64"));
        assertTrue(String.join(" ", lines).contains("stained glass, and glass panes"));
        assertTrue(String.join(" ", lines).contains("glazed terracotta"));
        assertTrue(String.join(" ", lines).contains("concrete slabs and stairs"));
        assertTrue(String.join(" ", lines).contains("achievements.victory above 0"));
        assertTrue(String.join(" ", lines).contains("the credits scroll"));
        assertTrue(String.join(" ", lines).contains("top left shows your achievements"));
        assertTrue(String.join(" ", lines).contains("chest sits on the back wall"));
        assertTrue(String.join(" ", lines).contains("largest lapis lazuli stack"));
        assertTrue(String.join(" ", lines).contains("same count connect"));
        assertTrue(String.join(" ", lines).contains("close the open door"));
        assertTrue(String.join(" ", lines).contains("100 blocks per item"));
        assertTrue(String.join(" ", lines).contains("Two kinds cover 200"));
        assertTrue(String.join(" ", lines).contains("All three cover 300"));
        assertTrue(String.join(" ", lines).contains("rounded up, and at least 1"));
        assertTrue(String.join(" ", lines).contains("largest stack first"));
        assertTrue(String.join(" ", lines).contains("amethyst shards"));
        assertTrue(String.join(" ", lines).contains("then redstone dust, then amethyst shards"));
        assertTrue(String.join(" ", lines).contains("more than two booths does not link"));
        assertTrue(String.join(" ", lines).contains("then 512, then 1024"));
        assertTrue(String.join(" ", lines).contains("small brown number"));
        assertTrue(String.join(" ", lines).contains("Sneak while holding a placeable block"));
        assertFalse(String.join(" ", lines).contains("your mana abilities"));
        assertTrue(String.join(" ", lines).contains("25 extra experience"));
        assertTrue(String.join(" ", lines).contains("3 experience"));
        assertTrue(String.join(" ", lines).contains("faster hit softer"));
        assertTrue(String.join(" ", lines).contains("1% per 10 blocks"));
        assertTrue(String.join(" ", lines).contains("1000 blocks or farther"));
        assertTrue(String.join(" ", lines).contains("look smaller"));
        assertTrue(String.join(" ", lines).contains("Shields absorb 100%"));
        assertTrue(String.join(" ", lines).contains("one-third as long"));
        assertTrue(String.join(" ", lines).contains("appears once"));
        assertTrue(String.join(" ", lines).contains("does not return"));
        assertTrue(String.join(" ", lines).contains("80 blocks"));
        assertTrue(String.join(" ", lines).contains("health bar with its name"));
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
        assertTrue(all.contains("24 to 400"));
        assertTrue(all.contains("Block light 5"));
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
        assertTrue(all.contains("40 health"));
        assertTrue(all.contains("below 5 health"));
        assertTrue(all.contains("After a mob hits them"));
        assertTrue(all.contains("has not appeared yet"));
        assertTrue(all.contains("wealthy trader"));
        assertTrue(all.contains("a jeweler, an armorsmith"));
        assertTrue(all.contains("twice as many residents"));
        assertTrue(all.contains("past three times that number"));
        assertTrue(all.contains("stay 20 blocks"));
        assertTrue(all.contains("More than 50 blocks"));
        assertTrue(all.contains("More than 100 blocks"));
        assertTrue(all.contains("35 blocks of a villager"));
        assertTrue(all.contains("Alden the Jeweler"));
        assertTrue(all.contains("higher-ranked clothes"));
        assertTrue(all.contains("copper or redstone block"));
        assertTrue(all.contains("A gold block is 6 emeralds and a diamond block is 32"));
        assertTrue(all.contains("SELL or BUY"));
        assertTrue(all.contains("extra stock"));
        assertTrue(all.contains("Your own chest is not offered to you"));
        assertTrue(all.contains("do not set a price"));
        assertTrue(all.contains("bottom-right empty slot"));
        assertTrue(all.contains("enough items and room for the emeralds"));
        assertTrue(all.contains("emeralds he pays with"));
        assertTrue(all.contains("row above it"));
        assertTrue(all.contains("while those emeralds last"));
        assertTrue(all.contains("no experience to you or to the resident"));
        assertTrue(all.contains("every 30 Minecraft days"));
        assertTrue(all.contains("near here"));
        assertTrue(all.contains("triple experience"));
        assertTrue(all.contains("line of sight"));
        assertTrue(all.contains("1 minute times its level"));
        assertTrue(all.contains("5 times the health"));
        assertTrue(all.contains("10 times the experience"));
        assertTrue(all.contains("emeralds from 10% to 20% of that health"));
        assertTrue(all.contains("emeralds from 10% to 20% of its health"));
        assertTrue(all.contains("without the diamond"));
        assertTrue(all.contains("An unfinished hunt ends after 14 Minecraft days"));
        assertTrue(all.contains("each player has his own"));
        assertTrue(all.contains("master armorsmith"));
        assertTrue(all.contains("25-50%"));
        assertTrue(all.contains("175%"));
        assertTrue(all.contains("random trades"));
        assertTrue(all.contains("trading experience"));
        assertTrue(all.contains("square root"));
        assertTrue(all.contains("rolls item amounts once from -25% to +25%"));
        assertTrue(all.contains("subtracts the items required"));
        assertTrue(all.contains("council member"));
        assertTrue(all.contains("three times as likely"));
        assertTrue(all.contains("Zombie Bounty 3/12"));
        assertTrue(all.contains("14 Minecraft days"));
        assertTrue(all.contains("7 Minecraft days after the last one was given"));
        assertTrue(all.contains("hold 5 at a time"));
        assertTrue(all.contains("unpaid emerald turn-ins"));
        assertTrue(all.contains("20% larger"));
        assertTrue(all.contains("Every 3 successes"));
        assertTrue(all.contains("with that resident"));
        assertTrue(all.contains("A kill counts for every open bounty"));
        assertTrue(all.contains("within 48 blocks"));
        assertTrue(all.contains("on or above a solid block"));
        assertTrue(all.contains("dark enough for a monster"));
        assertTrue(all.contains("1 in 250"));
        assertTrue(all.contains("usually brings 1 or 2"));
        assertTrue(all.contains("3 or more, is less common"));
        assertTrue(all.contains("plus 3 per finished bounty"));
        assertTrue(all.contains("rolls on its own"));
        assertTrue(all.contains("stays until the next Minecraft day"));
        assertTrue(all.contains("from 1/6 of those still to kill"));
        assertTrue(all.contains("through 1/3"));
        assertTrue(all.contains("not replaced in that conversation"));
        assertTrue(all.contains("name the resident"));
        assertTrue(all.contains("Emerald prices stay the same"));
        assertTrue(all.contains("only after you finish the previous one"));
        assertTrue(all.contains("compass direction"));
        assertTrue(all.contains("rising sun"));
        assertTrue(all.contains("loot collector"));
        assertTrue(all.contains("4 spider eyes"));
        assertTrue(all.contains("1 ghast tear"));
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
        assertTrue(index.contains("Golden sword or heavy golden sword: Smite Evil"));
        assertTrue(index.contains("/tougher ability help <name>"));
        assertTrue(index.contains("house points plus his trading experience"));
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
        assertTrue(EhmHelp.resolve("fire bolt").lines().stream().anyMatch(line -> line.contains("arrows burn")));
        assertTrue(EhmHelp.resolve("magic arrow").lines().stream().anyMatch(line -> line.contains("adds +2")));
        assertTrue(EhmHelp.resolve("flight").lines().stream().anyMatch(line -> line.startsWith("Flight:")));
        assertTrue(String.join(" ", EhmHelp.resolve("flight").lines()).contains("phantom membrane"));
        assertTrue(String.join(" ", EhmHelp.resolve("flight").lines()).contains("not a bundle"));
        assertTrue(String.join(" ", EhmHelp.resolve("abilities").lines()).contains("adds +3 to Flight"));
        assertTrue(EhmHelp.resolve("grow").lines().stream().anyMatch(line -> line.contains("Unbreaking can skip that wear")));
        assertTrue(EhmHelp.resolve("power mine").lines().stream().anyMatch(line -> line.startsWith("Power mining:")));
        assertTrue(EhmHelp.resolve("detect ore").lines().stream().anyMatch(line -> line.startsWith("Detect ore:")));
        assertTrue(EhmHelp.resolve("slow").lines().stream().anyMatch(line -> line.startsWith("Slow:")));
        assertTrue(EhmHelp.resolve("sense evil").lines().stream().anyMatch(line -> line.startsWith("Sense Evil:")));
        assertTrue(String.join(" ", EhmHelp.resolve("sense evil").lines()).contains("Hold the click to eat it instead"));
        assertTrue(String.join(" ", EhmHelp.resolve("abilities").lines()).contains("Hold the click to eat it"));
        assertTrue(EhmHelp.resolve("smite evil").lines().stream().anyMatch(line -> line.startsWith("Smite Evil:")));
        assertEquals(null, EhmHelp.resolve("light"));
        assertEquals(null, EhmHelp.resolve("let there be light"));
        assertTrue(EhmHelp.resolve("master builder").lines().stream().anyMatch(line -> line.startsWith("Master Builder:")));
        assertTrue(EhmHelp.resolve("diamond skin").lines().stream().anyMatch(line -> line.startsWith("Diamond Skin:")));
        assertTrue(EhmHelp.resolve("empower").lines().stream().anyMatch(line -> line.startsWith("Empower:")));
        assertTrue(EhmHelp.resolve("battery").lines().stream().anyMatch(line -> line.startsWith("Battery:")));
        assertTrue(EhmHelp.resolve("lapis shield").lines().stream().anyMatch(line -> line.startsWith("Lapis Shield:")));
        assertTrue(EhmHelp.resolve("write tome").lines().stream().anyMatch(line -> line.startsWith("Write Tome of the Wise:")));
        assertTrue(EhmHelp.resolve("read tome").lines().stream().anyMatch(line -> line.startsWith("Read Tome of the Wise:")));
        assertTrue(String.join(" ", EhmHelp.resolve("abilities").lines()).contains("Tome of the Wise"));
        assertTrue(EhmHelp.resolve("lapus").lines().stream().anyMatch(line -> line.startsWith("Lapis Shield:")));
        assertTrue(EhmHelp.resolve("ability fire bolt").lines().stream().anyMatch(line -> line.contains("arrows burn")));
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
        assertTrue(String.join(" ", EhmHelp.resolve("mana").lines()).contains("the way they will look"));
        String combatText = String.join(" ", EhmHelp.resolve("combat").lines());
        assertTrue(combatText.contains("teleports to the nearest open spot"));
        assertTrue(combatText.contains("phantom that ignores sunlight"));
        assertTrue(combatText.contains("chunkClearKills"));
        assertTrue(combatText.contains("being cleared for the alliance"));
        assertTrue(combatText.contains("25 experience and an emerald"));
        assertTrue(combatText.contains("quiet chime"));
        assertTrue(combatText.contains("each full chunk"));
        assertTrue(combatText.contains("of -1 is off"));
        assertEquals(null, EhmHelp.resolve("nope"));
        assertEquals(null, EhmHelp.resolve("  "));
        var mining = EhmHelp.resolve("mining");
        assertFalse(String.join(" ", mining.lines()).contains("overgraze"));
        assertTrue(String.join(" ", mining.lines()).contains("out to 50 blocks"));
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
