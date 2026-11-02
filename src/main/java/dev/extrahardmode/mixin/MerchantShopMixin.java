package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.FeatureBus;
import dev.extrahardmode.feature.Inhabitants;
import dev.extrahardmode.feature.PlayerShops;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantMenu.class)
public abstract class MerchantShopMixin {
    @Inject(method = "quickMoveStack", at = @At("HEAD"))
    private void tougher$stripShopMarker(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (!FeatureBus.guard(player == null ? null : player.level(), Inhabitants.ID)) {
            return;
        }
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (index < 0 || index >= menu.slots.size()) {
            return;
        }
        Slot slot = menu.slots.get(index);
        if (slot instanceof MerchantResultSlot) {
            PlayerShops.stripMarker(slot.getItem());
        }
    }
}
