package dev.extrahardmode.feature;

/**
 * A librarian sells an enchantment when that enchantment can scale to at least {@code 6 - rank}.
 * Novice sells only maximum 5. Each higher rank also sells every earlier enchantment.
 * The level on the book cannot pass the librarian's rank, so Efficiency is 1 from a novice
 * and can be 5 from a master.
 *
 * <p>Emeralds follow the vanilla book price for that level: {@code 2 + roll + 3 * level},
 * with {@code roll} in {@code 0 .. 5 + 10 * level - 1}. Treasure enchantments cost double.
 * The price is clamped to 1..64.
 */
public final class LibrarianBookRules {
    public static final int RANK_SUM = 6;
    public static final int MAX_EMERALDS = 64;
    public static final int MIN_RANK = 1;
    public static final int MAX_RANK = 5;

    private LibrarianBookRules() {}

    /** Lowest enchantment maximum this rank sells. Higher ranks also sell every larger maximum. */
    public static int requiredMaxLevel(int librarianLevel) {
        return RANK_SUM - librarianLevel;
    }

    public static boolean enchantmentAllowed(int librarianLevel, int enchantmentMaxLevel) {
        if (librarianLevel < MIN_RANK || librarianLevel > MAX_RANK || enchantmentMaxLevel <= 0) {
            return false;
        }
        return enchantmentMaxLevel >= requiredMaxLevel(librarianLevel);
    }

    /** Highest level this librarian may put on that enchantment, or 0 when it does not fit. */
    public static int levelCeiling(int librarianLevel, int enchantmentMin, int enchantmentMax) {
        int ceiling = Math.min(librarianLevel, enchantmentMax);
        int floor = Math.max(1, enchantmentMin);
        if (ceiling < floor) {
            return 0;
        }
        return ceiling;
    }

    /** Keeps a legal roll and pulls a higher one down to the ceiling. A rank that cannot sell this enchantment gets 0. */
    public static int bookLevel(int librarianLevel, int enchantmentMin, int enchantmentMax, int rolled) {
        if (!enchantmentAllowed(librarianLevel, enchantmentMax)) {
            return 0;
        }
        int ceiling = levelCeiling(librarianLevel, enchantmentMin, enchantmentMax);
        if (ceiling <= 0) {
            return 0;
        }
        int floor = Math.max(1, enchantmentMin);
        if (rolled < floor) {
            return floor;
        }
        return Math.min(rolled, ceiling);
    }

    public static int priceRollBound(int enchantmentLevel) {
        return 5 + 10 * Math.max(0, enchantmentLevel);
    }

    public static int emeraldCost(int enchantmentLevel, int roll, boolean treasure) {
        int bounded = priceRollBound(enchantmentLevel);
        int used = roll;
        if (used < 0) {
            used = 0;
        } else if (bounded > 0 && used >= bounded) {
            used = bounded - 1;
        }
        int cost = 2 + used + 3 * Math.max(0, enchantmentLevel);
        if (treasure) {
            cost *= 2;
        }
        if (cost < 1) {
            return 1;
        }
        return Math.min(MAX_EMERALDS, cost);
    }
}
