package dev.extrahardmode.mixin.client;

import dev.extrahardmode.client.FoodCravingOutline;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Craving mark on each food icon drawn in a bundle tooltip. */
@Mixin(ClientBundleTooltip.class)
public abstract class BundleFoodCravingMixin {
    @Inject(method = "extractSlot", at = @At("TAIL"))
    private void tougher$bundleFoodMark(
            int index,
            int x,
            int y,
            List<ItemStackTemplate> items,
            int indexAgain,
            Font font,
            GuiGraphicsExtractor graphics,
            CallbackInfo ci) {
        if (graphics == null || items == null || index < 1 || index > items.size()) {
            return;
        }
        ItemStack stack = items.get(items.size() - index).create();
        FoodCravingOutline.drawOnItem(graphics, stack, x + 4, y + 4);
    }
}
