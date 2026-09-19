package dev.extrahardmode.item;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HeavyArmorRulesTest {
    @Test
    void setTotalsMatchSpec() {
        assertEquals(5.0, HeavyArmorRules.setHearts("copper"), 0.001);
        assertEquals(10.0, HeavyArmorRules.setHearts("iron"), 0.001);
        assertEquals(20.0, HeavyArmorRules.setHearts("diamond"), 0.001);
        assertEquals(30.0, HeavyArmorRules.setHearts("netherite"), 0.001);
        assertEquals(10.0, HeavyArmorRules.setHealth("copper"), 0.001);
        assertEquals(20.0, HeavyArmorRules.setHealth("iron"), 0.001);
        assertEquals(40.0, HeavyArmorRules.setHealth("diamond"), 0.001);
        assertEquals(60.0, HeavyArmorRules.setHealth("netherite"), 0.001);
    }

    @Test
    void copperHeartsMatchSpec() {
        assertEquals(1.0, HeavyArmorRules.bonusHearts("copper", "helmet"), 0.001);
        assertEquals(2.0, HeavyArmorRules.bonusHearts("copper", "chestplate"), 0.001);
        assertEquals(1.5, HeavyArmorRules.bonusHearts("copper", "leggings"), 0.001);
        assertEquals(0.5, HeavyArmorRules.bonusHearts("copper", "boots"), 0.001);
        assertEquals(4.0, HeavyArmorRules.bonusHealth("copper", "chestplate"), 0.001);
        assertEquals(2.0, HeavyArmorRules.bonusHealth("copper", "helmet"), 0.001);
    }

    @Test
    void ironHeartsMatchSpec() {
        assertEquals(2.0, HeavyArmorRules.bonusHearts("iron", "helmet"), 0.001);
        assertEquals(4.0, HeavyArmorRules.bonusHearts("iron", "chestplate"), 0.001);
        assertEquals(3.0, HeavyArmorRules.bonusHearts("iron", "leggings"), 0.001);
        assertEquals(1.0, HeavyArmorRules.bonusHearts("iron", "boots"), 0.001);
    }

    @Test
    void diamondHeartsMatchSpec() {
        assertEquals(4.0, HeavyArmorRules.bonusHearts("diamond", "helmet"), 0.001);
        assertEquals(8.0, HeavyArmorRules.bonusHearts("diamond", "chestplate"), 0.001);
        assertEquals(6.0, HeavyArmorRules.bonusHearts("diamond", "leggings"), 0.001);
        assertEquals(2.0, HeavyArmorRules.bonusHearts("diamond", "boots"), 0.001);
    }

    @Test
    void netheriteHeartsMatchSpec() {
        assertEquals(6.0, HeavyArmorRules.bonusHearts("netherite", "helmet"), 0.001);
        assertEquals(12.0, HeavyArmorRules.bonusHearts("netherite", "chestplate"), 0.001);
        assertEquals(9.0, HeavyArmorRules.bonusHearts("netherite", "leggings"), 0.001);
        assertEquals(3.0, HeavyArmorRules.bonusHearts("netherite", "boots"), 0.001);
    }

    @Test
    void healthIsTwoPerHeart() {
        assertEquals(8.0, HeavyArmorRules.bonusHealth("iron", "chestplate"), 0.001);
        assertEquals(16.0, HeavyArmorRules.bonusHealth("diamond", "chestplate"), 0.001);
        assertEquals(24.0, HeavyArmorRules.bonusHealth("netherite", "chestplate"), 0.001);
        assertEquals(0.0, HeavyArmorRules.bonusHealth("iron", "unknown"), 0.001);
    }

    @Test
    void piecesLastTwiceVanilla() {
        assertEquals(2, HeavyArmorRules.DURABILITY_MULTIPLIER);
        assertEquals(330, HeavyArmorRules.durability(165));
        assertEquals(480, HeavyArmorRules.durability(240));
        assertEquals(0, HeavyArmorRules.durability(0));
    }
}
