package dev.extrahardmode.task;

import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.world.EhmTags;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Rain pass: exposed torches drop as items; covered (any block above) survive.
 * Campfires unlit when configured (default off). Budgeted to 16 torch drops/tick/world.
 */
public final class RemoveExposedTorchesTask {
    public static final int MAX_TORCHES_PER_TICK = 16;

    private RemoveExposedTorchesTask() {}

    public static int run(ServerLevel level, WorldConfig config, int torchBudget) {
        if (!level.isRaining()) {
            return torchBudget;
        }
        if (!config.rainBreaksTorches() && !config.rainExtinguishesCampfires()) {
            return torchBudget;
        }
        int remaining = torchBudget;
        List<LevelChunk> chunks = new ArrayList<>();
        level.getChunkSource().chunkMap.forEachBlockTickingChunk(chunks::add);
        for (LevelChunk chunk : chunks) {
            if (remaining <= 0 && !config.rainExtinguishesCampfires()) {
                break;
            }
            remaining = processChunk(level, chunk, config, remaining);
        }
        return remaining;
    }

    static int processChunk(ServerLevel level, LevelChunk chunk, WorldConfig config, int torchBudget) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int remaining = torchBudget;
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = minX + lx;
                int z = minZ + lz;
                int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, lx, lz);
                BlockPos pos = new BlockPos(x, y, z);
                BlockState state = level.getBlockState(pos);
                if (state.isAir()) {
                    pos = pos.below();
                    state = level.getBlockState(pos);
                }
                if (state.isAir() || !level.canSeeSky(pos) || !level.isRainingAt(pos)) {
                    continue;
                }
                if (config.rainBreaksTorches() && remaining > 0 && state.is(EhmTags.DEPTH_LIMITED_LIGHTS)) {
                    // 1% per exposed torch on this chunk pass
                    if (level.getRandom().nextFloat() < 0.01F) {
                        level.destroyBlock(pos, true);
                        remaining--;
                    }
                    continue;
                }
                if (config.rainExtinguishesCampfires()
                        && (state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE))
                        && state.getValue(CampfireBlock.LIT)) {
                    level.setBlock(pos, state.setValue(CampfireBlock.LIT, false), 3);
                }
            }
        }
        return remaining;
    }
}
