package dev.extrahardmode.feature;

/**
 * Locator bar stays off until a golden carrot is eaten, then stays on only
 * while saturation remains. Players keep broadcasting. Someone sees them only
 * while that someone's own bar is on.
 */
public final class LocatorBarRules {
    public static final String GOLDEN_CARROT_ID = "minecraft:golden_carrot";

    private LocatorBarRules() {}

    public static boolean isGoldenCarrot(String itemId) {
        return GOLDEN_CARROT_ID.equals(itemId);
    }

    /**
     * Armed after a golden carrot until saturation is gone. Other food does not
     * start it, and saturation from earlier food keeps it going once started.
     */
    public static boolean armed(boolean alreadyArmed, boolean ateGoldenCarrot, float saturation) {
        if (saturation <= 0.0F) {
            return false;
        }
        return alreadyArmed || ateGoldenCarrot;
    }

    /** See other players, including others whose bar is also on. */
    public static boolean receives(boolean armed) {
        return armed;
    }
}
