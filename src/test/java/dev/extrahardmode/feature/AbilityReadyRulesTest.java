package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertEquals(new AbilityReadyRules.Placement(184, 72, 8, 176, 72), placed.get(0));
        assertEquals(new AbilityReadyRules.Placement(188, 85, 4, 174, 83), placed.get(1));
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
        assertEquals(1.0 + 2.0 + 4.0 + 2.0 + 1.0, casts.get(0).level(), 1e-9);
        assertEquals(AbilityRules.FIRE_BOLT, casts.get(1).ability());
        assertEquals(3.0 + 2.0 + 1.0, casts.get(1).level(), 1e-9);
        assertEquals(AbilityRules.FLIGHT, casts.get(2).ability());
        assertEquals(1.0 + 2.0 + 1.0, casts.get(2).level(), 1e-9);
        assertEquals(AbilityRules.GROW, casts.get(3).ability());
        assertEquals(1.0 + 5.0 + 2.0 + 1.0, casts.get(3).level(), 1e-9);
        assertEquals(5, casts.size());
        assertEquals(AbilityRules.DIAMOND_SKIN, casts.get(4).ability());
        assertEquals(2.0 + 1.0, casts.get(4).level(), 1e-9);
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
    void honeyLetsHealingCastWithoutMana() {
        List<AbilityReadyRules.SupplyStack> paper = List.of(new AbilityReadyRules.SupplyStack(AbilityRules.PAPER_ID, 1));
        List<AbilityReadyRules.StrongCast> heal = AbilityReadyRules.strongCasts(
                0, 0.0, Map.of(AbilityRules.HEAL, 9), Set.of(AbilityRules.HEAL), 0, paper, true, 0L, Map.of());
        assertEquals(1, heal.size());
        assertEquals(3.0 + AbilityRules.HONEY_HEAL_BONUS, heal.get(0).level(), 1e-9);
    }
}
