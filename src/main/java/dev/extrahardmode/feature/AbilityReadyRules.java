package dev.extrahardmode.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Which strong mana abilities to show, and where their icons sit.
 * Minecraft-free for JUnit.
 */
public final class AbilityReadyRules {
    public static final int ICON_SIZE = 16;
    public static final int GAP = 2;
    public static final int MARGIN = 8;
    /** Icons appear only when the cast's effective level is above this. */
    public static final double MIN_LEVEL = 2.0;
    public static final String REDSTONE_ID = "minecraft:redstone";
    public static final String QUARTZ_ID = "minecraft:quartz";

    private AbilityReadyRules() {}

    /** One stack the player could move into their hand. {@code count} is that stack, not a sum. */
    public record SupplyStack(String itemId, int count) {}

    /** An ability the player can cast now, with its effective level. */
    public record StrongCast(String ability, double level) {}

    public record Placement(int x, int y, int size) {}

    /** {@code (level * 10)%} of {@link #ICON_SIZE}, or 0 when the level is not above {@link #MIN_LEVEL}. */
    public static int iconPixels(double level) {
        if (!(level > MIN_LEVEL)) {
            return 0;
        }
        int size = (int) Math.round(ICON_SIZE * level * 0.10);
        return Math.max(1, size);
    }

    /**
     * Vertical list in the bottom-right corner. Catalog order runs top to bottom.
     * Each icon's right edge sits on the margin, and its bottom sits on the next icon.
     */
    public static List<Placement> place(int guiWidth, int guiHeight, List<Integer> sizes) {
        List<Placement> placed = new ArrayList<>();
        if (sizes == null || sizes.isEmpty()) {
            return placed;
        }
        int count = 0;
        int height = 0;
        for (int size : sizes) {
            if (size <= 0) {
                continue;
            }
            if (count > 0) {
                height += GAP;
            }
            height += size;
            count++;
        }
        if (count == 0) {
            return placed;
        }
        int y = guiHeight - MARGIN - height;
        int right = guiWidth - MARGIN;
        for (int size : sizes) {
            if (size <= 0) {
                continue;
            }
            placed.add(new Placement(right - size, y, size));
            y += size + GAP;
        }
        return placed;
    }

    /**
     * Abilities the player can cast right now at an effective level above {@link #MIN_LEVEL}.
     * The level matches a real cast: skill, gold, mana level for Fire bolt and Magic arrow,
     * the best catalyst they are carrying, redstone, detect-ore quartz, and a honey bottle for Healing.
     * Cooldown or missing mana leaves the ability off the list. Healing with honey does not need mana.
     */
    public static List<StrongCast> strongCasts(
            int manaLevel,
            double currentMana,
            Map<String, Integer> uses,
            Collection<String> learned,
            int goldBonus,
            List<SupplyStack> stacks,
            boolean honey,
            long now,
            Map<String, Long> lastUse) {
        Map<String, Integer> bestCount = new HashMap<>();
        boolean redstone = false;
        boolean quartz = false;
        int bestHoe = -1;
        int bestPick = -1;
        if (stacks != null) {
            for (SupplyStack stack : stacks) {
                if (stack == null || stack.itemId() == null || stack.itemId().isEmpty() || stack.count() <= 0) {
                    continue;
                }
                String id = stack.itemId();
                bestCount.merge(id, stack.count(), Math::max);
                if (REDSTONE_ID.equals(id)) {
                    redstone = true;
                }
                if (QUARTZ_ID.equals(id)) {
                    quartz = true;
                }
                if (AbilityRules.isGrowHoe(id)) {
                    bestHoe = Math.max(bestHoe, AbilityRules.materialBonus(id));
                }
                if (AbilityRules.isPowerPickaxe(id)) {
                    bestPick = Math.max(bestPick, AbilityRules.materialBonus(id));
                }
            }
        }
        Map<String, Integer> useCounts = uses == null ? Map.of() : uses;
        Map<String, Long> usedAt = lastUse == null ? Map.of() : lastUse;
        int gold = Math.max(0, goldBonus);
        List<StrongCast> ready = new ArrayList<>();
        for (String ability : AbilityRules.ABILITY_IDS) {
            if (!hasCatalyst(ability, bestCount, bestHoe, bestPick)) {
                continue;
            }
            boolean enoughMana = AbilityRules.HEAL.equals(ability)
                    ? AbilityRules.hasManaToHeal(currentMana, honey)
                    : AbilityRules.hasManaToUse(ability, currentMana);
            if (!enoughMana) {
                continue;
            }
            int useCount = useCounts.getOrDefault(ability, 0);
            int cooldown = AbilityRules.cooldownTicks(ability, manaLevel, useCount);
            int remaining = AbilityRules.remainingCooldownTicks(now, usedAt.getOrDefault(ability, -1L), cooldown);
            if (remaining > 0) {
                continue;
            }
            int itemBonus = itemBonus(ability, bestCount, redstone, quartz, honey, bestHoe, bestPick);
            double level = AbilityRules.power(
                    ability,
                    manaLevel,
                    useCount,
                    itemBonus + gold,
                    AbilityRules.isTrained(manaLevel, learned, ability));
            if (AbilityRules.DIAMOND_SKIN.equals(ability)) {
                level = AbilityRules.diamondSkinLevel(level);
            }
            if (level > MIN_LEVEL) {
                ready.add(new StrongCast(ability, level));
            }
        }
        return ready;
    }

    static boolean hasCatalyst(String ability, Map<String, Integer> bestCount, int bestHoe, int bestPick) {
        if (AbilityRules.GROW.equals(ability)) {
            return bestHoe >= 0;
        }
        if (AbilityRules.POWER_MINE.equals(ability)) {
            return bestPick >= 0;
        }
        String catalyst = catalystId(ability);
        return catalyst != null && bestCount.getOrDefault(catalyst, 0) > 0;
    }

    static int itemBonus(
            String ability,
            Map<String, Integer> bestCount,
            boolean redstone,
            boolean quartz,
            boolean honey,
            int bestHoe,
            int bestPick) {
        int bonus = AbilityRules.DETECT_ORE.equals(ability)
                ? AbilityRules.detectOreItemBonus(quartz, redstone)
                : AbilityRules.redstoneDustBonus(redstone);
        if (AbilityRules.HEAL.equals(ability) && honey) {
            bonus += AbilityRules.HONEY_HEAL_BONUS;
        }
        if (AbilityRules.GROW.equals(ability)) {
            return bonus + Math.max(0, bestHoe);
        }
        if (AbilityRules.POWER_MINE.equals(ability)) {
            return bonus + Math.max(0, bestPick);
        }
        if (AbilityRules.DETECT_ORE.equals(ability)) {
            return bonus;
        }
        String catalyst = catalystId(ability);
        if (catalyst != null) {
            bonus += AbilityRules.catalystBonus(bestCount.getOrDefault(catalyst, 0));
        }
        return bonus;
    }

    static String catalystId(String ability) {
        if (AbilityRules.HEAL.equals(ability)) {
            return AbilityRules.PAPER_ID;
        }
        if (AbilityRules.IRON_HEART.equals(ability)) {
            return AbilityRules.IRON_INGOT_ID;
        }
        if (AbilityRules.FIRE_BOLT.equals(ability)) {
            return AbilityRules.CHARCOAL_ID;
        }
        if (AbilityRules.MAGIC_ARROW.equals(ability)) {
            return AbilityRules.ARROW_ID;
        }
        if (AbilityRules.FLIGHT.equals(ability)) {
            return AbilityRules.FEATHER_ID;
        }
        if (AbilityRules.LIGHT.equals(ability)) {
            return AbilityRules.COAL_ID;
        }
        if (AbilityRules.DETECT_ORE.equals(ability)) {
            return AbilityRules.COMPASS_ID;
        }
        if (AbilityRules.SLOW.equals(ability)) {
            return AbilityRules.STRING_ID;
        }
        if (AbilityRules.SENSE_EVIL.equals(ability)) {
            return AbilityRules.SPIDER_EYE_ID;
        }
        if (AbilityRules.SMITE_EVIL.equals(ability)) {
            return AbilityRules.GOLDEN_SWORD_ID;
        }
        if (AbilityRules.MASTER_BUILDER.equals(ability)) {
            return AbilityRules.STONE_BRICKS_ID;
        }
        if (AbilityRules.DIAMOND_SKIN.equals(ability)) {
            return AbilityRules.DIAMOND_ID;
        }
        return null;
    }
}
