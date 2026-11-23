package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EnchantingRulesTest {
    @Test
    void tableAllowsRequirementAtOrBelowSkill() {
        assertEquals(3, EnchantingRules.DEFAULT_ABILITY);
        assertEquals(3, EnchantingRules.TRAIN_WINDOW);
        assertEquals(40, EnchantingRules.SKILL_DIE);
        assertFalse(EnchantingRules.canEnchant(3, 0));
        assertTrue(EnchantingRules.canEnchant(3, 1));
        assertTrue(EnchantingRules.canEnchant(3, 3));
        assertFalse(EnchantingRules.canEnchant(3, 4));
        assertFalse(EnchantingRules.canEnchant(0, 1));
    }

    @Test
    void requirementWithinThreeRaisesSkillByOne() {
        assertTrue(EnchantingRules.trains(3, 1));
        assertTrue(EnchantingRules.trains(3, 3));
        assertFalse(EnchantingRules.trains(3, 4));
        assertFalse(EnchantingRules.trains(10, 6));
        assertTrue(EnchantingRules.trains(10, 7));
        assertTrue(EnchantingRules.trains(10, 10));
        assertFalse(EnchantingRules.trains(10, 11));
        assertEquals(1, EnchantingRules.skillDieFace(0));
        assertEquals(40, EnchantingRules.skillDieFace(39));
        assertEquals(1, EnchantingRules.skillDieFace(40));
        assertEquals(40, EnchantingRules.skillDieFace(-1));
        assertFalse(EnchantingRules.skillRollGrants(3, 1));
        assertTrue(EnchantingRules.skillRollGrants(3, 2));
        assertTrue(EnchantingRules.skillRollGrants(40, 39));
        assertFalse(EnchantingRules.skillRollGrants(41, 39));
        assertEquals(4, EnchantingRules.nextAbility(3, 1, 39));
        assertEquals(4, EnchantingRules.nextAbility(3, 3, 2));
        assertEquals(3, EnchantingRules.nextAbility(3, 3, 1));
        assertEquals(3, EnchantingRules.nextAbility(3, 4, 39));
        assertEquals(10, EnchantingRules.nextAbility(10, 6, 39));
        assertEquals(11, EnchantingRules.nextAbility(10, 7, 39));
        assertEquals(10, EnchantingRules.nextAbility(10, 7, 8));
        assertEquals(10, EnchantingRules.nextAbility(10, 11, 39));
        assertEquals(41, EnchantingRules.nextAbility(40, 40, 39));
        assertEquals(41, EnchantingRules.nextAbility(41, 41, 39));
        assertEquals(Integer.MAX_VALUE, EnchantingRules.nextAbility(Integer.MAX_VALUE, Integer.MAX_VALUE, 39));
    }
}
