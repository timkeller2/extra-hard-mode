package dev.extrahardmode.world;

import com.mojang.serialization.Codec;
import dev.extrahardmode.ExtraHardModeMod;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Overworld SavedData listing dimensions that have already received first-apply. */
public final class ExtraHardModeBootData extends SavedData {
    public static final Codec<ExtraHardModeBootData> CODEC = Identifier.CODEC
            .listOf()
            .fieldOf("applied")
            .xmap(ExtraHardModeBootData::new, ExtraHardModeBootData::appliedList)
            .codec();

    public static final SavedDataType<ExtraHardModeBootData> TYPE = new SavedDataType<>(
            ExtraHardModeMod.id("boot"), ExtraHardModeBootData::new, CODEC, DataFixTypes.SAVED_DATA_MAP_INDEX);

    private final Set<Identifier> applied = new LinkedHashSet<>();

    public ExtraHardModeBootData() {}

    public ExtraHardModeBootData(List<Identifier> applied) {
        this.applied.addAll(applied);
    }

    public boolean contains(Identifier dimensionId) {
        return applied.contains(dimensionId);
    }

    public void markApplied(Identifier dimensionId) {
        if (applied.add(dimensionId)) {
            setDirty();
        }
    }

    public List<Identifier> appliedList() {
        return new ArrayList<>(applied);
    }
}
