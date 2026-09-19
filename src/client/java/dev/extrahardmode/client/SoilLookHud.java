package dev.extrahardmode.client;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.feature.CropGrowthRules;
import dev.extrahardmode.network.ClientboundSoilLookPayload;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** Shown soil-modifier percent under the crosshair: green at or above 0, red below. */
public final class SoilLookHud {
    public static final Identifier ELEMENT_ID = ExtraHardModeMod.id("soil_look");
    private static final int OFFSET_Y = 12;

    private SoilLookHud() {}

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, ELEMENT_ID, SoilLookHud::extract);
    }

    static void extract(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) {
        ClientboundSoilLookPayload look = ExtraHardModeClient.lastSoilLook();
        if (look == null || !look.visible()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator() || minecraft.font == null) {
            return;
        }
        String label = CropGrowthRules.percentLabel(look.displayed());
        graphics.centeredText(
                minecraft.font,
                label,
                graphics.guiWidth() / 2,
                graphics.guiHeight() / 2 + OFFSET_Y,
                CropGrowthRules.soilLookColor(look.displayed()));
    }
}
