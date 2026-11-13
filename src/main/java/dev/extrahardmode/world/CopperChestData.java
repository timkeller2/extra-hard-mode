package dev.extrahardmode.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.feature.CopperChestRules;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Placed copper-chest claims. Stored on the overworld so a chest in any
 * dimension keeps its owner. Guest names are read from the sign, not saved here.
 */
public final class CopperChestData extends SavedData {
    public static final Codec<CopperChestData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.listOf().optionalFieldOf("chests", List.of()).forGetter(CopperChestData::chestList))
            .apply(instance, CopperChestData::new));

    public static final SavedDataType<CopperChestData> TYPE = new SavedDataType<>(
            ExtraHardModeMod.id("copper_chests"),
            CopperChestData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_MAP_INDEX);

    private final LinkedHashSet<String> chests = new LinkedHashSet<>();

    public CopperChestData() {}

    public CopperChestData(List<String> chests) {
        if (chests == null) {
            return;
        }
        for (String key : chests) {
            if (CopperChestRules.parse(key) != null) {
                this.chests.add(key);
            }
        }
    }

    public static CopperChestData of(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public CopperChestRules.Claim find(String dimension, int x, int y, int z) {
        for (String raw : chests) {
            CopperChestRules.Claim claim = CopperChestRules.parse(raw);
            if (claim != null && claim.samePos(dimension, x, y, z)) {
                return claim;
            }
        }
        return null;
    }

    /** Replaces any claim already stored at this position. */
    public void remember(CopperChestRules.Claim claim) {
        String key = CopperChestRules.key(claim);
        if (key == null) {
            return;
        }
        boolean removed = dropPos(claim.dimension(), claim.x(), claim.y(), claim.z(), key);
        if (chests.contains(key)) {
            if (removed) {
                setDirty();
            }
            return;
        }
        chests.add(key);
        setDirty();
    }

    public void forget(String dimension, int x, int y, int z) {
        if (dropPos(dimension, x, y, z, null)) {
            setDirty();
        }
    }

    private boolean dropPos(String dimension, int x, int y, int z, String keep) {
        if (dimension == null) {
            return false;
        }
        boolean removed = false;
        Iterator<String> it = chests.iterator();
        while (it.hasNext()) {
            String raw = it.next();
            if (raw.equals(keep)) {
                continue;
            }
            CopperChestRules.Claim claim = CopperChestRules.parse(raw);
            if (claim == null || claim.samePos(dimension, x, y, z)) {
                it.remove();
                removed = true;
            }
        }
        return removed;
    }

    private List<String> chestList() {
        return new ArrayList<>(chests);
    }
}
