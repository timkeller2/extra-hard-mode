package dev.extrahardmode.task;

import dev.extrahardmode.module.PhysicsQueue;
import dev.extrahardmode.tag.EhmTags;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Staggers log converts through {@link PhysicsQueue} so stacked falling entities do not collide.
 */
public final class FallingLogsTask {
    private FallingLogsTask() {}

    public static void enqueue(ServerLevel level, List<BlockPos> logs, BlockPos skip) {
        List<BlockPos> ordered = new ArrayList<>(logs.size());
        for (BlockPos pos : logs) {
            if (skip != null && pos.equals(skip)) {
                continue;
            }
            ordered.add(pos.immutable());
        }
        ordered.sort(Comparator.comparingInt((BlockPos pos) -> pos.getY())
                .thenComparingInt(pos -> pos.getX())
                .thenComparingInt(pos -> pos.getZ()));
        PhysicsQueue queue = PhysicsQueue.of(level);
        int delay = 0;
        int lastY = Integer.MIN_VALUE;
        for (BlockPos pos : ordered) {
            if (lastY != Integer.MIN_VALUE && pos.getY() != lastY) {
                delay++;
            }
            lastY = pos.getY();
            BlockState state = level.getBlockState(pos);
            if (!state.is(EhmTags.FELLABLE_LOGS)) {
                continue;
            }
            queue.enqueueFalling(level, pos, state, PhysicsQueue.unwaterlog(state), delay);
        }
    }
}
