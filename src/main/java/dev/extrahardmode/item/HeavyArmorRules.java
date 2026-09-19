package dev.extrahardmode.item;

/**
 * Extra max-health hearts on heavy armor. Minecraft-free so JUnit can cover the table.
 * One heart is 2 health points. Full-set totals: copper 5, iron 10, diamond 20,
 * netherite 30 hearts.
 */
public final class HeavyArmorRules {
    /** Each piece lasts this many times as long as the matching vanilla armor. */
    public static final int DURABILITY_MULTIPLIER = 2;

    private static final String[] SLOTS = {"helmet", "chestplate", "leggings", "boots"};

    private HeavyArmorRules() {}

    public static int durability(int vanilla) {
        return vanilla * DURABILITY_MULTIPLIER;
    }

    public static double bonusHearts(String material, String slot) {
        if (material == null || slot == null) {
            return 0;
        }
        return switch (material) {
            case "copper" -> switch (slot) {
                case "chestplate" -> 2;
                case "leggings" -> 1.5;
                case "helmet" -> 1;
                case "boots" -> 0.5;
                default -> 0;
            };
            case "iron" -> switch (slot) {
                case "chestplate" -> 4;
                case "leggings" -> 3;
                case "helmet" -> 2;
                case "boots" -> 1;
                default -> 0;
            };
            case "diamond" -> switch (slot) {
                case "chestplate" -> 8;
                case "leggings" -> 6;
                case "helmet" -> 4;
                case "boots" -> 2;
                default -> 0;
            };
            case "netherite" -> switch (slot) {
                case "chestplate" -> 12;
                case "leggings" -> 9;
                case "helmet" -> 6;
                case "boots" -> 3;
                default -> 0;
            };
            default -> 0;
        };
    }

    public static double bonusHealth(String material, String slot) {
        return bonusHearts(material, slot) * 2.0;
    }

    public static double setHearts(String material) {
        double total = 0;
        for (String slot : SLOTS) {
            total += bonusHearts(material, slot);
        }
        return total;
    }

    public static double setHealth(String material) {
        return setHearts(material) * 2.0;
    }
}
