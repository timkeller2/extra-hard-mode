package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.FeatureBus;
import dev.extrahardmode.feature.Inhabitants;
import dev.extrahardmode.feature.PlayerShops;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Chest trades are not the resident's own work, so they grant no experience. */
@Mixin(Villager.class)
public abstract class VillagerShopXpMixin {
    @Inject(method = "rewardTradeXp", at = @At("HEAD"), cancellable = true)
    private void tougher$noShopTradeXp(MerchantOffer offer, CallbackInfo ci) {
        if (!FeatureBus.guard(((Villager) (Object) this).level(), Inhabitants.ID)) {
            return;
        }
        if (PlayerShops.isShopOffer(offer)) {
            ci.cancel();
        }
    }
}
