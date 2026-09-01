package dev.extrahardmode.feature;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool budgets for hardened stone. N means N breaks; Unbreaking does not extend N.
 * Minecraft-free so JUnit can cover the 128th-break rule.
 */
public final class HardenedBudget {
    public static final int COPPER = Math.round(128 * 190 / 250.0f);
    public static final int IRON = 128;
    public static final int DIAMOND = 512;
    public static final int NETHERITE = 1024;

    public static final List<String> DEFAULT_ENTRIES = List.of(
            "copper_pickaxe@" + COPPER,
            "iron_pickaxe@" + IRON,
            "diamond_pickaxe@" + DIAMOND,
            "netherite_pickaxe@" + NETHERITE);

    private HardenedBudget() {}

    public static int increment(int mined) {
        return mined + 1;
    }

    /** True when the break that just happened consumes the tool. Unbreaking is ignored. */
    public static boolean shouldConsume(int minedInclusive, int budget) {
        return budget > 0 && minedInclusive >= budget;
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
