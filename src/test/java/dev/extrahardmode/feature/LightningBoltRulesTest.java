package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LightningBoltRulesTest {
    @Test
    void boltStartsAtTheCasterAndEndsAtTheTarget() {
        double[][] points = LightningBoltRules.points(1.0, 2.0, 3.0, 9.0, 4.0, -5.0, 3);
        assertTrue(points.length >= 2);
        assertEquals(1.0, points[0][0], 1e-9);
        assertEquals(2.0, points[0][1], 1e-9);
        assertEquals(3.0, points[0][2], 1e-9);
        double[] end = points[points.length - 1];
        assertEquals(9.0, end[0], 1e-9);
        assertEquals(4.0, end[1], 1e-9);
        assertEquals(-5.0, end[2], 1e-9);
        double reach = LightningBoltRules.JAG * Math.sqrt(2.0) + 1e-6;
        for (double[] point : points) {
            assertTrue(LightningBoltRules.distanceToSegment(
                            point[0], point[1], point[2], 1.0, 2.0, 3.0, 9.0, 4.0, -5.0)
                    <= reach);
        }
    }

    @Test
    void twoBoltsToOneTargetDoNotShareTheSameJag() {
        double[][] first = LightningBoltRules.points(0.0, 1.5, 0.0, 8.0, 1.0, 0.0, 1);
        double[][] second = LightningBoltRules.points(0.0, 1.5, 0.0, 8.0, 1.0, 0.0, 2);
        assertEquals(first.length, second.length);
        boolean differs = false;
        for (int i = 1; i < first.length - 1; i++) {
            if (Math.abs(first[i][0] - second[i][0]) > 1e-6
                    || Math.abs(first[i][1] - second[i][1]) > 1e-6
                    || Math.abs(first[i][2] - second[i][2]) > 1e-6) {
                differs = true;
                break;
            }
        }
        assertTrue(differs);
    }

    @Test
    void aZeroLengthBoltIsThePointItself() {
        double[][] points = LightningBoltRules.points(4.0, 5.0, 6.0, 4.0, 5.0, 6.0, 9);
        assertEquals(1, points.length);
        assertEquals(4.0, points[0][0], 1e-9);
        assertEquals(5.0, points[0][1], 1e-9);
        assertEquals(6.0, points[0][2], 1e-9);
        assertEquals(0, LightningBoltRules.segments(0.0));
        assertEquals(LightningBoltRules.MAX_SEGMENTS, LightningBoltRules.segments(10_000.0));
    }
}
