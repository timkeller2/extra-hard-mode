package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * A biome-boss ray. {@code targetId} of -1 is a fixed end point. Otherwise the client draws
 * the beam to that entity and, when it is the local player, pulls them until {@code untilTick}.
 */
public record ClientboundBossRayPayload(int bossId, int targetId, double x, double y, double z, long untilTick)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundBossRayPayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("boss_ray"));

    public static final StreamCodec<ByteBuf, ClientboundBossRayPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ClientboundBossRayPayload::bossId,
            ByteBufCodecs.VAR_INT,
            ClientboundBossRayPayload::targetId,
            ByteBufCodecs.DOUBLE,
            ClientboundBossRayPayload::x,
            ByteBufCodecs.DOUBLE,
            ClientboundBossRayPayload::y,
            ByteBufCodecs.DOUBLE,
            ClientboundBossRayPayload::z,
            ByteBufCodecs.VAR_LONG,
            ClientboundBossRayPayload::untilTick,
            ClientboundBossRayPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
