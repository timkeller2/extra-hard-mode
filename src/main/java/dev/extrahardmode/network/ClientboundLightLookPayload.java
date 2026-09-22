package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Look-at remaining torch, campfire, or resident restock time. Hidden when {@code visible} is false.
 * {@code remainingTicks} below 0 means permanent.
 */
public record ClientboundLightLookPayload(boolean visible, int remainingTicks) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundLightLookPayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("light_look"));

    public static final StreamCodec<ByteBuf, ClientboundLightLookPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ClientboundLightLookPayload::visible,
            ByteBufCodecs.VAR_INT,
            ClientboundLightLookPayload::remainingTicks,
            ClientboundLightLookPayload::new);

    public static final ClientboundLightLookPayload HIDDEN = new ClientboundLightLookPayload(false, 0);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
