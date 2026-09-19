package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TorchLifetimeRulesTest {
    @Test
    void zeroDaysIsPermanent() {
        assertTrue(TorchLifetimeRules.permanent(0));
        assertTrue(TorchLifetimeRules.permanent(-4));
        assertFalse(TorchLifetimeRules.permanent(1));
        assertEquals(0, TorchLifetimeRules.clampDays(-1));
        assertEquals(7, TorchLifetimeRules.clampDays(7));
        assertEquals(TorchLifetimeRules.MAX_DAYS, TorchLifetimeRules.clampDays(TorchLifetimeRules.MAX_DAYS + 50));
    }

    @Test
    void sevenDaysIsOneHundredSixtyEightThousandTicks() {
        assertEquals(24000L, TorchLifetimeRules.lifetimeTicks(1));
        assertEquals(168000L, TorchLifetimeRules.lifetimeTicks(7));
        assertEquals(Long.MAX_VALUE, TorchLifetimeRules.lifetimeTicks(0));
    }

    @Test
    void unstampedAndPermanentNeverExpire() {
        assertFalse(TorchLifetimeRules.expired(-1L, 999_999L, 7));
        assertFalse(TorchLifetimeRules.expired(0L, Long.MAX_VALUE / 4, 0));
        assertFalse(TorchLifetimeRules.expired(100L, 100L + 23999L, 1));
        assertTrue(TorchLifetimeRules.expired(100L, 100L + 24000L, 1));
        assertTrue(TorchLifetimeRules.expired(0L, 168000L, 7));
        assertFalse(TorchLifetimeRules.expired(0L, 167999L, 7));
    }
}
