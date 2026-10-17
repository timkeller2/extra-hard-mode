package dev.extrahardmode.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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
    /** Space between the level number and its icon. */
    public static final int LABEL_GAP = 2;
    /** Icons appear only when the cast's effective level is above this. */
    public static final double MIN_LEVEL = 2.0;
    /** Edge of an icon shown for cooldown, an active effect, or the held item when the level is not above {@link #MIN_LEVEL}. */
    public static final int FORCED_ICON_PIXELS = 8;
    /** Learned ability levels. Unlearned levels stay white. */
    public static final int LEARNED_LEVEL_COLOR = 0xFFFFAA00;
    public static final int UNLEARNED_LEVEL_COLOR = 0xFFFFFFFF;
    /** Seconds remaining, drawn on the icon. */
    public static final int COOLDOWN_COLOR = 0xFFFFFF00;
    public static final String REDSTONE_ID = "minecraft:redstone";

    /** A non-empty item id can be brought into hand for a cast. Bundle contents count. */
    public static boolean countsForAbilitySupply(String itemId, boolean insideBundle) {
        return itemId != null && !itemId.isEmpty() && (insideBundle || !itemId.isEmpty());
    }
    public static final String QUARTZ_ID = "minecraft:quartz";

    private AbilityReadyRules() {}

    /** One stack the player could move into their hand. {@code count} is that stack, not a sum. */
    public record SupplyStack(String itemId, int count) {}

    /** An ability the player can cast now, with its effective level. */
    public record StrongCast(String ability, double level) {}

    /**
     * One bottom-right icon. {@code cooldownSeconds} is 0 when the ability is ready.
     * {@code learned} is true only when the player has actually learned it.
     */
    public record HudIcon(String ability, double level, boolean learned, int cooldownSeconds) {}

    /** Icon box plus the level number drawn just to its right, at the icon's scale. */
    public record Placement(int x, int y, int size, int labelX, int labelY) {}

    /** {@code (level * 5)%} of {@link #ICON_SIZE}, or 0 when the level is not above {@link #MIN_LEVEL}. */
    public static int iconPixels(double level) {
        if (!(level > MIN_LEVEL)) {
            return 0;
        }
        int size = (int) Math.round(ICON_SIZE * level * 0.05);
        return Math.max(1, size);
    }

    /**
     * {@code unscaled} GUI pixels after the icon scale {@code iconSize / ICON_SIZE}.
     * At least 1 when both inputs are positive, so a low level still reserves a pixel.
     */
    public static int scaledPixels(int unscaled, int iconSize) {
        if (unscaled <= 0 || iconSize <= 0) {
            return 0;
        }
        int scaled = (int) Math.round(unscaled * (iconSize / (double) ICON_SIZE));
        return Math.max(1, scaled);
    }

    /** Nearest integer of the effective level, or 0 when the level is not above {@link #MIN_LEVEL}. */
    public static int levelNumber(double level) {
        if (!(level > MIN_LEVEL)) {
            return 0;
        }
        long rounded = Math.round(level);
        return rounded > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) rounded;
    }

    /** Level printed beside an icon, including a low level that is shown for another reason. */
    public static int hudLevelNumber(double level) {
        int numbered = levelNumber(level);
        if (numbered > 0) {
            return numbered;
        }
        if (level <= 0.0) {
            return 0;
        }
        long rounded = Math.round(level);
        if (rounded <= 0) {
            return 0;
        }
        return rounded > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) rounded;
    }

    /** Whole seconds left on a cooldown, at least 1 while any ticks remain. */
    public static int cooldownSeconds(int remainingTicks) {
        if (remainingTicks <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(remainingTicks / 20.0));
    }

    /** A book, written book, book and quill, enchanted book, or knowledge book. */
    public static boolean isBook(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return false;
        }
        int colon = itemId.indexOf(':');
        String path = colon >= 0 ? itemId.substring(colon + 1) : itemId;
        return path.endsWith("book");
    }

    /**
     * Holding a book lists every ability above {@link #MIN_LEVEL}. Otherwise an
     * icon is shown only while that ability is active, on cooldown, or triggered
     * by the item in the main hand.
     */
    public static boolean includeIcon(
            double level, boolean holdingBook, boolean active, boolean onCooldown, boolean held) {
        if (active || onCooldown || held) {
            return true;
        }
        return holdingBook && level > MIN_LEVEL;
    }

    /** Icon edge. A forced low-level icon uses {@link #FORCED_ICON_PIXELS}. */
    public static int hudIconPixels(double level, boolean forced) {
        int size = iconPixels(level);
        if (size > 0) {
            return size;
        }
        return forced ? FORCED_ICON_PIXELS : 0;
    }

    /**
     * Vertical list in the bottom-right corner. Catalog order runs top to bottom.
     * The level number sits just to the right of its icon, scaled like that icon,
     * and the number's right edge sits on the margin. A row is as tall as the
     * icon or the scaled number, whichever is taller.
     */
    public static List<Placement> place(
            int guiWidth, int guiHeight, List<Integer> sizes, List<Integer> labelWidths, int textHeight) {
        List<Placement> placed = new ArrayList<>();
        if (sizes == null || sizes.isEmpty()) {
            return placed;
        }
        int font = Math.max(0, textHeight);
        int count = 0;
        int height = 0;
        for (int size : sizes) {
            if (size <= 0) {
                continue;
            }
            if (count > 0) {
                height += GAP;
            }
            height += Math.max(size, scaledPixels(font, size));
            count++;
        }
        if (count == 0) {
            return placed;
        }
        int y = guiHeight - MARGIN - height;
        int right = guiWidth - MARGIN;
        for (int i = 0; i < sizes.size(); i++) {
            int size = sizes.get(i);
            if (size <= 0) {
                continue;
            }
            int labelWidth = 0;
            if (labelWidths != null && i < labelWidths.size()) {
                labelWidth = Math.max(0, labelWidths.get(i));
            }
            int scaledWidth = scaledPixels(labelWidth, size);
            int scaledHeight = scaledPixels(font, size);
            int gap = scaledWidth > 0 ? LABEL_GAP : 0;
            int row = Math.max(size, scaledHeight);
            int iconX = right - size - gap - scaledWidth;
            int iconY = y + (row - size) / 2;
            placed.add(new Placement(
                    iconX,
                    iconY,
                    size,
                    iconX + size + gap,
                    y + Math.max(0, (row - scaledHeight) / 2)));
            y += row + GAP;
        }
        return placed;
    }

    /**
     * Abilities the player can cast right now at an effective level above {@link #MIN_LEVEL}.
     * The level matches a real cast: skill, the best catalyst they are carrying, the largest nether quartz stack
     * in the supply list, charcoal in that list for Magic Arrow, a honey bottle for Healing, and Empower's bonus.
     * Mana level is not added.
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
        return strongCasts(
                manaLevel, currentMana, uses, learned, goldBonus, stacks, honey, now, lastUse, 0.0);
    }

    public static List<StrongCast> strongCasts(
            int manaLevel,
            double currentMana,
            Map<String, Integer> uses,
            Collection<String> learned,
            int goldBonus,
            List<SupplyStack> stacks,
            boolean honey,
            long now,
            Map<String, Long> lastUse,
            double empowerBonus) {
        Map<String, Integer> bestCount = new HashMap<>();
        int quartzStack = largestQuartzStack(stacks);
        int bestHoe = -1;
        int bestPick = -1;
        if (stacks != null) {
            for (SupplyStack stack : stacks) {
                if (stack == null || stack.itemId() == null || stack.itemId().isEmpty() || stack.count() <= 0) {
                    continue;
                }
                String id = stack.itemId();
                bestCount.merge(id, stack.count(), Math::max);
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
        int mana = effectiveManaLevel(manaLevel, goldBonus);
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
            int cooldown = AbilityRules.cooldownTicks(ability, mana, useCount);
            int remaining = AbilityRules.remainingCooldownTicks(now, usedAt.getOrDefault(ability, -1L), cooldown);
            if (remaining > 0) {
                continue;
            }
            int itemBonus = itemBonus(ability, bestCount, quartzStack, honey, bestHoe, bestPick);
            double level = AbilityRules.power(
                    ability,
                    mana,
                    useCount,
                    itemBonus,
                    AbilityRules.isTrained(mana, learned, ability));
            level += Math.max(0.0, empowerBonus);
            if (AbilityRules.DIAMOND_SKIN.equals(ability)) {
                level = AbilityRules.diamondSkinLevel(level);
            }
            if (level > MIN_LEVEL) {
                ready.add(new StrongCast(ability, level));
            }
        }
        return ready;
    }

    /**
     * Bottom-right icons, highest effective level first. A book in hand shows every
     * ability above {@link #MIN_LEVEL}. Otherwise only an active effect, a cooldown,
     * or the ability triggered by {@code heldAbility}. Saturation of the list does
     * not require mana or a catalyst.
     */
    public static List<HudIcon> hudIcons(
            int manaLevel,
            Map<String, Integer> uses,
            Collection<String> learned,
            int goldBonus,
            List<SupplyStack> stacks,
            boolean honey,
            long now,
            Map<String, Long> lastUse,
            boolean holdingBook,
            String heldAbility,
            Collection<String> active) {
        return hudIcons(
                manaLevel,
                uses,
                learned,
                goldBonus,
                stacks,
                honey,
                now,
                lastUse,
                holdingBook,
                heldAbility,
                active,
                0.0);
    }

    public static List<HudIcon> hudIcons(
            int manaLevel,
            Map<String, Integer> uses,
            Collection<String> learned,
            int goldBonus,
            List<SupplyStack> stacks,
            boolean honey,
            long now,
            Map<String, Long> lastUse,
            boolean holdingBook,
            String heldAbility,
            Collection<String> active,
            double empowerBonus) {
        Map<String, Integer> bestCount = new HashMap<>();
        int quartzStack = largestQuartzStack(stacks);
        int bestHoe = -1;
        int bestPick = -1;
        if (stacks != null) {
            for (SupplyStack stack : stacks) {
                if (stack == null || stack.itemId() == null || stack.itemId().isEmpty() || stack.count() <= 0) {
                    continue;
                }
                String id = stack.itemId();
                bestCount.merge(id, stack.count(), Math::max);
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
        int mana = effectiveManaLevel(manaLevel, goldBonus);
        List<HudIcon> icons = new ArrayList<>();
        for (String ability : AbilityRules.ABILITY_IDS) {
            int useCount = useCounts.getOrDefault(ability, 0);
            int cooldown = AbilityRules.cooldownTicks(ability, mana, useCount);
            int remaining = AbilityRules.remainingCooldownTicks(now, usedAt.getOrDefault(ability, -1L), cooldown);
            int seconds = cooldownSeconds(remaining);
            boolean running = active != null && active.contains(ability);
            boolean held = ability.equals(heldAbility);
            int itemBonus = itemBonus(ability, bestCount, quartzStack, honey, bestHoe, bestPick);
            double level = AbilityRules.power(
                    ability,
                    mana,
                    useCount,
                    itemBonus,
                    AbilityRules.isTrained(mana, learned, ability));
            level += Math.max(0.0, empowerBonus);
            if (AbilityRules.DIAMOND_SKIN.equals(ability)) {
                level = AbilityRules.diamondSkinLevel(level);
            }
            if (!includeIcon(level, holdingBook, running, seconds > 0, held)) {
                continue;
            }
            boolean forced = running || seconds > 0 || held;
            if (hudIconPixels(level, forced) <= 0) {
                continue;
            }
            icons.add(new HudIcon(ability, level, AbilityRules.isLearned(learned, ability), seconds));
        }
        icons.sort(Comparator.comparingDouble(HudIcon::level)
                .reversed()
                .thenComparingInt(icon -> {
                    int index = AbilityRules.ABILITY_IDS.indexOf(icon.ability());
                    return index < 0 ? Integer.MAX_VALUE : index;
                }));
        return icons;
    }

    /** Passed mana level plus a separate ready-gold count. Production mana already includes gold, so it passes 0. */
    static int effectiveManaLevel(int manaLevel, int goldBonus) {
        return Math.max(0, manaLevel) + Math.max(0, goldBonus);
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

    /** Largest nether quartz stack in {@code stacks}. Callers leave out bundles, armor, and the offhand. */
    static int largestQuartzStack(List<SupplyStack> stacks) {
        int largest = 0;
        if (stacks == null) {
            return 0;
        }
        for (SupplyStack stack : stacks) {
            if (stack == null || !QUARTZ_ID.equals(stack.itemId()) || stack.count() <= 0) {
                continue;
            }
            largest = Math.max(largest, stack.count());
        }
        return largest;
    }

    static int itemBonus(
            String ability,
            Map<String, Integer> bestCount,
            int quartzStack,
            boolean honey,
            int bestHoe,
            int bestPick) {
        int bonus = AbilityRules.DETECT_ORE.equals(ability) ? AbilityRules.detectOreQuartzBonus(quartzStack) : 0;
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
        if (AbilityRules.MAGIC_ARROW.equals(ability)
                && bestCount.getOrDefault(AbilityRules.CHARCOAL_ID, 0) > 0) {
            bonus += AbilityRules.CHARCOAL_ARROW_BONUS;
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
        if (AbilityRules.EMPOWER.equals(ability)) {
            return AbilityRules.REDSTONE_ID;
        }
        if (AbilityRules.BATTERY.equals(ability)) {
            return AbilityRules.QUARTZ_BLOCK_ID;
        }
        if (AbilityRules.LAPIS_SHIELD.equals(ability)) {
            return AbilityRules.LAPIS_LAZULI_ID;
        }
        return null;
    }
}
