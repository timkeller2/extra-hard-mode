package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class HardenedBudgetTest {
    @Test
    void extraDamageMatchesBudgets() {
        assertEquals(35, HardenedBudget.extraDamage(250, HardenedBudget.IRON));
        assertEquals(11, HardenedBudget.extraDamage(1561, HardenedBudget.DIAMOND));
        assertEquals(3, HardenedBudget.extraDamage(2031, HardenedBudget.NETHERITE));
        assertEquals(62, HardenedBudget.extraDamage(190, HardenedBudget.COPPER));
    }

    @Test
    void extraDamageIsZeroWhenBudgetCoversVanillaDurability() {
        assertEquals(0, HardenedBudget.extraDamage(3, HardenedBudget.COPPER));
        assertEquals(0, HardenedBudget.extraDamage(250, 0));
        assertEquals(0, HardenedBudget.extraDamage(0, HardenedBudget.IRON));
    }

    @Test
    void unbreakingCanSkipExtraDamage() {
        assertEquals(1.0, HardenedBudget.unbreakingKeepChance(0));
        assertEquals(0.5, HardenedBudget.unbreakingKeepChance(1));
        assertEquals(0.25, HardenedBudget.unbreakingKeepChance(3));
        assertEquals(1, HardenedBudget.applyUnbreaking(1, 3, bound -> 0));
        assertEquals(0, HardenedBudget.applyUnbreaking(1, 3, bound -> 1));
        assertEquals(2, HardenedBudget.applyUnbreaking(2, 0, bound -> 1));
        int extra = HardenedBudget.extraDamage(250, HardenedBudget.IRON);
        assertEquals(0, HardenedBudget.applyUnbreaking(extra, 3, bound -> 2));
        assertTrue(extra > 0);
    }

    @Test
    void defaultBudgetsMatchLiveOverworld() {
        assertEquals(3, HardenedBudget.COPPER);
        assertEquals(7, HardenedBudget.IRON);
        assertEquals(128, HardenedBudget.DIAMOND);
        assertEquals(512, HardenedBudget.NETHERITE);
        Map<String, Integer> parsed = HardenedBudget.parseAll(HardenedBudget.DEFAULT_ENTRIES);
        assertEquals(3, parsed.get("minecraft:copper_pickaxe"));
        assertEquals(7, parsed.get("minecraft:iron_pickaxe"));
        assertEquals(128, parsed.get("minecraft:diamond_pickaxe"));
        assertEquals(512, parsed.get("minecraft:netherite_pickaxe"));
    }
}
