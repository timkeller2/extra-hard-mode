package dev.extrahardmode.mixin.client;

import dev.extrahardmode.client.FoodCravingOutline;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The trade-list hover tooltip is not the container slot tooltip, so the craving line is added here. */
@Mixin(targets = "net.minecraft.client.gui.screens.inventory.MerchantScreen$TradeOfferButton")
public abstract class MerchantOfferFoodCravingMixin {
    @Redirect(
            method = "extractToolTip",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V"))
    private void tougher$shopFoodTooltip(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int mouseX, int mouseY) {
        FoodCravingOutline.showTooltip(graphics, font, stack, mouseX, mouseY);
    }
}
