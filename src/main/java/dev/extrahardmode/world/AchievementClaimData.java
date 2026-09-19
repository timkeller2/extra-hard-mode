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
 * First player to claim each builder/slayer tier. Stored on the overworld so
 * every dimension shares the same competitive board.
 */
public final class AchievementClaimData extends SavedData {
    public static final Codec<AchievementClaimData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING
                            .listOf()
                            .optionalFieldOf("claimed", List.of())
                            .forGetter(AchievementClaimData::claimedList))
            .apply(instance, AchievementClaimData::new));

    public static final SavedDataType<AchievementClaimData> TYPE = new SavedDataType<>(
            ExtraHardModeMod.id("achievement_claims"),
            AchievementClaimData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_MAP_INDEX);

    private final LinkedHashSet<String> claimed = new LinkedHashSet<>();

    public AchievementClaimData() {}

    public AchievementClaimData(List<String> claimed) {
        if (claimed != null) {
            for (String key : claimed) {
                if (key != null && !key.isEmpty()) {
                    this.claimed.add(key);
                }
            }
        }
    }

    public static AchievementClaimData of(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean isClaimed(String key) {
        return key != null && claimed.contains(key);
    }

    /** True when this call is the first claim. */
    public boolean tryClaim(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        if (!claimed.add(key)) {
            return false;
        }
        setDirty();
        return true;
    }

    public Set<String> snapshot() {
        return Set.copyOf(claimed);
    }

    private List<String> claimedList() {
        return new ArrayList<>(claimed);
    }
}
