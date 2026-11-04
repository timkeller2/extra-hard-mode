package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ManaSparkleRulesTest {
    @Test
    void sparklesOnlyAboveThreeAndMatchWholeMana() {
        assertEquals(0, ManaSparkleRules.sparkleCount(0));
        assertEquals(0, ManaSparkleRules.sparkleCount(1));
        assertEquals(0, ManaSparkleRules.sparkleCount(2));
        assertEquals(0, ManaSparkleRules.sparkleCount(3));
        assertEquals(3, ManaSparkleRules.sparkleCount(3.9));
        assertEquals(4, ManaSparkleRules.sparkleCount(4));
        assertEquals(4, ManaSparkleRules.sparkleCount(4.9));
        assertEquals(10, ManaSparkleRules.sparkleCount(10.2));
    }

    @Test
    void gapAndBurstStayInsideAQuietRange() {
        assertEquals(600, ManaSparkleRules.gapTicks(0));
        assertEquals(1200, ManaSparkleRules.gapTicks(600));
        assertEquals(600, ManaSparkleRules.gapTicks(601));
        assertEquals(20, ManaSparkleRules.burstTicks(0));
        assertEquals(40, ManaSparkleRules.burstTicks(20));
        assertEquals(20, ManaSparkleRules.burstTicks(21));
        assertTrue(ManaSparkleRules.gapTicks(-3) >= 600);
        assertTrue(ManaSparkleRules.gapTicks(-3) <= 1200);
        assertTrue(ManaSparkleRules.burstTicks(-7) >= 20);
        assertTrue(ManaSparkleRules.burstTicks(-7) <= 40);
        assertTrue(ManaSparkleRules.gapTicks(Integer.MIN_VALUE) >= 600);
        assertTrue(ManaSparkleRules.gapTicks(Integer.MIN_VALUE) <= 1200);
        assertTrue(ManaSparkleRules.burstTicks(Integer.MIN_VALUE) >= 20);
        assertTrue(ManaSparkleRules.burstTicks(Integer.MIN_VALUE) <= 40);
    }

    @Test
    void aBurstEmitsOneSparklePerPoint() {
        assertEquals(4, emitted(4, 40));
        assertEquals(4, emitted(4, 20));
        assertEquals(40, emitted(40, 20));
        assertEquals(1, emitted(1, 30));
        assertEquals(0, emitted(0, 30));
        assertEquals(0, ManaSparkleRules.sparklesOnTick(4, 20, -1));
        assertEquals(0, ManaSparkleRules.sparklesOnTick(4, 20, 20));
    }

    private static int emitted(int total, int burstTicks) {
        int sum = 0;
        for (int tick = 0; tick < burstTicks; tick++) {
            int count = ManaSparkleRules.sparklesOnTick(total, burstTicks, tick);
            assertTrue(count >= 0, "tick " + tick);
            sum += count;
        }
        return sum;
    }
}
