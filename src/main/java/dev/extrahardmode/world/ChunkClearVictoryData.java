package dev.extrahardmode.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.extrahardmode.ExtraHardModeMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** The alliance goal that already played the win. Stored on the overworld so it happens once per goal. */
public final class ChunkClearVictoryData extends SavedData {
    public static final Codec<ChunkClearVictoryData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.optionalFieldOf("won_goal", 0).forGetter(ChunkClearVictoryData::wonGoal))
            .apply(instance, ChunkClearVictoryData::new));

    public static final SavedDataType<ChunkClearVictoryData> TYPE = new SavedDataType<>(
            ExtraHardModeMod.id("chunk_clear_victory"),
            ChunkClearVictoryData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_MAP_INDEX);

    private int wonGoal;

    public ChunkClearVictoryData() {}

    public ChunkClearVictoryData(int wonGoal) {
        this.wonGoal = Math.max(0, wonGoal);
    }

    public static ChunkClearVictoryData of(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public int wonGoal() {
        return wonGoal;
    }

    public boolean alreadyWon(int goal) {
        return goal > 0 && wonGoal == goal;
    }

    public void markWon(int goal) {
        if (goal <= 0 || wonGoal == goal) {
            return;
        }
        wonGoal = goal;
        setDirty();
    }
}
