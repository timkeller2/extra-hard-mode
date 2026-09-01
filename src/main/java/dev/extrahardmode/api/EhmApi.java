package dev.extrahardmode.api;

import dev.extrahardmode.command.EhmPermissions;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.commands.Commands;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class EhmApi {
    private EhmApi() {}

    /**
     * True when the server-global {@code extrahardmode:enabled} gamerule is on and
     * this dimension's enabled flag is on. {@code /gamerule extrahardmode:enabled false}
     * turns Extra Hard Mode off in every dimension.
     */
    public static boolean isActive(ServerLevel level) {
        return WorldGate.isActive(level);
    }

    public static boolean moduleEnabled(ServerLevel level, Identifier moduleId) {
        return WorldGate.isModuleActive(level, moduleId);
    }

    /**
     * Personal bypass for player-triggered features. World physics still runs.
     * Creative bypasses player-triggered features only when configured.
     */
    public static boolean playerBypasses(ServerPlayer player) {
        if (Boolean.TRUE.equals(player.getAttachedOrElse(EhmAttachments.EHM_BYPASS, Boolean.FALSE))) {
            return true;
        }
        WorldConfig config = ConfigManager.world(player.level());
        if (config.creativeBypasses() && player.isCreative()) {
            return true;
        }
        if (config.operatorsBypass() && Commands.LEVEL_GAMEMASTERS.check(player.permissions())) {
            return true;
        }
        return config.checkPermission() && player.checkPermission(EhmPermissions.BYPASS, false);
    }

    public static WorldConfig worldConfig(ServerLevel level) {
        return ConfigManager.world(level);
    }
}
