package dev.extrahardmode.module;

import java.util.HashMap;
import java.util.Map;

/** Pure persist helper so shown-count rules can be unit-tested without Minecraft. */
public final class TutorialCounts {
    private TutorialCounts() {}

    public static int shown(Map<String, Integer> counts, String id) {
        if (counts == null || id == null) {
            return 0;
        }
        return Math.max(0, counts.getOrDefault(id, 0));
    }

    /**
     * @return {@code true} if this showing is still under {@code maxShows} and the count was incremented
     */
    public static boolean tryIncrement(Map<String, Integer> counts, String id, int maxShows) {
        if (counts == null || id == null || maxShows <= 0) {
            return false;
        }
        int shown = shown(counts, id);
        if (shown >= maxShows) {
            return false;
        }
        counts.put(id, shown + 1);
        return true;
    }

    public static Map<String, Integer> mutableCopy(Map<String, Integer> stored) {
        return stored == null || stored.isEmpty() ? new HashMap<>() : new HashMap<>(stored);
    }

    /**
     * {@code configured <= 0} disables every toast, including once-only extras.
     * Once/announce ids cap at 1 when toasts are enabled.
     */
    public static int effectiveMax(int configured, boolean once) {
        if (configured <= 0) {
            return 0;
        }
        return once ? 1 : configured;
    }
}
