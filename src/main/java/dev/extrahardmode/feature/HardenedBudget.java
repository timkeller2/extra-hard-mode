package dev.extrahardmode.feature;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

/**
 * Tool budgets for hardened stone. N is the expected lifetime in hardened breaks
 * with no Unbreaking: extra durability per break is {@code round(max / N) - 1}
 * on top of vanilla's 1. {@code ItemStack.hurtAndBreak} applies Unbreaking to
 * that extra damage.
 */
public final class HardenedBudget {
    public static final int COPPER = 3;
    public static final int IRON = 7;
    public static final int DIAMOND = 128;
    public static final int NETHERITE = 512;

    public static final List<String> DEFAULT_ENTRIES = List.of(
            "minecraft:copper_pickaxe@" + COPPER,
            "minecraft:iron_pickaxe@" + IRON,
            "minecraft:diamond_pickaxe@" + DIAMOND,
            "minecraft:netherite_pickaxe@" + NETHERITE);

    private HardenedBudget() {}

    /**
     * Extra durability applied after vanilla's 1-per-block so the tool lasts about
     * {@code budget} hardened breaks with no Unbreaking. Zero when the budget is
     * already at least the tool's max durability.
     */
    public static int extraDamage(int maxDamage, int budget) {
        if (maxDamage <= 0 || budget <= 0) {
            return 0;
        }
        int perBreak = Math.max(1, (int) Math.round(maxDamage / (double) budget));
        return Math.max(0, perBreak - 1);
    }

    /**
     * Vanilla tool Unbreaking: each durability point is actually taken with
     * probability {@code 1 / (level + 1)}.
     */
    public static double unbreakingKeepChance(int unbreakingLevel) {
        if (unbreakingLevel <= 0) {
            return 1.0;
        }
        return 1.0 / (unbreakingLevel + 1);
    }

    /**
     * Applies vanilla tool Unbreaking to {@code amount} durability points.
     * {@code nextIntExclusive} is {@code RandomSource.nextInt(bound)}.
     */
    public static int applyUnbreaking(int amount, int unbreakingLevel, IntUnaryOperator nextIntExclusive) {
        if (amount <= 0) {
            return 0;
        }
        if (unbreakingLevel <= 0) {
            return amount;
        }
        int taken = 0;
        int bound = unbreakingLevel + 1;
        for (int i = 0; i < amount; i++) {
            if (nextIntExclusive.applyAsInt(bound) == 0) {
                taken++;
            }
        }
        return taken;
    }

    public record Entry(String itemId, int budget) {}

    public static Entry parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("empty budget entry");
        }
        String[] parts = raw.split("@", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new IllegalArgumentException("budget entry must be item@N: " + raw);
        }
        String id = parts[0];
        if (id.indexOf(':') < 0) {
            id = "minecraft:" + id;
        }
        return new Entry(id, Integer.parseInt(parts[1].trim()));
    }

    public static Map<String, Integer> parseAll(List<String> entries) {
        Map<String, Integer> budgets = new LinkedHashMap<>();
        for (String entry : entries) {
            Entry parsed = parse(entry);
            budgets.put(parsed.itemId(), parsed.budget());
        }
        return budgets;
    }
}
