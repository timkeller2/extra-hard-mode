package dev.extrahardmode.client;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.feature.ReconnectRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * Rejoins the last multiplayer server after a short network drop.
 * Quits, kicks, and bans stay on the disconnect screen.
 */
public final class BriefReconnect {
    private static ServerData server;
    private static Screen returnTo;
    private static int attempts;

    private BriefReconnect() {}

    public static boolean isRetrying() {
        return server != null;
    }

    public static void joined() {
        clear();
    }

    public static void abandon() {
        clear();
    }

    /**
     * Called after vanilla has opened the disconnect screen.
     * The level is already gone, so this is not a world rule.
     */
    public static void consider(
            Minecraft minecraft, ServerData serverData, Component reason, boolean inPlay, boolean transferring) {
        if (minecraft == null || transferring || serverData == null || blank(serverData.ip)) {
            return;
        }
        if (!ReconnectRules.transportDrop(translationKey(reason), reason == null ? "" : reason.getString())) {
            abandon();
            return;
        }
        if (!inPlay && !isRetrying()) {
            return;
        }
        Screen screen = minecraft.gui.screen();
        if (!(screen instanceof DisconnectedScreen)) {
            return;
        }
        if (server == null || !serverData.ip.equals(server.ip)) {
            server = serverData;
            attempts = 0;
        } else {
            server = serverData;
        }
        if (attempts >= ReconnectRules.MAX_ATTEMPTS) {
            ExtraHardModeMod.LOGGER.info(
                    "Tougher left the disconnect screen after {} reconnects", ReconnectRules.MAX_ATTEMPTS);
            abandon();
            return;
        }
        attempts++;
        returnTo = screen;
        String name = blank(serverData.name) ? "the server" : serverData.name;
        minecraft.gui.setScreen(new ReconnectScreen(attempts, name));
    }

    public static void beginConnect(Minecraft minecraft) {
        if (minecraft == null || server == null || blank(server.ip)) {
            abandon();
            return;
        }
        if (!(minecraft.gui.screen() instanceof ReconnectScreen)) {
            return;
        }
        Screen parent = returnTo != null ? returnTo : new JoinMultiplayerScreen(new TitleScreen());
        ExtraHardModeMod.LOGGER.info(
                "Tougher reconnect attempt {} of {}", attempts, ReconnectRules.MAX_ATTEMPTS);
        ConnectScreen.startConnecting(
                parent, minecraft, ServerAddress.parseString(server.ip), server, false, null);
    }

    public static void cancel(Minecraft minecraft) {
        Screen back = returnTo;
        if (server == null && back == null) {
            return;
        }
        clear();
        if (minecraft != null && back != null) {
            minecraft.gui.setScreen(back);
        }
    }

    private static void clear() {
        server = null;
        returnTo = null;
        attempts = 0;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    static String translationKey(Component reason) {
        if (reason != null && reason.getContents() instanceof TranslatableContents contents) {
            String key = contents.getKey();
            return key == null ? "" : key;
        }
        return "";
    }
}
