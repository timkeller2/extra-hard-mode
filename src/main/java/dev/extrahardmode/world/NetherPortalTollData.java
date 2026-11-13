package dev.extrahardmode.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.feature.NetherPortalRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** World counter for nether-portal backlash. Stored on the overworld. Starts at 0. */
public final class NetherPortalTollData extends SavedData {
    public static final Codec<NetherPortalTollData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.optionalFieldOf("counter", 0).forGetter(NetherPortalTollData::counter))
            .apply(instance, NetherPortalTollData::new));

    public static final SavedDataType<NetherPortalTollData> TYPE = new SavedDataType<>(
            ExtraHardModeMod.id("nether_portal_toll"),
            NetherPortalTollData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_MAP_INDEX);

    private int counter;

    public NetherPortalTollData() {}

    public NetherPortalTollData(int counter) {
        this.counter = Math.max(0, counter);
    }

    public static NetherPortalTollData of(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public int counter() {
        return counter;
    }

    /** One d20. Returns the damage, which is the counter after this roll. */
    public int applyRoll(int roll) {
        int next = NetherPortalRules.afterRoll(counter, roll);
        if (next != counter) {
            counter = next;
            setDirty();
        }
        return counter;
    }
}
