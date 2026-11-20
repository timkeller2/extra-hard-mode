package dev.extrahardmode.feature;

/**
 * Losing a work block ends that villager's job. Minecraft-free for JUnit.
 * Replacing the block must not roll a new trade.
 */
public final class JobSiteLossRules {
    private JobSiteLossRules() {}

    /** True when the old block was a job site and the new block is not that same job site. */
    public static boolean workBlockLost(boolean oldIsJobSite, boolean newIsSameJobSite) {
        return oldIsJobSite && !newIsSameJobSite;
    }

    /** A resident, an unemployed villager, and a nitwit are left as they are. */
    public static boolean becomesNitwit(boolean resident, boolean unemployed, boolean alreadyNitwit) {
        return !resident && !unemployed && !alreadyNitwit;
    }
}
