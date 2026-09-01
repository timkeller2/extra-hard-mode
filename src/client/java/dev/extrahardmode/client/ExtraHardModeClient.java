package dev.extrahardmode.client;

import dev.extrahardmode.module.MessageId;
import dev.extrahardmode.network.ClientboundSyncPayload;
import dev.extrahardmode.network.ClientboundToastPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

public class ExtraHardModeClient implements ClientModInitializer {
    private static final Map<String, SystemToast.SystemToastId> TOAST_IDS = new ConcurrentHashMap<>();

    private static volatile ClientboundSyncPayload lastSync;

    public static ClientboundSyncPayload lastSync() {
        return lastSync;
    }

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ClientboundSyncPayload.TYPE, (payload, context) -> lastSync = payload);
        ClientPlayNetworking.registerGlobalReceiver(ClientboundToastPayload.TYPE, ExtraHardModeClient::onToast);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> lastSync = null);
    }

    private static void onToast(ClientboundToastPayload payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> showToast(context.client(), payload.messageId()));
    }

    private static void showToast(Minecraft client, String messageId) {
        Component title = Component.translatableWithFallback("extrahardmode.toast.title", "Extra Hard Mode");
        Component body = Component.translatableWithFallback(
                "extrahardmode.toast." + messageId,
                toastFallback(messageId));
        SystemToast.addOrUpdate(client.gui.toastManager(), toastId(messageId), title, body);
    }

    private static SystemToast.SystemToastId toastId(String messageId) {
        return TOAST_IDS.computeIfAbsent(messageId, id -> new SystemToast.SystemToastId(5000L));
    }

    private static String toastFallback(String messageId) {
        MessageId known = MessageId.byId(messageId);
        return known != null ? known.fallback() : messageId;
    }
}
