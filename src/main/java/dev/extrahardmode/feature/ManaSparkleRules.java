package dev.extrahardmode.feature;

/**
 * Quiet blue shimmer for a player whose current mana is above 3. Minecraft-free.
 * One burst lasts a second or two, then the next waits 30 to 60 seconds.
 * The burst emits one sparkle per whole point of mana.
 */
public final class ManaSparkleRules {
    /** Sparkles begin only when current mana is above this. */
    public static final double MIN_MANA = 3.0;
    /** Shortest wait between bursts: 30 seconds. */
    public static final int GAP_MIN_TICKS = 600;
    /** Longest wait between bursts: 60 seconds. */
    public static final int GAP_MAX_TICKS = 1200;
    /** Shortest shimmer: 1 second. */
    public static final int BURST_MIN_TICKS = 20;
    /** Longest shimmer: 2 seconds. */
    public static final int BURST_MAX_TICKS = 40;

    private ManaSparkleRules() {}

    /** Whole points of mana to show, or 0 at 3 mana or below. */
    public static int sparkleCount(double mana) {
        if (!(mana > MIN_MANA)) {
            return 0;
        }
        return Math.max(0, (int) Math.floor(mana));
    }

    /** A wait of 30 to 60 seconds. {@code roll} may be any int. */
    public static int gapTicks(int roll) {
        return span(roll, GAP_MIN_TICKS, GAP_MAX_TICKS);
    }

    /** A shimmer of 1 or 2 seconds. {@code roll} may be any int. */
    public static int burstTicks(int roll) {
        return span(roll, BURST_MIN_TICKS, BURST_MAX_TICKS);
    }

    /**
     * Sparkles to emit on this tick so a burst of {@code burstTicks} emits {@code total} in all.
     * {@code tickIndex} is 0-based and inside the burst.
     */
    public static int sparklesOnTick(int total, int burstTicks, int tickIndex) {
        if (total <= 0 || burstTicks <= 0 || tickIndex < 0 || tickIndex >= burstTicks) {
            return 0;
        }
        int through = (int) ((long) total * (tickIndex + 1) / burstTicks);
        int before = (int) ((long) total * tickIndex / burstTicks);
        return through - before;
    }

    private static int span(int roll, int min, int max) {
        int width = max - min + 1;
        return min + Math.floorMod(roll, width);
    }
}
