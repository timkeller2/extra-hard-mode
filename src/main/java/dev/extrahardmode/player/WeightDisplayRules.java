package dev.extrahardmode.player;

import java.util.Locale;

/**
 * Inventory-screen weight label. Minecraft-free so the bands and the text can be tested.
 *
 * <p>The line is drawn in inventory-panel coordinates, on the row under the 2×2 crafting grid
 * and above the recipe-book button, at three-quarter size. It is right-aligned and may start as
 * far left as the player preview. At or under the swim limit is green. Less than {@link #SEVERAL_POINTS} over is yellow.
 * That many points over, or more, is red.
 */
public final class WeightDisplayRules {
    /** "Several points" above the swim limit. */
    public static final double SEVERAL_POINTS = 3.0;
    public static final int GREEN = 0xFF55FF55;
    public static final int YELLOW = 0xFFFFFF55;
    public static final int RED = 0xFFFF5555;

    /** Right edge of the text inside the 176-wide inventory panel. */
    public static final int TEXT_RIGHT = 169;
    /** Top of the line just under the crafting grid. */
    public static final int TEXT_Y = 54;
    /** Left of the player preview. Wider numbers stop here. */
    public static final int TEXT_MIN_X = 76;
    /** A little under the normal GUI font, so the line clears the slots and the recipe button. */
    public static final float TEXT_SCALE = 0.75F;

    public enum Band {
        GREEN,
        YELLOW,
        RED,
        HIDDEN
    }

    private WeightDisplayRules() {}

    public static Band band(boolean shown, double weight, double maxPoints) {
        if (!shown) {
            return Band.HIDDEN;
        }
        double over = weight - maxPoints;
        if (over <= 0.0) {
            return Band.GREEN;
        }
        if (over < SEVERAL_POINTS) {
            return Band.YELLOW;
        }
        return Band.RED;
    }

    public static int color(Band band) {
        return switch (band) {
            case GREEN -> GREEN;
            case YELLOW -> YELLOW;
            case RED -> RED;
            case HIDDEN -> 0;
        };
    }

    /** {@code Weight 12.5/18}. Whole numbers drop the decimal. */
    public static String label(double weight, double maxPoints) {
        return "Weight " + formatPoints(weight) + "/" + formatPoints(maxPoints);
    }

    /** {@code textWidth} is the unscaled font width. The result is the panel x of the scaled line. */
    public static int x(int textWidth) {
        int scaledWidth = Math.round(textWidth * TEXT_SCALE);
        return Math.max(TEXT_MIN_X, TEXT_RIGHT - scaledWidth);
    }

    static String formatPoints(double value) {
        if (!Double.isFinite(value)) {
            return "0";
        }
        long tenths = Math.round(value * 10.0);
        long whole = Math.round(value);
        if (tenths == whole * 10L) {
            return Long.toString(whole);
        }
        return String.format(Locale.ROOT, "%.1f", tenths / 10.0);
    }
}
