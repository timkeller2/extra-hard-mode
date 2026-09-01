package dev.extrahardmode.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.MonsterConfig;
import dev.extrahardmode.player.EhmAttachments;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Marked EHM spider webs. Surface (Y ≥ 48) entries are cleaned; cave webs persist. Cap 2048/world. */
public final class SpiderWebData extends SavedData {
    public static final Codec<SpiderWebData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    EhmAttachments.LONG_SET_CODEC
                            .optionalFieldOf("positions", new LongOpenHashSet())
                            .forGetter(data -> new LongOpenHashSet(data.positions)))
            .apply(instance, SpiderWebData::new));

    public static final SavedDataType<SpiderWebData> TYPE = new SavedDataType<>(
            ExtraHardModeMod.id("spider_webs"), SpiderWebData::new, CODEC, DataFixTypes.SAVED_DATA_MAP_INDEX);

    private final LongLinkedOpenHashSet positions = new LongLinkedOpenHashSet();
    private final Long2LongOpenHashMap placedTick = new Long2LongOpenHashMap();

    {
        placedTick.defaultReturnValue(-1L);
    }

    public SpiderWebData() {}

    public SpiderWebData(LongOpenHashSet stored) {
        LongArrayList list = new LongArrayList(stored);
        for (int i = 0; i < list.size(); i++) {
            positions.add(list.getLong(i));
        }
        trim();
    }

    public static SpiderWebData of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public void add(BlockPos pos, long gameTime) {
        long packed = pos.asLong();
        if (!positions.add(packed)) {
            placedTick.put(packed, gameTime);
            return;
        }
        placedTick.put(packed, gameTime);
        trim();
        setDirty();
    }

    public boolean placedOnTick(BlockPos pos, long gameTime) {
        return placedTick.get(pos.asLong()) == gameTime;
    }

    public void remove(BlockPos pos) {
        long packed = pos.asLong();
        placedTick.remove(packed);
        if (positions.remove(packed)) {
            setDirty();
        }
    }

    public boolean contains(BlockPos pos) {
        return positions.contains(pos.asLong());
    }

    public List<BlockPos> snapshot() {
        List<BlockPos> list = new java.util.ArrayList<>(positions.size());
        positions.forEach((long packed) -> list.add(BlockPos.of(packed)));
        return list;
    }

    private void trim() {
        while (positions.size() > MonsterConfig.WEB_CAP) {
            long dropped = positions.removeFirstLong();
            placedTick.remove(dropped);
            setDirty();
        }
    }
}
