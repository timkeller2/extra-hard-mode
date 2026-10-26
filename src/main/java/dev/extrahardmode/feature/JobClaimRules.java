package dev.extrahardmode.feature;

/**
 * An unemployed villager cannot take a job once that job already has the rounded-up
 * eighth of the villagers within {@link #RADIUS} blocks. One librarian among 8 is full.
 * A ninth villager raises the cap to 2.
 */
public final class JobClaimRules {
    public static final int RADIUS = 64;
    public static final int SHARE = 8;

    private JobClaimRules() {}

    /** Ceiling of {@code villagers / 8}. Zero villagers has no room. */
    public static int cap(int villagers) {
        if (villagers <= 0) {
            return 0;
        }
        return (villagers + SHARE - 1) / SHARE;
    }

    public static boolean claimBlocked(int alreadyThisJob, int villagers) {
        int already = Math.max(0, alreadyThisJob);
        return already >= cap(villagers);
    }

    public static boolean within(double distanceSquared) {
        double limit = (double) RADIUS * RADIUS;
        return distanceSquared <= limit;
    }

    /** Nitwits and the unemployed have no job site. */
    public static boolean isClaimedJob(boolean none, boolean nitwit) {
        return !none && !nitwit;
    }
}
