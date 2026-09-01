package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.feature.HardenedStone;
import dev.extrahardmode.tag.EhmTags;
import dev.extrahardmode.world.WorldGate;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;

public record ClientboundSyncPayload(
        boolean limitedBuilding,
        boolean torchSoftDeny,
        int torchNoPlacementUnderY,
        boolean torchYDeny,
        boolean blockOreNextToStone,
        boolean playerBypass,
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
            ClientboundSyncPayload::blockOreNextToStone,
            ByteBufCodecs.BOOL,
            ClientboundSyncPayload::playerBypass,
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
        return from(null, config, false);
    }

    public static ClientboundSyncPayload from(ServerLevel level) {
        return from(level, ConfigManager.world(level), false);
    }

    /** Rebuild from live world + this player's bypass so the client cannot refuse what the server allows. */
    public static ClientboundSyncPayload from(ServerPlayer player) {
        ServerLevel level = player.level();
        return from(level, ConfigManager.world(level), EhmApi.playerBypasses(player));
    }

    public static ClientboundSyncPayload from(ServerLevel level, WorldConfig config, boolean playerBypass) {
        boolean hardened = level != null
                && WorldGate.isModuleActive(level, HardenedStone.ID)
                && config.hardenedEnable();
        List<Identifier> hardenedBlocks = List.of();
        List<Identifier> hardenedPicks = List.of();
        List<Identifier> caveInOres = List.of();
        boolean blockOreNextToStone = false;
        if (hardened) {
            hardenedBlocks = snapshot(level.registryAccess().lookupOrThrow(Registries.BLOCK), EhmTags.HARDENED);
            if (hardenedBlocks.isEmpty()) {
                hardenedBlocks = List.of(
                        Identifier.withDefaultNamespace("stone"),
                        Identifier.withDefaultNamespace("deepslate"),
                        Identifier.withDefaultNamespace("tuff"));
            }
            List<Identifier> picks = new ArrayList<>(
                    snapshot(level.registryAccess().lookupOrThrow(Registries.ITEM), EhmTags.HARDENED_MINER));
            for (Identifier id : config.hardenedBudgets().keySet()) {
                if (!picks.contains(id)) {
                    picks.add(id);
                }
            }
            hardenedPicks = List.copyOf(picks);
            caveInOres = snapshot(level.registryAccess().lookupOrThrow(Registries.BLOCK), EhmTags.CAVE_IN_ORES);
            blockOreNextToStone = config.blockOreNextToStone();
        }
        return new ClientboundSyncPayload(
                config.limitedBuilding(),
                config.torchSoftDeny(),
                config.torchNoPlacementUnderY(),
                config.torchYDeny(),
                blockOreNextToStone,
                playerBypass,
                hardenedBlocks,
                hardenedPicks,
                caveInOres,
                List.of(),
                List.of());
    }

    private static <T> List<Identifier> snapshot(HolderLookup.RegistryLookup<T> lookup, TagKey<T> tag) {
        return lookup.get(tag)
                .map(named -> named.stream()
                        .map(Holder::getRegisteredName)
                        .map(Identifier::parse)
                        .toList())
                .orElse(List.of());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
