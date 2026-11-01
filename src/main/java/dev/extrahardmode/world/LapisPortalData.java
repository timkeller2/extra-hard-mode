package dev.extrahardmode.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.feature.LapisPortalRules;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Booth locations for lapis portals. Stored on the overworld so a pair can
 * cross dimensions. The lapis count is read from the chest at travel time.
 */
public final class LapisPortalData extends SavedData {
    public static final Codec<LapisPortalData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.listOf().optionalFieldOf("portals", List.of()).forGetter(LapisPortalData::portalList))
            .apply(instance, LapisPortalData::new));

    public static final SavedDataType<LapisPortalData> TYPE = new SavedDataType<>(
            ExtraHardModeMod.id("lapis_portals"),
            LapisPortalData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_MAP_INDEX);

    private final LinkedHashSet<String> portals = new LinkedHashSet<>();

    public LapisPortalData() {}

    public LapisPortalData(List<String> portals) {
        if (portals == null) {
            return;
        }
        for (String key : portals) {
            if (LapisPortalRules.parse(key) != null) {
                this.portals.add(key);
            }
        }
    }

    public static LapisPortalData of(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public List<String> snapshot() {
        return List.copyOf(portals);
    }

    public void remember(String key) {
        LapisPortalRules.Place place = LapisPortalRules.parse(key);
        if (place == null) {
            return;
        }
        if (portals.contains(key)) {
            if (dropOtherFacings(key, place)) {
                setDirty();
            }
            return;
        }
        dropOtherFacings(key, place);
        portals.add(key);
        setDirty();
    }

    public void forget(String key) {
        if (key != null && portals.remove(key)) {
            setDirty();
        }
    }

    public void forgetPlace(String dimension, int x, int y, int z) {
        boolean removed = false;
        Iterator<String> it = portals.iterator();
        while (it.hasNext()) {
            LapisPortalRules.Place place = LapisPortalRules.parse(it.next());
            if (place != null && dimension.equals(place.dimension()) && place.x() == x && place.y() == y && place.z() == z) {
                it.remove();
                removed = true;
            }
        }
        if (removed) {
            setDirty();
        }
    }

    private boolean dropOtherFacings(String key, LapisPortalRules.Place place) {
        boolean removed = false;
        Iterator<String> it = portals.iterator();
        while (it.hasNext()) {
            String existing = it.next();
            if (existing.equals(key)) {
                continue;
            }
            LapisPortalRules.Place other = LapisPortalRules.parse(existing);
            if (other != null && other.samePlace(place)) {
                it.remove();
                removed = true;
            }
        }
        return removed;
    }

    private List<String> portalList() {
        return new ArrayList<>(portals);
    }
}
