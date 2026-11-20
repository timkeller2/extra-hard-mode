package dev.extrahardmode.feature;

/**
 * Mourning after a nearby villager dies. Minecraft-free for JUnit.
 * The end day is an overworld day index. Trading is allowed again once that day starts.
 */
public final class VillagerMourningRules {
    public static final int MOURNING_DAYS = 3;
    /** Euclidean range, in blocks, of villagers who mourn a death. */
    public static final int RANGE = 64;
    public static final double RANGE_SQR = (double) RANGE * RANGE;
    public static final String MESSAGE = "I am in a time of mourning for %s more days...";
    public static final String MESSAGE_ONE = "I am in a time of mourning for 1 more day...";

    private VillagerMourningRules() {}

    /** End day after this death. An open mourning period keeps its remaining time and gains 3 days. */
    public static long mournUntil(long today, long currentUntil) {
        long base = currentUntil > today ? currentUntil : today;
        long next = base + MOURNING_DAYS;
        if (next < base) {
            return Long.MAX_VALUE;
        }
        return next;
    }

    public static boolean mourning(long today, long until) {
        return until > today;
    }

    public static int daysLeft(long today, long until) {
        if (until <= today) {
            return 0;
        }
        long left = until - today;
        if (left > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) left;
    }

    public static boolean within(double distanceSqr) {
        return distanceSqr <= RANGE_SQR;
    }

    public static String message(int daysLeft) {
        if (daysLeft == 1) {
            return MESSAGE_ONE;
        }
        return MESSAGE.replace("%s", Integer.toString(Math.max(0, daysLeft)));
    }
}
