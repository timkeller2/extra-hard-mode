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
    public static final int MANA_COST = 1;
    public static final int DIAMOND_SKIN_MANA_COST = 2;
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
    /** Magic arrow cooldown starts at this many seconds, then subtracts skill. */
    public static final int FIRE_BOLT_COOLDOWN_SECONDS = 10;
    /** Extra catalyst in hand when two are consumed. */
    public static final int CATALYST_BONUS = 2;
    /** Charcoal in the hotbar or backpack adds this to Magic Arrow and is spent one at a time. */
    public static final int CHARCOAL_ARROW_BONUS = 2;
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
    public static final String SPIDER_EYE_ID = "minecraft:spider_eye";
    public static final String GOLDEN_SWORD_ID = "minecraft:golden_sword";
    /** Clay brick item, not the bricks block and not stone bricks. */
    public static final String BRICK_ID = "minecraft:brick";
    /** Master Builder lasts this many seconds per point of effective ability level. */
    public static final int MASTER_BUILDER_SECONDS_PER_POWER = 30;
    public static final String DIAMOND_ID = "minecraft:diamond";
    public static final String REDSTONE_ID = "minecraft:redstone";
    /** Empower lasts this many seconds per point of effective ability level, and at least {@link #EMPOWER_MIN_SECONDS}. */
    public static final int EMPOWER_SECONDS_PER_POWER = 30;
    public static final int EMPOWER_MIN_SECONDS = 30;
    public static final String QUARTZ_BLOCK_ID = "minecraft:quartz_block";
    /** Battery lasts this many seconds per point of effective ability level, and at least {@link #BATTERY_MIN_SECONDS}. */
    public static final int BATTERY_SECONDS_PER_POWER = 120;
    public static final int BATTERY_MIN_SECONDS = 120;
    public static final String LAPIS_LAZULI_ID = "minecraft:lapis_lazuli";
    public static final String BOOK_ID = "minecraft:book";
    /** Lapis Shield lasts this many seconds per point of effective ability level. */
    public static final int LAPIS_SHIELD_SECONDS_PER_POWER = 60;
    /** Diamond Skin lasts this many seconds per point of effective ability level. */
    public static final int DIAMOND_SKIN_SECONDS_PER_POWER = 30;
    /** A cast with no skill and no item bonus still counts as this level. */
    public static final double DIAMOND_SKIN_MIN_LEVEL = 2.0;
    /** Cyan sparkle when Diamond Skin reduces a hit. 0.25 seconds. */
    public static final int DIAMOND_SKIN_SPARKLE_TICKS = 5;
    /** Distance at which Sense Evil reports a rough range: 250 × ability level. */
    public static final double SENSE_EVIL_RANGE_PER_POWER = 250.0;
    public static final int SENSE_EVIL_SPARKLE_TICKS = 120;
    public static final int GROW_RANGE = 2;
    public static final int GROW_HOE_DAMAGE = 3;
    public static final int POWER_MINE_SECONDS_PER_POWER = 12;
    /** Reach before the ore's difficulty is subtracted. */
    public static final int DETECT_ORE_BASE_RANGE = 4;
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
                    READ_TOME);

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
        if (GOLDEN_SWORD_ID.equals(itemId)) {
            return SMITE_EVIL;
        }
        if (BRICK_ID.equals(itemId)) {
            return MASTER_BUILDER;
        }
        if (DIAMOND_ID.equals(itemId)) {
            return DIAMOND_SKIN;
        }
        if (REDSTONE_ID.equals(itemId)) {
            return EMPOWER;
        }
        if (QUARTZ_BLOCK_ID.equals(itemId)) {
            return BATTERY;
        }
        if (LAPIS_LAZULI_ID.equals(itemId)) {
            return LAPIS_SHIELD;
        }
        if (BOOK_ID.equals(itemId)) {
            return WRITE_TOME;
        }
        return null;
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
        return path.startsWith("golden_");
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
            case MAGIC_ARROW -> "Magic arrow";
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
            case LAPIS_SHIELD -> "Lapis Shield";
            case WRITE_TOME -> "Write Tome of the Wise";
            case READ_TOME -> "Read Tome of the Wise";
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
            case MAGIC_ARROW -> "An arrow can be taught to want a target, and charcoal still remembers the fire.";
            case FLIGHT -> "The sky is only heavy if you agree that it is.";
            case GROW -> "Green things listen, if you speak with a hoe.";
            case POWER_MINE -> "Stone yields to a pick that has been properly introduced.";
            case DETECT_ORE -> "The ground is talkative, if you hold a compass still.";
            case SLOW -> "Haste is a habit. String can interrupt it.";
            case SENSE_EVIL -> "Large hungers leave a smell on the wind.";
            case SMITE_EVIL -> "A golden sword is a polite way to end a monster.";
            case MASTER_BUILDER -> "Stone will hold for a while, even where the ground does not agree.";
            case DIAMOND_SKIN -> "A diamond can be asked to stand between you and the blow.";
            case EMPOWER -> "Redstone will lend its strength to the next working.";
            case BATTERY -> "A block of quartz will hold a charge for your mana.";
            case LAPIS_SHIELD -> "Lapis can stand in the air between a body and the blow.";
            case WRITE_TOME -> "Experience can be set down in a book, and carried.";
            case READ_TOME -> "A careful reading gives back only a share of what was written down.";
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
                "Hold paper and right-click an injured person, or yourself if you are hurt. It costs 1 mana.";
            case IRON_HEART ->
                "Hold an iron ingot and right-click. Right-click again with an ingot to cancel your own.";
            case MAGIC_ARROW ->
                "Hold an arrow and right-click. It seeks a creature along your aim. Charcoal in your inventory adds +2 and makes it burn. It costs 1 mana.";
            case FLIGHT -> "Hold a feather and right-click. Jump to rise, sneak in the air to descend, and sneak on a block to stop.";
            case GROW -> "Hold any hoe and right-click a plant. It costs 1 mana and some hoe durability.";
            case POWER_MINE -> "Hold any pickaxe and right-click. Hardened stone stops eating extra durability for a while.";
            case DETECT_ORE ->
                "Hold a compass and right-click. You turn toward the best ore you are skilled enough to sense, preferring ore within 2 blocks of your height.";
            case SLOW -> "Hold string and right-click. A web flies out and bursts on a creature or a solid block.";
            case SENSE_EVIL -> "Hold a spider eye and right-click. You turn toward a living biome boss, if one is out.";
            case SMITE_EVIL ->
                "Hold a golden sword and right-click. It strikes a creature along your aim. With at least 1 mana the crosshair turns gold on one in reach. It costs 1/4 mana.";
            case MASTER_BUILDER ->
                "Hold a clay brick and right-click. Right-click again to cancel. It costs 1 mana.";
            case DIAMOND_SKIN ->
                "Hold diamonds and right-click. Right-click again to cancel. It costs 2 mana.";
            case EMPOWER ->
                "Hold redstone dust and right-click in the air. Right-click in the air again to cancel. It costs 1 mana.";
            case BATTERY ->
                "Hold a quartz block and right-click in the air. Right-click again to cancel. It costs no mana. It renews until mana reaches your level and spends a quartz block.";
            case LAPIS_SHIELD ->
                "Hold lapis lazuli and right-click a creature, or yourself if none is targeted. It costs 1 mana. Right-click again to cancel.";
            case WRITE_TOME ->
                "Hold a book at experience level 7 or higher and right-click. It costs 1 mana. Two or more books add +2. Three, four, or five books make 2, 3, or 4 tomes and split the experience. More than five still uses five and makes 4.";
            case READ_TOME ->
                "Hold a tome that holds experience and right-click. It costs 1 mana and returns 80% of what was saved, plus 2% per ability level.";
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
                "magic arrow",
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
                "lapis shield",
                "write tome",
                "read tome");
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
            case "fire bolt", "firebolt", "magic arrow", "magicarrow" -> MAGIC_ARROW;
            case "flight" -> FLIGHT;
            case "grow", "let it grow", "letitgrow" -> GROW;
            case "power mine", "power mining", "powermine" -> POWER_MINE;
            case "detect ore", "detectore" -> DETECT_ORE;
            case "slow" -> SLOW;
            case "sense evil", "senseevil" -> SENSE_EVIL;
            case "smite", "smite evil", "smiteevil" -> SMITE_EVIL;
            case "master builder", "masterbuilder" -> MASTER_BUILDER;
            case "diamond skin", "diamondskin" -> DIAMOND_SKIN;
            case "empower" -> EMPOWER;
            case "battery" -> BATTERY;
            case "lapis shield", "lapisshield", "lapus shield", "lapusshield", "lapus" -> LAPIS_SHIELD;
            case "book", "write tome", "write tome of the wise", "writetome" -> WRITE_TOME;
            case "read tome", "read tome of the wise", "readtome", "tome" -> READ_TOME;
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
                    "Healing: Right-click an injured teammate within 2 + ability level blocks while holding paper.",
                    "Healing: If no teammate is targeted and you are injured, you heal yourself instead.",
                    "Healing: Costs 1 mana. A honey bottle in your main inventory (not a bundle) is consumed for +4, bottle included, with or without mana.",
                    "Healing: If you have no mana, a honey bottle still lets you heal, even with no mana levels.",
                    "Healing: The recipient sparkles blue for 6 seconds, and the healing is spread across that sparkle. One paper is consumed. Two in hand consume both and add +2.");
            case IRON_HEART -> List.of(
                    "Iron Heart: Right-click while holding an iron ingot. Costs 1 mana. Raises maximum health by ability level for ability level minutes.",
                    "Iron Heart: If another player is targeted within 2 + ability level blocks, they receive the buff instead.",
                    "Iron Heart: Right-click again with an iron ingot to cancel your own; you cannot cancel another player's Iron Heart.",
                    "Iron Heart: The recipient sparkles silver for 3 seconds. One ingot is consumed. Two in hand consume both and add +2.",
                    "Iron Heart: If it ends and you still have mana, another mana is spent and it continues, and you gain skill as if you cast it again.",
                    "Iron Heart: A renew spends an ingot only when you are holding one, and that one adds +2.");
            case MAGIC_ARROW -> List.of(
                    "Magic arrow: Right-click while holding an arrow. Costs 1 mana. The arrow seeks the nearest creature along your aim.",
                    "Magic arrow: Damage is 3 times your ability level. Range scales with ability level. Cooldown is 10 seconds minus skill.",
                    "Magic arrow: Every 2 ability levels, rounded down, launches another arrow at a different creature. That costs nothing more.",
                    "Magic arrow: Charcoal in your hotbar or inventory, not a bundle, adds +2. One charcoal is consumed, and the arrows burn.",
                    "Magic arrow: One arrow is consumed. Two in hand consume both and add +2.");
            case FLIGHT -> List.of(
                    "Flight: Right-click while holding a feather. Costs 1 mana. Jump to rise, sneak in the air to descend, and sneak on a block to stop.",
                    "Flight: Speed scales with ability level and reaches 70% of walking speed at 10.",
                    "Flight: One feather is consumed. Two in hand consume both and add +2.",
                    "Flight: If it ends and you still have mana, another mana is spent and it continues, and you gain skill as if you cast it again.",
                    "Flight: A renew spends a feather only when you are holding one, and that one adds +2.");
            case GROW -> List.of(
                    "Let it grow: Right-click a plant while holding any hoe to grow it by your ability level in stages.",
                    "Let it grow: Leftover stages go to the nearest plant within 2 blocks. Right-click also still grows random plants around you.",
                    "Let it grow: Costs 1 mana and 3 durability. Unbreaking can skip that wear with the same chance as tilling. Does not replace tilling.",
                    "Let it grow: Each plant grown improves that soil's shown crop-loss modifier by 3.",
                    "Let it grow: Random growth is one plant within 2 blocks by 1 stage, repeated (ability level + hoe bonus) times.",
                    "Let it grow: Hoe bonus: wood/stone 0, copper +1, iron +2, diamond +3, gold +4, netherite +5.");
            case POWER_MINE -> List.of(
                    "Power mining: Right-click while holding any pickaxe. Costs 1 mana.",
                    "Power mining: For (ability level × 12) seconds, Tougher extra pickaxe wear on hardened stone is suspended. Ends when the timer expires.",
                    "Power mining: Pickaxe bonus: wood/stone 0, copper +1, iron +2, diamond +3, gold +4, netherite +5.");
            case DETECT_ORE -> List.of(
                    "Detect ore: Right-click while holding a compass. The compass is kept. Costs 1 mana. Nether quartz is not required.",
                    "Detect ore: The largest nether quartz stack in your hotbar or inventory, not a bundle, adds the square root of its count, rounded. That many quartz are consumed from that stack.",
                    "Detect ore: Ores within 2 blocks of your height come first, then farther up or down, out to 4 + ability level minus the ore difficulty, through walls.",
                    "Detect ore: It tells you the ore and approximate distance. You only sense ores at or below your ability level.",
                    "Detect ore: coal 1, quartz 2, copper 3, iron 4, redstone 5, lapis 6, gold 7, emerald 8, diamond 9, ancient debris 10.");
            case SLOW -> List.of(
                    "Slow: Right-click while holding string. Costs 1 mana. Throws a web up to (ability level × 2 + 4) blocks.",
                    "Slow: It bursts on a creature or a solid block and slows creatures within (ability level) blocks of the landing.",
                    "Slow: The slow is 20% + 5% per ability level (max 80%) for (ability level × 6) seconds.",
                    "Slow: Above 100 health, both the slow and the duration are multiplied by 100 divided by that health.",
                    "Slow: It does not affect you, or any player closer to you than the hit. One string is consumed. Two in hand consume both and add +2.");
            case SENSE_EVIL -> List.of(
                    "Sense Evil: Right-click while holding a spider eye. Costs 1 mana.",
                    "Sense Evil: If a biome boss is spawned, you turn toward the nearest one and sparkle with a creepy aura for 6 seconds.",
                    "Sense Evil: If it is within 250 × ability level blocks, you also learn the rough distance.",
                    "Sense Evil: If none are spawned, you sparkle as with Healing. One spider eye is consumed. Two in hand consume both and add +2.");
            case SMITE_EVIL -> List.of(
                    "Smite Evil: Right-click while holding a golden sword. Costs 1/4 mana.",
                    "Smite Evil: Strikes a creature along your aim up to your ability level in blocks with a normal melee attack from that sword.",
                    "Smite Evil: With at least 1 mana, the crosshair turns gold on a creature within that reach.",
                    "Smite Evil: The strike includes enchantments, durability, and attack cooldown. Undead take extra damage equal to your ability level.",
                    "Smite Evil: A small flash of light appears on the creature hit.");
            case MASTER_BUILDER -> List.of(
                    "Master Builder: Right-click while holding a clay brick. Costs 1 mana. Lasts 30 seconds times your ability level.",
                    "Master Builder: While it lasts, you can place solid blocks in the air, and block reach increases by your ability level. Torch rules still apply.",
                    "Master Builder: Right-click again with a clay brick to cancel. If it ends and you still have mana, it spends another and continues.",
                    "Master Builder: One brick is consumed. Two in hand consume both and add +2. Continuing gains skill as if you cast it again.",
                    "Master Builder: A renew spends a brick only when you are holding one, and that one adds +2.");
            case DIAMOND_SKIN -> List.of(
                    "Diamond Skin: Right-click while holding diamonds. Costs 2 mana. Effective level is at least 2. Lasts 30 seconds times that level.",
                    "Diamond Skin: Damage that would reach your health is divided by your ability level. Armor still takes the hit first.",
                    "Diamond Skin: Each point of that damage it prevents shortens Diamond Skin by 3 seconds. Poison, drowning, and suffocation are not reduced.",
                    "Diamond Skin: A cyan sparkle shows for a quarter second when a hit is reduced.",
                    "Diamond Skin: Right-click again with a diamond to cancel. If it ends and you still have 2 mana, it spends that and continues.",
                    "Diamond Skin: One diamond is consumed. Two in hand consume both and add +2. Continuing gains skill as if you cast it again.",
                    "Diamond Skin: A renew spends a diamond only when you are holding one, and that one adds +2.");
            case EMPOWER -> List.of(
                    "Empower: Right-click in the air while holding redstone dust. Costs 1 mana. Lasts 30 seconds per ability level, and at least 30 seconds.",
                    "Empower: While it lasts, each later mana ability cast gains extra level equal to the square root of Empower's level.",
                    "Empower: Right-click in the air again with redstone dust to cancel.",
                    "Empower: One dust is consumed. Two in hand consume both and add +2.");
            case BATTERY -> List.of(
                    "Battery: Right-click in the air while holding a quartz block. Costs no mana. Lasts 2 minutes per ability level, and at least 2 minutes.",
                    "Battery: While it lasts, mana regeneration is multiplied by 1 plus the square root of your ability level.",
                    "Battery: Right-click in the air again with a quartz block to cancel. It ends when your mana reaches your mana level.",
                    "Battery: It continues only while mana is below your level and a quartz block is in hand. One block is consumed. Two in hand consume both and add +2.",
                    "Battery: Continuing gains skill as if you cast it again.");
            case LAPIS_SHIELD -> List.of(
                    "Lapis Shield: Right-click while holding lapis lazuli. A targeted character gets the shield. Otherwise it shields you. Costs 1 mana.",
                    "Lapis Shield: It lasts 1 minute times your ability level. A force field stays around that character until it ends.",
                    "Lapis Shield: Damage up to that level is absorbed before armor or health. Every 3 points blocked costs 1 lapis lazuli, rounded up.",
                    "Lapis Shield: That lapis comes from your hotbar or inventory, not a bundle, armor, or the offhand. If you cannot pay, the hit lands and the shield ends.",
                    "Lapis Shield: Right-click again with lapis lazuli to cancel. One is consumed to cast. Two in hand consume both and add +2.");
            case WRITE_TOME -> List.of(
                    "Write Tome of the Wise: Right-click while holding a book at experience level 7 or higher. Costs 1 mana.",
                    "Write Tome of the Wise: Above level 30 and at 30 or below, it saves 80% plus 2% per ability level. Below 10 some is lost, and above 10 some is gained.",
                    "Write Tome of the Wise: Above level 30 only the excess is saved, and you return to exactly level 30. At level 30 or below, all experience is saved and you return to level 0.",
                    "Write Tome of the Wise: The tome names you and the experience it holds. That amount is stored on the book.",
                    "Write Tome of the Wise: One book makes one tome. Two or more add +2. Three, four, or five books make 2, 3, or 4 tomes and split the saved experience.",
                    "Write Tome of the Wise: More than five books still uses five, adds +2, and makes 4 tomes. One of those books is the +2 bonus.");
            case READ_TOME -> List.of(
                    "Read Tome of the Wise: Right-click a tome that holds experience, at any level. Costs 1 mana and the tome.",
                    "Read Tome of the Wise: You gain 80% of the saved experience, plus 2% per ability level, above or below level 30. Sneak and right-click to open it instead.");
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
                "Base power: skill, the square root of times you have used that ability. Effective level is at least 1.",
                "A cast consumes one catalyst. Two in hand consume both for +2. One consumed catalyst adds nothing. Swords, hoes, and pickaxes are kept.",
                "A renew spends no catalyst unless you are holding it. Holding it spends one and adds +2.",
                "Gold armor adds 1 mana level after 90 seconds, then 180, 360, and 720 seconds more. Four pieces put on together reach +4 in 1350 seconds. A piece that comes off loses its bonus.",
                "A golden item held for 30 seconds, such as a golden hoe, adds 1 more mana level. Gold ingots, nuggets, and blocks do not.",
                "Using an ability you have not learned still gains skill, at −3 power (floored at 1), until you have a free slot to learn them.");
    }

    public static List<String> powerHelpKeys() {
        return List.of(
                POWER_HELP_KEY,
                "tougher.ability.power.help.2",
                "tougher.ability.power.help.3",
                "tougher.ability.power.help.4",
                "tougher.ability.power.help.5",
                "tougher.ability.power.help.6");
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
                "tougher.ability.index.unlock",
                "tougher.ability.index.unlock.2",
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
                "Arrow: Magic arrow",
                "Any hoe: Let it grow",
                "Any pickaxe: Power mining",
                "Compass: Detect ore",
                "String: Slow",
                "Spider eye: Sense Evil",
                "Golden sword: Smite Evil",
                "Clay brick: Master Builder",
                "Diamond: Diamond Skin",
                "Redstone dust: Empower",
                "Quartz block: Battery",
                "Lapis lazuli: Lapis Shield",
                "Book: Write Tome of the Wise",
                "Tome: Read Tome of the Wise",
                "You can learn up to half your mana level in abilities, rounded up.",
                "Unlearned abilities can be used and still gain skill, at −3 power (floored at 1), until you have a free slot to learn them.",
                "A wise teacher teaches one ability. A 24-point home charges 30 emeralds; each point above that takes 2 off, down to free.",
                "That lesson does not count toward abilities you learn on your own. Paying for one you already know frees that slot.",
                "That teacher will raise your mana once for a diamond block and a lapis lazuli block.",
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

    /** Distinct learned mana abilities (slot-cap set, not merely used). */
    public static int learnedSkillCount(Collection<String> learned) {
        return learnedSkillCount(learned, Set.of());
    }

    /** Learned abilities that still use a self-learned slot. Wise-teacher lessons are exempt. */
    public static int learnedSkillCount(Collection<String> learned, Collection<String> exempt) {
        if (learned == null || learned.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String ability : ABILITY_IDS) {
            if (learned.contains(ability) && (exempt == null || !exempt.contains(ability))) {
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
     * Extra ability level from held supplies: extra catalyst in hand, hoe/pickaxe
     * material, the largest hotbar or backpack nether quartz stack for Detect ore,
     * and charcoal in that same inventory for Magic Arrow.
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
        if (ability == null) {
            return 0;
        }
        int bonus = DETECT_ORE.equals(ability) ? detectOreQuartzBonus(quartzStack) : 0;
        if (MAGIC_ARROW.equals(ability)) {
            bonus += charcoalArrowBonus(charcoal);
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
        return canLearnAbility(manaLevel, learned, Set.of());
    }

    public static boolean canLearnAbility(int manaLevel, Collection<String> learned, Collection<String> exempt) {
        return learnedSkillCount(learned, exempt) < abilitySlotCap(manaLevel);
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

    /** Magic arrow uses a 10-second cooldown that shrinks with skill. Mana level does not change it or its power. */
    public static boolean usesShortCooldown(String ability) {
        return MAGIC_ARROW.equals(ability);
    }

    /** +2 when the hotbar or backpack holds charcoal. Bundles, armor, and the offhand do not count. */
    public static int charcoalArrowBonus(boolean hasCharcoal) {
        return hasCharcoal ? CHARCOAL_ARROW_BONUS : 0;
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
     * Effective ability level from skill and item bonuses, at least {@link #MIN_POWER}.
     * Mana level is not added. Pass {@code trained} false for an unlearned ability over the slot cap.
     */
    public static double power(String ability, int manaLevel, int uses, int itemBonus) {
        return power(ability, manaLevel, uses, itemBonus, true);
    }

    public static double power(String ability, int manaLevel, int uses, int itemBonus, boolean trained) {
        double p = power(manaLevel, uses, itemBonus);
        if (!trained) {
            p = untrainedPower(p);
        }
        return Math.max(MIN_POWER, p);
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
        return FLIGHT.equals(ability)
                || IRON_HEART.equals(ability)
                || MASTER_BUILDER.equals(ability)
                || DIAMOND_SKIN.equals(ability)
                || BATTERY.equals(ability);
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

    /** Diamond Skin never casts below {@link #DIAMOND_SKIN_MIN_LEVEL}. */
    public static double diamondSkinLevel(double power) {
        return Math.max(DIAMOND_SKIN_MIN_LEVEL, power);
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

    /** Hit damage for Magic arrow: ability level times 3, rounded. */
    public static int projectileDamage(double power) {
        return (int) Math.round(Math.max(0.0, power) * 3.0);
    }

    /** Seconds a charcoal-fed Magic arrow burns its target. This stays the rounded ability level. */
    public static int projectileBurnSeconds(double power) {
        return Math.max(0, (int) Math.round(Math.max(0.0, power)));
    }

    /** Extra shots besides the first: one per 2 ability levels, rounded down. */
    public static int extraProjectileCount(double power) {
        if (power <= 0.0) {
            return 0;
        }
        return (int) Math.floor(power / 2.0);
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
     * Cube reach for one ore: {@link #DETECT_ORE_BASE_RANGE} plus rounded ability
     * level, minus that ore's difficulty. Never negative.
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
     * Coal 1, quartz 2, copper 3, iron 4, redstone 5, lapis 6, gold 7, emerald 8,
     * diamond 9, ancient debris 10.
     */
    public static int oreDetectLevel(String blockId) {
        return switch (oreFamily(blockId)) {
            case "coal" -> 1;
            case "quartz" -> 2;
            case "copper" -> 3;
            case "iron" -> 4;
            case "redstone" -> 5;
            case "lapis" -> 6;
            case "gold" -> 7;
            case "emerald" -> 8;
            case "diamond" -> 9;
            case "ancient_debris" -> 10;
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
     * Hits the 1-second floor at combined rating 40. Magic arrow uses
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
