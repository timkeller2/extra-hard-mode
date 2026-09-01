package dev.extrahardmode.client;

import dev.extrahardmode.network.ClientboundSyncPayload;
import dev.extrahardmode.network.ClientboundToastPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

public class ExtraHardModeClient implements ClientModInitializer {
    private static final SystemToast.SystemToastId TOAST_ID = new SystemToast.SystemToastId(5000L);

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
        SystemToast.addOrUpdate(client.gui.toastManager(), TOAST_ID, title, body);
    }

    private static String toastFallback(String messageId) {
        return switch (messageId) {
            case "enabled_on" -> "Extra Hard Mode is on. /gamerule extrahardmode:enabled";
            case "enabled_off" -> "Extra Hard Mode is off. /gamerule extrahardmode:enabled";
            case "no_crafting_melon_seeds" -> "You can't craft melon or pumpkin seeds. Find them in the world.";
            default -> messageId;
        };
    }
}
