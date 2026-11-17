package dev.extrahardmode.feature;

/**
 * Mana crystal, Well Fed chicken, and enchanting skill on the survival inventory, above
 * the offhand slot. Minecraft-free. Mana and Well Fed are drawn only above 0. The crystal
 * is the lower icon. The chicken sits above it when both are showing. The enchantment
 * table sits above the chicken.
 */
public final class OffhandStatusIconRules {
    /** Panel-local top-left of the offhand item icon. */
    public static final int OFFHAND_X = 77;
    public static final int OFFHAND_Y = 62;
    /** Slot pitch. The first icon sits one slot above the offhand. */
    public static final int SLOT = 18;
    public static final int ICON = 16;
    /** Inventory numbers use this fraction of the GUI font, so they match. */
    public static final float NUMBER_SCALE = 0.5F;

    private OffhandStatusIconRules() {}

    /** Whole numbers stay whole. A value that would print as 0 stays hidden. */
    public static boolean showCapacity(double capacity) {
        return !"0".equals(InhabitantRules.learningLabel(capacity));
    }

    /** The Well Fed level. Health and hunger each rise by this much. */
    public static boolean showWellFed(int wellFed) {
        return wellFed > 0;
    }

    public static String capacityLabel(double capacity) {
        return InhabitantRules.learningLabel(capacity);
    }

    /** Stored mana level over learning capacity, such as {@code 3/7}. */
    public static String manaLabel(int baseManaLevel, double capacity) {
        return Math.max(0, baseManaLevel) + "/" + capacityLabel(capacity);
    }

    public static float numberScale(int firstWidth, int secondWidth) {
        return numberScale(firstWidth, secondWidth, 0);
    }

    /**
     * One scale for every inventory number. Half the GUI font, and smaller when the
     * widest measured width would still pass the icon. A hidden number passes width 0.
     */
    public static float numberScale(int firstWidth, int secondWidth, int thirdWidth) {
        int widest = Math.max(0, firstWidth);
        widest = Math.max(widest, Math.max(0, secondWidth));
        widest = Math.max(widest, Math.max(0, thirdWidth));
        if (widest <= 0) {
            return NUMBER_SCALE;
        }
        return Math.min(NUMBER_SCALE, (float) ICON / widest);
    }

    public static String wellFedLabel(int wellFed) {
        return Integer.toString(Math.max(0, wellFed));
    }

    /** Negative means the enchanting module is off, so the icon stays hidden. */
    public static boolean showEnchant(int enchantAbility) {
        return enchantAbility >= 0;
    }

    public static String enchantLabel(int enchantAbility) {
        return Integer.toString(Math.max(0, enchantAbility));
    }

    /** 0 is the slot above the offhand. 1 is the slot above that. */
    public static int rowY(int row) {
        return OFFHAND_Y - SLOT * (row + 1);
    }

    public static int iconX() {
        return OFFHAND_X;
    }

    /** Lower row when both show. -1 when mana learning is not shown. */
    public static int crystalRow(boolean capacity, boolean wellFed) {
        return capacity ? 0 : -1;
    }

    /** Upper row when both show, or the lower row when it is the only icon. -1 when Well Fed is 0. */
    public static int chickenRow(boolean capacity, boolean wellFed) {
        if (!wellFed) {
            return -1;
        }
        return capacity ? 1 : 0;
    }

    /**
     * The row above the chicken when Well Fed is showing, otherwise the next open row.
     * -1 when the enchanting module is off.
     */
    public static int enchantRow(boolean capacity, boolean wellFed, boolean enchant) {
        if (!enchant) {
            return -1;
        }
        int row = 0;
        if (capacity) {
            row++;
        }
        if (wellFed) {
            row++;
        }
        return row;
    }

    public static boolean hit(int iconX, int iconY, int localMouseX, int localMouseY) {
        return localMouseX >= iconX
                && localMouseX < iconX + ICON
                && localMouseY >= iconY
                && localMouseY < iconY + ICON;
    }
}
