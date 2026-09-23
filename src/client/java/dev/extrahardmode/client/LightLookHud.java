package dev.extrahardmode.client;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.feature.TorchLifetimeRules;
import dev.extrahardmode.network.ClientboundLightLookPayload;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Torch time, restock time, and resident hearts under the crosshair. */
public final class LightLookHud {
    public static final Identifier ELEMENT_ID = ExtraHardModeMod.id("light_look");
    private static final Identifier HEART = Identifier.withDefaultNamespace("hud/heart/full");
    private static final int OFFSET_Y = 12;
    private static final int HEART_SIZE = 9;

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
        boolean showTime = look.remainingTicks() >= 0;
        String duration = showTime ? TorchLifetimeRules.remainingLabel(look.remainingTicks()) : "";
        int hearts = Math.max(0, look.hearts());
        int textWidth = duration.isEmpty() ? 0 : minecraft.font.width(duration);
        int gap = duration.isEmpty() || hearts == 0 ? 0 : 4;
        int heartsWidth = hearts == 0 ? 0 : hearts * 10 - 1;
        int total = textWidth + gap + heartsWidth;
        int x = graphics.guiWidth() / 2 - total / 2;
        int y = graphics.guiHeight() / 2 + OFFSET_Y;
        int color = showTime ? TorchLifetimeRules.lightLookColor(look.remainingTicks()) : 0xFFFF5555;
        if (!duration.isEmpty()) {
            graphics.text(minecraft.font, duration, x, y, color, false);
        }
        int heartX = x + textWidth + gap;
        int heartY = y - 1;
        for (int i = 0; i < hearts; i++) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART, heartX + i * 10, heartY, HEART_SIZE, HEART_SIZE);
        }
    }
}
