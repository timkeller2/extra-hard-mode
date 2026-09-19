package dev.extrahardmode.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.extrahardmode.ExtraHardModeMod;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Place-time stamps for torches and campfires that should burn out. Positions with
 * no entry are permanent (worldgen and lights placed before this data existed).
 */
public final class TorchLifetimeData extends SavedData {
    public static final Codec<TorchLifetimeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.LONG.listOf().optionalFieldOf("positions", List.of()).forGetter(TorchLifetimeData::positionList),
                    Codec.LONG.listOf().optionalFieldOf("placedAt", List.of()).forGetter(TorchLifetimeData::placedAtList))
            .apply(instance, TorchLifetimeData::new));

    public static final SavedDataType<TorchLifetimeData> TYPE = new SavedDataType<>(
            ExtraHardModeMod.id("torch_lifetime"), TorchLifetimeData::new, CODEC, DataFixTypes.SAVED_DATA_MAP_INDEX);

    private final Long2LongOpenHashMap placedAt = new Long2LongOpenHashMap();

    {
        placedAt.defaultReturnValue(-1L);
    }

    public TorchLifetimeData() {}

    public TorchLifetimeData(List<Long> positions, List<Long> times) {
        int size = Math.min(positions.size(), times.size());
        for (int i = 0; i < size; i++) {
            long time = times.get(i);
            if (time >= 0L) {
                placedAt.put(positions.get(i).longValue(), time);
            }
        }
    }

    public static TorchLifetimeData of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public void record(BlockPos pos, long gameTime) {
        placedAt.put(pos.asLong(), Math.max(0L, gameTime));
        setDirty();
    }

    public long placedAt(BlockPos pos) {
        return placedAt.get(pos.asLong());
    }

    public boolean hasStamp(BlockPos pos) {
        return placedAt.containsKey(pos.asLong());
    }

    public void remove(BlockPos pos) {
        if (placedAt.containsKey(pos.asLong())) {
            placedAt.remove(pos.asLong());
            setDirty();
        }
    }

    public void removePacked(long packed) {
        if (placedAt.containsKey(packed)) {
            placedAt.remove(packed);
            setDirty();
        }
    }

    public Long2LongOpenHashMap stamps() {
        return placedAt;
    }

    private List<Long> positionList() {
        List<Long> list = new ArrayList<>(placedAt.size());
        placedAt.long2LongEntrySet().forEach(entry -> list.add(entry.getLongKey()));
        return list;
    }

    private List<Long> placedAtList() {
        List<Long> list = new ArrayList<>(placedAt.size());
        placedAt.long2LongEntrySet().forEach(entry -> list.add(entry.getLongValue()));
        return list;
    }
}
