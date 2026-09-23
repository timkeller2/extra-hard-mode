package dev.extrahardmode.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * How many blocks in a TNT blast catch fire. Minecraft-free so the percent can be tested.
 */
public final class ExplosionFireRules {
    public static final int DEFAULT_TNT_FIRE_PERCENT = 20;

    private ExplosionFireRules() {}

    /** Rounded share of {@code affected}. {@code 0} and below ignite nothing; {@code 100} and above ignite all. */
    public static int igniteCount(int affected, int percent) {
        if (affected <= 0 || percent <= 0) {
            return 0;
        }
        int clamped = Math.min(percent, 100);
        return (int) Math.round(affected * (clamped / 100.0));
    }

    /**
     * {@code count} distinct indices in {@code [0, size)}. {@code nextBound} is {@code nextInt(bound)}
     * and is only called with a positive bound.
     */
    public static List<Integer> chooseIndices(int size, int count, IntUnaryOperator nextBound) {
        if (size <= 0 || count <= 0) {
            return List.of();
        }
        int take = Math.min(size, count);
        int[] order = new int[size];
        for (int i = 0; i < size; i++) {
            order[i] = i;
        }
        for (int i = 0; i < take; i++) {
            int pick = i + nextBound.applyAsInt(size - i);
            int swap = order[i];
            order[i] = order[pick];
            order[pick] = swap;
        }
        List<Integer> chosen = new ArrayList<>(take);
        for (int i = 0; i < take; i++) {
            chosen.add(order[i]);
        }
        return chosen;
    }
}
