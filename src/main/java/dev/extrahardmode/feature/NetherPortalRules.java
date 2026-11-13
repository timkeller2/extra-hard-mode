package dev.extrahardmode.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Nether portal fuel, the per-player wait, and the overworld backlash.
 * Minecraft-free so JUnit can cover it.
 *
 * <p>Fuel is redstone blocks, quartz blocks, and amethyst blocks. One kind covers
 * 1 point of weight per block, two kinds cover 2, and all three cover 4.
 * Payment is {@link LapisPortalRules#pay}. Leaving the overworld rolls a d20.
 * A roll above the world counter raises that counter by one. The counter is damage.
 */
public final class NetherPortalRules {
    public static final String REDSTONE_BLOCK = "minecraft:redstone_block";
    public static final String QUARTZ_BLOCK = "minecraft:quartz_block";
    public static final String AMETHYST_BLOCK = "minecraft:amethyst_block";

    /** Tie order when the largest stacks match: quartz block, redstone block, amethyst block. */
    public static final List<String> FUELS = List.of(QUARTZ_BLOCK, REDSTONE_BLOCK, AMETHYST_BLOCK);

    /** Points of weight one block covers for one kind, two kinds, and all three. */
    public static final int ONE_KIND_WEIGHT = 1;
    public static final int TWO_KINDS_WEIGHT = 2;
    public static final int THREE_KINDS_WEIGHT = 4;
    public static final int COOLDOWN_SECONDS = 15;
    public static final int COOLDOWN_TICKS = COOLDOWN_SECONDS * 20;
    /** Do not repeat a denial line while the player is still standing in the frame. */
    public static final int MESSAGE_GAP_TICKS = 20;
    /** Overworld departures roll this many sides. A 1 is the lowest face. */
    public static final int DIE_SIDES = 20;
    /** Animals this close to the departure share the backlash. */
    public static final int BACKLASH_RANGE = 64;

    /**
     * Animals the overworld backlash can hurt. Players, villagers, wandering traders,
     * golems, water creatures, allays, and bats are not in this set.
     */
    public static final Set<String> BACKLASH_ANIMALS = Set.of(
            "minecraft:armadillo",
            "minecraft:bee",
            "minecraft:camel",
            "minecraft:cat",
            "minecraft:chicken",
            "minecraft:cow",
            "minecraft:donkey",
            "minecraft:fox",
            "minecraft:frog",
            "minecraft:goat",
            "minecraft:horse",
            "minecraft:llama",
            "minecraft:trader_llama",
            "minecraft:mooshroom",
            "minecraft:mule",
            "minecraft:ocelot",
            "minecraft:panda",
            "minecraft:parrot",
            "minecraft:pig",
            "minecraft:polar_bear",
            "minecraft:rabbit",
            "minecraft:sheep",
            "minecraft:skeleton_horse",
            "minecraft:sniffer",
            "minecraft:strider",
            "minecraft:turtle",
            "minecraft:wolf",
            "minecraft:happy_ghast");

    /** True when this entity id is one of the backlash animals. */
    public static boolean backlashAnimal(String entityId) {
        return entityId != null && BACKLASH_ANIMALS.contains(entityId);
    }

    private NetherPortalRules() {}

    /**
     * Counter after one overworld departure. A roll above {@code counter} raises it by one.
     * The returned value is the damage for this departure.
     */
    public static int afterRoll(int counter, int roll) {
        int current = Math.max(0, counter);
        if (roll > current) {
            return current + 1;
        }
        return current;
    }

    /**
     * Damage left for the traveler after friendlies, nearest first, each lose at most their health.
     * A dead or empty list leaves the whole amount.
     */
    public static float travelerShare(float damage, float[] friendlyHealth) {
        float left = Math.max(0.0F, damage);
        if (friendlyHealth == null) {
            return left;
        }
        for (float health : friendlyHealth) {
            if (left <= 0.0F) {
                break;
            }
            if (health > 0.0F) {
                left -= Math.min(left, health);
            }
        }
        return left;
    }

    /** Names the blocks a nether portal took. Quartz, then redstone, then amethyst. */
    public static String spentPhrase(int redstone, int quartz, int amethyst) {
        List<String> parts = new ArrayList<>();
        if (quartz > 0) {
            parts.add(quartz + (quartz == 1 ? " quartz block" : " quartz blocks"));
        }
        if (redstone > 0) {
            parts.add(redstone + (redstone == 1 ? " redstone block" : " redstone blocks"));
        }
        if (amethyst > 0) {
            parts.add(amethyst + (amethyst == 1 ? " amethyst block" : " amethyst blocks"));
        }
        if (parts.isEmpty()) {
            return "";
        }
        if (parts.size() == 1) {
            return parts.get(0);
        }
        if (parts.size() == 2) {
            return parts.get(0) + " and " + parts.get(1);
        }
        return parts.get(0) + ", " + parts.get(1) + ", and " + parts.get(2);
    }

    /** 1 point for one kind, 2 for two, and 4 for all three. No fuel still prices at 1. */
    public static int weightPerFuel(int kinds) {
        if (kinds >= 3) {
            return THREE_KINDS_WEIGHT;
        }
        if (kinds == 2) {
            return TWO_KINDS_WEIGHT;
        }
        return ONE_KIND_WEIGHT;
    }

    /**
     * {@code ceil(weight / weightPerFuel(kinds))}, and at least 1 when the weight is positive.
     * Zero weight is free.
     */
    public static int travelCost(double weight, int kinds) {
        if (weight <= 0.0) {
            return 0;
        }
        int cost = (int) Math.ceil(weight / (double) weightPerFuel(kinds));
        return Math.max(1, cost);
    }

    /** Game time when this player may take a nether portal again. */
    public static long readyAt(long now) {
        return now + COOLDOWN_TICKS;
    }

    /** Whole seconds still to wait, rounded up. Zero when the player may travel. */
    public static int secondsLeft(long readyAt, long now) {
        return LapisPortalRules.secondsLeft(readyAt, now);
    }

    public static String waitPhrase(int seconds) {
        return LapisPortalRules.rechargeWait(seconds);
    }

    /** True when a denial was just told and another line would only repeat it. */
    public static boolean repeatTooSoon(long toldAt, long now) {
        return toldAt > 0L && now >= toldAt && now - toldAt < MESSAGE_GAP_TICKS;
    }
}
