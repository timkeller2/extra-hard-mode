package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.world.EhmTags;
import dev.extrahardmode.world.WorldGate;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

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

    public static ClientboundSyncPayload inactive() {
        return new ClientboundSyncPayload(
                false, false, 0, false, List.of(), List.of(), List.of(), List.of(), List.of());
    }

    public static ClientboundSyncPayload from(ServerLevel level) {
        if (!WorldGate.isActive(level)) {
            return inactive();
        }
        WorldConfig config = ConfigManager.world(level);
        boolean torches = WorldGate.isModuleActive(level, ExtraHardModeMod.id("torches"));
        boolean building = WorldGate.isModuleActive(level, ExtraHardModeMod.id("limited_building"));
        return new ClientboundSyncPayload(
                building && config.limitedBuilding(),
                torches && config.torchSoftDeny(),
                config.torchNoPlacementUnderY(),
                torches && config.torchYDeny(),
                List.of(),
                List.of(),
                List.of(),
                torches ? EhmTags.snapshot(EhmTags.SOFT_TORCH_SURFACES) : List.of(),
                torches ? EhmTags.snapshot(EhmTags.DEPTH_LIMITED_LIGHTS) : List.of());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
