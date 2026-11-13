package dev.extrahardmode.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.feature.HopperRules;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Placed hopper claims. Stored on the overworld so a hopper in any dimension
 * keeps its owner. Guest names are read from the sign, not saved here.
 */
public final class HopperData extends SavedData {
    public static final Codec<HopperData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.listOf().optionalFieldOf("hoppers", List.of()).forGetter(HopperData::hopperList))
            .apply(instance, HopperData::new));

    public static final SavedDataType<HopperData> TYPE = new SavedDataType<>(
            ExtraHardModeMod.id("owned_hoppers"), HopperData::new, CODEC, DataFixTypes.SAVED_DATA_MAP_INDEX);

    private final LinkedHashSet<String> hoppers = new LinkedHashSet<>();

    public HopperData() {}

    public HopperData(List<String> hoppers) {
        if (hoppers == null) {
            return;
        }
        for (String key : hoppers) {
            if (HopperRules.parse(key) != null) {
                this.hoppers.add(key);
            }
        }
    }

    public static HopperData of(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public HopperRules.Claim find(String dimension, int x, int y, int z) {
        for (String raw : hoppers) {
            HopperRules.Claim claim = HopperRules.parse(raw);
            if (claim != null && claim.samePos(dimension, x, y, z)) {
                return claim;
            }
        }
        return null;
    }

    /** Replaces any claim already stored at this position. */
    public void remember(HopperRules.Claim claim) {
        String key = HopperRules.key(claim);
        if (key == null) {
            return;
        }
        boolean removed = dropPos(claim.dimension(), claim.x(), claim.y(), claim.z(), key);
        if (hoppers.contains(key)) {
            if (removed) {
                setDirty();
            }
            return;
        }
        hoppers.add(key);
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
        Iterator<String> it = hoppers.iterator();
        while (it.hasNext()) {
            String raw = it.next();
            if (raw.equals(keep)) {
                continue;
            }
            HopperRules.Claim claim = HopperRules.parse(raw);
            if (claim == null || claim.samePos(dimension, x, y, z)) {
                it.remove();
                removed = true;
            }
        }
        return removed;
    }

    private List<String> hopperList() {
        return new ArrayList<>(hoppers);
    }
}
