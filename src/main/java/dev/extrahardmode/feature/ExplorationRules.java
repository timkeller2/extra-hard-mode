package dev.extrahardmode.feature;

/**
 * First biome visits and leaving spawn. Minecraft-free for JUnit.
 */
public final class ExplorationRules {
    public static final int BIOME_VISIT_XP = 100;
    public static final int WORLD_FIRST_BONUS_XP = 100;
    public static final int SPAWN_DISTANCE = 300;
    public static final int SPAWN_XP = 100;

    private ExplorationRules() {}

    public static boolean farFromSpawn(double dx, double dz, int distance) {
        long min = Math.max(0, distance);
        return dx * dx + dz * dz >= (double) min * (double) min;
    }

    public static boolean shouldAwardSpawn(boolean alreadyAwarded, double dx, double dz) {
        return !alreadyAwarded && farFromSpawn(dx, dz, SPAWN_DISTANCE);
    }

    public static int biomeXp(boolean playerFirst, boolean worldFirst) {
        if (!playerFirst) {
            return 0;
        }
        return BIOME_VISIT_XP + (worldFirst ? WORLD_FIRST_BONUS_XP : 0);
    }

    /** {@code dark_forest} → {@code Dark Forest}. */
    public static String biomeFallbackName(String path) {
        if (path == null || path.isEmpty()) {
            return "an unknown land";
        }
        String[] parts = path.split("_");
        StringBuilder name = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (name.length() > 0) {
                name.append(' ');
            }
            name.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                name.append(part.substring(1));
            }
        }
        return name.length() == 0 ? "an unknown land" : name.toString();
    }

    public static String biomeTranslationKey(String namespace, String path) {
        String ns = namespace == null || namespace.isEmpty() ? "minecraft" : namespace;
        String id = path == null ? "" : path;
        return "biome." + ns + "." + id;
    }
}
