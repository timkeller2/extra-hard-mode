package dev.extrahardmode.module;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.WorldGate;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
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

    private static final Map<EntityType<?>, ReplaceFn> REPLACEMENTS = new ConcurrentHashMap<>();
    private static final AtomicBoolean MIXIN_APPLIED = new AtomicBoolean();
    private static final AtomicBoolean MIXIN_MISSING_WARNED = new AtomicBoolean();

    @FunctionalInterface
    public interface ReplaceFn {
        /**
         * @return replacement type, or {@code null} to keep {@code original}
         */
        EntityType<?> roll(Mob original, ServerLevel level);
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

    public static void register(EntityType<?> from, ReplaceFn fn) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(fn, "fn");
        REPLACEMENTS.put(from, fn);
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
        ReplaceFn fn = REPLACEMENTS.get(mob.getType());
        if (fn == null) {
            return false;
        }
        EntityType<?> replacement = fn.roll(mob, level);
        if (replacement == null || replacement == mob.getType()) {
            return false;
        }
        return spawnReplacement(mob, level, replacement);
    }

    private static boolean spawnReplacement(Mob original, ServerLevel level, EntityType<?> type) {
        Entity spawned = type.spawn(level, original.blockPosition(), EntitySpawnReason.EVENT);
        if (spawned == null) {
            return false;
        }
        spawned.snapTo(original.getX(), original.getY(), original.getZ(), original.getYRot(), original.getXRot());
        if (spawned instanceof Mob replacement) {
            replacement.setAttached(EhmAttachments.EHM_SPAWN_PROCESSED, true);
        }
        if (ConfigManager.global().debug()) {
            ExtraHardModeMod.LOGGER.debug(
                    "EHM spawn replace {} -> {} at {}", original.getType(), type, original.blockPosition());
        }
        original.discard();
        return true;
    }
}
