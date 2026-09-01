package dev.extrahardmode.world;

import dev.extrahardmode.tag.EhmTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Deep Dark / protected structures / {@code #physics_protected} never run physics.
 * Unloaded chunks are “not yet” and must be requeued, not dropped.
 */
public final class PhysicsSkip {
    private PhysicsSkip() {}

    /** Chunk or entity-ticking not ready. Do not read biome/block; that can load the chunk. */
    public static boolean notReady(ServerLevel level, BlockPos pos) {
        if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            return true;
        }
        return !level.areEntitiesLoaded(ChunkPos.pack(pos));
    }

    /** Permanent skip: biome, structure piece, or protected block. False when the chunk is not loaded. */
    public static boolean never(ServerLevel level, BlockPos pos) {
        if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            return false;
        }
        if (level.getBiome(pos).is(EhmTags.NO_PHYSICS)) {
            return true;
        }
        StructureStart start =
                level.structureManager().getStructureWithPieceAt(pos, EhmTags.PHYSICS_PROTECTED_STRUCTURES);
        if (start != null && start.isValid()) {
            return true;
        }
        BlockState state = level.getBlockState(pos);
        return state.is(EhmTags.PHYSICS_PROTECTED);
    }

    public static boolean skip(ServerLevel level, BlockPos pos) {
        return notReady(level, pos) || never(level, pos);
    }
}
