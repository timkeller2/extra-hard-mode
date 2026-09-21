package dev.extrahardmode.module;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.GlobalConfig;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.feature.FallingBlocks;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

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
        enqueue(level, pos, from, to, applyPhysics, 0, null, 0.0, 0.0);
    }

    public void enqueueFalling(ServerLevel level, BlockPos pos, BlockState from, BlockState to) {
        enqueue(level, pos, from, to, true, 0, null, 0.0, 0.0);
    }

    public void enqueueFalling(
            ServerLevel level, BlockPos pos, BlockState from, BlockState to, int delayTicks) {
        enqueue(level, pos, from, to, true, delayTicks, null, 0.0, 0.0);
    }

    /** Flying explosion debris. Budgeted like other conversions; live-cap overflow becomes air. */
    public void enqueueFlying(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            Vec3 origin,
            double upVelocity,
            double spreadVelocity) {
        enqueue(level, pos, state, state, true, 0, origin, upVelocity, spreadVelocity);
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
            ServerLevel level,
            BlockPos pos,
            BlockState from,
            BlockState to,
            boolean spawnEntity,
            int delayTicks,
            Vec3 flyOrigin,
            double upVelocity,
            double spreadVelocity) {
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
        queue.addLast(new FallRequest(
                pos.immutable(), from, to, spawnEntity, Math.max(0, delayTicks), flyOrigin, upVelocity, spreadVelocity));
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
            if (request.flyOrigin != null) {
                level.setBlock(request.pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                FallingBlocks.onBeganFalling(level, request.pos);
            } else {
                level.setBlock(request.pos, place, Block.UPDATE_ALL);
            }
            return true;
        }
        FallingBlockEntity entity = FallingBlockEntity.fall(level, request.pos, place);
        WorldConfig config = ConfigManager.world(level);
        // Vanilla deletes a falling block that cannot place when dropItem is false.
        // Trunk logs often share a landing cell while the piece below is still an
        // entity (the block under them is air), so those logs must drop as items.
        entity.dropItem = config.fallingDropAsItemWhenBlocked() || place.is(EhmTags.FELLABLE_LOGS);
        entity.setAttached(EhmAttachments.EHM_OURS, Boolean.TRUE);
        int amount = Math.max(0, config.fallingDamage());
        if (amount > 0) {
            entity.setHurtsEntities(amount, amount);
        }
        if (request.flyOrigin != null) {
            applyFlyVelocity(level, entity, request);
            entity.setAttached(EhmAttachments.EHM_FLY_ORIGIN, request.flyOrigin);
        }
        live.put(entity.getUUID(), entity);
        FallingBlocks.onBeganFalling(level, request.pos);
        return true;
    }

    private static void applyFlyVelocity(ServerLevel level, FallingBlockEntity entity, FallRequest request) {
        Vec3 away = entity.position().subtract(request.flyOrigin);
        if (away.lengthSqr() < 1.0E-6) {
            away = new Vec3(level.getRandom().nextGaussian(), 0.0, level.getRandom().nextGaussian());
        }
        if (away.lengthSqr() < 1.0E-6) {
            away = new Vec3(1.0, 0.0, 0.0);
        }
        away = away.normalize().scale(request.spreadVelocity);
        entity.setDeltaMovement(away.x, request.upVelocity, away.z);
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
        private final Vec3 flyOrigin;
        private final double upVelocity;
        private final double spreadVelocity;

        private FallRequest(
                BlockPos pos,
                BlockState from,
                BlockState to,
                boolean spawnEntity,
                int delayTicks,
                Vec3 flyOrigin,
                double upVelocity,
                double spreadVelocity) {
            this.pos = pos;
            this.from = from;
            this.to = to;
            this.spawnEntity = spawnEntity;
            this.delayTicks = delayTicks;
            this.flyOrigin = flyOrigin;
            this.upVelocity = upVelocity;
            this.spreadVelocity = spreadVelocity;
        }
    }
}
