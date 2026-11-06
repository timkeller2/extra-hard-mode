package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AbilityClickRulesTest {
    @Test
    void aSecondClickOfTheSameItemWithinHalfASecondCasts() {
        assertEquals(10, AbilityClickRules.DOUBLE_CLICK_TICKS);
        var paper = new AbilityClickRules.Press(100L, "minecraft:paper");
        assertFalse(AbilityClickRules.completes(null, 100L, "minecraft:paper"));
        assertFalse(AbilityClickRules.completes(paper, 100L, "minecraft:paper"));
        assertFalse(AbilityClickRules.completes(paper, 99L, "minecraft:paper"));
        assertTrue(AbilityClickRules.completes(paper, 101L, "minecraft:paper"));
        assertTrue(AbilityClickRules.completes(paper, 110L, "minecraft:paper"));
        assertFalse(AbilityClickRules.completes(paper, 111L, "minecraft:paper"));
        assertFalse(AbilityClickRules.completes(paper, 105L, "minecraft:feather"));
        assertFalse(AbilityClickRules.completes(paper, 105L, ""));
        assertFalse(AbilityClickRules.completes(paper, 105L, null));
    }
}
