package dev.extrahardmode.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.extrahardmode.ExtraHardModeMod;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Hostile player-kills in each chunk. The count stays with the world. */
public final class ChunkClearData extends SavedData {
    public static final Codec<ChunkClearData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.LONG.listOf().optionalFieldOf("chunks", List.of()).forGetter(ChunkClearData::chunkList),
                    Codec.INT.listOf().optionalFieldOf("kills", List.of()).forGetter(ChunkClearData::killList))
            .apply(instance, ChunkClearData::new));

    public static final SavedDataType<ChunkClearData> TYPE = new SavedDataType<>(
            ExtraHardModeMod.id("chunk_clear"), ChunkClearData::new, CODEC, DataFixTypes.SAVED_DATA_MAP_INDEX);

    private final Long2IntOpenHashMap kills = new Long2IntOpenHashMap();

    {
        kills.defaultReturnValue(0);
    }

    public ChunkClearData() {}

    public ChunkClearData(List<Long> chunks, List<Integer> counts) {
        int size = Math.min(chunks.size(), counts.size());
        for (int i = 0; i < size; i++) {
            int count = counts.get(i);
            if (count > 0) {
                kills.put(chunks.get(i).longValue(), count);
            }
        }
    }

    public static ChunkClearData of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public int kills(BlockPos pos) {
        return kills.get(ChunkPos.pack(pos));
    }

    /** Adds one kill and returns the count before this one. */
    public int addKill(BlockPos pos) {
        long packed = ChunkPos.pack(pos);
        int before = kills.get(packed);
        kills.put(packed, before + 1);
        setDirty();
        return before;
    }

    private List<Long> chunkList() {
        List<Long> list = new ArrayList<>(kills.size());
        kills.long2IntEntrySet().forEach(entry -> list.add(entry.getLongKey()));
        return list;
    }

    private List<Integer> killList() {
        List<Integer> list = new ArrayList<>(kills.size());
        kills.long2IntEntrySet().forEach(entry -> list.add(entry.getIntValue()));
        return list;
    }
}
