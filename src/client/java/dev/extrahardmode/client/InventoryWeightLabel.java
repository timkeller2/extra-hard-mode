package dev.extrahardmode.client;

import dev.extrahardmode.network.ClientboundSyncPayload;
import dev.extrahardmode.player.WeightDisplayRules;
import dev.extrahardmode.task.WeightCheckTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;

/** Weight line on the survival inventory screen. Coordinates are panel-local. */
public final class InventoryWeightLabel {
    private InventoryWeightLabel() {}

    public static void draw(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.font == null) {
            return;
        }
        ClientboundSyncPayload sync = ExtraHardModeClient.lastSync();
        if (sync == null || !sync.worldActive()) {
            return;
        }
        ClientboundSyncPayload.DisplayExtras.WeightBits limits = sync.extras().weight();
        if (!limits.enabled()) {
            return;
        }
        double weight = WeightCheckTask.inventoryWeight(
                minecraft.player, limits.armorPiece(), limits.stack(), limits.tool());
        String text = WeightDisplayRules.label(weight, limits.maxPoints());
        WeightDisplayRules.Band band = WeightDisplayRules.band(true, weight, limits.maxPoints());
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        try {
            pose.translate(WeightDisplayRules.x(minecraft.font.width(text)), WeightDisplayRules.TEXT_Y);
            pose.scale(WeightDisplayRules.TEXT_SCALE, WeightDisplayRules.TEXT_SCALE);
            graphics.text(minecraft.font, text, 0, 0, WeightDisplayRules.color(band), true);
        } finally {
            pose.popMatrix();
        }
    }
}
