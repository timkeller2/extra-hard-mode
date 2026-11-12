package dev.extrahardmode.feature;

/**
 * Nether portal fuel and the per-player wait. Minecraft-free so JUnit can cover it.
 *
 * <p>One fuel kind covers 1 point of weight per item, two kinds cover 2, and all three cover 4.
 * Payment is {@link LapisPortalRules#pay}.
 */
public final class NetherPortalRules {
    /** Points of weight one item covers for one kind, two kinds, and all three. */
    public static final int ONE_KIND_WEIGHT = 1;
    public static final int TWO_KINDS_WEIGHT = 2;
    public static final int THREE_KINDS_WEIGHT = 4;
    public static final int COOLDOWN_SECONDS = 15;
    public static final int COOLDOWN_TICKS = COOLDOWN_SECONDS * 20;
    /** Do not repeat a denial line while the player is still standing in the frame. */
    public static final int MESSAGE_GAP_TICKS = 20;

    private NetherPortalRules() {}

    /** 1 point for one kind, 2 for two, and 4 for all three. No fuel still prices at 1. */
    public static int weightPerFuel(int kinds) {
        if (kinds >= 3) {
            return THREE_KINDS_WEIGHT;
        }
        if (kinds == 2) {
            return TWO_KINDS_WEIGHT;
        }
        return ONE_KIND_WEIGHT;
    }

    /**
     * {@code ceil(weight / weightPerFuel(kinds))}, and at least 1 when the weight is positive.
     * Zero weight is free.
     */
    public static int travelCost(double weight, int kinds) {
        if (weight <= 0.0) {
            return 0;
        }
        int cost = (int) Math.ceil(weight / (double) weightPerFuel(kinds));
        return Math.max(1, cost);
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
