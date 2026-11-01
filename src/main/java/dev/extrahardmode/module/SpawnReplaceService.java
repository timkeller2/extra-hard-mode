package dev.extrahardmode.module;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.WorldGate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Spawn-time replacement registry. NATURAL only; never {@code ENTITY_LOAD} or
 * {@code CHUNK_GENERATION}. The processed stamp is written before any roll.
 */
public final class SpawnReplaceService {
    public static final TagKey<Biome> NO_SPAWN_REPLACEMENTS =
            TagKey.create(Registries.BIOME, ExtraHardModeMod.id("no_spawn_replacements"));
    public static final TagKey<Structure> NO_SPAWN_REPLACEMENT_STRUCTURES =
            TagKey.create(Registries.STRUCTURE, ExtraHardModeMod.id("no_spawn_replacement_structures"));

    private static final Map<EntityType<?>, List<ReplaceFn>> REPLACEMENTS = new ConcurrentHashMap<>();
    private static final List<ReplaceFn> ANY_REPLACEMENTS = new CopyOnWriteArrayList<>();
    private static final AtomicBoolean MIXIN_APPLIED = new AtomicBoolean();
    private static final AtomicBoolean MIXIN_MISSING_WARNED = new AtomicBoolean();

    @FunctionalInterface
    public interface ReplaceFn {
        /**
         * @return replacement type, or {@code null} to keep {@code original}
         */
        EntityType<?> roll(Mob original, ServerLevel level);

        /** The replacement is in the world. The original is discarded after this returns. */
        default void afterSpawn(Mob original, Entity spawned) {}

        /** {@link EntityType#spawn} returned null, so the original mob stays. */
        default void cancelSpawn() {}
    }

    private SpawnReplaceService() {}

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(
                server -> server.execute(SpawnReplaceService::warnIfMixinMissing));
    }

    public static void markMixinApplied() {
        MIXIN_APPLIED.set(true);
    }

    public static void warnIfMixinMissing() {
        if (MIXIN_APPLIED.get()) {
            return;
        }
        if (MIXIN_MISSING_WARNED.compareAndSet(false, true)) {
            ExtraHardModeMod.LOGGER.warn(
                    "EHM NaturalSpawner spawn inject did not apply; spawn replacements disabled. Lithium or another mixin may have replaced spawnCategoryForPosition.");
        }
    }

    /** Checked for every natural spawn, before the per-type rolls. The first hit wins. */
    public static void registerAny(ReplaceFn fn) {
        Objects.requireNonNull(fn, "fn");
        ANY_REPLACEMENTS.add(fn);
    }

    /** First non-null type replacement wins. Register order is module bootstrap order. */
    public static void register(EntityType<?> from, ReplaceFn fn) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(fn, "fn");
        REPLACEMENTS.computeIfAbsent(from, type -> new CopyOnWriteArrayList<>()).add(fn);
    }

    /** Deep Dark biome tag and ancient city / trial chamber structure tags. */
    public static boolean locationExcluded(ServerLevel level, BlockPos pos) {
        if (level.getBiome(pos).is(NO_SPAWN_REPLACEMENTS)) {
            return true;
        }
        return level.structureManager().getStructureWithPieceAt(pos, NO_SPAWN_REPLACEMENT_STRUCTURES).isValid();
    }

    public static boolean replaceIfNeeded(Mob mob, ServerLevel level, EntitySpawnReason reason) {
        if (!WorldGate.isActive(level)) {
            return false;
        }
        if (Boolean.TRUE.equals(mob.getAttachedOrElse(EhmAttachments.EHM_SPAWN_PROCESSED, Boolean.FALSE))) {
            return false;
        }
        mob.setAttached(EhmAttachments.EHM_SPAWN_PROCESSED, true);
        if (reason != EntitySpawnReason.NATURAL) {
            return false;
        }
        BlockPos pos = mob.blockPosition();
        if (locationExcluded(level, pos)) {
            return false;
        }
        if (replaceFirst(mob, level, ANY_REPLACEMENTS)) {
            return true;
        }
        List<ReplaceFn> fns = REPLACEMENTS.get(mob.getType());
        if (fns == null || fns.isEmpty()) {
            return false;
        }
        return replaceFirst(mob, level, fns);
    }

    private static boolean replaceFirst(Mob mob, ServerLevel level, List<ReplaceFn> fns) {
        for (ReplaceFn fn : fns) {
            EntityType<?> replacement = fn.roll(mob, level);
            if (replacement != null && replacement != mob.getType()) {
                if (spawnReplacement(mob, level, replacement, fn)) {
                    return true;
                }
                fn.cancelSpawn();
            }
        }
        return false;
    }

    private static boolean spawnReplacement(Mob original, ServerLevel level, EntityType<?> type, ReplaceFn fn) {
        Entity spawned = type.spawn(level, original.blockPosition(), EntitySpawnReason.EVENT);
        if (spawned == null) {
            return false;
        }
        spawned.snapTo(original.getX(), original.getY(), original.getZ(), original.getYRot(), original.getXRot());
        if (spawned instanceof Mob replacement) {
            replacement.setAttached(EhmAttachments.EHM_SPAWN_PROCESSED, true);
        }
        try {
            fn.afterSpawn(original, spawned);
        } finally {
            if (ConfigManager.global().debug()) {
                ExtraHardModeMod.LOGGER.debug(
                        "EHM spawn replace {} -> {} at {}", original.getType(), type, original.blockPosition());
            }
            original.discard();
        }
        return true;
    }
}
