package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class HardenedBudgetTest {
    @Test
    void iron128thBreakConsumesPick() {
        int budget = HardenedBudget.IRON;
        int mined = 0;
        for (int i = 0; i < 127; i++) {
            mined = HardenedBudget.increment(mined);
            assertFalse(HardenedBudget.shouldConsume(mined, budget), "break " + mined + " must not consume");
        }
        mined = HardenedBudget.increment(mined);
        assertEquals(128, mined);
        assertTrue(HardenedBudget.shouldConsume(mined, budget), "128th hardened break consumes iron pick");
    }

    @Test
    void unbreakingDoesNotExtendN() {
        int unbreakingIii = 3;
        int budget = HardenedBudget.IRON;
        int mined = 0;
        HardenedBudget.BreakResult last = null;
        for (int i = 0; i < budget; i++) {
            last = HardenedBudget.afterHardenedBreak(mined, budget, unbreakingIii);
            mined = last.mined();
            assertEquals(i + 1, mined, "Unbreaking must not skip the component counter");
            if (i < budget - 1) {
                assertFalse(last.consume(), "Unbreaking III must not consume before N");
            }
        }
        assertEquals(128, mined);
        assertTrue(last.unbreakingSkippedVanillaDamage(), "Unbreaking III applied");
        assertTrue(last.consume(), "Unbreaking III still consumes the pick at N");
        assertEquals(
                HardenedBudget.shouldConsume(budget, budget, 0),
                HardenedBudget.shouldConsume(budget, budget, unbreakingIii));
    }

    @Test
    void copperBudgetIs97() {
        assertEquals(97, HardenedBudget.COPPER);
        assertEquals(97, Math.round(128 * 190 / 250.0));
        Map<String, Integer> parsed = HardenedBudget.parseAll(HardenedBudget.DEFAULT_ENTRIES);
        assertEquals(97, parsed.get("minecraft:copper_pickaxe"));
        assertEquals(128, parsed.get("minecraft:iron_pickaxe"));
        assertEquals(512, parsed.get("minecraft:diamond_pickaxe"));
        assertEquals(1024, parsed.get("minecraft:netherite_pickaxe"));
    }
}
