package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Ask the client to scroll the credits, then return to the game. */
public record ClientboundCreditsPayload() implements CustomPacketPayload {
    public static final ClientboundCreditsPayload INSTANCE = new ClientboundCreditsPayload();

    public static final CustomPacketPayload.Type<ClientboundCreditsPayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("credits"));

    public static final StreamCodec<ByteBuf, ClientboundCreditsPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
