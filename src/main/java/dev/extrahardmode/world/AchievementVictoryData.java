package dev.extrahardmode.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.extrahardmode.ExtraHardModeMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** The first player to win by achievements. Stored on the overworld so it happens once. */
public final class AchievementVictoryData extends SavedData {
    public static final Codec<AchievementVictoryData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.optionalFieldOf("winner", "").forGetter(AchievementVictoryData::winner))
            .apply(instance, AchievementVictoryData::new));

    public static final SavedDataType<AchievementVictoryData> TYPE = new SavedDataType<>(
            ExtraHardModeMod.id("achievement_victory"),
            AchievementVictoryData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_MAP_INDEX);

    private String winner = "";

    public AchievementVictoryData() {}

    public AchievementVictoryData(String winner) {
        this.winner = winner == null ? "" : winner;
    }

    public static AchievementVictoryData of(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean won() {
        return !winner.isEmpty();
    }

    public String winner() {
        return winner;
    }

    /** True when this call records the winner. */
    public boolean tryWin(String uuid) {
        if (uuid == null || uuid.isEmpty() || won()) {
            return false;
        }
        winner = uuid;
        setDirty();
        return true;
    }
}
