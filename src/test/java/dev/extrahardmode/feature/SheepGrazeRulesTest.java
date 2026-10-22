package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SheepGrazeRulesTest {
    @Test
    void defaultWaitIsSixTimesVanilla() {
        assertEquals(6, SheepGrazeRules.DEFAULT_MULTIPLIER);
        assertEquals(6000, SheepGrazeRules.eatRollBound(1000, 6));
        assertEquals(300, SheepGrazeRules.eatRollBound(50, 6));
    }

    @Test
    void oneIsVanillaAndSmallMultipliersDoNotShrink() {
        assertEquals(1000, SheepGrazeRules.eatRollBound(1000, 1));
        assertEquals(1000, SheepGrazeRules.eatRollBound(1000, 0));
        assertEquals(1000, SheepGrazeRules.eatRollBound(1000, -3));
    }
}
