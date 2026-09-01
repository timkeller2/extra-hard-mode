package dev.extrahardmode.world;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
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

/** Every mixin inject's first statement must be a WorldGate check. */
public final class WorldGate {
    public static final GameRule<Boolean> ENABLED = GameRuleBuilder.forBoolean(false)
            .category(GameRuleCategory.MISC)
            .buildAndRegister(Identifier.fromNamespaceAndPath("extrahardmode", "enabled"));

    private static boolean overworldToastPending;

    private WorldGate() {}

    public static void register() {
        // Touch ENABLED so the gamerule is registered during mod init.
    }

    public static boolean isActive(ServerLevel level) {
        return level.getGameRules().get(ENABLED);
    }

    public static boolean isModuleActive(ServerLevel level, Identifier moduleId) {
        return isActive(level) && ConfigManager.world(level).isModuleEnabled(moduleId);
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
        Identifier id = level.dimension().identifier();
        if (boot.contains(id)) {
            return;
        }
        boolean enabledByDefault = ConfigManager.global().enabledByDefault();
        Boolean overworldRule = overworldRule(server, level);
        boolean value = FirstApply.resolveEnabled(id.toString(), enabledByDefault, overworldRule);
        level.getGameRules().set(ENABLED, value, server);
        boot.markApplied(id);
        ExtraHardModeMod.LOGGER.info(
                "EHM first-apply {} enabled={} (overworld save {})",
                id,
                value,
                server.getWorldPath(LevelResource.ROOT).toAbsolutePath());
        if (level.dimension() == Level.OVERWORLD) {
            overworldToastPending = true;
            toastOnlinePlayers(level, value);
        } else {
            ExtraHardModeMod.LOGGER.info("EHM enabled={} in {}", value, id);
        }
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
        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            if (level.dimension() == Level.OVERWORLD) {
                overworld = level;
            } else {
                ExtraHardModeMod.LOGGER.warn("Skipping first-apply for {} — overworld save not available", level.dimension().identifier());
                return null;
            }
        }
        return overworld.getDataStorage().computeIfAbsent(ExtraHardModeBootData.TYPE);
    }

    private static Boolean overworldRule(MinecraftServer server, ServerLevel level) {
        ServerLevel overworld = server.overworld();
        if (overworld == null || overworld == level) {
            return null;
        }
        return overworld.getGameRules().get(ENABLED);
    }

}
