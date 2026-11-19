package dev.extrahardmode.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.extrahardmode.ExtraHardModeMod;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Real-world expiry, in epoch millis, for each chunk a lodestone is keeping active. */
public final class LodestoneChunkData extends SavedData {
    public static final Codec<LodestoneChunkData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.LONG.listOf().optionalFieldOf("chunks", List.of()).forGetter(LodestoneChunkData::chunkList),
                    Codec.LONG.listOf().optionalFieldOf("expires", List.of()).forGetter(LodestoneChunkData::expiryList))
            .apply(instance, LodestoneChunkData::new));

    public static final SavedDataType<LodestoneChunkData> TYPE = new SavedDataType<>(
            ExtraHardModeMod.id("lodestone_chunks"),
            LodestoneChunkData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_MAP_INDEX);

    private final Long2LongOpenHashMap expiresAt = new Long2LongOpenHashMap();

    {
        expiresAt.defaultReturnValue(0L);
    }

    public LodestoneChunkData() {}

    public LodestoneChunkData(List<Long> chunks, List<Long> expires) {
        int size = Math.min(chunks.size(), expires.size());
        for (int i = 0; i < size; i++) {
            long expiry = expires.get(i);
            if (expiry > 0L) {
                expiresAt.put(chunks.get(i).longValue(), expiry);
            }
        }
    }

    public static LodestoneChunkData of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public long expiry(long packedChunk) {
        return expiresAt.get(packedChunk);
    }

    public void setExpiry(long packedChunk, long expiry) {
        if (expiresAt.get(packedChunk) == expiry) {
            return;
        }
        expiresAt.put(packedChunk, expiry);
        setDirty();
    }

    public void remove(long packedChunk) {
        if (!expiresAt.containsKey(packedChunk)) {
            return;
        }
        expiresAt.remove(packedChunk);
        setDirty();
    }

    public int size() {
        return expiresAt.size();
    }

    /** Packed chunk positions. The array is a copy. */
    public long[] packedChunks() {
        long[] packed = new long[expiresAt.size()];
        int index = 0;
        var iterator = expiresAt.keySet().iterator();
        while (iterator.hasNext()) {
            packed[index++] = iterator.nextLong();
        }
        return packed;
    }

    private List<Long> chunkList() {
        List<Long> list = new ArrayList<>(expiresAt.size());
        expiresAt.long2LongEntrySet().forEach(entry -> list.add(entry.getLongKey()));
        return list;
    }

    private List<Long> expiryList() {
        List<Long> list = new ArrayList<>(expiresAt.size());
        expiresAt.long2LongEntrySet().forEach(entry -> list.add(entry.getLongValue()));
        return list;
    }
}
