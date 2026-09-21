package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.GlobalConfig;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.feature.AntiFarming;
import dev.extrahardmode.feature.Dragon;
import dev.extrahardmode.feature.HardenedStone;
import dev.extrahardmode.feature.TorchLifetimeRules;
import dev.extrahardmode.feature.monster.Horses;
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
import net.minecraft.world.level.Level;

public record ClientboundSyncPayload(
        boolean limitedBuilding,
        boolean torchSoftDeny,
        int torchNoPlacementUnderY,
        boolean torchYDeny,
        int torchBurnDays,
        boolean blockOreNextToStone,
        boolean playerBypass,
        boolean horseBlockChest,
        int horseBlockChestBelowY,
        List<Identifier> hardenedBlocks,
        List<Identifier> hardenedPicks,
        List<Identifier> caveInOres,
        List<Identifier> softTorchSurfaces,
        List<Identifier> depthLimitedLights,
        boolean noEndBuilding,
        DisplayExtras extras)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("sync"));

    private static final StreamCodec<ByteBuf, List<Identifier>> IDENTIFIERS =
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list());

    public static final StreamCodec<ByteBuf, ClientboundSyncPayload> STREAM_CODEC = StreamCodec.composite(
            Flags.STREAM_CODEC,
            ClientboundSyncPayload::flags,
            Lists.STREAM_CODEC,
            ClientboundSyncPayload::lists,
            DisplayExtras.STREAM_CODEC,
            ClientboundSyncPayload::extras,
            ClientboundSyncPayload::fromParts);

    public static ClientboundSyncPayload inactive() {
        return new ClientboundSyncPayload(
                false,
                false,
                0,
                false,
                TorchLifetimeRules.DEFAULT_DAYS,
                false,
                false,
                false,
                0,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                false,
                DisplayExtras.INACTIVE);
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
        GlobalConfig global = ConfigManager.global();
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
                        Identifier.withDefaultNamespace("deepslate"));
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
        boolean noEndBuilding = level.dimension() == Level.END
                && WorldGate.isModuleActive(level, Dragon.ID)
                && config.dragon().noBuilding();
        return new ClientboundSyncPayload(
                building && config.limitedBuilding(),
                torches && config.torchSoftDeny(),
                config.torchNoPlacementUnderY(),
                torches && config.torchYDeny(),
                config.torchBurnDays(),
                blockOreNextToStone,
                playerBypass,
                WorldGate.isModuleActive(level, Horses.ID) && config.horseBlockChest(),
                config.horseBlockChestBelowY(),
                hardenedBlocks,
                hardenedPicks,
                caveInOres,
                softTorch,
                depthLights,
                noEndBuilding,
                new DisplayExtras(
                        config.checkPermission(),
                        config.creativeBypasses(),
                        config.operatorsBypass(),
                        config.torchFizz(),
                        config.creeperTntWarning(),
                        global.enabledByDefault(),
                        global.debug(),
                        global.tutorialMaxShows(),
                        config.f3Enabled(),
                        new DisplayExtras.SeasonBits(
                                WorldGate.isModuleActive(level, AntiFarming.ID),
                                config.lossRate(),
                                config.changingSeasons())));
    }

    private Flags flags() {
        return new Flags(
                limitedBuilding,
                torchSoftDeny,
                torchNoPlacementUnderY,
                torchYDeny,
                torchBurnDays,
                blockOreNextToStone,
                playerBypass,
                horseBlockChest,
                horseBlockChestBelowY,
                noEndBuilding);
    }

    private Lists lists() {
        return new Lists(hardenedBlocks, hardenedPicks, caveInOres, softTorchSurfaces, depthLimitedLights);
    }

    private static ClientboundSyncPayload fromParts(Flags flags, Lists lists, DisplayExtras extras) {
        return new ClientboundSyncPayload(
                flags.limitedBuilding(),
                flags.torchSoftDeny(),
                flags.torchNoPlacementUnderY(),
                flags.torchYDeny(),
                flags.torchBurnDays(),
                flags.blockOreNextToStone(),
                flags.playerBypass(),
                flags.horseBlockChest(),
                flags.horseBlockChestBelowY(),
                lists.hardenedBlocks(),
                lists.hardenedPicks(),
                lists.caveInOres(),
                lists.softTorchSurfaces(),
                lists.depthLimitedLights(),
                flags.noEndBuilding(),
                extras);
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

    public record DisplayExtras(
            boolean checkPermission,
            boolean creativeBypasses,
            boolean operatorsBypass,
            boolean torchFizz,
            boolean creeperTntWarning,
            boolean enabledByDefault,
            boolean debug,
            int tutorialMaxShows,
            boolean f3Enabled,
            SeasonBits season) {
        static final DisplayExtras INACTIVE = new DisplayExtras(
                true,
                true,
                false,
                true,
                true,
                false,
                false,
                GlobalConfig.DEFAULT_TUTORIAL_MAX_SHOWS,
                true,
                SeasonBits.INACTIVE);

        static final StreamCodec<ByteBuf, DisplayExtras> STREAM_CODEC = StreamCodec.composite(
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
                ByteBufCodecs.BOOL,
                DisplayExtras::f3Enabled,
                SeasonBits.STREAM_CODEC,
                DisplayExtras::season,
                DisplayExtras::new);

        public record SeasonBits(boolean antiFarming, int lossRate, boolean changingSeasons) {
            static final SeasonBits INACTIVE = new SeasonBits(false, 25, true);

            static final StreamCodec<ByteBuf, SeasonBits> STREAM_CODEC = StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    SeasonBits::antiFarming,
                    ByteBufCodecs.VAR_INT,
                    SeasonBits::lossRate,
                    ByteBufCodecs.BOOL,
                    SeasonBits::changingSeasons,
                    SeasonBits::new);
        }
    }

    private record Flags(
            boolean limitedBuilding,
            boolean torchSoftDeny,
            int torchNoPlacementUnderY,
            boolean torchYDeny,
            int torchBurnDays,
            boolean blockOreNextToStone,
            boolean playerBypass,
            boolean horseBlockChest,
            int horseBlockChestBelowY,
            boolean noEndBuilding) {
        static final StreamCodec<ByteBuf, Flags> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL,
                Flags::limitedBuilding,
                ByteBufCodecs.BOOL,
                Flags::torchSoftDeny,
                ByteBufCodecs.VAR_INT,
                Flags::torchNoPlacementUnderY,
                ByteBufCodecs.BOOL,
                Flags::torchYDeny,
                ByteBufCodecs.VAR_INT,
                Flags::torchBurnDays,
                ByteBufCodecs.BOOL,
                Flags::blockOreNextToStone,
                ByteBufCodecs.BOOL,
                Flags::playerBypass,
                HorseBits.STREAM_CODEC,
                Flags::horse,
                ByteBufCodecs.BOOL,
                Flags::noEndBuilding,
                Flags::fromBits);

        private HorseBits horse() {
            return new HorseBits(horseBlockChest, horseBlockChestBelowY);
        }

        private static Flags fromBits(
                boolean limitedBuilding,
                boolean torchSoftDeny,
                int torchNoPlacementUnderY,
                boolean torchYDeny,
                int torchBurnDays,
                boolean blockOreNextToStone,
                boolean playerBypass,
                HorseBits horse,
                boolean noEndBuilding) {
            return new Flags(
                    limitedBuilding,
                    torchSoftDeny,
                    torchNoPlacementUnderY,
                    torchYDeny,
                    torchBurnDays,
                    blockOreNextToStone,
                    playerBypass,
                    horse.horseBlockChest(),
                    horse.horseBlockChestBelowY(),
                    noEndBuilding);
        }

        private record HorseBits(boolean horseBlockChest, int horseBlockChestBelowY) {
            static final StreamCodec<ByteBuf, HorseBits> STREAM_CODEC = StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    HorseBits::horseBlockChest,
                    ByteBufCodecs.VAR_INT,
                    HorseBits::horseBlockChestBelowY,
                    HorseBits::new);
        }
    }

    private record Lists(
            List<Identifier> hardenedBlocks,
            List<Identifier> hardenedPicks,
            List<Identifier> caveInOres,
            List<Identifier> softTorchSurfaces,
            List<Identifier> depthLimitedLights) {
        static final StreamCodec<ByteBuf, Lists> STREAM_CODEC = StreamCodec.composite(
                IDENTIFIERS,
                Lists::hardenedBlocks,
                IDENTIFIERS,
                Lists::hardenedPicks,
                IDENTIFIERS,
                Lists::caveInOres,
                IDENTIFIERS,
                Lists::softTorchSurfaces,
                IDENTIFIERS,
                Lists::depthLimitedLights,
                Lists::new);
    }
}
