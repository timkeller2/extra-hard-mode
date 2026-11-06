package dev.extrahardmode.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builder/slayer thresholds, break-decrement, and mana regen. Minecraft-free for JUnit.
 */
public final class AchievementRules {
    public static final int[] BUILDER_THRESHOLDS = {256, 512, 1024};
    /**
     * One tier for wool, glass, glass panes, terracotta, concrete, and concrete powder.
     * Each block id is its own track.
     */
    public static final int[] COLOR_BUILDER_THRESHOLDS = {64};
    private static final Set<String> WOOL_COLORS = Set.of(
            "white",
            "orange",
            "magenta",
            "light_blue",
            "yellow",
            "lime",
            "pink",
            "gray",
            "light_gray",
            "cyan",
            "purple",
            "blue",
            "brown",
            "green",
            "red",
            "black");
    public static final int[] SLAYER_THRESHOLDS = {25, 50, 100, 175, 275, 425, 600};
    public static final int BREAK_DECREMENT_PERCENT = 75;
    public static final int TICKS_PER_MINUTE = 1200;
    /** Base regen per Minecraft minute is {@code (manaLevel + currentMana) / REGEN_DIVISOR}. */
    public static final double REGEN_DIVISOR = 200.0;
    /** Saturation at or above this doubles the regen rate for that minute and is not spent. */
    public static final double SATURATION_REGEN_MINIMUM = 18.0;
    public static final double SATURATION_REGEN_MULTIPLIER = 2.0;
    /** Regen multiplier once current mana is at or above mana level. */
    public static final double OVERFLOW_REGEN_MULTIPLIER = 0.25;
    /** Stored mana cannot exceed this (20 crystals), unless mana level is higher. */
    public static final double MANA_HARD_CAP = 40.0;
    public static final int REWARD_EXPERIENCE_POINTS = 75;
    /** Extra XP for the first player to claim a given achievement server-wide. */
    public static final int FIRST_CLAIM_BONUS_EXPERIENCE = 25;
    /** Rockets launched at the player who wins by achievements. */
    public static final int VICTORY_FIREWORKS = 5;

    public static final int CLOSEST_LIST_LIMIT = 15;
    public static final int SLAYER_LIST_LIMIT = 3;
    public static final int BUILDER_LIST_LIMIT = 5;
    public static final int WATCH_SLAYER_LIMIT = 1;
    public static final int WATCH_BUILDER_LIMIT = BUILDER_LIST_LIMIT;

    public record Progress(String id, boolean builder, int count, int nextTarget) {
        public int remaining() {
            return Math.max(0, nextTarget - count);
        }
    }

    private AchievementRules() {}

    /**
     * Highest tier unlocked by {@code count} that has not already been granted.
     * Tiers are 1-based; {@code alreadyAwarded} is how many tiers were previously given.
     */
    public static int awardedAfter(int alreadyAwarded, int count, int[] thresholds) {
        int awarded = Math.max(0, alreadyAwarded);
        while (awarded < thresholds.length && count >= thresholds[awarded]) {
            awarded++;
        }
        return awarded;
    }

    public static int increment(int count) {
        if (count >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, count) + 1;
    }

    /** {@code roll} is 0–99. */
    public static int maybeDecrement(int count, int roll, int percent) {
        if (count <= 0) {
            return 0;
        }
        if (percent <= 0) {
            return count;
        }
        if (percent >= 100 || roll < percent) {
            return count - 1;
        }
        return count;
    }

    /** {@code (manaLevel + currentMana) / 200} mana per Minecraft minute. */
    public static double regenPerMinute(int manaLevel, double currentMana) {
        return regenPerMinute(manaLevel, currentMana, 1.0);
    }

    /** Base regen times {@code multiplier}. Battery passes its multiplier. Inactive regen passes 1. */
    public static double regenPerMinute(int manaLevel, double currentMana, double multiplier) {
        if (manaLevel <= 0) {
            return 0.0;
        }
        double base = (manaLevel + Math.max(0.0, currentMana)) / REGEN_DIVISOR;
        return base * Math.max(0.0, multiplier);
    }

    /**
     * Apply one regen tick. Full rate while below mana level, 1/4 rate above it,
     * never more than {@link #manaCap(int)}.
     */
    public static double addMana(double current, int manaLevel, double regen) {
        if (manaLevel <= 0) {
            return 0.0;
        }
        double now = Math.max(0.0, current);
        double add = Math.max(0.0, regen);
        double level = manaLevel;
        double next;
        if (now >= level) {
            next = now + add * OVERFLOW_REGEN_MULTIPLIER;
        } else if (add <= level - now) {
            next = now + add;
        } else {
            next = level + (add - (level - now)) * OVERFLOW_REGEN_MULTIPLIER;
        }
        return Math.min(manaCap(manaLevel), next);
    }

    /** Stored mana stops at {@link #MANA_HARD_CAP}. A higher mana level raises that ceiling. */
    public static double manaCap(int manaLevel) {
        return Math.max(MANA_HARD_CAP, Math.max(0, manaLevel));
    }

    /** True when stored mana has reached {@link #manaCap(int)}. */
    public static boolean manaFull(double current, int manaLevel) {
        return current >= manaCap(manaLevel);
    }

    /** True when stored mana has reached the player's mana level. The storage cap can be higher. */
    public static boolean manaReachedLevel(double current, int manaLevel) {
        return current >= Math.max(0, manaLevel);
    }

    public static double clampMana(double current) {
        return clampMana(current, MANA_HARD_CAP);
    }

    public static double clampMana(double current, double cap) {
        if (current <= 0.0) {
            return 0.0;
        }
        return Math.min(Math.max(0.0, cap), current);
    }

    /** True when saturation is high enough to double this minute's regen rate. */
    public static boolean saturationDoublesRegen(double saturation) {
        return saturation >= SATURATION_REGEN_MINIMUM;
    }

    /**
     * Double {@code regenPerMinute} while saturation is at least {@link #SATURATION_REGEN_MINIMUM}.
     * Saturation itself is not spent.
     */
    public static double withSaturationRegen(double regenPerMinute, double saturation) {
        if (!saturationDoublesRegen(saturation)) {
            return regenPerMinute;
        }
        return regenPerMinute * SATURATION_REGEN_MULTIPLIER;
    }

    /** Base 75 XP, plus 25 when this player is first server-wide. */
    public static int rewardExperience(boolean firstClaim) {
        return REWARD_EXPERIENCE_POINTS + (firstClaim ? FIRST_CLAIM_BONUS_EXPERIENCE : 0);
    }

    /** Emeralds for a track that awards only once, such as a block placed 64 times. */
    public static final int SINGLE_TIER_EMERALDS = 2;

    /**
     * Emeralds for completing {@code tierIndex} (0-based) on a track of
     * {@code tierCount} tiers. One tier pays {@link #SINGLE_TIER_EMERALDS}.
     * A longer track pays the tier number: the first pays 1, and the 7th slayer tier pays 7.
     */
    public static int rewardEmeralds(int tierIndex, int tierCount) {
        if (tierIndex < 0 || tierIndex >= tierCount) {
            return 0;
        }
        if (tierCount == 1) {
            return SINGLE_TIER_EMERALDS;
        }
        return tierIndex + 1;
    }

    /** Experience-level percent chance of a bonus mana level, 0–100. */
    public static int manaChancePercent(int experienceLevel) {
        return Math.min(100, Math.max(0, experienceLevel));
    }

    /** {@code roll} is 0–99. */
    public static boolean manaFromExperience(int experienceLevel, int roll) {
        int chance = manaChancePercent(experienceLevel);
        if (chance <= 0) {
            return false;
        }
        if (chance >= 100) {
            return true;
        }
        return roll < chance;
    }

    /** Configured base of {@link #manaDieFaces(int, double)} before redstone gifts. */
    public static final int DEFAULT_MANA_LEARNING_CAPACITY = 0;

    /**
     * Faces on the die rolled after {@link #manaFromExperience} succeeds.
     * The configured base plus earned capacity, rounded down. {@code 0} cannot succeed.
     */
    public static int manaDieFaces(int configBase, double earned) {
        double earnedSafe = earned > 0.0 && !Double.isNaN(earned) ? earned : 0.0;
        double total = Math.max(0, configBase) + earnedSafe;
        if (Double.isNaN(total) || total < 1.0) {
            return 0;
        }
        return (int) Math.floor(total);
    }

    /**
     * The experience roll offered a mana level. {@code die} is 1 through the player's mana learning
     * capacity and must be strictly higher than the stored mana level. Well Fed and ready gold
     * are not part of that roll.
     */
    public static boolean manaDieBeatsBaseLevel(int baseManaLevel, int die) {
        return die > Math.max(0, baseManaLevel);
    }

    /** {@code b:minecraft:oak_planks:0} / {@code s:minecraft:zombie:1}. */
    public static String claimKey(boolean builder, String id, int tierIndex) {
        if (id == null || id.isEmpty() || tierIndex < 0) {
            return "";
        }
        return (builder ? "b:" : "s:") + id + ":" + tierIndex;
    }

    /** Next unawarded threshold from count alone (no prior grants). */
    public static int nextTarget(int count, int[] thresholds) {
        return nextTarget(count, thresholds, 0);
    }

    /**
     * Next achievement that has not already been granted. Already-earned tiers
     * are skipped even if the action count later dropped below them. Server-wide
     * first-claim bonuses do not hide a tier from other players.
     */
    public static int nextTarget(int count, int[] thresholds, int alreadyAwarded) {
        if (thresholds == null || thresholds.length == 0) {
            return 0;
        }
        int start = Math.max(0, alreadyAwarded);
        if (start >= thresholds.length) {
            return 0;
        }
        return thresholds[start];
    }

    /** Same next tier as {@link #nextTarget(int, int[], int)}; claimed keys do not skip. */
    public static int nextTarget(
            int count,
            int[] thresholds,
            int alreadyAwarded,
            Collection<String> claimed,
            boolean builder,
            String id) {
        return nextTarget(count, thresholds, alreadyAwarded);
    }

    public static boolean isClaimed(Collection<String> claimed, boolean builder, String id, int tierIndex) {
        return claimed != null && !claimed.isEmpty() && claimed.contains(claimKey(builder, id, tierIndex));
    }

    /**
     * In-progress builder and slayer tracks with at least one action, nearest
     * next tier first. Fully completed tracks are omitted, and so is a builder
     * track that has finished its only tier. At most {@link #SLAYER_LIST_LIMIT}
     * slayer tracks are mixed in so they cannot crowd out builder progress.
     */
    public static List<Progress> closest(
            Map<String, Integer> builderCounts, Map<String, Integer> slayerCounts, int limit) {
        return closest(builderCounts, slayerCounts, Map.of(), Map.of(), limit, SLAYER_LIST_LIMIT, Set.of());
    }

    public static List<Progress> closest(
            Map<String, Integer> builderCounts,
            Map<String, Integer> slayerCounts,
            Map<String, Integer> builderAwarded,
            Map<String, Integer> slayerAwarded,
            int limit) {
        return closest(builderCounts, slayerCounts, builderAwarded, slayerAwarded, limit, SLAYER_LIST_LIMIT, Set.of());
    }

    public static List<Progress> closest(
            Map<String, Integer> builderCounts,
            Map<String, Integer> slayerCounts,
            Map<String, Integer> builderAwarded,
            Map<String, Integer> slayerAwarded,
            int limit,
            Collection<String> claimed) {
        return closest(builderCounts, slayerCounts, builderAwarded, slayerAwarded, limit, SLAYER_LIST_LIMIT, claimed);
    }

    public static List<Progress> closest(
            Map<String, Integer> builderCounts,
            Map<String, Integer> slayerCounts,
            int limit,
            int slayerLimit) {
        return closest(builderCounts, slayerCounts, Map.of(), Map.of(), limit, slayerLimit, Set.of());
    }

    public static List<Progress> closest(
            Map<String, Integer> builderCounts,
            Map<String, Integer> slayerCounts,
            Map<String, Integer> builderAwarded,
            Map<String, Integer> slayerAwarded,
            int limit,
            int slayerLimit) {
        return closest(builderCounts, slayerCounts, builderAwarded, slayerAwarded, limit, slayerLimit, Set.of());
    }

    public static List<Progress> closest(
            Map<String, Integer> builderCounts,
            Map<String, Integer> slayerCounts,
            Map<String, Integer> builderAwarded,
            Map<String, Integer> slayerAwarded,
            int limit,
            int slayerLimit,
            Collection<String> claimed) {
        List<Progress> builders = new ArrayList<>();
        List<Progress> slayers = new ArrayList<>();
        collect(builders, builderCounts, builderAwarded, true, claimed, true);
        collect(slayers, slayerCounts, slayerAwarded, false, claimed, true);
        sortByRemaining(builders);
        sortByRemaining(slayers);
        int slayerCap = Math.min(Math.max(0, slayerLimit), slayers.size());
        int builderCap = Math.min(BUILDER_LIST_LIMIT, builders.size());
        List<Progress> mixed = new ArrayList<>(builderCap + slayerCap);
        mixed.addAll(slayers.subList(0, slayerCap));
        mixed.addAll(builders.subList(0, builderCap));
        sortByRemaining(mixed);
        int cap = Math.max(0, limit);
        if (mixed.size() <= cap) {
            return List.copyOf(mixed);
        }
        return List.copyOf(mixed.subList(0, cap));
    }

    /**
     * Clock report: the nearest in-progress slayer track, then the nearest
     * {@link #BUILDER_LIST_LIMIT} builder tracks. Slayer stays first even when a
     * builder is closer.
     */
    public static List<Progress> closestWatch(
            Map<String, Integer> builderCounts, Map<String, Integer> slayerCounts) {
        return closestWatch(builderCounts, slayerCounts, Map.of(), Map.of(), Set.of());
    }

    public static List<Progress> closestWatch(
            Map<String, Integer> builderCounts,
            Map<String, Integer> slayerCounts,
            Map<String, Integer> builderAwarded,
            Map<String, Integer> slayerAwarded) {
        return closestWatch(builderCounts, slayerCounts, builderAwarded, slayerAwarded, Set.of());
    }

    public static List<Progress> closestWatch(
            Map<String, Integer> builderCounts,
            Map<String, Integer> slayerCounts,
            Map<String, Integer> builderAwarded,
            Map<String, Integer> slayerAwarded,
            Collection<String> claimed) {
        List<Progress> builders = new ArrayList<>();
        List<Progress> slayers = new ArrayList<>();
        collect(builders, builderCounts, builderAwarded, true, claimed);
        collect(slayers, slayerCounts, slayerAwarded, false, claimed);
        sortByRemaining(builders);
        sortByRemaining(slayers);
        List<Progress> report = new ArrayList<>(WATCH_SLAYER_LIMIT + BUILDER_LIST_LIMIT);
        int slayerCap = Math.min(WATCH_SLAYER_LIMIT, slayers.size());
        report.addAll(slayers.subList(0, slayerCap));
        int builderCap = Math.min(BUILDER_LIST_LIMIT, builders.size());
        report.addAll(builders.subList(0, builderCap));
        return List.copyOf(report);
    }

    /**
     * Builder progress for the last block this player placed. Completed tracks
     * still report against the final threshold so the last-placed line is never
     * blank.
     */
    public static Progress lastPlaced(String id, int count, int awarded) {
        return lastPlaced(id, count, awarded, Set.of());
    }

    public static Progress lastPlaced(String id, int count, int awarded, Collection<String> claimed) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        int[] thresholds = builderThresholds(id);
        int next = nextTarget(Math.max(0, count), thresholds, Math.max(0, awarded), claimed, true, id);
        if (next <= 0) {
            next = thresholds[thresholds.length - 1];
        }
        return new Progress(id, true, Math.max(0, count), next);
    }

    /**
     * Last-placed line for {@code /tougher achieve}. A block that has finished its only tier is
     * omitted. {@link #lastPlaced} still reports that final threshold.
     */
    public static Progress achieveLastPlaced(String id, int count, int awarded) {
        if (finishedSingleTier(id, count, awarded)) {
            return null;
        }
        return lastPlaced(id, count, awarded);
    }

    /**
     * One-threshold builder track that has reached that threshold, or has already been awarded it.
     * A later drop in the count does not bring it back.
     */
    public static boolean finishedSingleTier(String blockId, int count, int awarded) {
        int[] thresholds = builderThresholds(blockId);
        if (thresholds.length != 1) {
            return false;
        }
        return awarded >= thresholds.length || count >= thresholds[0];
    }

    private static void sortByRemaining(List<Progress> tracks) {
        tracks.sort(Comparator.comparingInt(Progress::remaining)
                .thenComparing(Progress::id, String.CASE_INSENSITIVE_ORDER));
    }

    /**
     * Wool, glass, glass panes, terracotta, concrete, and concrete powder use
     * {@link #COLOR_BUILDER_THRESHOLDS}. Every other block uses {@link #BUILDER_THRESHOLDS}.
     */
    public static int[] builderThresholds(String blockId) {
        return colorBuilderBlock(blockId) ? COLOR_BUILDER_THRESHOLDS : BUILDER_THRESHOLDS;
    }

    /**
     * True for one dye color of wool (block, slab, stairs, carpet), stained glass,
     * stained glass panes, terracotta, glazed terracotta, concrete, concrete slabs,
     * concrete stairs, and concrete powder. Also clear glass, tinted glass, clear
     * glass panes, and plain terracotta.
     */
    public static boolean colorBuilderBlock(String blockId) {
        String path = pathOf(blockId);
        if (path.isEmpty()) {
            return false;
        }
        if ("glass".equals(path)
                || "tinted_glass".equals(path)
                || "glass_pane".equals(path)
                || "terracotta".equals(path)) {
            return true;
        }
        if (path.endsWith("_wool_stairs")) {
            return dyeColor(path, "_wool_stairs");
        }
        if (path.endsWith("_wool_slab")) {
            return dyeColor(path, "_wool_slab");
        }
        if (path.endsWith("_wool")) {
            return dyeColor(path, "_wool");
        }
        if (path.endsWith("_carpet")) {
            return dyeColor(path, "_carpet");
        }
        if (path.endsWith("_stained_glass_pane")) {
            return dyeColor(path, "_stained_glass_pane");
        }
        if (path.endsWith("_stained_glass")) {
            return dyeColor(path, "_stained_glass");
        }
        if (path.endsWith("_glazed_terracotta")) {
            return dyeColor(path, "_glazed_terracotta");
        }
        if (path.endsWith("_concrete_powder")) {
            return dyeColor(path, "_concrete_powder");
        }
        if (path.endsWith("_concrete_stairs")) {
            return dyeColor(path, "_concrete_stairs");
        }
        if (path.endsWith("_concrete_slab")) {
            return dyeColor(path, "_concrete_slab");
        }
        if (path.endsWith("_terracotta")) {
            return dyeColor(path, "_terracotta");
        }
        if (path.endsWith("_concrete")) {
            return dyeColor(path, "_concrete");
        }
        return false;
    }

    private static boolean dyeColor(String path, String suffix) {
        return WOOL_COLORS.contains(path.substring(0, path.length() - suffix.length()));
    }

    private static String pathOf(String id) {
        if (id == null || id.isEmpty()) {
            return "";
        }
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(colon + 1) : id;
    }

    public static String prettyId(String id) {
        if (id == null || id.isEmpty()) {
            return "unknown";
        }
        int colon = id.indexOf(':');
        String path = colon >= 0 ? id.substring(colon + 1) : id;
        return path.replace('_', ' ').replace('/', ' ');
    }

    private static void collect(
            List<Progress> into,
            Map<String, Integer> counts,
            Map<String, Integer> awarded,
            boolean builder,
            Collection<String> claimed) {
        collect(into, counts, awarded, builder, claimed, false);
    }

    private static void collect(
            List<Progress> into,
            Map<String, Integer> counts,
            Map<String, Integer> awarded,
            boolean builder,
            Collection<String> claimed,
            boolean omitFinishedSingle) {
        if (counts == null || counts.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            String id = entry.getKey();
            Integer value = entry.getValue();
            if (id == null || id.isEmpty() || value == null || value < 1) {
                continue;
            }
            int already = awarded == null ? 0 : awarded.getOrDefault(id, 0);
            if (omitFinishedSingle && builder && finishedSingleTier(id, value, already)) {
                continue;
            }
            int[] thresholds = builder ? builderThresholds(id) : SLAYER_THRESHOLDS;
            int next = nextTarget(value, thresholds, already, claimed, builder, id);
            if (next <= 0) {
                continue;
            }
            into.add(new Progress(id, builder, value, next));
        }
    }

    /** Sum of awarded builder tiers and slayer tiers. Each tier is one achievement. */
    public static int earnedAchievements(Map<String, Integer> builderAwarded, Map<String, Integer> slayerAwarded) {
        return sumAwarded(builderAwarded) + sumAwarded(slayerAwarded);
    }

    /** A positive goal is on. {@code 0} is off. */
    public static boolean victoryEnabled(int goal) {
        return goal > 0;
    }

    /** True when this total meets a positive goal and nobody has won yet. */
    public static boolean declaresVictory(int earned, int goal, boolean alreadyWon) {
        return victoryEnabled(goal) && !alreadyWon && earned >= goal;
    }

    /** Top-left counter, such as {@code 12/40}. */
    public static String victoryProgress(int earned, int goal) {
        return Math.max(0, earned) + "/" + Math.max(0, goal);
    }

    private static int sumAwarded(Map<String, Integer> awarded) {
        if (awarded == null || awarded.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (Integer value : awarded.values()) {
            if (value != null && value > 0) {
                total += value;
            }
        }
        return total;
    }
}
