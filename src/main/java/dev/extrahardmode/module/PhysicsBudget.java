package dev.extrahardmode.module;

/**
 * Queue caps. Minecraft-free so overflow/drop math can be unit-tested.
 */
public final class PhysicsBudget {
    public static final int CONVERSIONS_PER_TICK = 64;
    public static final int MAX_LIVE_EHM_FALLING = 128;
    public static final int MAX_FLOOD_FILL = 16;
    public static final int MAX_QUEUE_DEPTH = 4096;

    private PhysicsBudget() {}

    public static boolean overflowToSetBlock(int liveEntities, int maxLive) {
        return liveEntities >= maxLive;
    }

    public static boolean shouldDropOldest(int queueDepth, int maxDepth) {
        return queueDepth >= maxDepth;
    }

    public static int conversionsThisTick(int ready, int budget) {
        return Math.min(Math.max(ready, 0), Math.max(budget, 0));
    }
}
