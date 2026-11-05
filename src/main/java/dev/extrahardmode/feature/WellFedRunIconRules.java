package dev.extrahardmode.feature;

/**
 * Where the Well Fed reminder sits on the survival inventory. Minecraft-free.
 * The offhand slot is the shield slot in that screen.
 */
public final class WellFedRunIconRules {
    /** Panel-local top-left of the offhand item icon. */
    public static final int OFFHAND_X = 77;
    public static final int OFFHAND_Y = 62;
    /** Slot pitch. The reminder sits one slot above the offhand. */
    public static final int SLOT = 18;
    public static final int ICON = 16;

    private WellFedRunIconRules() {}

    public static int iconX() {
        return OFFHAND_X;
    }

    public static int iconY() {
        return OFFHAND_Y - SLOT;
    }

    public static boolean hit(int localMouseX, int localMouseY) {
        int x = iconX();
        int y = iconY();
        return localMouseX >= x && localMouseX < x + ICON && localMouseY >= y && localMouseY < y + ICON;
    }
}
