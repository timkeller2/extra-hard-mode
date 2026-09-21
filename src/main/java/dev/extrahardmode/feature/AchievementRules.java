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
    public static final int[] SLAYER_THRESHOLDS = {25, 100, 250};
    public static final int BREAK_DECREMENT_PERCENT = 75;
    public static final int TICKS_PER_MINUTE = 1200;
    /** Base regen per Minecraft minute is {@code (manaLevel + currentMana) / REGEN_DIVISOR}. */
    public static final double REGEN_DIVISOR = 200.0;
    /** Saturation restore: current mana must be below {@code saturation - SATURATION_MANA_OFFSET}. */
    public static final double SATURATION_MANA_OFFSET = 17.0;
    public static final double SATURATION_MANA_RESTORE = 1.0;
    public static final float SATURATION_MANA_COST = 1.0F;
    public static final double QUARTZ_REGEN_MULTIPLIER = 3.0;
    /** One nether quartz is consumed per this much quartz-boosted mana (one crystal). */
    public static final double QUARTZ_MANA_PER_ITEM = 2.0;
    /** Regen multiplier once current mana is at or above mana level. */
    public static final double OVERFLOW_REGEN_MULTIPLIER = 0.25;
    /** Stored mana cannot exceed this (10 crystals). */
    public static final double MANA_HARD_CAP = 20.0;
    public static final int REWARD_EXPERIENCE_POINTS = 50;
    /** Extra XP for the first player to claim a given achievement server-wide. */
    public static final int FIRST_CLAIM_BONUS_EXPERIENCE = 25;
    public static final int MANA_DIAMOND_COST = 1;
    public static final int MANA_LAPIS_COST_START = 1;

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
    public static double regenPerMinute(int manaLevel, double currentMana, boolean quartz) {
        if (manaLevel <= 0) {
            return 0.0;
        }
        double base = (manaLevel + Math.max(0.0, currentMana)) / REGEN_DIVISOR;
        return quartz ? base * QUARTZ_REGEN_MULTIPLIER : base;
    }

    /**
     * Apply one regen tick. Full rate while below mana level, 1/4 rate above it,
     * never more than {@link #MANA_HARD_CAP}.
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
        return Math.min(MANA_HARD_CAP, next);
    }

    public static double clampMana(double current) {
        if (current <= 0.0) {
            return 0.0;
        }
        return Math.min(MANA_HARD_CAP, current);
    }

    public static boolean shouldBoostWithQuartz(int manaLevel, double current) {
        return manaLevel > 0 && current < manaLevel;
    }

    /**
     * Extra 1 mana per minute, paid with 1 saturation, when below mana level and
     * {@code currentMana < saturation - 17}.
     */
    public static boolean shouldRestoreFromSaturation(int manaLevel, double currentMana, double saturation) {
        if (manaLevel <= 0 || currentMana >= manaLevel) {
            return false;
        }
        if (saturation < SATURATION_MANA_COST) {
            return false;
        }
        return currentMana < saturation - SATURATION_MANA_OFFSET;
    }

    public static float saturationAfterManaRestore(float saturation) {
        return Math.max(0.0F, saturation - SATURATION_MANA_COST);
    }

    public static double addQuartzCredit(double credit, double gained) {
        return Math.max(0.0, credit) + Math.max(0.0, gained);
    }

    public static int quartzItemsForCredit(double credit) {
        if (credit < QUARTZ_MANA_PER_ITEM) {
            return 0;
        }
        return (int) Math.floor(credit / QUARTZ_MANA_PER_ITEM);
    }

    public static double remainingQuartzCredit(double credit, int consumed) {
        return Math.max(0.0, credit - Math.max(0, consumed) * QUARTZ_MANA_PER_ITEM);
    }

    /** Base 50 XP, plus 25 when this player is first server-wide. */
    public static int rewardExperience(boolean firstClaim) {
        return REWARD_EXPERIENCE_POINTS + (firstClaim ? FIRST_CLAIM_BONUS_EXPERIENCE : 0);
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

    /** Lapis blocks required after {@code paymentsMade} successful diamond+lapis grants. */
    public static int lapisCost(int paymentsMade) {
        return MANA_LAPIS_COST_START + Math.max(0, paymentsMade);
    }

    public static boolean canPayManaBlocks(int diamondBlocks, int lapisBlocks) {
        return canPayManaBlocks(diamondBlocks, lapisBlocks, MANA_LAPIS_COST_START);
    }

    public static boolean canPayManaBlocks(int diamondBlocks, int lapisBlocks, int requiredLapis) {
        return diamondBlocks >= MANA_DIAMOND_COST && lapisBlocks >= Math.max(1, requiredLapis);
    }

    /** Diamond is present but lapis is short of the current requirement. */
    public static boolean shouldHintLapis(int diamondBlocks, int lapisBlocks, int requiredLapis) {
        return diamondBlocks >= MANA_DIAMOND_COST && lapisBlocks < Math.max(1, requiredLapis);
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
     * next tier first. Fully completed tracks are omitted. At most
     * {@link #SLAYER_LIST_LIMIT} slayer tracks are mixed in so they cannot
     * crowd out builder progress.
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
        collect(builders, builderCounts, builderAwarded, true, BUILDER_THRESHOLDS, claimed);
        collect(slayers, slayerCounts, slayerAwarded, false, SLAYER_THRESHOLDS, claimed);
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
        collect(builders, builderCounts, builderAwarded, true, BUILDER_THRESHOLDS, claimed);
        collect(slayers, slayerCounts, slayerAwarded, false, SLAYER_THRESHOLDS, claimed);
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
        int next = nextTarget(Math.max(0, count), BUILDER_THRESHOLDS, Math.max(0, awarded), claimed, true, id);
        if (next <= 0) {
            next = BUILDER_THRESHOLDS[BUILDER_THRESHOLDS.length - 1];
        }
        return new Progress(id, true, Math.max(0, count), next);
    }

    private static void sortByRemaining(List<Progress> tracks) {
        tracks.sort(Comparator.comparingInt(Progress::remaining)
                .thenComparing(Progress::id, String.CASE_INSENSITIVE_ORDER));
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
            int[] thresholds,
            Collection<String> claimed) {
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
            int next = nextTarget(value, thresholds, already, claimed, builder, id);
            if (next <= 0) {
                continue;
            }
            into.add(new Progress(id, builder, value, next));
        }
    }
}
