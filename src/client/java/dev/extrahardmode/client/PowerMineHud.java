package dev.extrahardmode.client;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.network.ClientboundPowerMinePayload;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Depleting bar under the crosshair while power mining is active. */
public final class PowerMineHud {
    public static final net.minecraft.resources.Identifier ELEMENT_ID = ExtraHardModeMod.id("power_mine_bar");
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;

    private PowerMineHud() {}

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, ELEMENT_ID, PowerMineHud::extract);
    }

    static void extract(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) {
        ClientboundPowerMinePayload mining = ExtraHardModeClient.lastPowerMine();
        if (mining == null || mining.remainingTicks() <= 0 || mining.maxTicks() <= 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator()) {
            return;
        }
        int filled = Math.max(1, (int) (BAR_WIDTH * (mining.remainingTicks() / (float) mining.maxTicks())));
        filled = Math.min(BAR_WIDTH, filled);
        int x = graphics.guiWidth() / 2 - BAR_WIDTH / 2;
        int y = graphics.guiHeight() / 2 + 16;
        if (flightBarVisible()) {
            y += BAR_HEIGHT + 6;
        }
        graphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, 0xFF000000);
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFF1A2430);
        graphics.fill(x, y, x + filled, y + BAR_HEIGHT, 0xFF6EC1E4);
        graphics.item(new ItemStack(Items.IRON_PICKAXE), x - 20, y - 6);
    }

    private static boolean flightBarVisible() {
        var flight = ExtraHardModeClient.lastFlight();
        return flight != null && flight.remainingTicks() > 0 && flight.maxTicks() > 0;
    }
}
