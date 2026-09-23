package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExplosionFireRulesTest {
    @Test
    void igniteCountIsARoundedShare() {
        assertEquals(20, ExplosionFireRules.DEFAULT_TNT_FIRE_PERCENT);
        assertEquals(0, ExplosionFireRules.igniteCount(0, 20));
        assertEquals(0, ExplosionFireRules.igniteCount(10, 0));
        assertEquals(0, ExplosionFireRules.igniteCount(10, -5));
        assertEquals(0, ExplosionFireRules.igniteCount(2, 20));
        assertEquals(1, ExplosionFireRules.igniteCount(3, 20));
        assertEquals(1, ExplosionFireRules.igniteCount(7, 20));
        assertEquals(2, ExplosionFireRules.igniteCount(8, 20));
        assertEquals(2, ExplosionFireRules.igniteCount(10, 20));
        assertEquals(10, ExplosionFireRules.igniteCount(10, 100));
        assertEquals(10, ExplosionFireRules.igniteCount(10, 250));
    }

    @Test
    void chooseIndicesTakesADistinctShare() {
        List<Integer> chosen = ExplosionFireRules.chooseIndices(10, 2, bound -> 0);
        assertEquals(List.of(0, 1), chosen);
        List<Integer> tail = ExplosionFireRules.chooseIndices(5, 2, bound -> bound - 1);
        assertEquals(List.of(4, 0), tail);
        List<Integer> all = ExplosionFireRules.chooseIndices(4, 4, bound -> 0);
        assertEquals(4, new HashSet<>(all).size());
        assertTrue(ExplosionFireRules.chooseIndices(5, 0, bound -> 0).isEmpty());
    }
}
