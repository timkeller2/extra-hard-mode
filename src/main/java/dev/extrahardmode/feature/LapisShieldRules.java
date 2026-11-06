package dev.extrahardmode.feature;

/**
 * Amethyst Shield absorb math. Minecraft-free so the cost can be tested without a world.
 * The ability id is still {@code lapis_shield}. Amethyst shards, lapis lazuli, and whole
 * mana pay for blocked damage. How many of those you have sets the rate before any is spent.
 */
public final class LapisShieldRules {
    /** Float noise below this does not count as a blocked point. */
    public static final float COST_EPSILON = 0.0001F;
    /** One fuel type on hand: one unit covers this many points. A remainder rounds up. */
    public static final int ONE_KIND_POINTS = 3;
    /** Two fuel types on hand. */
    public static final int TWO_KIND_POINTS = 5;
    /** Amethyst, lapis, and at least 1 mana all on hand. */
    public static final int THREE_KIND_POINTS = 7;

    private LapisShieldRules() {}

    /** Damage removed before armor and health. Never more than the hit or the ability level. */
    public static float absorb(float damage, float level) {
        if (!(damage > 0.0F) || !(level > 0.0F)) {
            return 0.0F;
        }
        return Math.min(damage, level);
    }

    /**
     * True when this hit does not exceed the shield level, including a hit of zero.
     * Harmful effects that ride along with that hit are skipped only after the block is paid for.
     */
    public static boolean blocksHarmfulEffects(float damage, float level) {
        if (damage < 0.0F || level < 0.0F) {
            return false;
        }
        return damage <= level + COST_EPSILON;
    }

    /** Points one unit pays for when this many fuel types are on hand. Zero when none are. */
    public static int pointsPerFuel(int kinds) {
        return switch (kinds) {
            case 1 -> ONE_KIND_POINTS;
            case 2 -> TWO_KIND_POINTS;
            case 3 -> THREE_KIND_POINTS;
            default -> 0;
        };
    }

    /**
     * Units spent for {@code absorbed} damage at the rate for {@code kinds}.
     * An exact multiple stays on that multiple. Nothing blocked costs 0.
     */
    public static int fuelCost(float absorbed, int kinds) {
        int per = pointsPerFuel(kinds);
        if (per <= 0 || !(absorbed > COST_EPSILON)) {
            return 0;
        }
        return (int) Math.ceil((absorbed - COST_EPSILON) / per);
    }

    /** Whole mana that can pay. 0.9 counts as none. 2.7 counts as 2. */
    public static int manaUnits(double mana) {
        if (!Double.isFinite(mana) || mana < 1.0) {
            return 0;
        }
        return (int) Math.floor(mana);
    }

    /**
     * What to take for {@code absorbed} damage.
     * Kinds are amethyst shards, lapis lazuli, and whole mana, each counted only at 1 or more.
     * The rate is fixed from those kinds before anything is spent.
     * Payment starts with the largest amount and then cycles one unit at a time.
     * Equal amounts go amethyst, then lapis, then mana. Mana's amount is the current mana, not the floored units.
     * A kind that runs out is skipped. Returns null when the fuels cannot cover the cost, and nothing should be spent.
     */
    public static Payment plan(float absorbed, int amethyst, int lapis, double mana) {
        int amy = Math.max(0, amethyst);
        int lap = Math.max(0, lapis);
        int manaWhole = manaUnits(mana);
        int kinds = (amy >= 1 ? 1 : 0) + (lap >= 1 ? 1 : 0) + (manaWhole >= 1 ? 1 : 0);
        int cost = fuelCost(absorbed, kinds);
        if (cost <= 0) {
            return null;
        }
        if (amy + lap + manaWhole < cost) {
            return null;
        }
        double manaAmount = Double.isFinite(mana) ? mana : 0.0;
        FuelStock[] stocks = new FuelStock[kinds];
        int n = 0;
        if (amy >= 1) {
            stocks[n++] = new FuelStock(0, amy, amy);
        }
        if (lap >= 1) {
            stocks[n++] = new FuelStock(1, lap, lap);
        }
        if (manaWhole >= 1) {
            stocks[n++] = new FuelStock(2, manaAmount, manaWhole);
        }
        sortByAmount(stocks, n);
        int[] left = new int[n];
        for (int i = 0; i < n; i++) {
            left[i] = stocks[i].units;
        }
        int[] spent = new int[3];
        int cursor = 0;
        for (int remaining = cost; remaining > 0; remaining--) {
            int pick = nextStock(left, cursor);
            if (pick < 0) {
                return null;
            }
            left[pick]--;
            spent[stocks[pick].kind]++;
            cursor = (pick + 1) % n;
        }
        return new Payment(spent[0], spent[1], spent[2]);
    }

    /** Hit left after a successful absorb. A zero absorb leaves the hit unchanged. */
    public static float damageAfterAbsorb(float damage, float absorbed) {
        if (!(absorbed > 0.0F)) {
            return damage;
        }
        float left = damage - absorbed;
        return left > 0.0F ? left : 0.0F;
    }

    /** Amethyst shards, lapis lazuli, and whole mana taken for one block. */
    public record Payment(int amethyst, int lapis, int mana) {
        public int total() {
            return amethyst + lapis + mana;
        }
    }

    private record FuelStock(int kind, double amount, int units) {}

    /** Largest amount first. Equal amounts keep amethyst, then lapis, then mana. */
    private static void sortByAmount(FuelStock[] stocks, int n) {
        for (int i = 1; i < n; i++) {
            FuelStock stock = stocks[i];
            int j = i;
            while (j > 0 && before(stock, stocks[j - 1])) {
                stocks[j] = stocks[j - 1];
                j--;
            }
            stocks[j] = stock;
        }
    }

    private static boolean before(FuelStock left, FuelStock right) {
        int byAmount = Double.compare(left.amount, right.amount);
        if (byAmount != 0) {
            return byAmount > 0;
        }
        return left.kind < right.kind;
    }

    /** Next stock at or after {@code cursor} that still has a unit. -1 when every stock is empty. */
    private static int nextStock(int[] left, int cursor) {
        for (int step = 0; step < left.length; step++) {
            int index = (cursor + step) % left.length;
            if (left[index] > 0) {
                return index;
            }
        }
        return -1;
    }
}
