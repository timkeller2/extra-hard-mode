package dev.extrahardmode.mixin.client;

import dev.extrahardmode.client.FoodCravingOutline;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Craving mark and tooltip on food in containers, on a dragged stack, and on a drag preview. */
@Mixin(AbstractContainerScreen.class)
public abstract class ContainerFoodCravingMixin {
    @Shadow
    protected AbstractContainerMenu menu;

    @Shadow
    protected Set<Slot> quickCraftSlots;

    @Shadow
    protected boolean isQuickCrafting;

    @Inject(method = "extractSlot", at = @At("TAIL"))
    private void tougher$foodCravingOutline(
            GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        ItemStack shown = slot.getItem();
        if (this.isQuickCrafting && this.quickCraftSlots.size() > 1 && this.quickCraftSlots.contains(slot)) {
            ItemStack carried = this.menu.getCarried();
            if (!carried.isEmpty()) {
                shown = carried;
            }
        }
        FoodCravingOutline.drawOnItem(graphics, shown, slot.x, slot.y);
    }

    @Inject(method = "extractFloatingItem", at = @At("TAIL"))
    private void tougher$draggedFoodCraving(
            GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, String text, CallbackInfo ci) {
        FoodCravingOutline.drawOnItem(graphics, stack, x, y);
    }

    @Inject(method = "getTooltipFromContainerItem", at = @At("RETURN"), cancellable = true)
    private void tougher$foodCravingTooltip(ItemStack stack, CallbackInfoReturnable<List<Component>> cir) {
        Component line = FoodCravingOutline.tooltip(stack);
        if (line == null || cir.getReturnValue() == null) {
            return;
        }
        List<Component> lines = new ArrayList<>(cir.getReturnValue());
        lines.add(line);
        cir.setReturnValue(lines);
    }
}
