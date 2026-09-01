package dev.extrahardmode.network;

import dev.extrahardmode.command.EhmPermissions;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.GlobalConfig;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.world.WorldGate;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public final class EhmNetworking {
    private EhmNetworking() {}

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(ClientboundSyncPayload.TYPE, ClientboundSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ClientboundToastPayload.TYPE, ClientboundToastPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ServerboundConfigPayload.TYPE, ServerboundConfigPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ServerboundConfigPayload.TYPE, EhmNetworking::onConfig);
        ServerPlayConnectionEvents.JOIN.register(EhmNetworking::onJoin);
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(
                (player, origin, destination) -> sendSync(player));
    }

    public static void sendSync(ServerPlayer player) {
        ServerPlayNetworking.send(player, ClientboundSyncPayload.from(ConfigManager.world(player.level())));
    }

    public static void sendToast(ServerPlayer player, String messageId) {
        sendToast(player, messageId, "");
    }

    public static void sendToast(ServerPlayer player, String messageId, String arg) {
        ServerPlayNetworking.send(player, new ClientboundToastPayload(messageId, arg == null ? "" : arg));
    }

    public static void syncAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendSync(player);
        }
    }

    private static void onJoin(
            ServerGamePacketListenerImpl handler,
            net.fabricmc.fabric.api.networking.v1.PacketSender sender,
            MinecraftServer server) {
        sendSync(handler.player);
        WorldGate.onPlayerJoin(handler.player);
    }

    private static void onConfig(ServerboundConfigPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        boolean ops = Commands.LEVEL_ADMINS.check(player.permissions());
        if (!player.checkPermission(EhmPermissions.ADMIN, ops)) {
            return;
        }
        MinecraftServer server = player.level().getServer();
        ConfigManager.setGlobal(new GlobalConfig(
                payload.enabledByDefault(), payload.debug(), Math.max(0, payload.tutorialMaxShows())));
        ConfigManager.saveGlobal();
        if (payload.applyWorld()) {
            WorldConfig world = ConfigManager.world(player.level());
            world.applyCloth(
                    payload.checkPermission(),
                    payload.creativeBypasses(),
                    payload.operatorsBypass(),
                    payload.limitedBuilding(),
                    payload.torchYDeny(),
                    payload.torchSoftDeny(),
                    payload.torchNoPlacementUnderY(),
                    payload.torchFizz(),
                    payload.creeperTntWarning());
            world.save(server);
        }
        syncAll(server);
    }
}
