package dev.extrahardmode.feature;

/**
 * How bright a normal overworld hostile may spawn below Y=0. Minecraft-free.
 *
 * <p>Each full 15 blocks below Y=0 adds 1 to the block-light limit. Y=-15 allows
 * light 1. Y=-30 allows light 2. Y=-14 still requires light 0.
 */
public final class HostileDepthSpawnRules {
    public static final int SURFACE_Y = 0;
    public static final int BLOCKS_PER_LIGHT = 15;

    private HostileDepthSpawnRules() {}

    /** Extra block light allowed at this block Y. Zero at Y=0 and above. */
    public static int extraBlockLight(int y) {
        int below = SURFACE_Y - y;
        if (below <= 0) {
            return 0;
        }
        return below / BLOCKS_PER_LIGHT;
    }

    /** Keeps a higher vanilla limit. The depth allowance never lowers it. */
    public static int blockLightLimit(int vanillaLimit, int y) {
        return Math.max(vanillaLimit, extraBlockLight(y));
    }
}
