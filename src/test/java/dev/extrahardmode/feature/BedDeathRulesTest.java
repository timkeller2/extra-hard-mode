package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BedDeathRulesTest {
    @Test
    void sixBlockCubeIncludesEdgesAndCorners() {
        assertTrue(BedDeathRules.withinRange(0, 0, 0, 6, 0, 0));
        assertTrue(BedDeathRules.withinRange(0, 0, 0, 0, 6, 0));
        assertTrue(BedDeathRules.withinRange(0, 0, 0, 6, 6, 6));
        assertFalse(BedDeathRules.withinRange(0, 0, 0, 7, 0, 0));
        assertFalse(BedDeathRules.withinRange(0, 0, 0, 0, -7, 0));
        assertTrue(BedDeathRules.withinRange(10, 64, -4, 10, 64, -4));
        assertFalse(BedDeathRules.withinRange(10, 64, -4, 17, 64, -4));
    }

    @Test
    void rangeIncludesTheOtherBedHalf() {
        assertTrue(BedDeathRules.withinRangeOfBed(7, 64, 0, 0, 64, 0));
        assertFalse(BedDeathRules.withinRangeOfBed(8, 64, 0, 0, 64, 0));
        assertTrue(BedDeathRules.withinRangeOfBed(0, 64, 7, 0, 64, 0));
    }
}
