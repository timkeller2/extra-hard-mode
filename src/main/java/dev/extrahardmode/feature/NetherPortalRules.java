package dev.extrahardmode.feature;

/**
 * Nether portal fuel and the per-player wait. Minecraft-free so JUnit can cover it.
 *
 * <p>The weight being carried replaces the lapis portal's distance. One fuel kind covers
 * {@link LapisPortalRules#BLOCKS_PER_FUEL} points of weight per item, two kinds cover twice
 * that, and all three cover three times that. Payment is {@link LapisPortalRules#pay}.
 */
public final class NetherPortalRules {
    public static final int COOLDOWN_SECONDS = 15;
    public static final int COOLDOWN_TICKS = COOLDOWN_SECONDS * 20;
    /** Do not repeat a denial line while the player is still standing in the frame. */
    public static final int MESSAGE_GAP_TICKS = 20;

    private NetherPortalRules() {}

    /**
     * {@link LapisPortalRules#travelCost} with weight in place of distance. Zero weight is free.
     * A positive weight costs at least one item.
     */
    public static int travelCost(double weight, int kinds) {
        return LapisPortalRules.travelCost(weight, kinds);
    }

    /** Game time when this player may take a nether portal again. */
    public static long readyAt(long now) {
        return now + COOLDOWN_TICKS;
    }

    /** Whole seconds still to wait, rounded up. Zero when the player may travel. */
    public static int secondsLeft(long readyAt, long now) {
        return LapisPortalRules.secondsLeft(readyAt, now);
    }

    public static String waitPhrase(int seconds) {
        return LapisPortalRules.rechargeWait(seconds);
    }

    /** True when a denial was just told and another line would only repeat it. */
    public static boolean repeatTooSoon(long toldAt, long now) {
        return toldAt > 0L && now >= toldAt && now - toldAt < MESSAGE_GAP_TICKS;
    }
}
