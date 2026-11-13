package dev.extrahardmode.client;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.network.ClientboundCopperChestLookPayload;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** Days until a copper chest is public, drawn under the crosshair for someone who does not own it. */
public final class CopperChestLookHud {
    public static final Identifier ELEMENT_ID = ExtraHardModeMod.id("copper_chest_look");
    private static final int OFFSET_Y = 12;
    private static final int COLOR = 0xFFFFCC66;

    private CopperChestLookHud() {}

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, ELEMENT_ID, CopperChestLookHud::extract);
    }

    static void extract(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) {
        ClientboundCopperChestLookPayload look = ExtraHardModeClient.lastCopperChestLook();
        if (look == null || !look.visible() || look.label() == null || look.label().isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.font == null) {
            return;
        }
        graphics.centeredText(
                minecraft.font, look.label(), graphics.guiWidth() / 2, graphics.guiHeight() / 2 + OFFSET_Y, COLOR);
    }
}
