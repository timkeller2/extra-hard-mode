package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.Level;

/** The local player's bed or respawn anchor. Absent when they have not set one. */
public record ClientboundCompassSpawnPayload(boolean set, GlobalPos pos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundCompassSpawnPayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("compass_spawn"));

    private static final GlobalPos UNUSED = GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO);

    public static final StreamCodec<ByteBuf, ClientboundCompassSpawnPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ClientboundCompassSpawnPayload::set,
            GlobalPos.STREAM_CODEC,
            ClientboundCompassSpawnPayload::pos,
            ClientboundCompassSpawnPayload::new);

    public static ClientboundCompassSpawnPayload cleared() {
        return new ClientboundCompassSpawnPayload(false, UNUSED);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
