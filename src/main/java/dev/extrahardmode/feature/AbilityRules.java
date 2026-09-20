package dev.extrahardmode.feature;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntUnaryOperator;

/**
 * Mana ability power, range, and cooldown. Minecraft-free for JUnit.
 */
public final class AbilityRules {
    public static final String HEAL = "heal";
    public static final String IRON_HEART = "iron_heart";
    public static final String FIRE_BOLT = "fire_bolt";
    public static final String MAGIC_ARROW = "magic_arrow";
    public static final String FLIGHT = "flight";
    public static final String GROW = "grow";
    public static final String LIGHT = "light";
    public static final String POWER_MINE = "power_mine";
    public static final String DETECT_ORE = "detect_ore";
    public static final String SLOW = "slow";
    public static final String SENSE_EVIL = "sense_evil";
    public static final String SMITE_EVIL = "smite_evil";
    public static final int MANA_COST = 1;
    public static final int DETECT_ORE_MANA_COST = 1;
    public static final double HEAL_BASE_RANGE = 2.0;
    /** Blue sparkle on the heal recipient. */
    public static final int HEAL_SPARKLE_TICKS = 120;
    /** Silver sparkle on the Iron Heart recipient. */
    public static final int IRON_HEART_SPARKLE_TICKS = 60;
    public static final int IRON_HEART_SECONDS_PER_POWER = 60;
    /** Honey bottle in the main inventory (not a bundle) adds this to Healing power. */
    public static final int HONEY_HEAL_BONUS = 6;
    public static final String HONEY_BOTTLE_ID = "minecraft:honey_bottle";
    /** Hotbar + main inventory slots; excludes armor, offhand, and bundle contents. */
    public static final int MAIN_INVENTORY_SLOTS = 36;
    /** Duration HUDs warn this many ticks before expiry unless the ability can auto-renew. */
    public static final int DURATION_WARN_TICKS = 15 * 20;
    public static final double FIRE_BASE_RANGE = 16.0;
    /** How strongly a fire bolt steers toward its target each tick (0–1). */
    public static final double FIRE_BOLT_HOMING = 0.4;
    public static final double FIRE_BOLT_SPEED = 1.5;
    public static final int FLIGHT_SECONDS_PER_POWER = 6;
    /** Creative default fly speed is 0.05 (~10.9 m/s). Walking is ~4.3 m/s → ~0.02. */
    public static final float WALKING_FLY_SPEED = 0.02F;
    /** Flight is this fraction of the walking-scaled speed. */
    public static final float FLIGHT_SPEED_FACTOR = 0.7F;
    /** Ability power at which flight reaches {@link #FLIGHT_SPEED_FACTOR} of walking speed. */
    public static final double FLIGHT_WALKING_POWER = 10.0;
    /** Small hop on takeoff so standing on a block does not cancel flight immediately. */
    public static final double FLIGHT_LAUNCH_Y = 0.35;
    public static final int COOLDOWN_BASE_TICKS = 2400;
    public static final int COOLDOWN_REDUCE_PER_POINT_TICKS = 100;
    public static final int COOLDOWN_MIN_TICKS = 20;
    /** Fire bolt / magic arrow cooldown starts at this many seconds, then subtracts mana level + skill. */
    public static final int FIRE_BOLT_COOLDOWN_SECONDS = 10;
    /** Extra catalyst in hand, or redstone / quartz from inventory. */
    public static final int CATALYST_BONUS = 2;
    public static final int REDSTONE_BONUS = 2;
    public static final int QUARTZ_BONUS = 2;
    /** Unlearned abilities (over the slot cap) subtract this, then floor at {@link #MIN_POWER}. */
    public static final int UNKNOWN_ABILITY_PENALTY = 3;
    public static final double MIN_POWER = 1.0;
    public static final String PAPER_ID = "minecraft:paper";
    public static final String IRON_INGOT_ID = "minecraft:iron_ingot";
    public static final String FEATHER_ID = "minecraft:feather";
    public static final String ARROW_ID = "minecraft:arrow";
    public static final String CHARCOAL_ID = "minecraft:charcoal";
    public static final String COMPASS_ID = "minecraft:compass";
    public static final String STRING_ID = "minecraft:string";
    public static final String WOODEN_HOE_ID = "minecraft:wooden_hoe";
    public static final String STONE_HOE_ID = "minecraft:stone_hoe";
    public static final String COPPER_HOE_ID = "minecraft:copper_hoe";
    public static final String IRON_HOE_ID = "minecraft:iron_hoe";
    public static final String DIAMOND_HOE_ID = "minecraft:diamond_hoe";
    public static final String GOLDEN_HOE_ID = "minecraft:golden_hoe";
    public static final String NETHERITE_HOE_ID = "minecraft:netherite_hoe";
    public static final String COAL_ID = "minecraft:coal";
    public static final String SPIDER_EYE_ID = "minecraft:spider_eye";
    public static final String GOLDEN_SWORD_ID = "minecraft:golden_sword";
    /** Distance at which Sense Evil reports a rough range: 250 × ability level. */
    public static final double SENSE_EVIL_RANGE_PER_POWER = 250.0;
    public static final int SENSE_EVIL_SPARKLE_TICKS = 120;
    public static final int GROW_RANGE = 2;
    public static final int GROW_HOE_DAMAGE = 3;
    public static final int POWER_MINE_SECONDS_PER_POWER = 12;
    public static final int DETECT_ORE_BASE_RANGE = 2;
    public static final int DETECT_ORE_RANGE_PER_POWER = 1;
    public static final double SLOW_BASE = 0.20;
    public static final double SLOW_PER_POWER = 0.05;
    public static final double SLOW_MAX = 0.80;
    public static final double SLOW_TOUGH_HEALTH = 100.0;
    public static final int SLOW_SECONDS_PER_POWER = 6;
    /** Base glow duration, plus this many seconds per rounded ability level. */
    public static final int LIGHT_SECONDS_BASE = 30;
    public static final int LIGHT_SECONDS_PER_LEVEL = 30;
    public static final int TICKS_PER_MINUTE = 1200;
    /** Light level is this plus whole minutes remaining, capped at {@link #LIGHT_LEVEL}. */
    public static final int LIGHT_LEVEL_BASE = 9;
    /** Torch-bright block light (maximum). */
    public static final int LIGHT_LEVEL = 14;
    private static final int[][] ORE_NEIGHBORS = {
        {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };
    public static final List<String> ABILITY_IDS =
            List.of(
                    HEAL,
                    IRON_HEART,
                    FIRE_BOLT,
                    MAGIC_ARROW,
                    FLIGHT,
                    GROW,
                    LIGHT,
                    POWER_MINE,
                    DETECT_ORE,
                    SLOW,
                    SENSE_EVIL,
                    SMITE_EVIL);

    private AbilityRules() {}

    /** Catalyst item → ability id. Empty hand and other items return null. */
    public static String abilityForItem(String itemId) {
        if (PAPER_ID.equals(itemId)) {
            return HEAL;
        }
        if (IRON_INGOT_ID.equals(itemId)) {
            return IRON_HEART;
        }
        if (FEATHER_ID.equals(itemId)) {
            return FLIGHT;
        }
        if (CHARCOAL_ID.equals(itemId)) {
            return FIRE_BOLT;
        }
        if (ARROW_ID.equals(itemId)) {
            return MAGIC_ARROW;
        }
        if (isGrowHoe(itemId)) {
            return GROW;
        }
        if (isLightCoal(itemId)) {
            return LIGHT;
        }
        if (isPowerPickaxe(itemId)) {
            return POWER_MINE;
        }
        if (COMPASS_ID.equals(itemId)) {
            return DETECT_ORE;
        }
        if (STRING_ID.equals(itemId)) {
            return SLOW;
        }
        if (SPIDER_EYE_ID.equals(itemId)) {
            return SENSE_EVIL;
        }
        if (GOLDEN_SWORD_ID.equals(itemId)) {
            return SMITE_EVIL;
        }
        return null;
    }

    public static boolean isGrowHoe(String itemId) {
        return itemId != null && itemId.endsWith("_hoe");
    }

    public static boolean isLightCoal(String itemId) {
        return COAL_ID.equals(itemId);
    }

    public static boolean isPowerPickaxe(String itemId) {
        return itemId != null && itemId.endsWith("_pickaxe");
    }

    /**
     * Extra ability level from tool material. Wooden/stone 0, copper 1, iron 2,
     * diamond 3, gold 4, netherite 5. Unknown tools count as 0.
     */
    public static int materialBonus(String itemId) {
        if (itemId == null) {
            return 0;
        }
        int slash = itemId.indexOf(':');
        String path = slash >= 0 ? itemId.substring(slash + 1) : itemId;
        if (path.startsWith("copper_")) {
            return 1;
        }
        if (path.startsWith("iron_")) {
            return 2;
        }
        if (path.startsWith("diamond_")) {
            return 3;
        }
        if (path.startsWith("golden_") || path.startsWith("gold_")) {
            return 4;
        }
        if (path.startsWith("netherite_")) {
            return 5;
        }
        return 0;
    }

    public static int growHoeBonus(String itemId) {
        return materialBonus(itemId);
    }

    public static int pickaxeBonus(String itemId) {
        return materialBonus(itemId);
    }

    public static String growHoeBonusLabel(int hoeBonus) {
        return materialBonusLabel(hoeBonus);
    }

    public static String materialBonusLabel(int bonus) {
        int value = Math.max(0, bonus);
        if (value <= 0) {
            return "";
        }
        return "+" + value;
    }

    public static String nameKey(String ability) {
        if (ability == null || ability.isEmpty()) {
            return null;
        }
        return "tougher.ability." + ability;
    }

    public static String nameFallback(String ability) {
        if (ability == null) {
            return "this ability";
        }
        return switch (ability) {
            case HEAL -> "Healing";
            case IRON_HEART -> "Iron Heart";
            case FIRE_BOLT -> "Fire bolt";
            case MAGIC_ARROW -> "Magic arrow";
            case FLIGHT -> "Flight";
            case GROW -> "Let it grow";
            case LIGHT -> "Let there be light";
            case POWER_MINE -> "Power mining";
            case DETECT_ORE -> "Detect ore";
            case SLOW -> "Slow";
            case SENSE_EVIL -> "Sense Evil";
            case SMITE_EVIL -> "Smite Evil";
            default -> "this ability";
        };
    }

    public static String lockedFallback(String ability) {
        return "Gain more mana levels to unlock " + nameFallback(ability) + "...";
    }

    public static String helpKey(String ability) {
        if (ability == null || ability.isEmpty()) {
            return null;
        }
        return "tougher.ability." + ability + ".help";
    }

    public static String helpFallback(String ability) {
        if (ability == null) {
            return null;
        }
        return switch (ability) {
            case HEAL ->
                "Healing: Right-click an injured teammate within 2 + ability level blocks while holding paper. If no teammate is targeted and you are injured, you heal yourself instead. Costs 1 mana. A honey bottle in your main inventory (not a bundle) is consumed for +6, with or without mana. If you have no mana, a honey bottle still lets you heal, even with no mana levels. The recipient sparkles blue for 6 seconds. An extra paper is consumed for +2 if you have more than one. The last paper is kept.";
            case IRON_HEART ->
                "Iron Heart: Right-click while holding an iron ingot. Costs 1 mana. Raises maximum health by ability level for ability level minutes. If another player is targeted within 2 + ability level blocks, they receive the buff instead. Right-click again with an iron ingot to cancel your own; you cannot cancel another player's Iron Heart. The recipient sparkles silver for 3 seconds. An extra iron ingot is consumed for +2 if you have more than one. The last ingot is kept. If it ends and you still have mana, another mana is spent and it continues, and you gain skill as if you cast it again.";
            case FIRE_BOLT ->
                "Fire bolt: Right-click while holding charcoal. Costs 1 mana. The bolt seeks the nearest creature along your aim. Range scales with ability level. Ability level includes your mana level. Cooldown is 10 seconds minus (mana level + skill). An extra charcoal is consumed for +2 if you have more than one. The last charcoal is kept.";
            case MAGIC_ARROW ->
                "Magic arrow: Right-click while holding an arrow. Costs 1 mana. The arrow seeks the nearest creature along your aim and deals arrow damage; it does not ignite. Range scales with ability level. Ability level includes your mana level. Cooldown is 10 seconds minus (mana level + skill). An extra arrow is consumed for +2 if you have more than one. The last arrow is kept.";
            case FLIGHT ->
                "Flight: Right-click while holding a feather. Costs 1 mana. Jump to rise, sneak to descend, land to stop. Speed scales with ability level and reaches 70% of walking speed at 10. An extra feather is consumed for +2 if you have more than one. The last feather is kept. If it ends and you still have mana, another mana is spent and it continues, and you gain skill as if you cast it again.";
            case GROW ->
                "Let it grow: Right-click a plant while holding any hoe to grow it by your ability level in stages (leftover stages go to the nearest plant within 2 blocks). Right-click also still grows random plants around you. Costs 1 mana and 3 durability. Unbreaking can skip that wear with the same chance as tilling. Does not replace tilling. Each plant grown improves that soil's shown crop-loss modifier by 3. Random growth: one plant within 2 blocks by 1 stage, repeated (ability level + hoe bonus) times. Hoe bonus: wood/stone 0, copper +1, iron +2, diamond +3, gold +4, netherite +5.";
            case LIGHT ->
                "Let there be light: Right-click while holding coal. Costs 1 mana. You shine for 30 seconds plus 30 seconds per ability level. Brightness is 9 plus whole minutes remaining, up to torch-bright (14). Right-click again with coal to snuff it. If the glow ends and you still have mana, another mana is spent and it continues, and you gain skill as if you cast it again. An extra coal is consumed for +2 if you have more than one. The last coal is kept.";
            case POWER_MINE ->
                "Power mining: Right-click while holding any pickaxe. Costs 1 mana. For (ability level × 12) seconds, Tougher extra pickaxe wear on hardened stone is suspended. Ends when the timer expires. Pickaxe bonus: wood/stone 0, copper +1, iron +2, diamond +3, gold +4, netherite +5.";
            case DETECT_ORE ->
                "Detect ore: Right-click while holding a compass. Costs 1 mana. Optionally consumes 1 nether quartz from your inventory for +2 (stacks with redstone dust). Turns you to face the highest-value ore deposit within 2 + ability level blocks, even through walls, and tells you the ore and approximate distance.";
            case SLOW ->
                "Slow: Right-click while holding string. Costs 1 mana. Slows enemy mobs within ability level blocks by 20% + 5% per ability level (max 80%) for (ability level × 6) seconds. Mobs with more than 100 health are affected half as much. An extra string is consumed for +2 if you have more than one. The last string is kept.";
            case SENSE_EVIL ->
                "Sense Evil: Right-click while holding a spider eye. Costs 1 mana. If a biome boss is spawned, you turn toward the nearest one and sparkle with a creepy aura for 6 seconds. If it is within 250 × ability level blocks, you also learn the rough distance. If none are spawned, you sparkle as with Healing. An extra spider eye is consumed for +2 if you have more than one. The last spider eye is kept.";
            case SMITE_EVIL ->
                "Smite Evil: Right-click while holding a golden sword. Costs 1 mana. Strikes a creature along your aim up to your ability level in blocks with a normal melee attack from that sword, including enchantments, durability, and attack cooldown. Undead take extra damage equal to your ability level. A small flash of light appears on the creature hit.";
            default -> null;
        };
    }

    public static final String INDEX_HELP_KEY = "tougher.ability.index.help";
    public static final String POWER_HELP_KEY = "tougher.ability.power.help";

    public static String powerHelpFallback() {
        return "Base power: skill, the square root of times you have used that ability. Extra catalyst, redstone, and tools add to that. Extra catalyst and redstone dust are +2 each. Fire bolt and Magic arrow also add your mana level. Using an ability you have not learned still gains skill, at −3 power (floored at 1), until you have a free slot to learn it.";
    }

    public static String indexHelpFallback() {
        return "Mana abilities — hold the item and press ? for details: Paper: Healing. Iron ingot: Iron Heart. Feather: Flight. Charcoal: Fire bolt. Arrow: Magic arrow. Any hoe: Let it grow. Coal: Let there be light. Any pickaxe: Power mining. Compass: Detect ore. String: Slow. Spider eye: Sense Evil. Golden sword: Smite Evil. Redstone dust in your inventory: +2 to any ability (1 is consumed). You can learn up to half your mana level in abilities, rounded up. Unlearned abilities can be used and still gain skill, at −3 power (floored at 1), until you have a free slot to learn them.";
    }

    public static List<String> indexHelpKeys() {
        return List.of(
                INDEX_HELP_KEY,
                "tougher.ability.index.heal",
                "tougher.ability.index.iron_heart",
                "tougher.ability.index.flight",
                "tougher.ability.index.fire_bolt",
                "tougher.ability.index.magic_arrow",
                "tougher.ability.index.grow",
                "tougher.ability.index.light",
                "tougher.ability.index.power_mine",
                "tougher.ability.index.detect_ore",
                "tougher.ability.index.slow",
                "tougher.ability.index.sense_evil",
                "tougher.ability.index.smite_evil",
                "tougher.ability.index.redstone",
                "tougher.ability.index.unlock");
    }

    public static List<String> indexHelpLines() {
        return List.of(
                "Mana abilities — hold the item and press ? for details:",
                "Paper: Healing",
                "Iron ingot: Iron Heart",
                "Feather: Flight",
                "Charcoal: Fire bolt",
                "Arrow: Magic arrow",
                "Any hoe: Let it grow",
                "Coal: Let there be light",
                "Any pickaxe: Power mining",
                "Compass: Detect ore",
                "String: Slow",
                "Spider eye: Sense Evil",
                "Golden sword: Smite Evil",
                "Redstone dust in your inventory: +2 to any ability (1 is consumed)",
                "You can learn up to half your mana level in abilities, rounded up. Unlearned abilities can be used and still gain skill, at −3 power (floored at 1), until you have a free slot to learn them.");
    }

    /** Growth applications: rounded ability level + hoe bonus, at least 1. */
    public static int growTimes(int manaLevel, int uses) {
        return growTimes(manaLevel, uses, 0);
    }

    public static int growTimes(int manaLevel, int uses, int hoeBonus) {
        return Math.max(1, (int) Math.round(power(manaLevel, uses, hoeBonus)));
    }

    /**
     * Unbreaking is rolled once, like a single hoe till. If that use would have
     * taken durability, the full {@link #GROW_HOE_DAMAGE} is applied; otherwise
     * none.
     */
    public static int growHoeDamageTaken(int unbreakingLevel, IntUnaryOperator nextIntExclusive) {
        int useTaken = HardenedBudget.applyUnbreaking(1, unbreakingLevel, nextIntExclusive);
        return useTaken > 0 ? GROW_HOE_DAMAGE : 0;
    }

    /**
     * Distinct random picks, without replacement. Each application targets a
     * different plant so one crop is not grown {@code times} stages.
     */
    public static <T> List<T> pickDistinctRandom(
            List<T> candidates, int times, IntUnaryOperator nextIntExclusive) {
        List<T> pool = new ArrayList<>(candidates);
        List<T> picked = new ArrayList<>();
        int n = Math.min(Math.max(0, times), pool.size());
        for (int i = 0; i < n; i++) {
            picked.add(pool.remove(nextIntExclusive.applyAsInt(pool.size())));
        }
        return picked;
    }

    /**
     * Index of the nearest other point to {@code (x,y,z)}, or -1. Points with
     * {@code skip[i]} true are ignored.
     */
    public static int nearestOtherIndex(
            int x, int y, int z, int[] xs, int[] ys, int[] zs, boolean[] skip) {
        if (xs == null || ys == null || zs == null) {
            return -1;
        }
        int n = Math.min(xs.length, Math.min(ys.length, zs.length));
        int best = -1;
        long bestDist = Long.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            if (skip != null && i < skip.length && skip[i]) {
                continue;
            }
            if (xs[i] == x && ys[i] == y && zs[i] == z) {
                continue;
            }
            long dx = (long) xs[i] - x;
            long dy = (long) ys[i] - y;
            long dz = (long) zs[i] - z;
            long dist = dx * dx + dy * dy + dz * dz;
            if (best < 0 || dist < bestDist) {
                best = i;
                bestDist = dist;
            }
        }
        return best;
    }

    public static int manaCost(String ability) {
        if (DETECT_ORE.equals(ability)) {
            return DETECT_ORE_MANA_COST;
        }
        return MANA_COST;
    }

    public static boolean hasManaToUse(double currentMana) {
        return hasManaToUse(null, currentMana);
    }

    public static boolean hasManaToUse(String ability, double currentMana) {
        return currentMana >= manaCost(ability);
    }

    /** Healing may be cast with a honey bottle instead of mana, including at mana level 0. */
    public static boolean honeyWaivesHealMana(boolean hasHoneyBottle) {
        return hasHoneyBottle;
    }

    public static boolean hasManaToHeal(double currentMana, boolean hasHoneyBottle) {
        return hasManaToUse(HEAL, currentMana) || honeyWaivesHealMana(hasHoneyBottle);
    }

    /** Overlay hints for held catalysts start only after the first mana level. */
    public static boolean showsAbilityHints(int manaLevel) {
        return manaLevel > 0;
    }

    public static double skill(int uses) {
        return Math.sqrt(Math.max(0, uses));
    }

    /** True when a catalyst stack can spend one item and still leave a trigger item. */
    public static boolean consumesCatalyst(int stackCount) {
        return stackCount > 1;
    }

    /** +{@link #CATALYST_BONUS} only when a catalyst item is actually consumed. */
    public static int catalystBonus(int stackCount) {
        return consumesCatalyst(stackCount) ? CATALYST_BONUS : 0;
    }

    /** Distinct learned mana abilities (slot-cap set, not merely used). */
    public static int learnedSkillCount(Collection<String> learned) {
        if (learned == null || learned.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String ability : ABILITY_IDS) {
            if (learned.contains(ability)) {
                count++;
            }
        }
        return count;
    }

    public static boolean isLearned(Collection<String> learned, String ability) {
        return ability != null && learned != null && learned.contains(ability);
    }

    public record AbilitySkill(String id, String name, int uses, double skill) {}

    /** Learned abilities in catalog order, with skill {@code sqrt(uses)}. */
    public static List<AbilitySkill> playerSkills(Map<String, Integer> usesByAbility) {
        List<AbilitySkill> skills = new ArrayList<>();
        Map<String, Integer> uses = usesByAbility == null ? Map.of() : usesByAbility;
        for (String ability : ABILITY_IDS) {
            int count = uses.getOrDefault(ability, 0);
            if (count <= 0) {
                continue;
            }
            skills.add(new AbilitySkill(ability, nameFallback(ability), count, skill(count)));
        }
        return skills;
    }

    /**
     * Extra ability level from inventory/held supplies: redstone dust, extra
     * catalyst in hand, hoe/pickaxe material, and detect-ore quartz.
     */
    public static int supplyBonus(
            String ability, String heldItemId, int heldCount, boolean redstone, boolean quartz) {
        if (ability == null) {
            return 0;
        }
        int bonus = DETECT_ORE.equals(ability)
                ? detectOreItemBonus(quartz, redstone)
                : redstoneDustBonus(redstone);
        if (!ability.equals(abilityForItem(heldItemId))) {
            return bonus;
        }
        if (GROW.equals(ability)) {
            return bonus + growHoeBonus(heldItemId);
        }
        if (POWER_MINE.equals(ability)) {
            return bonus + pickaxeBonus(heldItemId);
        }
        if (DETECT_ORE.equals(ability)) {
            return bonus;
        }
        return bonus + catalystBonus(heldCount);
    }

    /** How many distinct abilities a player may know: ceil(manaLevel / 2). */
    public static int abilitySlotCap(int manaLevel) {
        int level = Math.max(0, manaLevel);
        return (level + 1) / 2;
    }

    /**
     * True when this cast is trained: already learned, or a free slot will learn it
     * on this use. Unlearned abilities over {@link #abilitySlotCap(int)} can still
     * be cast and still gain skill, at {@link #untrainedPower(double)}.
     */
    public static boolean canLearnAbility(int manaLevel, Collection<String> learned) {
        return learnedSkillCount(learned) < abilitySlotCap(manaLevel);
    }

    public static boolean isTrained(int manaLevel, Collection<String> learned, String ability) {
        if (ability == null) {
            return false;
        }
        if (isLearned(learned, ability)) {
            return true;
        }
        return canLearnAbility(manaLevel, learned);
    }

    public static boolean canUseAbility(int manaLevel, Collection<String> learned, String ability) {
        return isTrained(manaLevel, learned, ability);
    }

    /** Ability level before item bonuses: skill, {@code sqrt(uses)}. */
    public static double basePower(int uses) {
        return skill(uses);
    }

    /** {@link #basePower} plus optional consumed-item / hoe bonus. */
    public static double power(int manaLevel, int uses, int itemBonus) {
        return Math.max(0.0, basePower(uses)) + Math.max(0, itemBonus);
    }

    public static boolean addsManaLevel(String ability) {
        return FIRE_BOLT.equals(ability) || MAGIC_ARROW.equals(ability);
    }

    /**
     * Effective ability level, including Fire bolt / Magic arrow's extra mana-level
     * bonus. Pass {@code trained} false for an unlearned ability over the slot cap.
     */
    public static double power(String ability, int manaLevel, int uses, int itemBonus) {
        return power(ability, manaLevel, uses, itemBonus, true);
    }

    public static double power(String ability, int manaLevel, int uses, int itemBonus, boolean trained) {
        double p = power(manaLevel, uses, itemBonus);
        if (addsManaLevel(ability)) {
            p += Math.max(0, manaLevel);
        }
        if (!trained) {
            p = untrainedPower(p);
        }
        return p;
    }

    public static double untrainedPower(double power) {
        return Math.max(MIN_POWER, power - UNKNOWN_ABILITY_PENALTY);
    }

    public static String powerLabel(double power) {
        double value = Math.max(0.0, power);
        if (Math.abs(value - Math.round(value)) < 1.0E-6) {
            return Long.toString(Math.round(value));
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    public static final String UNLEARNED_MARK = "*";

    /** Clock / {@code /ehm me} name: unlearned abilities get a trailing {@code *}. */
    public static String abilityListName(String name, boolean learned) {
        String base = name == null || name.isEmpty() ? "this ability" : name;
        return learned ? base : base + UNLEARNED_MARK;
    }

    public static boolean isMainInventorySlot(int slot) {
        return slot >= 0 && slot < MAIN_INVENTORY_SLOTS;
    }

    public static boolean autoRenews(String ability) {
        return FLIGHT.equals(ability) || LIGHT.equals(ability) || IRON_HEART.equals(ability);
    }

    public static double healRange(double power) {
        return HEAL_BASE_RANGE + Math.max(0.0, power);
    }

    /** Other-player range: 2 + ability level. */
    public static double ironHeartRange(double power) {
        return healRange(power);
    }

    /** Extra max health (health points) equal to ability level. */
    public static float ironHeartHealth(double power) {
        return (float) Math.max(0.0, power);
    }

    /** Duration in ticks: ability level minutes. */
    public static int ironHeartDurationTicks(double power) {
        return Math.max(0, (int) Math.round(Math.max(0.0, power) * IRON_HEART_SECONDS_PER_POWER * 20.0));
    }

    public static double fireRange(double power) {
        return FIRE_BASE_RANGE + Math.max(0.0, power) * 2.0;
    }

    /**
     * Index of the point nearest the aim ray {@code origin + t * look} with
     * {@code 0 ≤ t ≤ range}. Behind the origin or past range is ignored. -1 if none.
     */
    public static int nearestAlongAim(
            double ox,
            double oy,
            double oz,
            double dx,
            double dy,
            double dz,
            double range,
            double[] xs,
            double[] ys,
            double[] zs) {
        if (xs == null || ys == null || zs == null) {
            return -1;
        }
        int n = Math.min(xs.length, Math.min(ys.length, zs.length));
        double lookLen = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (n <= 0 || lookLen < 1.0e-9 || range <= 0.0) {
            return -1;
        }
        double lx = dx / lookLen;
        double ly = dy / lookLen;
        double lz = dz / lookLen;
        int best = -1;
        double bestOff = Double.POSITIVE_INFINITY;
        double bestT = Double.POSITIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            double px = xs[i] - ox;
            double py = ys[i] - oy;
            double pz = zs[i] - oz;
            double t = px * lx + py * ly + pz * lz;
            if (t < 0.0 || t > range) {
                continue;
            }
            double offx = xs[i] - (ox + lx * t);
            double offy = ys[i] - (oy + ly * t);
            double offz = zs[i] - (oz + lz * t);
            double off = offx * offx + offy * offy + offz * offz;
            if (off < bestOff || (off == bestOff && t < bestT)) {
                best = i;
                bestOff = off;
                bestT = t;
            }
        }
        return best;
    }

    /**
     * New velocity steering toward {@code (tx,ty,tz)}, keeping speed. {@code homing}
     * is 0–1 how much of the direction comes from the target.
     */
    public static double[] seekDelta(
            double px,
            double py,
            double pz,
            double vx,
            double vy,
            double vz,
            double tx,
            double ty,
            double tz,
            double homing) {
        double dx = tx - px;
        double dy = ty - py;
        double dz = tz - pz;
        double tlen = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (tlen < 1.0e-6) {
            return new double[] {vx, vy, vz};
        }
        dx /= tlen;
        dy /= tlen;
        dz /= tlen;
        double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (speed < 0.05) {
            speed = FIRE_BOLT_SPEED;
        }
        double nx = vx;
        double ny = vy;
        double nz = vz;
        double nlen = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (nlen < 1.0e-6) {
            nx = dx;
            ny = dy;
            nz = dz;
        } else {
            nx /= nlen;
            ny /= nlen;
            nz /= nlen;
        }
        double blend = Math.min(1.0, Math.max(0.0, homing));
        double hx = nx * (1.0 - blend) + dx * blend;
        double hy = ny * (1.0 - blend) + dy * blend;
        double hz = nz * (1.0 - blend) + dz * blend;
        double hlen = Math.sqrt(hx * hx + hy * hy + hz * hz);
        if (hlen < 1.0e-6) {
            return new double[] {vx, vy, vz};
        }
        double scale = speed / hlen;
        return new double[] {hx * scale, hy * scale, hz * scale};
    }

    public static int flightDurationTicks(double power) {
        return Math.max(1, (int) Math.round(Math.max(0.0, power) * FLIGHT_SECONDS_PER_POWER * 20.0));
    }

    public static int powerMineDurationTicks(double power) {
        return Math.max(1, (int) Math.round(Math.max(0.0, power) * POWER_MINE_SECONDS_PER_POWER * 20.0));
    }

    public static int detectOreRange(double power) {
        return DETECT_ORE_BASE_RANGE
                + DETECT_ORE_RANGE_PER_POWER * Math.max(0, (int) Math.round(Math.max(0.0, power)));
    }

    public static double slowRange(double power) {
        return Math.max(0.0, power);
    }

    public static int slowDurationTicks(double power) {
        return Math.max(0, (int) Math.round(Math.max(0.0, power) * SLOW_SECONDS_PER_POWER * 20.0));
    }

    /** 30 seconds plus 30 seconds per rounded ability level. */
    public static int lightDurationTicks(double power) {
        int levels = Math.max(0, (int) Math.round(Math.max(0.0, power)));
        return Math.max(20, (LIGHT_SECONDS_BASE + LIGHT_SECONDS_PER_LEVEL * levels) * 20);
    }

    /** Block light: 9 + floor(remaining minutes), capped at 14. */
    public static int lightLevel(int remainingTicks) {
        int minutes = Math.max(0, remainingTicks) / TICKS_PER_MINUTE;
        return Math.min(LIGHT_LEVEL, LIGHT_LEVEL_BASE + minutes);
    }

    /**
     * Movement-speed reduction from 0 to {@link #SLOW_MAX}. Tough mobs
     * ({@code maxHealth >} {@link #SLOW_TOUGH_HEALTH}) take half.
     */
    public static double slowAmount(double power, double maxHealth) {
        double amount = SLOW_BASE + SLOW_PER_POWER * Math.max(0.0, power);
        if (amount > SLOW_MAX) {
            amount = SLOW_MAX;
        }
        if (maxHealth > SLOW_TOUGH_HEALTH) {
            amount *= 0.5;
        }
        return amount;
    }

    /** +{@link #REDSTONE_BONUS} when redstone dust is consumed from inventory. */
    public static int redstoneDustBonus(boolean hasRedstoneDust) {
        return hasRedstoneDust ? REDSTONE_BONUS : 0;
    }

    /** Detect ore quartz bonus, stacked with the shared redstone-dust booster. */
    public static int detectOreItemBonus(boolean quartz, boolean redstoneDust) {
        return (quartz ? QUARTZ_BONUS : 0) + redstoneDustBonus(redstoneDust);
    }

    public static double senseEvilRange(double power) {
        return SENSE_EVIL_RANGE_PER_POWER * Math.max(0.0, power);
    }

    /** Melee projection range in blocks: the effective ability level. */
    public static double smiteRange(double power) {
        return Math.max(0.0, power);
    }

    /** Extra sword damage against undead, equal to ability level. */
    public static float smiteUndeadBonus(double power) {
        return (float) Math.max(0.0, power);
    }

    public static boolean senseEvilShowsDistance(double distance, double power) {
        return distance >= 0.0 && distance <= senseEvilRange(power);
    }

    public static int roughBlocks(double distance) {
        return (int) Math.round(Math.max(0.0, distance));
    }

    /** Rounded 3D block distance from the player to the deposit's nearest ore. */
    public static int depositDistanceBlocks(OreDeposit deposit) {
        if (deposit == null) {
            return 0;
        }
        return roughBlocks(Math.sqrt(Math.max(0L, deposit.distSqr())));
    }

    public static String detectOreFoundFallback(String family, int blocks) {
        String name = family == null || family.isEmpty() ? "ore" : family;
        return "The compass pulls toward " + name + " (roughly " + Math.max(0, blocks) + " blocks away).";
    }

    /**
     * Index of the nearest 3D point to {@code (ox,oy,oz)}, or -1 if none.
     */
    public static int nearestPointIndex(
            double ox, double oy, double oz, double[] xs, double[] ys, double[] zs) {
        if (xs == null || ys == null || zs == null) {
            return -1;
        }
        int n = Math.min(xs.length, Math.min(ys.length, zs.length));
        int best = -1;
        double bestDist = Double.POSITIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            double dx = xs[i] - ox;
            double dy = ys[i] - oy;
            double dz = zs[i] - oz;
            double dist = dx * dx + dy * dy + dz * dz;
            if (best < 0 || dist < bestDist) {
                best = i;
                bestDist = dist;
            }
        }
        return best;
    }

    public static String senseEvilFoundFallback() {
        return "You sense an ominous presence ahead...";
    }

    public static String senseEvilFoundNearFallback(int blocks) {
        return senseEvilFoundFallback() + " (roughly " + Math.max(0, blocks) + " blocks away)";
    }

    public static String senseEvilNoneFallback() {
        return "You do not sense any powerful evil yet.  More exploring may be necessary...";
    }

    /** Minecraft yaw: 0 looks +Z (south), 90 looks −X (west). */
    public static float lookYaw(double dx, double dz) {
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    /** Minecraft pitch: 0 is horizontal, 90 looks down, −90 looks up. */
    public static float lookPitch(double dx, double dy, double dz) {
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        return (float) Math.toDegrees(Math.atan2(-dy, horizontal));
    }

    /** Family key for ranking: deepslate_iron_ore and iron_ore both become "iron". */
    public static String oreFamily(String blockId) {
        if (blockId == null || blockId.isEmpty()) {
            return "";
        }
        int slash = blockId.indexOf(':');
        String path = slash >= 0 ? blockId.substring(slash + 1) : blockId;
        if (path.startsWith("deepslate_")) {
            path = path.substring("deepslate_".length());
        }
        if ("ancient_debris".equals(path)) {
            return "ancient_debris";
        }
        if ("nether_gold_ore".equals(path)) {
            return "gold";
        }
        if ("nether_quartz_ore".equals(path)) {
            return "quartz";
        }
        if (path.endsWith("_ore")) {
            return path.substring(0, path.length() - 4);
        }
        return "";
    }

    /**
     * Higher is better. Deepslate uses the same family as the stone variant.
     * Type outweighs vein size when scoring deposits.
     */
    public static int oreValue(String blockId) {
        return switch (oreFamily(blockId)) {
            case "ancient_debris" -> 100;
            case "diamond" -> 90;
            case "emerald" -> 80;
            case "gold" -> 70;
            case "lapis" -> 60;
            case "redstone" -> 50;
            case "iron" -> 40;
            case "copper" -> 30;
            case "quartz" -> 20;
            case "coal" -> 10;
            default -> 0;
        };
    }

    public static boolean isDetectableOre(String blockId) {
        return oreValue(blockId) > 0;
    }

    public static String oreFamilyLabel(String family) {
        if (family == null || family.isEmpty()) {
            return "ore";
        }
        if ("ancient_debris".equals(family)) {
            return "ancient debris";
        }
        if ("lapis".equals(family)) {
            return "lapis lazuli";
        }
        return family.replace('_', ' ');
    }

    /**
     * Deposit score: ore value dominates, then vein size. {@code look*} is the
     * closest block of the vein to the player.
     */
    public record OreDeposit(String family, int value, int size, int lookX, int lookY, int lookZ, long distSqr) {
        public int score() {
            return value * 1000 + Math.min(Math.max(0, size), 999);
        }
    }

    /** One ore block found in the detect-ore scan. */
    public record OreSample(String blockId, int x, int y, int z) {}

    public static long blockDistSqr(int x1, int y1, int z1, int x2, int y2, int z2) {
        long dx = (long) x1 - x2;
        long dy = (long) y1 - y2;
        long dz = (long) z1 - z2;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Face-adjacent (6-connected) clusters of the same ore family. Deepslate and
     * stone variants of one metal count as one deposit.
     */
    public static List<OreDeposit> clusterDeposits(List<OreSample> samples, int px, int py, int pz) {
        if (samples == null || samples.isEmpty()) {
            return List.of();
        }
        record Cell(int x, int y, int z) {}
        Map<Cell, OreSample> byCell = new HashMap<>();
        for (OreSample sample : samples) {
            if (sample == null || !isDetectableOre(sample.blockId())) {
                continue;
            }
            byCell.putIfAbsent(new Cell(sample.x(), sample.y(), sample.z()), sample);
        }
        Set<Cell> visited = new HashSet<>();
        List<OreDeposit> deposits = new ArrayList<>();
        for (OreSample start : byCell.values()) {
            Cell startCell = new Cell(start.x(), start.y(), start.z());
            if (!visited.add(startCell)) {
                continue;
            }
            String family = oreFamily(start.blockId());
            int value = oreValue(start.blockId());
            int size = 0;
            int lookX = start.x();
            int lookY = start.y();
            int lookZ = start.z();
            long bestDist = blockDistSqr(px, py, pz, start.x(), start.y(), start.z());
            ArrayDeque<Cell> queue = new ArrayDeque<>();
            queue.add(startCell);
            while (!queue.isEmpty()) {
                Cell cell = queue.removeFirst();
                OreSample sample = byCell.get(cell);
                if (sample == null) {
                    continue;
                }
                size++;
                long dist = blockDistSqr(px, py, pz, sample.x(), sample.y(), sample.z());
                if (dist < bestDist) {
                    bestDist = dist;
                    lookX = sample.x();
                    lookY = sample.y();
                    lookZ = sample.z();
                }
                for (int[] delta : ORE_NEIGHBORS) {
                    Cell next = new Cell(cell.x() + delta[0], cell.y() + delta[1], cell.z() + delta[2]);
                    OreSample neighbor = byCell.get(next);
                    if (neighbor == null || !family.equals(oreFamily(neighbor.blockId()))) {
                        continue;
                    }
                    if (visited.add(next)) {
                        queue.add(next);
                    }
                }
            }
            deposits.add(new OreDeposit(family, value, size, lookX, lookY, lookZ, bestDist));
        }
        return deposits;
    }

    public static OreDeposit bestDeposit(List<OreDeposit> deposits) {
        if (deposits == null || deposits.isEmpty()) {
            return null;
        }
        OreDeposit best = null;
        for (OreDeposit deposit : deposits) {
            if (deposit == null) {
                continue;
            }
            if (best == null
                    || deposit.score() > best.score()
                    || deposit.score() == best.score() && deposit.distSqr() < best.distSqr()) {
                best = deposit;
            }
        }
        return best;
    }

    public static double flightLaunchY(double currentY) {
        return Math.max(currentY, FLIGHT_LAUNCH_Y);
    }

    public static float flightSpeed(double power) {
        double scale = Math.max(0.0, power) / FLIGHT_WALKING_POWER;
        if (scale > 1.0) {
            scale = 1.0;
        }
        return (float) (WALKING_FLY_SPEED * scale * FLIGHT_SPEED_FACTOR);
    }

    /**
     * 2 minutes minus 5 seconds per (mana level + skill), minimum 1 second.
     * Hits the 1-second floor around combined rating 24 (for example 12 crystals
     * at 2 mana each with no skill). Fire bolt and Magic arrow use
     * {@link #FIRE_BOLT_COOLDOWN_SECONDS} minus (mana level + skill) instead.
     */
    public static int cooldownTicks(int manaLevel, int uses) {
        double points = Math.max(0, manaLevel) + skill(uses);
        int ticks = COOLDOWN_BASE_TICKS - (int) Math.round(points * COOLDOWN_REDUCE_PER_POINT_TICKS);
        return Math.max(COOLDOWN_MIN_TICKS, ticks);
    }

    public static int cooldownTicks(String ability, int manaLevel, int uses) {
        if (SMITE_EVIL.equals(ability)) {
            return 0;
        }
        if (addsManaLevel(ability)) {
            return fireBoltCooldownTicks(manaLevel, uses);
        }
        return cooldownTicks(manaLevel, uses);
    }

    /** 10 seconds minus (mana level + skill), not below 0. */
    public static int fireBoltCooldownTicks(int manaLevel, int uses) {
        double seconds = FIRE_BOLT_COOLDOWN_SECONDS - (Math.max(0, manaLevel) + skill(uses));
        return Math.max(0, (int) Math.round(seconds * 20.0));
    }

    public static int remainingCooldownTicks(long now, long lastUseTick, int cooldownTicks) {
        if (lastUseTick < 0L || cooldownTicks <= 0) {
            return 0;
        }
        long elapsed = now - lastUseTick;
        if (elapsed >= cooldownTicks) {
            return 0;
        }
        return (int) (cooldownTicks - elapsed);
    }

    public static String cooldownLabel(int remainingTicks) {
        int seconds = Math.max(1, (int) Math.ceil(remainingTicks / 20.0));
        if (seconds == 1) {
            return "1 second";
        }
        return seconds + " seconds";
    }
}
