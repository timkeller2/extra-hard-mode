package dev.extrahardmode.world;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.network.EhmNetworking;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Enable gate for Extra Hard Mode.
 *
 * <p>26.2 {@code GameRules} are server-global ({@code ServerLevel.getGameRules()} is
 * {@code MinecraftServer.getGameRules()}). {@link #ENABLED} is a master switch,
 * first-applied once on overworld from TOML {@code enabledByDefault}.
 * {@code /gamerule extrahardmode:enabled false} disables every dimension;
 * {@code /gamerule extrahardmode:enabled true} enables dimensions whose live
 * {@code enabled} flag is still true (opt-out, default true).
 *
 * <p>{@link #isActive} is gamerule AND that dimension's flag, so
 * {@code /ehm enabled [world]} can disagree after {@code /ehm set-world}.
 *
 * <p>Every mixin inject's first statement must be a WorldGate check.
 */
public final class WorldGate {
    public static final GameRule<Boolean> ENABLED = GameRuleBuilder.forBoolean(false)
            .category(GameRuleCategory.MISC)
            .buildAndRegister(Identifier.fromNamespaceAndPath("extrahardmode", "enabled"));

    private static boolean overworldToastPending;

    private WorldGate() {}

    public static void register() {
        // Touch ENABLED so the gamerule is registered during mod init.
    }

    /**
     * True when the global gamerule is on and this dimension's enabled flag is on
     * (dimension flags default true).
     */
    public static boolean isActive(ServerLevel level) {
        if (!level.getGameRules().get(ENABLED)) {
            return false;
        }
        return dimensionEnabled(level);
    }

    public static boolean isModuleActive(ServerLevel level, Identifier moduleId) {
        return isActive(level) && ConfigManager.world(level).isModuleEnabled(moduleId);
    }

    public static boolean dimensionEnabled(ServerLevel level) {
        return ConfigManager.world(level).enabled();
    }

    public static void onLevelLoad(MinecraftServer server, ServerLevel level) {
        ServerLevel overworld = server.overworld();
        if (overworld != null && overworld != level) {
            applyIfNeeded(server, overworld);
        }
        applyIfNeeded(server, level);
        ConfigManager.loadWorld(level);
    }

    public static void applyIfNeeded(MinecraftServer server, ServerLevel level) {
        ExtraHardModeBootData boot = bootData(server, level);
        if (boot == null) {
            return;
        }
        if (level.dimension() == Level.OVERWORLD && !boot.isGameruleApplied()) {
            boolean enabledByDefault = ConfigManager.global().enabledByDefault();
            level.getGameRules().set(ENABLED, enabledByDefault, server);
            boot.markGameruleApplied();
        }
        Identifier id = level.dimension().identifier();
        if (boot.contains(id)) {
            return;
        }
        Boolean overworldLive = overworldLiveEnabled(server, level);
        boolean value = FirstApply.resolveDimensionEnabled(id.toString(), overworldLive);
        WorldConfig config = ConfigManager.world(level);
        if (!config.hasEnabledKey()) {
            config.setEnabled(value);
        }
        if (!config.save(server)) {
            ExtraHardModeMod.LOGGER.error("EHM first-apply {} skipped stamp; world.toml save failed", id);
            return;
        }
        boot.markApplied(id, config.enabled());
        ExtraHardModeMod.LOGGER.info(
                "EHM first-apply {} enabled={} (overworld save {})",
                id,
                config.enabled(),
                server.getWorldPath(LevelResource.ROOT).toAbsolutePath());
        if (level.dimension() == Level.OVERWORLD) {
            overworldToastPending = true;
            toastOnlinePlayers(level, isActive(level));
        } else {
            ExtraHardModeMod.LOGGER.info("EHM enabled={} in {}", isActive(level), id);
        }
    }

    public static ExtraHardModeBootData bootData(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            return null;
        }
        return overworld.getDataStorage().computeIfAbsent(ExtraHardModeBootData.TYPE);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        if (overworldToastPending && player.level().dimension() == Level.OVERWORLD) {
            boolean enabled = isActive(player.level());
            EhmNetworking.sendToast(player, enabled ? "enabled_on" : "enabled_off");
            overworldToastPending = false;
        }
    }

    private static void toastOnlinePlayers(ServerLevel overworld, boolean enabled) {
        if (overworld.players().isEmpty()) {
            return;
        }
        String messageId = enabled ? "enabled_on" : "enabled_off";
        for (ServerPlayer player : overworld.getServer().getPlayerList().getPlayers()) {
            EhmNetworking.sendToast(player, messageId);
        }
        overworldToastPending = false;
    }

    private static ExtraHardModeBootData bootData(MinecraftServer server, ServerLevel level) {
        ExtraHardModeBootData boot = bootData(server);
        if (boot != null) {
            return boot;
        }
        if (level.dimension() == Level.OVERWORLD) {
            return level.getDataStorage().computeIfAbsent(ExtraHardModeBootData.TYPE);
        }
        ExtraHardModeMod.LOGGER.warn(
                "Skipping first-apply for {} — overworld save not available", level.dimension().identifier());
        return null;
    }

    private static Boolean overworldLiveEnabled(MinecraftServer server, ServerLevel level) {
        ServerLevel overworld = server.overworld();
        if (overworld == null || overworld == level) {
            return null;
        }
        return ConfigManager.world(overworld).enabled();
    }
}
