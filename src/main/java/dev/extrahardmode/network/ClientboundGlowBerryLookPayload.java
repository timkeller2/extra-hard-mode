package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Glow berry bone-meal cooldown under the crosshair. Hidden when {@code visible} is false. */
public record ClientboundGlowBerryLookPayload(boolean visible, int remainingTicks) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundGlowBerryLookPayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("glow_berry_look"));

    public static final StreamCodec<ByteBuf, ClientboundGlowBerryLookPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ClientboundGlowBerryLookPayload::visible,
            ByteBufCodecs.VAR_INT,
            ClientboundGlowBerryLookPayload::remainingTicks,
            ClientboundGlowBerryLookPayload::new);

    public static final ClientboundGlowBerryLookPayload HIDDEN = new ClientboundGlowBerryLookPayload(false, 0);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
