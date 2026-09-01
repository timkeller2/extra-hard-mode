package dev.extrahardmode.task;

import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.world.EhmTags;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Rain pass: one ticking chunk every {@link #CHUNK_STAGGER_TICKS} (storm stagger),
 * 1% per exposed torch per pass, max 16 drops/tick. Covered (any block above) survive.
 */
public final class RemoveExposedTorchesTask {
    public static final int MAX_TORCHES_PER_TICK = 16;
    /** Original scheduled one chunk every 100 ticks when rain starts. */
    public static final int CHUNK_STAGGER_TICKS = 100;

    private static final Map<Identifier, Stagger> STAGGERS = new ConcurrentHashMap<>();

    private RemoveExposedTorchesTask() {}

    public static void clear(ServerLevel level) {
        STAGGERS.remove(level.dimension().identifier());
    }

    public static int run(ServerLevel level, WorldConfig config, int torchBudget) {
        Identifier dim = level.dimension().identifier();
        if (!level.isRaining() || (!config.rainBreaksTorches() && !config.rainExtinguishesCampfires())) {
            STAGGERS.remove(dim);
            return torchBudget;
        }
        Stagger stagger = STAGGERS.computeIfAbsent(dim, id -> new Stagger());
        if (stagger.cooldown > 0) {
            stagger.cooldown--;
            return torchBudget;
        }
        List<LevelChunk> chunks = new ArrayList<>();
        level.getChunkSource().chunkMap.forEachBlockTickingChunk(chunks::add);
        if (chunks.isEmpty()) {
            return torchBudget;
        }
        LevelChunk chunk = chunks.get(Math.floorMod(stagger.cursor++, chunks.size()));
        int remaining = processChunk(level, chunk, config, torchBudget);
        stagger.cooldown = CHUNK_STAGGER_TICKS - 1;
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

    private static final class Stagger {
        int cooldown;
        int cursor;
    }
}
