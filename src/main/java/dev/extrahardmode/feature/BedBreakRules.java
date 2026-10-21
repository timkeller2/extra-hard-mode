package dev.extrahardmode.feature;

/** Player breaks of a bed. Minecraft-free for JUnit. */
public final class BedBreakRules {
    private BedBreakRules() {}

    /** True when a player break should destroy the bed and drop nothing. */
    public static boolean dropsNothing(boolean configured, boolean bypasses) {
        return configured && !bypasses;
    }
}
