package dev.extrahardmode.feature;

/** When breaking a burning block should ignite the player. Minecraft-free. */
public final class FireBreakRules {
    private FireBreakRules() {}

    /**
     * Empty-hand punches of the fire block itself are already handled as extinguishing.
     * Any other break of fire, or of a block with fire against it, uses that same ignite.
     */
    public static boolean igniteOnBreak(boolean brokenIsFire, boolean adjacentFire, boolean emptyHand) {
        if (brokenIsFire && emptyHand) {
            return false;
        }
        return brokenIsFire || adjacentFire;
    }
}
