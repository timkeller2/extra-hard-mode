package dev.extrahardmode.feature.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PigMenTest {
    @Test
    void lightningCountIsSixtyTwentyTwenty() {
        assertEquals(2, PigMen.lightningCount(0));
        assertEquals(2, PigMen.lightningCount(1));
        assertEquals(3, PigMen.lightningCount(2));
        assertEquals(3, PigMen.lightningCount(3));
        assertEquals(1, PigMen.lightningCount(4));
        assertEquals(1, PigMen.lightningCount(9));
    }

    @Test
    void playerDamageScalesByPercent() {
        assertEquals(7.0F, PigMen.scalePlayerDamage(10.0F, 70), 0.0001F);
        assertEquals(10.0F, PigMen.scalePlayerDamage(10.0F, 100), 0.0001F);
        assertEquals(10.0F, PigMen.scalePlayerDamage(10.0F, 0), 0.0001F);
    }
}
