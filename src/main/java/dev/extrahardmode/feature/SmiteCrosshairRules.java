package dev.extrahardmode.feature;

/**
 * When the crosshair turns gold for Smite Evil. The cast itself costs
 * {@link AbilityRules#SMITE_EVIL_MANA_COST}; the marker waits for a full mana.
 */
public final class SmiteCrosshairRules {
    /** Mana the player must already be holding before the crosshair turns gold. */
    public static final double MANA_REQUIRED = 1.0;
    /** Vanilla crosshair blit color. */
    public static final int WHITE = -1;
    /** Same gold as a learned ability level. */
    public static final int GOLD = AbilityReadyRules.LEARNED_LEVEL_COLOR;

    private SmiteCrosshairRules() {}

    /**
     * {@code creatureOnCrosshair} is the living thing the look ray hits before a block,
     * inside the ability's reach. A creature merely near the aim does not count.
     */
    public static boolean highlight(
            boolean holdingGoldenSword, double currentMana, double range, boolean creatureOnCrosshair) {
        return holdingGoldenSword && currentMana >= MANA_REQUIRED && range > 0.0 && creatureOnCrosshair;
    }

    public static int color(boolean highlight) {
        return highlight ? GOLD : WHITE;
    }
}
