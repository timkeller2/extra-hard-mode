package dev.extrahardmode.module;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.WorldGate;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
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

    @FunctionalInterface
    public interface ReplaceFn {
        /**
         * @return replacement type, or {@code null} to keep {@code original}
         */
        EntityType<?> roll(Mob original, ServerLevel level);
    }

    private SpawnReplaceService() {}

    public static void init() {
        // Fail-soft when Lithium removes the NaturalSpawner invoke. NATURAL only; never disk load.
        ServerEntityEvents.ALLOW_LOAD.register(SpawnReplaceService::onAllowLoad);
    }

    public static void register(EntityType<?> from, ReplaceFn fn) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(fn, "fn");
        REPLACEMENTS.put(from, fn);
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
        if (level.getBiome(pos).is(NO_SPAWN_REPLACEMENTS)) {
            return false;
        }
        if (level.structureManager().getStructureWithPieceAt(pos, NO_SPAWN_REPLACEMENT_STRUCTURES).isValid()) {
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

    private static boolean onAllowLoad(
            Entity entity, ServerLevel level, EntitySpawnReason reason, boolean loadedFromDisk) {
        if (loadedFromDisk || reason != EntitySpawnReason.NATURAL) {
            return true;
        }
        if (!(entity instanceof Mob mob)) {
            return true;
        }
        replaceIfNeeded(mob, level, reason);
        return !mob.isRemoved();
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
