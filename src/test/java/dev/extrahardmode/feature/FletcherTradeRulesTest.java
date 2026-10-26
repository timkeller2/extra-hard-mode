package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FletcherTradeRulesTest {
    @Test
    void noviceStickBuyMatchesOnlyThirtyTwoSticksForOneEmerald() {
        assertTrue(FletcherTradeRules.isNoviceStickBuy(true, 32, false, true, 1));
        assertFalse(FletcherTradeRules.isNoviceStickBuy(true, 31, false, true, 1));
        assertFalse(FletcherTradeRules.isNoviceStickBuy(true, 16, false, true, 1));
        assertFalse(FletcherTradeRules.isNoviceStickBuy(true, 32, true, true, 1));
        assertFalse(FletcherTradeRules.isNoviceStickBuy(true, 32, false, true, 2));
        assertFalse(FletcherTradeRules.isNoviceStickBuy(true, 32, false, false, 1));
        assertFalse(FletcherTradeRules.isNoviceStickBuy(false, 32, false, true, 1));
        assertFalse(FletcherTradeRules.isNoviceStickBuy(false, 26, false, true, 1));
    }
}
