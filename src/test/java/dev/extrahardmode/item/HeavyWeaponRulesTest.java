package dev.extrahardmode.item;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HeavyWeaponRulesTest {
    @Test
    void durabilityIsThreeTimesVanilla() {
        assertEquals(3, HeavyWeaponRules.DURABILITY_MULTIPLIER);
        assertEquals(570, HeavyWeaponRules.durability(HeavyWeaponRules.COPPER_DURABILITY));
        assertEquals(750, HeavyWeaponRules.durability(HeavyWeaponRules.IRON_DURABILITY));
        assertEquals(96, HeavyWeaponRules.durability(HeavyWeaponRules.GOLD_DURABILITY));
        assertEquals(4683, HeavyWeaponRules.durability(HeavyWeaponRules.DIAMOND_DURABILITY));
        assertEquals(6093, HeavyWeaponRules.durability(HeavyWeaponRules.NETHERITE_DURABILITY));
        assertEquals(0, HeavyWeaponRules.durability(0));
    }

    @Test
    void swordsHitTwoHarderAtTheSameSpeed() {
        assertEquals(2.0F, HeavyWeaponRules.DAMAGE_BONUS, 0.001F);
        assertEquals(5.0F, HeavyWeaponRules.swordAttack(), 0.001F);
        assertEquals(-2.4F, HeavyWeaponRules.SWORD_SPEED, 0.001F);
    }

    @Test
    void axesHitTwoHarderAtTheVanillaSpeed() {
        assertEquals(9.0F, HeavyWeaponRules.axeAttack(HeavyWeaponRules.COPPER_AXE_ATTACK), 0.001F);
        assertEquals(-3.2F, HeavyWeaponRules.COPPER_AXE_SPEED, 0.001F);
        assertEquals(8.0F, HeavyWeaponRules.axeAttack(HeavyWeaponRules.IRON_AXE_ATTACK), 0.001F);
        assertEquals(-3.1F, HeavyWeaponRules.IRON_AXE_SPEED, 0.001F);
        assertEquals(8.0F, HeavyWeaponRules.axeAttack(HeavyWeaponRules.GOLD_AXE_ATTACK), 0.001F);
        assertEquals(-3.0F, HeavyWeaponRules.GOLD_AXE_SPEED, 0.001F);
        assertEquals(7.0F, HeavyWeaponRules.axeAttack(HeavyWeaponRules.DIAMOND_AXE_ATTACK), 0.001F);
        assertEquals(-3.0F, HeavyWeaponRules.DIAMOND_AXE_SPEED, 0.001F);
        assertEquals(7.0F, HeavyWeaponRules.axeAttack(HeavyWeaponRules.NETHERITE_AXE_ATTACK), 0.001F);
        assertEquals(-3.0F, HeavyWeaponRules.NETHERITE_AXE_SPEED, 0.001F);
    }
}
