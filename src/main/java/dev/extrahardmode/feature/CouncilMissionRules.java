package dev.extrahardmode.feature;

import java.util.ArrayList;
import java.util.List;

/**
 * Council-member kill bounties. Minecraft-free so JUnit can cover the math.
 */
public final class CouncilMissionRules {
    public static final int INITIAL_MIN = 6;
    public static final int INITIAL_MAX = 18;
    public static final double GROWTH = 1.2;
    public static final int MISSION_DAYS = 14;
    /** Days after a bounty is given before that resident may give another. The previous one must be finished. */
    public static final int COOLDOWN_DAYS = 7;
    /** Active hunts plus bounties waiting for their emerald payout. */
    public static final int MAX_OPEN_MISSIONS = 5;
    public static final int XP_FLAT = 25;
    public static final double XP_PER_EXTRA_POINT = 0.10;
    public static final int TIER_EVERY_COMPLETED = 2;
    public static final int MAX_TIER = 3;
    public static final int SPAWN_WEIGHT_FACTOR = 3;
    public static final int HUD_GAP = 2;

    public record Mob(String id, String label, int hp, int tier) {}

    /**
     * {@code lastCompletedTarget} and {@code completedCount} persist between hunts for this giver
     * only. Empty {@code mobId} means no active or turn-in bounty.
     * {@code giverId} is the home id of the council member who gave it.
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
            int completedCount,
            String giverId,
            String giverName) {
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
                    completedCount,
                    giverId,
                    giverName);
        }

        public Mission withGiver(String id, String name) {
            return new Mission(
                    mobId,
                    label,
                    hp,
                    target,
                    kills,
                    assignedDay,
                    houseScore,
                    completed,
                    awardedXp,
                    pendingEmeralds,
                    lastCompletedTarget,
                    completedCount,
                    id == null ? "" : id,
                    name == null ? "" : name);
        }

        /**
         * One success for this giver. The recorded target is this hunt's target the first time,
         * then 20% above that giver's recorded target.
         */
        public Mission asCompleted(int xp, int emeralds) {
            int done = Math.max(target, kills);
            int recorded = lastCompletedTarget <= 0 ? target : nextCount(lastCompletedTarget);
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
                    recorded,
                    completedCount + 1,
                    giverId,
                    giverName);
        }

        /** Drop the hunt; keep the giver and lifetime progress. */
        public Mission clearHunt() {
            return new Mission(
                    "",
                    "",
                    0,
                    0,
                    0,
                    assignedDay,
                    0,
                    false,
                    0,
                    0,
                    lastCompletedTarget,
                    completedCount,
                    giverId,
                    giverName);
        }
    }

    public static final Mission NONE =
            new Mission("", "", 0, 0, 0, -1L, 0, false, 0, 0, 0, 0, "", "");

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

    /** Days until that resident may give another bounty. Zero means the wait is over. */
    public static int daysUntilNextMission(long lastAssignedDay, long today) {
        if (lastAssignedDay < 0L) {
            return 0;
        }
        long wait = lastAssignedDay + COOLDOWN_DAYS - today;
        if (wait <= 0L) {
            return 0;
        }
        if (wait > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) wait;
    }

    /**
     * True when this resident has no open bounty and the wait since they last gave one is over.
     * An active hunt or an unpaid payout blocks the next one even after the wait.
     */
    public static boolean canTakeNext(Mission previous, long today) {
        if (previous == null || (!previous.hasMob() && previous.assignedDay() < 0L)) {
            return true;
        }
        if (previous.active() || previous.awaitingTurnIn()) {
            return false;
        }
        return daysUntilNextMission(previous.assignedDay(), today) == 0;
    }

    /** A saved single bounty from before residents each kept their own. */
    public static boolean worthMigrating(Mission mission) {
        if (mission == null) {
            return false;
        }
        return mission.hasMob()
                || mission.assignedDay() >= 0L
                || mission.completedCount() > 0
                || mission.lastCompletedTarget() > 0;
    }

    public static int openMissions(List<Mission> missions) {
        int open = 0;
        if (missions == null) {
            return 0;
        }
        for (Mission mission : missions) {
            if (mission.hasMob()) {
                open++;
            }
        }
        return open;
    }

    public static boolean atCap(List<Mission> missions) {
        return openMissions(missions) >= MAX_OPEN_MISSIONS;
    }

    public static Mission findGiver(List<Mission> missions, String giverId) {
        String id = giverId == null ? "" : giverId;
        if (missions == null) {
            return null;
        }
        Mission found = null;
        for (Mission mission : missions) {
            String existing = mission.giverId() == null ? "" : mission.giverId();
            if (id.equals(existing)) {
                found = mission;
            }
        }
        return found;
    }

    /** Replace the slot with this giver id, or append it. */
    public static List<Mission> put(List<Mission> missions, Mission mission) {
        List<Mission> next = new ArrayList<>();
        String id = mission == null || mission.giverId() == null ? "" : mission.giverId();
        boolean replaced = false;
        if (missions != null) {
            for (Mission existing : missions) {
                String existingId = existing.giverId() == null ? "" : existing.giverId();
                if (!replaced && id.equals(existingId)) {
                    next.add(mission);
                    replaced = true;
                } else {
                    next.add(existing);
                }
            }
        }
        if (!replaced && mission != null) {
            next.add(mission);
        }
        return List.copyOf(next);
    }

    /** Give an old unassigned bounty to the resident being talked to, when they have no slot yet. */
    public static List<Mission> claimLegacy(List<Mission> missions, String giverId, String giverName) {
        if (missions == null || missions.isEmpty() || giverId == null || giverId.isEmpty()) {
            return missions == null ? List.of() : List.copyOf(missions);
        }
        if (findGiver(missions, giverId) != null || findGiver(missions, "") == null) {
            return List.copyOf(missions);
        }
        List<Mission> next = new ArrayList<>();
        for (Mission mission : missions) {
            String existing = mission.giverId() == null ? "" : mission.giverId();
            if (existing.isEmpty()) {
                next.add(mission.withGiver(giverId, giverName));
            } else {
                next.add(mission);
            }
        }
        return List.copyOf(next);
    }

    public static List<String> hudLines(List<Mission> missions) {
        List<Mission> active = new ArrayList<>();
        if (missions != null) {
            for (Mission mission : missions) {
                if (mission.active()) {
                    active.add(mission);
                }
            }
        }
        List<String> lines = new ArrayList<>();
        boolean named = active.size() > 1;
        for (Mission mission : active) {
            String base = hudLabel(mission);
            if (named && mission.giverName() != null && !mission.giverName().isEmpty()) {
                lines.add(mission.giverName() + ": " + base);
            } else {
                lines.add(base);
            }
        }
        return lines;
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

    /** {@code indexFromBottom} 0 sits on {@link #bountyHudY}. Further lines stack upward. */
    public static int bountyHudLineY(int guiHeight, int lineHeight, int indexFromBottom, int count) {
        int bottom = bountyHudY(guiHeight, lineHeight);
        int step = Math.max(1, lineHeight) + HUD_GAP;
        int index = Math.max(0, indexFromBottom);
        if (count > 0) {
            index = Math.min(index, count - 1);
        }
        return bottom - index * step;
    }

    public static Mission assign(
            Mission previous, String giverId, String giverName, int houseScore, long today, int countRoll, int mobRoll) {
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
                prior.completedCount(),
                giverId == null ? "" : giverId,
                giverName == null ? "" : giverName);
    }
}
