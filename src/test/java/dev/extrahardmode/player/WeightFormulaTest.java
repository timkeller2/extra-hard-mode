package dev.extrahardmode.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WeightFormulaTest {
    @Test
    void fullArmorEmptyInventoryIsEight() {
        assertEquals(8.0, WeightFormula.weight(4, 0, 0, 2.0, 1.0, 0.5), 1e-9);
    }

    @Test
    void stacksAreFractionalByMaxStack() {
        assertEquals(1.5, WeightFormula.weight(0, 1.5, 0, 2.0, 1.0, 0.5), 1e-9);
    }

    @Test
    void toolsAddHalfPointEach() {
        assertEquals(1.5, WeightFormula.weight(0, 0, 3, 2.0, 1.0, 0.5), 1e-9);
    }

    @Test
    void combinedFormulaMatchesRootNode() {
        // 4 armor * 2 + 10 stack units * 1 + 3 tools * 0.5 = 19.5
        assertEquals(19.5, WeightFormula.weight(4, 10.0, 3, 2.0, 1.0, 0.5), 1e-9);
    }

    @Test
    void drownRateIsBaseWhenAtCap() {
        assertEquals(35, WeightFormula.drownRatePercent(18.0, 18.0, 35, 2));
    }

    @Test
    void overencumbranceAddsTwoPerExtraPoint() {
        // 35 + 7 * 2 = 49
        assertEquals(49, WeightFormula.drownRatePercent(25.0, 18.0, 35, 2));
    }

    @Test
    void drownRateCapsAtOneHundred() {
        assertEquals(100, WeightFormula.drownRatePercent(100.0, 18.0, 35, 2));
    }
}
