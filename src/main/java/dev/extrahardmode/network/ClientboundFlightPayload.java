package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClientboundFlightPayload(int remainingTicks, int maxTicks) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundFlightPayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("flight"));

    public static final StreamCodec<ByteBuf, ClientboundFlightPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ClientboundFlightPayload::remainingTicks,
            ByteBufCodecs.VAR_INT,
            ClientboundFlightPayload::maxTicks,
            ClientboundFlightPayload::new);

    public static final ClientboundFlightPayload INACTIVE = new ClientboundFlightPayload(0, 0);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
