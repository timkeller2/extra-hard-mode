package dev.extrahardmode.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.feature.CouncilMissionRules;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.phys.Vec3;

public final class EhmAttachments {
    public static final Codec<LongOpenHashSet> LONG_SET_CODEC = Codec.LONG.listOf().xmap(list -> {
        LongOpenHashSet set = new LongOpenHashSet(list.size());
        for (Long value : list) {
            set.add(value.longValue());
        }
        return set;
    }, set -> {
        List<Long> list = new ArrayList<>(set.size());
        set.forEach((long value) -> list.add(value));
        return list;
    });

    public static final AttachmentType<Map<String, Integer>> EHM_TUTORIAL = AttachmentRegistry.create(
            ExtraHardModeMod.id("tutorial"),
            builder -> builder.persistent(Codec.unboundedMap(Codec.STRING, Codec.INT))
                    .copyOnDeath()
                    .initializer(HashMap::new));

    public static final AttachmentType<Boolean> EHM_BYPASS = AttachmentRegistry.create(
            ExtraHardModeMod.id("bypass"),
            builder -> builder.persistent(Codec.BOOL).copyOnDeath().initializer(() -> Boolean.FALSE));

    public static final AttachmentType<Double> EHM_WEIGHT_CACHE =
            AttachmentRegistry.create(ExtraHardModeMod.id("weight_cache"), builder -> builder.initializer(() -> 0.0));

    public static final Codec<LongLinkedOpenHashSet> LONG_LINKED_SET_CODEC = Codec.LONG.listOf().xmap(list -> {
        LongLinkedOpenHashSet set = new LongLinkedOpenHashSet(list.size());
        for (Long value : list) {
            set.add(value.longValue());
        }
        return set;
    }, set -> {
        List<Long> list = new ArrayList<>(set.size());
        set.forEach((long value) -> list.add(value));
        return list;
    });

    public static final Codec<Map<String, LongLinkedOpenHashSet>> VISITED_BY_DIMENSION_CODEC =
            Codec.unboundedMap(Codec.STRING, LONG_LINKED_SET_CODEC).xmap(HashMap::new, map -> map);

    /** Per-dimension FIFO of visited section keys. */
    public static final AttachmentType<Map<String, LongLinkedOpenHashSet>> EHM_VISITED_SECTIONS =
            AttachmentRegistry.create(
                    ExtraHardModeMod.id("visited_sections"),
                    builder -> builder.persistent(VISITED_BY_DIMENSION_CODEC)
                            .copyOnDeath()
                            .initializer(HashMap::new));

    /** Persistent; stamped before a spawn-replacement roll so chunk reload cannot re-roll. */
    public static final AttachmentType<Boolean> EHM_SPAWN_PROCESSED = AttachmentRegistry.create(
            ExtraHardModeMod.id("spawn_processed"),
            builder -> builder.persistent(Codec.BOOL).initializer(() -> Boolean.FALSE));

    public static final AttachmentType<Boolean> EHM_OURS = AttachmentRegistry.create(
            ExtraHardModeMod.id("ours"),
            builder -> builder.persistent(Codec.BOOL).initializer(() -> Boolean.FALSE));
    public static final AttachmentType<Boolean> EHM_INHABITANT = AttachmentRegistry.create(
            ExtraHardModeMod.id("inhabitant"),
            builder -> builder.persistent(Codec.BOOL).initializer(() -> Boolean.FALSE));
    public static final AttachmentType<String> EHM_INHABITANT_HOME = AttachmentRegistry.create(
            ExtraHardModeMod.id("inhabitant_home"),
            builder -> builder.persistent(Codec.STRING).initializer(() -> ""));

    public static final AttachmentType<UUID> EHM_SILVERFISH_OWNER = AttachmentRegistry.create(
            ExtraHardModeMod.id("silverfish_owner"), builder -> builder.persistent(UUIDUtil.CODEC));

    public static final AttachmentType<Integer> EHM_SILVERFISH_SPAWNED = AttachmentRegistry.create(
            ExtraHardModeMod.id("silverfish_spawned"),
            builder -> builder.persistent(Codec.INT).initializer(() -> 0));

    /** Persistent; EHM-spawned mobs that should not drop loot (witch baby zombies, etc.). */
    public static final AttachmentType<DamageTracker> EHM_DAMAGE_TRACKER = AttachmentRegistry.create(
            ExtraHardModeMod.id("damage_tracker"),
            builder -> builder.persistent(DamageTracker.CODEC).initializer(() -> DamageTracker.EMPTY));
    public static final AttachmentType<Boolean> EHM_UNNATURAL_SPAWN = AttachmentRegistry.create(
            ExtraHardModeMod.id("unnatural_spawn"),
            builder -> builder.persistent(Codec.BOOL).initializer(() -> Boolean.FALSE));
    public static final AttachmentType<Boolean> EHM_LOOTLESS = AttachmentRegistry.create(
            ExtraHardModeMod.id("lootless"),
            builder -> builder.persistent(Codec.BOOL).initializer(() -> Boolean.FALSE));

    /** Transient shot tag: snowball / firework / fireball. */
    public static final AttachmentType<String> EHM_SKELETON_SPECIAL =
            AttachmentRegistry.create(ExtraHardModeMod.id("skeleton_special"));

    /** Transient; caches the one deflect roll for this arrow so mixin + ALLOW_DAMAGE cannot double-roll. */
    public static final AttachmentType<Boolean> EHM_ARROW_DEFLECT =
            AttachmentRegistry.create(ExtraHardModeMod.id("arrow_deflect"));
    /** Transient game-time of the last enderman-forced player teleport. */
    public static final AttachmentType<Long> EHM_ENDERMAN_TP_TICK = AttachmentRegistry.create(
            ExtraHardModeMod.id("enderman_tp_tick"), builder -> builder.initializer(() -> 0L));
    public static final AttachmentType<Boolean> EHM_TRIAL_SPAWNED = AttachmentRegistry.create(
            ExtraHardModeMod.id("trial_spawned"),
            builder -> builder.persistent(Codec.BOOL).initializer(() -> Boolean.FALSE));
    public static final AttachmentType<Vec3> EHM_FLY_ORIGIN = AttachmentRegistry.create(
            ExtraHardModeMod.id("fly_origin"), builder -> builder.persistent(Vec3.CODEC));

    public static final AttachmentType<Boolean> EHM_IGNORE = AttachmentRegistry.create(
            ExtraHardModeMod.id("ignore"),
            builder -> builder.persistent(Codec.BOOL).initializer(() -> Boolean.FALSE));

    public static final AttachmentType<Integer> EHM_REANIMATE_COUNT = AttachmentRegistry.create(
            ExtraHardModeMod.id("reanimate_count"),
            builder -> builder.persistent(Codec.INT).initializer(() -> 0));

    public static final AttachmentType<Integer> EHM_BLAZE_SPLIT =
            AttachmentRegistry.create(ExtraHardModeMod.id("blaze_split"), builder -> builder.initializer(() -> 0));

    /** Persistent FIFO of recently eaten food item ids for the variety bonus. */
    public static final AttachmentType<List<String>> EHM_FOOD_HISTORY = AttachmentRegistry.create(
            ExtraHardModeMod.id("food_history"),
            builder -> builder.persistent(Codec.STRING.listOf()).copyOnDeath().initializer(List::of));

    /** Persistent; biome-family bosses skip grinders and crowding, and drop special loot. */
    public static final AttachmentType<Boolean> EHM_BIOME_BOSS = AttachmentRegistry.create(
            ExtraHardModeMod.id("biome_boss"),
            builder -> builder.persistent(Codec.BOOL).initializer(() -> Boolean.FALSE));
    public static final AttachmentType<String> EHM_BOSS_FAMILY = AttachmentRegistry.create(
            ExtraHardModeMod.id("boss_family"), builder -> builder.persistent(Codec.STRING));
    /** Difficulty % from spawn distance at stamp time; loot uses this so dragging the boss does not shrink treasure. */
    public static final AttachmentType<Double> EHM_BOSS_DIFFICULTY_PERCENT = AttachmentRegistry.create(
            ExtraHardModeMod.id("boss_difficulty_percent"),
            builder -> builder.persistent(Codec.DOUBLE).initializer(() -> 0.0));
    /** Campaign bosses already defeated when this boss spawned; loot and stats use this snapshot. */
    public static final AttachmentType<Integer> EHM_BOSS_DEFEAT_COUNT = AttachmentRegistry.create(
            ExtraHardModeMod.id("boss_defeat_count"),
            builder -> builder.persistent(Codec.INT).initializer(() -> 0));

    public static final AttachmentType<Map<String, Integer>> EHM_BLOCK_PLACE_COUNTS = AttachmentRegistry.create(
            ExtraHardModeMod.id("block_place_counts"),
            builder -> builder.persistent(Codec.unboundedMap(Codec.STRING, Codec.INT))
                    .copyOnDeath()
                    .initializer(HashMap::new));
    /** Last builder-tracked block this player placed; shown on achieve/clock reports. */
    public static final AttachmentType<String> EHM_LAST_PLACED_BLOCK = AttachmentRegistry.create(
            ExtraHardModeMod.id("last_placed_block"),
            builder -> builder.persistent(Codec.STRING).copyOnDeath().initializer(() -> ""));
    public static final AttachmentType<Map<String, Integer>> EHM_BLOCK_AWARDED = AttachmentRegistry.create(
            ExtraHardModeMod.id("block_awarded"),
            builder -> builder.persistent(Codec.unboundedMap(Codec.STRING, Codec.INT))
                    .copyOnDeath()
                    .initializer(HashMap::new));
    public static final AttachmentType<Map<String, Integer>> EHM_KILL_COUNTS = AttachmentRegistry.create(
            ExtraHardModeMod.id("kill_counts"),
            builder -> builder.persistent(Codec.unboundedMap(Codec.STRING, Codec.INT))
                    .copyOnDeath()
                    .initializer(HashMap::new));
    public static final AttachmentType<Map<String, Integer>> EHM_KILL_AWARDED = AttachmentRegistry.create(
            ExtraHardModeMod.id("kill_awarded"),
            builder -> builder.persistent(Codec.unboundedMap(Codec.STRING, Codec.INT))
                    .copyOnDeath()
                    .initializer(HashMap::new));
    public static final AttachmentType<Integer> EHM_MANA_LEVEL = AttachmentRegistry.create(
            ExtraHardModeMod.id("mana_level"),
            builder -> builder.persistent(Codec.INT).copyOnDeath().initializer(() -> 0));
    /** Successful diamond+lapis mana payments; next lapis cost is 1 plus this. */
    public static final AttachmentType<Integer> EHM_MANA_LAPIS_PAYMENTS = AttachmentRegistry.create(
            ExtraHardModeMod.id("mana_lapis_payments"),
            builder -> builder.persistent(Codec.INT).copyOnDeath().initializer(() -> 0));
    public static final AttachmentType<Double> EHM_MANA_CURRENT = AttachmentRegistry.create(
            ExtraHardModeMod.id("mana_current"),
            builder -> builder.persistent(Codec.DOUBLE).copyOnDeath().initializer(() -> 0.0));
    /** Quartz-boosted mana regained toward the next quartz consumed. */
    public static final AttachmentType<Double> EHM_QUARTZ_MANA_CREDIT = AttachmentRegistry.create(
            ExtraHardModeMod.id("quartz_mana_credit"),
            builder -> builder.persistent(Codec.DOUBLE).copyOnDeath().initializer(() -> 0.0));
    public static final AttachmentType<Map<String, Integer>> EHM_ABILITY_USES = AttachmentRegistry.create(
            ExtraHardModeMod.id("ability_uses"),
            builder -> builder.persistent(Codec.unboundedMap(Codec.STRING, Codec.INT))
                    .copyOnDeath()
                    .initializer(HashMap::new));
    public static final AttachmentType<List<String>> EHM_ABILITY_LEARNED = AttachmentRegistry.create(
            ExtraHardModeMod.id("ability_learned"),
            builder -> builder.persistent(Codec.STRING.listOf()).copyOnDeath().initializer(ArrayList::new));
    public static final AttachmentType<Boolean> EHM_ABILITY_LEARNED_MIGRATED = AttachmentRegistry.create(
            ExtraHardModeMod.id("ability_learned_migrated"),
            builder -> builder.persistent(Codec.BOOL).copyOnDeath().initializer(() -> Boolean.FALSE));
    public static final AttachmentType<Map<String, Long>> EHM_ABILITY_LAST_USE = AttachmentRegistry.create(
            ExtraHardModeMod.id("ability_last_use"),
            builder -> builder.persistent(Codec.unboundedMap(Codec.STRING, Codec.LONG))
                    .copyOnDeath()
                    .initializer(HashMap::new));
    public static final AttachmentType<Integer> EHM_FIREBOLT_POWER = AttachmentRegistry.create(
            ExtraHardModeMod.id("firebolt_power"), builder -> builder.persistent(Codec.INT).initializer(() -> 0));
    public static final AttachmentType<UUID> EHM_FIREBOLT_TARGET = AttachmentRegistry.create(
            ExtraHardModeMod.id("firebolt_target"));
    public static final AttachmentType<Long> EHM_ABILITY_HANDLED_TICK = AttachmentRegistry.create(
            ExtraHardModeMod.id("ability_handled_tick"), builder -> builder.initializer(() -> -1L));
    public static final AttachmentType<Boolean> EHM_FLIGHT_ACTIVE = AttachmentRegistry.create(
            ExtraHardModeMod.id("flight_active"),
            builder -> builder.persistent(Codec.BOOL).initializer(() -> Boolean.FALSE));
    public static final AttachmentType<Integer> EHM_FLIGHT_REMAINING = AttachmentRegistry.create(
            ExtraHardModeMod.id("flight_remaining"),
            builder -> builder.persistent(Codec.INT).initializer(() -> 0));
    public static final AttachmentType<Integer> EHM_FLIGHT_BAR_MAX = AttachmentRegistry.create(
            ExtraHardModeMod.id("flight_bar_max"),
            builder -> builder.persistent(Codec.INT).initializer(() -> 0));
    public static final AttachmentType<Float> EHM_FLIGHT_SAVED_SPEED = AttachmentRegistry.create(
            ExtraHardModeMod.id("flight_saved_speed"), builder -> builder.initializer(() -> 0.05F));
    public static final AttachmentType<Float> EHM_FLIGHT_SPEED = AttachmentRegistry.create(
            ExtraHardModeMod.id("flight_speed"),
            builder -> builder.persistent(Codec.FLOAT).initializer(() -> 0.05F));
    public static final AttachmentType<Boolean> EHM_FLIGHT_LEFT_GROUND = AttachmentRegistry.create(
            ExtraHardModeMod.id("flight_left_ground"), builder -> builder.initializer(() -> Boolean.FALSE));
    public static final AttachmentType<Integer> EHM_POWER_MINE_REMAINING = AttachmentRegistry.create(
            ExtraHardModeMod.id("power_mine_remaining"),
            builder -> builder.persistent(Codec.INT).initializer(() -> 0));
    public static final AttachmentType<Integer> EHM_POWER_MINE_BAR_MAX = AttachmentRegistry.create(
            ExtraHardModeMod.id("power_mine_bar_max"),
            builder -> builder.persistent(Codec.INT).initializer(() -> 0));
    public static final AttachmentType<String> EHM_POWER_MINE_ITEM = AttachmentRegistry.create(
            ExtraHardModeMod.id("power_mine_item"),
            builder -> builder.persistent(Codec.STRING).initializer(() -> ""));
    public static final AttachmentType<Long> EHM_SLOW_UNTIL = AttachmentRegistry.create(
            ExtraHardModeMod.id("slow_until"), builder -> builder.initializer(() -> -1L));
    public static final AttachmentType<Long> EHM_HEAL_SPARKLE_UNTIL = AttachmentRegistry.create(
            ExtraHardModeMod.id("heal_sparkle_until"), builder -> builder.initializer(() -> -1L));
    public static final AttachmentType<Integer> EHM_IRON_HEART_LOCK = AttachmentRegistry.create(
            ExtraHardModeMod.id("iron_heart_lock"),
            builder -> builder.persistent(Codec.INT).initializer(() -> 0));
    public static final AttachmentType<Integer> EHM_IRON_HEART_REMAINING = AttachmentRegistry.create(
            ExtraHardModeMod.id("iron_heart_remaining"),
            builder -> builder.persistent(Codec.INT).initializer(() -> 0));
    public static final AttachmentType<Float> EHM_IRON_HEART_AMOUNT = AttachmentRegistry.create(
            ExtraHardModeMod.id("iron_heart_amount"),
            builder -> builder.persistent(Codec.FLOAT).initializer(() -> 0.0F));
    public static final AttachmentType<Integer> EHM_IRON_HEART_BAR_MAX = AttachmentRegistry.create(
            ExtraHardModeMod.id("iron_heart_bar_max"),
            builder -> builder.persistent(Codec.INT).initializer(() -> 0));
    public static final AttachmentType<UUID> EHM_IRON_HEART_CASTER = AttachmentRegistry.create(
            ExtraHardModeMod.id("iron_heart_caster"), builder -> builder.persistent(UUIDUtil.CODEC));
    public static final AttachmentType<UUID> EHM_IRON_HEART_TARGET = AttachmentRegistry.create(
            ExtraHardModeMod.id("iron_heart_target"), builder -> builder.persistent(UUIDUtil.CODEC));
    public static final AttachmentType<Long> EHM_IRON_HEART_SPARKLE_UNTIL = AttachmentRegistry.create(
            ExtraHardModeMod.id("iron_heart_sparkle_until"), builder -> builder.initializer(() -> -1L));
    public static final AttachmentType<Long> EHM_SENSE_SPARKLE_UNTIL = AttachmentRegistry.create(
            ExtraHardModeMod.id("sense_sparkle_until"), builder -> builder.initializer(() -> -1L));
    public static final AttachmentType<Integer> EHM_LIGHT_REMAINING = AttachmentRegistry.create(
            ExtraHardModeMod.id("light_remaining"),
            builder -> builder.persistent(Codec.INT).initializer(() -> 0));
    public static final AttachmentType<Integer> EHM_LIGHT_BAR_MAX = AttachmentRegistry.create(
            ExtraHardModeMod.id("light_bar_max"),
            builder -> builder.persistent(Codec.INT).initializer(() -> 0));
    public static final AttachmentType<Long> EHM_LIGHT_POS = AttachmentRegistry.create(
            ExtraHardModeMod.id("light_pos"), builder -> builder.initializer(() -> Long.MIN_VALUE));
    public static final AttachmentType<String> EHM_LIGHT_DIM = AttachmentRegistry.create(
            ExtraHardModeMod.id("light_dim"), builder -> builder.initializer(() -> ""));

    /** Remaining ticks for a floating soil-modifier number. Unset when not a pop. */
    public static final AttachmentType<Integer> EHM_FLOAT_TEXT = AttachmentRegistry.create(
            ExtraHardModeMod.id("float_text"), builder -> builder.initializer(() -> 0));
    /** Last hoe-look soil HUD key, empty when hidden. */
    public static final AttachmentType<String> EHM_SOIL_LOOK = AttachmentRegistry.create(
            ExtraHardModeMod.id("soil_look"), builder -> builder.initializer(() -> ""));

    /** Cooking XP millipoints (1000 = 1 XP) stored on hoppers/chests that received furnace output. */
    public static final AttachmentType<Integer> EHM_COOKING_XP_MILLI = AttachmentRegistry.create(
            ExtraHardModeMod.id("cooking_xp_milli"),
            builder -> builder.persistent(Codec.INT).initializer(() -> 0));

    /** Game time of this animal's next overgrazing check. 0 means unscheduled. */
    public static final AttachmentType<Long> EHM_OVERGRAZE_AT = AttachmentRegistry.create(
            ExtraHardModeMod.id("overgraze_at"),
            builder -> builder.persistent(Codec.LONG).initializer(() -> 0L));
    /** Packed chest positions this animal can path to. */
    public static final AttachmentType<LongOpenHashSet> EHM_OVERGRAZE_CHESTS = AttachmentRegistry.create(
            ExtraHardModeMod.id("overgraze_chests"),
            builder -> builder.persistent(LONG_SET_CODEC).initializer(LongOpenHashSet::new));

    /** Biome ids this player has already been rewarded for visiting. */
    public static final AttachmentType<List<String>> EHM_VISITED_BIOMES = AttachmentRegistry.create(
            ExtraHardModeMod.id("visited_biomes"),
            builder -> builder.persistent(Codec.STRING.listOf()).copyOnDeath().initializer(ArrayList::new));
    /** True after the first 300-block trip from world spawn. */
    public static final AttachmentType<Boolean> EHM_LEFT_SPAWN = AttachmentRegistry.create(
            ExtraHardModeMod.id("left_spawn"),
            builder -> builder.persistent(Codec.BOOL).copyOnDeath().initializer(() -> Boolean.FALSE));
    /** Last packed block position checked for exploration awards. */
    public static final AttachmentType<Long> EHM_EXPLORATION_LAST_POS = AttachmentRegistry.create(
            ExtraHardModeMod.id("exploration_last_pos"), builder -> builder.initializer(() -> Long.MIN_VALUE));

    public static final Codec<CouncilMissionRules.Mission> COUNCIL_MISSION_CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                            Codec.STRING.optionalFieldOf("mob_id", "").forGetter(CouncilMissionRules.Mission::mobId),
                            Codec.STRING.optionalFieldOf("label", "").forGetter(CouncilMissionRules.Mission::label),
                            Codec.INT.optionalFieldOf("hp", 0).forGetter(CouncilMissionRules.Mission::hp),
                            Codec.INT.optionalFieldOf("target", 0).forGetter(CouncilMissionRules.Mission::target),
                            Codec.INT.optionalFieldOf("kills", 0).forGetter(CouncilMissionRules.Mission::kills),
                            Codec.LONG
                                    .optionalFieldOf("assigned_day", -1L)
                                    .forGetter(CouncilMissionRules.Mission::assignedDay),
                            Codec.INT
                                    .optionalFieldOf("house_score", 0)
                                    .forGetter(CouncilMissionRules.Mission::houseScore),
                            Codec.BOOL
                                    .optionalFieldOf("completed", false)
                                    .forGetter(CouncilMissionRules.Mission::completed),
                            Codec.INT
                                    .optionalFieldOf("awarded_xp", 0)
                                    .forGetter(CouncilMissionRules.Mission::awardedXp),
                            Codec.INT
                                    .optionalFieldOf("pending_emeralds", 0)
                                    .forGetter(CouncilMissionRules.Mission::pendingEmeralds),
                            Codec.INT
                                    .optionalFieldOf("last_completed_target", 0)
                                    .forGetter(CouncilMissionRules.Mission::lastCompletedTarget),
                            Codec.INT
                                    .optionalFieldOf("completed_count", 0)
                                    .forGetter(CouncilMissionRules.Mission::completedCount))
                    .apply(instance, CouncilMissionRules.Mission::new));

    /** Per-player council kill bounty and lifetime hunt progress. */
    public static final AttachmentType<CouncilMissionRules.Mission> EHM_COUNCIL_MISSION = AttachmentRegistry.create(
            ExtraHardModeMod.id("council_mission"),
            builder -> builder.persistent(COUNCIL_MISSION_CODEC)
                    .copyOnDeath()
                    .initializer(() -> CouncilMissionRules.NONE));

    private EhmAttachments() {}

    public static void register() {
        // Static fields register on class load.
    }
}
