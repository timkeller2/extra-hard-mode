package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClientboundToastPayload(String messageId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundToastPayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("toast"));

    public static final StreamCodec<ByteBuf, ClientboundToastPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ClientboundToastPayload::messageId, ClientboundToastPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
