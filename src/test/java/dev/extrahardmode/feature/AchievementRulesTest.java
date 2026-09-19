package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AchievementRulesTest {
    @Test
    void builderTiersUnlockOnce() {
        assertEquals(0, AchievementRules.awardedAfter(0, 255, AchievementRules.BUILDER_THRESHOLDS));
        assertEquals(1, AchievementRules.awardedAfter(0, 256, AchievementRules.BUILDER_THRESHOLDS));
        assertEquals(2, AchievementRules.awardedAfter(0, 512, AchievementRules.BUILDER_THRESHOLDS));
        assertEquals(3, AchievementRules.awardedAfter(0, 1024, AchievementRules.BUILDER_THRESHOLDS));
        assertEquals(1, AchievementRules.awardedAfter(1, 256, AchievementRules.BUILDER_THRESHOLDS));
        assertEquals(1, AchievementRules.awardedAfter(1, 100, AchievementRules.BUILDER_THRESHOLDS));
        assertEquals(2, AchievementRules.awardedAfter(1, 512, AchievementRules.BUILDER_THRESHOLDS));
    }

    @Test
    void slayerTiers() {
        assertEquals(0, AchievementRules.awardedAfter(0, 24, AchievementRules.SLAYER_THRESHOLDS));
        assertEquals(1, AchievementRules.awardedAfter(0, 25, AchievementRules.SLAYER_THRESHOLDS));
        assertEquals(2, AchievementRules.awardedAfter(0, 100, AchievementRules.SLAYER_THRESHOLDS));
        assertEquals(3, AchievementRules.awardedAfter(0, 250, AchievementRules.SLAYER_THRESHOLDS));
        assertEquals(1, AchievementRules.awardedAfter(1, 25, AchievementRules.SLAYER_THRESHOLDS));
    }

    @Test
    void breakDecrementSeventyFivePercentNotBelowZero() {
        assertEquals(0, AchievementRules.maybeDecrement(0, 0, 75));
        assertEquals(4, AchievementRules.maybeDecrement(5, 0, 75));
        assertEquals(5, AchievementRules.maybeDecrement(5, 75, 75));
        assertEquals(4, AchievementRules.maybeDecrement(5, 74, 75));
    }

    @Test
    void manaRegenPerLevelPerMinute() {
        assertEquals(0, AchievementRules.regenUnits(0));
        assertEquals(1, AchievementRules.regenUnits(1));
        assertEquals(1, AchievementRules.regenUnits(2));
        assertEquals(1, AchievementRules.regenUnits(3));
        assertEquals(1, AchievementRules.regenUnits(4));
        assertEquals(2, AchievementRules.regenUnits(5));
        assertEquals(5, AchievementRules.regenUnits(20));
        assertEquals(0.0, AchievementRules.regenPerMinute(0, false), 1e-9);
        assertEquals(0.0, AchievementRules.regenPerMinute(0, true), 1e-9);
        assertEquals(0.05, AchievementRules.regenPerMinute(1, false), 1e-9);
        assertEquals(0.15, AchievementRules.regenPerMinute(1, true), 1e-9);
        assertEquals(0.05, AchievementRules.regenPerMinute(2, false), 1e-9);
        assertEquals(0.05, AchievementRules.regenPerMinute(3, false), 1e-9);
        assertEquals(0.25, AchievementRules.regenPerMinute(20, false), 1e-9);
        assertEquals(0.75, AchievementRules.regenPerMinute(20, true), 1e-9);
        assertEquals(0.5, AchievementRules.addMana(0.0, 2, 0.5), 1e-9);
        assertEquals(1.025, AchievementRules.addMana(0.95, 1, 0.15), 1e-9);
        assertEquals(1.0125, AchievementRules.addMana(1.0, 1, 0.05), 1e-9);
        assertEquals(2.1, AchievementRules.addMana(1.9, 2, 0.5), 1e-9);
        assertEquals(20.0, AchievementRules.addMana(19.9, 2, 4.0), 1e-9);
        assertEquals(20.0, AchievementRules.addMana(20.0, 8, 1.0), 1e-9);
        assertEquals(20.0, AchievementRules.clampMana(25.0), 1e-9);
        assertTrue(AchievementRules.shouldConsumeQuartz(2, 1.0));
        assertFalse(AchievementRules.shouldConsumeQuartz(2, 2.0));
        assertFalse(AchievementRules.shouldConsumeQuartz(0, 0.0));
    }

    @Test
    void achievementRewardUsesExperienceLevelChanceAndBlockPayment() {
        assertEquals(50, AchievementRules.REWARD_EXPERIENCE_POINTS);
        assertEquals(0, AchievementRules.manaChancePercent(0));
        assertEquals(15, AchievementRules.manaChancePercent(15));
        assertEquals(100, AchievementRules.manaChancePercent(100));
        assertEquals(100, AchievementRules.manaChancePercent(250));
        assertFalse(AchievementRules.manaFromExperience(0, 0));
        assertTrue(AchievementRules.manaFromExperience(1, 0));
        assertFalse(AchievementRules.manaFromExperience(1, 1));
        assertTrue(AchievementRules.manaFromExperience(15, 14));
        assertFalse(AchievementRules.manaFromExperience(15, 15));
        assertTrue(AchievementRules.manaFromExperience(100, 99));
        assertTrue(AchievementRules.canPayManaBlocks(1, 1));
        assertTrue(AchievementRules.canPayManaBlocks(8, 3));
        assertFalse(AchievementRules.canPayManaBlocks(1, 0));
        assertFalse(AchievementRules.canPayManaBlocks(0, 1));
        assertFalse(AchievementRules.canPayManaBlocks(0, 0));
        assertEquals(1, AchievementRules.lapisCost(0));
        assertEquals(2, AchievementRules.lapisCost(1));
        assertEquals(5, AchievementRules.lapisCost(4));
        assertEquals(1, AchievementRules.lapisCost(-3));
        assertEquals(1, AchievementRules.MANA_DIAMOND_COST);
        assertTrue(AchievementRules.canPayManaBlocks(1, 2, 2));
        assertFalse(AchievementRules.canPayManaBlocks(1, 1, 2));
        assertTrue(AchievementRules.shouldHintLapis(1, 0, 1));
        assertTrue(AchievementRules.shouldHintLapis(1, 1, 2));
        assertFalse(AchievementRules.shouldHintLapis(1, 2, 2));
        assertFalse(AchievementRules.shouldHintLapis(0, 0, 1));
        assertFalse(AchievementRules.shouldHintLapis(0, 5, 2));
    }

    @Test
    void nextTargetIsTheUpcomingThreshold() {
        assertEquals(256, AchievementRules.nextTarget(0, AchievementRules.BUILDER_THRESHOLDS));
        assertEquals(256, AchievementRules.nextTarget(1, AchievementRules.BUILDER_THRESHOLDS));
        assertEquals(256, AchievementRules.nextTarget(255, AchievementRules.BUILDER_THRESHOLDS));
        assertEquals(256, AchievementRules.nextTarget(256, AchievementRules.BUILDER_THRESHOLDS));
        assertEquals(25, AchievementRules.nextTarget(1, AchievementRules.SLAYER_THRESHOLDS));
        assertEquals(512, AchievementRules.nextTarget(250, AchievementRules.BUILDER_THRESHOLDS, 1));
        assertEquals(256, AchievementRules.nextTarget(250, AchievementRules.BUILDER_THRESHOLDS, 0));
        assertEquals(1024, AchievementRules.nextTarget(200, AchievementRules.BUILDER_THRESHOLDS, 2));
        assertEquals(0, AchievementRules.nextTarget(200, AchievementRules.BUILDER_THRESHOLDS, 3));
        assertEquals(100, AchievementRules.nextTarget(10, AchievementRules.SLAYER_THRESHOLDS, 1));
    }

    @Test
    void closestListsInProgressByRemaining() {
        Map<String, Integer> builders = new LinkedHashMap<>();
        builders.put("minecraft:oak_planks", 200);
        builders.put("minecraft:dirt", 0);
        builders.put("minecraft:stone", 1024);
        Map<String, Integer> slayers = Map.of("minecraft:zombie", 23, "minecraft:creeper", 99);
        List<AchievementRules.Progress> closest = AchievementRules.closest(
                builders,
                slayers,
                Map.of("minecraft:stone", 3),
                Map.of("minecraft:creeper", 1),
                AchievementRules.CLOSEST_LIST_LIMIT);
        assertEquals(3, closest.size());
        assertEquals("minecraft:creeper", closest.get(0).id());
        assertEquals(99, closest.get(0).count());
        assertEquals(100, closest.get(0).nextTarget());
        assertEquals("minecraft:zombie", closest.get(1).id());
        assertEquals(23, closest.get(1).count());
        assertEquals(25, closest.get(1).nextTarget());
        assertEquals("minecraft:oak_planks", closest.get(2).id());
        assertEquals(256, closest.get(2).nextTarget());
        assertTrue(AchievementRules.closest(Map.of(), Map.of(), 15).isEmpty());
        assertEquals("oak planks", AchievementRules.prettyId("minecraft:oak_planks"));
    }

    @Test
    void closestHonorsLimit() {
        Map<String, Integer> builders = new LinkedHashMap<>();
        for (int i = 0; i < 20; i++) {
            builders.put("minecraft:block_" + i, 250);
        }
        List<AchievementRules.Progress> closest =
                AchievementRules.closest(builders, Map.of(), AchievementRules.CLOSEST_LIST_LIMIT);
        assertEquals(5, closest.size());
        assertEquals(6, closest.get(0).remaining());
        assertEquals(5, AchievementRules.BUILDER_LIST_LIMIT);
    }

    @Test
    void closestMixesAtMostThreeSlayerTracks() {
        Map<String, Integer> builders = new LinkedHashMap<>();
        for (int i = 0; i < 12; i++) {
            builders.put("minecraft:block_" + i, 200);
        }
        Map<String, Integer> slayers = new LinkedHashMap<>();
        for (int i = 0; i < 10; i++) {
            slayers.put("minecraft:mob_" + i, 11);
        }
        List<AchievementRules.Progress> closest =
                AchievementRules.closest(builders, slayers, AchievementRules.CLOSEST_LIST_LIMIT);
        assertEquals(8, closest.size());
        long slayerCount = closest.stream().filter(progress -> !progress.builder()).count();
        long builderCount = closest.stream().filter(AchievementRules.Progress::builder).count();
        assertEquals(3, slayerCount);
        assertEquals(5, builderCount);
        assertEquals(3, AchievementRules.closest(Map.of(), slayers, 15).size());
    }

    @Test
    void closestWatchShowsNearestSlayerThenThreeBuilders() {
        Map<String, Integer> builders = new LinkedHashMap<>();
        builders.put("minecraft:oak_planks", 250);
        builders.put("minecraft:dirt", 200);
        builders.put("minecraft:stone", 100);
        builders.put("minecraft:cobblestone", 240);
        builders.put("minecraft:gravel", 90);
        builders.put("minecraft:sand", 10);
        Map<String, Integer> slayers = new LinkedHashMap<>();
        slayers.put("minecraft:zombie", 24);
        slayers.put("minecraft:creeper", 10);
        List<AchievementRules.Progress> watch = AchievementRules.closestWatch(builders, slayers);
        assertEquals(6, watch.size());
        assertFalse(watch.get(0).builder());
        assertEquals("minecraft:zombie", watch.get(0).id());
        assertEquals(25, watch.get(0).nextTarget());
        assertEquals("minecraft:oak_planks", watch.get(1).id());
        assertEquals("minecraft:cobblestone", watch.get(2).id());
        assertEquals("minecraft:dirt", watch.get(3).id());
        assertEquals("minecraft:stone", watch.get(4).id());
        assertEquals("minecraft:gravel", watch.get(5).id());
        assertEquals(1, AchievementRules.WATCH_SLAYER_LIMIT);
        assertEquals(5, AchievementRules.WATCH_BUILDER_LIMIT);
        assertEquals(5, AchievementRules.BUILDER_LIST_LIMIT);
        assertTrue(AchievementRules.closestWatch(Map.of(), Map.of()).isEmpty());
        List<AchievementRules.Progress> buildersOnly = AchievementRules.closestWatch(builders, Map.of());
        assertEquals(5, buildersOnly.size());
        assertTrue(buildersOnly.stream().allMatch(AchievementRules.Progress::builder));
        List<AchievementRules.Progress> slayerOnly = AchievementRules.closestWatch(Map.of(), slayers);
        assertEquals(1, slayerOnly.size());
        assertEquals("minecraft:zombie", slayerOnly.get(0).id());
    }

    @Test
    void closestWatchKeepsSlayerFirstEvenWhenABuilderIsCloser() {
        Map<String, Integer> builders = Map.of("minecraft:oak_planks", 255);
        Map<String, Integer> slayers = Map.of("minecraft:zombie", 1);
        List<AchievementRules.Progress> watch = AchievementRules.closestWatch(builders, slayers);
        assertEquals(2, watch.size());
        assertEquals("minecraft:zombie", watch.get(0).id());
        assertEquals(24, watch.get(0).remaining());
        assertEquals("minecraft:oak_planks", watch.get(1).id());
        assertEquals(1, watch.get(1).remaining());
        List<AchievementRules.Progress> awarded = AchievementRules.closestWatch(
                builders, slayers, Map.of(), Map.of("minecraft:zombie", 3));
        assertEquals(1, awarded.size());
        assertTrue(awarded.get(0).builder());
    }

    @Test
    void closestUsesUnawardedTierAfterCountDrops() {
        Map<String, Integer> builders = Map.of("minecraft:oak_planks", 250);
        Map<String, Integer> awarded = Map.of("minecraft:oak_planks", 1);
        List<AchievementRules.Progress> dropped =
                AchievementRules.closest(builders, Map.of(), awarded, Map.of(), 15);
        assertEquals(1, dropped.size());
        assertEquals(512, dropped.get(0).nextTarget());
        assertEquals(262, dropped.get(0).remaining());
        List<AchievementRules.Progress> unearned =
                AchievementRules.closest(builders, Map.of(), Map.of(), Map.of(), 15);
        assertEquals(256, unearned.get(0).nextTarget());
        assertEquals(6, unearned.get(0).remaining());
        assertTrue(AchievementRules.closest(builders, Map.of(), Map.of("minecraft:oak_planks", 3), Map.of(), 15)
                .isEmpty());
    }

    @Test
    void lastPlacedReportsCurrentOrFinalThreshold() {
        AchievementRules.Progress early = AchievementRules.lastPlaced("minecraft:oak_planks", 200, 0);
        assertEquals("minecraft:oak_planks", early.id());
        assertTrue(early.builder());
        assertEquals(200, early.count());
        assertEquals(256, early.nextTarget());
        AchievementRules.Progress mid = AchievementRules.lastPlaced("minecraft:oak_planks", 300, 1);
        assertEquals(512, mid.nextTarget());
        AchievementRules.Progress done = AchievementRules.lastPlaced("minecraft:oak_planks", 1024, 3);
        assertEquals(1024, done.count());
        assertEquals(1024, done.nextTarget());
        assertEquals(0, done.remaining());
        assertEquals(null, AchievementRules.lastPlaced("", 10, 0));
        assertEquals(null, AchievementRules.lastPlaced(null, 10, 0));
    }

    @Test
    void competitiveClaimsSkipUpcomingTiers() {
        assertEquals("b:minecraft:oak_planks:0", AchievementRules.claimKey(true, "minecraft:oak_planks", 0));
        assertEquals("s:minecraft:zombie:1", AchievementRules.claimKey(false, "minecraft:zombie", 1));
        var claimed = Set.of(AchievementRules.claimKey(true, "minecraft:oak_planks", 0));
        assertEquals(
                512,
                AchievementRules.nextTarget(
                        200, AchievementRules.BUILDER_THRESHOLDS, 0, claimed, true, "minecraft:oak_planks"));
        assertEquals(256, AchievementRules.nextTarget(200, AchievementRules.BUILDER_THRESHOLDS, 0));
        Map<String, Integer> builders = Map.of("minecraft:oak_planks", 200, "minecraft:dirt", 100);
        List<AchievementRules.Progress> closest = AchievementRules.closest(
                builders, Map.of(), Map.of(), Map.of(), 15, claimed);
        assertEquals(2, closest.size());
        assertEquals("minecraft:dirt", closest.get(0).id());
        assertEquals(256, closest.get(0).nextTarget());
        assertEquals("minecraft:oak_planks", closest.get(1).id());
        assertEquals(512, closest.get(1).nextTarget());
        var allOak = Set.of(
                AchievementRules.claimKey(true, "minecraft:oak_planks", 0),
                AchievementRules.claimKey(true, "minecraft:oak_planks", 1),
                AchievementRules.claimKey(true, "minecraft:oak_planks", 2));
        assertTrue(AchievementRules.closest(Map.of("minecraft:oak_planks", 200), Map.of(), Map.of(), Map.of(), 15, allOak)
                .isEmpty());
        assertEquals(
                512, AchievementRules.lastPlaced("minecraft:oak_planks", 200, 0, claimed).nextTarget());
    }
}
