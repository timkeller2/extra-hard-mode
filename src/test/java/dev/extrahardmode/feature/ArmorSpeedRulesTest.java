package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ArmorSpeedRulesTest {
    private static final int METAL = ArmorSpeedRules.DEFAULT_COPPER_IRON_GOLD_PERCENT;
    private static final int DIAMOND = ArmorSpeedRules.DEFAULT_DIAMOND_PERCENT;
    private static final int LIGHT = ArmorSpeedRules.DEFAULT_LEATHER_CHAINMAIL_PERCENT;
    private static final int HEAVY = ArmorSpeedRules.DEFAULT_HEAVY_EXTRA_PERCENT;

    @Test
    void defaultsMatchTheFourConfiguredGroups() {
        assertEquals(10, METAL);
        assertEquals(5, DIAMOND);
        assertEquals(0, LIGHT);
        assertEquals(5, HEAVY);
    }

    @Test
    void fullSetsUseTheMaterialPercent() {
        assertEquals(10.0, full("copper"), 0.0);
        assertEquals(10.0, full("iron"), 0.0);
        assertEquals(10.0, full("golden"), 0.0);
        assertEquals(5.0, full("diamond"), 0.0);
        assertEquals(5.0, full("netherite"), 0.0);
        assertEquals(0.0, full("leather"), 0.0);
        assertEquals(0.0, full("chainmail"), 0.0);
    }

    @Test
    void eachPieceIsOneQuarter() {
        assertEquals(2.5, piece("minecraft:iron_boots"), 0.0);
        assertEquals(2.5, piece("copper_helmet"), 0.0);
        assertEquals(2.5, piece("minecraft:golden_chestplate"), 0.0);
        assertEquals(2.5, piece("minecraft:gold_leggings"), 0.0);
        assertEquals(1.25, piece("minecraft:diamond_chestplate"), 0.0);
        assertEquals(1.25, piece("netherite_boots"), 0.0);
        assertEquals(0.0, piece("minecraft:leather_helmet"), 0.0);
        assertEquals(0.0, piece("chainmail_chestplate"), 0.0);
    }

    @Test
    void heavyAddsFivePointsToThatMaterial() {
        assertEquals(15.0, fullHeavy("copper"), 0.0);
        assertEquals(15.0, fullHeavy("iron"), 0.0);
        assertEquals(10.0, fullHeavy("diamond"), 0.0);
        assertEquals(10.0, fullHeavy("netherite"), 0.0);
        assertEquals(3.75, piece("tougher:heavy_iron_chestplate"), 0.0);
        assertEquals(2.5, piece("tougher:heavy_netherite_boots"), 0.0);
    }

    @Test
    void mixedSetsAddTheWornPieces() {
        double mixed = ArmorSpeedRules.slowPercent(
                List.of(
                        "minecraft:iron_helmet",
                        "minecraft:iron_chestplate",
                        "minecraft:diamond_leggings",
                        "minecraft:diamond_boots"),
                METAL,
                DIAMOND,
                LIGHT,
                HEAVY);
        assertEquals(7.5, mixed, 0.0);
        double oneHeavy = ArmorSpeedRules.slowPercent(
                List.of(
                        "tougher:heavy_iron_helmet",
                        "minecraft:iron_chestplate",
                        "minecraft:iron_leggings",
                        "minecraft:iron_boots"),
                METAL,
                DIAMOND,
                LIGHT,
                HEAVY);
        assertEquals(11.25, oneHeavy, 0.0);
    }

    @Test
    void weaponsAndOtherSlotsStayAtZero() {
        assertEquals(0.0, piece("minecraft:iron_sword"), 0.0);
        assertEquals(0.0, piece("tougher:heavy_iron_sword"), 0.0);
        assertEquals(0.0, piece("tougher:heavy_golden_sword"), 0.0);
        assertEquals(0.0, piece("tougher:heavy_copper_axe"), 0.0);
        assertEquals(0.0, piece("minecraft:diamond_pickaxe"), 0.0);
        assertEquals(0.0, piece("minecraft:elytra"), 0.0);
        assertEquals(0.0, piece("minecraft:turtle_helmet"), 0.0);
        assertEquals(0.0, piece("minecraft:carved_pumpkin"), 0.0);
        assertEquals(0.0, piece("minecraft:iron_horse_armor"), 0.0);
        assertEquals(0.0, piece(null), 0.0);
        assertEquals(0.0, piece("  "), 0.0);
        assertEquals(0.0, ArmorSpeedRules.slowPercent(List.of(), METAL, DIAMOND, LIGHT, HEAVY), 0.0);
    }

    @Test
    void configuredPercentsReplaceTheDefaults() {
        assertEquals(5.0, ArmorSpeedRules.piecePercent("minecraft:iron_helmet", 20, 5, 0, 5), 0.0);
        assertEquals(4.0, ArmorSpeedRules.piecePercent("minecraft:leather_boots", 10, 5, 16, 5), 0.0);
        assertEquals(3.0, ArmorSpeedRules.piecePercent("tougher:heavy_diamond_helmet", 10, 8, 0, 4), 0.0);
    }

    @Test
    void modifierIsTheUnarmoredPaceTimesTheRemainingShare() {
        assertEquals(0.1, ArmorSpeedRules.modifierAmount(0.22, 0.0), 1.0E-9);
        assertEquals(-0.01, ArmorSpeedRules.modifierAmount(0.22, 10.0), 1.0E-9);
        assertEquals(0.045, ArmorSpeedRules.modifierAmount(0.22, 5.0), 1.0E-9);
        assertEquals(-0.065, ArmorSpeedRules.modifierAmount(0.22, 15.0), 1.0E-9);
        assertEquals(-1.0, ArmorSpeedRules.modifierAmount(0.22, 100.0), 1.0E-9);
        assertEquals(-1.0, ArmorSpeedRules.modifierAmount(0.22, 150.0), 1.0E-9);
        assertEquals(0.21, ArmorSpeedRules.modifierAmount(0.22, -10.0), 1.0E-9);
    }

    private static double piece(String id) {
        return ArmorSpeedRules.piecePercent(id, METAL, DIAMOND, LIGHT, HEAVY);
    }

    private static double full(String material) {
        return ArmorSpeedRules.slowPercent(
                List.of(
                        "minecraft:" + material + "_helmet",
                        "minecraft:" + material + "_chestplate",
                        "minecraft:" + material + "_leggings",
                        "minecraft:" + material + "_boots"),
                METAL,
                DIAMOND,
                LIGHT,
                HEAVY);
    }

    private static double fullHeavy(String material) {
        return ArmorSpeedRules.slowPercent(
                List.of(
                        "tougher:heavy_" + material + "_helmet",
                        "tougher:heavy_" + material + "_chestplate",
                        "tougher:heavy_" + material + "_leggings",
                        "tougher:heavy_" + material + "_boots"),
                METAL,
                DIAMOND,
                LIGHT,
                HEAVY);
    }
}
