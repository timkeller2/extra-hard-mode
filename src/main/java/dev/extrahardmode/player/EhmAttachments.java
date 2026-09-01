package dev.extrahardmode.player;

import com.mojang.serialization.Codec;
import dev.extrahardmode.ExtraHardModeMod;
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
            Codec.unboundedMap(Codec.STRING, LONG_LINKED_SET_CODEC);

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

    public static final AttachmentType<Boolean> EHM_LOOTLESS = AttachmentRegistry.create(
            ExtraHardModeMod.id("lootless"),
    public static final AttachmentType<Integer> EHM_REANIMATE_COUNT = AttachmentRegistry.create(
            ExtraHardModeMod.id("reanimate_count"),
            builder -> builder.persistent(Codec.INT).initializer(() -> 0));
    private EhmAttachments() {}

    public static void register() {
        // Static fields register on class load.
    }
}
