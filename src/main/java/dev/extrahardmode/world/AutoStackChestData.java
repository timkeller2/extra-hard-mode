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
 * Excluded slots for each chest, keyed by the canonical block position.
 * A mask of 0 is not stored. Bit 0 is the first slot of that chest.
 */
public final class AutoStackChestData extends SavedData {
    public static final Codec<AutoStackChestData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.LONG.listOf().optionalFieldOf("positions", List.of()).forGetter(AutoStackChestData::positionList),
                    Codec.LONG.listOf().optionalFieldOf("masks", List.of()).forGetter(AutoStackChestData::maskList))
            .apply(instance, AutoStackChestData::new));

    public static final SavedDataType<AutoStackChestData> TYPE = new SavedDataType<>(
            ExtraHardModeMod.id("auto_stack_chests"), AutoStackChestData::new, CODEC, DataFixTypes.SAVED_DATA_MAP_INDEX);

    private final Long2LongOpenHashMap masks = new Long2LongOpenHashMap();

    {
        masks.defaultReturnValue(0L);
    }

    public AutoStackChestData() {}

    public AutoStackChestData(List<Long> positions, List<Long> stored) {
        int size = Math.min(positions.size(), stored.size());
        for (int i = 0; i < size; i++) {
            long mask = stored.get(i);
            if (mask != 0L) {
                masks.put(positions.get(i).longValue(), mask);
            }
        }
    }

    public static AutoStackChestData of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public long mask(BlockPos pos) {
        if (pos == null) {
            return 0L;
        }
        return masks.get(pos.asLong());
    }

    /** Stores the mask. A zero mask removes the chest. */
    public void set(BlockPos pos, long mask) {
        if (pos == null) {
            return;
        }
        long key = pos.asLong();
        if (mask == 0L) {
            if (masks.containsKey(key)) {
                masks.remove(key);
                setDirty();
            }
            return;
        }
        if (masks.put(key, mask) != mask) {
            setDirty();
        }
    }

    public void forget(BlockPos pos) {
        set(pos, 0L);
    }

    private List<Long> positionList() {
        List<Long> list = new ArrayList<>(masks.size());
        masks.long2LongEntrySet().forEach(entry -> {
            if (entry.getLongValue() != 0L) {
                list.add(entry.getLongKey());
            }
        });
        return list;
    }

    private List<Long> maskList() {
        List<Long> list = new ArrayList<>(masks.size());
        masks.long2LongEntrySet().forEach(entry -> {
            if (entry.getLongValue() != 0L) {
                list.add(entry.getLongValue());
            }
        });
        return list;
    }
}
