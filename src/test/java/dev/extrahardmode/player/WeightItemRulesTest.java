package dev.extrahardmode.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WeightItemRulesTest {
    @Test
    void heavyGearWeighsHalfAgainAsMuch() {
        assertEquals(1.5, WeightItemRules.HEAVY, 0.0);
        assertEquals(3.0, WeightItemRules.contribution(2.0, "tougher:heavy_iron_chestplate"), 1e-9);
        assertEquals(0.75, WeightItemRules.contribution(0.5, "tougher:heavy_netherite_axe"), 1e-9);
        assertEquals(1.5, WeightItemRules.contribution(1.0, "heavy_golden_sword"), 1e-9);
        assertEquals(1.5, WeightItemRules.contribution(1.0, "tougher:heavy_copper_boots"), 1e-9);
        assertEquals(1.0, WeightItemRules.multiplier("minecraft:heavy_core"), 0.0);
        assertEquals(1.0, WeightItemRules.multiplier("minecraft:iron_chestplate"), 0.0);
        assertEquals(1.0, WeightItemRules.multiplier("minecraft:iron_sword"), 0.0);
    }

    @Test
    void leatherArmorWeighsHalf() {
        assertEquals(0.5, WeightItemRules.LEATHER_ARMOR, 0.0);
        assertEquals(1.0, WeightItemRules.contribution(2.0, "minecraft:leather_helmet"), 1e-9);
        assertEquals(0.5, WeightItemRules.contribution(1.0, "leather_chestplate"), 1e-9);
        assertEquals(1.0, WeightItemRules.contribution(2.0, "minecraft:leather_leggings"), 1e-9);
        assertEquals(1.0, WeightItemRules.contribution(2.0, "leather_boots"), 1e-9);
        assertEquals(1.0, WeightItemRules.multiplier("minecraft:leather_horse_armor"), 0.0);
        assertEquals(1.0, WeightItemRules.multiplier("minecraft:leather"), 0.0);
    }

    @Test
    void chainmailWeighsThreeQuarters() {
        assertEquals(0.75, WeightItemRules.CHAINMAIL, 0.0);
        assertEquals(1.5, WeightItemRules.contribution(2.0, "minecraft:chainmail_helmet"), 1e-9);
        assertEquals(1.5, WeightItemRules.contribution(2.0, "chainmail_chestplate"), 1e-9);
        assertEquals(0.75, WeightItemRules.contribution(1.0, "minecraft:chainmail_leggings"), 1e-9);
        assertEquals(1.5, WeightItemRules.contribution(2.0, "chainmail_boots"), 1e-9);
    }

    @Test
    void missingIdsStayAtTheUsualWeight() {
        assertEquals(1.0, WeightItemRules.multiplier(null), 0.0);
        assertEquals(1.0, WeightItemRules.multiplier(""), 0.0);
        assertEquals(2.0, WeightItemRules.contribution(2.0, null), 1e-9);
    }
}
