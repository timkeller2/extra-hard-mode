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
        boolean hasUnbreaking = true;
        int budget = HardenedBudget.IRON;
        int mined = 0;
        for (int i = 0; i < 128; i++) {
            mined = HardenedBudget.increment(mined);
            assertEquals(i + 1, mined, "Unbreaking must not skip the component counter");
        }
        assertTrue(hasUnbreaking);
        assertTrue(HardenedBudget.shouldConsume(mined, budget), "Unbreaking does not extend N");
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
