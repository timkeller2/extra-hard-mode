package dev.extrahardmode.task;

import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.feature.Torches;
import dev.extrahardmode.world.EhmTags;
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
 * Rain pass: every ticking chunk every {@link #CHUNK_STAGGER_TICKS}, 1% per exposed
 * torch, max 16 drops/tick. Covered (cannot see sky) survive. {@code isRainingAt}
 * already requires rain, sky, and a raining biome.
 */
public final class RemoveExposedTorchesTask {
    public static final int MAX_TORCHES_PER_TICK = 16;
    /** All ticking chunks, once per second. */
    public static final int CHUNK_STAGGER_TICKS = 20;
    public static final float BREAK_CHANCE = 0.01F;
    /** Walk down from WORLD_SURFACE so snow/plants above a torch still count. */
    public static final int COLUMN_SCAN_DEPTH = 8;

    private static final Map<Identifier, Stagger> STAGGERS = new ConcurrentHashMap<>();
    /** Forces {@link Stagger} to load with this class, not on the first rain tick. */
    private static final Class<Stagger> STAGGER = Stagger.class;

    private RemoveExposedTorchesTask() {}

    public static void clear(ServerLevel level) {
        STAGGERS.remove(level.dimension().identifier());
    }

    /**
     * WORLD_SURFACE is often the air above the highest non-air block. Step down once
     * in that case so the torch itself is inspected.
     */
    public static int surfaceBlockY(int heightmapY, boolean heightmapCellIsAir) {
        return heightmapCellIsAir ? heightmapY - 1 : heightmapY;
    }

    public static boolean coveredFromSky(boolean canSeeSky) {
        return !canSeeSky;
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
        stagger.cooldown = CHUNK_STAGGER_TICKS - 1;
        int[] remaining = {torchBudget};
        level.getChunkSource().chunkMap.forEachBlockTickingChunk(chunk -> {
            remaining[0] = processChunk(level, chunk, config, remaining[0]);
        });
        return remaining[0];
    }

    static int processChunk(ServerLevel level, LevelChunk chunk, WorldConfig config, int torchBudget) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int remaining = torchBudget;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = minX + lx;
                int z = minZ + lz;
                int heightmapY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, lx, lz);
                cursor.set(x, heightmapY, z);
                int top = surfaceBlockY(heightmapY, chunk.getBlockState(cursor).isAir());
                int bottom = Math.max(chunk.getMinY(), top - COLUMN_SCAN_DEPTH);
                for (int y = top; y >= bottom; y--) {
                    cursor.set(x, y, z);
                    BlockState state = chunk.getBlockState(cursor);
                    if (state.isAir()) {
                        continue;
                    }
                    if (coveredFromSky(level.canSeeSky(cursor))) {
                        break;
                    }
                    if (!level.isRainingAt(cursor)) {
                        continue;
                    }
                    if (config.rainBreaksTorches()
                            && remaining > 0
                            && state.is(EhmTags.DEPTH_LIMITED_LIGHTS)) {
                        if (level.getRandom().nextFloat() < BREAK_CHANCE) {
                            BlockPos broken = cursor.immutable();
                            level.destroyBlock(broken, true);
                            Torches.forgetPlaced(level, broken);
                            remaining--;
                        }
                        continue;
                    }
                    if (config.rainExtinguishesCampfires()
                            && (state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE))
                            && state.getValue(CampfireBlock.LIT)) {
                        level.setBlock(cursor.immutable(), state.setValue(CampfireBlock.LIT, false), 3);
                    }
                }
            }
        }
        return remaining;
    }

    private static final class Stagger {
        int cooldown;
    }
}
