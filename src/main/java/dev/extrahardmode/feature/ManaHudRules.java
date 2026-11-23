package dev.extrahardmode.feature;

/**
 * Heart-style mana crystal layout. Minecraft-free so JUnit can cover packing and fill.
 */
public final class ManaHudRules {
    public static final int CRYSTAL_SIZE = 9;
    /** Left slice of a half-capacity container. The HUD scissors to this width. */
    public static final int HALF_WIDTH = 5;
    /** One crystal is two mana. The fill and the faint mark use this many steps. */
    public static final int EIGHTHS = 8;
    public static final float MANA_PER_CRYSTAL = 2.0F;
    /**
     * Bottom pixel rows for 0 through 8 eighths. Four eighths is 5 rows, the old half sprite.
     */
    private static final int[] EIGHTH_HEIGHT = {0, 1, 2, 3, 5, 6, 7, 8, 9};
    public static final int PREFERRED_SPACING = 8;
    public static final int MIN_SPACING = 4;
    public static final int ROW_HEIGHT = 10;
    /** Same width as a vanilla heart row (10 hearts at 8px spacing). */
    public static final int ROW_WIDTH = 81;
    /** How far ahead the faint crystal mark looks, in Minecraft minutes. */
    public static final int PREVIEW_MINUTES = 10;
    /** White-tint alpha for {@code blitSprite}. The cyan sprite stays, drawn faintly. */
    public static final float PREVIEW_ALPHA = 0.4F;

    public record Layout(int spacing, int perRow, int rows) {
        public int height() {
            return rows <= 0 ? 0 : rows * ROW_HEIGHT;
        }
    }

    private ManaHudRules() {}

    /** Two mana points make one crystal, like two HP make one heart. The hard cap is 20 crystals. */
    public static int crystalCount(int manaLevel) {
        return crystalCount(manaLevel, manaLevel);
    }

    /**
     * Containers shown for both mana level and stored mana, so overflow above
     * your level is visible, up to {@link AchievementRules#MANA_HARD_CAP}.
     */
    public static int crystalCount(int manaLevel, double currentMana) {
        int level = Math.max(0, manaLevel);
        int fromCurrent = (int) Math.ceil(Math.max(0.0, currentMana));
        int pool = Math.max(level, fromCurrent);
        if (pool <= 0) {
            return 0;
        }
        int cap = Math.max((int) AchievementRules.MANA_HARD_CAP, level);
        return (Math.min(cap, pool) + 1) / 2;
    }

    /** Points represented by the crystals, after the same cap as {@link #crystalCount}. */
    public static int displayedPoints(int manaLevel, double currentMana) {
        int crystals = crystalCount(manaLevel, currentMana);
        if (crystals <= 0) {
            return 0;
        }
        int level = Math.max(0, manaLevel);
        int fromCurrent = (int) Math.ceil(Math.max(0.0, currentMana));
        int cap = Math.max((int) AchievementRules.MANA_HARD_CAP, level);
        return Math.min(cap, Math.max(level, fromCurrent));
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

    /** Eighths of this crystal that current mana fills, from 0 through 8. Each step is 0.25 mana. */
    public static int eighths(int crystalIndex, float currentMana) {
        if (crystalIndex < 0) {
            return 0;
        }
        float amount = currentMana - crystalIndex * MANA_PER_CRYSTAL;
        if (amount >= MANA_PER_CRYSTAL) {
            return EIGHTHS;
        }
        if (amount <= 0.0F) {
            return 0;
        }
        double step = MANA_PER_CRYSTAL / EIGHTHS;
        int filled = (int) Math.floor(amount / step + 1.0e-6);
        if (filled < 0) {
            return 0;
        }
        return Math.min(EIGHTHS - 1, filled);
    }

    /** Pixel rows from the bottom of the crystal for this many eighths. */
    public static int eighthHeight(int eighths) {
        if (eighths <= 0) {
            return 0;
        }
        if (eighths >= EIGHTHS) {
            return CRYSTAL_SIZE;
        }
        return EIGHTH_HEIGHT[eighths];
    }

    /**
     * Stored mana after {@link #PREVIEW_MINUTES} of the real regen tick.
     * Saturation and the Battery multiplier stay constant while the pool grows.
     * Mana level 0 stays at 0. The result never passes {@link AchievementRules#manaCap(int)}.
     */
    public static double previewMana(int manaLevel, double currentMana, double saturation, double batteryMultiplier) {
        if (manaLevel <= 0) {
            return 0.0;
        }
        double mana = Math.max(0.0, currentMana);
        double cap = AchievementRules.manaCap(manaLevel);
        if (mana >= cap) {
            return cap;
        }
        double multiplier = Math.max(0.0, batteryMultiplier);
        for (int minute = 0; minute < PREVIEW_MINUTES; minute++) {
            if (mana >= cap) {
                break;
            }
            double rate = AchievementRules.regenPerMinute(manaLevel, mana, multiplier);
            mana = AchievementRules.addMana(mana, manaLevel, AchievementRules.withSaturationRegen(rate, saturation));
        }
        return mana;
    }

    /**
     * Battery multiplies the preview only while it is still running and its power was synced.
     * A missing power stays at 1. Inactive Battery stays at 1 even if a power is passed.
     */
    public static double previewBatteryMultiplier(boolean durationActive, boolean powerKnown, double power) {
        if (!durationActive || !powerKnown) {
            return 1.0;
        }
        return AbilityRules.batteryRegenMultiplier(power);
    }

    /** Crystals to draw, including one the ten-minute mark opens past the solid row. */
    public static int drawnCrystalCount(int manaLevel, double currentMana, double previewMana) {
        return Math.max(crystalCount(manaLevel, currentMana), crystalCount(manaLevel, previewMana));
    }

    /**
     * Eighths this crystal will show after the preview, when that is more filled than now.
     * A gain that stays inside the current eighth draws nothing extra.
     */
    public static Integer previewEighths(int crystalIndex, double currentMana, double previewMana) {
        if (crystalIndex < 0) {
            return null;
        }
        double nowMana = Math.max(0.0, currentMana);
        double laterMana = Math.max(0.0, previewMana);
        if (laterMana <= nowMana) {
            return null;
        }
        int now = eighths(crystalIndex, (float) nowMana);
        int later = eighths(crystalIndex, (float) laterMana);
        if (later <= now) {
            return null;
        }
        return later;
    }
}
