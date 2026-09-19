package dev.extrahardmode.client;

import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.GlobalConfig;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.feature.TorchLifetimeRules;
import dev.extrahardmode.network.ClientboundSyncPayload;
import dev.extrahardmode.network.ServerboundConfigPayload;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Optional Cloth Config screen. Classloaded only when Cloth is present.
 * Dedicated servers never load this client class. World/global gameplay
 * settings are applied on the logical server via {@link ServerboundConfigPayload}.
 */
public final class ClothConfigScreen {
    private ClothConfigScreen() {}

    public static boolean available() {
        return FabricLoader.getInstance().isModLoaded("cloth-config")
                || FabricLoader.getInstance().isModLoaded("cloth-config2");
    }

    public static Screen create(Screen parent) {
        Seed seed = Seed.capture();
        boolean[] enabledByDefault = {seed.enabledByDefault};
        boolean[] debug = {seed.debug};
        int[] tutorialMaxShows = {seed.tutorialMaxShows};
        boolean[] checkPermission = {seed.checkPermission};
        boolean[] creativeBypasses = {seed.creativeBypasses};
        boolean[] operatorsBypass = {seed.operatorsBypass};
        boolean[] limitedBuilding = {seed.limitedBuilding};
        boolean[] torchYDeny = {seed.torchYDeny};
        boolean[] torchSoftDeny = {seed.torchSoftDeny};
        int[] torchY = {seed.torchY};
        boolean[] torchFizz = {seed.torchFizz};
        int[] torchBurnDays = {seed.torchBurnDays};
        boolean[] creeperTntWarning = {seed.creeperTntWarning};

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatableWithFallback("extrahardmode.config.title", "Extra Hard Mode"))
                .setSavingRunnable(() -> save(
                        enabledByDefault[0],
                        debug[0],
                        tutorialMaxShows[0],
                        seed.showWorld,
                        checkPermission[0],
                        creativeBypasses[0],
                        operatorsBypass[0],
                        limitedBuilding[0],
                        torchYDeny[0],
                        torchSoftDeny[0],
                        torchY[0],
                        torchFizz[0],
                        torchBurnDays[0],
                        creeperTntWarning[0]));
        ConfigEntryBuilder entry = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(
                Component.translatableWithFallback("extrahardmode.config.general", "General"));
        general.addEntry(entry.startBooleanToggle(
                        Component.translatableWithFallback(
                                "extrahardmode.config.enabledByDefault", "Enable by default on first world load"),
                        enabledByDefault[0])
                .setDefaultValue(GlobalConfig.defaultEnabledByDefault())
                .setTooltip(Component.translatableWithFallback(
                        "extrahardmode.config.enabledByDefault.tooltip",
                        "Copied into the gamerule once per save. Missing key is true in singleplayer and false on dedicated servers."))
                .setSaveConsumer(v -> enabledByDefault[0] = v)
                .build());
        general.addEntry(entry.startBooleanToggle(
                        Component.translatableWithFallback("extrahardmode.config.debug", "Debug logging"), debug[0])
                .setDefaultValue(false)
                .setSaveConsumer(v -> debug[0] = v)
                .build());
        general.addEntry(entry.startIntField(
                        Component.translatableWithFallback(
                                "extrahardmode.config.tutorialMaxShows", "Tutorial toast max shows"),
                        tutorialMaxShows[0])
                .setDefaultValue(GlobalConfig.DEFAULT_TUTORIAL_MAX_SHOWS)
                .setMin(0)
                .setMax(99)
                .setTooltip(Component.translatableWithFallback(
                        "extrahardmode.config.tutorialMaxShows.tooltip",
                        "SystemToast first-time mechanics per player, including once-only extras. 0 disables toasts. Instant denies still use the action bar. Connected servers apply this via packet, not the client config file."))
                .setSaveConsumer(v -> tutorialMaxShows[0] = v)
                .build());

        if (seed.showWorld) {
            ConfigCategory current = builder.getOrCreateCategory(
                    Component.translatableWithFallback("extrahardmode.config.world", "This world"));
            current.addEntry(entry.startBooleanToggle(
                            Component.translatableWithFallback(
                                    "extrahardmode.config.checkPermission", "Honor permission nodes"),
                            checkPermission[0])
                    .setDefaultValue(true)
                    .setSaveConsumer(v -> checkPermission[0] = v)
                    .build());
            current.addEntry(entry.startBooleanToggle(
                            Component.translatableWithFallback(
                                    "extrahardmode.config.creativeBypasses", "Creative bypasses player rules"),
                            creativeBypasses[0])
                    .setDefaultValue(true)
                    .setSaveConsumer(v -> creativeBypasses[0] = v)
                    .build());
            current.addEntry(entry.startBooleanToggle(
                            Component.translatableWithFallback(
                                    "extrahardmode.config.operatorsBypass", "Operators bypass player rules"),
                            operatorsBypass[0])
                    .setDefaultValue(false)
                    .setSaveConsumer(v -> operatorsBypass[0] = v)
                    .build());
            current.addEntry(entry.startBooleanToggle(
                            Component.translatableWithFallback(
                                    "extrahardmode.config.limitedBuilding", "Limited block placement"),
                            limitedBuilding[0])
                    .setDefaultValue(true)
                    .setSaveConsumer(v -> limitedBuilding[0] = v)
                    .build());
            current.addEntry(entry.startBooleanToggle(
                            Component.translatableWithFallback(
                                    "extrahardmode.config.torchYDeny", "Deny torches and campfires below Y"),
                            torchYDeny[0])
                    .setDefaultValue(true)
                    .setSaveConsumer(v -> torchYDeny[0] = v)
                    .build());
            current.addEntry(entry.startIntField(
                            Component.translatableWithFallback(
                                    "extrahardmode.config.torchNoPlacementUnderY", "Torch cutoff Y"),
                            torchY[0])
                    .setDefaultValue(0)
                    .setSaveConsumer(v -> torchY[0] = v)
                    .build());
            current.addEntry(entry.startBooleanToggle(
                            Component.translatableWithFallback(
                                    "extrahardmode.config.torchSoftDeny", "No torches on soft surfaces"),
                            torchSoftDeny[0])
                    .setDefaultValue(true)
                    .setSaveConsumer(v -> torchSoftDeny[0] = v)
                    .build());
            current.addEntry(entry.startBooleanToggle(
                            Component.translatableWithFallback("extrahardmode.config.torchFizz", "Torch deny fizz"),
                            torchFizz[0])
                    .setDefaultValue(true)
                    .setSaveConsumer(v -> torchFizz[0] = v)
                    .build());
            current.addEntry(entry.startIntField(
                            Component.translatableWithFallback(
                                    "extrahardmode.config.torchBurnDays", "Torch burn days"),
                            torchBurnDays[0])
                    .setDefaultValue(TorchLifetimeRules.DEFAULT_DAYS)
                    .setMin(0)
                    .setMax(TorchLifetimeRules.MAX_DAYS)
                    .setTooltip(Component.translatableWithFallback(
                            "extrahardmode.config.torchBurnDays.tooltip",
                            "Minecraft days a newly placed torch or campfire lasts before it disappears. Campfires pull one log from a chest within 12 blocks to add another period. 0 = permanent. Lights placed before this setting never burn out."))
                    .setSaveConsumer(v -> torchBurnDays[0] = v)
                    .build());
            current.addEntry(entry.startBooleanToggle(
                            Component.translatableWithFallback(
                                    "extrahardmode.config.creeperTntWarning", "Creeper TNT warning sound"),
                            creeperTntWarning[0])
                    .setDefaultValue(true)
                    .setSaveConsumer(v -> creeperTntWarning[0] = v)
                    .build());
        }

        return builder.build();
    }

    private static void save(
            boolean enabledByDefault,
            boolean debug,
            int tutorialMaxShows,
            boolean applyWorld,
            boolean checkPermission,
            boolean creativeBypasses,
            boolean operatorsBypass,
            boolean limitedBuilding,
            boolean torchYDeny,
            boolean torchSoftDeny,
            int torchY,
            boolean torchFizz,
            int torchBurnDays,
            boolean creeperTntWarning) {
        Minecraft client = Minecraft.getInstance();
        boolean connected = client.player != null;
        if (connected && ClientPlayNetworking.canSend(ServerboundConfigPayload.TYPE)) {
            ClientPlayNetworking.send(new ServerboundConfigPayload(
                    enabledByDefault,
                    debug,
                    tutorialMaxShows,
                    applyWorld,
                    checkPermission,
                    creativeBypasses,
                    operatorsBypass,
                    limitedBuilding,
                    torchYDeny,
                    torchSoftDeny,
                    torchY,
                    torchFizz,
                    torchBurnDays,
                    creeperTntWarning));
            return;
        }
        if (client.getSingleplayerServer() != null || connected) {
            return;
        }
        ConfigManager.setGlobal(ConfigManager.global()
                .withEnabledByDefault(enabledByDefault)
                .withDebug(debug)
                .withTutorialMaxShows(tutorialMaxShows));
        ConfigManager.saveGlobal();
    }

    private record Seed(
            boolean showWorld,
            boolean enabledByDefault,
            boolean debug,
            int tutorialMaxShows,
            boolean checkPermission,
            boolean creativeBypasses,
            boolean operatorsBypass,
            boolean limitedBuilding,
            boolean torchYDeny,
            boolean torchSoftDeny,
            int torchY,
            boolean torchFizz,
            int torchBurnDays,
            boolean creeperTntWarning) {
        static Seed capture() {
            GlobalConfig global = ConfigManager.global();
            Minecraft client = Minecraft.getInstance();
            MinecraftServer integrated = client.getSingleplayerServer();
            WorldConfig world = null;
            if (integrated != null && client.player != null) {
                ServerPlayer serverPlayer = integrated.getPlayerList().getPlayer(client.player.getUUID());
                if (serverPlayer != null) {
                    world = ConfigManager.world(serverPlayer.level());
                }
            }
            ClientboundSyncPayload sync = ExtraHardModeClient.lastSync();
            ClientboundSyncPayload.DisplayExtras extras = sync == null ? null : sync.extras();
            boolean showWorld = world != null || sync != null;
            return new Seed(
                    showWorld,
                    extras != null ? extras.enabledByDefault() : global.enabledByDefault(),
                    extras != null ? extras.debug() : global.debug(),
                    extras != null ? extras.tutorialMaxShows() : global.tutorialMaxShows(),
                    world != null
                            ? world.checkPermission()
                            : extras != null && extras.checkPermission(),
                    world != null
                            ? world.creativeBypasses()
                            : extras == null || extras.creativeBypasses(),
                    world != null
                            ? world.operatorsBypass()
                            : extras != null && extras.operatorsBypass(),
                    world != null ? world.limitedBuilding() : sync == null || sync.limitedBuilding(),
                    world != null ? world.torchYDeny() : sync == null || sync.torchYDeny(),
                    world != null ? world.torchSoftDeny() : sync == null || sync.torchSoftDeny(),
                    world != null ? world.torchNoPlacementUnderY() : sync == null ? 0 : sync.torchNoPlacementUnderY(),
                    world != null ? world.torchFizz() : extras == null || extras.torchFizz(),
                    world != null
                            ? world.torchBurnDays()
                            : sync == null ? TorchLifetimeRules.DEFAULT_DAYS : sync.torchBurnDays(),
                    world != null
                            ? world.creeperTntWarning()
                            : extras == null || extras.creeperTntWarning());
        }
    }
}
