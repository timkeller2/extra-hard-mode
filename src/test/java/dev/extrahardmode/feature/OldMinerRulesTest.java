package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OldMinerRulesTest {
    @Test
    void offerCountRoundsUpAndFollowsDetectOreOrder() {
        assertEquals(0, OldMinerRules.offerCount(0, 0));
        assertEquals(0, OldMinerRules.offerCount(-4, 0));
        assertEquals(1, OldMinerRules.offerCount(1, 0));
        assertEquals(1, OldMinerRules.offerCount(5, 0));
        assertEquals(3, OldMinerRules.offerCount(12, 0));
        assertEquals(3, OldMinerRules.offerCount(12, 3));
        assertEquals(4, OldMinerRules.offerCount(12, 4));
        assertEquals(5, OldMinerRules.offerCount(24, 0));
        assertEquals(10, OldMinerRules.offerCount(100, 100));
        assertEquals(3, OldMinerRules.offerCount(12, -2));
        assertEquals(
                List.of("coal", "quartz", "copper"),
                OldMinerRules.offerPool(12, 0, OldMinerRules.ORES));
        assertEquals(10, OldMinerRules.ORES.size());
        int previous = 0;
        for (String family : OldMinerRules.ORES) {
            String block = switch (family) {
                case "ancient_debris" -> "minecraft:ancient_debris";
                case "quartz" -> "minecraft:nether_quartz_ore";
                default -> "minecraft:" + family + "_ore";
            };
            int level = AbilityRules.oreDetectLevel(block);
            assertTrue(level > previous, family);
            previous = level;
            assertEquals(family, AbilityRules.oreFamily("minecraft:deepslate_" + family + "_ore"));
        }
        assertEquals("gold", AbilityRules.oreFamily("minecraft:nether_gold_ore"));
        assertEquals("quartz", AbilityRules.oreFamily("minecraft:nether_quartz_ore"));
    }

    @Test
    void cycleSkipsFinishedOresUntilEveryOreInTheGroundIsNamed() {
        List<String> ground = OldMinerRules.ORES;
        OldMinerRules.Offer first = OldMinerRules.pickOffer(12, 0, ground, List.of(), Set.of(), 0);
        assertEquals("coal", first.ore());
        assertEquals(3, first.poolSize());
        assertEquals(List.of("coal", "quartz", "copper"), OldMinerRules.missionOres(12, 0, ground, List.of()));
        OldMinerRules.Offer second = OldMinerRules.pickOffer(12, 0, ground, List.of("coal"), Set.of(), 0);
        assertEquals("quartz", second.ore());
        assertEquals(List.of("quartz", "copper", "iron"), OldMinerRules.missionOres(12, 0, ground, List.of("coal")));
        OldMinerRules.Offer third = OldMinerRules.pickOffer(12, 0, ground, List.of("coal", "quartz"), Set.of(), 1);
        assertEquals("iron", third.ore());
        OldMinerRules.Offer stillOpen = OldMinerRules.pickOffer(
                12, 0, ground, List.of("coal", "quartz", "copper"), Set.of(), 0);
        assertEquals("iron", stillOpen.ore());
        assertEquals(List.of("coal", "quartz", "copper"), stillOpen.finished());
        OldMinerRules.Offer again = OldMinerRules.pickOffer(12, 0, ground, List.of("quartz"), Set.of(), 0);
        assertEquals("coal", again.ore());
        OldMinerRules.Offer grown = OldMinerRules.pickOffer(14, 2, ground, List.of("coal"), Set.of("coal"), 0);
        assertEquals("quartz", grown.ore());
        assertEquals(4, grown.poolSize());
        OldMinerRules.Offer reset = OldMinerRules.pickOffer(12, 0, ground, ground, Set.of(), 0);
        assertEquals("coal", reset.ore());
        assertTrue(reset.finished().isEmpty());
        assertEquals(3, reset.poolSize());
    }

    @Test
    void onlyOresInTheGroundAreNamedAndFinishingAnEasierOneAdmitsTheNext() {
        List<String> sparse = List.of("diamond", "coal", "iron", "copper");
        assertEquals(List.of("coal", "copper", "iron", "diamond"), OldMinerRules.oresPresent(sparse));
        assertEquals(List.of("coal", "copper", "iron"), OldMinerRules.offerPool(12, 0, sparse));
        assertEquals(
                List.of("copper", "iron", "diamond"),
                OldMinerRules.missionOres(12, 0, sparse, List.of("coal")));
        assertEquals("copper", OldMinerRules.pickOffer(12, 1, sparse, List.of("coal"), Set.of(), 0).ore());
        assertEquals("iron", OldMinerRules.pickOffer(12, 1, sparse, List.of("coal"), Set.of(), 1).ore());
        OldMinerRules.Offer diamond = OldMinerRules.pickOffer(12, 1, sparse, List.of("coal"), Set.of(), 2);
        assertEquals("diamond", diamond.ore());
        assertEquals(3, diamond.poolSize());
        assertEquals(List.of("coal"), diamond.finished());
        OldMinerRules.Offer last = OldMinerRules.pickOffer(
                12, 3, sparse, List.of("coal", "copper", "iron"), Set.of(), 0);
        assertEquals("diamond", last.ore());
        assertEquals(1, last.poolSize());
        OldMinerRules.Offer reset = OldMinerRules.pickOffer(
                12, 0, sparse, List.of("coal", "copper", "iron", "diamond"), Set.of(), 0);
        assertEquals("coal", reset.ore());
        assertEquals(3, reset.poolSize());
        assertTrue(reset.finished().isEmpty());
        for (int roll = 0; roll < 8; roll++) {
            String ore = OldMinerRules.pickOffer(12, 0, sparse, List.of(), Set.of(), roll).ore();
            assertFalse(ore.equals("quartz") || ore.equals("ancient_debris"));
        }
        OldMinerRules.Offer alone = OldMinerRules.pickOffer(12, 0, List.of("diamond"), List.of(), Set.of(), 0);
        assertEquals("diamond", alone.ore());
        assertEquals(1, alone.poolSize());
    }

    @Test
    void aVeinMissingFromThisDrawIsSkippedWithoutCountingItFinished() {
        List<String> three = List.of("coal", "quartz", "copper");
        OldMinerRules.Offer missed = OldMinerRules.pickOffer(12, 1, three, List.of("coal"), Set.of("coal"), 0);
        assertEquals("quartz", missed.ore());
        assertEquals(List.of("coal"), missed.finished());
        assertNull(OldMinerRules.pickOffer(1, 0, List.of("coal"), List.of(), Set.of("coal"), 0));
        assertNull(OldMinerRules.pickOffer(12, 0, List.of(), List.of(), Set.of(), 0));
        OldMinerRules.Offer afterReset = OldMinerRules.pickOffer(
                12, 0, three, List.of("coal", "quartz", "copper"), Set.of("coal"), 0);
        assertEquals("quartz", afterReset.ore());
        assertTrue(afterReset.finished().isEmpty());
    }

    @Test
    void expiryWithoutAVisitLeavesTheOreAvailable() {
        OldMinerRules.Lead active = OldMinerRules.Lead.blank("miner", "Jack")
                .assigned("iron", 4, 5, 6, "minecraft:overworld", 10);
        assertEquals(17L, active.expireDay());
        assertEquals(17L, active.nextDay());
        OldMinerRules.Lead missed = active.expired();
        assertFalse(missed.active());
        assertEquals(0, missed.successes());
        assertEquals("", missed.finished());
        assertEquals(17L, missed.nextDay());
        assertEquals(
                "iron",
                OldMinerRules.pickOffer(20, missed.successes(), OldMinerRules.ORES, missed.finishedOres(), Set.of(), 3)
                        .ore());
        OldMinerRules.Lead done = active.completed();
        assertEquals(1, done.successes());
        assertEquals("iron", done.finished());
        assertFalse(done.active());
        assertEquals(17L, done.nextDay());
        assertEquals(
                "coal",
                OldMinerRules.pickOffer(12, done.successes(), OldMinerRules.ORES, done.finishedOres(), Set.of(), 0)
                        .ore());
        assertEquals(3L, OldMinerRules.daysUntil(14, done.nextDay()));
    }

    @Test
    void directionIsTheFurthestAxisAndTheLineFollowsThePlayer() {
        assertEquals("East", OldMinerRules.direction(5, 1, 1));
        assertEquals("West", OldMinerRules.direction(-5, 1, 1));
        assertEquals("South", OldMinerRules.direction(1, 1, 5));
        assertEquals("North", OldMinerRules.direction(1, 1, -5));
        assertEquals("Up", OldMinerRules.direction(1, 5, 1));
        assertEquals("Down", OldMinerRules.direction(1, -5, 1));
        assertEquals("East", OldMinerRules.direction(3, 3, 3));
        assertEquals("East", OldMinerRules.direction(3, 0, 3));
        assertEquals("South", OldMinerRules.direction(0, 3, 3));
        assertEquals("Here", OldMinerRules.direction(0, 0, 0));
        assertEquals("Iron", OldMinerRules.oreTitle("iron"));
        assertEquals("Ancient Debris", OldMinerRules.oreTitle("ancient_debris"));
        assertEquals("Lapis Lazuli", OldMinerRules.oreTitle("lapis"));
        assertEquals("Jack: Iron East", OldMinerRules.leadLine("Jack", "iron", "East"));
        OldMinerRules.Lead lead = OldMinerRules.Lead.blank("b", "Jack")
                .assigned("iron", 10, 0, 0, "minecraft:overworld", 0);
        assertEquals(
                List.of("Jack: Iron East"),
                OldMinerRules.hudLines(List.of(lead), "minecraft:overworld", 0, 0, 0, 1));
        assertEquals(
                List.of("Jack: Iron West"),
                OldMinerRules.hudLines(List.of(lead), "minecraft:overworld", 20, 0, 0, 1));
        assertEquals(
                List.of("Jack: Iron Up"),
                OldMinerRules.hudLines(List.of(lead), "minecraft:overworld", 10, -8, 0, 1));
        assertTrue(OldMinerRules.hudLines(List.of(lead), "minecraft:the_nether", 0, 0, 0, 1).isEmpty());
        assertTrue(OldMinerRules.hudLines(List.of(lead), "minecraft:overworld", 10, 0, 0, 1).isEmpty());
        assertTrue(OldMinerRules.hudLines(List.of(lead), "minecraft:overworld", 0, 0, 0, 7).isEmpty());
    }

    @Test
    void arrivalIsWithinThreeBlocks() {
        assertTrue(OldMinerRules.arrived(0, 0, 0, 3, 0, 0));
        assertTrue(OldMinerRules.arrived(0, 0, 0, 2, 2, 1));
        assertFalse(OldMinerRules.arrived(0, 0, 0, 2, 2, 2));
        assertFalse(OldMinerRules.arrived(0, 0, 0, 0, 0, 4));
        assertTrue(OldMinerRules.arrived(5, 5, 5, 5, 5, 5));
    }

    @Test
    void largestVeinPrefersSizeThenTheBed() {
        List<OldMinerRules.Spot> spots = List.of(
                new OldMinerRules.Spot(0, 0, 0),
                new OldMinerRules.Spot(1, 0, 0),
                new OldMinerRules.Spot(2, 0, 0),
                new OldMinerRules.Spot(0, 5, 0),
                new OldMinerRules.Spot(0, 6, 0),
                new OldMinerRules.Spot(8, 8, 0));
        List<OldMinerRules.Spot> vein = OldMinerRules.largestVein(spots, 10, 0, 0);
        assertEquals(3, vein.size());
        assertEquals(new OldMinerRules.Spot(2, 0, 0), OldMinerRules.anchor(vein, 10, 0, 0));
        List<OldMinerRules.Spot> tied = OldMinerRules.largestVein(
                List.of(new OldMinerRules.Spot(0, 0, 0), new OldMinerRules.Spot(2, 0, 0)), 1, 0, 0);
        assertEquals(List.of(new OldMinerRules.Spot(0, 0, 0)), tied);
        List<OldMinerRules.Spot> diagonal = OldMinerRules.largestVein(
                List.of(new OldMinerRules.Spot(0, 0, 0), new OldMinerRules.Spot(1, 1, 0)), 2, 0, 0);
        assertEquals(1, diagonal.size());
        assertEquals(new OldMinerRules.Spot(1, 1, 0), diagonal.getFirst());
    }

    @Test
    void growthConvertsOnlyStoneAroundTheFamily() {
        assertEquals("minecraft:iron_ore", OldMinerRules.oreBlockFor("iron", "minecraft:stone"));
        assertEquals("minecraft:iron_ore", OldMinerRules.oreBlockFor("iron", "granite"));
        assertEquals("minecraft:iron_ore", OldMinerRules.oreBlockFor("iron", "minecraft:diorite"));
        assertEquals("minecraft:iron_ore", OldMinerRules.oreBlockFor("iron", "andesite"));
        assertEquals("minecraft:deepslate_iron_ore", OldMinerRules.oreBlockFor("iron", "minecraft:deepslate"));
        assertEquals("minecraft:deepslate_iron_ore", OldMinerRules.oreBlockFor("iron", "minecraft:tuff"));
        assertEquals("", OldMinerRules.oreBlockFor("iron", "minecraft:netherrack"));
        assertEquals("", OldMinerRules.oreBlockFor("iron", "minecraft:air"));
        assertEquals("", OldMinerRules.oreBlockFor("iron", "minecraft:chest"));
        assertEquals("", OldMinerRules.oreBlockFor("quartz", "minecraft:stone"));
        assertEquals("minecraft:nether_quartz_ore", OldMinerRules.oreBlockFor("quartz", "minecraft:netherrack"));
        assertEquals("minecraft:nether_quartz_ore", OldMinerRules.oreBlockFor("quartz", "minecraft:blackstone"));
        assertEquals("minecraft:nether_quartz_ore", OldMinerRules.oreBlockFor("quartz", "minecraft:basalt"));
        assertEquals("minecraft:gold_ore", OldMinerRules.oreBlockFor("gold", "minecraft:stone"));
        assertEquals("minecraft:deepslate_gold_ore", OldMinerRules.oreBlockFor("gold", "minecraft:deepslate"));
        assertEquals("minecraft:nether_gold_ore", OldMinerRules.oreBlockFor("gold", "minecraft:netherrack"));
        assertEquals("", OldMinerRules.oreBlockFor("ancient_debris", "minecraft:stone"));
        assertEquals("minecraft:ancient_debris", OldMinerRules.oreBlockFor("ancient_debris", "minecraft:netherrack"));
        assertEquals("minecraft:deepslate_diamond_ore", OldMinerRules.oreBlockFor("diamond", "tuff"));
        assertEquals("minecraft:lapis_ore", OldMinerRules.oreBlockFor("lapis", "minecraft:stone"));
        assertEquals("", OldMinerRules.oreBlockFor("quartz", "minecraft:bedrock"));
    }
}
