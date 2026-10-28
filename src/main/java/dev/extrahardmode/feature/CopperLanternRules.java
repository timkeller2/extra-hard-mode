package dev.extrahardmode.feature;

/**
 * Copper lanterns keep their vanilla light at Y=0 and above. Each full 5 blocks
 * below Y=0 removes 1. Y=-4 stays full. Y=-5 loses 1. The result never goes below 0.
 */
public final class CopperLanternRules {
    public static final int SURFACE_Y = 0;
    public static final int DEPTH_STEP = 5;

    private CopperLanternRules() {}

    public static int light(int vanillaLight, int y) {
        int vanilla = Math.max(0, vanillaLight);
        int below = SURFACE_Y - y;
        if (below <= 0) {
            return vanilla;
        }
        return Math.max(0, vanilla - below / DEPTH_STEP);
    }
}
