package dev.extrahardmode.player;

import com.mojang.serialization.Codec;
import dev.extrahardmode.ExtraHardModeMod;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
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

    public static final AttachmentType<LongOpenHashSet> EHM_VISITED_SECTIONS = AttachmentRegistry.create(
            ExtraHardModeMod.id("visited_sections"),
            builder -> builder.persistent(LONG_SET_CODEC).copyOnDeath().initializer(LongOpenHashSet::new));

    public static final AttachmentType<Boolean> EHM_SPAWN_PROCESSED = AttachmentRegistry.create(
            ExtraHardModeMod.id("spawn_processed"),
            builder -> builder.persistent(Codec.BOOL).initializer(() -> Boolean.FALSE));

    public static final AttachmentType<Boolean> EHM_OURS = AttachmentRegistry.create(
            ExtraHardModeMod.id("ours"),
            builder -> builder.persistent(Codec.BOOL).initializer(() -> Boolean.FALSE));

    public static final AttachmentType<Vec3> EHM_FLY_ORIGIN = AttachmentRegistry.create(
            ExtraHardModeMod.id("fly_origin"), builder -> builder.persistent(Vec3.CODEC));

    private EhmAttachments() {}

    public static void register() {
        // Static fields register on class load.
    }
}
