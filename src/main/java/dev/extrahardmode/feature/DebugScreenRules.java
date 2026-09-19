package dev.extrahardmode.feature;

/** F3 debug overlay. Minecraft-free so JUnit can cover the gate. */
public final class DebugScreenRules {
    private DebugScreenRules() {}

    /**
     * When Extra Hard Mode is off, F3 stays vanilla. Bypass always allows F3.
     * Otherwise the config flag wins (default false).
     */
    public static boolean allowed(boolean extraHardModeActive, boolean playerBypass, boolean f3Enabled) {
        if (!extraHardModeActive || playerBypass) {
            return true;
        }
        return f3Enabled;
    }
}
