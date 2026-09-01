package dev.extrahardmode.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.extrahardmode.ExtraHardModeMod;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Overworld SavedData for first-apply. {@code applied} is the set of dimension ids
 * already stamped; {@code enabled} is each dimension's enable flag. The gamerule is
 * not stored here (it is server-global).
 */
public final class ExtraHardModeBootData extends SavedData {
    public static final Codec<ExtraHardModeBootData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Identifier.CODEC.listOf().optionalFieldOf("applied", List.of()).forGetter(ExtraHardModeBootData::appliedList),
                    Codec.unboundedMap(Identifier.CODEC, Codec.BOOL)
                            .optionalFieldOf("enabled", Map.of())
                            .forGetter(ExtraHardModeBootData::enabledMap))
            .apply(instance, ExtraHardModeBootData::new));

    public static final SavedDataType<ExtraHardModeBootData> TYPE = new SavedDataType<>(
            ExtraHardModeMod.id("boot"), ExtraHardModeBootData::new, CODEC, DataFixTypes.SAVED_DATA_MAP_INDEX);

    private final Set<Identifier> applied = new LinkedHashSet<>();
    private final Map<Identifier, Boolean> enabled = new LinkedHashMap<>();

    public ExtraHardModeBootData() {}

    public ExtraHardModeBootData(List<Identifier> applied, Map<Identifier, Boolean> enabled) {
        this.applied.addAll(applied);
        this.enabled.putAll(enabled);
        for (Identifier id : this.applied) {
            this.enabled.putIfAbsent(id, Boolean.TRUE);
        }
    }

    public boolean contains(Identifier dimensionId) {
        return applied.contains(dimensionId);
    }

    /**
     * Per-dimension enable flag. Missing map entries for an applied id are treated
     * as true (legacy {@code applied}-only saves).
     */
    public boolean isDimensionEnabled(Identifier dimensionId) {
        Boolean value = enabled.get(dimensionId);
        if (value != null) {
            return value;
        }
        return applied.contains(dimensionId);
    }

    public void markApplied(Identifier dimensionId, boolean dimensionEnabled) {
        boolean changed = applied.add(dimensionId);
        Boolean previous = enabled.put(dimensionId, dimensionEnabled);
        if (changed || !Boolean.valueOf(dimensionEnabled).equals(previous)) {
            setDirty();
        }
    }

    public List<Identifier> appliedList() {
        return new ArrayList<>(applied);
    }

    public Map<Identifier, Boolean> enabledMap() {
        return new LinkedHashMap<>(enabled);
    }
}
