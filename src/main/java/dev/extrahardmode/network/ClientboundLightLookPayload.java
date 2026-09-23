package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Look-at torch or campfire time, or a resident's restock time and hearts.
 * Hidden when {@code visible} is false. {@code remainingTicks} below 0 means no duration line.
 * {@code hearts} is one icon per 10 health, or 0 when this is not a resident.
 */
public record ClientboundLightLookPayload(boolean visible, int remainingTicks, int hearts)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundLightLookPayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("light_look"));

    public static final StreamCodec<ByteBuf, ClientboundLightLookPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ClientboundLightLookPayload::visible,
            ByteBufCodecs.VAR_INT,
            ClientboundLightLookPayload::remainingTicks,
            ByteBufCodecs.VAR_INT,
            ClientboundLightLookPayload::hearts,
            ClientboundLightLookPayload::new);

    public static final ClientboundLightLookPayload HIDDEN = new ClientboundLightLookPayload(false, -1, 0);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
