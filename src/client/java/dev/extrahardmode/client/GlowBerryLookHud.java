package dev.extrahardmode.client;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.feature.CropGrowthRules;
import dev.extrahardmode.network.ClientboundGlowBerryLookPayload;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** Remaining bone-meal wait on the glow berry segment under the crosshair. */
public final class GlowBerryLookHud {
    public static final Identifier ELEMENT_ID = ExtraHardModeMod.id("glow_berry_look");
    private static final int OFFSET_Y = 12;

    private GlowBerryLookHud() {}

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, ELEMENT_ID, GlowBerryLookHud::extract);
    }

    static void extract(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) {
        ClientboundGlowBerryLookPayload look = ExtraHardModeClient.lastGlowBerryLook();
        if (look == null || !look.visible() || look.remainingTicks() <= 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator() || minecraft.font == null) {
            return;
        }
        graphics.centeredText(
                minecraft.font,
                CropGrowthRules.glowBerryMealLabel(look.remainingTicks()),
                graphics.guiWidth() / 2,
                graphics.guiHeight() / 2 + OFFSET_Y,
                CropGrowthRules.GLOW_BERRY_MEAL_LOOK_COLOR);
    }
}
