package dev.extrahardmode.feature;

import java.util.List;

/**
 * Ownership of a placed copper chest. Minecraft-free so JUnit can cover it.
 *
 * <p>A claim lasts {@link #CLAIM_DAYS} Minecraft days. A day is {@link #TICKS_PER_DAY}
 * ticks of overworld day time. Opening the chest starts that wait again. A sign on
 * the chest names other players, one name on each line, and those players are owners
 * too. Everyone else sees how many days remain until the chest is public.
 */
public final class CopperChestRules {
    public static final int CLAIM_DAYS = 90;
    public static final long TICKS_PER_DAY = 24000L;

    private CopperChestRules() {}

    /** One locked chest. {@code owner} is the placer's id. {@code expiresAt} is overworld day time. */
    public record Claim(String dimension, int x, int y, int z, String owner, long expiresAt) {
        public boolean samePos(String dimension, int x, int y, int z) {
            return this.dimension != null
                    && this.dimension.equals(dimension)
                    && this.x == x
                    && this.y == y
                    && this.z == z;
        }

        public Claim withExpiry(long expiresAt) {
            return new Claim(dimension, x, y, z, owner, expiresAt);
        }
    }

    /** Day time when a new claim, or a fresh opening, becomes public. */
    public static long expiresAt(long now) {
        return Math.max(0L, now) + CLAIM_DAYS * TICKS_PER_DAY;
    }

    /** True while {@code now} is still before the chest becomes public. */
    public static boolean locked(long expiresAt, long now) {
        return now < expiresAt;
    }

    /**
     * Whole days until the chest is public, rounded up. One tick left is still 1 day.
     * Zero once {@link #locked} is false.
     */
    public static int daysLeft(long expiresAt, long now) {
        if (!locked(expiresAt, now)) {
            return 0;
        }
        long left = expiresAt - Math.max(0L, now);
        long days = (left + TICKS_PER_DAY - 1L) / TICKS_PER_DAY;
        if (days > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) days;
    }

    /** {@code 1 day} or {@code 87 days}. */
    public static String daysPhrase(int days) {
        if (days == 1) {
            return "1 day";
        }
        return Math.max(0, days) + " days";
    }

    /** Empty once the chest is public. */
    public static String lookLabel(int days) {
        if (days <= 0) {
            return "";
        }
        return "Public in " + daysPhrase(days);
    }

    /**
     * True when a whole sign line is that player's name. Blank lines are skipped.
     * Matching ignores case and spaces around the line. A line that is only SELL
     * or BUY marks a player shop, so it is not a name.
     */
    public static boolean nameListed(List<String> lines, String playerName) {
        if (lines == null || playerName == null) {
            return false;
        }
        String name = playerName.trim();
        if (name.isEmpty()) {
            return false;
        }
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            String trimmed = line.trim();
            if (trimmed.equalsIgnoreCase("SELL") || trimmed.equalsIgnoreCase("BUY")) {
                continue;
            }
            if (trimmed.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /** The placer, or a player named on the chest's sign. */
    public static boolean allowed(String ownerId, String playerId, List<String> lines, String playerName) {
        if (ownerId != null && !ownerId.isEmpty() && ownerId.equals(playerId)) {
            return true;
        }
        return nameListed(lines, playerName);
    }

    public static String key(Claim claim) {
        if (claim == null
                || claim.dimension == null
                || claim.dimension.isEmpty()
                || claim.dimension.indexOf('|') >= 0
                || claim.owner == null
                || claim.owner.isEmpty()
                || claim.owner.indexOf('|') >= 0
                || claim.expiresAt < 0L) {
            return null;
        }
        return claim.dimension
                + "|"
                + claim.x
                + "|"
                + claim.y
                + "|"
                + claim.z
                + "|"
                + claim.owner
                + "|"
                + claim.expiresAt;
    }

    public static Claim parse(String raw) {
        if (raw == null) {
            return null;
        }
        String[] parts = raw.split("\\|", -1);
        if (parts.length != 6 || parts[0].isEmpty() || parts[4].isEmpty()) {
            return null;
        }
        try {
            long expiresAt = Long.parseLong(parts[5]);
            if (expiresAt < 0L) {
                return null;
            }
            return new Claim(
                    parts[0],
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]),
                    parts[4],
                    expiresAt);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
