package dev.extrahardmode.feature;

import java.util.List;
import java.util.Locale;

/**
 * Movement slowdown from worn armor, as a percent of the unarmored pace.
 * A full set is four pieces. Each worn piece applies one quarter of its material.
 * Heavy armor adds the configured extra percentage points to that material for a full set.
 * Minecraft-free so JUnit can cover the mapping.
 */
public final class ArmorSpeedRules {
    /** Vanilla walk speed. The movement attribute treats this as no modifier. */
    public static final double VANILLA_WALK_SPEED = 0.2;
    public static final int PIECES_IN_FULL_SET = 4;
    /** Full set of copper, iron, or gold. */
    public static final int DEFAULT_COPPER_IRON_GOLD_PERCENT = 10;
    /** Full set of diamond or netherite. */
    public static final int DEFAULT_DIAMOND_PERCENT = 5;
    /** Full set of leather or chainmail. */
    public static final int DEFAULT_LEATHER_CHAINMAIL_PERCENT = 0;
    /** Extra percentage points a full set of heavy armor adds to its material. */
    public static final int DEFAULT_HEAVY_EXTRA_PERCENT = 5;

    private ArmorSpeedRules() {}

    /**
     * Slowdown percent for one item id. Zero for anything that is not a worn armor piece
     * of leather, chainmail, copper, iron, gold, diamond, or netherite.
     */
    public static double piecePercent(
            String itemId, int copperIronGold, int diamond, int leatherChainmail, int heavyExtra) {
        String path = pathOf(itemId);
        if (path.isEmpty()) {
            return 0.0;
        }
        boolean heavy = path.startsWith("heavy_");
        if (heavy) {
            path = path.substring("heavy_".length());
        }
        Integer full = fullSetPercent(path, copperIronGold, diamond, leatherChainmail);
        if (full == null) {
            return 0.0;
        }
        int percent = full;
        if (heavy) {
            percent += heavyExtra;
        }
        return percent / (double) PIECES_IN_FULL_SET;
    }

    /** Sum of {@link #piecePercent} for the worn pieces. */
    public static double slowPercent(
            List<String> itemIds, int copperIronGold, int diamond, int leatherChainmail, int heavyExtra) {
        if (itemIds == null || itemIds.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (String itemId : itemIds) {
            sum += piecePercent(itemId, copperIronGold, diamond, leatherChainmail, heavyExtra);
        }
        return sum;
    }

    /**
     * {@code ADD_MULTIPLIED_BASE} amount. {@code baseSpeed} 0.22 with a slowdown of 0 is 10%
     * faster than vanilla. {@code slowPercent} removes that share of the unarmored pace.
     * A slowdown above 100 is treated as 100 so the pace stays at zero.
     */
    public static double modifierAmount(double baseSpeed, double slowPercent) {
        double fraction = slowPercent / 100.0;
        if (fraction > 1.0) {
            fraction = 1.0;
        }
        return (baseSpeed / VANILLA_WALK_SPEED) * (1.0 - fraction) - 1.0;
    }

    private static Integer fullSetPercent(String path, int copperIronGold, int diamond, int leatherChainmail) {
        String material = material(path);
        if (material == null) {
            return null;
        }
        return switch (material) {
            case "leather", "chainmail" -> leatherChainmail;
            case "copper", "iron", "golden", "gold" -> copperIronGold;
            case "diamond", "netherite" -> diamond;
            default -> null;
        };
    }

    /** Path after the namespace, or null when the id is not a helmet, chestplate, leggings, or boots. */
    private static String material(String path) {
        if (path.endsWith("_helmet")) {
            return path.substring(0, path.length() - "_helmet".length());
        }
        if (path.endsWith("_chestplate")) {
            return path.substring(0, path.length() - "_chestplate".length());
        }
        if (path.endsWith("_leggings")) {
            return path.substring(0, path.length() - "_leggings".length());
        }
        if (path.endsWith("_boots")) {
            return path.substring(0, path.length() - "_boots".length());
        }
        return null;
    }

    private static String pathOf(String itemId) {
        if (itemId == null) {
            return "";
        }
        String id = itemId.trim().toLowerCase(Locale.ROOT);
        int colon = id.indexOf(':');
        if (colon >= 0) {
            id = id.substring(colon + 1);
        }
        return id;
    }
}
