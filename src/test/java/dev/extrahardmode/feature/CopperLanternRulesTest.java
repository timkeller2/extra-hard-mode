package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CopperLanternRulesTest {
    @Test
    void surfaceAndAboveStayFull() {
        assertEquals(15, CopperLanternRules.light(15, 0));
        assertEquals(15, CopperLanternRules.light(15, 1));
        assertEquals(15, CopperLanternRules.light(15, 64));
    }

    @Test
    void eachTenBlocksBelowRemovesOne() {
        assertEquals(15, CopperLanternRules.light(15, -1));
        assertEquals(15, CopperLanternRules.light(15, -9));
        assertEquals(14, CopperLanternRules.light(15, -10));
        assertEquals(14, CopperLanternRules.light(15, -19));
        assertEquals(13, CopperLanternRules.light(15, -20));
        assertEquals(9, CopperLanternRules.light(15, -64));
        assertEquals(0, CopperLanternRules.light(15, -150));
        assertEquals(0, CopperLanternRules.light(15, -160));
    }

    @Test
    void darkBlocksStayDark() {
        assertEquals(0, CopperLanternRules.light(0, -40));
        assertEquals(0, CopperLanternRules.light(-3, -40));
    }
}
