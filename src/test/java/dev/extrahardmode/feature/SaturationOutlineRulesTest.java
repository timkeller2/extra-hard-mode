package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SaturationOutlineRulesTest {
    @Test
    void coverageFillsFromTheRight() {
        assertEquals(0.0F, SaturationOutlineRules.coverage(0.0F, 0));
        assertEquals(0.25F, SaturationOutlineRules.coverage(0.4F, 0));
        assertEquals(0.5F, SaturationOutlineRules.coverage(0.6F, 0));
        assertEquals(0.75F, SaturationOutlineRules.coverage(1.2F, 0));
        assertEquals(1.0F, SaturationOutlineRules.coverage(2.0F, 0));
        assertEquals(0.0F, SaturationOutlineRules.coverage(2.0F, 1));
        assertEquals(0.25F, SaturationOutlineRules.coverage(2.1F, 1));
        assertEquals(1.0F, SaturationOutlineRules.coverage(20.0F, 9));
        assertEquals(0.5F, SaturationOutlineRules.coverage(21.0F, 10));
        assertEquals(1.0F, SaturationOutlineRules.coverage(24.0F, 11));
        assertEquals(0.0F, SaturationOutlineRules.coverage(24.0F, 12));
        assertEquals(0.0F, SaturationOutlineRules.coverage(5.0F, -1));
    }

    @Test
    void clipWidthMatchesTheCoveredSide() {
        assertEquals(0, SaturationOutlineRules.clipWidth(0.0F));
        assertEquals(3, SaturationOutlineRules.clipWidth(0.25F));
        assertEquals(5, SaturationOutlineRules.clipWidth(0.5F));
        assertEquals(7, SaturationOutlineRules.clipWidth(0.75F));
        assertEquals(9, SaturationOutlineRules.clipWidth(1.0F));
    }

    @Test
    void iconsRunRightToLeftAndUpARow() {
        assertEquals(91, SaturationOutlineRules.iconX(100, 0));
        assertEquals(50, SaturationOutlineRules.iconY(50, 0));
        assertEquals(91, SaturationOutlineRules.iconX(100, 10));
        assertEquals(40, SaturationOutlineRules.iconY(50, 10));
        assertEquals(0xFFFFAA00, SaturationOutlineRules.GOLD);
    }
}
