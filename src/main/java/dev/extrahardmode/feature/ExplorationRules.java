package dev.extrahardmode.feature;

import java.util.Collection;

/**
 * First biome visits and leaving spawn. Minecraft-free for JUnit.
 */
public final class ExplorationRules {
    public static final int BIOME_VISIT_XP = 50;
    public static final int WORLD_FIRST_BONUS_XP = 100;
    /** XP per house point for a habitable home that qualifies. */
    public static final int HOME_BIOME_XP_PER_POINT = 3;
    /**
     * Another home of the same biome pays again when it is farther than this,
     * measured horizontally from bed to bed. The same distance does not pay.
     */
    public static final int HOME_BIOME_SPACING = 250;
    public static final int SPAWN_DISTANCE = 300;
    public static final int SPAWN_XP = 100;

    /** One home that already paid this player. Legacy biome-only strings have no place. */
    public record HomeAward(String biomeId, String dimension, int x, int z) {}

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

    public static boolean unseenBiome(Collection<String> already, String biomeId) {
        return biomeId != null && !biomeId.isEmpty() && (already == null || !already.contains(biomeId));
    }

    public static int homeBiomeXp(int houseScore) {
        if (houseScore <= 0) {
            return 0;
        }
        return houseScore * HOME_BIOME_XP_PER_POINT;
    }

    /** Saved award: {@code biome|dimension|x|z}. A bare biome id is a legacy claim with no place. */
    public static String homeAwardRecord(String biomeId, String dimension, int x, int z) {
        String dim = dimension == null ? "" : dimension;
        return biomeId + "|" + dim + "|" + x + "|" + z;
    }

    public static HomeAward parseHomeAward(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        String[] parts = raw.split("\\|", -1);
        if (parts.length != 4 || parts[0].isEmpty()) {
            return null;
        }
        try {
            return new HomeAward(parts[0], parts[1], Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * True when this bed can pay. Same biome within {@link #HOME_BIOME_SPACING} blocks
     * in the same dimension blocks it. A different biome, another dimension, or a
     * legacy biome name with no saved place does not.
     */
    public static boolean homeEarnsExperience(
            Collection<String> awards, String biomeId, String dimension, int x, int z) {
        if (biomeId == null || biomeId.isEmpty()) {
            return false;
        }
        if (awards == null || awards.isEmpty()) {
            return true;
        }
        long limit = (long) HOME_BIOME_SPACING * HOME_BIOME_SPACING;
        String here = dimension == null ? "" : dimension;
        for (String raw : awards) {
            HomeAward award = parseHomeAward(raw);
            if (award == null || !biomeId.equals(award.biomeId())) {
                continue;
            }
            if (!here.equals(award.dimension())) {
                continue;
            }
            long dx = (long) x - award.x();
            long dz = (long) z - award.z();
            if (dx * dx + dz * dz <= limit) {
                return false;
            }
        }
        return true;
    }

    /** Clock line for a bed whose room is not habitable yet. */
    public static String homeExperienceProspect(boolean earns) {
        if (earns) {
            return "Completing this home can earn experience.";
        }
        return "Completing this home will not earn experience. One of this biome is already within 250 blocks.";
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
