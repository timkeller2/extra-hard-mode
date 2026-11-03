package dev.extrahardmode.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WeightDisplayRulesTest {
    @Test
    void atOrUnderTheLimitIsGreen() {
        assertEquals(WeightDisplayRules.Band.GREEN, WeightDisplayRules.band(true, 0.0, 18.0));
        assertEquals(WeightDisplayRules.Band.GREEN, WeightDisplayRules.band(true, 17.5, 18.0));
        assertEquals(WeightDisplayRules.Band.GREEN, WeightDisplayRules.band(true, 18.0, 18.0));
    }

    @Test
    void slightlyOverIsYellow() {
        assertEquals(WeightDisplayRules.Band.YELLOW, WeightDisplayRules.band(true, 18.5, 18.0));
        assertEquals(WeightDisplayRules.Band.YELLOW, WeightDisplayRules.band(true, 20.9, 18.0));
    }

    @Test
    void severalPointsOverIsRed() {
        assertEquals(WeightDisplayRules.Band.RED, WeightDisplayRules.band(true, 21.0, 18.0));
        assertEquals(WeightDisplayRules.Band.RED, WeightDisplayRules.band(true, 25.0, 18.0));
    }

    @Test
    void hiddenWhenWeightIsOff() {
        assertEquals(WeightDisplayRules.Band.HIDDEN, WeightDisplayRules.band(false, 30.0, 18.0));
        assertEquals(0, WeightDisplayRules.color(WeightDisplayRules.Band.HIDDEN));
    }

    @Test
    void colorsMatchTheChatGreensYellowAndRed() {
        assertEquals(0xFF55FF55, WeightDisplayRules.color(WeightDisplayRules.Band.GREEN));
        assertEquals(0xFFFFFF55, WeightDisplayRules.color(WeightDisplayRules.Band.YELLOW));
        assertEquals(0xFFFF5555, WeightDisplayRules.color(WeightDisplayRules.Band.RED));
    }

    @Test
    void labelDropsATrailingZero() {
        assertEquals("Weight 8/18", WeightDisplayRules.label(8.0, 18.0));
        assertEquals("Weight 12.5/18", WeightDisplayRules.label(12.5, 18.0));
        assertEquals("Weight 10.5/20", WeightDisplayRules.label(10.5, 20.0));
        assertEquals("Weight 0/18", WeightDisplayRules.label(0.04, 18.0));
    }

    @Test
    void lineSitsUnderTheCraftingGrid() {
        assertEquals(54, WeightDisplayRules.TEXT_Y);
        assertEquals(0.75F, WeightDisplayRules.TEXT_SCALE);
        assertEquals(110, WeightDisplayRules.x(79));
        assertEquals(76, WeightDisplayRules.x(200));
    }
}
