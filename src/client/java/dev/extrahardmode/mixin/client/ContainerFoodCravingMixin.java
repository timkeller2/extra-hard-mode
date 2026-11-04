package dev.extrahardmode.mixin.client;

import dev.extrahardmode.client.FoodCravingOutline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Craving mark and tooltip on food in survival inventories, chests, and other containers. */
@Mixin(AbstractContainerScreen.class)
public abstract class ContainerFoodCravingMixin {
    @Inject(method = "extractSlot", at = @At("TAIL"))
    private void tougher$foodCravingOutline(
            GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        FoodCravingOutline.draw(graphics, slot);
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
