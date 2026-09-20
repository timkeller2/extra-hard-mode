package dev.extrahardmode.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.extrahardmode.ExtraHardModeMod;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Grass blocks claimed by livestock for the current Minecraft day. */
public final class OvergrazeGrassData extends SavedData {
    public static final Codec<OvergrazeGrassData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.LONG.listOf().optionalFieldOf("positions", List.of()).forGetter(OvergrazeGrassData::positionList),
                    Codec.INT.listOf().optionalFieldOf("days", List.of()).forGetter(OvergrazeGrassData::dayList))
            .apply(instance, OvergrazeGrassData::new));

    public static final SavedDataType<OvergrazeGrassData> TYPE = new SavedDataType<>(
            ExtraHardModeMod.id("overgraze_grass"), OvergrazeGrassData::new, CODEC, DataFixTypes.SAVED_DATA_MAP_INDEX);

    private final Long2IntOpenHashMap markedDay = new Long2IntOpenHashMap();
    private int prunedDay = Integer.MIN_VALUE;

    {
        markedDay.defaultReturnValue(-1);
    }

    public OvergrazeGrassData() {}

    public OvergrazeGrassData(List<Long> positions, List<Integer> days) {
        int size = Math.min(positions.size(), days.size());
        for (int i = 0; i < size; i++) {
            markedDay.put(positions.get(i).longValue(), days.get(i).intValue());
        }
    }

    public static OvergrazeGrassData of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public void prepareDay(int day) {
        if (prunedDay == day) {
            return;
        }
        LongArrayList stale = new LongArrayList();
        markedDay.long2IntEntrySet().forEach(entry -> {
            if (entry.getIntValue() != day) {
                stale.add(entry.getLongKey());
            }
        });
        if (!stale.isEmpty()) {
            for (int i = 0; i < stale.size(); i++) {
                markedDay.remove(stale.getLong(i));
            }
            setDirty();
        }
        prunedDay = day;
    }

    public boolean isMarkedToday(BlockPos pos, int day) {
        return markedDay.get(pos.asLong()) == day;
    }

    public void mark(BlockPos pos, int day) {
        markedDay.put(pos.asLong(), day);
        setDirty();
    }

    private List<Long> positionList() {
        List<Long> list = new ArrayList<>(markedDay.size());
        markedDay.long2IntEntrySet().forEach(entry -> list.add(entry.getLongKey()));
        return list;
    }

    private List<Integer> dayList() {
        List<Integer> list = new ArrayList<>(markedDay.size());
        markedDay.long2IntEntrySet().forEach(entry -> list.add(entry.getIntValue()));
        return list;
    }
}
