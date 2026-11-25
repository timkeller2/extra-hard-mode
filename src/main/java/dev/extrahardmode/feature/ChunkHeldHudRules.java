package dev.extrahardmode.feature;

/**
 * Small diamond in the top-left corner of the HUD while the player stands in a cleared chunk.
 * Minecraft-free so the size and the hover box can be tested.
 */
public final class ChunkHeldHudRules {
    public static final int MARGIN = 4;
    /** Drawn size. The diamond item is 16 pixels and is scaled down to this. */
    public static final int SIZE = 8;

    private ChunkHeldHudRules() {}

    public static int width() {
        return SIZE;
    }

    public static int height() {
        return SIZE;
    }

    public static int x() {
        return MARGIN;
    }

    public static int y() {
        return MARGIN;
    }

    public static boolean hit(int mouseX, int mouseY) {
        return mouseX >= x() && mouseX < x() + width() && mouseY >= y() && mouseY < y() + height();
    }
}
