package dev.extrahardmode.feature;

/** When a compass needle should use the player's own spawn instead of the world spawn. */
public final class CompassRules {
    private CompassRules() {}

    public static boolean pointAtPersonalSpawn(
            boolean sneaking, boolean localOwner, boolean hasSpawn, boolean sameDimension) {
        return sneaking && localOwner && hasSpawn && sameDimension;
    }
}
