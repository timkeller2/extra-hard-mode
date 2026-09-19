package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClientboundPowerMinePayload(int remainingTicks, int maxTicks) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundPowerMinePayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("power_mine"));

    public static final StreamCodec<ByteBuf, ClientboundPowerMinePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ClientboundPowerMinePayload::remainingTicks,
            ByteBufCodecs.VAR_INT,
            ClientboundPowerMinePayload::maxTicks,
            ClientboundPowerMinePayload::new);

    public static final ClientboundPowerMinePayload INACTIVE = new ClientboundPowerMinePayload(0, 0);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
