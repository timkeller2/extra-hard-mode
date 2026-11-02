package dev.extrahardmode.feature;

import java.util.ArrayList;
import java.util.List;

/**
 * Wise-teacher hunts. Minecraft-free so JUnit can cover the math.
 * A pack is offered every 30 Minecraft days. An unfinished hunt ends after 14.
 * Size is a 3–8 base from the home and the resident's trading experience,
 * times 1 plus services already taken.
 */
public final class WiseLessonRules {
    public static final int INTERVAL_DAYS = 30;
    /** An unfinished hunt ends this many Minecraft days after it is given. */
    public static final int MISSION_DAYS = 14;
    public static final int PERSIST_DAYS = 3;
    public static final int BASE_MIN = 3;
    public static final int BASE_MAX = 8;
    public static final int MIN_DISTANCE = 200;
    public static final int MAX_DISTANCE = 500;
    /** Two real minutes. The player loop adds {@link #TICKS_PER_SECOND} once a second. */
    public static final int DWELL_TICKS = 2400;
    public static final int TICKS_PER_SECOND = 20;
    public static final int SPAWN_WAIT_MIN = 100;
    public static final int SPAWN_WAIT_MAX = 300;
    /** Opening half of the pack, counted from the first creature, finishes inside this many seconds. */
    public static final int BURST_SECONDS = 3;
    /** Added with ADD_VALUE. 1 is immune to knockback. */
    public static final double LEAD_KNOCKBACK_RESISTANCE = 0.8;
    public static final int SPAWN_RADIUS_MIN = 20;
    public static final int SPAWN_RADIUS_MAX = 40;
    public static final int TIER_EVERY = 3;
    public static final int MAX_TIER = 3;
    public static final int XP_FACTOR = 3;
    /** Ordinary marks. 50 is 1.5 times health and 100 is twice, added with ADD_MULTIPLIED_BASE. */
    public static final int MARK_HEALTH_MIN_PERCENT = 50;
    public static final int MARK_HEALTH_MAX_PERCENT = 100;
    /** The first creature of a hunt. Later creatures keep the ordinary factors, including the health band. */
    public static final double LEAD_SCALE = 1.25;
    public static final int LEAD_HEALTH_FACTOR = 5;
    public static final int LEAD_XP_FACTOR = 10;
    public static final int LOOT_ROLLS = 2;
    public static final int LEAD_LOOT_ROLLS = 3;
    public static final int LEAD_EMERALD_MIN_PERCENT = 10;
    public static final int LEAD_EMERALD_MAX_PERCENT = 20;
    public static final int NIGHT_START = 13000;
    public static final int NIGHT_END = 23000;
    public static final String NEAR = "near here";

    public record Mob(String id, String plural, int tier) {}

    /** One hunt from one teacher. {@code mobs} entries are {@code uuid|expireDay}. */
    public record Lesson(
            String teacherId,
            String teacherName,
            String dimension,
            double originX,
            double originZ,
            String mobId,
            String plural,
            int size,
            int distance,
            String direction,
            long assignedDay,
            int spawned,
            int dwellTicks,
            int spawnWait,
            boolean active,
            List<String> mobs) {
        public Lesson withDwell(int dwellTicks, int spawnWait) {
            if (this.dwellTicks == dwellTicks && this.spawnWait == spawnWait) {
                return this;
            }
            return new Lesson(
                    teacherId,
                    teacherName,
                    dimension,
                    originX,
                    originZ,
                    mobId,
                    plural,
                    size,
                    distance,
                    direction,
                    assignedDay,
                    spawned,
                    dwellTicks,
                    spawnWait,
                    active,
                    mobs);
        }

        public Lesson withSpawn(int spawned, int spawnWait, List<String> mobs, boolean active) {
            return new Lesson(
                    teacherId,
                    teacherName,
                    dimension,
                    originX,
                    originZ,
                    mobId,
                    plural,
                    size,
                    distance,
                    direction,
                    assignedDay,
                    spawned,
                    dwellTicks,
                    spawnWait,
                    active,
                    mobs == null ? List.of() : List.copyOf(mobs));
        }

        public Lesson withActive(boolean active) {
            if (this.active == active) {
                return this;
            }
            return new Lesson(
                    teacherId,
                    teacherName,
                    dimension,
                    originX,
                    originZ,
                    mobId,
                    plural,
                    size,
                    distance,
                    direction,
                    assignedDay,
                    spawned,
                    dwellTicks,
                    spawnWait,
                    active,
                    mobs);
        }

        /** Drop one tracked creature. A delivered pack with none left is finished. */
        public Lesson withoutMob(String uuid) {
            if (uuid == null || uuid.isEmpty() || mobs == null || mobs.isEmpty()) {
                return this;
            }
            List<String> kept = new ArrayList<>();
            boolean removed = false;
            for (String raw : mobs) {
                Mark mark = parseMark(raw);
                if (mark != null && uuid.equals(mark.uuid())) {
                    removed = true;
                    continue;
                }
                kept.add(raw);
            }
            if (!removed) {
                return this;
            }
            boolean nextActive = active && !(spawned >= size && kept.isEmpty());
            return withSpawn(spawned, spawnWait, kept, nextActive);
        }
    }

    /** One second of travel. {@code attemptSpawn} is true only when a creature should be placed now. */
    public record Second(Lesson lesson, boolean attemptSpawn) {}

    public record Mark(String uuid, long expireDay) {}

    public static final List<Mob> MOBS = List.of(
            new Mob("minecraft:zombie", "zombies", 0),
            new Mob("minecraft:skeleton", "skeletons", 0),
            new Mob("minecraft:spider", "spiders", 0),
            new Mob("minecraft:creeper", "creepers", 0),
            new Mob("minecraft:husk", "husks", 1),
            new Mob("minecraft:stray", "strays", 1),
            new Mob("minecraft:cave_spider", "cave spiders", 1),
            new Mob("minecraft:witch", "witches", 1),
            new Mob("minecraft:pillager", "pillagers", 1),
            new Mob("minecraft:zombie_villager", "zombie villagers", 1),
            new Mob("minecraft:enderman", "endermen", 2),
            new Mob("minecraft:blaze", "blazes", 2),
            new Mob("minecraft:wither_skeleton", "wither skeletons", 2),
            new Mob("minecraft:vindicator", "vindicators", 2),
            new Mob("minecraft:ravager", "ravagers", 3),
            new Mob("minecraft:evoker", "evokers", 3),
            new Mob("minecraft:piglin_brute", "piglin brutes", 3));

    private static final List<String> DIRECTIONS = List.of("north", "south", "east", "west");

    private WiseLessonRules() {}

    /** House points plus trading experience, mapped from a 24-point home to a 38-point home. */
    public static int baseSize(double stockScore) {
        if (stockScore <= InhabitantRules.NOTABLE_SCORE) {
            return BASE_MIN;
        }
        if (stockScore >= InhabitantRules.MAX_HOUSE_SCORE) {
            return BASE_MAX;
        }
        double span = InhabitantRules.MAX_HOUSE_SCORE - InhabitantRules.NOTABLE_SCORE;
        double t = (stockScore - InhabitantRules.NOTABLE_SCORE) / span;
        return BASE_MIN + (int) Math.round(t * (BASE_MAX - BASE_MIN));
    }

    /** One for the ability and one for the mana raise. The product is not clamped. */
    public static int serviceUses(boolean taughtAbility, boolean tookMana) {
        return (taughtAbility ? 1 : 0) + (tookMana ? 1 : 0);
    }

    public static int packSize(double stockScore, int serviceUses) {
        return baseSize(stockScore) * (1 + Math.max(0, serviceUses));
    }

    public static int distance(int roll) {
        int span = MAX_DISTANCE - MIN_DISTANCE + 1;
        return MIN_DISTANCE + Math.floorMod(roll, span);
    }

    public static String direction(int roll) {
        return DIRECTIONS.get(Math.floorMod(roll, DIRECTIONS.size()));
    }

    public static int spawnInterval(int roll) {
        int span = SPAWN_WAIT_MAX - SPAWN_WAIT_MIN + 1;
        return SPAWN_WAIT_MIN + Math.floorMod(roll, span);
    }

    public static int spawnRadius(int roll) {
        int span = SPAWN_RADIUS_MAX - SPAWN_RADIUS_MIN + 1;
        return SPAWN_RADIUS_MIN + Math.floorMod(roll, span);
    }

    /** Day time in [13000, 23000). Dimensions without a sun are handled by the caller. */
    public static boolean nightTime(long dayTime) {
        long time = Math.floorMod(dayTime, 24000L);
        return time >= NIGHT_START && time < NIGHT_END;
    }

    /** On land, and either night or under a roof of stone. Water only pauses the spawn clock. */
    public static boolean spawnGate(boolean onLand, boolean night, boolean belowGround) {
        return onLand && (night || belowGround);
    }

    public static boolean offerDue(long assignedDay, long today) {
        if (assignedDay < 0L) {
            return true;
        }
        return today >= assignedDay + INTERVAL_DAYS;
    }

    /** Day window only. A negative assigned day has not been given. */
    public static boolean expired(long assignedDay, long today) {
        if (assignedDay < 0L) {
            return false;
        }
        return today >= assignedDay + MISSION_DAYS;
    }

    /** A finished hunt is not expired. Assigned day is left for the 30-day wait. */
    public static boolean expired(Lesson lesson, long today) {
        return lesson != null && lesson.active() && expired(lesson.assignedDay(), today);
    }

    /** Deactivate an unfinished hunt whose 14 days are up. Otherwise the same lesson. */
    public static Lesson endIfExpired(Lesson lesson, long today) {
        if (!expired(lesson, today)) {
            return lesson;
        }
        return lesson.withActive(false);
    }

    public static int unlockedTier(int givenCount) {
        return Math.min(MAX_TIER, Math.max(0, givenCount) / TIER_EVERY);
    }

    public static List<Mob> mobsAtTier(int tier) {
        int clamped = Math.max(0, Math.min(MAX_TIER, tier));
        List<Mob> matched = new ArrayList<>();
        for (Mob mob : MOBS) {
            if (mob.tier() == clamped) {
                matched.add(mob);
            }
        }
        return matched;
    }

    /** {@code givenCount} is how many hunts this player has already been given, before this one. */
    public static Mob pickMob(int givenCount, int roll) {
        List<Mob> pool = mobsAtTier(unlockedTier(givenCount));
        if (pool.isEmpty()) {
            pool = mobsAtTier(0);
        }
        return pool.get(Math.floorMod(roll, pool.size()));
    }

    public static boolean atRange(
            String missionDimension,
            String playerDimension,
            double originX,
            double originZ,
            double playerX,
            double playerZ,
            int distance) {
        if (missionDimension == null || playerDimension == null || !missionDimension.equals(playerDimension)) {
            return false;
        }
        double dx = playerX - originX;
        double dz = playerZ - originZ;
        return Math.hypot(dx, dz) >= Math.max(0, distance);
    }

    public static String statusLine(String nameplate, int size, String plural, String direction, boolean near) {
        String who = nameplate == null || nameplate.isBlank() ? "Traveler" : nameplate;
        String creatures = plural == null || plural.isBlank() ? "creatures" : plural;
        String where = near ? NEAR : (direction == null || direction.isBlank() ? "north" : direction);
        return who + ": " + Math.max(0, size) + " " + creatures + " left " + where;
    }

    /** Pack size minus creatures already removed. Unspawned marks still count. */
    public static int marksLeft(Lesson lesson) {
        if (lesson == null) {
            return 0;
        }
        int living = lesson.mobs() == null ? 0 : lesson.mobs().size();
        int gone = Math.max(0, lesson.spawned() - living);
        return Math.max(0, lesson.size() - gone);
    }

    public static List<String> hudLines(List<Lesson> lessons, String playerDimension, double x, double z) {
        if (lessons == null || lessons.isEmpty()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        for (Lesson lesson : lessons) {
            if (lesson == null || !lesson.active()) {
                continue;
            }
            boolean near = atRange(
                    lesson.dimension(), playerDimension, lesson.originX(), lesson.originZ(), x, z, lesson.distance());
            lines.add(statusLine(
                    lesson.teacherName(), marksLeft(lesson), lesson.plural(), lesson.direction(), near));
        }
        return lines;
    }

    /** Half the pack, at least one when the pack exists. The rest keep the 5 to 15 second gap. */
    public static int burstCount(int size) {
        if (size <= 0) {
            return 0;
        }
        return Math.max(1, size / 2);
    }

    /**
     * How many creatures to place on this spawn second. The opening half is split across
     * {@link #BURST_SECONDS} seconds starting with the first. Later seconds place one.
     */
    public static int spawnsThisSecond(int alreadySpawned, int size) {
        int total = Math.max(0, size);
        int spawned = Math.max(0, alreadySpawned);
        if (spawned >= total) {
            return 0;
        }
        int burst = Math.min(burstCount(total), total);
        if (spawned >= burst) {
            return 1;
        }
        int base = burst / BURST_SECONDS;
        int extra = burst % BURST_SECONDS;
        int cursor = 0;
        for (int second = 0; second < BURST_SECONDS; second++) {
            int wave = base + (second < extra ? 1 : 0);
            int end = cursor + wave;
            if (spawned < end) {
                return end - spawned;
            }
            cursor = end;
        }
        return 1;
    }

    /** One second while the opening half is still arriving. Otherwise the usual 5 to 15 seconds. */
    public static int retryWait(int spawned, int size, int rolledWait) {
        if (spawned > 0 && spawned < burstCount(size)) {
            return TICKS_PER_SECOND;
        }
        return spawnInterval(rolledWait);
    }

    /** True for the creature spawned while none of this hunt have spawned yet. */
    public static boolean leadSpawn(int alreadySpawned) {
        return alreadySpawned <= 0;
    }

    public static int xpFactor(boolean lead) {
        return lead ? LEAD_XP_FACTOR : XP_FACTOR;
    }

    public static int lootRolls(boolean lead) {
        return lead ? LEAD_LOOT_ROLLS : LOOT_ROLLS;
    }

    /** Added with ADD_MULTIPLIED_BASE so max health becomes {@link #LEAD_HEALTH_FACTOR} times the base. */
    public static double leadHealthBonus() {
        return LEAD_HEALTH_FACTOR - 1.0;
    }

    /**
     * A mark gives off red mist when its owner can see it, still has saturation from a golden carrot,
     * or Sense Evil is revealing marks.
     */
    public static boolean showMist(boolean lineOfSight, boolean goldenCarrotArmed, boolean senseEvilActive) {
        return lineOfSight || goldenCarrotArmed || senseEvilActive;
    }

    /** True while the Sense Evil mist window has not reached {@code now}. */
    public static boolean senseOutlineActive(long until, long now) {
        return until > now;
    }

    /**
     * Ordinary-mark health bonus, from {@link #MARK_HEALTH_MIN_PERCENT} through
     * {@link #MARK_HEALTH_MAX_PERCENT} inclusive. {@code roll} may be any int.
     */

    public static double markHealthBonus(int roll) {
        int span = MARK_HEALTH_MAX_PERCENT - MARK_HEALTH_MIN_PERCENT + 1;
        int percent = MARK_HEALTH_MIN_PERCENT + Math.floorMod(roll, span);
        return percent / 100.0;
    }

    /** Added with ADD_MULTIPLIED_BASE so the creature is {@link #LEAD_SCALE} times normal size. */
    public static double leadScaleBonus() {
        return LEAD_SCALE - 1.0;
    }

    /** Added with ADD_VALUE on top of whatever knockback resistance the creature already has. */
    public static double leadKnockbackResistance() {
        return LEAD_KNOCKBACK_RESISTANCE;
    }

    public static int scaledXp(int xp) {
        return scaledXp(xp, false);
    }

    public static int scaledXp(int xp, boolean lead) {
        if (xp <= 0) {
            return 0;
        }
        return xp * xpFactor(lead);
    }

    public static int wholeHealth(double health) {
        if (health <= 0.0 || Double.isNaN(health)) {
            return 0;
        }
        return (int) Math.round(health);
    }

    /** Rounded share of health. 10% of 100 is 10. */
    public static int healthShare(int health, int percent) {
        if (health <= 0 || percent <= 0) {
            return 0;
        }
        return (int) Math.round(health * (percent / 100.0));
    }

    /**
     * Emeralds for the first creature: a whole number from 10% through 20% of its health, inclusive.
     * {@code roll} may be any int.
     */
    public static int leadEmeralds(int finalHealth, int roll) {
        int min = healthShare(finalHealth, LEAD_EMERALD_MIN_PERCENT);
        int max = healthShare(finalHealth, LEAD_EMERALD_MAX_PERCENT);
        if (max < min) {
            max = min;
        }
        if (min == max) {
            return min;
        }
        return min + Math.floorMod(roll, max - min + 1);
    }

    public static long expireDay(long spawnDay) {
        return spawnDay + PERSIST_DAYS;
    }

    public static String mark(String uuid, long expireDay) {
        return uuid + "|" + expireDay;
    }

    public static Mark parseMark(String raw) {
        if (raw == null) {
            return null;
        }
        int bar = raw.lastIndexOf('|');
        if (bar <= 0 || bar >= raw.length() - 1) {
            return null;
        }
        try {
            return new Mark(raw.substring(0, bar), Long.parseLong(raw.substring(bar + 1)));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * One real second. Out of range clears the wait. In range, the two minutes keep
     * counting even in water. Spawning waits until that time is up, then 5 to 15 seconds,
     * and only while {@code spawnGate} is true. The interval freezes when the gate is shut.
     * After the first creature, half the pack finishes within a few seconds. The rest
     * keep the 5 to 15 second gap.
     */
    public static Second afterSecond(Lesson lesson, boolean inRange, boolean spawnGate, int rolledWait) {
        if (lesson == null || !lesson.active()) {
            return new Second(lesson, false);
        }
        if (lesson.spawned() >= lesson.size()) {
            if (!inRange && (lesson.dwellTicks() != 0 || lesson.spawnWait() != -1)) {
                return new Second(lesson.withDwell(0, -1), false);
            }
            return new Second(lesson, false);
        }
        if (!inRange) {
            if (lesson.dwellTicks() == 0 && lesson.spawnWait() < 0) {
                return new Second(lesson, false);
            }
            return new Second(lesson.withDwell(0, -1), false);
        }
        int dwell = Math.min(DWELL_TICKS, lesson.dwellTicks() + TICKS_PER_SECOND);
        boolean finishedNow = lesson.dwellTicks() < DWELL_TICKS && dwell >= DWELL_TICKS;
        if (finishedNow) {
            return new Second(lesson.withDwell(dwell, spawnInterval(rolledWait)), false);
        }
        if (dwell < DWELL_TICKS) {
            if (dwell == lesson.dwellTicks()) {
                return new Second(lesson, false);
            }
            return new Second(lesson.withDwell(dwell, lesson.spawnWait()), false);
        }
        if (!spawnGate) {
            return new Second(lesson, false);
        }
        int wait = lesson.spawnWait();
        if (wait < 0) {
            return new Second(lesson.withDwell(dwell, spawnInterval(rolledWait)), false);
        }
        int next = wait - TICKS_PER_SECOND;
        if (next > 0) {
            return new Second(lesson.withDwell(DWELL_TICKS, next), false);
        }
        return new Second(lesson.withDwell(DWELL_TICKS, 0), true);
    }

    /** After a spawn attempt. Failure spends no creature. The opening half retries in one second. */
    public static Lesson afterAttempt(Lesson lesson, boolean spawnedOne, String mark, int rolledWait) {
        if (lesson == null) {
            return null;
        }
        if (!spawnedOne) {
            return lesson.withDwell(lesson.dwellTicks(), retryWait(lesson.spawned(), lesson.size(), rolledWait));
        }
        List<String> mobs = new ArrayList<>(lesson.mobs() == null ? List.of() : lesson.mobs());
        if (mark != null && !mark.isEmpty()) {
            mobs.add(mark);
        }
        int spawned = lesson.spawned() + 1;
        return lesson.withSpawn(spawned, retryWait(spawned, lesson.size(), rolledWait), mobs, lesson.active());
    }
}
