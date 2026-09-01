package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.WorldConfig;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ClientboundSyncPayload(
        boolean limitedBuilding,
        boolean torchSoftDeny,
        int torchNoPlacementUnderY,
        boolean torchYDeny,
        List<Identifier> hardenedBlocks,
        List<Identifier> hardenedPicks,
        List<Identifier> caveInOres,
        List<Identifier> softTorchSurfaces,
        List<Identifier> depthLimitedLights)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("sync"));

    private static final StreamCodec<ByteBuf, List<Identifier>> IDENTIFIERS =
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list());

    public static final StreamCodec<ByteBuf, ClientboundSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ClientboundSyncPayload::limitedBuilding,
            ByteBufCodecs.BOOL,
            ClientboundSyncPayload::torchSoftDeny,
            ByteBufCodecs.VAR_INT,
            ClientboundSyncPayload::torchNoPlacementUnderY,
            ByteBufCodecs.BOOL,
            ClientboundSyncPayload::torchYDeny,
            IDENTIFIERS,
            ClientboundSyncPayload::hardenedBlocks,
            IDENTIFIERS,
            ClientboundSyncPayload::hardenedPicks,
            IDENTIFIERS,
            ClientboundSyncPayload::caveInOres,
            IDENTIFIERS,
            ClientboundSyncPayload::softTorchSurfaces,
            IDENTIFIERS,
            ClientboundSyncPayload::depthLimitedLights,
            ClientboundSyncPayload::new);

    public static ClientboundSyncPayload from(WorldConfig config) {
        return new ClientboundSyncPayload(
                config.limitedBuilding(),
                config.torchSoftDeny(),
                config.torchNoPlacementUnderY(),
                config.torchYDeny(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
