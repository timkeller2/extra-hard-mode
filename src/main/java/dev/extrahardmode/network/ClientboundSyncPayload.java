package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.GlobalConfig;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.feature.Dragon;
import dev.extrahardmode.feature.HardenedStone;
import dev.extrahardmode.tag.EhmTags;
import dev.extrahardmode.feature.monster.Horses;
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
import net.minecraft.world.level.Level;

public record ClientboundSyncPayload(
        boolean limitedBuilding,
        boolean torchSoftDeny,
        int torchNoPlacementUnderY,
        boolean torchYDeny,
        boolean blockOreNextToStone,
        boolean playerBypass,
        boolean horseBlockChest,
        int horseBlockChestBelowY,
        List<Identifier> hardenedBlocks,
        List<Identifier> hardenedPicks,
        List<Identifier> caveInOres,
        List<Identifier> softTorchSurfaces,
        List<Identifier> depthLimitedLights,
        boolean noEndBuilding)
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
            ByteBufCodecs.BOOL,
            ClientboundSyncPayload::blockOreNextToStone,
            ByteBufCodecs.BOOL,
            ClientboundSyncPayload::playerBypass,
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
            ByteBufCodecs.BOOL,
            ClientboundSyncPayload::noEndBuilding,
            ClientboundSyncPayload::new);

    public static ClientboundSyncPayload inactive() {
    public static ClientboundSyncPayload from(ServerLevel level) {
        WorldConfig config = ConfigManager.world(level);
            DisplayExtras.STREAM_CODEC,
            ClientboundSyncPayload::extras,
    public List<Identifier> depthLimitedLights() {
        return extras.depthLimitedLights();
    }
    public static ClientboundSyncPayload from(WorldConfig config) {
        GlobalConfig global = ConfigManager.global();
        return new ClientboundSyncPayload(
                false, false, 0, false, false, false, List.of(), List.of(), List.of(), List.of(), List.of());
    }

    public static ClientboundSyncPayload from(ServerLevel level) {
        return from(level, ConfigManager.world(level), false);
    }

    public static ClientboundSyncPayload from(ServerPlayer player) {
        ServerLevel level = player.level();
        return from(level, ConfigManager.world(level), EhmApi.playerBypasses(player));
    }

    public static ClientboundSyncPayload from(ServerLevel level, WorldConfig config, boolean playerBypass) {
        if (level == null || !WorldGate.isActive(level)) {
            return inactive();
        }
        boolean torches = WorldGate.isModuleActive(level, ExtraHardModeMod.id("torches"));
        boolean building = WorldGate.isModuleActive(level, ExtraHardModeMod.id("limited_building"));
        boolean hardened = WorldGate.isModuleActive(level, HardenedStone.ID) && config.hardenedEnable();
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
        List<Identifier> softTorch = List.of();
        List<Identifier> depthLights = List.of();
        if (torches) {
            softTorch = snapshot(level.registryAccess().lookupOrThrow(Registries.BLOCK), EhmTags.SOFT_TORCH_SURFACES);
            depthLights = snapshot(level.registryAccess().lookupOrThrow(Registries.BLOCK), EhmTags.DEPTH_LIMITED_LIGHTS);
        }
        return new ClientboundSyncPayload(
                building && config.limitedBuilding(),
                torches && config.torchSoftDeny(),
                config.torchNoPlacementUnderY(),
                torches && config.torchYDeny(),
                blockOreNextToStone,
                playerBypass,
                hardenedBlocks,
                hardenedPicks,
                caveInOres,
                softTorch,
                depthLights);
                List.of(),
                level != null
                        && level.dimension() == Level.END
                        && WorldGate.isModuleActive(level, Dragon.ID)
                        && config.dragon().noBuilding());
    }

    private static <T> List<Identifier> snapshot(HolderLookup.RegistryLookup<T> lookup, TagKey<T> tag) {
        return lookup.get(tag)
                .map(named -> named.stream()
                        .map(Holder::getRegisteredName)
                        .map(Identifier::parse)
                        .toList())
                .orElse(List.of());
                config.torchYDeny(),
                WorldGate.isModuleActive(level, Horses.ID) && config.horseBlockChest(),
                config.horseBlockChestBelowY(),
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
