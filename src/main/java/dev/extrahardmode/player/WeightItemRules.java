package dev.extrahardmode.player;

/**
 * Per-item weight multipliers. Minecraft-free so JUnit can cover the table.
 * A heavy weapon or armor piece weighs 50% more than the usual piece.
 * Leather armor weighs half. Chainmail weighs 75%.
 */
public final class WeightItemRules {
    public static final double HEAVY = 1.5;
    public static final double LEATHER_ARMOR = 0.5;
    public static final double CHAINMAIL = 0.75;

    private WeightItemRules() {}

    /** {@code 1} for an ordinary item. Accepts a path or a namespaced id. */
    public static double multiplier(String itemId) {
        String path = path(itemId);
        if (heavyGear(path)) {
            return HEAVY;
        }
        if (leatherArmor(path)) {
            return LEATHER_ARMOR;
        }
        if (path.startsWith("chainmail_")) {
            return CHAINMAIL;
        }
        return 1.0;
    }

    /** {@code base} is the usual weight of that worn piece, tool, or stack portion. */
    public static double contribution(double base, String itemId) {
        return base * multiplier(itemId);
    }

    static String path(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return "";
        }
        int colon = itemId.indexOf(':');
        return colon >= 0 ? itemId.substring(colon + 1) : itemId;
    }

    private static boolean heavyGear(String path) {
        if (!path.startsWith("heavy_") || path.equals("heavy_core")) {
            return false;
        }
        return path.endsWith("_helmet")
                || path.endsWith("_chestplate")
                || path.endsWith("_leggings")
                || path.endsWith("_boots")
                || path.endsWith("_sword")
                || path.endsWith("_axe");
    }

    private static boolean leatherArmor(String path) {
        return path.equals("leather_helmet")
                || path.equals("leather_chestplate")
                || path.equals("leather_leggings")
                || path.equals("leather_boots");
    }
}
