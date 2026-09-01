package dev.extrahardmode.network;

import dev.extrahardmode.world.WorldGate;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public final class EhmNetworking {
    private EhmNetworking() {}

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(ClientboundSyncPayload.TYPE, ClientboundSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ClientboundToastPayload.TYPE, ClientboundToastPayload.STREAM_CODEC);
        ServerPlayConnectionEvents.JOIN.register(EhmNetworking::onJoin);
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(
                (player, origin, destination) -> sendSync(player));
    }

    public static void sendSync(ServerPlayer player) {
        ServerPlayNetworking.send(player, ClientboundSyncPayload.from(player.level()));
    }

    public static void sendToast(ServerPlayer player, String messageId) {
        ServerPlayNetworking.send(player, new ClientboundToastPayload(messageId));
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
}
