package dev.extrahardmode.module;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.GlobalConfig;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.tag.EhmTags;
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

    public void enqueueConvert(
            ServerLevel level, BlockPos pos, BlockState from, BlockState to, boolean applyPhysics) {
        enqueue(level, pos, from, to, applyPhysics, 0);
    }

    public void enqueueFalling(ServerLevel level, BlockPos pos, BlockState from, BlockState to) {
        enqueue(level, pos, from, to, true, 0);
    }

    public void enqueueFalling(
            ServerLevel level, BlockPos pos, BlockState from, BlockState to, int delayTicks) {
        enqueue(level, pos, from, to, true, delayTicks);
    }

    public void markLanded(FallingBlockEntity entity) {
        live.remove(entity.getUUID());
    }

    /** Reloaded {@code EHM_OURS} falling entities still count against the 128 cap. */
    public void trackLive(FallingBlockEntity entity) {
        if (entity == null || entity.isRemoved()) {
            return;
        }
        if (!Boolean.TRUE.equals(entity.getAttachedOrElse(EhmAttachments.EHM_OURS, Boolean.FALSE))) {
            return;
        }
        live.put(entity.getUUID(), entity);
    }

    private void enqueue(
            ServerLevel level, BlockPos pos, BlockState from, BlockState to, boolean spawnEntity, int delayTicks) {
        if (PhysicsSkip.never(level, pos)) {
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
            if (dropped == 1 || dropped % 64 == 0) {
                ExtraHardModeMod.LOGGER.warn(
                        "EHM physics queue overflow in {}, dropped={} (max {})",
                        level.dimension().identifier(),
                        dropped,
                        global.maxQueueDepth());
            }
        }
        queue.addLast(new FallRequest(pos.immutable(), from, to, spawnEntity, Math.max(0, delayTicks)));
        if (ConfigManager.global().debug()) {
            ExtraHardModeMod.LOGGER.debug("EHM physics enqueue {} -> {} {} delay={}", from, to, pos, delayTicks);
        }
    }

    private void drain(ServerLevel level) {
        conversionsLastTick = 0;
        pruneLive();
        GlobalConfig global = ConfigManager.global();
        int budget = global.budgetConversionsPerTick();
        int snapshot = queue.size();
        int visited = 0;
        while (visited < snapshot && conversionsLastTick < budget && !queue.isEmpty()) {
            FallRequest request = queue.pollFirst();
            if (request == null) {
                break;
            }
            visited++;
            queued.remove(request.pos.asLong());
            if (request.delayTicks > 0) {
                request.delayTicks--;
                requeue(request);
                continue;
            }
            if (PhysicsSkip.notReady(level, request.pos)) {
                requeue(request);
                continue;
            }
            if (PhysicsSkip.never(level, request.pos)) {
                continue;
            }
            if (!convert(level, request, global)) {
                continue;
            }
            conversionsLastTick++;
        }
    }

    private void requeue(FallRequest request) {
        if (queued.add(request.pos.asLong())) {
            queue.addLast(request);
        }
    }

    private boolean convert(ServerLevel level, FallRequest request, GlobalConfig global) {
        BlockState current = level.getBlockState(request.pos);
        if (current.isAir() || current.is(EhmTags.PHYSICS_PROTECTED)) {
            return false;
        }
        if (current.getBlock() != request.from.getBlock()) {
            return false;
        }
        BlockState place = request.to;
        if (!request.spawnEntity) {
            level.setBlock(request.pos, place, Block.UPDATE_ALL);
            return true;
        }
        pruneLive();
        if (PhysicsBudget.overflowToSetBlock(live.size(), global.maxLiveEhmFallingEntities())) {
            level.setBlock(request.pos, place, Block.UPDATE_ALL);
            return true;
        }
        FallingBlockEntity entity = FallingBlockEntity.fall(level, request.pos, place);
        WorldConfig config = ConfigManager.world(level);
        entity.dropItem = config.fallingDropAsItemWhenBlocked();
        entity.setAttached(EhmAttachments.EHM_OURS, Boolean.TRUE);
        int amount = Math.max(0, config.fallingDamage());
        if (amount > 0) {
            entity.setHurtsEntities(amount, amount);
        }
        live.put(entity.getUUID(), entity);
        return true;
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
        private final BlockState from;
        private final BlockState to;
        private final boolean spawnEntity;
        private int delayTicks;

        private FallRequest(BlockPos pos, BlockState from, BlockState to, boolean spawnEntity, int delayTicks) {
            this.pos = pos;
            this.from = from;
            this.to = to;
            this.spawnEntity = spawnEntity;
            this.delayTicks = delayTicks;
        }
    }
}
