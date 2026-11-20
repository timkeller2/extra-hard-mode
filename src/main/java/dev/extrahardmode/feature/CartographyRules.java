package dev.extrahardmode.feature;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A cartographer's mark. Minecraft-free so JUnit can cover the distance and the wait.
 * The place ignores height. Each player keeps a separate mark with each cartographer.
 */
public final class CartographyRules {
    /** Horizontal blocks that finish the mark. Height does not count. */
    public static final int ARRIVE_BLOCKS = 50;
    public static final long ARRIVE_DIST_SQR = (long) ARRIVE_BLOCKS * ARRIVE_BLOCKS;
    /** The next mark waits a Minecraft week after the first success. */
    public static final int COOLDOWN_DAYS = 7;
    /** Each further success adds this many days to that wait. */
    public static final int COOLDOWN_STEP_DAYS = 3;
    /** An unfinished mark expires after two Minecraft weeks. */
    public static final int EXPIRE_DAYS = 14;
    public static final int BLOCKS_PER_POINT = 25;
    /** Added to the house points before the distance is scaled. Three dice from 1 to 6. */
    public static final int DICE = 3;
    public static final int DIE_FACES = 6;
    /** Each finished mark adds this share of the base distance. */
    public static final double GROWTH = 0.30;
    /** The grown distance then swings by up to this share, either way. */
    public static final double SWING = 0.25;
    public static final int XP_PER_BLOCKS = 10;
    public static final int EMERALDS_PER_BLOCKS = 100;

    private CartographyRules() {}

    /**
     * Days after this many finished marks before that cartographer marks another place.
     * The first finish waits {@link #COOLDOWN_DAYS}. Each further finish adds {@link #COOLDOWN_STEP_DAYS}.
     * Zero finished marks do not impose a wait.
     */
    public static int cooldownDays(int completedCount) {
        int done = Math.max(0, completedCount);
        if (done <= 0) {
            return 0;
        }
        long days = (long) COOLDOWN_DAYS + (long) COOLDOWN_STEP_DAYS * (done - 1L);
        if (days > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) days;
    }

    /** One face of a six-sided die. {@code roll} may be any int. */
    public static int dieFace(int roll) {
        return Math.floorMod(roll, DIE_FACES) + 1;
    }

    /** Three dice, each from 1 to 6. */
    public static int threeD6(int first, int second, int third) {
        return dieFace(first) + dieFace(second) + dieFace(third);
    }

    /**
     * Horizontal blocks from the cartographer to the mark.
     * {@code (house points + dice) × 25}, plus {@link #GROWTH} of that for each finished mark,
     * then {@code swing} from -1 to 1 applies up to {@link #SWING} of the grown distance.
     * The result is rounded to the nearest block and is at least 1.
     */
    public static int distance(int housePoints, int completed, int diceSum, double swing) {
        int points = Math.max(0, housePoints);
        int dice = Math.max(0, diceSum);
        int done = Math.max(0, completed);
        double raw = (points + (double) dice) * BLOCKS_PER_POINT;
        double grown = raw * (1.0 + GROWTH * done);
        double fraction = Math.clamp(swing, -1.0, 1.0) * SWING;
        long rounded = Math.round(grown * (1.0 + fraction));
        if (rounded < 1L) {
            return 1;
        }
        if (rounded > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) rounded;
    }

    /** Experience for reaching a mark placed {@code distance} blocks out. One point per 10 blocks. */
    public static int experience(int distance) {
        if (distance <= 0) {
            return 0;
        }
        return distance / XP_PER_BLOCKS;
    }

    /** Emeralds for that same distance. One emerald per 100 blocks. */
    public static int emeralds(int distance) {
        if (distance <= 0) {
            return 0;
        }
        return distance / EMERALDS_PER_BLOCKS;
    }

    /**
     * A block at {@code distance} from the origin. Angle 0 is East and 90 is South.
     * Rounding can leave the block a step off the exact distance. The reward uses the planned distance.
     */
    public static int[] place(int originX, int originZ, int distance, int angleDegrees) {
        double rad = Math.toRadians(Math.floorMod(angleDegrees, 360));
        int blocks = Math.max(0, distance);
        int x = originX + (int) Math.round(Math.cos(rad) * blocks);
        int z = originZ + (int) Math.round(Math.sin(rad) * blocks);
        return new int[] {x, z};
    }

    public static boolean arrived(int px, int pz, int tx, int tz) {
        long dx = (long) tx - px;
        long dz = (long) tz - pz;
        return dx * dx + dz * dz <= ARRIVE_DIST_SQR;
    }

    /**
     * Furthest horizontal axis. Positive x is East and positive z is South.
     * A tie prefers East or West. Height is ignored.
     */
    public static String direction(int dx, int dz) {
        int ax = Math.abs(dx);
        int az = Math.abs(dz);
        if (ax == 0 && az == 0) {
            return "Here";
        }
        if (ax >= az) {
            return dx > 0 ? "East" : "West";
        }
        return dz > 0 ? "South" : "North";
    }

    public static String markLine(String name, String direction) {
        String who = name == null || name.isBlank() ? "Cartographer" : name.trim();
        String way = direction == null || direction.isBlank() ? "Here" : direction;
        return who + ": " + way;
    }

    public static List<String> hudLines(
            List<Mission> missions, String dimension, int px, int pz, long today, long clockTime) {
        if (missions == null || missions.isEmpty()) {
            return List.of();
        }
        List<Mission> active = new ArrayList<>();
        for (Mission mission : missions) {
            if (mission == null || !mission.active()) {
                continue;
            }
            if (mission.expireDay() >= 0L && today >= mission.expireDay()) {
                continue;
            }
            if (dimension == null || !dimension.equals(mission.dimension())) {
                continue;
            }
            if (arrived(px, pz, mission.x(), mission.z())) {
                continue;
            }
            active.add(mission);
        }
        active.sort(Comparator.comparing(Mission::giverName, Comparator.nullsFirst(String::compareTo))
                .thenComparing(Mission::giverId, Comparator.nullsFirst(String::compareTo)));
        List<String> lines = new ArrayList<>(active.size());
        for (Mission mission : active) {
            lines.add(MissionTimerRules.append(
                    markLine(mission.giverName(), direction(mission.x() - px, mission.z() - pz)),
                    mission.expireDay(),
                    clockTime));
        }
        return List.copyOf(lines);
    }

    /**
     * One player's mark from one cartographer. An empty dimension means no mark is open.
     * {@code successes} is how many marks this player has finished with him.
     * {@code nextDay} is the first day another mark may be given. Negative means no wait.
     */
    public record Mission(
            String giverId,
            String giverName,
            int x,
            int z,
            int distance,
            String dimension,
            long givenDay,
            long expireDay,
            long nextDay,
            int successes) {
        public Mission {
            giverId = giverId == null ? "" : giverId;
            giverName = giverName == null ? "" : giverName;
            dimension = dimension == null ? "" : dimension;
            distance = Math.max(0, distance);
            successes = Math.max(0, successes);
        }

        public static Mission blank(String giverId, String giverName) {
            return new Mission(giverId, giverName, 0, 0, 0, "", -1L, -1L, -1L, 0);
        }

        public boolean active() {
            return expireDay >= 0L && !dimension.isEmpty();
        }

        public Mission named(String giverId, String giverName) {
            return new Mission(giverId, giverName, x, z, distance, dimension, givenDay, expireDay, nextDay, successes);
        }

        public Mission assigned(int x, int z, int distance, String dimension, long today) {
            return new Mission(
                    giverId,
                    giverName,
                    x,
                    z,
                    distance,
                    dimension == null ? "" : dimension,
                    today,
                    today + EXPIRE_DAYS,
                    nextDay,
                    successes);
        }

        /** The player reached the place. The next mark waits from today. */
        public Mission completed(long today) {
            if (!active()) {
                return this;
            }
            int done = successes + 1;
            return new Mission(giverId, giverName, 0, 0, 0, "", givenDay, -1L, today + cooldownDays(done), done);
        }

        /** Two weeks passed. The mark is not finished, so the wait does not grow. */
        public Mission expired() {
            if (!active()) {
                return this;
            }
            return new Mission(giverId, giverName, 0, 0, 0, "", givenDay, -1L, nextDay, successes);
        }
    }
}
