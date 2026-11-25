package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GoldBeamRulesTest {
    @Test
    void beamStartsAtTheCasterAndEndsAtTheTarget() {
        double[][] points = GoldBeamRules.points(1.0, 2.0, 3.0, 9.0, 4.0, -5.0);
        assertTrue(points.length >= 2);
        assertEquals(1.0, points[0][0], 1e-9);
        assertEquals(2.0, points[0][1], 1e-9);
        assertEquals(3.0, points[0][2], 1e-9);
        double[] end = points[points.length - 1];
        assertEquals(9.0, end[0], 1e-9);
        assertEquals(4.0, end[1], 1e-9);
        assertEquals(-5.0, end[2], 1e-9);
        for (double[] point : points) {
            assertEquals(
                    0.0,
                    LightningBoltRules.distanceToSegment(
                            point[0], point[1], point[2], 1.0, 2.0, 3.0, 9.0, 4.0, -5.0),
                    1e-9);
        }
    }

    @Test
    void aZeroLengthBeamIsThePointItself() {
        double[][] points = GoldBeamRules.points(4.0, 5.0, 6.0, 4.0, 5.0, 6.0);
        assertEquals(1, points.length);
        assertEquals(4.0, points[0][0], 1e-9);
        assertEquals(5.0, points[0][1], 1e-9);
        assertEquals(6.0, points[0][2], 1e-9);
        assertEquals(0, GoldBeamRules.segments(0.0));
        assertEquals(GoldBeamRules.MAX_SEGMENTS, GoldBeamRules.segments(10_000.0));
    }
}
