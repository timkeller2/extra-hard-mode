package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AntiFarmingTest {
    @Test
    void darkAlwaysDies() {
        assertEquals(100, AntiFarming.deathProbability(25, 9, false, true, 7));
        assertEquals(100, AntiFarming.deathProbability(25, 0, true, true, 0));
    }

    @Test
    void tendedCropUsesBaseLossRate() {
        assertEquals(25, AntiFarming.deathProbability(25, 15, false, true, 7));
    }

    @Test
    void unwateredAddsTwentyFive() {
        assertEquals(50, AntiFarming.deathProbability(25, 15, false, true, 0));
    }

    @Test
    void desertAddsFifty() {
        assertEquals(75, AntiFarming.deathProbability(25, 15, true, true, 7));
        assertEquals(25, AntiFarming.deathProbability(25, 15, true, false, 7));
    }

    @Test
    void desertAndDryCapsAtOneHundred() {
        assertEquals(100, AntiFarming.deathProbability(25, 15, true, true, 0));
    }
}
