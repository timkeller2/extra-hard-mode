package dev.extrahardmode.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.extrahardmode.ExtraHardModeMod;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Occupied and empty homes in one dimension. */
public final class InhabitantData extends SavedData {
    public static final Codec<Home> HOME_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("id").forGetter(Home::id),
                    BlockPos.CODEC.fieldOf("bed").forGetter(Home::bed),
                    Codec.INT.optionalFieldOf("score", 0).forGetter(Home::score),
                    Codec.STRING.optionalFieldOf("specialty", "").forGetter(Home::specialty),
                    Codec.STRING.optionalFieldOf("name", "").forGetter(Home::name),
                    UUIDUtil.CODEC.optionalFieldOf("living").forGetter(Home::living),
                    Codec.LONG.optionalFieldOf("leave_after_day", -1L).forGetter(Home::leaveAfterDay),
                    Codec.LONG.optionalFieldOf("empty_until_day", -1L).forGetter(Home::emptyUntilDay),
                    Codec.BOOL.optionalFieldOf("uneasy", false).forGetter(Home::uneasy),
                    Codec.LONG.optionalFieldOf("last_restock_day", -1L).forGetter(Home::lastRestockDay),
                    Codec.LONG.optionalFieldOf("last_roll_day", -1L).forGetter(Home::lastRollDay))
            .apply(instance, Home::new));

    public static final Codec<InhabitantData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    HOME_CODEC.listOf().optionalFieldOf("homes", List.of()).forGetter(InhabitantData::homes),
                    Codec.LONG.optionalFieldOf("last_dawn_day", -1L).forGetter(InhabitantData::lastDawnDay),
                    Codec.LONG.optionalFieldOf("last_restock_day", -1L).forGetter(InhabitantData::lastRestockDay),
                    Codec.STRING
                            .listOf()
                            .optionalFieldOf("spawned_specialties", List.of())
                            .forGetter(InhabitantData::spawnedSpecialtyList))
            .apply(instance, InhabitantData::new));

    public static final SavedDataType<InhabitantData> TYPE = new SavedDataType<>(
            ExtraHardModeMod.id("inhabitants"), InhabitantData::new, CODEC, DataFixTypes.SAVED_DATA_MAP_INDEX);

    private final Map<String, Home> homes = new LinkedHashMap<>();
    private final LinkedHashSet<String> spawnedSpecialties = new LinkedHashSet<>();
    private long lastDawnDay = -1L;
    private long lastRestockDay = -1L;

    public InhabitantData() {}

    public InhabitantData(List<Home> homes, long lastDawnDay, long lastRestockDay, List<String> spawnedSpecialties) {
        for (Home home : homes) {
            this.homes.put(home.id(), home);
            rememberSpecialty(home.specialty());
        }
        this.lastDawnDay = lastDawnDay;
        this.lastRestockDay = lastRestockDay;
        if (spawnedSpecialties != null) {
            for (String type : spawnedSpecialties) {
                rememberSpecialty(type);
            }
        }
    }

    public static InhabitantData of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public List<Home> homes() {
        return new ArrayList<>(homes.values());
    }

    public long lastDawnDay() {
        return lastDawnDay;
    }

    public void setLastDawnDay(long day) {
        this.lastDawnDay = day;
        setDirty();
    }

    public long lastRestockDay() {
        return lastRestockDay;
    }

    public void setLastRestockDay(long day) {
        this.lastRestockDay = day;
        setDirty();
    }

    public Set<String> spawnedSpecialties() {
        return Set.copyOf(spawnedSpecialties);
    }

    public void markSpawned(String specialty) {
        if (rememberSpecialty(specialty)) {
            setDirty();
        }
    }

    private boolean rememberSpecialty(String specialty) {
        if (specialty == null || specialty.isEmpty()) {
            return false;
        }
        return spawnedSpecialties.add(specialty);
    }

    private List<String> spawnedSpecialtyList() {
        return new ArrayList<>(spawnedSpecialties);
    }

    public Home get(String id) {
        return homes.get(id);
    }

    public void put(Home home) {
        homes.put(home.id(), home);
        setDirty();
    }

    public void remove(String id) {
        if (homes.remove(id) != null) {
            setDirty();
        }
    }

    public Home byLiving(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        for (Home home : homes.values()) {
            if (home.living().isPresent() && home.living().get().equals(uuid)) {
                return home;
            }
        }
        return null;
    }

    public record Home(
            String id,
            BlockPos bed,
            int score,
            String specialty,
            String name,
            Optional<UUID> living,
            long leaveAfterDay,
            long emptyUntilDay,
            boolean uneasy,
            long lastRestockDay,
            long lastRollDay) {
        public Home withLiving(Optional<UUID> next) {
            return new Home(
                    id, bed, score, specialty, name, next, leaveAfterDay, emptyUntilDay, uneasy, lastRestockDay, lastRollDay);
        }

        public Home withScore(int next) {
            return new Home(
                    id, bed, next, specialty, name, living, leaveAfterDay, emptyUntilDay, uneasy, lastRestockDay, lastRollDay);
        }

        public Home withLeave(long day, boolean uneasy) {
            return new Home(
                    id, bed, score, specialty, name, living, day, emptyUntilDay, uneasy, lastRestockDay, lastRollDay);
        }

        public Home withEmptyUntil(long day) {
            return new Home(id, bed, score, specialty, name, Optional.empty(), -1L, day, false, lastRestockDay, lastRollDay);
        }

        public Home withLastRestock(long day) {
            return new Home(
                    id, bed, score, specialty, name, living, leaveAfterDay, emptyUntilDay, uneasy, day, lastRollDay);
        }

        public Home withLastRoll(long day) {
            return new Home(
                    id, bed, score, specialty, name, living, leaveAfterDay, emptyUntilDay, uneasy, lastRestockDay, day);
        }
    }
}
