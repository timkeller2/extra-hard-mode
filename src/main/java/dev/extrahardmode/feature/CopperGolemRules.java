package dev.extrahardmode.feature;

/**
 * Which copper golem may use a copper chest. Minecraft-free so JUnit can cover it.
 *
 * <p>A chest with no owner is public, and any golem may use it. A locked chest accepts
 * only a golem whose summoner is that placer. A missing summoner is not that placer.
 */
public final class CopperGolemRules {
    private CopperGolemRules() {}

    /**
     * True when this golem may take from or put into the chest. A null or blank owner
     * is a public chest. {@code summonerId} is the player who built or spawn-egged the
     * golem, or null when nobody did.
     */
    public static boolean mayUse(String summonerId, String chestOwnerId) {
        if (chestOwnerId == null || chestOwnerId.isEmpty()) {
            return true;
        }
        return chestOwnerId.equals(summonerId);
    }
}
