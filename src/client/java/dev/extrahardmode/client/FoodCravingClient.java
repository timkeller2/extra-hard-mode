package dev.extrahardmode.client;

import dev.extrahardmode.network.ServerboundFoodCravingRequestPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/**
 * Asks again after a rejoin until a craving snapshot arrives.
 * A server that does not know this packet is left alone.
 */
public final class FoodCravingClient {
    private static final int ASK_EVERY = 20;
    private static int wait;

    private FoodCravingClient() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(FoodCravingClient::tick);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> ask());
    }

    public static void disconnected() {
        wait = 0;
    }

    private static void tick(Minecraft client) {
        if (client.player == null || ExtraHardModeClient.lastFoodCraving() != null) {
            wait = 0;
            return;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        ask();
    }

    private static void ask() {
        if (!ClientPlayNetworking.canSend(ServerboundFoodCravingRequestPayload.TYPE)) {
            return;
        }
        ClientPlayNetworking.send(ServerboundFoodCravingRequestPayload.INSTANCE);
        wait = ASK_EVERY;
    }
}
