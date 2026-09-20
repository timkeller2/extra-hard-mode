package dev.extrahardmode.feature;

/**
 * Boats ignore vanilla fall damage. Tougher smashes occupied boats
 * that drop farther than this. Minecraft-free so JUnit can cover the gate.
 */
public final class BoatRules {
    /** Vanilla fall damage starts after 3 blocks; boats break on a longer drop. */
    public static final double BREAK_FALL_DISTANCE = 3.0;

    private BoatRules() {}

    public static boolean shouldBreak(double fallDistance, boolean onGround, boolean playerAboard) {
        return onGround && playerAboard && fallDistance > BREAK_FALL_DISTANCE;
    }
}
