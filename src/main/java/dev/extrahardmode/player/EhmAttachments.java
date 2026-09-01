package dev.extrahardmode.player;

import com.mojang.serialization.Codec;
import dev.extrahardmode.ExtraHardModeMod;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.UUIDUtil;

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

    public static final AttachmentType<LongOpenHashSet> EHM_VISITED_SECTIONS = AttachmentRegistry.create(
            ExtraHardModeMod.id("visited_sections"),
            builder -> builder.persistent(LONG_SET_CODEC).copyOnDeath().initializer(LongOpenHashSet::new));

    /** Persistent; stamped before a spawn-replacement roll so chunk reload cannot re-roll. */
    public static final AttachmentType<Boolean> EHM_SPAWN_PROCESSED = AttachmentRegistry.create(
            ExtraHardModeMod.id("spawn_processed"),
    public static final AttachmentType<Boolean> EHM_OURS = AttachmentRegistry.create(
            ExtraHardModeMod.id("ours"),
            builder -> builder.persistent(Codec.BOOL).initializer(() -> Boolean.FALSE));

    public static final AttachmentType<UUID> EHM_SILVERFISH_OWNER = AttachmentRegistry.create(
            ExtraHardModeMod.id("silverfish_owner"), builder -> builder.persistent(UUIDUtil.CODEC));

    public static final AttachmentType<Integer> EHM_SILVERFISH_SPAWNED = AttachmentRegistry.create(
            ExtraHardModeMod.id("silverfish_spawned"),
            builder -> builder.persistent(Codec.INT).initializer(() -> 0));

    public static final AttachmentType<Boolean> EHM_LOOTLESS = AttachmentRegistry.create(
            ExtraHardModeMod.id("lootless"),
            builder -> builder.persistent(Codec.BOOL).initializer(() -> Boolean.FALSE));

    /** Transient shot tag: snowball / firework / fireball. */
    public static final AttachmentType<String> EHM_SKELETON_SPECIAL =
            AttachmentRegistry.create(ExtraHardModeMod.id("skeleton_special"));

    /** Transient; caches the one deflect roll for this arrow so mixin + ALLOW_DAMAGE cannot double-roll. */
    public static final AttachmentType<Boolean> EHM_ARROW_DEFLECT =
            AttachmentRegistry.create(ExtraHardModeMod.id("arrow_deflect"));

    private EhmAttachments() {}

    public static void register() {
        // Static fields register on class load.
    }
}
