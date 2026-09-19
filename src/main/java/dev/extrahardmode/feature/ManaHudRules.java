package dev.extrahardmode.feature;

/**
 * Heart-style mana crystal layout. Minecraft-free so JUnit can cover packing and fill.
 */
public final class ManaHudRules {
    public static final int CRYSTAL_SIZE = 9;
    public static final int PREFERRED_SPACING = 8;
    public static final int MIN_SPACING = 4;
    public static final int ROW_HEIGHT = 10;
    /** Same width as a vanilla heart row (10 hearts at 8px spacing). */
    public static final int ROW_WIDTH = 81;

    public enum Fill {
        EMPTY,
        HALF,
        FULL
    }

    public record Layout(int spacing, int perRow, int rows) {
        public int height() {
            return rows <= 0 ? 0 : rows * ROW_HEIGHT;
        }
    }

    private ManaHudRules() {}

    /** Two mana points make one crystal, like two HP make one heart. Capped at 10. */
    public static int crystalCount(int manaLevel) {
        return crystalCount(manaLevel, manaLevel);
    }

    /**
     * Containers shown for both mana level and stored mana, so overflow above
     * your level is visible, up to {@link AchievementRules#MANA_HARD_CAP}.
     */
    public static int crystalCount(int manaLevel, double currentMana) {
        int pool = Math.max(0, manaLevel);
        int fromCurrent = (int) Math.ceil(Math.max(0.0, currentMana));
        pool = Math.max(pool, fromCurrent);
        if (pool <= 0) {
            return 0;
        }
        int capped = Math.min((int) AchievementRules.MANA_HARD_CAP, pool);
        return (capped + 1) / 2;
    }

    public static Layout layout(int crystalCount) {
        if (crystalCount <= 0) {
            return new Layout(PREFERRED_SPACING, 0, 0);
        }
        int maxPreferred = 1 + (ROW_WIDTH - CRYSTAL_SIZE) / PREFERRED_SPACING;
        if (crystalCount <= maxPreferred) {
            return new Layout(PREFERRED_SPACING, maxPreferred, 1);
        }
        int spacingToFit = (ROW_WIDTH - CRYSTAL_SIZE) / (crystalCount - 1);
        if (spacingToFit >= MIN_SPACING) {
            return new Layout(spacingToFit, crystalCount, 1);
        }
        int perRow = 1 + (ROW_WIDTH - CRYSTAL_SIZE) / MIN_SPACING;
        int rows = (crystalCount + perRow - 1) / perRow;
        return new Layout(MIN_SPACING, perRow, rows);
    }

    public static int offsetX(int index, Layout layout) {
        if (layout.perRow() <= 0) {
            return 0;
        }
        return (index % layout.perRow()) * layout.spacing();
    }

    /** Rows grow upward, like extra heart rows. */
    public static int offsetY(int index, Layout layout) {
        if (layout.perRow() <= 0) {
            return 0;
        }
        return -(index / layout.perRow()) * ROW_HEIGHT;
    }

    /** Each crystal holds two mana. Half-filled = one mana level of current mana. */
    public static Fill fill(int crystalIndex, float currentMana) {
        float amount = currentMana - crystalIndex * 2.0F;
        if (amount >= 2.0F) {
            return Fill.FULL;
        }
        if (amount >= 1.0F) {
            return Fill.HALF;
        }
        return Fill.EMPTY;
    }
}
