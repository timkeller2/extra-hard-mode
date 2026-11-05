package dev.extrahardmode.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.feature.BiomeBossesRules;
import dev.extrahardmode.feature.BossFamily;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Per-dimension living boss UUID, last-spawn time, and earlier spawn sites, plus the overworld
 * campaign defeat count. {@code appeared} stays so old saves still load. A family can appear
 * again 500 blocks or more from every earlier site of that family in this dimension.
 */
public final class BiomeBossData extends SavedData {
    public static final Codec<FamilyState> FAMILY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    UUIDUtil.CODEC.optionalFieldOf("living").forGetter(FamilyState::living),
                    Codec.LONG.optionalFieldOf("last_spawn_epoch_ms", 0L).forGetter(FamilyState::lastSpawnEpochMs))
            .apply(instance, FamilyState::new));

    public static final Codec<BiomeBossesRules.SpawnSite> SPAWN_SITE_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                            Codec.STRING
                                    .optionalFieldOf("dimension", "")
                                    .forGetter(BiomeBossesRules.SpawnSite::dimension),
                            Codec.INT.optionalFieldOf("x", 0).forGetter(BiomeBossesRules.SpawnSite::x),
                            Codec.INT.optionalFieldOf("y", 0).forGetter(BiomeBossesRules.SpawnSite::y),
                            Codec.INT.optionalFieldOf("z", 0).forGetter(BiomeBossesRules.SpawnSite::z))
                    .apply(instance, BiomeBossesRules.SpawnSite::new));

    public static final Codec<BiomeBossData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.unboundedMap(Codec.STRING, FAMILY_CODEC)
                            .optionalFieldOf("families", Map.of())
                            .forGetter(BiomeBossData::families),
                    Codec.INT.optionalFieldOf("defeated", 0).forGetter(BiomeBossData::defeated),
                    Codec.STRING.listOf().optionalFieldOf("appeared", List.of()).forGetter(BiomeBossData::appearedIds),
                    Codec.unboundedMap(Codec.STRING, SPAWN_SITE_CODEC.listOf())
                            .optionalFieldOf("spawns", Map.of())
                            .forGetter(BiomeBossData::spawnSites))
            .apply(instance, BiomeBossData::new));

    public static final SavedDataType<BiomeBossData> TYPE = new SavedDataType<>(
            ExtraHardModeMod.id("biome_bosses"), BiomeBossData::new, CODEC, DataFixTypes.SAVED_DATA_MAP_INDEX);

    private final Map<String, FamilyState> families = new LinkedHashMap<>();
    private final Set<String> appeared = new LinkedHashSet<>();
    private final Map<String, List<BiomeBossesRules.SpawnSite>> spawns = new LinkedHashMap<>();
    private int defeated;

    public BiomeBossData() {}

    public BiomeBossData(
            Map<String, FamilyState> families,
            int defeated,
            List<String> appeared,
            Map<String, List<BiomeBossesRules.SpawnSite>> spawns) {
        this.families.putAll(families);
        this.defeated = Math.max(0, defeated);
        if (appeared != null) {
            for (String id : appeared) {
                if (id != null && !id.isEmpty()) {
                    this.appeared.add(id);
                }
            }
        }
        if (spawns != null) {
            for (Map.Entry<String, List<BiomeBossesRules.SpawnSite>> entry : spawns.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isEmpty() || entry.getValue() == null) {
                    continue;
                }
                List<BiomeBossesRules.SpawnSite> copy = new ArrayList<>();
                for (BiomeBossesRules.SpawnSite site : entry.getValue()) {
                    if (site != null && site.dimension() != null && !site.dimension().isEmpty()) {
                        copy.add(site);
                    }
                }
                if (!copy.isEmpty()) {
                    this.spawns.put(entry.getKey(), copy);
                }
            }
        }
    }

    public static BiomeBossData of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /** Campaign defeat count lives on the overworld so every dimension shares it. */
    public static BiomeBossData campaign(ServerLevel level) {
        return of(level.getServer().overworld());
    }

    public Map<String, FamilyState> families() {
        return new LinkedHashMap<>(families);
    }

    public int defeated() {
        return defeated;
    }

    public int recordDefeat() {
        if (defeated < Integer.MAX_VALUE) {
            defeated++;
        }
        setDirty();
        return defeated;
    }

    public List<String> appearedIds() {
        return List.copyOf(appeared);
    }

    public boolean hasAppeared(BossFamily family) {
        return family != null && appeared.contains(family.id());
    }

    /** Kept so old saves still load. New spawns record a site instead of calling this. */
    public void markAppeared(BossFamily family) {
        if (family != null && appeared.add(family.id())) {
            setDirty();
        }
    }

    public FamilyState state(BossFamily family) {
        return families.getOrDefault(family.id(), FamilyState.EMPTY);
    }

    public boolean hasLiving(ServerLevel level, BossFamily family) {
        FamilyState state = state(family);
        if (state.living().isEmpty()) {
            return false;
        }
        UUID uuid = state.living().get();
        Entity entity = level.getEntityInAnyDimension(uuid);
        if (entity == null) {
            return true;
        }
        if (entity.isAlive()) {
            return true;
        }
        clearLiving(family);
        return false;
    }

    public void markSpawned(BossFamily family, UUID uuid, long epochMs) {
        families.put(family.id(), new FamilyState(Optional.of(uuid), epochMs));
        setDirty();
    }

    public Map<String, List<BiomeBossesRules.SpawnSite>> spawnSites() {
        Map<String, List<BiomeBossesRules.SpawnSite>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<BiomeBossesRules.SpawnSite>> entry : spawns.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return copy;
    }

    public List<BiomeBossesRules.SpawnSite> sitesFor(String familyId) {
        if (familyId == null) {
            return List.of();
        }
        List<BiomeBossesRules.SpawnSite> sites = spawns.get(familyId);
        return sites == null ? List.of() : List.copyOf(sites);
    }

    public boolean hasAnySite(BossFamily family) {
        if (family == null) {
            return false;
        }
        List<BiomeBossesRules.SpawnSite> sites = spawns.get(family.id());
        return sites != null && !sites.isEmpty();
    }

    /** Appends one spawn site. Later checks use horizontal distance in {@code dimension} only. */
    public void recordSpawn(BossFamily family, String dimension, int x, int y, int z) {
        if (family == null || dimension == null || dimension.isEmpty()) {
            return;
        }
        spawns.computeIfAbsent(family.id(), id -> new ArrayList<>())
                .add(new BiomeBossesRules.SpawnSite(dimension, x, y, z));
        setDirty();
    }

    public void clearLiving(BossFamily family) {
        FamilyState previous = state(family);
        if (previous.living().isEmpty()) {
            return;
        }
        families.put(family.id(), new FamilyState(Optional.empty(), previous.lastSpawnEpochMs()));
        setDirty();
    }

    public void onDeath(UUID uuid) {
        for (Map.Entry<String, FamilyState> entry : families.entrySet()) {
            Optional<UUID> living = entry.getValue().living();
            if (living.isPresent() && living.get().equals(uuid)) {
                families.put(entry.getKey(), new FamilyState(Optional.empty(), entry.getValue().lastSpawnEpochMs()));
                setDirty();
                return;
            }
        }
    }

    public record FamilyState(Optional<UUID> living, long lastSpawnEpochMs) {
        static final FamilyState EMPTY = new FamilyState(Optional.empty(), 0L);
    }
}
