package dev.extrahardmode.client;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.feature.ChunkClearRules;
import dev.extrahardmode.feature.ChunkHeldHudRules;
import dev.extrahardmode.network.ClientboundSyncPayload;
import dev.extrahardmode.player.EhmAttachments;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix3x2fStack;

/** Corner diamond while the player stands in a chunk the alliance has cleared. */
public final class ChunkHeldHud {
    public static final Identifier ELEMENT_ID = ExtraHardModeMod.id("chunk_held");
    private static final float SCALE = ChunkHeldHudRules.SIZE / 16.0F;
    /** Built on first draw. A stack in the class initializer runs before components are bound. */
    private static ItemStack diamond;

    private ChunkHeldHud() {}

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, ELEMENT_ID, ChunkHeldHud::extract);
    }

    static void extract(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) {
        ClientboundSyncPayload sync = ExtraHardModeClient.lastSync();
        if (sync != null && !sync.worldActive()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || minecraft.player.isSpectator()
                || minecraft.font == null
                || !minecraft.player.getAttachedOrElse(EhmAttachments.EHM_CHUNK_HELD, Boolean.FALSE)) {
            return;
        }
        if (diamond == null) {
            diamond = new ItemStack(Items.DIAMOND);
        }
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        try {
            pose.translate(ChunkHeldHudRules.x(), ChunkHeldHudRules.y());
            pose.scale(SCALE, SCALE);
            graphics.item(diamond, 0, 0);
        } finally {
            pose.popMatrix();
        }
        int mouseX = (int) minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
        int mouseY = (int) minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());
        if (!ChunkHeldHudRules.hit(mouseX, mouseY)) {
            return;
        }
        Font font = minecraft.font;
        graphics.setTooltipForNextFrame(
                font,
                Component.translatableWithFallback(
                        "tougher.tooltip.chunk_held", ChunkClearRules.PEACE_MESSAGE),
                mouseX,
                mouseY);
        graphics.extractDeferredElements(mouseX, mouseY, 0.0F);
    }
}
