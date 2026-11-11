package dev.extrahardmode.feature;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;

/**
 * Saves a council bounty. The sixteen original fields stay in one group, and dimension is stored
 * beside them so older saves that omit it still load.
 */
public final class CouncilMissionCodec {
    private static final Codec<CouncilMissionRules.Mission> FIELDS = RecordCodecBuilder.create(
            instance -> instance.group(
                            Codec.STRING.optionalFieldOf("mob_id", "").forGetter(CouncilMissionRules.Mission::mobId),
                            Codec.STRING.optionalFieldOf("label", "").forGetter(CouncilMissionRules.Mission::label),
                            Codec.INT.optionalFieldOf("hp", 0).forGetter(CouncilMissionRules.Mission::hp),
                            Codec.INT.optionalFieldOf("target", 0).forGetter(CouncilMissionRules.Mission::target),
                            Codec.INT.optionalFieldOf("kills", 0).forGetter(CouncilMissionRules.Mission::kills),
                            Codec.LONG
                                    .optionalFieldOf("assigned_day", -1L)
                                    .forGetter(CouncilMissionRules.Mission::assignedDay),
                            Codec.INT
                                    .optionalFieldOf("house_score", 0)
                                    .forGetter(CouncilMissionRules.Mission::houseScore),
                            Codec.BOOL
                                    .optionalFieldOf("completed", false)
                                    .forGetter(CouncilMissionRules.Mission::completed),
                            Codec.INT
                                    .optionalFieldOf("awarded_xp", 0)
                                    .forGetter(CouncilMissionRules.Mission::awardedXp),
                            Codec.INT
                                    .optionalFieldOf("pending_emeralds", 0)
                                    .forGetter(CouncilMissionRules.Mission::pendingEmeralds),
                            Codec.INT
                                    .optionalFieldOf("last_completed_target", 0)
                                    .forGetter(CouncilMissionRules.Mission::lastCompletedTarget),
                            Codec.INT
                                    .optionalFieldOf("completed_count", 0)
                                    .forGetter(CouncilMissionRules.Mission::completedCount),
                            Codec.STRING
                                    .optionalFieldOf("giver_id", "")
                                    .forGetter(CouncilMissionRules.Mission::giverId),
                            Codec.STRING
                                    .optionalFieldOf("giver_name", "")
                                    .forGetter(CouncilMissionRules.Mission::giverName),
                            Codec.LONG
                                    .optionalFieldOf("spawn_count_day", -1L)
                                    .forGetter(CouncilMissionRules.Mission::spawnCountDay),
                            Codec.INT
                                    .optionalFieldOf("spawn_replacements", 0)
                                    .forGetter(CouncilMissionRules.Mission::spawnReplacements))
                    .apply(instance, CouncilMissionRules::stored));

    public static final Codec<CouncilMissionRules.Mission> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<CouncilMissionRules.Mission, T>> decode(DynamicOps<T> ops, T input) {
            return FIELDS.decode(ops, input)
                    .flatMap(pair -> readDimension(ops, input, pair))
                    .flatMap(pair -> readAccepted(ops, input, pair));
        }

        @Override
        public <T> DataResult<T> encode(CouncilMissionRules.Mission input, DynamicOps<T> ops, T prefix) {
            return FIELDS.encode(input, ops, prefix).flatMap(encoded -> {
                DataResult<T> written = writeDimension(ops, encoded, input.dimension());
                if (input.accepted()) {
                    return written;
                }
                return written.flatMap(map -> ops.mergeToMap(
                        map, ops.createString("accepted"), ops.createBoolean(false)));
            });
        }
    };

    private static <T> DataResult<Pair<CouncilMissionRules.Mission, T>> readDimension(
            DynamicOps<T> ops, T input, Pair<CouncilMissionRules.Mission, T> pair) {
        Optional<T> raw = ops.get(input, "dimension").result();
        if (raw.isEmpty()) {
            return DataResult.success(pair);
        }
        return ops.getStringValue(raw.get())
                .map(dimension -> Pair.of(pair.getFirst().withDimension(dimension), pair.getSecond()));
    }

    /** Missing means the bounty was already given. Older saves have no accepted field. */
    private static <T> DataResult<Pair<CouncilMissionRules.Mission, T>> readAccepted(
            DynamicOps<T> ops, T input, Pair<CouncilMissionRules.Mission, T> pair) {
        Optional<T> raw = ops.get(input, "accepted").result();
        if (raw.isEmpty()) {
            return DataResult.success(pair);
        }
        return ops.getBooleanValue(raw.get())
                .map(accepted -> Pair.of(pair.getFirst().withAccepted(accepted), pair.getSecond()));
    }

    private static <T> DataResult<T> writeDimension(DynamicOps<T> ops, T encoded, String dimension) {
        if (dimension == null || dimension.isEmpty()) {
            return DataResult.success(encoded);
        }
        return ops.mergeToMap(encoded, ops.createString("dimension"), ops.createString(dimension));
    }

    private CouncilMissionCodec() {}
}
