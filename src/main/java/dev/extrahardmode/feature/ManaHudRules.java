package dev.extrahardmode.feature;

/**
 * Heart-style mana crystal layout. Minecraft-free so JUnit can cover packing and fill.
 */
public final class ManaHudRules {
    public static final int CRYSTAL_SIZE = 9;
    /** Left slice of a half-capacity container. The HUD scissors to this width. */
    public static final int HALF_WIDTH = 5;
    public static final int PREFERRED_SPACING = 8;
    public static final int MIN_SPACING = 4;
    public static final int ROW_HEIGHT = 10;
    /** Same width as a vanilla heart row (10 hearts at 8px spacing). */
    public static final int ROW_WIDTH = 81;
    /** How far ahead the faint crystal mark looks, in Minecraft minutes. */
    public static final int PREVIEW_MINUTES = 10;
    /** White-tint alpha for {@code blitSprite}. The cyan sprite stays, drawn faintly. */
    public static final float PREVIEW_ALPHA = 0.4F;

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

    /** Ghost pixels on one crystal, {@code fromPixel} inclusive and {@code toPixel} exclusive. */
    public record PreviewSpan(int fromPixel, int toPixel) {
        public boolean visible() {
            return toPixel > fromPixel;
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

    /** Left-to-right pixels of one crystal. Two mana is the full 9px width. */
    public static int fillPixels(double manaInCrystal) {
        double clamped = Math.max(0.0, Math.min(2.0, manaInCrystal));
        return (int) Math.round(clamped / 2.0 * CRYSTAL_SIZE);
    }

    /** Future pixels only. A crystal that is already full through {@code previewMana} is empty. */
    public static PreviewSpan previewSpan(int crystalIndex, double currentMana, double previewMana) {
        if (crystalIndex < 0) {
            return new PreviewSpan(0, 0);
        }
        double origin = crystalIndex * 2.0;
        int from = fillPixels(currentMana - origin);
        int to = fillPixels(previewMana - origin);
        if (to <= from) {
            return new PreviewSpan(from, from);
        }
        return new PreviewSpan(from, to);
    }

    /** Clip a span to the container. A half container stops at {@link #HALF_WIDTH}. */
    public static PreviewSpan visibleSpan(PreviewSpan span, boolean halfCapacity) {
        int limit = halfCapacity ? HALF_WIDTH : CRYSTAL_SIZE;
        int from = Math.min(Math.max(0, span.fromPixel()), limit);
        int to = Math.min(Math.max(0, span.toPixel()), limit);
        if (to <= from) {
            return new PreviewSpan(from, from);
        }
        return new PreviewSpan(from, to);
    }
}
