package dev.extrahardmode.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.feature.LodestoneChunkRules;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Time left, in real milliseconds, for each chunk a lodestone is keeping active.
 * The value does not move while it is only sitting in this file.
 */
public final class LodestoneChunkData extends SavedData {
    public static final Codec<LodestoneChunkData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.LONG.listOf().optionalFieldOf("chunks", List.of()).forGetter(LodestoneChunkData::chunkList),
                    Codec.LONG
                            .listOf()
                            .optionalFieldOf("remaining", List.of())
                            .forGetter(LodestoneChunkData::remainingList),
                    Codec.LONG.listOf().optionalFieldOf("expires", List.of()).forGetter(data -> List.of()))
            .apply(instance, LodestoneChunkData::new));

    public static final SavedDataType<LodestoneChunkData> TYPE = new SavedDataType<>(
            ExtraHardModeMod.id("lodestone_chunks"),
            LodestoneChunkData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_MAP_INDEX);

    private final Long2LongOpenHashMap remaining = new Long2LongOpenHashMap();

    {
        remaining.defaultReturnValue(0L);
    }

    public LodestoneChunkData() {}

    public LodestoneChunkData(List<Long> chunks, List<Long> remainingMillis, List<Long> legacyExpires) {
        boolean legacy = remainingMillis.isEmpty();
        List<Long> values = legacy ? legacyExpires : remainingMillis;
        long now = System.currentTimeMillis();
        int size = Math.min(chunks.size(), values.size());
        for (int i = 0; i < size; i++) {
            long left = LodestoneChunkRules.fromStored(values.get(i), now);
            if (left > 0L) {
                remaining.put(chunks.get(i).longValue(), left);
            }
        }
    }

    public static LodestoneChunkData of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public long remaining(long packedChunk) {
        return remaining.get(packedChunk);
    }

    public void setRemaining(long packedChunk, long millis) {
        if (remaining.get(packedChunk) == millis) {
            return;
        }
        remaining.put(packedChunk, millis);
        setDirty();
    }

    public void remove(long packedChunk) {
        if (!remaining.containsKey(packedChunk)) {
            return;
        }
        remaining.remove(packedChunk);
        setDirty();
    }

    public int size() {
        return remaining.size();
    }

    /** Packed chunk positions. The array is a copy. */
    public long[] packedChunks() {
        long[] packed = new long[remaining.size()];
        int index = 0;
        var iterator = remaining.keySet().iterator();
        while (iterator.hasNext()) {
            packed[index++] = iterator.nextLong();
        }
        return packed;
    }

    private List<Long> chunkList() {
        List<Long> list = new ArrayList<>(remaining.size());
        remaining.long2LongEntrySet().forEach(entry -> list.add(entry.getLongKey()));
        return list;
    }

    private List<Long> remainingList() {
        List<Long> list = new ArrayList<>(remaining.size());
        remaining.long2LongEntrySet().forEach(entry -> list.add(entry.getLongValue()));
        return list;
    }
}
