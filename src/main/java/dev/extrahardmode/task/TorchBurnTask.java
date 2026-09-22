package dev.extrahardmode.task;

import dev.extrahardmode.feature.TorchLifetimeRules;
import dev.extrahardmode.feature.Torches;
import dev.extrahardmode.world.TorchLifetimeData;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Burns out stamped torches and campfires after {@code burnDays} Minecraft days.
 * Lit campfires pull a nearby log to last another period. Unstamped lights are
 * never touched. Torches are refueled by hand, not from chests.
 */
public final class TorchBurnTask {
    private static final Map<ServerLevel, Long2IntOpenHashMap> LAST_LIGHT_DAYS = new WeakHashMap<>();

    private TorchBurnTask() {}

    public static void clear(ServerLevel level) {
        LAST_LIGHT_DAYS.remove(level);
    }

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
        LongArrayList expiredCampfires = new LongArrayList();
        for (Long2LongMap.Entry entry : data.stamps().long2LongEntrySet()) {
            long packed = entry.getLongKey();
            BlockPos pos = BlockPos.of(packed);
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (Torches.isBurnableTorch(state)) {
                int torchDays = Torches.burnDaysFor(state, burnDays);
                if (expired.size() < TorchLifetimeRules.MAX_EXPIRE_PER_TICK
                        && TorchLifetimeRules.expired(entry.getLongValue(), now, torchDays)) {
                    expired.add(packed);
                } else {
                    refreshLight(level, pos, state, entry.getLongValue(), now, torchDays);
                }
                continue;
            }
            if (Torches.isCampfire(state)) {
                if (expiredCampfires.size() < TorchLifetimeRules.MAX_EXPIRE_PER_TICK
                        && TorchLifetimeRules.expired(entry.getLongValue(), now, burnDays)) {
                    expiredCampfires.add(packed);
                }
                continue;
            }
            stale.add(packed);
        }
        for (int i = 0; i < stale.size(); i++) {
            data.removePacked(stale.getLong(i));
        }
        for (int i = 0; i < expired.size(); i++) {
            long packed = expired.getLong(i);
            BlockPos pos = BlockPos.of(packed);
            Torches.burnOut(level, pos);
            data.removePacked(packed);
            Long2IntOpenHashMap lastDays = LAST_LIGHT_DAYS.get(level);
            if (lastDays != null) {
                lastDays.remove(packed);
            }
        }
        for (int i = 0; i < expiredCampfires.size(); i++) {
            long packed = expiredCampfires.getLong(i);
            BlockPos pos = BlockPos.of(packed);
            BlockState state = level.getBlockState(pos);
            if (Torches.isLitCampfire(state) && Torches.tryRefuelCampfire(level, pos)) {
                data.record(pos, TorchLifetimeRules.extendPlacedAt(data.placedAt(pos), burnDays));
                continue;
            }
            Torches.burnOutCampfire(level, pos);
            data.removePacked(packed);
        }
    }

    static void refreshLight(
            ServerLevel level, BlockPos pos, BlockState state, long placedAt, long now, int burnDays) {
        int vanilla = state.getLightEmission();
        int light = TorchLifetimeRules.lightLevel(vanilla, placedAt, now, burnDays);
        if (light >= vanilla) {
            return;
        }
        int days = TorchLifetimeRules.daysBurning(placedAt, now);
        Long2IntOpenHashMap lastDays = LAST_LIGHT_DAYS.computeIfAbsent(level, ignored -> new Long2IntOpenHashMap());
        lastDays.defaultReturnValue(Integer.MIN_VALUE);
        long packed = pos.asLong();
        if (lastDays.get(packed) == days) {
            return;
        }
        lastDays.put(packed, days);
        level.getLightEngine().checkBlock(pos);
    }
}
