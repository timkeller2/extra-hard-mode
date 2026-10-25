package dev.extrahardmode.feature;

/**
 * Experience saved and returned by the Tome of the Wise. Minecraft-free for JUnit.
 *
 * <p>Above {@link #KEPT_LEVEL}, write banks only the experience past that level and leaves the
 * player there. At that level or below, write banks all visible experience and leaves the
 * player at level 0. Either amount is then reduced to {@link #BASE_PERCENT} plus
 * {@link #PERCENT_PER_LEVEL} per ability level. Level 10 is 100%. Read grants that same share
 * of the stored amount.
 */
public final class TomeRules {
    /** Experience level kept when writing above it. At this level or below, the player is emptied to 0. */
    public static final int KEPT_LEVEL = 30;
    /** Percent of the banked experience before ability level. Level 10 reaches 100%. */
    public static final int BASE_PERCENT = 80;
    /** Added to {@link #BASE_PERCENT} for each point of ability level. */
    public static final int PERCENT_PER_LEVEL = 2;
    public static final String TITLE = "Tome of the Wise";
    /** Custom data key on the written book. Read uses this, not the page text. */
    public static final String XP_KEY = "tougher_xp";

    private TomeRules() {}

    /** Level left after a successful write. Above {@link #KEPT_LEVEL}, that level. Otherwise 0. */
    public static int levelAfterWrite(int experienceLevel) {
        return experienceLevel > KEPT_LEVEL ? KEPT_LEVEL : 0;
    }

    /**
     * Experience considered before the ability share. Above {@link #KEPT_LEVEL}, only the excess.
     * At that level or below, all visible experience.
     */
    public static int bankedXp(int experienceLevel, int totalXp) {
        int safe = Math.max(0, totalXp);
        if (experienceLevel > KEPT_LEVEL) {
            return excessAboveKeptLevel(safe);
        }
        return safe;
    }

    /**
     * Experience to finish the current level. Matches 26.3 {@code Player.getXpNeededForNextLevel}:
     * {@code 2 * level + 7} below 15, {@code 5 * level - 38} from 15 to 29, and
     * {@code 9 * level - 158} from 30 up.
     */
    public static int xpForNextLevel(int level) {
        long safe = Math.max(0, level);
        long cost;
        if (safe >= 30) {
            cost = 112L + (safe - 30L) * 9L;
        } else if (safe >= 15) {
            cost = 37L + (safe - 15L) * 5L;
        } else {
            cost = 7L + safe * 2L;
        }
        return saturate(cost);
    }

    /** Experience required to be exactly {@code level} with an empty progress bar. */
    public static int xpToReachLevel(int level) {
        long total = 0L;
        int target = Math.max(0, level);
        for (int i = 0; i < target; i++) {
            total += xpForNextLevel(i);
            if (total >= Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
        }
        return (int) total;
    }

    /** Visible experience from the level and the progress bar, not the scoreboard total. */
    public static int totalXp(int level, float progress) {
        int safeLevel = Math.max(0, level);
        int base = xpToReachLevel(safeLevel);
        if (!(progress > 0.0F) || base == Integer.MAX_VALUE) {
            return base;
        }
        int span = xpForNextLevel(safeLevel);
        if (span <= 0) {
            return base;
        }
        double fraction = Math.min(1.0, progress);
        long into = Math.round(fraction * span);
        if (into >= span) {
            into = span - 1L;
        }
        return saturate(base + Math.max(0L, into));
    }

    /** Experience above what is required to be exactly level {@link #KEPT_LEVEL}. */
    public static int excessAboveKeptLevel(int totalXp) {
        int floor = xpToReachLevel(KEPT_LEVEL);
        return Math.max(0, totalXp - floor);
    }

    /** {@code round(amount * (80% + 2% * abilityLevel))}. Zero when the input or level is empty. */
    public static int share(int amount, double abilityLevel) {
        if (amount <= 0 || !(abilityLevel > 0.0)) {
            return 0;
        }
        double raw = amount * (BASE_PERCENT + PERCENT_PER_LEVEL * abilityLevel) / 100.0;
        if (raw >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        long rounded = Math.round(raw);
        if (rounded <= 0L) {
            return 0;
        }
        return saturate(rounded);
    }

    public static String authorName(String name) {
        if (name == null || name.isBlank()) {
            return "Someone";
        }
        String trimmed = name.trim().replace('\n', ' ').replace('\r', ' ');
        if (trimmed.length() > 64) {
            trimmed = trimmed.substring(0, 64).trim();
        }
        return trimmed.isBlank() ? "Someone" : trimmed;
    }

    /** Page and tooltip note: who saved the experience, and how much. */
    public static String note(String author, int xp) {
        return authorName(author) + " saved " + Math.max(0, xp) + " experience in this tome.";
    }

    private static int saturate(long value) {
        if (value <= 0L) {
            return 0;
        }
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }
}
