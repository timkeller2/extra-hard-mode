package dev.extrahardmode.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.extrahardmode.ExtraHardModeMod;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Per-block soil crop-loss modifiers and last planted crop. */
public final class FarmlandData extends SavedData {
    public static final Codec<FarmlandData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.LONG.listOf().optionalFieldOf("positions", List.of()).forGetter(FarmlandData::positionList),
                    Codec.INT.listOf().optionalFieldOf("modifiers", List.of()).forGetter(FarmlandData::modifierList),
                    Codec.LONG
                            .listOf()
                            .optionalFieldOf("cropPositions", List.of())
                            .forGetter(FarmlandData::cropPositionList),
                    Codec.STRING.listOf().optionalFieldOf("cropIds", List.of()).forGetter(FarmlandData::cropIdList))
            .apply(instance, FarmlandData::new));

    public static final SavedDataType<FarmlandData> TYPE = new SavedDataType<>(
            ExtraHardModeMod.id("farmland"), FarmlandData::new, CODEC, DataFixTypes.SAVED_DATA_MAP_INDEX);

    private final Long2IntOpenHashMap modifiers = new Long2IntOpenHashMap();
    private final Long2ObjectOpenHashMap<String> lastCrops = new Long2ObjectOpenHashMap<>();

    {
        modifiers.defaultReturnValue(0);
    }

    public FarmlandData() {}

    public FarmlandData(List<Long> positions, List<Integer> values) {
        this(positions, values, List.of(), List.of());
    }

    public FarmlandData(List<Long> positions, List<Integer> values, List<Long> cropPositions, List<String> cropIds) {
        int size = Math.min(positions.size(), values.size());
        for (int i = 0; i < size; i++) {
            modifiers.put(positions.get(i).longValue(), values.get(i).intValue());
        }
        int crops = Math.min(cropPositions.size(), cropIds.size());
        for (int i = 0; i < crops; i++) {
            String id = cropIds.get(i);
            if (id != null && !id.isEmpty()) {
                lastCrops.put(cropPositions.get(i).longValue(), id);
            }
        }
    }

    public static FarmlandData of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public int modifier(BlockPos pos) {
        return modifiers.get(pos.asLong());
    }

    public boolean hasModifier(BlockPos pos) {
        return modifiers.containsKey(pos.asLong());
    }

    public void setModifier(BlockPos pos, int modifier) {
        modifiers.put(pos.asLong(), modifier);
        setDirty();
    }

    public void addModifier(BlockPos pos, int delta) {
        setModifier(pos, modifier(pos) + delta);
    }

    public String lastCrop(BlockPos pos) {
        return lastCrops.get(pos.asLong());
    }

    public void setLastCrop(BlockPos pos, String cropId) {
        if (cropId == null || cropId.isEmpty()) {
            return;
        }
        lastCrops.put(pos.asLong(), cropId);
        setDirty();
    }

    private List<Long> positionList() {
        List<Long> list = new ArrayList<>(modifiers.size());
        modifiers.long2IntEntrySet().forEach(entry -> list.add(entry.getLongKey()));
        return list;
    }

    private List<Integer> modifierList() {
        List<Integer> list = new ArrayList<>(modifiers.size());
        modifiers.long2IntEntrySet().forEach(entry -> list.add(entry.getIntValue()));
        return list;
    }

    private List<Long> cropPositionList() {
        List<Long> list = new ArrayList<>(lastCrops.size());
        lastCrops.long2ObjectEntrySet().forEach(entry -> list.add(entry.getLongKey()));
        return list;
    }

    private List<String> cropIdList() {
        List<String> list = new ArrayList<>(lastCrops.size());
        lastCrops.long2ObjectEntrySet().forEach(entry -> list.add(entry.getValue()));
        return list;
    }
}
