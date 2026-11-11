package dev.extrahardmode.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    /** Added to that wait for each bounty this resident has already finished with this player. */
    public static final int COOLDOWN_STEP_DAYS = 3;
    /** Accepted hunts plus bounties waiting for their emerald payout. A named bounty still waiting for its fee does not count. */
    public static final int MAX_OPEN_MISSIONS = 5;
    /** Emeralds in the main hand. The member names the bounty before this fee is spent. */
    public static final int DEPUTATION_FEE = 5;
    public static final int XP_FLAT = 25;
    public static final double XP_PER_EXTRA_POINT = 0.10;
    public static final int TIER_EVERY_COMPLETED = 4;
    public static final int MAX_TIER = 3;
    public static final int SPAWN_WEIGHT_FACTOR = 3;
    /** A common hostile. Bounties for mobs above this health ask for fewer of them. */
    public static final int REFERENCE_HP = 20;
    public static final String OVERWORLD = "minecraft:overworld";
    public static final String NETHER = "minecraft:the_nether";
    public static final String END = "minecraft:the_end";
    public static final int HUD_GAP = 2;
    /** Overworld night, matching the hostile night window. A full night is 10,000 ticks. */
    public static final int NIGHT_START = 13000;
    public static final int NIGHT_END = 23000;
    /** One try per second while that night lasts. */
    public static final int NIGHT_SPAWN_TICKS = 20;
    /**
     * One success per this many tries, rolled separately for each open bounty. Five hundred tries in a
     * night makes the mean 2 groups from one bounty, so 1 or 2 are the common results.
     * Several open bounties multiply the encounters.
     */
    public static final int NIGHT_SPAWN_ONE_IN = 250;
    /** The creature is placed at least this far out, and no farther than {@link #BOUNTY_SPAWN_NEAR_BLOCKS}. */
    public static final int BOUNTY_SPAWN_MIN_BLOCKS = 8;
    public static final int BOUNTY_SPAWN_NEAR_BLOCKS = 64;
    /** Half of the marks start hunting. The rest keep their usual notice range. */
    public static final int BOUNTY_CHASE_ONE_IN = 2;

    public record Mob(String id, String label, int hp, int tier, List<String> dimensions) {
        public Mob {
            dimensions = dimensions == null || dimensions.isEmpty() ? List.of(OVERWORLD) : List.copyOf(dimensions);
        }

        public Mob(String id, String label, int hp, int tier, String... dimensions) {
            this(id, label, hp, tier, List.of(dimensions));
        }

        public boolean livesIn(String dimension) {
            return dimension != null && dimensions.contains(dimension);
        }
    }

    /**
     * {@code lastCompletedTarget} and {@code completedCount} persist between hunts for this giver
     * only. Empty {@code mobId} means no active or turn-in bounty.
     * {@code giverId} is the resident's id. Older saves used the home id, which stays with the bed.
     * {@code dimension} is the world where the bounty was given. Empty means an older save, and
     * marks then use that mob's habitat.
     * {@code accepted} is false while the member has named the bounty and the fee is still unpaid.
     * Older saves omit it and load as already given.
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
            String giverName,
            long spawnCountDay,
            int spawnReplacements,
            String dimension,
            boolean accepted) {
        public Mission {
            dimension = dimension == null ? "" : dimension;
        }

        public boolean hasMob() {
            return mobId != null && !mobId.isEmpty();
        }

        public boolean active() {
            return accepted && hasMob() && !completed;
        }

        /** Named, and still waiting for the emerald fee. */
        public boolean offered() {
            return hasMob() && !completed && !accepted;
        }

        public boolean awaitingTurnIn() {
            return hasMob() && completed;
        }

        /** Kills still owed on this hunt. */
        public int remaining() {
            return Math.max(0, target - Math.max(0, kills));
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
                    giverName,
                    spawnCountDay,
                    spawnReplacements,
                    dimension,
                    accepted);
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
                    name == null ? "" : name,
                    spawnCountDay,
                    spawnReplacements,
                    dimension,
                    accepted);
        }

        /** Keep this hunt and move the ladder that survives a home swap. */
        public Mission withProgress(int nextLastCompletedTarget, int nextCompletedCount) {
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
                    Math.max(0, nextLastCompletedTarget),
                    Math.max(0, nextCompletedCount),
                    giverId,
                    giverName,
                    spawnCountDay,
                    spawnReplacements,
                    dimension,
                    accepted);
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
                    giverName,
                    spawnCountDay,
                    spawnReplacements,
                    dimension,
                    accepted);
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
                    giverName,
                    -1L,
                    0,
                    "",
                    true);
        }

        /** Advances the saved replacement count. Night spawns do not call this. */
        public Mission withReplacement(long today) {
            int used = spawnCountDay == today ? Math.max(0, spawnReplacements) + 1 : 1;
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
                    giverId,
                    giverName,
                    today,
                    used,
                    dimension,
                    accepted);
        }

        public Mission withDimension(String next) {
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
                    giverId,
                    giverName,
                    spawnCountDay,
                    spawnReplacements,
                    next,
                    accepted);
        }

        public Mission withAccepted(boolean next) {
            if (next == accepted) {
                return this;
            }
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
                    giverId,
                    giverName,
                    spawnCountDay,
                    spawnReplacements,
                    dimension,
                    next);
        }

        /** The same quarry, waiting for the fee. The 14 days start when the player pays. */
        public Mission asOffer() {
            return new Mission(
                    mobId,
                    label,
                    hp,
                    target,
                    0,
                    -1L,
                    houseScore,
                    false,
                    0,
                    0,
                    lastCompletedTarget,
                    completedCount,
                    giverId,
                    giverName,
                    -1L,
                    0,
                    dimension,
                    false);
        }

        /** The fee has been paid. The hunt starts today. */
        public Mission accept(long today) {
            return new Mission(
                    mobId,
                    label,
                    hp,
                    target,
                    kills,
                    today,
                    houseScore,
                    completed,
                    awardedXp,
                    pendingEmeralds,
                    lastCompletedTarget,
                    completedCount,
                    giverId,
                    giverName,
                    spawnCountDay,
                    spawnReplacements,
                    dimension,
                    true);
        }
    }

    public static final Mission NONE =
            new Mission("", "", 0, 0, 0, -1L, 0, false, 0, 0, 0, 0, "", "", -1L, 0, "", true);

    /**
     * Tier 0 in every dimension stays at {@link #REFERENCE_HP} or below, so the first recorded
     * hunt count is the unscaled roll. Guardians are omitted. Drowned stay.
     */
    public static final List<Mob> MOBS = List.of(
            new Mob("minecraft:zombie", "Zombie", 20, 0),
            new Mob("minecraft:skeleton", "Skeleton", 20, 0),
            new Mob("minecraft:spider", "Spider", 16, 0),
            new Mob("minecraft:creeper", "Creeper", 20, 0),
            new Mob("minecraft:slime", "Slime", 16, 0),
            new Mob("minecraft:drowned", "Drowned", 20, 0),
            new Mob("minecraft:zombified_piglin", "Zombified Piglin", 20, 0, NETHER),
            new Mob("minecraft:magma_cube", "Magma Cube", 16, 0, NETHER),
            new Mob("minecraft:endermite", "Endermite", 8, 0, END),
            new Mob("minecraft:husk", "Husk", 20, 1),
            new Mob("minecraft:stray", "Stray", 20, 1),
            new Mob("minecraft:cave_spider", "Cave Spider", 12, 1),
            new Mob("minecraft:zombie_villager", "Zombie Villager", 20, 1),
            new Mob("minecraft:silverfish", "Silverfish", 8, 1),
            new Mob("minecraft:phantom", "Phantom", 20, 1),
            new Mob("minecraft:bogged", "Bogged", 16, 1),
            new Mob("minecraft:blaze", "Blaze", 20, 1, NETHER),
            new Mob("minecraft:piglin", "Piglin", 16, 1, NETHER),
            new Mob("minecraft:ghast", "Ghast", 10, 1, NETHER),
            new Mob("minecraft:witch", "Witch", 26, 2),
            new Mob("minecraft:pillager", "Pillager", 24, 2),
            new Mob("minecraft:vindicator", "Vindicator", 24, 2),
            new Mob("minecraft:enderman", "Enderman", 40, 2, OVERWORLD, NETHER, END),
            new Mob("minecraft:breeze", "Breeze", 30, 2),
            new Mob("minecraft:wither_skeleton", "Wither Skeleton", 20, 2, NETHER),
            new Mob("minecraft:hoglin", "Hoglin", 40, 2, NETHER),
            new Mob("minecraft:shulker", "Shulker", 30, 2, END),
            new Mob("minecraft:ravager", "Ravager", 100, 3),
            new Mob("minecraft:evoker", "Evoker", 24, 3),
            new Mob("minecraft:piglin_brute", "Piglin Brute", 50, 3, NETHER));

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

    /**
     * The rolled count, reduced when this mob has more than {@link #REFERENCE_HP} health.
     * Twice the health is half as many, rounded, and at least 1. Frailer mobs keep the rolled count.
     * The growth ladder stays on the unscaled count.
     */
    public static int healthScaledCount(int base, int hp) {
        if (base <= 0) {
            return 0;
        }
        if (hp <= REFERENCE_HP) {
            return base;
        }
        return Math.max(1, (int) Math.round(base * (double) REFERENCE_HP / hp));
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
        return pickMob(completedCount, roll, OVERWORLD);
    }

    /** Mobs of this tier that live in {@code dimension}. An empty or unknown dimension is the overworld. */
    public static List<Mob> mobsAtTier(int tier, String dimension) {
        String home = dimension == null || dimension.isEmpty() ? OVERWORLD : dimension;
        List<Mob> matched = new ArrayList<>();
        for (Mob mob : mobsAtTier(tier)) {
            if (mob.livesIn(home)) {
                matched.add(mob);
            }
        }
        return matched;
    }

    /**
     * The unlocked tier, limited to this resident's dimension. When that tier has none there,
     * the nearest lower tier in that dimension is used, then a higher one.
     * A dimension with no listed mobs uses the overworld's common mobs.
     */
    public static Mob pickMob(int completedCount, int roll, String dimension) {
        String home = dimension == null || dimension.isEmpty() ? OVERWORLD : dimension;
        int unlocked = unlockedTier(completedCount);
        List<Mob> pool = mobsAtTier(unlocked, home);
        if (pool.isEmpty()) {
            for (int tier = unlocked - 1; tier >= 0; tier--) {
                pool = mobsAtTier(tier, home);
                if (!pool.isEmpty()) {
                    break;
                }
            }
        }
        if (pool.isEmpty()) {
            for (int tier = unlocked + 1; tier <= MAX_TIER; tier++) {
                pool = mobsAtTier(tier, home);
                if (!pool.isEmpty()) {
                    break;
                }
            }
        }
        if (pool.isEmpty()) {
            pool = mobsAtTier(0, OVERWORLD);
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

    /**
     * Wait after a bounty is given, before that resident may give another. Seven days, plus three for
     * each bounty already finished with this player. A hunt that expires does not add a step.
     */
    public static int cooldownDays(int completedCount) {
        int done = Math.max(0, completedCount);
        long days = (long) COOLDOWN_DAYS + (long) COOLDOWN_STEP_DAYS * done;
        if (days > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) days;
    }

    /** Days until that resident may give another bounty. Zero means the wait is over. */
    public static int daysUntilNextMission(long lastAssignedDay, long today, int completedCount) {
        if (lastAssignedDay < 0L) {
            return 0;
        }
        long wait = lastAssignedDay + cooldownDays(completedCount) - today;
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
        return daysUntilNextMission(previous.assignedDay(), today, previous.completedCount()) == 0;
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
            if (mission.accepted() && mission.hasMob()) {
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

    /** A home id is the bed position as a decimal long. A resident id is a UUID. */
    public static boolean homeKey(String giverId) {
        if (giverId == null || giverId.isEmpty()) {
            return false;
        }
        int start = giverId.charAt(0) == '-' ? 1 : 0;
        if (start >= giverId.length()) {
            return false;
        }
        for (int i = start; i < giverId.length(); i++) {
            if (!Character.isDigit(giverId.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Move this resident's ladder onto their id. The home id still belongs to them while they live
     * there. After a swap, a home id left behind is theirs when the mission carries their name and
     * the person now in that home does not. The kept slot is the one with the most finished hunts.
     */
    public static List<Mission> adoptResident(
            List<Mission> missions,
            String residentId,
            String homeId,
            String name,
            Map<String, String> occupantNames) {
        if (missions == null || missions.isEmpty() || residentId == null || residentId.isEmpty()) {
            return missions == null ? List.of() : List.copyOf(missions);
        }
        List<Mission> owned = new ArrayList<>();
        for (Mission mission : missions) {
            if (belongsToResident(mission, residentId, homeId, name, occupantNames)) {
                owned.add(mission);
            }
        }
        if (owned.isEmpty()) {
            return List.copyOf(missions);
        }
        String keptName = name == null ? "" : name;
        if (owned.size() == 1) {
            Mission only = owned.get(0);
            String onlyName = only.giverName() == null ? "" : only.giverName();
            if (residentId.equals(only.giverId()) && (keptName.isEmpty() || keptName.equals(onlyName))) {
                return List.copyOf(missions);
            }
        }
        Mission kept = mergeResident(owned, residentId, keptName);
        List<Mission> next = new ArrayList<>();
        boolean placed = false;
        for (Mission mission : missions) {
            if (!belongsToResident(mission, residentId, homeId, name, occupantNames)) {
                next.add(mission);
                continue;
            }
            if (!placed) {
                next.add(kept);
                placed = true;
            }
        }
        return List.copyOf(next);
    }

    static boolean belongsToResident(
            Mission mission,
            String residentId,
            String homeId,
            String name,
            Map<String, String> occupantNames) {
        if (mission == null || residentId == null || residentId.isEmpty()) {
            return false;
        }
        String id = mission.giverId() == null ? "" : mission.giverId();
        if (id.equals(residentId)) {
            return true;
        }
        if (!homeKey(id)) {
            return false;
        }
        String missionName = mission.giverName() == null ? "" : mission.giverName();
        String residentName = name == null ? "" : name;
        if (id.equals(homeId == null ? "" : homeId)) {
            return missionName.isEmpty() || residentName.isEmpty() || missionName.equals(residentName);
        }
        if (residentName.isEmpty() || !missionName.equals(residentName)) {
            return false;
        }
        Map<String, String> occupants = occupantNames == null ? Map.of() : occupantNames;
        String occupant = occupants.get(id);
        return occupant == null || !occupant.equals(missionName);
    }

    static Mission mergeResident(List<Mission> owned, String residentId, String name) {
        Mission best = owned.get(0);
        for (int i = 1; i < owned.size(); i++) {
            Mission mission = owned.get(i);
            if (mission.completedCount() > best.completedCount()
                    || (mission.completedCount() == best.completedCount() && mission.hasMob() && !best.hasMob())) {
                best = mission;
            }
        }
        String keptName = name == null || name.isEmpty() ? (best.giverName() == null ? "" : best.giverName()) : name;
        if (!best.hasMob()) {
            Mission live = null;
            for (Mission mission : owned) {
                if (!mission.hasMob()) {
                    continue;
                }
                if (live == null || mission.kills() > live.kills()) {
                    live = mission;
                }
            }
            if (live != null) {
                return live.withProgress(
                                Math.max(best.lastCompletedTarget(), live.lastCompletedTarget()),
                                Math.max(best.completedCount(), live.completedCount()))
                        .withGiver(residentId, keptName);
            }
        }
        return best.withGiver(residentId, keptName);
    }

    public static List<String> hudLines(List<Mission> missions, long clockTime) {
        List<String> lines = new ArrayList<>();
        if (missions == null) {
            return lines;
        }
        for (Mission mission : missions) {
            if (mission.active()) {
                long expire = mission.assignedDay() < 0L ? -1L : mission.assignedDay() + MISSION_DAYS;
                lines.add(MissionTimerRules.append(hudLabel(mission), expire, clockTime));
            }
        }
        return lines;
    }

    /** Singular at 1 remaining. Every other count uses the plural. */
    public static String mobName(String label, int count) {
        String name = label == null || label.isEmpty() ? "Mob" : label;
        if (count == 1) {
            return name;
        }
        return plural(name);
    }

    private static String plural(String name) {
        if (name.equals("Enderman")) {
            return "Endermen";
        }
        if (name.equals("Silverfish") || name.equals("Drowned") || name.equals("Bogged")) {
            return name;
        }
        int space = name.lastIndexOf(' ');
        if (space >= 0) {
            return name.substring(0, space + 1) + plural(name.substring(space + 1));
        }
        if (name.endsWith("s")
                || name.endsWith("x")
                || name.endsWith("z")
                || name.endsWith("ch")
                || name.endsWith("sh")) {
            return name + "es";
        }
        int length = name.length();
        if (length >= 2 && name.endsWith("y") && "aeiou".indexOf(Character.toLowerCase(name.charAt(length - 2))) < 0) {
            return name.substring(0, length - 1) + "ies";
        }
        return name + "s";
    }

    public static boolean expired(Mission mission, long today) {
        return mission != null && mission.active() && expired(mission.assignedDay(), today);
    }

    public static String hudLabel(String giverName, String mobLabel, int remaining) {
        int left = Math.max(0, remaining);
        String who = giverName == null || giverName.isEmpty() ? "Council" : giverName;
        return who + ": " + left + " " + mobName(mobLabel, left);
    }

    public static String hudLabel(Mission mission) {
        if (mission == null || !mission.active()) {
            return "";
        }
        return hudLabel(mission.giverName(), mission.label(), mission.remaining());
    }

    /** {@code dayTime} is an overworld clock. Night is {@code [13000, 23000)}. */
    public static boolean night(long dayTime) {
        long time = Math.floorMod(dayTime, 24000L);
        return time >= NIGHT_START && time < NIGHT_END;
    }

    /** Tries during one full night. */
    public static int nightAttempts() {
        return (NIGHT_END - NIGHT_START) / NIGHT_SPAWN_TICKS;
    }

    /** Mean groups from one open bounty over one full night. Each other open bounty adds this many. */
    public static double meanNightSpawns() {
        return nightAttempts() / (double) NIGHT_SPAWN_ONE_IN;
    }

    /** {@code roll} is {@code 0} through {@link #NIGHT_SPAWN_ONE_IN} minus 1. Only 0 succeeds. */
    public static boolean nightSpawn(int roll) {
        return Math.floorMod(roll, NIGHT_SPAWN_ONE_IN) == 0;
    }

    /**
     * Smallest group for one night appearance. Floor of one sixth of the kills still owed, and at
     * least 1 while any remain.
     */
    public static int bountyGroupMin(int remaining) {
        if (remaining <= 0) {
            return 0;
        }
        return Math.max(1, remaining / 6);
    }

    /** Largest group: floor of one third of the kills still owed, and never below the minimum. */
    public static int bountyGroupMax(int remaining) {
        if (remaining <= 0) {
            return 0;
        }
        return Math.max(bountyGroupMin(remaining), remaining / 3);
    }

    /** Inclusive roll from {@link #bountyGroupMin} through {@link #bountyGroupMax}. */
    public static int bountyGroupSize(int remaining, int roll) {
        int low = bountyGroupMin(remaining);
        int high = bountyGroupMax(remaining);
        if (high <= 0) {
            return 0;
        }
        return low + Math.floorMod(roll, high - low + 1);
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
        return assign(previous, giverId, giverName, houseScore, today, countRoll, mobRoll, OVERWORLD);
    }

    public static Mission assign(
            Mission previous,
            String giverId,
            String giverName,
            int houseScore,
            long today,
            int countRoll,
            int mobRoll,
            String dimension) {
        Mission prior = previous == null ? NONE : previous;
        String world = dimension == null || dimension.isEmpty() ? OVERWORLD : dimension;
        int base = targetCount(prior.lastCompletedTarget(), countRoll);
        Mob mob = pickMob(prior.completedCount(), mobRoll, world);
        int target = healthScaledCount(base, mob.hp());
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
                giverName == null ? "" : giverName,
                -1L,
                0,
                world,
                true);
    }

    /** True when this stack is emeralds and holds at least {@link #DEPUTATION_FEE}. */
    public static boolean holdsDeputationFee(boolean emeraldItem, int count) {
        return emeraldItem && count >= DEPUTATION_FEE;
    }

    /** Half of all rolls, including negative ones, send the mark after the player immediately. */
    public static boolean chasesOnSpawn(int roll) {
        return Math.floorMod(roll, BOUNTY_CHASE_ONE_IN) == 0;
    }

    /**
     * The sixteen fields older saves already have. A missing dimension stays empty so those
     * bounties keep using the mob's habitat.
     */
    public static Mission stored(
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
            String giverName,
            long spawnCountDay,
            int spawnReplacements) {
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
                giverId,
                giverName,
                spawnCountDay,
                spawnReplacements,
                "",
                true);
    }

    /** An open bounty sends marks only in the dimension where it was given. */
    public static boolean spawnsIn(Mission mission, String playerDimension) {
        if (mission == null || !mission.active()) {
            return false;
        }
        return spawnsIn(mission.mobId(), mission.dimension(), playerDimension);
    }

    /**
     * A stored dimension must match the player's world. An older bounty with no dimension uses
     * that mob's habitat. An unknown mob with no stored dimension does not spawn.
     */
    public static boolean spawnsIn(String mobId, String storedDimension, String playerDimension) {
        if (playerDimension == null || playerDimension.isEmpty()) {
            return false;
        }
        if (storedDimension != null && !storedDimension.isEmpty()) {
            return storedDimension.equals(playerDimension);
        }
        return mobLivesIn(mobId, playerDimension);
    }

    public static boolean mobLivesIn(String mobId, String dimension) {
        if (mobId == null || mobId.isEmpty() || dimension == null) {
            return false;
        }
        for (Mob mob : MOBS) {
            if (mob.id().equals(mobId)) {
                return mob.livesIn(dimension);
            }
        }
        return false;
    }
}
