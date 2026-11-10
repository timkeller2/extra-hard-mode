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
    /** Retired. Saved uses, lessons, and cooldowns fold into {@link #MAGIC_ARROW}. */
    public static final String FIRE_BOLT = "fire_bolt";
    public static final String MAGIC_ARROW = "magic_arrow";
    public static final String FLIGHT = "flight";
    public static final String GROW = "grow";
    /** Retired. Saved lessons and practice are dropped. Night Vision potions replace it. */
    public static final String LIGHT = "light";
    public static final String POWER_MINE = "power_mine";
    public static final String DETECT_ORE = "detect_ore";
    public static final String SLOW = "slow";
    public static final String SENSE_EVIL = "sense_evil";
    public static final String SMITE_EVIL = "smite_evil";
    public static final String MASTER_BUILDER = "master_builder";
    public static final String DIAMOND_SKIN = "diamond_skin";
    public static final String EMPOWER = "empower";
    public static final String BATTERY = "battery";
    public static final String LAPIS_SHIELD = "lapis_shield";
    public static final String WRITE_TOME = "write_tome";
    public static final String READ_TOME = "read_tome";
    public static final String RESTORE_LIFE = "restore_life";
    public static final int MANA_COST = 1;
    public static final int DIAMOND_SKIN_MANA_COST = 2;
    /** Restore Life spends this much mana, and only when the cast can happen. */
    public static final int RESTORE_LIFE_MANA_COST = 4;
    /** Diamond price before the target's lives and the caster's Restore Life level. */
    public static final int RESTORE_LIFE_DIAMOND_BASE = 17;
    /** Lowest diamond price for Restore Life. */
    public static final int RESTORE_LIFE_DIAMOND_MIN = 3;
    public static final String RESTORE_LIFE_NO_LIVES = "This world has no lives to restore.";
    public static final String RESTORE_LIFE_UNAVAILABLE = "Restore Life is not available.";
    public static final String RESTORE_LIFE_AT_CAP = "%s has %s lives. Restore Life needs fewer than %s.";
    public static final String RESTORE_LIFE_MANA_NEED = "Restore Life needs 4 mana.";
    public static final String RESTORE_LIFE_DIAMONDS = "Restore Life needs %s diamonds. You have %s.";
    public static final int DETECT_ORE_MANA_COST = 1;
    /** Smite Evil spends a quarter mana. Four casts spend one. */
    public static final double SMITE_EVIL_MANA_COST = 0.25;
    public static final double HEAL_BASE_RANGE = 2.0;
    /** Blue sparkle on the heal recipient. */
    public static final int HEAL_SPARKLE_TICKS = 120;
    /** Silver sparkle on the Iron Heart recipient. */
    public static final int IRON_HEART_SPARKLE_TICKS = 60;
    public static final int IRON_HEART_SECONDS_PER_POWER = 60;
    /** Honey bottle in the main inventory (not a bundle) adds this to Healing power. */
    public static final int HONEY_HEAL_BONUS = 4;
    public static final String HONEY_BOTTLE_ID = "minecraft:honey_bottle";
    /** Hotbar + main inventory slots; excludes armor, offhand, and bundle contents. */
    public static final int MAIN_INVENTORY_SLOTS = 36;
    /** Duration HUDs warn this many ticks before expiry unless the ability can auto-renew. */
    public static final int DURATION_WARN_TICKS = 15 * 20;
    public static final double FIRE_BASE_RANGE = 16.0;
    /** How strongly a magic arrow steers toward its target each tick (0–1). */
    public static final double FIRE_BOLT_HOMING = 0.4;
    public static final double FIRE_BOLT_SPEED = 1.5;
    public static final int FLIGHT_SECONDS_PER_POWER = 6;
    /** Creative default fly speed is 0.05 (~10.9 m/s). Walking is ~4.3 m/s → ~0.02. */
    public static final float WALKING_FLY_SPEED = 0.02F;
    /** Flight is this fraction of the walking-scaled speed. */
    public static final float FLIGHT_SPEED_FACTOR = 0.7F;
    /** Ability power at which flight reaches {@link #FLIGHT_SPEED_FACTOR} of walking speed. */
    public static final double FLIGHT_WALKING_POWER = 10.0;
    /** Small hop on takeoff. */
    public static final double FLIGHT_LAUNCH_Y = 0.35;
    public static final int COOLDOWN_BASE_TICKS = 2400;
    /** 3 seconds, in ticks, removed per point of mana level plus skill. */
    public static final int COOLDOWN_REDUCE_PER_POINT_TICKS = 60;
    public static final int COOLDOWN_MIN_TICKS = 20;
    /** Missile Storm cooldown starts at this many seconds, then subtracts skill. */
    public static final int FIRE_BOLT_COOLDOWN_SECONDS = 10;
    /** Extra catalyst in hand when two are consumed. */
    public static final int CATALYST_BONUS = 2;
    /**
     * More than this many arrows in hand doubles Missile Storm. The levels added are capped at
     * the square root of that stack, rounded down, and that many arrows are consumed.
     */
    public static final int MISSILE_STORM_HAND = 3;
    /** Charcoal in the hotbar or backpack adds this to Missile Storm and is spent one at a time. */
    public static final int CHARCOAL_ARROW_BONUS = 2;
    /** Phantom membrane in the hotbar or backpack adds this to Flight and is spent one at a time. */
    public static final int FLIGHT_MEMBRANE_BONUS = 3;
    /** Added to skill and item bonuses. Worlds override this with {@code abilities.levelOffset}. */
    public static final int DEFAULT_LEVEL_OFFSET = -2;
    /** Effective level is never below this. */
    public static final double MIN_POWER = 0.0;
    /** A cast below this level practices. Diamond Skin uses {@link #DIAMOND_SKIN_MIN_LEVEL}. */
    public static final double PRACTICE_LEVEL = 1.0;
    public static final String PAPER_ID = "minecraft:paper";
    public static final String IRON_INGOT_ID = "minecraft:iron_ingot";
    public static final String FEATHER_ID = "minecraft:feather";
    public static final String ARROW_ID = "minecraft:arrow";
    public static final String CHARCOAL_ID = "minecraft:charcoal";
    public static final String PHANTOM_MEMBRANE_ID = "minecraft:phantom_membrane";
    public static final String COMPASS_ID = "minecraft:compass";
    public static final String STRING_ID = "minecraft:string";
    public static final String WOODEN_HOE_ID = "minecraft:wooden_hoe";
    public static final String STONE_HOE_ID = "minecraft:stone_hoe";
    public static final String COPPER_HOE_ID = "minecraft:copper_hoe";
    public static final String IRON_HOE_ID = "minecraft:iron_hoe";
    public static final String DIAMOND_HOE_ID = "minecraft:diamond_hoe";
    public static final String GOLDEN_HOE_ID = "minecraft:golden_hoe";
    public static final String NETHERITE_HOE_ID = "minecraft:netherite_hoe";
    public static final String SPIDER_EYE_ID = "minecraft:spider_eye";
    public static final String GOLDEN_SWORD_ID = "minecraft:golden_sword";
    public static final String HEAVY_GOLDEN_SWORD_ID = "tougher:heavy_golden_sword";
    /** Clay brick item, not the bricks block and not stone bricks. */
    public static final String BRICK_ID = "minecraft:brick";
    /** Master Builder lasts this many seconds per point of effective ability level. */
    public static final int MASTER_BUILDER_SECONDS_PER_POWER = 30;
    public static final String DIAMOND_ID = "minecraft:diamond";
    public static final String DIAMOND_BLOCK_ID = "minecraft:diamond_block";
    public static final String GLOWSTONE_DUST_ID = "minecraft:glowstone_dust";
    /** Empower lasts this many seconds per point of effective ability level, and at least {@link #EMPOWER_MIN_SECONDS}. */
    public static final int EMPOWER_SECONDS_PER_POWER = 30;
    public static final int EMPOWER_MIN_SECONDS = 30;
    public static final String QUARTZ_BLOCK_ID = "minecraft:quartz_block";
    /** Battery lasts this many seconds per point of effective ability level, and at least {@link #BATTERY_MIN_SECONDS}. */
    public static final int BATTERY_SECONDS_PER_POWER = 120;
    public static final int BATTERY_MIN_SECONDS = 120;
    public static final String LAPIS_LAZULI_ID = "minecraft:lapis_lazuli";
    public static final String AMETHYST_SHARD_ID = "minecraft:amethyst_shard";
    public static final String BOOK_ID = "minecraft:book";
    /** Amethyst Shield lasts this many seconds per point of effective ability level. */
    public static final int LAPIS_SHIELD_SECONDS_PER_POWER = 60;
    /** Diamond Skin lasts this many seconds per point of effective ability level. */
    public static final int DIAMOND_SKIN_SECONDS_PER_POWER = 30;
    /** Diamond Skin practices below this level. The cast is not raised up to it. */
    public static final double DIAMOND_SKIN_MIN_LEVEL = 2.0;
    /** Cyan sparkle when Diamond Skin reduces a hit. 0.25 seconds. */
    public static final int DIAMOND_SKIN_SPARKLE_TICKS = 5;
    /** Distance at which Sense Evil reports a rough range: 250 × ability level. */
    public static final double SENSE_EVIL_RANGE_PER_POWER = 250.0;
    public static final int SENSE_EVIL_SPARKLE_TICKS = 120;
    /** Wise-teacher marks and nearby hostiles stay outlined this many seconds per point of effective Sense Evil level. */
    public static final int SENSE_OUTLINE_SECONDS_PER_POWER = 60;
    /** Hostile mobs within this many blocks per point of effective Sense Evil level are outlined. */
    public static final double SENSE_HOSTILE_BLOCKS_PER_POWER = 8.0;
    public static final int GROW_RANGE = 2;
    public static final int GROW_HOE_DAMAGE = 3;
    public static final int POWER_MINE_SECONDS_PER_POWER = 12;
    /** Added to the rounded ability level before the ore's difficulty is subtracted. */
    public static final int DETECT_ORE_BASE_RANGE = 0;
    public static final int DETECT_ORE_RANGE_PER_POWER = 1;
    /** First search this many blocks above and below the player before looking farther up or down. */
    public static final int DETECT_ORE_LEVEL_BAND = 2;
    public static final double SLOW_BASE = 0.20;
    public static final double SLOW_PER_POWER = 0.05;
    public static final double SLOW_MAX = 0.80;
    public static final double SLOW_TOUGH_HEALTH = 100.0;
    public static final int SLOW_SECONDS_PER_POWER = 6;
    /** Blocks the web travels per tick. */
    public static final double SLOW_WEB_SPEED = 1.0;
    private static final int[][] ORE_NEIGHBORS = {
        {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };
    public static final List<String> ABILITY_IDS =
            List.of(
                    HEAL,
                    IRON_HEART,
                    MAGIC_ARROW,
                    FLIGHT,
                    GROW,
                    POWER_MINE,
                    DETECT_ORE,
                    SLOW,
                    SENSE_EVIL,
                    SMITE_EVIL,
                    MASTER_BUILDER,
                    DIAMOND_SKIN,
                    EMPOWER,
                    BATTERY,
                    LAPIS_SHIELD,
                    WRITE_TOME,
                    READ_TOME,
                    RESTORE_LIFE);

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
        if (ARROW_ID.equals(itemId)) {
            return MAGIC_ARROW;
        }
        if (isGrowHoe(itemId)) {
            return GROW;
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
        if (isSmiteSword(itemId)) {
            return SMITE_EVIL;
        }
        if (BRICK_ID.equals(itemId)) {
            return MASTER_BUILDER;
        }
        if (DIAMOND_ID.equals(itemId)) {
            return DIAMOND_SKIN;
        }
        if (DIAMOND_BLOCK_ID.equals(itemId)) {
            return RESTORE_LIFE;
        }
        if (GLOWSTONE_DUST_ID.equals(itemId)) {
            return EMPOWER;
        }
        if (QUARTZ_BLOCK_ID.equals(itemId)) {
            return BATTERY;
        }
        if (AMETHYST_SHARD_ID.equals(itemId)) {
            return LAPIS_SHIELD;
        }
        if (BOOK_ID.equals(itemId)) {
            return WRITE_TOME;
        }
        return null;
    }

    /** A normal golden sword or a heavy golden sword. Both cast Smite Evil. */
    public static boolean isSmiteSword(String itemId) {
        return GOLDEN_SWORD_ID.equals(itemId) || HEAVY_GOLDEN_SWORD_ID.equals(itemId);
    }

    public static boolean isGrowHoe(String itemId) {
        return itemId != null && itemId.endsWith("_hoe");
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

    /** Thirty seconds. A held golden item counts as one mana level after this long. */
    public static final int GOLD_BONUS_TICKS = 600;
    /** Armor waits are this many times the held-item wait: 90 seconds, then 180, 360, and 720. */
    public static final int GOLD_ARMOR_TIME_FACTOR = 3;
    /** Half a second of golden shimmer when a gold bonus starts. */
    public static final int GOLD_SHIMMER_TICKS = 10;

    /** Gold ingots, nuggets, blocks, and ore. These are gold itself, not a golden item. */
    public static boolean isGoldItself(String itemId) {
        String path = itemPath(itemId);
        return path.equals("gold_ingot")
                || path.equals("gold_nugget")
                || path.equals("gold_block")
                || path.equals("raw_gold")
                || path.equals("gold_ore")
                || path.equals("deepslate_gold_ore")
                || path.equals("nether_gold_ore");
    }

    /** A crafted golden item, such as a golden hoe. Not gold itself. */
    public static boolean isHeldGoldenItem(String itemId) {
        String path = itemPath(itemId);
        return path.startsWith("golden_")
                || path.equals("heavy_golden_sword")
                || path.equals("heavy_golden_axe");
    }

    public static boolean isGoldArmor(String itemId) {
        String path = itemPath(itemId);
        return path.equals("golden_helmet")
                || path.equals("golden_chestplate")
                || path.equals("golden_leggings")
                || path.equals("golden_boots");
    }

    /** Ready gold points. Each point is one mana level, not ability power. */
    public static int goldAbilityBonus(int readyArmorPieces, boolean readyHeldItem) {
        return Math.max(0, readyArmorPieces) + (readyHeldItem ? 1 : 0);
    }

    /** One line per newly active point: {@code Mana level +1 (+total)}. */
    public static List<String> goldBonusMessages(int previousShown, int readyNow) {
        List<String> messages = new ArrayList<>();
        int from = Math.max(0, previousShown);
        int to = Math.max(0, readyNow);
        for (int total = from + 1; total <= to; total++) {
            messages.add("Mana level +1 (+" + total + ")");
        }
        return messages;
    }

    public static boolean goldWornLongEnough(long equippedAt, long now) {
        return goldWornLongEnough(equippedAt, now, GOLD_BONUS_TICKS);
    }

    /**
     * Extra wait before the next gold-armor mana level, in ticks. The first piece is
     * 90 seconds, then 180, 360, and 720. Four pieces put on together take 1350 seconds.
     */
    public static int goldArmorStepTicks(int pieceNumber) {
        if (pieceNumber <= 0) {
            return 0;
        }
        int n = Math.min(pieceNumber, 4);
        return GOLD_BONUS_TICKS * GOLD_ARMOR_TIME_FACTOR << (n - 1);
    }

    /** Wear time, in ticks, before that many gold armor pieces all count. */
    public static int goldArmorDelayTicks(int pieceCount) {
        int total = 0;
        for (int piece = 1; piece <= pieceCount; piece++) {
            total += goldArmorStepTicks(piece);
        }
        return total;
    }

    static boolean goldWornLongEnough(long equippedAt, long now, int ticks) {
        return equippedAt >= 0 && now >= equippedAt && now - equippedAt >= ticks;
    }

    /**
     * Keep wear timers for slots that still hold the same qualifying item.
     * A changed or removed item drops out immediately. {@code nowIds} maps slot to the current item id.
     */
    public static Map<String, String> updateGoldWear(Map<String, String> previous, Map<String, String> nowIds, long now) {
        Map<String, String> next = new HashMap<>();
        if (nowIds == null) {
            return next;
        }
        Map<String, String> prior = previous == null ? Map.of() : previous;
        for (Map.Entry<String, String> entry : nowIds.entrySet()) {
            String itemId = entry.getValue();
            if (itemId == null || itemId.isEmpty()) {
                continue;
            }
            long since = goldWearSince(prior.get(entry.getKey()), itemId);
            if (since < 0) {
                since = now;
            }
            next.put(entry.getKey(), itemId + "|" + since);
        }
        return next;
    }

    /**
     * Armor pieces and one hand bonus that have been worn or held long enough.
     * Armor points arrive one at a time: 90 seconds, then 180, 360, and 720 more.
     * The next wait starts once the previous point is in and that many pieces are on.
     */
    public static int readyGoldBonus(Map<String, String> state, long now) {
        if (state == null || state.isEmpty()) {
            return 0;
        }
        List<Long> armorSince = new ArrayList<>();
        boolean hand = false;
        for (Map.Entry<String, String> entry : state.entrySet()) {
            String itemId = goldWearItem(entry.getValue());
            long since = goldWearSince(entry.getValue(), itemId);
            if ("main".equals(entry.getKey()) || "off".equals(entry.getKey())) {
                if (goldWornLongEnough(since, now)) {
                    hand = true;
                }
                continue;
            }
            if (since >= 0) {
                armorSince.add(since);
            }
        }
        armorSince.sort(Long::compareTo);
        int armor = 0;
        long levelReachedAt = Long.MIN_VALUE;
        for (int i = 0; i < armorSince.size(); i++) {
            long since = armorSince.get(i);
            if (since > now) {
                break;
            }
            long start = Math.max(levelReachedAt, since);
            long completes = start + goldArmorStepTicks(i + 1);
            if (now < completes) {
                break;
            }
            armor++;
            levelReachedAt = completes;
        }
        return goldAbilityBonus(armor, hand);
    }

    static long goldWearSince(String record, String itemId) {
        if (record == null || itemId == null || itemId.isEmpty()) {
            return -1L;
        }
        int split = record.lastIndexOf('|');
        if (split <= 0 || !record.substring(0, split).equals(itemId)) {
            return -1L;
        }
        try {
            return Long.parseLong(record.substring(split + 1));
        } catch (NumberFormatException ignored) {
            return -1L;
        }
    }

    static String goldWearItem(String record) {
        if (record == null) {
            return "";
        }
        int split = record.lastIndexOf('|');
        return split <= 0 ? "" : record.substring(0, split);
    }

    private static String itemPath(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return "";
        }
        int slash = itemId.indexOf(':');
        return slash >= 0 ? itemId.substring(slash + 1) : itemId;
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
            case MAGIC_ARROW -> "Missile Storm";
            case FLIGHT -> "Flight";
            case GROW -> "Let it grow";
            case POWER_MINE -> "Power mining";
            case DETECT_ORE -> "Detect ore";
            case SLOW -> "Slow";
            case SENSE_EVIL -> "Sense Evil";
            case SMITE_EVIL -> "Smite Evil";
            case MASTER_BUILDER -> "Master Builder";
            case DIAMOND_SKIN -> "Diamond Skin";
            case EMPOWER -> "Empower";
            case BATTERY -> "Battery";
            case LAPIS_SHIELD -> "Amethyst Shield";
            case WRITE_TOME -> "Write Tome of the Wise";
            case READ_TOME -> "Read Tome of the Wise";
            case RESTORE_LIFE -> "Restore Life";
            default -> "this ability";
        };
    }

    /** Short line a wise teacher says about the ability. */
    public static String teacherFlavor(String ability) {
        if (ability == null) {
            return "There is a lesson here, if you can pay for it.";
        }
        return switch (ability) {
            case HEAL -> "Mercy is a craft. I can put it in your hands.";
            case IRON_HEART -> "There is a second heart, if you ask the iron to remember you.";
            case MAGIC_ARROW -> "A handful of arrows can be taught to hunt together, and charcoal still remembers the fire.";
            case FLIGHT -> "The sky is only heavy if you agree that it is.";
            case GROW -> "Green things listen, if you speak with a hoe.";
            case POWER_MINE -> "Stone yields to a pick that has been properly introduced.";
            case DETECT_ORE -> "The ground is talkative, if you hold a compass still.";
            case SLOW -> "Haste is a habit. String can interrupt it.";
            case SENSE_EVIL -> "Large hungers leave a smell on the wind.";
            case SMITE_EVIL -> "A golden sword, heavy or not, is a polite way to end a monster.";
            case MASTER_BUILDER -> "Stone will hold for a while, even where the ground does not agree.";
            case DIAMOND_SKIN -> "A diamond can be asked to stand between you and the blow.";
            case EMPOWER -> "Glowstone will lend its light to the next working.";
            case BATTERY -> "A block of quartz will hold a charge for your mana.";
            case LAPIS_SHIELD -> "Amethyst can stand in the air between a body and the blow.";
            case WRITE_TOME -> "Experience can be set down in a book, and carried.";
            case READ_TOME -> "A careful reading gives back only a share of what was written down.";
            case RESTORE_LIFE -> "A life can be handed back, if the diamonds agree to the price.";
            default -> "There is a lesson here, if you can pay for it.";
        };
    }

    /** How to cast the ability, told after the lesson. */
    public static String teacherInstruction(String ability) {
        if (ability == null) {
            return "";
        }
        return switch (ability) {
            case HEAL ->
                "Hold paper and double-right-click an injured person, or yourself if you are hurt. It costs 1 mana.";
            case IRON_HEART ->
                "Hold an iron ingot and double-right-click. Double-right-click again with an ingot to cancel your own.";
            case MAGIC_ARROW ->
                "Hold an arrow and double-right-click. One arrow per level seeks a hostile in range, and each deals that much magic damage. More than 3 arrows doubles the level, up to the square root of the arrows in hand. Charcoal in your inventory adds +2 and makes them burn. It costs 1 mana.";
            case FLIGHT -> "Hold a feather and double-right-click. Jump to rise, sneak in the air to descend, and sneak on a block to stop.";
            case GROW -> "Hold any hoe and double-right-click a plant. It costs 1 mana and some hoe durability.";
            case POWER_MINE -> "Hold any pickaxe and double-right-click. Hardened stone stops eating extra durability for a while.";
            case DETECT_ORE ->
                "Hold a compass and double-right-click. You turn toward the best ore you are skilled enough to sense, preferring ore within 2 blocks of your height.";
            case SLOW -> "Hold string and double-right-click. A web flies out and bursts on a creature or a solid block.";
            case SENSE_EVIL ->
                "Double-right-click a spider eye. Hold the click to eat it instead. You turn toward a living biome boss, if one is out.";
            case SMITE_EVIL ->
                "Hold a golden sword or a heavy golden sword and double-right-click. It strikes along your aim. With at least 1 mana the crosshair turns gold on one in reach. It costs 1/4 mana.";
            case MASTER_BUILDER ->
                "Hold a clay brick and double-right-click. Double-right-click again to cancel. It costs 1 mana.";
            case DIAMOND_SKIN ->
                "Hold diamonds and double-right-click. Double-right-click again to cancel. It costs 2 mana.";
            case EMPOWER ->
                "Hold glowstone dust and double-right-click. Double-right-click again to cancel. It costs 1 mana.";
            case BATTERY ->
                "Hold a quartz block and double-right-click in the air. Double-right-click again to cancel. It costs no mana. It renews until mana reaches your level and spends a quartz block.";
            case LAPIS_SHIELD ->
                "Hold an amethyst shard and double-right-click a creature, or yourself if none is targeted. It costs 1 mana. Double-right-click again to cancel.";
            case WRITE_TOME ->
                "Hold a book at experience level 7 or higher and double-right-click. It costs 1 mana. Two or more books add +2. Three, four, or five books make 2, 3, or 4 tomes and split the experience. More than five still uses five and makes 4.";
            case READ_TOME ->
                "Hold a tome that holds experience and double-right-click. It costs 1 mana and returns 80% of what was saved, plus 2% per ability level.";
            case RESTORE_LIFE ->
                "Hold a diamond block and double-right-click in the air or on a player. Clicking a block still places it. It costs 4 mana and diamonds from your inventory.";
            default -> "";
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

    /** Chat topics for {@code /tougher ability help <name>}. */
    public static List<String> abilityTopics() {
        return List.of(
                "healing",
                "iron heart",
                "missile storm",
                "flight",
                "grow",
                "power mine",
                "detect ore",
                "slow",
                "sense evil",
                "smite evil",
                "master builder",
                "diamond skin",
                "empower",
                "battery",
                "amethyst shield",
                "write tome",
                "read tome",
                "restore life");
    }

    public static String abilityByTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            return null;
        }
        String key = topic.trim().toLowerCase(java.util.Locale.ROOT).replace('-', ' ').replace('_', ' ');
        while (key.contains("  ")) {
            key = key.replace("  ", " ");
        }
        return switch (key) {
            case "heal", "healing" -> HEAL;
            case "iron heart", "ironheart" -> IRON_HEART;
            case "fire bolt", "firebolt", "magic arrow", "magicarrow", "missile storm", "missilestorm" -> MAGIC_ARROW;
            case "flight" -> FLIGHT;
            case "grow", "let it grow", "letitgrow" -> GROW;
            case "power mine", "power mining", "powermine" -> POWER_MINE;
            case "detect ore", "detectore" -> DETECT_ORE;
            case "slow" -> SLOW;
            case "sense evil", "senseevil" -> SENSE_EVIL;
            case "smite", "smite evil", "smiteevil" -> SMITE_EVIL;
            case "master builder", "masterbuilder" -> MASTER_BUILDER;
            case "diamond skin", "diamondskin" -> DIAMOND_SKIN;
            case "empower", "glowstone", "glowstone dust" -> EMPOWER;
            case "battery" -> BATTERY;
            case "amethyst shield", "amethystshield", "amethyst", "lapis shield", "lapisshield", "lapus shield",
                    "lapusshield", "lapus" -> LAPIS_SHIELD;
            case "book", "write tome", "write tome of the wise", "writetome" -> WRITE_TOME;
            case "read tome", "read tome of the wise", "readtome", "tome" -> READ_TOME;
            case "restore life", "restorelife", "restore" -> RESTORE_LIFE;
            default -> null;
        };
    }

    /**
     * Short chat lines for one ability. Each line is its own message so it fits
     * in chat. {@link #helpFallback} joins them.
     */
    public static List<String> helpParts(String ability) {
        if (ability == null) {
            return null;
        }
        return switch (ability) {
            case HEAL -> List.of(
                    "Healing: Double-right-click an injured teammate within 2 + ability level blocks while holding paper.",
                    "Healing: If no teammate is targeted and you are injured, you heal yourself instead.",
                    "Healing: Costs 1 mana. A honey bottle in your main inventory (not a bundle) is consumed for +4, bottle included, with or without mana.",
                    "Healing: If you have no mana, a honey bottle still lets you heal, even with no mana levels.",
                    "Healing: The recipient sparkles blue for 6 seconds, and the healing is spread across that sparkle. One paper is consumed. Two in hand consume both and add +2.");
            case IRON_HEART -> List.of(
                    "Iron Heart: Double-right-click while holding an iron ingot. Costs 1 mana. Raises maximum health by ability level for ability level minutes.",
                    "Iron Heart: If another player is targeted within 2 + ability level blocks, they receive the buff instead.",
                    "Iron Heart: Double-right-click again with an iron ingot to cancel your own; you cannot cancel another player's Iron Heart.",
                    "Iron Heart: The recipient sparkles silver for 3 seconds. One ingot is consumed. Two in hand consume both and add +2.",
                    "Iron Heart: If it ends and you still have mana, another mana is spent and it continues, and you gain skill as if you cast it again.",
                    "Iron Heart: A renew spends an ingot only when you are holding one, and that one adds +2.");
            case MAGIC_ARROW -> List.of(
                    "Missile Storm: Double-right-click while holding an arrow. Costs 1 mana. It launches one arrow per level, rounded down, and each deals that much magic damage.",
                    "Missile Storm: Magic damage ignores armor. Skeletons do not deflect it. Witches resist it. Range scales with level. Cooldown is 10 seconds minus skill.",
                    "Missile Storm: Arrows seek hostiles in range. One hostile can be hit more than once. They spread so a shot does not overkill while another target can take it.",
                    "Missile Storm: Charcoal in your hotbar or inventory, not a bundle, adds +2. One charcoal is consumed, and the arrows burn.",
                    "Missile Storm: One arrow is consumed. Two or three consume two and add +2.",
                    "Missile Storm: More than 3 arrows doubles your level, adding at most the square root of those arrows, rounded down. You consume that many, so 4 arrows still costs 2.");
            case FLIGHT -> List.of(
                    "Flight: Double-right-click while holding a feather. Costs 1 mana. Jump to rise, sneak in the air to descend, and sneak on a block to stop.",
                    "Flight: Speed scales with ability level and reaches 70% of walking speed at 10.",
                    "Flight: One feather is consumed. Two in hand consume both and add +2.",
                    "Flight: If it ends and you still have mana, another mana is spent and it continues, and you gain skill as if you cast it again.",
                    "Flight: A renew spends a feather only when you are holding one, and that one adds +2.",
                    "Flight: A phantom membrane in your hotbar or inventory, not a bundle, adds +3. One is consumed, and each renew consumes another to keep that +3.");
            case GROW -> List.of(
                    "Let it grow: Double-right-click a plant while holding any hoe to grow it by your ability level in stages.",
                    "Let it grow: Leftover stages go to the nearest plant within 2 blocks. Double-right-click also still grows random plants around you.",
                    "Let it grow: Costs 1 mana and 3 durability. Unbreaking can skip that wear with the same chance as tilling. Does not replace tilling.",
                    "Let it grow: Each plant grown improves that soil's shown crop-loss modifier by 3.",
                    "Let it grow: Random growth is one plant within 2 blocks by 1 stage, repeated (ability level + hoe bonus) times.",
                    "Let it grow: Hoe bonus: wood/stone 0, copper +1, iron +2, diamond +3, gold +4, netherite +5.");
            case POWER_MINE -> List.of(
                    "Power mining: Double-right-click while holding any pickaxe. Costs 1 mana.",
                    "Power mining: For (ability level × 12) seconds, Tougher extra pickaxe wear on hardened stone is suspended. Ends when the timer expires.",
                    "Power mining: Pickaxe bonus: wood/stone 0, copper +1, iron +2, diamond +3, gold +4, netherite +5.");
            case DETECT_ORE -> List.of(
                    "Detect ore: Double-right-click while holding a compass. The compass is kept. Costs 1 mana. Nether quartz is not required.",
                    "Detect ore: One right-click says the biome you are in and the direction you face.",
                    "Detect ore: The largest nether quartz stack in your hotbar or inventory, not a bundle, adds the square root of its count, rounded. That many quartz are consumed from that stack.",
                    "Detect ore: Ores within 2 blocks of your height come first, then farther up or down, out to your rounded ability level minus the ore difficulty, through walls.",
                    "Detect ore: It tells you the ore and approximate distance. You only sense ores at or below your ability level.",
                    "Detect ore: coal 1, quartz 2, copper 3, iron 4, redstone 6, lapis 7, gold 8, emerald 9, diamond 11, ancient debris 13.");
            case SLOW -> List.of(
                    "Slow: Double-right-click while holding string. Costs 1 mana. Throws a web up to (ability level × 2 + 4) blocks.",
                    "Slow: It bursts on a creature or a solid block and slows creatures within (ability level) blocks of the landing.",
                    "Slow: The slow is 20% + 5% per ability level (max 80%) for (ability level × 6) seconds.",
                    "Slow: Above 100 health, both the slow and the duration are multiplied by 100 divided by that health.",
                    "Slow: It does not affect you, or any player closer to you than the hit. One string is consumed. Two in hand consume both and add +2.");
            case SENSE_EVIL -> List.of(
                    "Sense Evil: Double-right-click a spider eye to cast. Hold the click to eat it instead. Costs 1 mana.",
                    "Sense Evil: If a biome boss is spawned, you turn toward the nearest one and sparkle with a creepy aura for 6 seconds.",
                    "Sense Evil: If it is within 250 × ability level blocks, you also learn the rough distance.",
                    "Sense Evil: If none are spawned, you sparkle as with Healing. One spider eye is consumed. Two in hand consume both and add +2.",
                    "Sense Evil: Wise-teacher marks give off a red mist and a white outline through blocks for 1 minute times your ability level.",
                    "Sense Evil: Hostile mobs within 8 blocks times your ability level are outlined through solid blocks for 1 minute times that level.",
                    "Sense Evil: The outline time shows as a spider eye with your other ability durations.",
                    "Sense Evil: It says whether a biome boss can appear here. In a full chunk at Y=48 or above, it also says the alliance has cleared the area.");
            case SMITE_EVIL -> List.of(
                    "Smite Evil: Double-right-click while holding a golden sword or a heavy golden sword. Costs 1/4 mana.",
                    "Smite Evil: Strikes a creature along your aim up to your ability level in blocks with a normal melee attack from that sword.",
                    "Smite Evil: With at least 1 mana, the crosshair turns gold on a creature within that reach.",
                    "Smite Evil: The strike includes enchantments, durability, and attack cooldown. Undead take extra damage equal to your ability level.",
                    "Smite Evil: A small flash of light appears on the creature hit.");
            case MASTER_BUILDER -> List.of(
                    "Master Builder: Double-right-click while holding a clay brick. Costs 1 mana. Lasts 30 seconds times your ability level.",
                    "Master Builder: While it lasts, you can place solid blocks in the air, and block reach increases by your ability level. Torch rules still apply.",
                    "Master Builder: Double-right-click again with a clay brick to cancel. If it ends and you still have mana, it spends another and continues.",
                    "Master Builder: One brick is consumed. Two in hand consume both and add +2. Continuing gains skill as if you cast it again.",
                    "Master Builder: A renew spends a brick only when you are holding one, and that one adds +2.");
            case DIAMOND_SKIN -> List.of(
                    "Diamond Skin: Double-right-click while holding diamonds. Costs 2 mana. Below 2 it is practice and the diamond is kept. Lasts 30 seconds times its level.",
                    "Diamond Skin: Damage that would reach your health is divided by your ability level. Armor still takes the hit first.",
                    "Diamond Skin: Each point of that damage it prevents shortens Diamond Skin by 3 seconds. Poison, drowning, and suffocation are not reduced.",
                    "Diamond Skin: A cyan sparkle shows for a quarter second when a hit is reduced.",
                    "Diamond Skin: Double-right-click again with a diamond to cancel. If it ends and you still have 2 mana, it spends that and continues.",
                    "Diamond Skin: One diamond is consumed. Two in hand consume both and add +2. Continuing gains skill as if you cast it again.",
                    "Diamond Skin: A renew spends a diamond only when you are holding one, and that one adds +2.");
            case EMPOWER -> List.of(
                    "Empower: Double-right-click while holding glowstone dust. Costs 1 mana. Lasts 30 seconds per ability level, and at least 30 seconds.",
                    "Empower: While it lasts, each later mana ability cast gains extra level equal to the square root of Empower's level.",
                    "Empower: Double-right-click again with glowstone dust to cancel.",
                    "Empower: One dust is consumed. Two in hand consume both and add +2.");
            case BATTERY -> List.of(
                    "Battery: Double-right-click in the air while holding a quartz block. Costs no mana. Lasts 2 minutes per ability level, and at least 2 minutes.",
                    "Battery: While it lasts, mana regeneration is multiplied by 1 plus the square root of your ability level.",
                    "Battery: Double-right-click in the air again with a quartz block to cancel. It ends when your mana reaches your mana level.",
                    "Battery: It continues only while mana is below your level and a quartz block is in hand. One block is consumed. Two in hand consume both and add +2.",
                    "Battery: Continuing gains skill as if you cast it again.");
            case LAPIS_SHIELD -> List.of(
                    "Amethyst Shield: Double-right-click while holding an amethyst shard. A targeted character gets the shield. Otherwise it shields you. Costs 1 mana.",
                    "Amethyst Shield: It lasts 1 minute times your ability level. A force field stays around that character until it ends.",
                    "Amethyst Shield: Damage up to that level is absorbed before armor or health. A hit within that level also skips harmful effects, such as poison.",
                    "Amethyst Shield: Amethyst shards, lapis lazuli, and whole mana pay for the block. One kind costs 1 per 3 points, two kinds 1 per 5, and all three 1 per 7, rounded up.",
                    "Amethyst Shield: The cost alternates those fuels, largest amount first. Items come from your hotbar or inventory, not a bundle, armor, or the offhand.",
                    "Amethyst Shield: If you cannot pay, the hit lands and the shield ends. Double-right-click again with an amethyst shard to cancel.",
                    "Amethyst Shield: One shard is consumed to cast. Two in hand consume both and add +2. One lapis lazuli in your hotbar or inventory is also consumed and adds another +2.");
            case WRITE_TOME -> List.of(
                    "Write Tome of the Wise: Double-right-click while holding a book at experience level 7 or higher. Costs 1 mana.",
                    "Write Tome of the Wise: Above level 30 and at 30 or below, it saves 80% plus 2% per ability level. Below 10 some is lost, and above 10 some is gained.",
                    "Write Tome of the Wise: Above level 30 only the excess is saved, and you return to exactly level 30. At level 30 or below, all experience is saved and you return to level 0.",
                    "Write Tome of the Wise: The tome names you and the experience it holds. That amount is stored on the book.",
                    "Write Tome of the Wise: One book makes one tome. Two or more add +2. Three, four, or five books make 2, 3, or 4 tomes and split the saved experience.",
                    "Write Tome of the Wise: More than five books still uses five, adds +2, and makes 4 tomes. One of those books is the +2 bonus.");
            case READ_TOME -> List.of(
                    "Read Tome of the Wise: Double-right-click a tome that holds experience, at any level. Costs 1 mana and the tome. Below 1 the tome is kept.",
                    "Read Tome of the Wise: You gain 80% of the saved experience, plus 2% per ability level, above or below level 30. Sneak and right-click to open it instead.");
            case RESTORE_LIFE -> List.of(
                    "Restore Life: Double-right-click in the air or on a player while holding a diamond block. Clicking a block with it still places the block.",
                    "Restore Life: It targets a friendly player within 2 plus ability level blocks, the way Healing does, and otherwise you.",
                    "Restore Life: It gives one life. The target must have fewer lives than player.death.lives. Lives must be on, and Restore Life Available starts on.",
                    "Restore Life: It costs 4 mana and diamonds in your hotbar or inventory. The count is 17 minus their lives minus your level, rounded down, and at least 3.",
                    "Restore Life: A missing requirement spends nothing and says what is required. Below 1, practice spends the 4 mana and the block, and keeps the diamonds.",
                    "Restore Life: One diamond block is consumed on a real cast. Two in hand consume both and add +2. The diamonds that pay for the life are separate.");
            default -> null;
        };
    }

    public static List<String> helpKeys(String ability) {
        List<String> parts = helpParts(ability);
        if (parts == null) {
            return List.of();
        }
        String base = helpKey(ability);
        List<String> keys = new ArrayList<>(parts.size());
        for (int i = 0; i < parts.size(); i++) {
            keys.add(i == 0 ? base : base + "." + (i + 1));
        }
        return keys;
    }

    public static String helpFallback(String ability) {
        List<String> parts = helpParts(ability);
        if (parts == null) {
            return null;
        }
        return String.join(" ", parts);
    }

    public static final String INDEX_HELP_KEY = "tougher.ability.index.help";
    public static final String POWER_HELP_KEY = "tougher.ability.power.help";

    public static List<String> powerHelpLines() {
        return List.of(
                "Base power: skill, the square root of times you have used that ability, plus item bonuses, minus 2. Effective level is at least 0.",
                "A cast consumes one catalyst. Two in hand consume both for +2. One consumed catalyst adds nothing. Swords, hoes, and pickaxes are kept.",
                "A renew spends no catalyst unless you are holding it. Holding it spends one and adds +2.",
                "Gold armor adds 1 mana level after 90 seconds, then 180, 360, and 720 seconds more. Four pieces put on together reach +4 in 1350 seconds. A piece that comes off loses its bonus.",
                "A golden item held for 30 seconds, such as a golden hoe, adds 1 more mana level. Gold ingots, nuggets, and blocks do not.",
                "Below 1 the cast is practice: mana and focus are spent, the cooldown starts, and you gain one use. Diamond Skin practices below 2 and keeps the diamond. Read Tome keeps the tome.",
                "Redstone dust in the offhand adds half your skill, up to the square root of that stack rounded down. Success consumes that bonus, rounded down. Practice keeps the dust.");
    }

    public static List<String> powerHelpKeys() {
        return List.of(
                POWER_HELP_KEY,
                "tougher.ability.power.help.2",
                "tougher.ability.power.help.3",
                "tougher.ability.power.help.4",
                "tougher.ability.power.help.5",
                "tougher.ability.power.help.6",
                "tougher.ability.power.help.7");
    }

    public static String powerHelpFallback() {
        return String.join(" ", powerHelpLines());
    }

    public static String indexHelpFallback() {
        return String.join(" ", indexHelpLines());
    }

    public static List<String> indexHelpKeys() {
        return List.of(
                INDEX_HELP_KEY,
                "tougher.ability.index.heal",
                "tougher.ability.index.iron_heart",
                "tougher.ability.index.flight",
                "tougher.ability.index.magic_arrow",
                "tougher.ability.index.grow",
                "tougher.ability.index.power_mine",
                "tougher.ability.index.detect_ore",
                "tougher.ability.index.slow",
                "tougher.ability.index.sense_evil",
                "tougher.ability.index.smite_evil",
                "tougher.ability.index.master_builder",
                "tougher.ability.index.diamond_skin",
                "tougher.ability.index.redstone",
                "tougher.ability.index.battery",
                "tougher.ability.index.lapis_shield",
                "tougher.ability.index.write_tome",
                "tougher.ability.index.read_tome",
                "tougher.ability.index.restore_life",
                "tougher.ability.index.unlock",
                "tougher.ability.index.unlock.2",
                "tougher.ability.index.restore_life_keep",
                "tougher.ability.index.redstone_offhand",
                "tougher.ability.index.wise",
                "tougher.ability.index.wise.cap",
                "tougher.ability.index.wise.2",
                "tougher.ability.index.topic");
    }

    public static List<String> indexHelpLines() {
        return List.of(
                "Mana abilities — hold the item and press ? for details:",
                "Paper: Healing",
                "Iron ingot: Iron Heart",
                "Feather: Flight",
                "Arrow: Missile Storm",
                "Any hoe: Let it grow",
                "Any pickaxe: Power mining",
                "Compass: Detect ore",
                "String: Slow",
                "Spider eye: Sense Evil",
                "Golden sword or heavy golden sword: Smite Evil",
                "Clay brick: Master Builder",
                "Diamond: Diamond Skin",
                "Glowstone dust: Empower",
                "Quartz block: Battery",
                "Amethyst shard: Amethyst Shield",
                "Book: Write Tome of the Wise",
                "Tome: Read Tome of the Wise",
                "Diamond block: Restore Life",
                "Every ability's level is its skill plus item bonuses, minus 2, and at least 0.",
                "Below 1 the cast is practice: mana and focus are spent, the cooldown starts, and you gain one use. Diamond Skin practices below 2 and keeps the diamond. Read Tome keeps the tome.",
                "Restore Life: a missing requirement spends nothing. Below 1, practice spends 4 mana and the diamond block, and keeps the diamonds.",
                "Redstone dust in the offhand adds half your skill, up to the square root of that stack rounded down. Success consumes that bonus, rounded down. Practice keeps the dust.",
                "Hold a focus and he teaches that ability for half the house points in uses. At 24 points, 30 emeralds from inventory or either hand; each point above takes 2 off, down to free.",
                "Hold emeralds and he picks an ability at random for the house points. A new hunt, or killing more than half the marks, offers the lesson. He names what to hold only then.",
                "Hold redstone blocks. The first gift takes 1, then up to 4, 9, and so on. Mana learning grows by the square root of the blocks he takes.",
                "Type /tougher ability help <name> for one ability.");
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

    public static double manaCost(String ability) {
        if (SMITE_EVIL.equals(ability)) {
            return SMITE_EVIL_MANA_COST;
        }
        if (DETECT_ORE.equals(ability)) {
            return DETECT_ORE_MANA_COST;
        }
        if (DIAMOND_SKIN.equals(ability)) {
            return DIAMOND_SKIN_MANA_COST;
        }
        if (RESTORE_LIFE.equals(ability)) {
            return RESTORE_LIFE_MANA_COST;
        }
        if (BATTERY.equals(ability)) {
            return 0;
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

    /**
     * Fresh cast of a consumable catalyst. One held item is spent. Two or more
     * spend two. Tools and weapons are not passed through here.
     */
    public static int catalystConsumeCount(int stackCount) {
        if (stackCount <= 0) {
            return 0;
        }
        return stackCount == 1 ? 1 : 2;
    }

    /** +{@link #CATALYST_BONUS} only when a fresh cast spends two catalysts. */
    public static int catalystBonus(int stackCount) {
        return stackCount >= 2 ? CATALYST_BONUS : 0;
    }

    /** Auto-renew spends one held catalyst, and that one grants {@link #CATALYST_BONUS}. */
    public static int renewConsumeCount(int stackCount) {
        return stackCount >= 1 ? 1 : 0;
    }

    public static int renewCatalystBonus(int stackCount) {
        return stackCount >= 1 ? CATALYST_BONUS : 0;
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
     * Extra ability level from held supplies: extra catalyst in hand, hoe/pickaxe
     * material, the largest hotbar or backpack nether quartz stack for Detect ore,
     * charcoal in that same inventory for Missile Storm, and a phantom membrane there for Flight.
     * {@code quartzStack} is that one stack's count, not a sum and not a bundle.
     */
    public static int supplyBonus(
            String ability, String heldItemId, int heldCount, boolean redstone, int quartzStack) {
        return supplyBonus(ability, heldItemId, heldCount, redstone, quartzStack, false);
    }

    public static int supplyBonus(
            String ability,
            String heldItemId,
            int heldCount,
            boolean redstone,
            int quartzStack,
            boolean charcoal) {
        return supplyBonus(ability, heldItemId, heldCount, redstone, quartzStack, charcoal, false);
    }

    public static int supplyBonus(
            String ability,
            String heldItemId,
            int heldCount,
            boolean redstone,
            int quartzStack,
            boolean charcoal,
            boolean membrane) {
        if (ability == null) {
            return 0;
        }
        int bonus = DETECT_ORE.equals(ability) ? detectOreQuartzBonus(quartzStack) : 0;
        if (MAGIC_ARROW.equals(ability)) {
            bonus += charcoalArrowBonus(charcoal);
        }
        if (FLIGHT.equals(ability)) {
            bonus += flightMembraneBonus(membrane);
        }
        if (!ability.equals(abilityForItem(heldItemId))) {
            return bonus;
        }
        if (GROW.equals(ability)) {
            return bonus + growHoeBonus(heldItemId);
        }
        if (POWER_MINE.equals(ability)) {
            return bonus + pickaxeBonus(heldItemId);
        }
        if (DETECT_ORE.equals(ability) || SMITE_EVIL.equals(ability)) {
            return bonus;
        }
        if (MAGIC_ARROW.equals(ability) && heldCount > MISSILE_STORM_HAND) {
            return bonus;
        }
        return bonus + catalystBonus(heldCount);
    }

    /** Ability level before item bonuses: skill, {@code sqrt(uses)}. */
    public static double basePower(int uses) {
        return skill(uses);
    }

    /** {@link #basePower} plus optional consumed-item / hoe bonus. */
    public static double power(int manaLevel, int uses, int itemBonus) {
        return Math.max(0.0, basePower(uses)) + Math.max(0, itemBonus);
    }

    /** Missile Storm uses a 10-second cooldown that shrinks with skill. Mana level does not change it or its power. */
    public static boolean usesShortCooldown(String ability) {
        return MAGIC_ARROW.equals(ability);
    }

    /** +2 when the hotbar or backpack holds charcoal. Bundles, armor, and the offhand do not count. */
    public static int charcoalArrowBonus(boolean hasCharcoal) {
        return hasCharcoal ? CHARCOAL_ARROW_BONUS : 0;
    }

    /** +3 when the hotbar or backpack holds a phantom membrane. Bundles, armor, and the offhand do not count. */
    public static int flightMembraneBonus(boolean hasMembrane) {
        return hasMembrane ? FLIGHT_MEMBRANE_BONUS : 0;
    }

    /** +2 when the hotbar or backpack holds lapis lazuli for an Amethyst Shield cast. One lapis is spent. */
    public static int lapisCastBonus(boolean hasLapis) {
        return hasLapis ? CATALYST_BONUS : 0;
    }

    /** Saved Fire bolt practice becomes Magic Arrow practice. */
    public static Map<String, Integer> foldFireBoltUses(Map<String, Integer> uses) {
        if (uses == null || !uses.containsKey(FIRE_BOLT)) {
            return uses == null ? Map.of() : uses;
        }
        Map<String, Integer> next = new HashMap<>(uses);
        int moved = Math.max(0, next.remove(FIRE_BOLT));
        if (moved > 0) {
            next.merge(MAGIC_ARROW, moved, Integer::sum);
        }
        return next;
    }

    /** Replaces a saved Fire bolt lesson with Magic Arrow and drops the duplicate. */
    public static List<String> foldFireBoltList(List<String> ids) {
        if (ids == null || !ids.contains(FIRE_BOLT)) {
            return ids == null ? List.of() : ids;
        }
        List<String> next = new ArrayList<>();
        for (String id : ids) {
            String ability = FIRE_BOLT.equals(id) ? MAGIC_ARROW : id;
            if (!next.contains(ability)) {
                next.add(ability);
            }
        }
        return next;
    }

    /** The later of the two cooldowns remains. */
    public static Map<String, Long> foldFireBoltLastUse(Map<String, Long> lastUse) {
        if (lastUse == null || !lastUse.containsKey(FIRE_BOLT)) {
            return lastUse == null ? Map.of() : lastUse;
        }
        Map<String, Long> next = new HashMap<>(lastUse);
        long fire = next.remove(FIRE_BOLT);
        long arrow = next.getOrDefault(MAGIC_ARROW, -1L);
        long kept = Math.max(arrow, fire);
        if (kept >= 0L) {
            next.put(MAGIC_ARROW, kept);
        }
        return next;
    }

    /** A wise teacher who offered Fire bolt now offers Magic Arrow. Let there be light is retired. */
    public static String storedWiseAbility(String stored) {
        if (FIRE_BOLT.equals(stored)) {
            return MAGIC_ARROW;
        }
        if (LIGHT.equals(stored)) {
            return "";
        }
        return stored;
    }

    /** Drops a retired ability from saved practice. */
    public static Map<String, Integer> dropAbilityUses(Map<String, Integer> uses, String retired) {
        if (uses == null || retired == null || !uses.containsKey(retired)) {
            return uses == null ? Map.of() : uses;
        }
        Map<String, Integer> next = new HashMap<>(uses);
        next.remove(retired);
        return next;
    }

    /** Drops a retired ability from a saved lesson list. */
    public static List<String> dropAbilityList(List<String> ids, String retired) {
        if (ids == null || retired == null || !ids.contains(retired)) {
            return ids == null ? List.of() : ids;
        }
        List<String> next = new ArrayList<>();
        for (String id : ids) {
            if (!retired.equals(id)) {
                next.add(id);
            }
        }
        return next;
    }

    /** Drops a retired ability's cooldown. */
    public static Map<String, Long> dropAbilityLastUse(Map<String, Long> lastUse, String retired) {
        if (lastUse == null || retired == null || !lastUse.containsKey(retired)) {
            return lastUse == null ? Map.of() : lastUse;
        }
        Map<String, Long> next = new HashMap<>(lastUse);
        next.remove(retired);
        return next;
    }

    /**
     * Effective level: skill plus item bonuses plus the world offset, floored at {@link #MIN_POWER}.
     * Mana level is not added. Empower's extra level is applied by the caller after this.
     */
    public static double power(String ability, int uses, int itemBonus, int levelOffset) {
        return power(ability, uses, itemBonus, levelOffset, 0);
    }

    /**
     * Same level, with offhand redstone added to skill before item bonuses and the offset.
     * {@code offhandRedstone} is that one stack. Zero leaves skill unchanged.
     */
    public static double power(String ability, int uses, int itemBonus, int levelOffset, int offhandRedstone) {
        double base = skill(uses) + redstoneSkillBoost(uses, offhandRedstone);
        return Math.max(MIN_POWER, base + Math.max(0, itemBonus) + levelOffset);
    }

    /** Floor of the square root of one offhand redstone stack. Zero when the stack is empty. */
    public static int redstoneDustCap(int stackCount) {
        if (stackCount <= 0) {
            return 0;
        }
        return (int) Math.floor(Math.sqrt(stackCount));
    }

    /**
     * Added to skill before item bonuses and the level offset.
     * Half of skill, and no more than {@link #redstoneDustCap(int)}.
     */
    public static double redstoneSkillBoost(int uses, int offhandCount) {
        int cap = redstoneDustCap(offhandCount);
        if (cap <= 0) {
            return 0.0;
        }
        return Math.min(skill(uses) * 0.5, cap);
    }

    /** Dust taken from the offhand when a cast succeeds. The bonus, rounded down. */
    public static int redstoneDustToConsume(int uses, int offhandCount) {
        double boost = redstoneSkillBoost(uses, offhandCount);
        if (!(boost > 0.0) || Double.isNaN(boost)) {
            return 0;
        }
        return (int) Math.floor(boost);
    }

    /** True when this level spends the cast and gains a use without taking effect. */
    public static boolean practices(String ability, double level) {
        double need = DIAMOND_SKIN.equals(ability) ? DIAMOND_SKIN_MIN_LEVEL : PRACTICE_LEVEL;
        return level < need;
    }

    public static String powerLabel(double power) {
        double value = Math.max(0.0, power);
        if (Math.abs(value - Math.round(value)) < 1.0E-6) {
            return Long.toString(Math.round(value));
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    public static boolean isMainInventorySlot(int slot) {
        return slot >= 0 && slot < MAIN_INVENTORY_SLOTS;
    }

    public static boolean autoRenews(String ability) {
        return FLIGHT.equals(ability)
                || IRON_HEART.equals(ability)
                || MASTER_BUILDER.equals(ability)
                || DIAMOND_SKIN.equals(ability)
                || BATTERY.equals(ability);
    }

    /**
     * Diamonds Restore Life takes from the hotbar and backpack.
     * 17 minus the target's lives, minus the caster's effective level rounded down, and at least 3.
     */
    public static int restoreLifeDiamonds(int targetLives, double effectiveLevel) {
        int fromLevel = (int) Math.floor(Math.max(0.0, effectiveLevel));
        return Math.max(RESTORE_LIFE_DIAMOND_MIN, RESTORE_LIFE_DIAMOND_BASE - targetLives - fromLevel);
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

    /** Duration in ticks: 30 seconds per point of effective ability level. */
    public static int masterBuilderDurationTicks(double power) {
        return Math.max(0, (int) Math.round(Math.max(0.0, power) * MASTER_BUILDER_SECONDS_PER_POWER * 20.0));
    }

    /** Stored Diamond Skin level. A cast below {@link #DIAMOND_SKIN_MIN_LEVEL} practices instead. */
    public static double diamondSkinLevel(double power) {
        return Math.max(0.0, power);
    }

    /** Duration in ticks: 30 seconds per point of effective ability level. */
    public static int diamondSkinDurationTicks(double power) {
        return Math.max(0, (int) Math.round(Math.max(0.0, power) * DIAMOND_SKIN_SECONDS_PER_POWER * 20.0));
    }

    /** Duration in ticks: 30 seconds per ability level, and at least 30 seconds. */
    public static int empowerDurationTicks(double power) {
        int ticks = (int) Math.round(Math.max(0.0, power) * EMPOWER_SECONDS_PER_POWER * 20.0);
        return Math.max(EMPOWER_MIN_SECONDS * 20, ticks);
    }

    /** Extra effective level on later casts while Empower is active. */
    public static double empowerCastBonus(double empowerLevel) {
        if (empowerLevel <= 0.0) {
            return 0.0;
        }
        return Math.sqrt(empowerLevel);
    }

    /** Duration in ticks: 2 minutes per ability level, and at least 2 minutes. */
    public static int batteryDurationTicks(double power) {
        int ticks = (int) Math.round(Math.max(0.0, power) * BATTERY_SECONDS_PER_POWER * 20.0);
        return Math.max(BATTERY_MIN_SECONDS * 20, ticks);
    }

    /** Duration in ticks: 1 minute per ability level. */
    public static int lapisShieldDurationTicks(double power) {
        return Math.max(0, (int) Math.round(Math.max(0.0, power) * LAPIS_SHIELD_SECONDS_PER_POWER * 20.0));
    }

    /** Ticks a Sense Evil cast keeps marks and nearby hostiles outlined: 1 minute per effective level. */
    public static int senseMarkOutlineTicks(double power) {
        return Math.max(0, (int) Math.round(Math.max(0.0, power) * SENSE_OUTLINE_SECONDS_PER_POWER * 20.0));
    }

    /** Reach of the Sense Evil hostile outline: 8 blocks per effective level. */
    public static double senseHostileRange(double power) {
        return SENSE_HOSTILE_BLOCKS_PER_POWER * Math.max(0.0, power);
    }

    /** True when a hostile this far away, as a squared distance, is inside {@link #senseHostileRange}. */
    public static boolean senseHostileInRange(double distanceSquared, double power) {
        if (distanceSquared < 0.0) {
            return false;
        }
        double range = senseHostileRange(power);
        return range > 0.0 && distanceSquared <= range * range;
    }

    /**
     * Mana regen multiplier while Battery lasts: {@code 1 + sqrt(level)}.
     * Effective level is at least 1, so a non-positive level still multiplies by 2.
     */
    public static double batteryRegenMultiplier(double power) {
        return 1.0 + Math.sqrt(Math.max(1.0, power));
    }

    public static double fireRange(double power) {
        return FIRE_BASE_RANGE + Math.max(0.0, power) * 2.0;
    }

    /**
     * Levels added for a Missile Storm cast when the hand holds more than {@link #MISSILE_STORM_HAND}
     * arrows. That added amount is the base level, rounded down, and at most the square root of the
     * stack, rounded down. Two or three arrows keep the ordinary catalyst bonus instead.
     */
    public static int missileStormArrowBonus(double baseLevel, int heldArrows) {
        if (heldArrows <= MISSILE_STORM_HAND || !(baseLevel > 0.0)) {
            return 0;
        }
        int cap = (int) Math.floor(Math.sqrt(heldArrows));
        int doubling = (int) Math.floor(baseLevel);
        if (cap <= 0 || doubling <= 0) {
            return 0;
        }
        return Math.min(doubling, cap);
    }

    /** Arrows spent by one Missile Storm cast. Four arrows still costs 2 when the bonus is 2. */
    public static int missileStormArrowsToConsume(double baseLevel, int heldArrows) {
        if (heldArrows <= 0) {
            return 0;
        }
        if (heldArrows <= MISSILE_STORM_HAND) {
            return catalystConsumeCount(heldArrows);
        }
        int bonus = missileStormArrowBonus(baseLevel, heldArrows);
        return bonus <= 0 ? 1 : Math.min(heldArrows, bonus);
    }

    /** Effective level after the Missile Storm arrow bonus. Other abilities are unchanged. */
    public static double missileStormLevel(double baseLevel, int heldArrows) {
        return baseLevel + missileStormArrowBonus(baseLevel, heldArrows);
    }

    /** Shots in the volley, and the magic damage of each one: the effective level, rounded down. */
    public static int projectileDamage(double power) {
        return Math.max(0, (int) Math.floor(Math.max(0.0, power)));
    }

    /** Seconds a charcoal-fed Missile Storm arrow burns its target. This stays the rounded ability level. */
    public static int projectileBurnSeconds(double power) {
        return Math.max(0, (int) Math.round(Math.max(0.0, power)));
    }

    /**
     * One target index per shot. A nearer aim score is preferred. A shot goes to a hostile that can
     * take the whole hit, then to one that is still alive, then stacks on the nearest living hostile.
     */
    public static int[] assignMissileShots(int shots, int damage, double[] health, double[] aimScore) {
        if (shots <= 0 || health == null || health.length == 0) {
            return new int[0];
        }
        int hit = Math.max(0, damage);
        double[] left = new double[health.length];
        for (int i = 0; i < health.length; i++) {
            left[i] = health[i];
        }
        int[] assigned = new int[shots];
        for (int shot = 0; shot < shots; shot++) {
            int pick = missileCandidate(health, left, aimScore, hit, true);
            if (pick < 0) {
                pick = missileCandidate(health, left, aimScore, hit, false);
            }
            if (pick < 0) {
                pick = missileCandidate(health, null, aimScore, hit, false);
            }
            assigned[shot] = pick;
            if (pick >= 0 && hit > 0) {
                left[pick] -= hit;
            }
        }
        return assigned;
    }

    /**
     * Lowest aim score among living hostiles. {@code requireFull} keeps a target whose remaining
     * health can absorb {@code hit}. Otherwise any remaining health above 0 qualifies. A null
     * {@code left} uses the original health, so a later shot may stack after the target is spent.
     */
    private static int missileCandidate(
            double[] health, double[] left, double[] aimScore, int hit, boolean requireFull) {
        int best = -1;
        double bestAim = 0.0;
        for (int i = 0; i < health.length; i++) {
            if (!(health[i] > 0.0)) {
                continue;
            }
            double pool = left == null ? health[i] : left[i];
            if (left == null) {
                if (!(pool > 0.0)) {
                    continue;
                }
            } else if (requireFull) {
                if (pool < hit) {
                    continue;
                }
            } else if (!(pool > 0.0)) {
                continue;
            }
            double score = aimScore == null || i >= aimScore.length ? 0.0 : aimScore[i];
            if (best < 0 || score < bestAim) {
                best = i;
                bestAim = score;
            }
        }
        return best;
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

    /** Rounded ability level used for detect-ore range and ore-tier gating. */
    public static int detectOreSkill(double power) {
        return Math.max(0, (int) Math.round(Math.max(0.0, power)));
    }

    /**
     * Cube reach for one ore: rounded ability level, minus that ore's difficulty.
     * Never negative.
     */
    public static int detectOreRange(double power, int difficulty) {
        int range = DETECT_ORE_BASE_RANGE
                + DETECT_ORE_RANGE_PER_POWER * detectOreSkill(power)
                - Math.max(0, difficulty);
        return Math.max(0, range);
    }

    /** Farthest cube in a scan: coal, the easiest ore, when the player can sense it. */
    public static int detectOreScanRange(double power) {
        if (detectOreSkill(power) < 1) {
            return 0;
        }
        return detectOreRange(power, 1);
    }

    /** True when a block is inside the detect-ore cube of {@code range}. */
    public static boolean withinDetectOreReach(int px, int py, int pz, int x, int y, int z, int range) {
        return Math.abs(x - px) <= range && Math.abs(y - py) <= range && Math.abs(z - pz) <= range;
    }

    /** True when {@code oreY} is within {@link #DETECT_ORE_LEVEL_BAND} of the player's block height. */
    public static boolean inDetectOreLevelBand(int oreY, int playerY) {
        return Math.abs(oreY - playerY) <= DETECT_ORE_LEVEL_BAND;
    }

    /** How far the web flies: ability level × 2 + 4 blocks. */
    public static double slowProjectileRange(double power) {
        return Math.max(0.0, power) * 2.0 + 4.0;
    }

    /** Burst radius around the landing: the ability level, in blocks. */
    public static double slowBlastRadius(double power) {
        return Math.max(0.0, power);
    }

    /**
     * Full effect at {@link #SLOW_TOUGH_HEALTH} or below. Above that, both the
     * speed penalty and the duration scale by {@code 100 / health}.
     */
    public static double slowHealthFactor(double maxHealth) {
        if (!(maxHealth > SLOW_TOUGH_HEALTH)) {
            return 1.0;
        }
        return SLOW_TOUGH_HEALTH / maxHealth;
    }

    /**
     * Caster is never slowed. Another player is skipped when they are closer to
     * the caster than the hit. Mobs are not skipped for distance.
     */
    public static boolean slowAffects(
            boolean caster, boolean player, double distanceFromCaster, double targetDistanceFromCaster) {
        if (caster) {
            return false;
        }
        return !player || distanceFromCaster >= targetDistanceFromCaster;
    }

    public static int slowDurationTicks(double power) {
        return Math.max(0, (int) Math.round(Math.max(0.0, power) * SLOW_SECONDS_PER_POWER * 20.0));
    }

    /** Unscaled duration times {@link #slowHealthFactor}. */
    public static int slowDurationTicks(double power, double maxHealth) {
        int base = slowDurationTicks(power);
        if (base <= 0) {
            return 0;
        }
        return Math.max(0, (int) Math.round(base * slowHealthFactor(maxHealth)));
    }

    /**
     * Movement-speed reduction from 0 to {@link #SLOW_MAX}, then scaled by
     * {@link #slowHealthFactor} when max health is above 100.
     */
    public static double slowAmount(double power, double maxHealth) {
        double amount = SLOW_BASE + SLOW_PER_POWER * Math.max(0.0, power);
        if (amount > SLOW_MAX) {
            amount = SLOW_MAX;
        }
        return amount * slowHealthFactor(maxHealth);
    }

    /**
     * Rounded square root of one nether quartz stack. The cast consumes this many
     * from that stack. Zero when there is no stack. {@code stackCount} is the largest
     * hotbar or backpack stack, not a sum and not items inside a bundle.
     */
    public static int detectOreQuartzBonus(int stackCount) {
        if (stackCount <= 0) {
            return 0;
        }
        return (int) Math.round(Math.sqrt(stackCount));
    }

    /** Same count as {@link #detectOreQuartzBonus(int)}. */
    public static int detectOreQuartzConsumeCount(int stackCount) {
        return detectOreQuartzBonus(stackCount);
    }

    /** {@code stackCount} is the largest main-inventory nether quartz stack. */
    public static int detectOreItemBonus(int stackCount) {
        return detectOreQuartzBonus(stackCount);
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

    public static String senseEvilAreaSafeFallback() {
        return "This area is safe from great evil appearing...";
    }

    public static String senseEvilAreaRiskFallback() {
        return "This area could have great evil appear...";
    }

    public static String senseEvilClearedFallback() {
        return "This area has been cleared for the alliance!";
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
    /**
     * Minimum rounded Detect ore skill to sense this ore. 0 means not an ore.
     * Coal 1, quartz 2, copper 3, iron 4, redstone 6, lapis 7, gold 8, emerald 9,
     * diamond 11, ancient debris 13.
     */
    public static int oreDetectLevel(String blockId) {
        return switch (oreFamily(blockId)) {
            case "coal" -> 1;
            case "quartz" -> 2;
            case "copper" -> 3;
            case "iron" -> 4;
            case "redstone" -> 6;
            case "lapis" -> 7;
            case "gold" -> 8;
            case "emerald" -> 9;
            case "diamond" -> 11;
            case "ancient_debris" -> 13;
            default -> 0;
        };
    }

    public static int oreValue(String blockId) {
        int level = oreDetectLevel(blockId);
        return level > 0 ? level * 10 : 0;
    }

    public static boolean isDetectableOre(String blockId) {
        return oreDetectLevel(blockId) > 0;
    }

    public static boolean canDetectOre(String blockId, double power) {
        int required = oreDetectLevel(blockId);
        return required > 0 && detectOreSkill(power) >= required;
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

    /**
     * Best deposit in the scan. Ore within {@link #DETECT_ORE_LEVEL_BAND} blocks of
     * the player's height wins over anything farther up or down. Inside that band,
     * and when the band is empty, {@link #bestDeposit} still picks by value, then size.
     */
    public static OreDeposit bestDepositAtPlayerLevel(List<OreSample> samples, int px, int py, int pz) {
        List<OreSample> nearLevel = new ArrayList<>();
        if (samples != null) {
            for (OreSample sample : samples) {
                if (sample != null && inDetectOreLevelBand(sample.y(), py)) {
                    nearLevel.add(sample);
                }
            }
        }
        OreDeposit atLevel = bestDeposit(clusterDeposits(nearLevel, px, py, pz));
        if (atLevel != null) {
            return atLevel;
        }
        return bestDeposit(clusterDeposits(samples, px, py, pz));
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

    /** Sneaking stops flight only while standing on a block. In the air, sneak still descends. */
    public static boolean flightStops(boolean sneaking, boolean onBlock) {
        return sneaking && onBlock;
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
     * 2 minutes minus 3 seconds per (mana level + skill), minimum 1 second.
     * Hits the 1-second floor at combined rating 40. Missile Storm uses
     * {@link #FIRE_BOLT_COOLDOWN_SECONDS} minus skill instead. Mana level does not change that cooldown.
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
        if (usesShortCooldown(ability)) {
            return fireBoltCooldownTicks(manaLevel, uses);
        }
        return cooldownTicks(manaLevel, uses);
    }

    /** 10 seconds minus skill, not below 0. {@code manaLevel} does not change this cooldown. */
    public static int fireBoltCooldownTicks(int manaLevel, int uses) {
        double seconds = FIRE_BOLT_COOLDOWN_SECONDS - skill(uses);
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
