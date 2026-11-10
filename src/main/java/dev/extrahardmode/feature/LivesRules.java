package dev.extrahardmode.feature;

/**
 * Shared life pool. {@code player.death.lives} below 0 is off. 0 or above is on.
 * Each death spends one, and the count can go below 0. Below 0, base health falls.
 * The death that spends the last life above 0 announces a win or a loss.
 * Minecraft-free so JUnit can cover the count.
 */
public final class LivesRules {
    /** Off. 0 or above turns the pool on. */
    public static final int DEFAULT_LIVES = -1;

    /** Vanilla player base max health, 10 hearts. */
    public static final double VANILLA_BASE_HEALTH = 20.0;

    /** Lowest base max health once lives are negative, 3 hearts. Armor still adds. */
    public static final double MIN_BASE_HEALTH = 6.0;

    /** Message first, then the credits. 140 ticks is 7 seconds. */
    public static final int CREDITS_DELAY_TICKS = 140;

    public static final String LOSE_SOLO = "You lose!";
    public static final String LOSE_MANY = "You lose!!!";
    public static final String WIN = "You win!!!";

    public enum Verdict {
        NONE,
        SOLO_LOSS,
        LAST_SURVIVOR
    }

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

    /** This death spent the last life that was still above 0. */
    public static boolean spentLast(int before) {
        return before == 1;
    }

    /** A life can be given back while the count is still under the server setting. */
    public static boolean canRestore(int lives, int configured) {
        return enabled(configured) && lives < configured;
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

    /**
     * {@code online} is players in the contest. {@code withLivesAfter} is how many of them
     * still have a life above 0 after this death. A death that does not spend that last
     * life does not announce again. Alone, that death is a loss. With others online,
     * exactly one player still holding a life above 0 wins, and that only on the death
     * that made it so.
     */
    public static Verdict verdict(int online, int withLivesAfter, boolean spentLast) {
        if (!spentLast || online <= 0) {
            return Verdict.NONE;
        }
        if (online == 1) {
            return Verdict.SOLO_LOSS;
        }
        if (withLivesAfter == 1) {
            return Verdict.LAST_SURVIVOR;
        }
        return Verdict.NONE;
    }
}
