package dev.extrahardmode.client;

import dev.extrahardmode.feature.SaturationOutlineRules;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Gold rim around a hunger icon for the saturation it still holds. */
public final class SaturationOutline {
    private SaturationOutline() {}

    public static void draw(GuiGraphicsExtractor graphics, Identifier sprite, int x, int y, float coverage) {
        int clip = SaturationOutlineRules.clipWidth(coverage);
        if (graphics == null || sprite == null || clip <= 0) {
            return;
        }
        boolean partial = clip < SaturationOutlineRules.ICON_SIZE;
        if (partial) {
            graphics.enableScissor(x + SaturationOutlineRules.ICON_SIZE - clip - 1, y - 1, x + SaturationOutlineRules.ICON_SIZE + 1, y + SaturationOutlineRules.ICON_SIZE + 1);
        }
        int color = SaturationOutlineRules.GOLD;
        int size = SaturationOutlineRules.ICON_SIZE;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x - 1, y, size, size, color);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x + 1, y, size, size, color);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y - 1, size, size, color);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y + 1, size, size, color);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x - 1, y - 1, size, size, color);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x + 1, y - 1, size, size, color);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x - 1, y + 1, size, size, color);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x + 1, y + 1, size, size, color);
        if (partial) {
            graphics.disableScissor();
        }
    }
}
