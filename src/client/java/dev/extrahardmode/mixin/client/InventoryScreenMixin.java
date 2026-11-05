package dev.extrahardmode.mixin.client;

import dev.extrahardmode.client.InventoryWeightLabel;
import dev.extrahardmode.client.WellFedRunIcon;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Weight label and the Well Fed reminder. {@code leftPos} and {@code topPos}
 * belong to the container screen, so this mixin extends it.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> {
    private InventoryScreenMixin(InventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 0, 0);
    }

    @Inject(method = "extractLabels", at = @At("TAIL"))
    private void tougher$weightLabel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        InventoryWeightLabel.draw(graphics);
        WellFedRunIcon.draw(graphics, mouseX, mouseY, this.leftPos, this.topPos);
    }
}
