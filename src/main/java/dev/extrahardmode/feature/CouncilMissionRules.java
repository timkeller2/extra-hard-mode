package dev.extrahardmode.feature;

import java.util.ArrayList;
import java.util.List;

/**
 * Council-member kill bounties. Minecraft-free so JUnit can cover the math.
 */
public final class CouncilMissionRules {
    public static final int INITIAL_MIN = 6;
    public static final int INITIAL_MAX = 18;
    public static final double GROWTH = 1.3;
    public static final int MISSION_DAYS = 7;
    public static final int XP_FLAT = 25;
    public static final double XP_PER_EXTRA_POINT = 0.10;
    public static final int TIER_EVERY_COMPLETED = 2;
    public static final int MAX_TIER = 3;
    public static final int SPAWN_WEIGHT_FACTOR = 3;
    public static final int HUD_GAP = 2;

    public record Mob(String id, String label, int hp, int tier) {}

    /**
     * {@code lastCompletedTarget} and {@code completedCount} persist between hunts.
     * Empty {@code mobId} means no active or turn-in bounty.
     */
    public record Mission(
            String mobId,
            String label,
            int hp,
            int target,
            int kills,
            long assignedDay,
            int houseScore,
            boolean completed,
            int awardedXp,
            int pendingEmeralds,
            int lastCompletedTarget,
            int completedCount) {
        public boolean hasMob() {
            return mobId != null && !mobId.isEmpty();
        }

        public boolean active() {
            return hasMob() && !completed;
        }

        public boolean awaitingTurnIn() {
            return hasMob() && completed;
        }

        public Mission withKills(int next) {
            return new Mission(
                    mobId,
                    label,
                    hp,
                    target,
                    Math.max(0, next),
                    assignedDay,
                    houseScore,
                    completed,
                    awardedXp,
                    pendingEmeralds,
                    lastCompletedTarget,
                    completedCount);
        }

        public Mission asCompleted(int xp, int emeralds) {
            int done = Math.max(target, kills);
            return new Mission(
                    mobId,
                    label,
                    hp,
                    target,
                    done,
                    assignedDay,
                    houseScore,
                    true,
                    Math.max(0, xp),
                    Math.max(0, emeralds),
                    target,
                    completedCount + 1);
        }

        /** Drop the hunt; keep lifetime progress. */
        public Mission clearHunt() {
            return new Mission(
                    "",
                    "",
                    0,
                    0,
                    0,
                    -1L,
                    0,
                    false,
                    0,
                    0,
                    lastCompletedTarget,
                    completedCount);
        }
    }

    public static final Mission NONE =
            new Mission("", "", 0, 0, 0, -1L, 0, false, 0, 0, 0, 0);

    public static final List<Mob> MOBS = List.of(
            new Mob("minecraft:zombie", "Zombie", 20, 0),
            new Mob("minecraft:skeleton", "Skeleton", 20, 0),
            new Mob("minecraft:spider", "Spider", 16, 0),
            new Mob("minecraft:creeper", "Creeper", 20, 0),
            new Mob("minecraft:drowned", "Drowned", 20, 0),
            new Mob("minecraft:slime", "Slime", 16, 0),
            new Mob("minecraft:husk", "Husk", 20, 1),
            new Mob("minecraft:stray", "Stray", 20, 1),
            new Mob("minecraft:cave_spider", "Cave Spider", 12, 1),
            new Mob("minecraft:witch", "Witch", 26, 1),
            new Mob("minecraft:pillager", "Pillager", 24, 1),
            new Mob("minecraft:silverfish", "Silverfish", 8, 1),
            new Mob("minecraft:phantom", "Phantom", 20, 1),
            new Mob("minecraft:zombie_villager", "Zombie Villager", 20, 1),
            new Mob("minecraft:enderman", "Enderman", 40, 2),
            new Mob("minecraft:blaze", "Blaze", 20, 2),
            new Mob("minecraft:guardian", "Guardian", 30, 2),
            new Mob("minecraft:magma_cube", "Magma Cube", 16, 2),
            new Mob("minecraft:hoglin", "Hoglin", 40, 2),
            new Mob("minecraft:wither_skeleton", "Wither Skeleton", 20, 2),
            new Mob("minecraft:ghast", "Ghast", 10, 2),
            new Mob("minecraft:vindicator", "Vindicator", 24, 2),
            new Mob("minecraft:ravager", "Ravager", 100, 3),
            new Mob("minecraft:evoker", "Evoker", 24, 3),
            new Mob("minecraft:shulker", "Shulker", 30, 3),
            new Mob("minecraft:piglin_brute", "Piglin Brute", 50, 3),
            new Mob("minecraft:elder_guardian", "Elder Guardian", 80, 3));

    private CouncilMissionRules() {}

    public static int initialCount(int roll) {
        int span = INITIAL_MAX - INITIAL_MIN + 1;
        return INITIAL_MIN + Math.floorMod(roll, span);
    }

    public static int nextCount(int previousTarget) {
        if (previousTarget <= 0) {
            return INITIAL_MIN;
        }
        return Math.max(1, (int) Math.round(previousTarget * GROWTH));
    }

    public static int targetCount(int lastCompletedTarget, int roll) {
        if (lastCompletedTarget <= 0) {
            return initialCount(roll);
        }
        return nextCount(lastCompletedTarget);
    }

    public static int unlockedTier(int completedCount) {
        return Math.min(MAX_TIER, Math.max(0, completedCount) / TIER_EVERY_COMPLETED);
    }

    public static List<Mob> mobsAtTier(int tier) {
        int t = Math.max(0, Math.min(MAX_TIER, tier));
        List<Mob> matched = new ArrayList<>();
        for (Mob mob : MOBS) {
            if (mob.tier() == t) {
                matched.add(mob);
            }
        }
        return matched;
    }

    public static Mob pickMob(int completedCount, int roll) {
        List<Mob> pool = mobsAtTier(unlockedTier(completedCount));
        if (pool.isEmpty()) {
            pool = mobsAtTier(0);
        }
        return pool.get(Math.floorMod(roll, pool.size()));
    }

    public static int extraHousePoints(int houseScore) {
        return Math.max(0, houseScore - InhabitantRules.MIN_SCORE);
    }

    /** {@code (hp × kills) / 4 + 25}. */
    public static int baseXp(int hp, int kills) {
        if (hp <= 0 || kills <= 0) {
            return XP_FLAT;
        }
        return (hp * kills) / 4 + XP_FLAT;
    }

    public static int scaledXp(int baseXp, int extraPoints) {
        if (baseXp <= 0) {
            return 0;
        }
        return (int) Math.round(baseXp * (1.0 + Math.max(0, extraPoints) * XP_PER_EXTRA_POINT));
    }

    public static int completionXp(int hp, int kills, int houseScore) {
        return scaledXp(baseXp(hp, kills), extraHousePoints(houseScore));
    }

    public static int emeraldReward(int xp) {
        return Math.max(0, xp / 10);
    }

    public static boolean expired(long assignedDay, long today) {
        if (assignedDay < 0L) {
            return false;
        }
        return today >= assignedDay + MISSION_DAYS;
    }

    public static boolean expired(Mission mission, long today) {
        return mission != null && mission.active() && expired(mission.assignedDay(), today);
    }

    public static String hudLabel(String mobLabel, int kills, int target) {
        String name = mobLabel == null || mobLabel.isEmpty() ? "Mob" : mobLabel;
        return name + " Bounty " + Math.max(0, kills) + "/" + Math.max(0, target);
    }

    public static String hudLabel(Mission mission) {
        if (mission == null || !mission.active()) {
            return "";
        }
        return hudLabel(mission.label(), mission.kills(), mission.target());
    }

    /** Three times the heaviest other bag weight. */
    public static int councilBagCopies(int maxOtherWeight) {
        return SPAWN_WEIGHT_FACTOR * Math.max(1, maxOtherWeight);
    }

    public static int bountyHudY(int guiHeight, int lineHeight) {
        return AbilityDurationRules.originY(guiHeight) - Math.max(1, lineHeight) - HUD_GAP;
    }

    public static Mission assign(Mission previous, int houseScore, long today, int countRoll, int mobRoll) {
        Mission prior = previous == null ? NONE : previous;
        int target = targetCount(prior.lastCompletedTarget(), countRoll);
        Mob mob = pickMob(prior.completedCount(), mobRoll);
        return new Mission(
                mob.id(),
                mob.label(),
                mob.hp(),
                target,
                0,
                today,
                Math.max(0, houseScore),
                false,
                0,
                0,
                prior.lastCompletedTarget(),
                prior.completedCount());
    }
}
