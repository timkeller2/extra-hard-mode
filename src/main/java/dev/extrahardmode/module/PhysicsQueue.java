package dev.extrahardmode.module;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.GlobalConfig;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.PhysicsSkip;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class PhysicsQueue {
    private static final Map<Identifier, PhysicsQueue> QUEUES = new ConcurrentHashMap<>();

    private final ArrayDeque<FallRequest> queue = new ArrayDeque<>();
    private final LongOpenHashSet queued = new LongOpenHashSet();
    private final Map<UUID, FallingBlockEntity> live = new ConcurrentHashMap<>();
    private int conversionsLastTick;
    private int dropped;

    private PhysicsQueue() {}

    public static PhysicsQueue of(ServerLevel level) {
        return QUEUES.computeIfAbsent(level.dimension().identifier(), id -> new PhysicsQueue());
    }

    public static void discard(ServerLevel level) {
        QUEUES.remove(level.dimension().identifier());
    }

    public static void tick(ServerLevel level) {
        of(level).drain(level);
    }

    public int queueDepth() {
        return queue.size();
    }

    public int conversionsLastTick() {
        return conversionsLastTick;
    }

    public int dropped() {
        return dropped;
    }

    public int liveEntities() {
        pruneLive();
        return live.size();
    }

    public void enqueueConvert(ServerLevel level, BlockPos pos, BlockState to, boolean applyPhysics) {
        enqueue(level, pos, to, applyPhysics);
    }

    public void enqueueFalling(ServerLevel level, BlockPos pos, BlockState state) {
        enqueue(level, pos, state, true);
    }

    public void markLanded(FallingBlockEntity entity) {
        live.remove(entity.getUUID());
    }

    private void enqueue(ServerLevel level, BlockPos pos, BlockState state, boolean spawnEntity) {
        if (PhysicsSkip.skip(level, pos)) {
            return;
        }
        long key = pos.asLong();
        if (!queued.add(key)) {
            return;
        }
        GlobalConfig global = ConfigManager.global();
        if (PhysicsBudget.shouldDropOldest(queue.size(), global.maxQueueDepth())) {
            FallRequest oldest = queue.pollFirst();
            if (oldest != null) {
                queued.remove(oldest.pos.asLong());
            }
            dropped++;
            ExtraHardModeMod.LOGGER.warn(
                    "EHM physics queue overflow in {}, dropped={} (max {})",
                    level.dimension().identifier(),
                    dropped,
                    global.maxQueueDepth());
        }
        queue.addLast(new FallRequest(pos.immutable(), state, spawnEntity, 0));
        if (ConfigManager.global().debug()) {
            ExtraHardModeMod.LOGGER.debug("EHM physics enqueue {} {}", state, pos);
        }
    }

    private void drain(ServerLevel level) {
        conversionsLastTick = 0;
        pruneLive();
        GlobalConfig global = ConfigManager.global();
        int budget = global.budgetConversionsPerTick();
        int n = queue.size();
        for (int i = 0; i < n; i++) {
            FallRequest request = queue.pollFirst();
            if (request == null) {
                break;
            }
            queued.remove(request.pos.asLong());
            if (request.delayTicks > 0) {
                request.delayTicks--;
                requeue(request);
                continue;
            }
            if (conversionsLastTick >= budget) {
                requeue(request);
                continue;
            }
            if (PhysicsSkip.skip(level, request.pos)) {
                continue;
            }
            convert(level, request, global);
            conversionsLastTick++;
        }
    }

    private void requeue(FallRequest request) {
        if (queued.add(request.pos.asLong())) {
            queue.addLast(request);
        }
    }

    private void convert(ServerLevel level, FallRequest request, GlobalConfig global) {
        BlockState current = level.getBlockState(request.pos);
        if (current.isAir()) {
            return;
        }
        BlockState place = request.state;
        if (!request.spawnEntity) {
            level.setBlock(request.pos, place, Block.UPDATE_ALL);
            return;
        }
        pruneLive();
        if (PhysicsBudget.overflowToSetBlock(live.size(), global.maxLiveEhmFallingEntities())) {
            level.setBlock(request.pos, place, Block.UPDATE_ALL);
            return;
        }
        FallingBlockEntity entity = FallingBlockEntity.fall(level, request.pos, place);
        WorldConfig config = ConfigManager.world(level);
        entity.dropItem = config.fallingDropAsItemWhenBlocked();
        entity.setAttached(EhmAttachments.EHM_OURS, Boolean.TRUE);
        live.put(entity.getUUID(), entity);
    }

    private void pruneLive() {
        Iterator<Map.Entry<UUID, FallingBlockEntity>> iterator = live.entrySet().iterator();
        while (iterator.hasNext()) {
            FallingBlockEntity entity = iterator.next().getValue();
            if (entity == null || entity.isRemoved()) {
                iterator.remove();
            }
        }
    }

    public static boolean canFall(ServerLevel level, BlockPos pos) {
        return FallingBlock.isFree(level.getBlockState(pos.below()));
    }

    public static BlockState unwaterlog(BlockState state) {
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            return state.setValue(BlockStateProperties.WATERLOGGED, false);
        }
        return state;
    }

    private static final class FallRequest {
        private final BlockPos pos;
        private final BlockState state;
        private final boolean spawnEntity;
        private int delayTicks;

        private FallRequest(BlockPos pos, BlockState state, boolean spawnEntity, int delayTicks) {
            this.pos = pos;
            this.state = state;
            this.spawnEntity = spawnEntity;
            this.delayTicks = delayTicks;
        }
    }
}
