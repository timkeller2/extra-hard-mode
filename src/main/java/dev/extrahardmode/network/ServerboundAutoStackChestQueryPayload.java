package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** The client opened a chest screen and needs that chest's excluded slots. */
public record ServerboundAutoStackChestQueryPayload() implements CustomPacketPayload {
    public static final ServerboundAutoStackChestQueryPayload INSTANCE = new ServerboundAutoStackChestQueryPayload();

    public static final CustomPacketPayload.Type<ServerboundAutoStackChestQueryPayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("auto_stack_chest_query"));

    public static final StreamCodec<ByteBuf, ServerboundAutoStackChestQueryPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
