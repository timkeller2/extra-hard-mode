package dev.extrahardmode.client;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.feature.TorchLifetimeRules;
import dev.extrahardmode.network.ClientboundLightLookPayload;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** Remaining torch, campfire, or resident-restock time under the crosshair. */
public final class LightLookHud {
    public static final Identifier ELEMENT_ID = ExtraHardModeMod.id("light_look");
    private static final int OFFSET_Y = 12;

    private LightLookHud() {}

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, ELEMENT_ID, LightLookHud::extract);
    }

    static void extract(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) {
        ClientboundLightLookPayload look = ExtraHardModeClient.lastLightLook();
        if (look == null || !look.visible()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator() || minecraft.font == null) {
            return;
        }
        graphics.centeredText(
                minecraft.font,
                TorchLifetimeRules.remainingLabel(look.remainingTicks()),
                graphics.guiWidth() / 2,
                graphics.guiHeight() / 2 + OFFSET_Y,
                TorchLifetimeRules.lightLookColor(look.remainingTicks()));
    }
}
