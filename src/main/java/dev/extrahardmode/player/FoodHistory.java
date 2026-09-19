package dev.extrahardmode.player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** FIFO of recently eaten food item ids. Minecraft-free for unit tests. */
public final class FoodHistory {
    public static final int DEFAULT_SIZE = 7;

    private FoodHistory() {}

    public static boolean isNovel(List<String> recent, String foodId) {
        if (foodId == null || foodId.isEmpty()) {
            return false;
        }
        if (recent == null || recent.isEmpty()) {
            return true;
        }
        return !recent.contains(foodId);
    }

    public static List<String> record(List<String> recent, String foodId, int maxSize) {
        int cap = Math.max(1, maxSize);
        List<String> next = recent == null || recent.isEmpty() ? new ArrayList<>() : new ArrayList<>(recent);
        if (foodId != null && !foodId.isEmpty()) {
            next.add(foodId);
        }
        while (next.size() > cap) {
            next.remove(0);
        }
        return next;
    }

    /** How many times {@code foodId} appears in the last {@code window} entries of {@code recent}. */
    public static int countInLast(List<String> recent, String foodId, int window) {
        if (foodId == null || foodId.isEmpty() || recent == null || recent.isEmpty() || window <= 0) {
            return 0;
        }
        int start = Math.max(0, recent.size() - window);
        int count = 0;
        for (int i = start; i < recent.size(); i++) {
            if (foodId.equals(recent.get(i))) {
                count++;
            }
        }
        return count;
    }

    /** True when the last {@code window} meals are {@code window} different foods. */
    public static boolean lastAreAllDifferent(List<String> recent, int window) {
        if (recent == null || window <= 0 || recent.size() < window) {
            return false;
        }
        Set<String> distinct = new HashSet<>();
        for (int i = recent.size() - window; i < recent.size(); i++) {
            String id = recent.get(i);
            if (id == null || id.isEmpty() || !distinct.add(id)) {
                return false;
            }
        }
        return distinct.size() == window;
    }
}
