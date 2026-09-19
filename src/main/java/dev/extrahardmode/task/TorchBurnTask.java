package dev.extrahardmode.task;

import dev.extrahardmode.feature.TorchLifetimeRules;
import dev.extrahardmode.feature.Torches;
import dev.extrahardmode.world.TorchLifetimeData;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Burns out stamped torches after {@code burnDays} Minecraft days. Unstamped
 * torches are never touched.
 */
public final class TorchBurnTask {
    private TorchBurnTask() {}

    public static void run(ServerLevel level, int burnDays) {
        if (TorchLifetimeRules.permanent(burnDays)) {
            return;
        }
        if (level.getGameTime() % TorchLifetimeRules.SCAN_INTERVAL_TICKS != 0L) {
            return;
        }
        TorchLifetimeData data = TorchLifetimeData.of(level);
        if (data.stamps().isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        LongArrayList stale = new LongArrayList();
        LongArrayList expired = new LongArrayList();
        for (Long2LongMap.Entry entry : data.stamps().long2LongEntrySet()) {
            long packed = entry.getLongKey();
            BlockPos pos = BlockPos.of(packed);
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!Torches.isBurnableTorch(state)) {
                stale.add(packed);
                continue;
            }
            if (expired.size() < TorchLifetimeRules.MAX_EXPIRE_PER_TICK
                    && TorchLifetimeRules.expired(entry.getLongValue(), now, burnDays)) {
                expired.add(packed);
            }
        }
        for (int i = 0; i < stale.size(); i++) {
            data.removePacked(stale.getLong(i));
        }
        for (int i = 0; i < expired.size(); i++) {
            BlockPos pos = BlockPos.of(expired.getLong(i));
            Torches.burnOut(level, pos);
            data.removePacked(expired.getLong(i));
        }
    }
}
