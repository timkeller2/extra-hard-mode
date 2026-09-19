package dev.extrahardmode.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.extrahardmode.ExtraHardModeMod;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Biomes any player has visited. Stored on the overworld so every dimension
 * shares the same first-discovery board.
 */
public final class ExplorationData extends SavedData {
    public static final Codec<ExplorationData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING
                            .listOf()
                            .optionalFieldOf("biomes", List.of())
                            .forGetter(ExplorationData::biomeList))
            .apply(instance, ExplorationData::new));

    public static final SavedDataType<ExplorationData> TYPE = new SavedDataType<>(
            ExtraHardModeMod.id("exploration"), ExplorationData::new, CODEC, DataFixTypes.SAVED_DATA_MAP_INDEX);

    private final LinkedHashSet<String> biomes = new LinkedHashSet<>();

    public ExplorationData() {}

    public ExplorationData(List<String> biomes) {
        if (biomes != null) {
            for (String id : biomes) {
                if (id != null && !id.isEmpty()) {
                    this.biomes.add(id);
                }
            }
        }
    }

    public static ExplorationData of(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean visited(String biomeId) {
        return biomeId != null && biomes.contains(biomeId);
    }

    /** True when this call is the first visit by anyone. */
    public boolean tryVisit(String biomeId) {
        if (biomeId == null || biomeId.isEmpty()) {
            return false;
        }
        if (!biomes.add(biomeId)) {
            return false;
        }
        setDirty();
        return true;
    }

    public Set<String> snapshot() {
        return Set.copyOf(biomes);
    }

    private List<String> biomeList() {
        return new ArrayList<>(biomes);
    }
}
