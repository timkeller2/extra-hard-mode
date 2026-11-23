package dev.extrahardmode.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.feature.CropGrowthRules;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Last bone-meal game time for each glow berry vine segment. */
public final class GlowBerryMealData extends SavedData {
    public static final Codec<GlowBerryMealData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.LONG.listOf().optionalFieldOf("positions", List.of()).forGetter(GlowBerryMealData::positionList),
                    Codec.LONG.listOf().optionalFieldOf("usedAt", List.of()).forGetter(GlowBerryMealData::usedAtList))
            .apply(instance, GlowBerryMealData::new));

    public static final SavedDataType<GlowBerryMealData> TYPE = new SavedDataType<>(
            ExtraHardModeMod.id("glow_berry_meal"), GlowBerryMealData::new, CODEC, DataFixTypes.SAVED_DATA_MAP_INDEX);

    private final Long2LongOpenHashMap usedAt = new Long2LongOpenHashMap();

    {
        usedAt.defaultReturnValue(-1L);
    }

    public GlowBerryMealData() {}

    public GlowBerryMealData(List<Long> positions, List<Long> times) {
        int size = Math.min(positions.size(), times.size());
        for (int i = 0; i < size; i++) {
            long time = times.get(i);
            if (time >= 0L) {
                usedAt.put(positions.get(i).longValue(), time);
            }
        }
    }

    public static GlowBerryMealData of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public void mark(BlockPos pos, long gameTime) {
        usedAt.put(pos.asLong(), Math.max(0L, gameTime));
        setDirty();
    }

    /** Ticks left. An expired entry is dropped. */
    public int remaining(BlockPos pos, long now) {
        long packed = pos.asLong();
        if (!usedAt.containsKey(packed)) {
            return 0;
        }
        int left = CropGrowthRules.glowBerryMealRemaining(now, usedAt.get(packed));
        if (left <= 0) {
            usedAt.remove(packed);
            setDirty();
        }
        return Math.max(0, left);
    }

    private List<Long> positionList() {
        List<Long> list = new ArrayList<>(usedAt.size());
        usedAt.long2LongEntrySet().forEach(entry -> list.add(entry.getLongKey()));
        return list;
    }

    private List<Long> usedAtList() {
        List<Long> list = new ArrayList<>(usedAt.size());
        usedAt.long2LongEntrySet().forEach(entry -> list.add(entry.getLongValue()));
        return list;
    }
}
