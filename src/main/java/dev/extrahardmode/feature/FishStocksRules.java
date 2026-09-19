package dev.extrahardmode.feature;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Chunk fish-stock spawn odds. Minecraft-free so JUnit can cover 1/3 vs 1/20.
 *
 * <p>{@code healthyCount} or more of the same type in the chunk uses {@code healthyRatePercent}
 * (default 33 ≈ 1/3). Below that, {@code scarceRatePercent} (default 5 = 1/20).
 */
public final class FishStocksRules {
    public static final int DEFAULT_HEALTHY_COUNT = 3;
    public static final int DEFAULT_HEALTHY_RATE_PERCENT = 33;
    public static final int DEFAULT_SCARCE_RATE_PERCENT = 5;
    public static final int DEFAULT_BITE_WAIT_MULTIPLIER = 3;
    public static final int PACK_SIZE = 1;

    private FishStocksRules() {}

    public static int spawnRatePercent(int sameTypeInChunk, int healthyCount, int healthyRatePercent, int scarceRatePercent) {
        int threshold = Math.max(0, healthyCount);
        if (sameTypeInChunk >= threshold) {
            return clampPercent(healthyRatePercent);
        }
        return clampPercent(scarceRatePercent);
    }

    public static boolean allowSpawn(int sameTypeInChunk, int healthyCount, int healthyRatePercent, int scarceRatePercent) {
        return allowSpawn(
                sameTypeInChunk, healthyCount, healthyRatePercent, scarceRatePercent, ThreadLocalRandom.current().nextInt(100));
    }

    public static boolean allowSpawn(
            int sameTypeInChunk, int healthyCount, int healthyRatePercent, int scarceRatePercent, int roll0to99) {
        int rate = spawnRatePercent(sameTypeInChunk, healthyCount, healthyRatePercent, scarceRatePercent);
        if (rate <= 0) {
            return false;
        }
        if (rate >= 100) {
            return true;
        }
        return Math.floorMod(roll0to99, 100) < rate;
    }

    public static int scaleWait(int ticks, int multiplier) {
        long scaled = (long) Math.max(0, ticks) * (long) Math.max(1, multiplier);
        return scaled > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) scaled;
    }

    public static int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
