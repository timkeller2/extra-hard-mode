package dev.extrahardmode.feature;

/**
 * Shared life pool. {@code player.death.lives} below 0 is off. 0 or above is on.
 * Each death spends one, and the count can go below 0. Below 0, base health falls.
 * A death tells the player the lives still left, or how many hearts they are down.
 * Minecraft-free so JUnit can cover the count.
 */
public final class LivesRules {
    /** Off. 0 or above turns the pool on. */
    public static final int DEFAULT_LIVES = -1;

    /** Vanilla player base max health, 10 hearts. */
    public static final double VANILLA_BASE_HEALTH = 20.0;

    /** Lowest base max health once lives are negative, 3 hearts. Armor still adds. */
    public static final double MIN_BASE_HEALTH = 6.0;

    /** A life can be bought back only while experience level is above this. */
    public static final int RECOVERY_LEVEL = 32;

    /** Experience for one missing life, added again for each life below full. */
    public static final int RECOVERY_XP = 250;

    public static final String LIVES_REMAINING = "You have %s lives remaining.";
    public static final String LIFE_REMAINING = "You have 1 life remaining.";
    public static final String HEARTS_DOWN = "You are down %s hearts.";
    public static final String HEART_DOWN = "You are down 1 heart.";

    private LivesRules() {}

    /** Any negative setting is off. 0 is on, and players start with no spare lives. */
    public static boolean enabled(int configured) {
        return configured >= 0;
    }

    /** Starting count. Call this only when {@link #enabled(int)} is true. */
    public static int starting(int configured) {
        return configured;
    }

    /** One death. The count is allowed to go negative. */
    public static int spend(int lives) {
        return lives - 1;
    }

    /**
     * Chat after a death. Above -1, the lives still left. Below 0, that many hearts,
     * which is the absolute value of the count. One life and one heart stay singular.
     */
    public static String deathMessage(int lives) {
        if (lives < 0) {
            int down = -lives;
            return down == 1 ? HEART_DOWN : HEARTS_DOWN.formatted(down);
        }
        if (lives == 1) {
            return LIFE_REMAINING;
        }
        return LIVES_REMAINING.formatted(lives);
    }

    /** A life can be given back while the count is still under the server setting. */
    public static boolean canRestore(int lives, int configured) {
        return enabled(configured) && lives < configured;
    }

    /** Level 33 and higher. Level 32 does not qualify. */
    public static boolean aboveRecoveryLevel(int experienceLevel) {
        return experienceLevel > RECOVERY_LEVEL;
    }

    /**
     * Experience for one recovered life: {@code 250 + missing * 250}, and never more
     * than {@code totalXp}. Zero when the pool is full, lives are off, or there is no experience.
     */
    public static int recoveryCost(int lives, int configured, int totalXp) {
        if (!canRestore(lives, configured) || totalXp <= 0) {
            return 0;
        }
        long missing = (long) configured - lives;
        long price = RECOVERY_XP + missing * RECOVERY_XP;
        if (price > Integer.MAX_VALUE) {
            price = Integer.MAX_VALUE;
        }
        return (int) Math.min(price, totalXp);
    }

    /**
     * Base max health. Off, or zero or more lives, keeps the vanilla 20.
     * Below 0 it is 20 plus 2 for each life, and at least {@link #MIN_BASE_HEALTH}.
     */
    public static double baseHealth(int configured, int lives) {
        if (!enabled(configured) || lives >= 0) {
            return VANILLA_BASE_HEALTH;
        }
        return Math.max(MIN_BASE_HEALTH, VANILLA_BASE_HEALTH + lives * 2.0);
    }
}
