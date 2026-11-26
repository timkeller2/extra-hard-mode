package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** The player pressed Auto-stack. The server decides what moves. */
public record ServerboundAutoStackPayload() implements CustomPacketPayload {
    public static final ServerboundAutoStackPayload INSTANCE = new ServerboundAutoStackPayload();

    public static final CustomPacketPayload.Type<ServerboundAutoStackPayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("auto_stack"));

    public static final StreamCodec<ByteBuf, ServerboundAutoStackPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
