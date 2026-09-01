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

    /**
     * Vanilla {@code causeFallDamage} uses {@code ceil(fallDistance - 1)}. A 1-tick supported pop
     * (~0.04) must not deal EHM damage; a real drop ({@code fallDistance > 1}) may.
     */
    public static boolean farEnoughToHurt(double fallDistance) {
        return fallDistance > 1.0;
    }
}
