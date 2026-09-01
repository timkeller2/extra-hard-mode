package dev.extrahardmode.task;

import dev.extrahardmode.api.event.ZombieRespawnEvent;
import dev.extrahardmode.module.EntityHelper;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Delayed zombie reanimate. Skip if the skull was broken or the chunk unloaded. */
public final class RespawnZombieTask {
    private final ServerLevel level;
    private final Vec3 origin;
    private final UUID targetId;
    private final BlockPos skullPos;
    private final int reanimateCount;
    private int ticksLeft;
    private boolean cancelled;

    public RespawnZombieTask(
            ServerLevel level,
            Vec3 origin,
            UUID targetId,
            BlockPos skullPos,
            int reanimateCount,
            int delayTicks) {
        this.level = level;
        this.origin = origin;
        this.targetId = targetId;
        this.skullPos = skullPos;
        this.reanimateCount = reanimateCount;
        this.ticksLeft = delayTicks;
    }

    public BlockPos skullPos() {
        return skullPos;
    }

    public void cancel() {
        cancelled = true;
        clearSkull();
    }

    public boolean tick() {
        if (cancelled) {
            return true;
        }
        if (--ticksLeft > 0) {
            return false;
        }
        run();
        return true;
    }

    private void run() {
        if (cancelled) {
            return;
        }
        BlockPos pos = BlockPos.containing(origin);
        if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            clearSkull();
            return;
        }
        if (skullPos != null && !level.getBlockState(skullPos).is(Blocks.ZOMBIE_HEAD)) {
            return;
        }
        Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.EVENT);
        if (zombie == null) {
            clearSkull();
            return;
        }
        zombie.snapTo(origin.x, origin.y, origin.z);
        zombie.setHealth(zombie.getMaxHealth() / 2.0F);
        EntityHelper.markLootless(zombie);
        EntityHelper.setReanimateCount(zombie, reanimateCount);
        ServerPlayer target = targetId == null ? null : level.getServer().getPlayerList().getPlayer(targetId);
        if (target != null && target.isAlive() && target.level() == level) {
            zombie.setTarget(target);
        }
        if (!level.addFreshEntity(zombie)) {
            clearSkull();
            return;
        }
        ZombieRespawnEvent event = new ZombieRespawnEvent(target, zombie);
        ZombieRespawnEvent.EVENT.invoker().onZombieRespawn(event);
        if (event.isCanceled()) {
            zombie.discard();
        }
        clearSkull();
    }

    private void clearSkull() {
        if (skullPos == null) {
            return;
        }
        if (level.hasChunk(skullPos.getX() >> 4, skullPos.getZ() >> 4)
                && level.getBlockState(skullPos).is(Blocks.ZOMBIE_HEAD)) {
            level.setBlock(skullPos, Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
