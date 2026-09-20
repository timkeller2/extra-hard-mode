package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Torches;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class InventorySlotMixin {
    @Shadow
    @Final
    public net.minecraft.world.Container container;

    @Shadow
    public abstract int getContainerSlot();

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void tougher$blockOffhandLight(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!(this.container instanceof Inventory inventory)) {
            return;
        }
        if (this.getContainerSlot() != Inventory.SLOT_OFFHAND) {
            return;
        }
        if (inventory.player.level().isClientSide()) {
            return;
        }
        if (Torches.denyOffhandPlace(inventory.player, stack)) {
            cir.setReturnValue(false);
        }
    }
}
