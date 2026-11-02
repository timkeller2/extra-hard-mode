package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.FeatureBus;
import dev.extrahardmode.feature.Inhabitants;
import dev.extrahardmode.feature.PlayerShops;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantResultSlot.class)
public abstract class MerchantResultSlotMixin {
    @Inject(method = "onTake", at = @At("HEAD"))
    private void tougher$stripShopMarker(Player player, ItemStack stack, CallbackInfo ci) {
        if (!FeatureBus.guard(player == null ? null : player.level(), Inhabitants.ID)) {
            return;
        }
        PlayerShops.stripMarker(stack);
    }
}
