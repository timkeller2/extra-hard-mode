package dev.extrahardmode.feature;

/**
 * Yellow saturation outline on the hunger icons. Icon 0 is the rightmost drumstick.
 * Minecraft-free.
 */
public final class SaturationOutlineRules {
    public static final int ICON_SIZE = 9;
    public static final int ICON_STEP = 8;
    public static final int ICONS_PER_ROW = 10;
    public static final int ROW_HEIGHT = 10;
    /** Same gold as a learned ability level. */
    public static final int GOLD = 0xFFFFAA00;

    private SaturationOutlineRules() {}

    /**
     * How much of this icon the outline covers: 0, 0.25, 0.5, 0.75, or 1.
     * A full icon is 2 saturation. The right side fills first.
     */
    public static float coverage(float saturation, int index) {
        if (index < 0 || !(saturation > 0.0F)) {
            return 0.0F;
        }
        float covered = saturation / 2.0F - index;
        if (covered >= 1.0F) {
            return 1.0F;
        }
        if (covered > 0.5F) {
            return 0.75F;
        }
        if (covered > 0.25F) {
            return 0.5F;
        }
        if (covered > 0.0F) {
            return 0.25F;
        }
        return 0.0F;
    }

    /** Right-hand pixels of the 9-pixel icon that the outline uses. 0 draws nothing. */
    public static int clipWidth(float coverage) {
        if (coverage >= 1.0F) {
            return ICON_SIZE;
        }
        if (coverage >= 0.75F) {
            return 7;
        }
        if (coverage >= 0.5F) {
            return 5;
        }
        if (coverage > 0.0F) {
            return 3;
        }
        return 0;
    }

    public static int iconX(int right, int index) {
        return right - (index % ICONS_PER_ROW) * ICON_STEP - ICON_SIZE;
    }

    public static int iconY(int top, int index) {
        return top - (index / ICONS_PER_ROW) * ROW_HEIGHT;
    }
}
