package dev.extrahardmode.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.feature.BeeHoneyRules;
import dev.extrahardmode.feature.CropGrowthRules;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Fill percent for each hive, counted once per Minecraft day. A later bee that
 * returns the same day reuses the stored rate instead of scanning flowers again.
 */
public final class BeeHoneyData extends SavedData {
    public static final Codec<BeeHoneyData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.LONG.listOf().optionalFieldOf("positions", List.of()).forGetter(BeeHoneyData::positionList),
                    Codec.LONG.listOf().optionalFieldOf("days", List.of()).forGetter(BeeHoneyData::dayList),
                    Codec.INT.listOf().optionalFieldOf("percents", List.of()).forGetter(BeeHoneyData::percentList))
            .apply(instance, BeeHoneyData::new));

    public static final SavedDataType<BeeHoneyData> TYPE = new SavedDataType<>(
            ExtraHardModeMod.id("bee_honey"), BeeHoneyData::new, CODEC, DataFixTypes.SAVED_DATA_MAP_INDEX);

    private final Long2LongOpenHashMap days = new Long2LongOpenHashMap();
    private final Long2IntOpenHashMap percents = new Long2IntOpenHashMap();

    {
        days.defaultReturnValue(-1L);
        percents.defaultReturnValue(0);
    }

    public BeeHoneyData() {}

    public BeeHoneyData(List<Long> positions, List<Long> dayValues, List<Integer> percentValues) {
        int size = Math.min(positions.size(), Math.min(dayValues.size(), percentValues.size()));
        for (int i = 0; i < size; i++) {
            int percent = percentValues.get(i);
            long day = dayValues.get(i);
            if (percent <= 0 || day < 0L) {
                continue;
            }
            long packed = positions.get(i);
            days.put(packed, day);
            percents.put(packed, percent);
        }
    }

    public static BeeHoneyData of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /**
     * Percent chance this delivery fills the hive. {@code basePercent} of 0 never
     * fills, and 100 or more always does. Any other base is combined with nearby
     * flowers on the first return of the Minecraft day and reused after that.
     */
    public int rate(ServerLevel level, BlockPos pos, int basePercent) {
        if (basePercent <= 0) {
            return 0;
        }
        if (basePercent >= 100) {
            return 100;
        }
        long day = CropGrowthRules.dayIndex(level.getOverworldClockTime());
        long packed = pos.asLong();
        if (days.get(packed) == day) {
            int stored = percents.get(packed);
            if (stored > 0) {
                return stored;
            }
        }
        int percent = BeeHoneyRules.fillPercent(basePercent, flowerCounts(level, pos));
        remember(packed, day, percent);
        return percent;
    }

    /**
     * Fill percent a clock shows for this hive. A rate already stored today is reused.
     * Otherwise the flowers are counted and nothing is stored.
     */
    public int reportedRate(ServerLevel level, BlockPos pos, int basePercent) {
        if (basePercent <= 0 || basePercent >= 100) {
            return BeeHoneyRules.reportedPercent(basePercent, false, 0, null);
        }
        long day = CropGrowthRules.dayIndex(level.getOverworldClockTime());
        long packed = pos.asLong();
        if (days.get(packed) == day) {
            int stored = percents.get(packed);
            if (stored > 0) {
                return BeeHoneyRules.reportedPercent(basePercent, true, stored, null);
            }
        }
        return BeeHoneyRules.reportedPercent(basePercent, false, 0, flowerCounts(level, pos));
    }

    private void remember(long packed, long day, int percent) {
        days.put(packed, day);
        percents.put(packed, Math.min(100, Math.max(1, percent)));
        long oldest = day - 1L;
        LongArrayList drop = new LongArrayList();
        days.long2LongEntrySet().forEach(entry -> {
            if (entry.getLongValue() < oldest) {
                drop.add(entry.getLongKey());
            }
        });
        for (int i = 0; i < drop.size(); i++) {
            long key = drop.getLong(i);
            days.remove(key);
            percents.remove(key);
        }
        setDirty();
    }

    /** Flower counts inside harvest range. Unloaded chunks are skipped and not loaded. */
    private static int[] flowerCounts(ServerLevel level, BlockPos origin) {
        int radius = BeeHoneyRules.HARVEST_RANGE;
        int radiusSq = radius * radius;
        int minY = level.getMinY();
        int maxY = level.getMaxY() - 1;
        int originX = origin.getX();
        int originY = origin.getY();
        int originZ = origin.getZ();
        Reference2IntOpenHashMap<Block> counts = new Reference2IntOpenHashMap<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int loadedChunkX = Integer.MIN_VALUE;
        int loadedChunkZ = Integer.MIN_VALUE;
        boolean chunkLoaded = false;
        for (int dx = -radius; dx <= radius; dx++) {
            int x = originX + dx;
            int chunkX = x >> 4;
            for (int dz = -radius; dz <= radius; dz++) {
                int horizontal = dx * dx + dz * dz;
                if (horizontal > radiusSq) {
                    continue;
                }
                int z = originZ + dz;
                int chunkZ = z >> 4;
                if (chunkX != loadedChunkX || chunkZ != loadedChunkZ) {
                    loadedChunkX = chunkX;
                    loadedChunkZ = chunkZ;
                    chunkLoaded = level.hasChunk(chunkX, chunkZ);
                }
                if (!chunkLoaded) {
                    continue;
                }
                int yReach = (int) Math.sqrt(radiusSq - horizontal);
                int y0 = Math.max(minY, originY - yReach);
                int y1 = Math.min(maxY, originY + yReach);
                for (int y = y0; y <= y1; y++) {
                    BlockState state = level.getBlockState(cursor.set(x, y, z));
                    if (!countsAsHarvestFlower(state)) {
                        continue;
                    }
                    counts.addTo(state.getBlock(), 1);
                }
            }
        }
        if (counts.isEmpty()) {
            return new int[0];
        }
        int[] values = new int[counts.size()];
        int index = 0;
        for (var entry : counts.reference2IntEntrySet()) {
            values[index++] = entry.getIntValue();
        }
        return values;
    }

    private static boolean countsAsHarvestFlower(BlockState state) {
        if (!Bee.attractsBees(state)) {
            return false;
        }
        if (!(state.getBlock() instanceof DoublePlantBlock)) {
            return true;
        }
        boolean upper = state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER;
        return BeeHoneyRules.countsHalf(state.is(Blocks.SUNFLOWER), true, upper);
    }

    private List<Long> positionList() {
        List<Long> list = new ArrayList<>(days.size());
        days.long2LongEntrySet().forEach(entry -> list.add(entry.getLongKey()));
        return list;
    }

    private List<Long> dayList() {
        List<Long> list = new ArrayList<>(days.size());
        days.long2LongEntrySet().forEach(entry -> list.add(entry.getLongValue()));
        return list;
    }

    private List<Integer> percentList() {
        List<Integer> list = new ArrayList<>(days.size());
        days.long2LongEntrySet().forEach(entry -> list.add(percents.get(entry.getLongKey())));
        return list;
    }
}
