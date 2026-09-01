package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.GlobalConfig;
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
        DisplayExtras extras)
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
            DisplayExtras.STREAM_CODEC,
            ClientboundSyncPayload::extras,
            ClientboundSyncPayload::new);

    public List<Identifier> depthLimitedLights() {
        return extras.depthLimitedLights();
    }

    public static ClientboundSyncPayload from(WorldConfig config) {
        GlobalConfig global = ConfigManager.global();
        return new ClientboundSyncPayload(
                config.limitedBuilding(),
                config.torchSoftDeny(),
                config.torchNoPlacementUnderY(),
                config.torchYDeny(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new DisplayExtras(
                        List.of(),
                        config.checkPermission(),
                        config.creativeBypasses(),
                        config.operatorsBypass(),
                        config.torchFizz(),
                        config.creeperTntWarning(),
                        global.enabledByDefault(),
                        global.debug(),
                        global.tutorialMaxShows()));
    }

    public record DisplayExtras(
            List<Identifier> depthLimitedLights,
            boolean checkPermission,
            boolean creativeBypasses,
            boolean operatorsBypass,
            boolean torchFizz,
            boolean creeperTntWarning,
            boolean enabledByDefault,
            boolean debug,
            int tutorialMaxShows) {
        static final StreamCodec<ByteBuf, DisplayExtras> STREAM_CODEC = StreamCodec.composite(
                IDENTIFIERS,
                DisplayExtras::depthLimitedLights,
                ByteBufCodecs.BOOL,
                DisplayExtras::checkPermission,
                ByteBufCodecs.BOOL,
                DisplayExtras::creativeBypasses,
                ByteBufCodecs.BOOL,
                DisplayExtras::operatorsBypass,
                ByteBufCodecs.BOOL,
                DisplayExtras::torchFizz,
                ByteBufCodecs.BOOL,
                DisplayExtras::creeperTntWarning,
                ByteBufCodecs.BOOL,
                DisplayExtras::enabledByDefault,
                ByteBufCodecs.BOOL,
                DisplayExtras::debug,
                ByteBufCodecs.VAR_INT,
                DisplayExtras::tutorialMaxShows,
                DisplayExtras::new);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
