package dev.extrahardmode.mixin.client;

import dev.extrahardmode.client.InventoryWeightLabel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {
    @Inject(method = "extractLabels", at = @At("TAIL"))
    private void tougher$weightLabel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        InventoryWeightLabel.draw(graphics);
    }
}
