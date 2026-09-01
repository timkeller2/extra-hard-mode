package dev.extrahardmode.feature.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GhastsTest {
    @Test
    void arrowsDealTwentyPercent() {
        assertEquals(2.0F, Ghasts.scaleArrowDamage(10.0F, 20), 0.0001F);
        assertEquals(10.0F, Ghasts.scaleArrowDamage(10.0F, 100), 0.0001F);
        assertEquals(0.0F, Ghasts.scaleArrowDamage(10.0F, 0), 0.0001F);
    }

    @Test
    void expAndDropsMultipliers() {
        assertEquals(50, Ghasts.scaleExperience(5, 10));
        assertEquals(5, Ghasts.scaleExperience(5, 1));
        assertEquals(0, Ghasts.scaleExperience(5, 0));
    }
}
