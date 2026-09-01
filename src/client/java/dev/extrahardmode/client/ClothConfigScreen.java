package dev.extrahardmode.client;

import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.GlobalConfig;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.network.ClientboundSyncPayload;
import dev.extrahardmode.network.EhmNetworking;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Optional Cloth Config screen. Classloaded only when Cloth is present.
 * Dedicated servers never load this client class.
 */
public final class ClothConfigScreen {
    private ClothConfigScreen() {}

    public static boolean available() {
        return FabricLoader.getInstance().isModLoaded("cloth-config")
                || FabricLoader.getInstance().isModLoaded("cloth-config2");
    }

    public static Screen create(Screen parent) {
        GlobalConfig global = ConfigManager.global();
        boolean[] enabledByDefault = {global.enabledByDefault()};
        boolean[] debug = {global.debug()};
        int[] tutorialMaxShows = {global.tutorialMaxShows()};

        MinecraftServer integrated = Minecraft.getInstance().getSingleplayerServer();
        WorldConfig world = integrated == null ? null : ConfigManager.world(currentLevel(integrated));
        boolean[] checkPermission = {world == null || world.checkPermission()};
        boolean[] creativeBypasses = {world == null || world.creativeBypasses()};
        boolean[] operatorsBypass = {world != null && world.operatorsBypass()};
        boolean[] limitedBuilding = {world == null || world.limitedBuilding()};
        boolean[] torchYDeny = {world == null || world.torchYDeny()};
        boolean[] torchSoftDeny = {world == null || world.torchSoftDeny()};
        int[] torchY = {world == null ? 0 : world.torchNoPlacementUnderY()};
        boolean[] torchFizz = {world == null || world.torchFizz()};
        boolean[] creeperTntWarning = {world == null || world.creeperTntWarning()};

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatableWithFallback("extrahardmode.config.title", "Extra Hard Mode"))
                .setSavingRunnable(() -> save(
                        enabledByDefault[0],
                        debug[0],
                        tutorialMaxShows[0],
                        world != null,
                        checkPermission[0],
                        creativeBypasses[0],
                        operatorsBypass[0],
                        limitedBuilding[0],
                        torchYDeny[0],
                        torchSoftDeny[0],
                        torchY[0],
                        torchFizz[0],
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
                        "SystemToast first-time mechanics per player. 0 disables toasts. Instant denies still use the action bar."))
                .setSaveConsumer(v -> tutorialMaxShows[0] = v)
                .build());

        if (world != null) {
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
                            Component.translatableWithFallback("extrahardmode.config.torchYDeny", "Deny torches below Y"),
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
            current.addEntry(entry.startBooleanToggle(
                            Component.translatableWithFallback(
                                    "extrahardmode.config.creeperTntWarning", "Creeper TNT warning sound"),
                            creeperTntWarning[0])
                    .setDefaultValue(true)
                    .setSaveConsumer(v -> creeperTntWarning[0] = v)
                    .build());
        } else {
            ClientboundSyncPayload sync = ExtraHardModeClient.lastSync();
            if (sync != null) {
                ConfigCategory synced = builder.getOrCreateCategory(
                        Component.translatableWithFallback("extrahardmode.config.synced", "Server (read-only)"));
                synced.addEntry(readOnly(entry.startBooleanToggle(
                                Component.translatableWithFallback(
                                        "extrahardmode.config.limitedBuilding", "Limited block placement"),
                                sync.limitedBuilding())
                        .setDefaultValue(sync.limitedBuilding())
                        .build()));
                synced.addEntry(readOnly(entry.startBooleanToggle(
                                Component.translatableWithFallback(
                                        "extrahardmode.config.torchYDeny", "Deny torches below Y"),
                                sync.torchYDeny())
                        .setDefaultValue(sync.torchYDeny())
                        .build()));
                synced.addEntry(readOnly(entry.startIntField(
                                Component.translatableWithFallback(
                                        "extrahardmode.config.torchNoPlacementUnderY", "Torch cutoff Y"),
                                sync.torchNoPlacementUnderY())
                        .setDefaultValue(sync.torchNoPlacementUnderY())
                        .build()));
                synced.addEntry(readOnly(entry.startBooleanToggle(
                                Component.translatableWithFallback(
                                        "extrahardmode.config.torchSoftDeny", "No torches on soft surfaces"),
                                sync.torchSoftDeny())
                        .setDefaultValue(sync.torchSoftDeny())
                        .build()));
            }
        }

        return builder.build();
    }

    private static AbstractConfigListEntry<?> readOnly(AbstractConfigListEntry<?> entry) {
        entry.setEditable(false);
        return entry;
    }

    private static ServerLevel currentLevel(MinecraftServer server) {
        if (Minecraft.getInstance().player != null
                && Minecraft.getInstance().player.level() instanceof ServerLevel level) {
            return level;
        }
        return server.overworld();
    }

    private static void save(
            boolean enabledByDefault,
            boolean debug,
            int tutorialMaxShows,
            boolean hasWorld,
            boolean checkPermission,
            boolean creativeBypasses,
            boolean operatorsBypass,
            boolean limitedBuilding,
            boolean torchYDeny,
            boolean torchSoftDeny,
            int torchY,
            boolean torchFizz,
            boolean creeperTntWarning) {
        ConfigManager.setGlobal(new GlobalConfig(enabledByDefault, debug, tutorialMaxShows));
        ConfigManager.saveGlobal();
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (!hasWorld || server == null) {
            return;
        }
        server.execute(() -> {
            WorldConfig world = ConfigManager.world(currentLevel(server));
            world.setCheckPermission(checkPermission);
            world.setCreativeBypasses(creativeBypasses);
            world.setOperatorsBypass(operatorsBypass);
            world.setLimitedBuilding(limitedBuilding);
            world.setTorchYDeny(torchYDeny);
            world.setTorchSoftDeny(torchSoftDeny);
            world.setTorchNoPlacementUnderY(torchY);
            world.setTorchFizz(torchFizz);
            world.setCreeperTntWarning(creeperTntWarning);
            world.save(server);
            EhmNetworking.syncAll(server);
        });
    }
}
