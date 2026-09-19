package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Hoe look-at soil modifier. Hidden when {@code visible} is false. */
public record ClientboundSoilLookPayload(boolean visible, int displayed) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundSoilLookPayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("soil_look"));

    public static final StreamCodec<ByteBuf, ClientboundSoilLookPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ClientboundSoilLookPayload::visible,
            ByteBufCodecs.VAR_INT,
            ClientboundSoilLookPayload::displayed,
            ClientboundSoilLookPayload::new);

    public static final ClientboundSoilLookPayload HIDDEN = new ClientboundSoilLookPayload(false, 0);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
