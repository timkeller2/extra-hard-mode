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
    public static final String STILL_NEED_ONE = "You still need 1 life to reach full lives.";
    public static final String STILL_NEED = "You still need %s lives to reach full lives.";
    public static final String HAVE_ONE = "You have 1 life.";
    public static final String HAVE_MANY = "You have %s lives.";
    public static final String NAMED_NEED_ONE = "%s still needs 1 life to reach full lives.";
    public static final String NAMED_NEED = "%s still needs %s lives to reach full lives.";
    public static final String NAMED_HAS_ONE = "%s has 1 life.";
    public static final String NAMED_HAS = "%s has %s lives.";

    private LivesRules() {}

    /** Any negative setting is off. 0 is on, and players start with no spare lives. */
    public static boolean enabled(int configured) {
        return configured >= 0;
    }

    /** Starting count. Call this only when {@link #enabled(int)} is true. */
    public static int starting(int configured) {
        return configured;
    }

    /** Vanilla hunger bar. Full saturation is this high, because saturation cannot pass hunger. */
    public static final int FULL_FOOD = 20;

    /**
     * Health after a death. Lives on fills the player's current max, including a max
     * already lowered by lives below 0. Lives off uses {@code percent} of that max.
     */
    public static float respawnHealth(boolean livesOn, float maxHealth, int percent) {
        float max = Math.max(0.0F, maxHealth);
        if (livesOn) {
            return max;
        }
        return max * percent / 100.0F;
    }

    /**
     * Hunger after a death. Lives on fills the bar. Lives off uses the configured
     * food, and it cannot pass {@code foodMax}.
     */
    public static int respawnFood(boolean livesOn, int configuredFood, int foodMax) {
        int max = Math.max(0, foodMax);
        if (livesOn) {
            return max;
        }
        if (configuredFood < 0) {
            return 0;
        }
        return Math.min(configuredFood, max);
    }

    /** Saturation that fills a hunger bar. It cannot rise above the food. */
    public static float fullSaturation(int food) {
        return Math.max(0, food);
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

    /** Lives still short of the configured full count. Zero when the count is already there. */
    public static int stillNeeded(int lives, int configured) {
        long gap = (long) configured - lives;
        if (gap <= 0L) {
            return 0;
        }
        return gap > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) gap;
    }

    /**
     * After a life comes back. An empty {@code name} speaks to the player who gained it.
     * A name speaks about that player. The total is included only when it is above 0.
     */
    public static String recoveryNotice(int lives, int configured) {
        return recoveryNotice(lives, configured, null);
    }

    public static String recoveryNotice(int lives, int configured, String name) {
        int needed = stillNeeded(lives, configured);
        boolean named = name != null && !name.isEmpty();
        String notice;
        if (!named) {
            notice = needed == 1 ? STILL_NEED_ONE : STILL_NEED.formatted(needed);
        } else if (needed == 1) {
            notice = NAMED_NEED_ONE.formatted(name);
        } else {
            notice = NAMED_NEED.formatted(name, needed);
        }
        if (lives <= 0) {
            return notice;
        }
        String total;
        if (!named) {
            total = lives == 1 ? HAVE_ONE : HAVE_MANY.formatted(lives);
        } else if (lives == 1) {
            total = NAMED_HAS_ONE.formatted(name);
        } else {
            total = NAMED_HAS.formatted(name, lives);
        }
        return notice + " " + total;
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
