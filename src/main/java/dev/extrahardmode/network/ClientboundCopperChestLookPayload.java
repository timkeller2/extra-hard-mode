package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Days until a looked-at copper chest becomes public. Hidden when {@code visible} is false.
 * {@code label} is the finished line, such as {@code Public in 87 days}.
 */
public record ClientboundCopperChestLookPayload(boolean visible, String label) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundCopperChestLookPayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("copper_chest_look"));

    public static final StreamCodec<ByteBuf, ClientboundCopperChestLookPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ClientboundCopperChestLookPayload::visible,
            ByteBufCodecs.stringUtf8(64),
            ClientboundCopperChestLookPayload::label,
            ClientboundCopperChestLookPayload::new);

    public static final ClientboundCopperChestLookPayload HIDDEN = new ClientboundCopperChestLookPayload(false, "");

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
