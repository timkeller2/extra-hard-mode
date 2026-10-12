package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** World position and actions left for one achievement floater. */
public record ClientboundAchievementFloatPayload(double x, double y, double z, int remaining)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundAchievementFloatPayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("achievement_float"));

    public static final StreamCodec<ByteBuf, ClientboundAchievementFloatPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE,
            ClientboundAchievementFloatPayload::x,
            ByteBufCodecs.DOUBLE,
            ClientboundAchievementFloatPayload::y,
            ByteBufCodecs.DOUBLE,
            ClientboundAchievementFloatPayload::z,
            ByteBufCodecs.VAR_INT,
            ClientboundAchievementFloatPayload::remaining,
            ClientboundAchievementFloatPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
