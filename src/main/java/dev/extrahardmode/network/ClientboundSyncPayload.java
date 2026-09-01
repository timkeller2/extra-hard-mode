package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.feature.monster.Horses;
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
        boolean horseBlockChest,
        int horseBlockChestBelowY,
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
            ByteBufCodecs.BOOL,
            ClientboundSyncPayload::horseBlockChest,
            ByteBufCodecs.VAR_INT,
            ClientboundSyncPayload::horseBlockChestBelowY,
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

    public static ClientboundSyncPayload from(ServerLevel level) {
        WorldConfig config = ConfigManager.world(level);
        return new ClientboundSyncPayload(
                config.limitedBuilding(),
                config.torchSoftDeny(),
                config.torchNoPlacementUnderY(),
                config.torchYDeny(),
                WorldGate.isModuleActive(level, Horses.ID) && config.horseBlockChest(),
                config.horseBlockChestBelowY(),
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
