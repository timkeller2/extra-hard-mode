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
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Fruits already grown on each pumpkin/melon vine. Used for the rising weed chance. */
public final class StemFruitData extends SavedData {
    public static final Codec<StemFruitData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.LONG.listOf().optionalFieldOf("positions", List.of()).forGetter(StemFruitData::positionList),
                    Codec.INT.listOf().optionalFieldOf("counts", List.of()).forGetter(StemFruitData::countList))
            .apply(instance, StemFruitData::new));

    public static final SavedDataType<StemFruitData> TYPE = new SavedDataType<>(
            ExtraHardModeMod.id("stem_fruit"), StemFruitData::new, CODEC, DataFixTypes.SAVED_DATA_MAP_INDEX);

    private final Long2IntOpenHashMap counts = new Long2IntOpenHashMap();

    {
        counts.defaultReturnValue(0);
    }

    public StemFruitData() {}

    public StemFruitData(List<Long> positions, List<Integer> values) {
        int size = Math.min(positions.size(), values.size());
        for (int i = 0; i < size; i++) {
            int count = values.get(i);
            if (count > 0) {
                counts.put(positions.get(i).longValue(), count);
            }
        }
    }

    public static StemFruitData of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public int fruitsGrown(BlockPos pos) {
        return counts.get(pos.asLong());
    }

    public void setFruitsGrown(BlockPos pos, int count) {
        long packed = pos.asLong();
        if (count <= 0) {
            remove(pos);
            return;
        }
        counts.put(packed, count);
        setDirty();
    }

    public void remove(BlockPos pos) {
        if (counts.containsKey(pos.asLong())) {
            counts.remove(pos.asLong());
            setDirty();
        }
    }

    private List<Long> positionList() {
        List<Long> list = new ArrayList<>(counts.size());
        counts.long2IntEntrySet().forEach(entry -> list.add(entry.getLongKey()));
        return list;
    }

    private List<Integer> countList() {
        List<Integer> list = new ArrayList<>(counts.size());
        counts.long2IntEntrySet().forEach(entry -> list.add(entry.getIntValue()));
        return list;
    }
}
