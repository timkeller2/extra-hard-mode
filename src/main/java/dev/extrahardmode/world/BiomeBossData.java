package dev.extrahardmode.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.feature.BossFamily;
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
 * Per-dimension living boss UUID and last-spawn time, plus overworld campaign defeat count.
 * A family id in {@code appeared} has spawned once in this game and does not return.
 */
public final class BiomeBossData extends SavedData {
    public static final Codec<FamilyState> FAMILY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    UUIDUtil.CODEC.optionalFieldOf("living").forGetter(FamilyState::living),
                    Codec.LONG.optionalFieldOf("last_spawn_epoch_ms", 0L).forGetter(FamilyState::lastSpawnEpochMs))
            .apply(instance, FamilyState::new));

    public static final Codec<BiomeBossData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.unboundedMap(Codec.STRING, FAMILY_CODEC)
                            .optionalFieldOf("families", Map.of())
                            .forGetter(BiomeBossData::families),
                    Codec.INT.optionalFieldOf("defeated", 0).forGetter(BiomeBossData::defeated),
                    Codec.STRING.listOf().optionalFieldOf("appeared", List.of()).forGetter(BiomeBossData::appearedIds))
            .apply(instance, BiomeBossData::new));

    public static final SavedDataType<BiomeBossData> TYPE = new SavedDataType<>(
            ExtraHardModeMod.id("biome_bosses"), BiomeBossData::new, CODEC, DataFixTypes.SAVED_DATA_MAP_INDEX);

    private final Map<String, FamilyState> families = new LinkedHashMap<>();
    private final Set<String> appeared = new LinkedHashSet<>();
    private int defeated;

    public BiomeBossData() {}

    public BiomeBossData(Map<String, FamilyState> families, int defeated, List<String> appeared) {
        this.families.putAll(families);
        this.defeated = Math.max(0, defeated);
        if (appeared != null) {
            for (String id : appeared) {
                if (id != null && !id.isEmpty()) {
                    this.appeared.add(id);
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

    /** Records that this family has had its one spawn. Safe to call more than once. */
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
