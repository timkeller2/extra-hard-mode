package dev.extrahardmode.world;

import dev.extrahardmode.tag.EhmTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/** Deep Dark biome, protected structures, protected blocks, and unloaded chunks never run physics. */
public final class PhysicsSkip {
    private PhysicsSkip() {}

    public static boolean skip(ServerLevel level, BlockPos pos) {
        if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            return true;
        }
        if (!level.areEntitiesLoaded(ChunkPos.pack(pos))) {
            return true;
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
}
