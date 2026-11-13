package dev.extrahardmode.feature;

import java.util.List;

/**
 * Ownership of a placed hopper. The wait, the sign names, and the public-day
 * label are the same rules as a copper chest. Minecraft-free so JUnit can cover it.
 */
public final class HopperRules {
    private HopperRules() {}

    /** One locked hopper. {@code owner} is the placer's id. {@code ownerName} is their name at placement. */
    public record Claim(
            String dimension, int x, int y, int z, String owner, String ownerName, long expiresAt) {
        public boolean samePos(String dimension, int x, int y, int z) {
            return this.dimension != null
                    && this.dimension.equals(dimension)
                    && this.x == x
                    && this.y == y
                    && this.z == z;
        }

        public Claim withExpiry(long expiresAt) {
            return new Claim(dimension, x, y, z, owner, ownerName, expiresAt);
        }
    }

    /** The hopper and the chest were placed by the same player. */
    public static boolean sameOwner(String hopperOwnerId, String chestOwnerId) {
        return hopperOwnerId != null && !hopperOwnerId.isEmpty() && hopperOwnerId.equals(chestOwnerId);
    }

    /**
     * True when this hopper's placer may pull from a locked chest or hopper.
     * The placer's id matches, or their placement name is a whole sign line.
     */
    public static boolean mayPull(
            String hopperOwnerId, String hopperOwnerName, String sourceOwnerId, List<String> signLines) {
        return CopperChestRules.allowed(sourceOwnerId, hopperOwnerId, signLines, hopperOwnerName);
    }

    public static String key(Claim claim) {
        if (claim == null
                || claim.dimension == null
                || claim.dimension.isEmpty()
                || claim.dimension.indexOf('|') >= 0
                || claim.owner == null
                || claim.owner.isEmpty()
                || claim.owner.indexOf('|') >= 0
                || claim.ownerName == null
                || claim.ownerName.indexOf('|') >= 0
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
                + claim.expiresAt
                + "|"
                + claim.ownerName;
    }

    public static Claim parse(String raw) {
        if (raw == null) {
            return null;
        }
        String[] parts = raw.split("\\|", -1);
        if (parts.length != 7 || parts[0].isEmpty() || parts[4].isEmpty()) {
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
                    parts[6],
                    expiresAt);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
