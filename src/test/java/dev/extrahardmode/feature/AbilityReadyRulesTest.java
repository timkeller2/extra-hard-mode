package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AbilityReadyRulesTest {
    @Test
    void iconPixelsIsFivePercentOfNormalAboveLevelTwo() {
        assertEquals(0, AbilityReadyRules.iconPixels(2.0));
        assertEquals(0, AbilityReadyRules.iconPixels(1.0));
        assertEquals(2, AbilityReadyRules.iconPixels(2.01));
        assertEquals(2, AbilityReadyRules.iconPixels(3.0));
        assertEquals(8, AbilityReadyRules.iconPixels(10.0));
        assertEquals(12, AbilityReadyRules.iconPixels(15.0));
        assertEquals(0, AbilityReadyRules.scaledPixels(0, 8));
        assertEquals(0, AbilityReadyRules.scaledPixels(6, 0));
        assertEquals(3, AbilityReadyRules.scaledPixels(6, 8));
        assertEquals(1, AbilityReadyRules.scaledPixels(6, 2));
    }

    @Test
    void levelNumberRoundsTheEffectiveLevel() {
        assertEquals(0, AbilityReadyRules.levelNumber(2.0));
        assertEquals(2, AbilityReadyRules.levelNumber(2.01));
        assertEquals(2, AbilityReadyRules.levelNumber(2.4));
        assertEquals(3, AbilityReadyRules.levelNumber(2.5));
        assertEquals(10, AbilityReadyRules.levelNumber(10.4));
        assertEquals(11, AbilityReadyRules.levelNumber(10.5));
    }

    @Test
    void placeStacksIconsAndLevelsInTheBottomRight() {
        List<AbilityReadyRules.Placement> placed =
                AbilityReadyRules.place(200, 100, List.of(8, 4), List.of(6, 12), 9);
        assertEquals(2, placed.size());
        assertEquals(new AbilityReadyRules.Placement(179, 78, 8, 189, 79), placed.get(0));
        assertEquals(new AbilityReadyRules.Placement(183, 88, 4, 189, 89), placed.get(1));
        assertTrue(placed.get(0).labelX() > placed.get(0).x() + placed.get(0).size());
        assertTrue(placed.get(1).labelX() > placed.get(1).x() + placed.get(1).size());
        assertTrue(AbilityReadyRules.place(200, 100, List.of(), List.of(), 9).isEmpty());
        assertTrue(AbilityReadyRules.place(200, 100, List.of(0), List.of(6), 9).isEmpty());
    }

    @Test
    void strongCastsNeedACatalystManaAndLevelAboveTwo() {
        List<AbilityReadyRules.StrongCast> none = AbilityReadyRules.strongCasts(
                4, 5.0, Map.of(AbilityRules.HEAL, 16), Set.of(AbilityRules.HEAL), 0, List.of(), false, 0L, Map.of());
        assertTrue(none.isEmpty());

        List<AbilityReadyRules.SupplyStack> paper = List.of(new AbilityReadyRules.SupplyStack(AbilityRules.PAPER_ID, 1));
        assertTrue(AbilityReadyRules.strongCasts(
                        4, 0.0, Map.of(AbilityRules.HEAL, 16), Set.of(AbilityRules.HEAL), 0, paper, false, 0L, Map.of())
                .isEmpty());

        List<AbilityReadyRules.StrongCast> heal = AbilityReadyRules.strongCasts(
                4, 5.0, Map.of(AbilityRules.HEAL, 16), Set.of(AbilityRules.HEAL), 0, paper, false, 0L, Map.of());
        assertEquals(List.of(new AbilityReadyRules.StrongCast(AbilityRules.HEAL, 4.0)), heal);

        List<AbilityReadyRules.StrongCast> exactTwo = AbilityReadyRules.strongCasts(
                4, 5.0, Map.of(AbilityRules.HEAL, 4), Set.of(AbilityRules.HEAL), 0, paper, false, 0L, Map.of());
        assertTrue(exactTwo.isEmpty());
    }

    @Test
    void strongCastsUseBestToolRedstoneHoneyGoldAndManaLevel() {
        List<AbilityReadyRules.SupplyStack> supplies = List.of(
                new AbilityReadyRules.SupplyStack(AbilityRules.PAPER_ID, 2),
                new AbilityReadyRules.SupplyStack(AbilityRules.FEATHER_ID, 1),
                new AbilityReadyRules.SupplyStack(AbilityRules.NETHERITE_HOE_ID, 1),
                new AbilityReadyRules.SupplyStack(AbilityRules.WOODEN_HOE_ID, 1),
                new AbilityReadyRules.SupplyStack(AbilityRules.CHARCOAL_ID, 1),
                new AbilityReadyRules.SupplyStack(AbilityReadyRules.REDSTONE_ID, 4),
                new AbilityReadyRules.SupplyStack(AbilityRules.DIAMOND_ID, 1));
        List<AbilityReadyRules.StrongCast> casts = AbilityReadyRules.strongCasts(
                3,
                5.0,
                Map.of(
                        AbilityRules.HEAL, 1,
                        AbilityRules.FLIGHT, 1,
                        AbilityRules.GROW, 1,
                        AbilityRules.FIRE_BOLT, 0,
                        AbilityRules.DIAMOND_SKIN, 0),
                Set.of(
                        AbilityRules.HEAL,
                        AbilityRules.FLIGHT,
                        AbilityRules.GROW,
                        AbilityRules.FIRE_BOLT,
                        AbilityRules.DIAMOND_SKIN),
                1,
                supplies,
                true,
                0L,
                Map.of());
        assertEquals(AbilityRules.HEAL, casts.get(0).ability());
        assertEquals(1.0 + 2.0 + 4.0, casts.get(0).level(), 1e-9);
        assertEquals(AbilityRules.GROW, casts.get(1).ability());
        assertEquals(1.0 + 5.0, casts.get(1).level(), 1e-9);
        assertEquals(2, casts.size());
        assertTrue(casts.stream().noneMatch(cast -> AbilityRules.FIRE_BOLT.equals(cast.ability())));
        assertTrue(casts.stream().noneMatch(cast -> AbilityRules.FLIGHT.equals(cast.ability())));
        assertTrue(casts.stream().noneMatch(cast -> AbilityRules.DIAMOND_SKIN.equals(cast.ability())));

        List<AbilityReadyRules.StrongCast> boosted = AbilityReadyRules.strongCasts(
                3,
                5.0,
                Map.of(
                        AbilityRules.HEAL, 1,
                        AbilityRules.FLIGHT, 1,
                        AbilityRules.GROW, 1,
                        AbilityRules.FIRE_BOLT, 0,
                        AbilityRules.DIAMOND_SKIN, 0),
                Set.of(
                        AbilityRules.HEAL,
                        AbilityRules.FLIGHT,
                        AbilityRules.GROW,
                        AbilityRules.FIRE_BOLT,
                        AbilityRules.DIAMOND_SKIN),
                1,
                supplies,
                true,
                0L,
                Map.of(),
                2.0);
        assertEquals(1.0 + 2.0 + 4.0 + 2.0, boosted.get(0).level(), 1e-9);
        assertEquals(AbilityRules.FLIGHT, boosted.get(1).ability());
        assertEquals(1.0 + 2.0, boosted.get(1).level(), 1e-9);
        assertEquals(AbilityRules.GROW, boosted.get(2).ability());
        assertEquals(1.0 + 5.0 + 2.0, boosted.get(2).level(), 1e-9);
        assertEquals(AbilityRules.DIAMOND_SKIN, boosted.get(3).ability());
        assertEquals(3.0, boosted.get(3).level(), 1e-9);
        assertEquals(AbilityRules.EMPOWER, boosted.get(4).ability());
        assertEquals(3.0, boosted.get(4).level(), 1e-9);
        assertEquals(5, boosted.size());
        assertTrue(boosted.stream().noneMatch(cast -> AbilityRules.FIRE_BOLT.equals(cast.ability())));
    }

    @Test
    void batteryCastsWithNoManaWhenItsLevelIsAboveTwo() {
        List<AbilityReadyRules.SupplyStack> block =
                List.of(new AbilityReadyRules.SupplyStack(AbilityRules.QUARTZ_BLOCK_ID, 1));
        List<AbilityReadyRules.StrongCast> casts = AbilityReadyRules.strongCasts(
                0,
                0.0,
                Map.of(AbilityRules.BATTERY, 9),
                Set.of(AbilityRules.BATTERY),
                0,
                block,
                false,
                0L,
                Map.of());
        assertEquals(List.of(AbilityRules.BATTERY), casts.stream().map(AbilityReadyRules.StrongCast::ability).toList());
        assertEquals(3.0, casts.get(0).level(), 1e-9);
    }

    @Test
    void detectOreCastsWithoutQuartzAndUsesTheLargestStack() {
        Map<String, Integer> uses = Map.of(AbilityRules.DETECT_ORE, 16);
        Set<String> learned = Set.of(AbilityRules.DETECT_ORE);
        List<AbilityReadyRules.StrongCast> bare = AbilityReadyRules.strongCasts(
                1,
                5.0,
                uses,
                learned,
                0,
                List.of(new AbilityReadyRules.SupplyStack(AbilityRules.COMPASS_ID, 1)),
                false,
                0L,
                Map.of());
        assertEquals(List.of(AbilityRules.DETECT_ORE), bare.stream().map(AbilityReadyRules.StrongCast::ability).toList());
        assertEquals(4.0, bare.get(0).level(), 1e-9);

        List<AbilityReadyRules.StrongCast> stacked = AbilityReadyRules.strongCasts(
                1,
                5.0,
                uses,
                learned,
                0,
                List.of(
                        new AbilityReadyRules.SupplyStack(AbilityRules.COMPASS_ID, 1),
                        new AbilityReadyRules.SupplyStack(AbilityReadyRules.QUARTZ_ID, 1),
                        new AbilityReadyRules.SupplyStack(AbilityReadyRules.QUARTZ_ID, 2)),
                false,
                0L,
                Map.of());
        assertEquals(4.0 + 1.0, stacked.get(0).level(), 1e-9);

        List<AbilityReadyRules.StrongCast> full = AbilityReadyRules.strongCasts(
                1,
                5.0,
                uses,
                learned,
                0,
                List.of(
                        new AbilityReadyRules.SupplyStack(AbilityRules.COMPASS_ID, 1),
                        new AbilityReadyRules.SupplyStack(AbilityReadyRules.QUARTZ_ID, 64)),
                false,
                0L,
                Map.of());
        assertEquals(4.0 + 8.0, full.get(0).level(), 1e-9);
    }

    @Test
    void magicArrowGainsTwoLevelsFromCharcoalInTheSupplyList() {
        Map<String, Integer> uses = Map.of(AbilityRules.MAGIC_ARROW, 4);
        Set<String> learned = Set.of(AbilityRules.MAGIC_ARROW);
        List<AbilityReadyRules.StrongCast> bare = AbilityReadyRules.strongCasts(
                4,
                5.0,
                uses,
                learned,
                0,
                List.of(new AbilityReadyRules.SupplyStack(AbilityRules.ARROW_ID, 1)),
                false,
                0L,
                Map.of());
        assertTrue(bare.isEmpty());

        List<AbilityReadyRules.StrongCast> fed = AbilityReadyRules.strongCasts(
                4,
                5.0,
                uses,
                learned,
                0,
                List.of(
                        new AbilityReadyRules.SupplyStack(AbilityRules.ARROW_ID, 1),
                        new AbilityReadyRules.SupplyStack(AbilityRules.CHARCOAL_ID, 3)),
                false,
                0L,
                Map.of());
        assertEquals(List.of(AbilityRules.MAGIC_ARROW), fed.stream().map(AbilityReadyRules.StrongCast::ability).toList());
        assertEquals(4.0, fed.get(0).level(), 1e-9);

        List<AbilityReadyRules.StrongCast> doubled = AbilityReadyRules.strongCasts(
                4,
                5.0,
                uses,
                learned,
                0,
                List.of(
                        new AbilityReadyRules.SupplyStack(AbilityRules.ARROW_ID, 2),
                        new AbilityReadyRules.SupplyStack(AbilityRules.CHARCOAL_ID, 1)),
                false,
                0L,
                Map.of());
        assertEquals(6.0, doubled.get(0).level(), 1e-9);
    }

    @Test
    void goldBonusShortensCooldownWithoutRaisingOtherAbilityPower() {
        List<AbilityReadyRules.SupplyStack> feather =
                List.of(new AbilityReadyRules.SupplyStack(AbilityRules.FEATHER_ID, 1));
        Map<String, Integer> uses = Map.of(AbilityRules.FLIGHT, 16);
        Map<String, Long> lastUse = Map.of(AbilityRules.FLIGHT, 0L);
        long withGold = AbilityRules.cooldownTicks(1, 16);
        assertTrue(withGold < AbilityRules.cooldownTicks(0, 16));
        assertTrue(AbilityReadyRules.strongCasts(0, 5.0, uses, Set.of(AbilityRules.FLIGHT), 0, feather, false, withGold, lastUse)
                .isEmpty());
        List<AbilityReadyRules.StrongCast> ready = AbilityReadyRules.strongCasts(
                0, 5.0, uses, Set.of(AbilityRules.FLIGHT), 1, feather, false, withGold, lastUse);
        assertEquals(List.of(AbilityRules.FLIGHT), ready.stream().map(AbilityReadyRules.StrongCast::ability).toList());
        assertEquals(4.0, ready.get(0).level(), 1e-9);
    }

    @Test
    void cooldownUntrainedAndBareDiamondSkinStayHidden() {
        List<AbilityReadyRules.SupplyStack> feather =
                List.of(new AbilityReadyRules.SupplyStack(AbilityRules.FEATHER_ID, 1));
        assertTrue(AbilityReadyRules.strongCasts(
                        4,
                        5.0,
                        Map.of(AbilityRules.FLIGHT, 16),
                        Set.of(AbilityRules.FLIGHT),
                        0,
                        feather,
                        false,
                        100L,
                        Map.of(AbilityRules.FLIGHT, 100L))
                .isEmpty());

        List<AbilityReadyRules.StrongCast> untrained = AbilityReadyRules.strongCasts(
                2,
                5.0,
                Map.of(AbilityRules.FLIGHT, 36),
                Set.of(AbilityRules.HEAL),
                0,
                feather,
                false,
                0L,
                Map.of());
        assertEquals(3.0, untrained.get(0).level(), 1e-9);

        assertTrue(AbilityReadyRules.strongCasts(
                        2,
                        5.0,
                        Map.of(AbilityRules.FLIGHT, 25),
                        Set.of(AbilityRules.HEAL),
                        0,
                        feather,
                        false,
                        0L,
                        Map.of())
                .isEmpty());

        List<AbilityReadyRules.SupplyStack> diamond =
                List.of(new AbilityReadyRules.SupplyStack(AbilityRules.DIAMOND_ID, 1));
        assertTrue(AbilityReadyRules.strongCasts(
                        4, 5.0, Map.of(), Set.of(AbilityRules.DIAMOND_SKIN), 0, diamond, false, 0L, Map.of())
                .isEmpty());
    }

    @Test
    void hudIconsSortByLevelAndFilterUnlessHoldingABook() {
        assertTrue(AbilityReadyRules.countsForAbilitySupply(AbilityReadyRules.REDSTONE_ID, false));
        assertTrue(AbilityReadyRules.countsForAbilitySupply(AbilityReadyRules.REDSTONE_ID, true));
        assertTrue(AbilityReadyRules.countsForAbilitySupply(AbilityRules.FEATHER_ID, true));
        assertFalse(AbilityReadyRules.countsForAbilitySupply("", true));
        assertTrue(AbilityReadyRules.isBook("minecraft:book"));
        assertTrue(AbilityReadyRules.isBook("minecraft:enchanted_book"));
        assertTrue(AbilityReadyRules.isBook("minecraft:written_book"));
        assertFalse(AbilityReadyRules.isBook("minecraft:bookshelf"));
        assertFalse(AbilityReadyRules.isBook("minecraft:feather"));
        assertEquals(0, AbilityReadyRules.cooldownSeconds(0));
        assertEquals(1, AbilityReadyRules.cooldownSeconds(1));
        assertEquals(1, AbilityReadyRules.cooldownSeconds(20));
        assertEquals(2, AbilityReadyRules.cooldownSeconds(21));
        assertEquals(8, AbilityReadyRules.hudIconPixels(1.0, true));
        assertEquals(0, AbilityReadyRules.hudIconPixels(1.0, false));
        assertEquals(8, AbilityReadyRules.hudIconPixels(10.0, true));

        List<AbilityReadyRules.SupplyStack> supplies = List.of(
                new AbilityReadyRules.SupplyStack(AbilityRules.PAPER_ID, 1),
                new AbilityReadyRules.SupplyStack(AbilityRules.FEATHER_ID, 1),
                new AbilityReadyRules.SupplyStack(AbilityRules.CHARCOAL_ID, 1));
        Map<String, Integer> uses = Map.of(
                AbilityRules.HEAL, 100,
                AbilityRules.FLIGHT, 16,
                AbilityRules.FIRE_BOLT, 0);
        List<AbilityReadyRules.HudIcon> withBook = AbilityReadyRules.hudIcons(
                3,
                uses,
                Set.of(AbilityRules.HEAL),
                0,
                supplies,
                false,
                0L,
                Map.of(),
                true,
                null,
                Set.of());
        assertEquals(2, withBook.size());
        assertEquals(AbilityRules.HEAL, withBook.get(0).ability());
        assertEquals(10.0, withBook.get(0).level(), 1e-9);
        assertTrue(withBook.get(0).learned());
        assertEquals(AbilityRules.FLIGHT, withBook.get(1).ability());
        assertEquals(4.0, withBook.get(1).level(), 1e-9);
        assertFalse(withBook.get(1).learned());
        assertTrue(withBook.stream().noneMatch(icon -> AbilityRules.FIRE_BOLT.equals(icon.ability())));
        assertTrue(withBook.stream().noneMatch(icon -> AbilityRules.MAGIC_ARROW.equals(icon.ability())));

        List<AbilityReadyRules.HudIcon> hidden = AbilityReadyRules.hudIcons(
                3, uses, Set.of(AbilityRules.HEAL), 0, supplies, false, 0L, Map.of(), false, null, Set.of());
        assertTrue(hidden.isEmpty());

        List<AbilityReadyRules.HudIcon> held = AbilityReadyRules.hudIcons(
                3,
                uses,
                Set.of(AbilityRules.HEAL),
                0,
                supplies,
                false,
                0L,
                Map.of(),
                false,
                AbilityRules.FLIGHT,
                Set.of());
        assertEquals(List.of(AbilityRules.FLIGHT), held.stream().map(AbilityReadyRules.HudIcon::ability).toList());

        int healCooldown = AbilityRules.cooldownTicks(AbilityRules.HEAL, 3, 100);
        List<AbilityReadyRules.HudIcon> cooling = AbilityReadyRules.hudIcons(
                3,
                uses,
                Set.of(AbilityRules.HEAL),
                0,
                List.of(),
                false,
                50L,
                Map.of(AbilityRules.HEAL, 50L),
                false,
                null,
                Set.of(AbilityRules.POWER_MINE));
        assertEquals(AbilityRules.HEAL, cooling.get(0).ability());
        assertEquals(AbilityReadyRules.cooldownSeconds(healCooldown), cooling.get(0).cooldownSeconds());
        assertEquals(AbilityRules.POWER_MINE, cooling.get(1).ability());
        assertEquals(0, cooling.get(1).cooldownSeconds());
        assertEquals(AbilityReadyRules.FORCED_ICON_PIXELS, AbilityReadyRules.hudIconPixels(cooling.get(1).level(), true));
    }

    @Test
    void honeyLetsHealingCastWithoutMana() {
        List<AbilityReadyRules.SupplyStack> paper = List.of(new AbilityReadyRules.SupplyStack(AbilityRules.PAPER_ID, 1));
        List<AbilityReadyRules.StrongCast> heal = AbilityReadyRules.strongCasts(
                0, 0.0, Map.of(AbilityRules.HEAL, 9), Set.of(AbilityRules.HEAL), 0, paper, true, 0L, Map.of());
        assertEquals(1, heal.size());
        assertEquals(3.0 + AbilityRules.HONEY_HEAL_BONUS, heal.get(0).level(), 1e-9);
    }
}
